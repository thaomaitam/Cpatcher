package io.github.cpatcher

import android.content.res.XModuleResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.cpatcher.bridge.LoadPackageParam

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