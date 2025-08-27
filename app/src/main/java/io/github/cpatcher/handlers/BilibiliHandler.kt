package io.github.cpatcher.handlers

import android.content.Context
import io.github.cpatcher.arch.*
import io.github.cpatcher.bridge.HookParam
import io.github.cpatcher.logI
import io.github.cpatcher.logE
import io.github.cpatcher.logD
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Modifier

class BilibiliHandler : IHook() {
    companion object {
        private const val TABLE_VERSION = 2  // Incremented for cache invalidation
        
        // Quality constants
        private const val QUALITY_1080P = 1080
        private const val QUALITY_4K = 2160
        
        // Logical keys
        private const val KEY_PREMIUM_CHECK = "premium_status_check"
        private const val KEY_AD_CONTROLLER = "advertisement_controller"
        private const val KEY_QUALITY_LIMITER = "quality_restriction_method"
        private const val KEY_USER_MODEL = "user_model_class"
        private const val KEY_QUALITY_CONFIG = "quality_configuration"
    }
    
    override fun onHook() {
        // Phase 1: Enhanced package and process validation
        if (loadPackageParam.packageName != "com.bstar.intl") return
        
        // Critical: Skip sub-processes to prevent redundant hooking
        if (loadPackageParam.processName != loadPackageParam.packageName) {
            logI("${this::class.simpleName}: Skipping sub-process ${loadPackageParam.processName}")
            return
        }
        
        // Phase 2: Diagnostic-enhanced fingerprinting
        val obfsTable = try {
            createAdvancedObfsTable()
        } catch (e: Exception) {
            logE("${this::class.simpleName}: Fingerprinting catastrophic failure", e)
            // Fallback to direct class hooking strategy
            implementFallbackStrategy()
            return
        }
        
        // Phase 3: Selective hook deployment
        runCatching {
            implementPremiumBypass(obfsTable)
        }.onFailure { logE("Premium bypass failed", it) }
        
        runCatching {
            implementAdBlocking(obfsTable)
        }.onFailure { logE("Ad blocking failed", it) }
        
        runCatching {
            implementQualityUnlock(obfsTable)
        }.onFailure { logE("Quality unlock failed", it) }
        
        logI("${this::class.simpleName}: Hook deployment completed")
    }
    
    private fun createAdvancedObfsTable(): ObfsTable {
        return createObfsTable("bilibili", TABLE_VERSION) { bridge ->
            
            // Enhanced diagnostic logging
            logD("Starting DexKit analysis for Bilibili")
            
            // Strategy 1: Broader premium detection patterns
            val premiumMethods = bridge.findMethod {
                matcher {
                    // Relaxed string matching - partial matches
                    usingStrings(StringMatchType.Contains, 
                        "vip", "premium", "member", "subscription"
                    )
                    returnType = "boolean"
                }
            }.also { 
                logD("Found ${it.size} potential premium methods")
            }
            
            // Intelligent selection from candidates
            val premiumCheck = premiumMethods.firstOrNull { method ->
                // Additional validation criteria
                method.paramTypes.isEmpty() || 
                method.paramTypes.size == 1 && method.paramTypes[0] == "android.content.Context"
            } ?: premiumMethods.firstOrNull() 
              ?: error("Premium fingerprint failed after ${premiumMethods.size} candidates")
            
            // Strategy 2: Advertisement controller with relaxed criteria
            val adController = bridge.findMethod {
                matcher {
                    usingStrings(StringMatchType.Contains, "ad", "creative")
                    // No return type restriction - could be void or boolean
                }
            }.firstOrNull { method ->
                method.paramTypes.any { it.contains("String") }
            } ?: bridge.findMethod {
                matcher {
                    // Backup pattern - constructor/initialization methods
                    name(StringMatchType.Contains, "initAd", "loadAd", "showAd")
                }
            }.firstOrNull()
            
            // Strategy 3: Quality configuration detection
            val qualityMethods = bridge.findMethod {
                matcher {
                    usingStrings(StringMatchType.Contains, 
                        "quality", "resolution", "1080", "720", "480"
                    )
                    returnType(StringMatchType.Contains, "int", "Integer", "String")
                }
            }.also {
                logD("Found ${it.size} quality-related methods")
            }
            
            val qualityLimiter = qualityMethods.firstOrNull { method ->
                method.returnType == "int" && method.paramTypes.size <= 1
            } ?: qualityMethods.firstOrNull()
            
            // Strategy 4: User model class identification
            val userModelClass = bridge.findClass {
                matcher {
                    fields {
                        addAll(
                            listOf("vip", "expire", "uid"),
                            StringMatchType.Contains
                        )
                    }
                }
            }.firstOrNull()?.also {
                logD("Identified user model class: ${it.className}")
            }
            
            // Build result map with validation
            buildMap<String, ObfsInfo> {
                put(KEY_PREMIUM_CHECK, premiumCheck.toObfsInfo())
                adController?.let { 
                    put(KEY_AD_CONTROLLER, it.toObfsInfo()) 
                }
                qualityLimiter?.let { 
                    put(KEY_QUALITY_LIMITER, it.toObfsInfo()) 
                }
                userModelClass?.let {
                    put(KEY_USER_MODEL, ObfsInfo(it.className, ""))
                }
            }.also {
                logI("ObfsTable created with ${it.size} entries")
            }
        }
    }
    
