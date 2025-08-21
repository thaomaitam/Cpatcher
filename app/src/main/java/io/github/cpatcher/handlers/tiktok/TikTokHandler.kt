// handlers/tiktok/TikTokHandler.kt
package io.github.cpatcher.handlers.tiktok

import io.github.cpatcher.core.arch.IHook
import io.github.cpatcher.core.utils.Logger
import io.github.cpatcher.handlers.tiktok.patches.WatermarkRemoval
import io.github.cpatcher.handlers.tiktok.patches.VideoDownload

class TikTokHandler : IHook() {
    override fun onHook() {
        Logger.i("TikTok: Initializing patches for ${loadPackageParam.packageName}")
        Logger.i("TikTok: Process: ${loadPackageParam.processName}")
        Logger.i("TikTok: App version: ${loadPackageParam.appInfo.versionName}")
        
        try {
            // Initialize patches
            subHook(WatermarkRemoval())
            subHook(VideoDownload())
            
            Logger.i("TikTok: All patches loaded successfully")
        } catch (e: Exception) {
            Logger.e("TikTok: Failed to load patches", e)
        }
    }
}