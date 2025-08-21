package io.github.cpatcher

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.cpatcher.core.bridge.LoadPackageParam as CpatcherParam
import io.github.cpatcher.handlers.youtube.YouTubeHandler
import io.github.cpatcher.handlers.tiktok.TikTokHandler
import io.github.cpatcher.handlers.spotify.SpotifyHandler

class Entry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        val param = CpatcherParam(lpparam)
        
        when (lpparam.packageName) {
            "com.google.android.youtube" -> YouTubeHandler().hook(param)
            "com.ss.android.ugc.trill", "com.zhiliaoapp.musically" -> TikTokHandler().hook(param)
            "com.spotify.music" -> SpotifyHandler().hook(param)
        }
    }
}