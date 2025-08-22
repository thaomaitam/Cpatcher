package io.github.cpatcher.handlers

import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.createObfsTable
import io.github.cpatcher.arch.hookAllBefore
import io.github.cpatcher.arch.hookAllAfter
import io.github.cpatcher.arch.hookReplace
import io.github.cpatcher.arch.toObfsInfo
import io.github.cpatcher.logI
import io.github.cpatcher.logE

class TikTokHandler : IHook() {
    companion object {
        // Logical keys for our obfuscation table
        private const val KEY_CREATE_DOWNLOAD_REQUEST = "create_download_request"
        private const val KEY_IS_WATERMARK_ENABLED = "is_watermark_enabled"
        private const val KEY_WATERMARK_SETTINGS = "watermark_settings"
        private const val KEY_BUILD_DOWNLOAD_URL = "build_download_url"
        private const val KEY_DOWNLOAD_PARAMS = "download_params"
    }

    override fun onHook() {
        // Phase 1: Dynamic Fingerprinting with DexKit
        val tbl = createObfsTable("tiktok", 2) { bridge ->
            val obfsMap = mutableMapOf<String, io.github.cpatcher.arch.ObfsInfo>()
            
            // Find method that creates download requests
            // Look for methods containing "download_status_downloading" string
            bridge.findMethod {
                matcher {
                    usingStrings("download_status_downloading", "download_addr")
                }
            }.firstOrNull()?.let { method ->
                obfsMap[KEY_CREATE_DOWNLOAD_REQUEST] = method.toObfsInfo()
                logI("Found create_download_request: ${method.className}.${method.methodName}")
            }
            
            // Find watermark status check methods
            // These typically return boolean and contain "watermark_status"
            bridge.findMethod {
                matcher {
                    returnType = "boolean"
                    usingStrings("watermark_status", "watermark")
                }
            }.forEach { method ->
                obfsMap[KEY_IS_WATERMARK_ENABLED] = method.toObfsInfo()
                logI("Found is_watermark_enabled: ${method.className}.${method.methodName}")
            }
            
            // Find watermark settings/preference methods
            // Look for methods that handle watermark preferences
            bridge.findMethod {
                matcher {
                    usingStrings("save_video", "watermark", "settings")
                    returnType = "boolean"
                }
            }.firstOrNull()?.let { method ->
                obfsMap[KEY_WATERMARK_SETTINGS] = method.toObfsInfo()
                logI("Found watermark_settings: ${method.className}.${method.methodName}")
            }
            
            // Find URL building methods
            // These construct the actual download URLs
            bridge.findMethod {
                matcher {
                    returnType = "java.lang.String"
                    usingStrings("watermark=1", "https://")
                }
            }.forEach { method ->
                obfsMap[KEY_BUILD_DOWNLOAD_URL] = method.toObfsInfo()
                logI("Found build_download_url: ${method.className}.${method.methodName}")
            }
            
            // Find download parameter methods
            // These prepare parameters for download requests
            bridge.findMethod {
                matcher {
                    usingStrings("download_permission", "watermark")
                }
            }.firstOrNull()?.let { method ->
                obfsMap[KEY_DOWNLOAD_PARAMS] = method.toObfsInfo()
                logI("Found download_params: ${method.className}.${method.methodName}")
            }
            
            obfsMap
        }
        
        // Phase 2: Runtime Hooking with Xposed
        
        // Layer 1: URL Manipulation - Primary approach
        tbl[KEY_BUILD_DOWNLOAD_URL]?.let { info ->
            findClass(info.className).hookAllAfter(info.memberName) { param ->
                val result = param.result
                if (result is String && result.contains("watermark=1")) {
                    // Replace watermark=1 with watermark=0 in the URL
                    param.result = result.replace("watermark=1", "watermark=0")
                    logI("Modified download URL: removed watermark parameter")
                }
            }
        }
        
        // Also hook the download request creation method
        tbl[KEY_CREATE_DOWNLOAD_REQUEST]?.let { info ->
            findClass(info.className).hookAllBefore(info.memberName) { param ->
                // Check if any argument is a string containing watermark=1
                param.args.forEachIndexed { index, arg ->
                    if (arg is String && arg.contains("watermark=1")) {
                        param.args[index] = arg.replace("watermark=1", "watermark=0")
                        logI("Modified download request parameter at index $index")
                    }
                }
            }
        }
        
        // Layer 2: Watermark Status Check Bypass
        tbl[KEY_IS_WATERMARK_ENABLED]?.let { info ->
            findClass(info.className).hookReplace(info.memberName) { _ ->
                // Always return false - watermark is never enabled
                logI("Bypassed watermark status check - returning false")
                false
            }
        }
        
        // Layer 3: Settings Override
        tbl[KEY_WATERMARK_SETTINGS]?.let { info ->
            findClass(info.className).hookReplace(info.memberName) { _ ->
                // Always return false - user "prefers" no watermark
                logI("Overridden watermark settings - returning false")
                false
            }
        }
        
        // Layer 4: Download Parameters Hook
        tbl[KEY_DOWNLOAD_PARAMS]?.let { info ->
            findClass(info.className).hookAllBefore(info.memberName) { param ->
                // Modify any Map or Bundle parameters to set watermark to 0
                param.args.forEach { arg ->
                    when (arg) {
                        is MutableMap<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
                            (arg as? MutableMap<String, Any>)?.let { map ->
                                if (map.containsKey("watermark")) {
                                    map["watermark"] = 0
                                    logI("Modified watermark in Map parameter")
                                }
                            }
                        }
                        is android.os.Bundle -> {
                            if (arg.containsKey("watermark")) {
                                arg.putInt("watermark", 0)
                                logI("Modified watermark in Bundle parameter")
                            }
                        }
                    }
                }
            }
        }
        
        logI("TikTok watermark removal handler initialized successfully")
    }
}