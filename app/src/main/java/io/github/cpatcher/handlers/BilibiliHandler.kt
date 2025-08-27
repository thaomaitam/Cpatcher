package io.github.cpatcher.handlers

import android.content.Context
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.createObfsTable
import io.github.cpatcher.arch.hookAfter
import io.github.cpatcher.arch.hookBefore
import io.github.cpatcher.arch.hookReplace
import io.github.cpatcher.arch.hookAllConstant
import io.github.cpatcher.arch.call
import io.github.cpatcher.arch.getObjAs
import io.github.cpatcher.arch.getObjAsN
import io.github.cpatcher.arch.setObj
import io.github.cpatcher.bridge.HookParam
import io.github.cpatcher.logI
import io.github.cpatcher.logE
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Modifier

class BilibiliHandler : IHook() {
    companion object {
        // Version control constants
        private const val TABLE_VERSION = 1
        
        // System constants for premium features
        private const val QUALITY_1080P = 1080
        private const val QUALITY_4K = 2160
        private const val AD_TYPE_PREROLL = 1
        private const val AD_TYPE_MIDROLL = 2
        
        // Logical keys for obfuscation mapping
        private const val KEY_PREMIUM_CHECK = "premium_status_check"
        private const val KEY_AD_CONTROLLER = "advertisement_controller"
        private const val KEY_QUALITY_LIMITER = "quality_restriction_method"
        private const val KEY_USER_INFO = "user_info_provider"
        private const val KEY_AD_REQUEST = "ad_request_builder"
        private const val KEY_STREAM_RESOLVER = "stream_quality_resolver"
    }
    
    override fun onHook() {
        // Phase 1: Package validation
        if (loadPackageParam.packageName != "com.bstar.intl") return
        
        // Phase 2: Advanced fingerprinting with multi-criteria detection
        val obfsTable = createObfsTable("bilibili", TABLE_VERSION) { bridge ->
            
            // Premium status verification method
            val premiumCheck = bridge.findMethod {
                matcher {
                    usingStrings = listOf(
                        "premium_status",
                        "vip_info",
                        "user_status"
                    )
                    returnType = "boolean"
                    modifiers = Modifier.PUBLIC or Modifier.FINAL
                }
            }.singleOrNull() ?: bridge.findMethod {
                // Fallback pattern
                matcher {
                    usingStrings = listOf("isPremium", "vip_type")
                    returnType = "boolean"
                }
            }.firstOrNull() ?: error("Premium check fingerprint failed")
            
            // Advertisement controller identification
            val adController = bridge.findMethod {
                matcher {
                    usingStrings = listOf(
                        "ad_request",
                        "advertisement",
                        "creative_id"
                    )
                    paramTypes = listOf("java.lang.String", "int")
                }
            }.firstOrNull() ?: bridge.findMethod {
                // Alternative pattern
                matcher {
                    usingStrings = listOf("showAd", "loadAd")
                    returnType = "void"
                }
            }.firstOrNull() ?: error("Ad controller fingerprint failed")
            
            // Quality restriction enforcement
            val qualityLimiter = bridge.findMethod {
                matcher {
                    usingStrings = listOf(
                        "quality_limit",
                        "max_quality",
                        "1080",
                        "720"
                    )
                    returnType = "int"
                }
            }.firstOrNull() ?: bridge.findMethod {
                matcher {
                    usingStrings = listOf("getMaxQuality", "resolution")
                    returnType = "int"
                }
            }.firstOrNull() ?: error("Quality limiter fingerprint failed")
            
            // User information provider
            val userInfo = bridge.findMethod {
                matcher {
                    usingStrings = listOf("user_info", "uid", "access_token")
                    returnType = "java.lang.Object"
                }
            }.firstOrNull()
            
            // Ad request builder
            val adRequest = bridge.findMethod {
                matcher {
                    usingStrings = listOf("ad_extra", "cid", "aid")
                    paramTypes = listOf("java.lang.String")
                }
            }.firstOrNull()
            
            // Stream quality resolver
            val streamResolver = bridge.findMethod {
                matcher {
                    usingStrings = listOf("stream_url", "quality_id", "qn")
                }
            }.firstOrNull()
            
            mapOf(
                KEY_PREMIUM_CHECK to premiumCheck.toObfsInfo(),
                KEY_AD_CONTROLLER to adController.toObfsInfo(),
                KEY_QUALITY_LIMITER to qualityLimiter.toObfsInfo()
            ).apply {
                userInfo?.let { this + (KEY_USER_INFO to it.toObfsInfo()) }
                adRequest?.let { this + (KEY_AD_REQUEST to it.toObfsInfo()) }
                streamResolver?.let { this + (KEY_STREAM_RESOLVER to it.toObfsInfo()) }
            }
        }
        
        // Phase 3: Strategic hook deployment
        implementPremiumBypass(obfsTable)
        implementAdBlocking(obfsTable)
        implementQualityUnlock(obfsTable)
        
        // Phase 4: Success validation
        logI("${this::class.simpleName}: Successfully initialized premium bypass")
    }
    
