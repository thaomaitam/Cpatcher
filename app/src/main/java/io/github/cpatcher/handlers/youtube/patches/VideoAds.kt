package io.github.cpatcher.handlers.youtube.patches

import io.github.cpatcher.core.arch.IHook
import io.github.cpatcher.core.arch.createObfsTable
import io.github.cpatcher.core.arch.hookAllBefore
import io.github.cpatcher.core.arch.RevancedBridge

class VideoAds : IHook() {
    override fun onHook() {
        val obfsTable = createObfsTable("youtube_video_ads", 1) { dexkit ->
            mapOf(
                "videoAdLoader" to dexkit.findMethod {
                    matcher {
                        usingStrings("advertisement_", "preroll")
                        returnType = "boolean"
                    }
                }.single().toObfsInfo()
            )
        }
        
        val adClass = findClass(obfsTable["videoAdLoader"]!!.className)
        val adMethod = obfsTable["videoAdLoader"]!!.memberName
        
        adClass.hookAllBefore(adMethod) { param ->
            // Call ReVanced extension logic
            if (RevancedBridge.shouldBlockVideoAds()) {
                param.result = false
            }
        }
    }
}