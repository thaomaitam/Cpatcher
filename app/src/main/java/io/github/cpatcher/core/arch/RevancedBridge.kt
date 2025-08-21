package io.github.cpatcher.core.arch

object RevancedBridge {
    // Bridge to ReVanced extension methods
    fun shouldBlockVideoAds(): Boolean {
        return try {
            app.revanced.extension.youtube.patches.VideoAdsPatch.shouldBlockAd()
        } catch (e: Exception) {
            true // Fallback to always block
        }
    }
    
    fun shouldBlockSponsoredSegments(): Boolean {
        return try {
            app.revanced.extension.youtube.patches.SponsorBlockPatch.isEnabled()
        } catch (e: Exception) {
            false
        }
    }
}