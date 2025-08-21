// handlers/tiktok/patches/VideoDownload.kt
package io.github.cpatcher.handlers.tiktok.patches

import io.github.cpatcher.core.arch.IHook
import io.github.cpatcher.core.arch.createObfsTable
import io.github.cpatcher.core.arch.hookAllAfter
import io.github.cpatcher.core.utils.Logger

class VideoDownload : IHook() {
    override fun onHook() {
        Logger.i("TikTok: Setting up enhanced video download")
        
        try {
            val obfsTable = createObfsTable("tiktok_download", 1) { dexkit ->
                mapOf(
                    // Network request interceptor
                    "networkRequest" to dexkit.findMethod {
                        matcher {
                            usingStrings("http", "video", "request")
                            returnType = "java.lang.Object"
                        }
                    }.singleOrNull()?.toObfsInfo(),
                    
                    // Video quality selector
                    "videoQuality" to dexkit.findMethod {
                        matcher {
                            usingStrings("720p", "1080p", "origin")
                            returnType = "java.lang.String"
                        }
                    }.singleOrNull()?.toObfsInfo()
                ).filterValues { it != null } as Map<String, ObfsInfo>
            }
            
            setupDownloadHooks(obfsTable)
            Logger.i("TikTok: Video download enhancement complete")
            
        } catch (e: Exception) {
            Logger.e("TikTok: Video download setup failed", e)
        }
    }
    
    private fun setupDownloadHooks(obfsTable: Map<String, ObfsInfo>) {
        // Hook network requests
        obfsTable["networkRequest"]?.let { info ->
            try {
                val requestClass = findClass(info.className)
                requestClass.hookAllAfter(info.memberName) { param ->
                    val request = param.result
                    Logger.i("TikTok: Network request intercepted: ${request?.javaClass?.simpleName}")
                    
                    // Modify request to get original quality
                    modifyVideoRequest(request)
                }
                Logger.i("TikTok: Hooked network requests")
            } catch (e: Exception) {
                Logger.e("TikTok: Failed to hook network requests", e)
            }
        }
        
        // Hook video quality selection
        obfsTable["videoQuality"]?.let { info ->
            try {
                val qualityClass = findClass(info.className)
                qualityClass.hookAllAfter(info.memberName) { param ->
                    val quality = param.result as? String
                    if (quality != null && quality != "origin") {
                        param.result = "origin" // Force highest quality
                        Logger.i("TikTok: Forced original quality instead of: $quality")
                    }
                }
                Logger.i("TikTok: Hooked video quality selector")
            } catch (e: Exception) {
                Logger.e("TikTok: Failed to hook quality selector", e)
            }
        }
    }
    
    private fun modifyVideoRequest(request: Any?) {
        try {
            if (request != null) {
                // Try to modify URL in request object
                val urlField = request.javaClass.getDeclaredField("url")
                urlField.isAccessible = true
                val originalUrl = urlField.get(request) as? String
                
                if (originalUrl?.contains("watermark") == true) {
                    val cleanUrl = originalUrl.replace("/watermark/", "/origin/")
                                             .replace("wm=1", "wm=0")
                    urlField.set(request, cleanUrl)
                    Logger.i("TikTok: Modified request URL to remove watermark")
                }
            }
        } catch (e: Exception) {
            Logger.e("TikTok: Failed to modify request", e)
        }
    }
}