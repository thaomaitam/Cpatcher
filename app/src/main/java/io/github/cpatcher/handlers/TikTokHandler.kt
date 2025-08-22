package io.github.cpatcher.handlers

import android.os.Bundle
import io.github.cpatcher.arch.*
import io.github.cpatcher.logI
import io.github.cpatcher.logE
import io.github.cpatcher.logD

class TikTokHandler : IHook() {
    companion object {
        private const val KEY_VIDEO_DOWNLOADER = "video_downloader"
        private const val KEY_SHARE_DOWNLOAD = "share_download"
        private const val KEY_SAVE_VIDEO = "save_video"
        private const val KEY_WATERMARK_PARAM = "watermark_param"
    }

    override fun onHook() {
        logI("Starting TikTok handler for package: ${loadPackageParam.packageName}")
        
        val tbl = createObfsTable("tiktok", 3) { bridge ->
            val obfsMap = mutableMapOf<String, ObfsInfo>()
            
            // Pattern 1: Download/Save video methods
            bridge.findMethod {
                matcher {
                    usingStrings("save_video", "download_video", "save_to_album")
                }
            }.forEach { method ->
                obfsMap[KEY_SAVE_VIDEO] = method.toObfsInfo()
                logI("Found save_video method: ${method.className}.${method.methodName}")
            }
            
            // Pattern 2: Share & Download
            bridge.findMethod {
                matcher {
                    usingStrings("share", "download", "video")
                }
            }.forEach { method ->
                obfsMap[KEY_SHARE_DOWNLOAD] = method.toObfsInfo()
                logI("Found share/download: ${method.className}.${method.methodName}")
            }
            
            // Pattern 3: Video URL với watermark
            bridge.findMethod {
                matcher {
                    usingStrings("play_addr", "download_addr", "video/play")
                }
            }.forEach { method ->
                obfsMap[KEY_VIDEO_DOWNLOADER] = method.toObfsInfo()
                logI("Found video downloader: ${method.className}.${method.methodName}")
            }
            
            // Pattern 4: Direct watermark parameter
            bridge.findMethod {
                matcher {
                    usingStrings("is_watermark", "watermark", "wm")
                }
            }.forEach { method ->
                obfsMap[KEY_WATERMARK_PARAM] = method.toObfsInfo()
                logI("Found watermark param: ${method.className}.${method.methodName}")
            }
            
            logI("Total methods found: ${obfsMap.size}")
            obfsMap
        }
        
        // Hook tất cả methods tìm được
        var hookCount = 0
        
        // Hook 1: Generic string replacement cho mọi method
        tbl.forEach { (key, info) ->
            try {
                findClass(info.className).hookAllBefore(info.memberName) { param ->
                    logD("Hook triggered: $key")
                    
                    // Check và modify string arguments
                    param.args.forEachIndexed { index, arg ->
                        when (arg) {
                            is String -> {
                                if (arg.contains("watermark=1") || arg.contains("is_watermark=1")) {
                                    param.args[index] = arg
                                        .replace("watermark=1", "watermark=0")
                                        .replace("is_watermark=1", "is_watermark=0")
                                    logI("Modified string at index $index")
                                }
                            }
                            is Bundle -> {
                                if (arg.containsKey("watermark")) {
                                    arg.putInt("watermark", 0)
                                    logI("Modified Bundle watermark")
                                }
                            }
                            is Map<*, *> -> {
                                @Suppress("UNCHECKED_CAST")
                                (arg as? MutableMap<String, Any>)?.let { map ->
                                    if (map.containsKey("watermark")) {
                                        map["watermark"] = 0
                                        logI("Modified Map watermark")
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Also hook after to modify results
                findClass(info.className).hookAllAfter(info.memberName) { param ->
                    val result = param.result
                    if (result is String) {
                        if (result.contains("watermark=1")) {
                            param.result = result.replace("watermark=1", "watermark=0")
                            logI("Modified result URL")
                        }
                    }
                }
                
                hookCount++
                logI("Hooked successfully: $key")
            } catch (e: Throwable) {
                logE("Failed to hook $key: ${e.message}")
            }
        }
        
        // Hook 2: Brute force - hook all URL building methods
        try {
            val urlBuilderClass = classLoader.findClassN("com.ss.android.ugc.aweme.download.component.DownloadUrlProvider")
            urlBuilderClass?.hookAllAfter("a") { param ->
                val result = param.result
                if (result is String && result.contains("http")) {
                    param.result = result.replace("watermark=1", "watermark=0")
                    logI("Brute force URL hook triggered")
                }
            }
        } catch (e: Throwable) {
            logD("URL builder class not found")
        }
        
        logI("TikTok handler initialized with $hookCount hooks")
    }
}