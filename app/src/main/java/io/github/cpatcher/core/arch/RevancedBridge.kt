// core/arch/RevancedBridge.kt
package io.github.cpatcher.core.arch

import io.github.cpatcher.core.utils.Logger

object RevancedBridge {
    fun shouldBlockVideoAds(): Boolean {
        return try {
            // Call ReVanced extension if available
            val clazz = Class.forName("app.revanced.extension.youtube.patches.VideoAdsPatch")
            val method = clazz.getMethod("shouldBlockAd")
            method.invoke(null) as Boolean
        } catch (e: Exception) {
            Logger.e("RevancedBridge: VideoAds fallback", e)
            true // Default to blocking ads
        }
    }
    
    fun shouldBlockSponsoredSegments(): Boolean {
        return try {
            val clazz = Class.forName("app.revanced.extension.youtube.patches.SponsorBlockPatch")
            val method = clazz.getMethod("isEnabled")
            method.invoke(null) as Boolean
        } catch (e: Exception) {
            Logger.e("RevancedBridge: SponsorBlock fallback", e)
            false // Default to not blocking
        }
    }
    
    fun getRevancedSetting(settingClass: String, settingField: String): Boolean {
        return try {
            val clazz = Class.forName(settingClass)
            val field = clazz.getField(settingField)
            val setting = field.get(null)
            val getMethod = setting.javaClass.getMethod("get")
            getMethod.invoke(setting) as Boolean
        } catch (e: Exception) {
            Logger.e("RevancedBridge: Setting access failed", e)
            false
        }
    }
}