    private fun implementPremiumBypass(obfsTable: ObfsTable) {
        runCatching {
            val premiumInfo = obfsTable[KEY_PREMIUM_CHECK]!!
            
            // Primary strategy: Force premium status to true
            findClass(premiumInfo.className).hookAllConstant(
                premiumInfo.memberName, 
                true
            )
            
            // Secondary strategy: Manipulate user object fields
            obfsTable[KEY_USER_INFO]?.let { userInfo ->
                findClass(userInfo.className).hookAfter(userInfo.memberName) { param ->
                    param.result?.let { userObj ->
                        // Inject premium fields
                        runCatching {
                            userObj.setObj("vip_type", 2)  // Premium tier
                            userObj.setObj("vip_status", 1) // Active status
                            userObj.setObj("vip_expire", Long.MAX_VALUE)
                            userObj.setObj("is_premium", true)
                        }.onFailure { e ->
                            logE("Premium field injection failed", e)
                        }
                    }
                }
            }
            
            logI("Premium bypass hooks deployed")
        }.onFailure { t ->
            logE("Premium bypass implementation failed", t)
        }
    }
    
    private fun implementAdBlocking(obfsTable: ObfsTable) {
        runCatching {
            val adController = obfsTable[KEY_AD_CONTROLLER]!!
            
            // Primary: Neutralize ad controller
            findClass(adController.className).hookBefore(adController.memberName) { param ->
                // Prevent ad loading by intercepting before execution
                param.result = null
                logI("Ad request intercepted and blocked")
            }
            
            // Secondary: Block ad request construction
            obfsTable[KEY_AD_REQUEST]?.let { adRequest ->
                findClass(adRequest.className).hookReplace(adRequest.memberName) { param ->
                    // Return empty/null to prevent ad data fetch
                    null
                }
            }
            
            // Tertiary: Generic ad-related method suppression
            val adClasses = listOf(
                "com.bilibili.ad.AdManager",
                "com.bilibili.lib.ad.AdLoader",
                "tv.danmaku.bili.ui.video.ad.AdController"
            )
            
            adClasses.forEach { className ->
                findClassOrNull(className)?.let { clazz ->
                    clazz.hookAllConstant("loadAd", null)
                    clazz.hookAllConstant("showAd", null)
                    clazz.hookAllConstant("requestAd", null)
                }
            }
            
            logI("Ad blocking mechanisms engaged")
        }.onFailure { t ->
            logE("Ad blocking implementation failed", t)
        }
    }
    
    private fun implementQualityUnlock(obfsTable: ObfsTable) {
        runCatching {
            val qualityLimiter = obfsTable[KEY_QUALITY_LIMITER]!!
            
            // Primary: Override quality restrictions
            findClass(qualityLimiter.className).hookReplace(qualityLimiter.memberName) { param ->
                // Return maximum available quality
                QUALITY_1080P
            }
            
            // Secondary: Stream resolver manipulation
            obfsTable[KEY_STREAM_RESOLVER]?.let { resolver ->
                findClass(resolver.className).hookBefore(resolver.memberName) { param ->
                    // Inject high quality parameter
                    param.args.forEachIndexed { index, arg ->
                        if (arg is Int && arg < QUALITY_1080P) {
                            param.args[index] = QUALITY_1080P
                        }
                        if (arg is String && arg.contains("qn=")) {
                            param.args[index] = arg.replace(
                                Regex("qn=\\d+"), 
                                "qn=$QUALITY_1080P"
                            )
                        }
                    }
                }
            }
            
            // Tertiary: Quality selection UI manipulation
            val qualitySelectors = listOf(
                "getAvailableQualities",
                "getSupportedQualities",
                "getQualityList"
            )
            
            qualitySelectors.forEach { methodName ->
                runCatching {
                    findClass("com.bilibili.lib.media.resolver.resolve.BiliResolveResolver")
                        .hookAfter(methodName) { param ->
                            (param.result as? List<*>)?.let { qualities ->
                                // Ensure 1080p is available
                                if (!qualities.contains(QUALITY_1080P)) {
                                    val mutableList = qualities.toMutableList()
                                    mutableList.add(0, QUALITY_1080P)
                                    param.result = mutableList
                                }
                            }
                        }
                }
            }
            
            logI("Quality restrictions bypassed")
        }.onFailure { t ->
            logE("Quality unlock implementation failed", t)
        }
    }
}