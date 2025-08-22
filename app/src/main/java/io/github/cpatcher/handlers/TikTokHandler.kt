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
        private const val KEY_CREATE_DOWNLOAD_REQUEST = "create_download_request"
        private const val KEY_IS_WATERMARK_ENABLED = "is_watermark_enabled"
        private const val KEY_BUILD_DOWNLOAD_URL = "build_download_url"
    }

    override fun onHook() {
        val tbl = createObfsTable("tiktok", 1) { bridge ->
            val obfsMap = mutableMapOf<String, io.github.cpatcher.arch.ObfsInfo>()
            
            // Tìm method xử lý download request
            bridge.findMethod {
                matcher {
                    usingStrings("download_addr", "watermark")
                }
            }.firstOrNull()?.let { method ->
                obfsMap[KEY_CREATE_DOWNLOAD_REQUEST] = method.toObfsInfo()
                logI("Found download request: ${method.className}")
            }
            
            // Tìm watermark check methods
            bridge.findMethod {
                matcher {
                    returnType = "boolean"
                    usingStrings("watermark")
                }
            }.firstOrNull()?.let { method ->
                obfsMap[KEY_IS_WATERMARK_ENABLED] = method.toObfsInfo()
                logI("Found watermark check: ${method.className}")
            }
            
            // Tìm URL builder
            bridge.findMethod {
                matcher {
                    returnType = "java.lang.String"
                    usingStrings("watermark=1")
                }
            }.firstOrNull()?.let { method ->
                obfsMap[KEY_BUILD_DOWNLOAD_URL] = method.toObfsInfo()
                logI("Found URL builder: ${method.className}")
            }
            
            obfsMap
        }
        
        // Hook 1: Sửa URL trực tiếp
        tbl[KEY_BUILD_DOWNLOAD_URL]?.let { info ->
            findClass(info.className).hookAllAfter(info.memberName) { param ->
                val result = param.result
                if (result is String && result.contains("watermark=1")) {
                    param.result = result.replace("watermark=1", "watermark=0")
                    logI("Modified URL: removed watermark")
                }
            }
        }
        
        // Hook 2: Bypass watermark check
        tbl[KEY_IS_WATERMARK_ENABLED]?.let { info ->
            findClass(info.className).hookReplace(info.memberName) { _ ->
                false
            }
        }
        
        // Hook 3: Sửa download request params
        tbl[KEY_CREATE_DOWNLOAD_REQUEST]?.let { info ->
            findClass(info.className).hookAllBefore(info.memberName) { param ->
                param.args.forEachIndexed { index, arg ->
                    if (arg is String && arg.contains("watermark=1")) {
                        param.args[index] = arg.replace("watermark=1", "watermark=0")
                    }
                }
            }
        }
        
        logI("TikTok handler initialized")
    }
}