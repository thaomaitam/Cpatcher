package io.github.cpatcher

import android.content.res.XModuleResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.cpatcher.bridge.LoadPackageParam
import io.github.cpatcher.handlers.tiktok.TikTokHandler

class Entry : IXposedHookLoadPackage, IXposedHookZygoteInit {
    companion object {
        lateinit var modulePath: String
        val moduleRes: XModuleResources by lazy {
            XModuleResources.createInstance(
                modulePath,
                null
            )
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
    }

        override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        logI("Cpatcher: Hooking ${lpparam.packageName} (${lpparam.processName})")

        // 1. Tạo đối tượng handler trước
        val handler = when (lpparam.packageName) {
            "com.ss.android.ugc.trill",      // TikTok Global
            "com.zhiliaoapp.musically" -> {   // TikTok US
                logI("Entry: Found TikTok, creating handler for ${lpparam.packageName}")
                TikTokHandler() // Chỉ trả về đối tượng, không gọi hook ở đây
            }
            // Add other apps later
            // "com.google.android.youtube" -> YouTubeHandler()
            // "com.spotify.music" -> SpotifyHandler()
            else -> return // Nếu không phải app cần hook thì thoát
        }

        // 2. Cập nhật logPrefix
        logPrefix = "[${handler.javaClass.simpleName}] "
        
        // 3. Bây giờ mới gọi hàm hook trên đối tượng handler đã tạo
        handler.hook(LoadPackageParam(lpparam))
    }
}
