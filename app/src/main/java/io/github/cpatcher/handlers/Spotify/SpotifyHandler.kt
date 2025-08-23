package io.github.cpatcher.handlers.Spotify

import android.content.ClipData
import io.github.cpatcher.arch.*
import io.github.cpatcher.logI
import io.github.cpatcher.logE
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Modifier

class SpotifyHandler : IHook() {
    companion object {
        private const val KEY_PRODUCT_STATE = "product_state"
        private const val KEY_QUERY_PARAMS = "query_params"
        private const val KEY_SHARE_URL = "share_url"
        private const val KEY_NAV_BAR = "nav_bar"
        private const val KEY_WIDGET = "widget"
    }

    override fun onHook() {
        val tbl = createObfsTable("spotify", 2) { bridge ->
            val obfsMap = mutableMapOf<String, ObfsInfo>()
            
            // 1. ProductStateProto - simplified fingerprint
            bridge.findClass {
                matcher {
                    usingStrings("com.spotify.remoteconfig.internal.ProductStateProto")
                }
            }.firstOrNull()?.let { clazz ->
                clazz.methods.find { 
                    it.returnTypeName == "java.util.Map" 
                }?.let { method ->
                    obfsMap[KEY_PRODUCT_STATE] = method.toObfsInfo()
                    logI("Found ProductState: ${method.className}.${method.methodName}")
                }
            }
            
            // 2. Query parameters builder
            bridge.findMethod {
                matcher {
                    usingStrings("trackRows", "device_type:tablet")
                }
            }.firstOrNull()?.let { method ->
                obfsMap[KEY_QUERY_PARAMS] = method.toObfsInfo()
                logI("Found QueryParams: ${method.className}.${method.methodName}")
            }
            
            // 3. Share URL formatter - multiple variants
            val shareMethod = bridge.findMethod {
                matcher {
                    usingStrings("clipboard", "Spotify Link")
                }
            }.firstOrNull() ?: bridge.findMethod {
                matcher {
                    usingStrings("clipboard", "createNewSession failed")
                }
            }.firstOrNull()
            
            shareMethod?.let { method ->
                obfsMap[KEY_SHARE_URL] = method.toObfsInfo()
                logI("Found ShareURL: ${method.className}.${method.methodName}")
            }
            
            // 4. Navigation bar - simplified
            bridge.findClass {
                matcher {
                    usingStrings("NavigationBarItemSet(")
                }
            }.firstOrNull()?.let { clazz ->
                clazz.methods.find { 
                    it.isConstructor 
                }?.let { constructor ->
                    obfsMap[KEY_NAV_BAR] = constructor.toObfsInfo()
                    logI("Found NavBar: ${constructor.className}")
                }
            }
            
            // 5. Widget permission
            bridge.findMethod {
                matcher {
                    usingStrings("android.permission.BIND_APPWIDGET")
                }
            }.firstOrNull()?.let { method ->
                obfsMap[KEY_WIDGET] = method.toObfsInfo()
                logI("Found Widget: ${method.className}.${method.methodName}")
            }
            
            obfsMap
        }
        
        // Runtime hooks implementation
        
        // 1. Unlock Premium
        tbl[KEY_PRODUCT_STATE]?.let { info ->
            findClass(info.className).hookAfter(info.memberName) { param ->
                @Suppress("UNCHECKED_CAST")
                val attributesMap = param.result as? MutableMap<String, Any>
                attributesMap?.apply {
                    put("on-demand", "1")
                    put("high-quality-streaming", "1")
                    put("offline", "1")
                    put("ads", "0")
                    put("skip-limit", "0")
                    put("shuffle-restricted", "0")
                    logI("Premium attributes overridden")
                }
            }
        }
        
        // 2. Enable popular tracks
        tbl[KEY_QUERY_PARAMS]?.let { info ->
            findClass(info.className).hookAfter(info.memberName) { param ->
                val result = param.result?.toString() ?: return@hookAfter
                if (result.contains("checkDeviceCapability=")) {
                    param.result = XposedBridge.invokeOriginalMethod(
                        param.method, 
                        param.thisObject, 
                        arrayOf(param.args[0], true)
                    )
                    logI("Popular tracks enabled")
                }
            }
        }
        
        // 3. Sanitize share URLs
        tbl[KEY_SHARE_URL]?.let { info ->
            val clazz = findClass(info.className)
            
            // Hook with nested ClipData interception
            clazz.hook(info.memberName, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    // Set up nested hook for ClipData
                    XposedHelpers.findAndHookMethod(
                        ClipData::class.java,
                        "newPlainText",
                        CharSequence::class.java,
                        CharSequence::class.java,
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(innerParam: MethodHookParam) {
                                val url = innerParam.args[1] as? String ?: return
                                innerParam.args[1] = sanitizeUrl(url)
                            }
                        }
                    )
                }
            })
        }
        
        // 4. Hide Create button
        tbl[KEY_NAV_BAR]?.let { info ->
            findClass(info.className).hookBefore(info.memberName) { param ->
                param.args.forEachIndexed { index, arg ->
                    if (isCreateButton(arg)) {
                        param.args[index] = null
                        logI("Hidden Create button at index $index")
                    }
                }
            }
        }
        
        // 5. Fix widget permission
        tbl[KEY_WIDGET]?.let { info ->
            findClass(info.className).hookReplace(info.memberName) { 
                true // Force return true
            }
            logI("Widget permission granted")
        }
    }
    
    private fun sanitizeUrl(url: String): String {
        return url
            .replace(Regex("\\?si=[^&]*"), "")
            .replace(Regex("&si=[^&]*"), "")
            .replace(Regex("\\?context=[^&]*"), "")
            .replace(Regex("&context=[^&]*"), "")
    }
    
    private fun isCreateButton(item: Any?): Boolean {
        if (item == null) return false
        return try {
            val className = item.javaClass.simpleName
            className.contains("Create", ignoreCase = true) || 
            className.contains("Plus", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}