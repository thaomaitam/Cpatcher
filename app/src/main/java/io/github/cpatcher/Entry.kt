// Entry.kt (Update existing)
package io.github.cpatcher

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.cpatcher.core.bridge.LoadPackageParam as CpatcherParam
import io.github.cpatcher.core.utils.Logger
import io.github.cpatcher.handlers.tiktok.TikTokHandler

class Entry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        val param = CpatcherParam(lpparam)
        
        when (lpparam.packageName) {
            "com.ss.android.ugc.trill",      // TikTok Global
            "com.zhiliaoapp.musically" -> {   // TikTok US
                Logger.i("Entry: Loading TikTok handler for ${lpparam.packageName}")
                TikTokHandler().hook(param)
            }
            // Add other apps later
            // "com.google.android.youtube" -> YouTubeHandler().hook(param)
            // "com.spotify.music" -> SpotifyHandler().hook(param)
        }
    }
}