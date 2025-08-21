// handlers/tiktok/TikTokHandler.kt
package io.github.cpatcher.handlers.tiktok

import android.content.pm.PackageManager
import io.github.cpatcher.Logger
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.handlers.tiktok.patches.VideoDownload
import io.github.cpatcher.handlers.tiktok.patches.WatermarkRemoval

class TikTokHandler : IHook() {
    override fun onHook() {
        val appInfo = loadPackageParam.appInfo
        val packageName = appInfo.packageName

        // Lấy versionName một cách chính xác
        val packageManager = loadPackageParam.appInfo.loadCONTEXT().packageManager
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName

        Logger.i("TikTok: Initializing patches for $packageName")
        Logger.i("TikTok: Process: ${loadPackageParam.processName}")
        Logger.i("TikTok: App version: $versionName")

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
// Thêm hàm extension này để lấy Context
fun ApplicationInfo.loadCONTEXT(): android.content.Context {
    val activityThreadClass = Class.forName("android.app.ActivityThread")
    val currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread")
    val activityThread = currentActivityThreadMethod.invoke(null)
    val getSystemContextMethod = activityThreadClass.getDeclaredMethod("getSystemContext")
    return getSystemContextMethod.invoke(activityThread) as android.content.Context
}