    private fun implementFallbackStrategy() {
        logI("${this::class.simpleName}: Deploying fallback strategy")
        
        // Direct class targeting based on common patterns
        val potentialClasses = listOf(
            "com.bilibili.lib.account.model.AccountInfo",
            "com.bilibili.lib.account.UserInfo",
            "com.bstar.intl.model.User",
            "com.biliintl.app.model.UserModel"
        )
        
        potentialClasses.forEach { className ->
            findClassOrNull(className)?.let { clazz ->
                // Hook all boolean getters that might indicate premium
                clazz.declaredMethods
                    .filter { 
                        it.returnType == Boolean::class.java &&
                        it.parameterCount == 0 &&
                        (it.name.contains("vip", true) || 
                         it.name.contains("premium", true))
                    }
                    .forEach { method ->
                        method.hookConstant(true)
                        logD("Hooked fallback method: ${method.name}")
                    }
            }
        }
        
        // Quality restriction bypass - common method names
        val qualityMethods = listOf(
            "getMaxQuality", "getQualityLimit", 
            "getAvailableQualities", "checkQuality"
        )
        
        qualityMethods.forEach { methodName ->
            runCatching {
                findClass("com.bilibili.lib.media.resolver.resolve.BiliResolveResolver")
                    .hookAllConstant(methodName, QUALITY_1080P)
            }
        }
    }
    
    private fun implementPremiumBypass(obfsTable: ObfsTable) {
        val premiumInfo = obfsTable[KEY_PREMIUM_CHECK] ?: run {
            logE("Premium check not found in ObfsTable")
            return
        }
        
        // Primary hook with diagnostic logging
        findClass(premiumInfo.className).hookAllConstant(
            premiumInfo.memberName, 
            true
        ).also {
            logI("Premium bypass: Hooked ${it.size} methods")
        }
        
        // User model manipulation if identified
        obfsTable[KEY_USER_MODEL]?.let { userModel ->
            if (userModel.className.isNotEmpty()) {
                findClass(userModel.className).hookAllCAfter { param ->
                    param.thisObject?.apply {
                        runCatching {
                            // Attempt field injection with various naming patterns
                            listOf("vip_type", "vipType", "mVipType").forEach { field ->
                                runCatching { setObj(field, 2) }
                            }
                            listOf("vip_status", "vipStatus", "mVipStatus").forEach { field ->
                                runCatching { setObj(field, 1) }
                            }
                            listOf("is_vip", "isVip", "mIsVip").forEach { field ->
                                runCatching { setObj(field, true) }
                            }
                        }.onSuccess {
                            logD("User model fields injected")
                        }
                    }
                }
            }
        }
    }
    
    private fun implementAdBlocking(obfsTable: ObfsTable) {
        obfsTable[KEY_AD_CONTROLLER]?.let { adInfo ->
            findClass(adInfo.className).hookBefore(adInfo.memberName) { param ->
                param.result = null
                logD("Ad request blocked: ${adInfo.memberName}")
            }
        }
        
        // Generic ad suppression
        val adPatterns = listOf(
            "com.bilibili.ad",
            "com.bilibili.lib.ad",
            "com.bstar.ad"
        )
        
        adPatterns.forEach { packagePattern ->
            runCatching {
                val adClass = classLoader.loadClass(packagePattern)
                adClass.declaredMethods
                    .filter { it.name.contains("load") || it.name.contains("show") }
                    .forEach { it.hookNop() }
            }
        }
    }
    
    private fun implementQualityUnlock(obfsTable: ObfsTable) {
        obfsTable[KEY_QUALITY_LIMITER]?.let { qualityInfo ->
            findClass(qualityInfo.className).hookReplace(qualityInfo.memberName) { 
                QUALITY_1080P 
            }
        }
        
        // Stream parameter injection
        runCatching {
            findClass("com.bilibili.lib.media.MediaService")
                .hookAllBefore("requestStream") { param ->
                    param.args.forEachIndexed { index, arg ->
                        when (arg) {
                            is Int -> if (arg < QUALITY_1080P) {
                                param.args[index] = QUALITY_1080P
                            }
                            is String -> if (arg.contains("qn=")) {
                                param.args[index] = arg.replace(
                                    Regex("qn=\\d+"), 
                                    "qn=$QUALITY_1080P"
                                )
                            }
                        }
                    }
                }
        }
    }
}