package io.github.cpatcher.handlers

import android.os.Build
import io.github.cpatcher.arch.*
import io.github.cpatcher.logI

class GooglePhotosHandler : IHook() {
    companion object {
        // Keys for obfuscation table
        private const val KEY_FEATURE_CHECKER = "feature_checker"
        private const val KEY_IS_PIXEL_DEVICE = "is_pixel_device"
        
        // Device spoofing configuration
        private val PIXEL_BUILD_INFO = mapOf(
            "BRAND" to "google",
            "MANUFACTURER" to "Google", 
            "DEVICE" to "oriole",
            "PRODUCT" to "oriole",
            "MODEL" to "Pixel 6",
            "FINGERPRINT" to "google/oriole/oriole:14/UP1A.231105.001/10817346:user/release-keys"
        )
        
        // Features to enable (Nexus/Pixel exclusive features)
        private val FEATURES_TO_ENABLE = setOf(
            "com.google.android.apps.photos.NEXUS_PRELOAD",
            "com.google.android.apps.photos.nexus_preload",
            "com.google.android.feature.PIXEL_2021_EXPERIENCE",
            "com.google.android.feature.PIXEL_2022_EXPERIENCE",
            "com.google.android.feature.PIXEL_2023_EXPERIENCE",
            "com.google.android.feature.PIXEL_2024_EXPERIENCE"
        )
        
        // Features to disable (conflicting older Pixel features)
        private val FEATURES_TO_DISABLE = setOf(
            "com.google.android.apps.photos.PIXEL_2017_PRELOAD",
            "com.google.android.apps.photos.PIXEL_2018_PRELOAD",
            "com.google.android.apps.photos.PIXEL_2019_MIDYEAR_PRELOAD",
            "com.google.android.apps.photos.PIXEL_2019_PRELOAD",
            "com.google.android.feature.PIXEL_2020_MIDYEAR_EXPERIENCE",
            "com.google.android.feature.PIXEL_2020_EXPERIENCE"
        )
    }
    
    override fun onHook() {
        // Phase 0: Spoof device build properties
        spoofDeviceBuild()
        
        // Phase 1: Hook system-level feature checks
        hookSystemFeatures()
        
        // Phase 2: Hook app-internal feature checks using DexKit
        val tbl = createObfsTable("googlephotos", 1) { bridge ->
            val methods = mutableMapOf<String, ObfsInfo>()
            
            // Find internal feature checker method
            // Search for methods that check "magic_eraser" or "unblur" features
            try {
                val featureChecker = bridge.findMethod {
                    matcher {
                        usingStrings("magic_eraser", "unblur", "pixel_exclusive")
                        returnType = "boolean"
                    }
                }.firstOrNull()
                
                featureChecker?.let {
                    methods[KEY_FEATURE_CHECKER] = it.toObfsInfo()
                    logI("Found feature checker: ${it.className}.${it.methodName}")
                }
            } catch (e: Exception) {
                logI("Feature checker not found, using system hooks only")
            }
            
            // Find is_pixel_device checker
            try {
                val pixelChecker = bridge.findMethod {
                    matcher {
                        usingStrings("Pixel", "Google", "pixel_device")
                        returnType = "boolean"
                        paramCount = 0
                    }
                }.firstOrNull()
                
                pixelChecker?.let {
                    methods[KEY_IS_PIXEL_DEVICE] = it.toObfsInfo()
                    logI("Found pixel device checker: ${it.className}.${it.methodName}")
                }
            } catch (e: Exception) {
                logI("Pixel device checker not found")
            }
            
            methods
        }
        
        // Apply app-internal hooks if methods were found
        tbl[KEY_FEATURE_CHECKER]?.let { info ->
            findClass(info.className).hookAllBefore(info.memberName) { param ->
                val feature = param.args.getOrNull(0) as? String
                when (feature) {
                    "magic_eraser", "unblur", "pixel_exclusive" -> {
                        param.result = true
                        logI("Enabled feature: $feature")
                    }
                }
            }
        }
        
        tbl[KEY_IS_PIXEL_DEVICE]?.let { info ->
            findClass(info.className).hookAllConstant(info.memberName, true)
            logI("Forced is_pixel_device to return true")
        }
    }
    
    private fun spoofDeviceBuild() {
        val buildClass = Build::class.java
        PIXEL_BUILD_INFO.forEach { (field, value) ->
            try {
                buildClass.setObjS(field, value)
                logI("Spoofed Build.$field = $value")
            } catch (e: Exception) {
                logI("Failed to spoof Build.$field: ${e.message}")
            }
        }
    }
    
    private fun hookSystemFeatures() {
        // Hook ApplicationPackageManager.hasSystemFeature methods
        val apmClass = findClassOrNull("android.app.ApplicationPackageManager") ?: run {
            logI("ApplicationPackageManager not found, skipping system hooks")
            return
        }
        
        // Hook hasSystemFeature(String)
        apmClass.hookBefore("hasSystemFeature", String::class.java) { param ->
            val feature = param.args[0] as String
            when (feature) {
                in FEATURES_TO_ENABLE -> {
                    param.result = true
                    logI("Enabled system feature: $feature")
                }
                in FEATURES_TO_DISABLE -> {
                    param.result = false
                    logI("Disabled system feature: $feature")
                }
            }
        }
        
        // Hook hasSystemFeature(String, int)
        apmClass.hookBefore("hasSystemFeature", String::class.java, Int::class.java) { param ->
            val feature = param.args[0] as String
            when (feature) {
                in FEATURES_TO_ENABLE -> {
                    param.result = true
                    logI("Enabled system feature (versioned): $feature")
                }
                in FEATURES_TO_DISABLE -> {
                    param.result = false
                    logI("Disabled system feature (versioned): $feature")
                }
            }
        }
    }
}