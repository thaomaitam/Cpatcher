// handlers/tiktok/patches/WatermarkRemoval.kt
package io.github.cpatcher.handlers.tiktok.patches

import io.github.cpatcher.core.arch.IHook
import io.github.cpatcher.core.arch.ObfsTable
import io.github.cpatcher.core.arch.createObfsTable
import io.github.cpatcher.core.arch.hookAllBefore
import io.github.cpatcher.core.arch.hookAllAfter
import io.github.cpatcher.core.utils.Logger
import org.luckypray.dexkit.query.enums.StringMatchType

class WatermarkRemoval : IHook() {
    private lateinit var obfsTable: ObfsTable
    
    override fun onHook() {
        Logger.i("TikTok: Setting up watermark removal")
        
        try {
            obfsTable = createObfsTable("tiktok_watermark", 2) { dexkit ->
                mapOf(
                    // Method xử lý video URL
                    "videoUrlProcessor" to dexkit.findMethod {
                        matcher {
                            usingStrings("watermark", "video_url", "download")
                            returnType = "java.lang.String"
                            paramCount = 2
                        }
                    }.singleOrNull()?.toObfsInfo(),
                    
                    // Method xử lý watermark overlay
                    "watermarkOverlay" to dexkit.findMethod {
                        matcher {
                            usingStrings("watermark_position", "overlay")
                            returnType = "void"
                        }
                    }.singleOrNull()?.toObfsInfo(),
                    
                    // Download manager
                    "downloadManager" to dexkit.findMethod {
                        matcher {
                            declaredClass("DownloadManager", StringMatchType.Contains)
                            usingStrings("download", "video")
                            returnType = "boolean"
                        }
                    }.singleOrNull()?.toObfsInfo()
                ).filterValues { it != null } as Map<String, ObfsInfo>
            }
            
            setupWatermarkHooks()
            Logger.i("TikTok: Watermark removal setup complete")
            
        } catch (e: Exception) {
            Logger.e("TikTok: Watermark removal failed", e)
        }
    }
    
    private fun setupWatermarkHooks() {
        // Hook video URL processor
        obfsTable["videoUrlProcessor"]?.let { info ->
            try {
                val processorClass = findClass(info.className)
                processorClass.hookAllBefore(info.memberName) { param ->
                    val url = param.args[0] as? String
                    val addWatermark = param.args[1] as? Boolean
                    
                    if (addWatermark == true && url != null) {
                        param.args[1] = false // Force disable watermark
                        Logger.i("TikTok: Disabled watermark for video: ${url.substring(0, 50)}...")
                    }
                }
                Logger.i("TikTok: Hooked video URL processor")
            } catch (e: Exception) {
                Logger.e("TikTok: Failed to hook URL processor", e)
            }
        }
        
        // Hook watermark overlay
        obfsTable["watermarkOverlay"]?.let { info ->
            try {
                val overlayClass = findClass(info.className)
                overlayClass.hookAllBefore(info.memberName) { param ->
                    // Block watermark overlay completely
                    param.result = null
                    Logger.i("TikTok: Blocked watermark overlay")
                }
                Logger.i("TikTok: Hooked watermark overlay")
            } catch (e: Exception) {
                Logger.e("TikTok: Failed to hook overlay", e)
            }
        }
        
        // Hook download manager
        obfsTable["downloadManager"]?.let { info ->
            try {
                val downloadClass = findClass(info.className)
                downloadClass.hookAllAfter(info.memberName) { param ->
                    val result = param.result as? Boolean
                    if (result == true) {
                        Logger.i("TikTok: Video download initiated without watermark")
                    }
                }
                Logger.i("TikTok: Hooked download manager")
            } catch (e: Exception) {
                Logger.e("TikTok: Failed to hook download manager", e)
            }
        }
    }
}