package io.github.cpatcher.handlers.Spotify

import io.github.cpatcher.arch.*
import io.github.cpatcher.logI

class SpotifyHandler : IHook() {
    companion object {
        private const val KEY_PRODUCT_STATE = "product_state"
        private const val KEY_QUERY_PARAMS = "query_params"
        private const val KEY_SHARE_COPY = "share_copy"
        private const val KEY_SHARE_FORMAT = "share_format"
        private const val KEY_NAV_BAR = "nav_bar"
        private const val KEY_WIDGET = "widget"
        private const val KEY_CONTEXT_JSON = "context_json"
        
        // Thread-local storage for nested hook context
        private val shareContext = ThreadLocal<Boolean>()
    }

    override fun onHook() {
        val tbl = createObfsTable("spotify_premium", 3) { bridge ->
            val obfsMap = mutableMapOf<String, ObfsInfo>()
            
            // 1. ProductStateProto getter
            bridge.findClass {
                matcher {
                    usingStrings("com.spotify.remoteconfig.internal.ProductStateProto")
                }
            }.firstOrNull()?.methods?.find { 
                it.returnTypeName == "java.util.Map" 
            }?.let {
                obfsMap[KEY_PRODUCT_STATE] = it.toObfsInfo()
            }
            
            // 2. Query parameters
            bridge.findMethod {
                matcher {
                    usingStrings("trackRows", "device_type:tablet")
                }
            }.firstOrNull()?.let {
                obfsMap[KEY_QUERY_PARAMS] = it.toObfsInfo()
            }
            
            // 3. Share copy URL - dual fingerprint
            val shareCopy = bridge.findMethod {
                matcher {
                    usingStrings("clipboard", "Spotify Link")
                    name = "invokeSuspend"
                }
            }.firstOrNull() ?: bridge.findMethod {
                matcher {
                    usingStrings("clipboard", "createNewSession failed")
                    name = "apply"
                }
            }.firstOrNull()
            
            shareCopy?.let {
                obfsMap[KEY_SHARE_COPY] = it.toObfsInfo()
            }
            
            // 4. Share format URL
            bridge.findMethod {
                matcher {
                    returnType = "java.lang.String"
                    paramTypes(null, "java.lang.String")
                    usingNumbers('\n'.code)
                }
            }.filter {
                !it.usingStrings.contains("")
            }.firstOrNull()?.let {
                obfsMap[KEY_SHARE_FORMAT] = it.toObfsInfo()
            }
            
            // 5. Context JSON
            bridge.findMethod {
                matcher {
                    name = "fromJson"
                    declaredClass {
                        usingStrings("voiceassistants.playermodels.ContextJsonAdapter")
                    }
                }
            }.firstOrNull()?.let {
                obfsMap[KEY_CONTEXT_JSON] = it.toObfsInfo()
            }
            
            // 6. Navigation bar
            bridge.findClass {
                matcher {
                    usingStrings("NavigationBarItemSet(")
                }
            }.firstOrNull()?.methods?.find { 
                it.isConstructor 
            }?.let {
                obfsMap[KEY_NAV_BAR] = it.toObfsInfo()
            }
            
            // 7. Widget permission
            bridge.findMethod {
                matcher {
                    usingStrings("android.permission.BIND_APPWIDGET")
                }
            }.firstOrNull()?.let {
                obfsMap[KEY_WIDGET] = it.toObfsInfo()
            }
            
            obfsMap
        }
        
        // Runtime Hooking Phase
        
        // 1. Premium attributes override
        tbl[KEY_PRODUCT_STATE]?.let { info ->
            findClass(info.className).hookAllAfter(info.memberName) { param ->
                @Suppress("UNCHECKED_CAST")
                (param.result as? MutableMap<String, Any>)?.apply {
                    put("on-demand", "1")
                    put("high-quality-streaming", "1")
                    put("offline", "1")
                    put("ads", "0")
                    put("skip-limit", "0")
                    put("shuffle-restricted", "0")
                }
            }
        }
        
        // 2. Query parameter enhancement
        tbl[KEY_QUERY_PARAMS]?.let { info ->
            findClass(info.className).hookAllAfter(info.memberName) { param ->
                val result = param.result?.toString() ?: return@hookAllAfter
                if (result.contains("checkDeviceCapability=")) {
                    param.method.invoke(param.thisObject, param.args[0], true)?.let {
                        param.result = it
                    }
                }
            }
        }
        
        // 3. Share copy URL sanitization - simplified approach
        tbl[KEY_SHARE_COPY]?.let { info ->
            findClass(info.className).hookAllBefore(info.memberName) { param ->
                shareContext.set(true)
            }
            
            findClass(info.className).hookAllAfter(info.memberName) { param ->
                shareContext.remove()
            }
        }
        
        // Hook ClipData globally when share context is active
        android.content.ClipData::class.java.hookBefore(
            "newPlainText",
            CharSequence::class.java,
            CharSequence::class.java
        ) { param ->
            if (shareContext.get() == true) {
                (param.args[1] as? String)?.let { url ->
                    param.args[1] = sanitizeUrl(url)
                }
            }
        }
        
        // 4. Share format URL sanitization
        tbl[KEY_SHARE_FORMAT]?.let { info ->
            findClass(info.className).hookAllBefore(info.memberName) { param ->
                (param.args[1] as? String)?.let { url ->
                    param.args[1] = sanitizeUrl(url)
                }
            }
        }
        
        // 5. Context JSON station removal
        tbl[KEY_CONTEXT_JSON]?.let { info ->
            findClass(info.className).hookAllAfter(info.memberName) { param ->
                param.result?.let { result ->
                    val clazz = result.javaClass
                    
                    // Process uri field
                    clazz.declaredFields.find { it.name == "uri" }?.let { field ->
                        field.isAccessible = true
                        (field.get(result) as? String)?.let { uri ->
                            field.set(result, uri.replace("station:", ""))
                        }
                    }
                    
                    // Process url field
                    clazz.declaredFields.find { it.name == "url" }?.let { field ->
                        field.isAccessible = true
                        (field.get(result) as? String)?.let { url ->
                            field.set(result, url.replace("station:", ""))
                        }
                    }
                }
            }
        }
        
        // 6. Navigation bar Create button removal
        tbl[KEY_NAV_BAR]?.let { info ->
            findClass(info.className).hookAllBefore(info.memberName) { param ->
                param.args.forEachIndexed { index, arg ->
                    if (isCreateButton(arg)) {
                        param.args[index] = null
                    }
                }
            }
        }
        
        // 7. Widget permission bypass
        tbl[KEY_WIDGET]?.let { info ->
            findClass(info.className).hookAllConstant(info.memberName, true)
        }
        
        logI("SpotifyHandler: All hooks applied successfully")
    }
    
    private fun sanitizeUrl(url: String): String {
        return url
            .replace(Regex("\\?si=[^&]*"), "")
            .replace(Regex("&si=[^&]*"), "")
            .replace(Regex("\\?context=[^&]*"), "")
            .replace(Regex("&context=[^&]*"), "")
    }
    
    private fun isCreateButton(item: Any?): Boolean {
        return item?.javaClass?.simpleName?.let { name ->
            name.contains("Create", ignoreCase = true) || 
            name.contains("Plus", ignoreCase = true)
        } ?: false
    }
}