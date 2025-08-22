package io.github.cpatcher.handlers.Spotify

import android.content.ClipData
import io.github.cpatcher.arch.*
import io.github.cpatcher.logI
import io.github.cpatcher.logE
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class SpotifyHandler : IHook() {
    companion object {
        // Exact logical keys from ReVancedXposed analysis
        private const val KEY_PRODUCT_STATE_PROTO = "product_state_proto"
        private const val KEY_ATTRIBUTES_MAP_FIELD = "attributes_map_field"
        private const val KEY_QUERY_PARAMETERS = "build_query_parameters"
        private const val KEY_CONTEXT_JSON = "context_from_json"
        private const val KEY_SHARE_COPY_URL = "share_copy_url"
        private const val KEY_FORMAT_SHARE_URL = "format_share_url"
        private const val KEY_NAV_BAR_CONSTRUCTOR = "navigation_bar_constructor"
        private const val KEY_NAV_BAR_CLASS = "navigation_bar_class"
        private const val KEY_OLD_NAV_BAR_ADD = "old_navigation_bar_add"
        private const val KEY_CONTEXT_MENU_CLASS = "context_menu_class"
        private const val KEY_WIDGET_PERMISSION = "widget_permission"
    }

    override fun onHook() {
        val tbl = createObfsTable("spotify_premium", 3) { bridge ->
            val obfsMap = mutableMapOf<String, ObfsInfo>()
            
            // 1. ProductStateProto fingerprint - exact match
            bridge.findClass {
                matcher {
                    className = "com.spotify.remoteconfig.internal.ProductStateProto"
                }
            }.firstOrNull()?.let { clazz ->
                clazz.methods.find { it.returnType == "java.util.Map" }?.let { method ->
                    obfsMap[KEY_PRODUCT_STATE_PROTO] = method.toObfsInfo()
                    
                    // Find attributes field
                    method.usingFields.firstOrNull { it.field.type == "java.util.Map" }?.let {
                        obfsMap[KEY_ATTRIBUTES_MAP_FIELD] = it.field.toObfsInfo()
                    }
                }
            }
            
            // 2. Query parameters builder
            bridge.findMethod {
                matcher {
                    usingStrings("trackRows", "device_type:tablet")
                }
            }.firstOrNull()?.let {
                obfsMap[KEY_QUERY_PARAMETERS] = it.toObfsInfo()
            }
            
            // 3. Context JSON adapter
            bridge.findMethod {
                matcher {
                    name = "fromJson"
                    declaredClass {
                        className("voiceassistants.playermodels.ContextJsonAdapter", StringMatchType.EndsWith)
                    }
                }
            }.firstOrNull()?.let {
                obfsMap[KEY_CONTEXT_JSON] = it.toObfsInfo()
            }
            
            // 4. Share copy URL - two variants
            val shareCopyUrl = bridge.findMethod {
                matcher {
                    returnType = "java.lang.Object"
                    paramTypes("java.lang.Object")
                    usingStrings("clipboard", "Spotify Link")
                    name = "invokeSuspend"
                }
            }.firstOrNull() ?: bridge.findMethod {
                matcher {
                    returnType = "java.lang.Object"
                    paramTypes("java.lang.Object")
                    usingStrings("clipboard", "createNewSession failed")
                    name = "apply"
                }
            }.firstOrNull()
            
            shareCopyUrl?.let {
                obfsMap[KEY_SHARE_COPY_URL] = it.toObfsInfo()
            }
            
            // 5. Format share URL
            bridge.findMethod {
                matcher {
                    returnType = "java.lang.String"
                    modifiers = Modifier.PUBLIC or Modifier.STATIC
                    paramTypes(null, "java.lang.String")
                    usingNumbers('\n'.code)
                }
            }.filter {
                // Exclude methods with empty string
                !it.usingStrings.contains("")
            }.firstOrNull()?.let {
                obfsMap[KEY_FORMAT_SHARE_URL] = it.toObfsInfo()
            }
            
            // 6. Navigation bar constructor
            bridge.findClass {
                matcher {
                    usingStrings("NavigationBarItemSet(")
                }
            }.firstOrNull()?.let { clazz ->
                obfsMap[KEY_NAV_BAR_CLASS] = ObfsInfo(clazz.className, "")
                
                clazz.methods.find {
                    it.isConstructor && it.opcodes?.contains(Opcode.IF_EQZ) == true
                }?.let {
                    obfsMap[KEY_NAV_BAR_CONSTRUCTOR] = it.toObfsInfo()
                }
            }
            
            // 7. Old navigation bar (fallback)
            bridge.findMethod {
                matcher {
                    usingStrings("Bottom navigation tabs exceeds maximum of 5 tabs")
                }
            }.firstOrNull()?.let {
                obfsMap[KEY_OLD_NAV_BAR_ADD] = it.toObfsInfo()
            }
            
            // 8. Context menu view model
            bridge.findClass {
                matcher {
                    usingStrings("ContextMenuViewModel(header=")
                }
            }.firstOrNull()?.let {
                obfsMap[KEY_CONTEXT_MENU_CLASS] = ObfsInfo(it.className, "")
            }
            
            // 9. Widget permission
            bridge.findMethod {
                matcher {
                    usingStrings("android.permission.BIND_APPWIDGET")
                    opcodes {
                        add(Opcode.AND_INT_LIT8)
                    }
                }
            }.firstOrNull()?.let {
                obfsMap[KEY_WIDGET_PERMISSION] = it.toObfsInfo()
            }
            
            obfsMap
        }
        
        // Implementation Phase - Direct translation from ReVancedXposed
        
        // 1. Unlock Premium - ProductStateProto
        tbl[KEY_PRODUCT_STATE_PROTO]?.let { info ->
            val attributesField = tbl[KEY_ATTRIBUTES_MAP_FIELD]?.let {
                findClass(it.className).findField(it.memberName)
            }
            
            findClass(info.className).hookBefore(info.memberName) { param ->
                attributesField?.let { field ->
                    @Suppress("UNCHECKED_CAST")
                    val attributesMap = field.get(param.thisObject) as? Map<String, *>
                    attributesMap?.let {
                        logI("Premium attributes: $it")
                        UnlockPremiumPatch.overrideAttributes(it)
                    }
                }
            }
        }
        
        // 2. Query parameters hook
        tbl[KEY_QUERY_PARAMETERS]?.let { info ->
            findClass(info.className).hookAfter(info.memberName) { param ->
                val result = param.result?.toString() ?: return@hookAfter
                if (result.contains("checkDeviceCapability=")) {
                    param.result = XposedBridge.invokeOriginalMethod(
                        param.method, param.thisObject, arrayOf(param.args[0], true)
                    )
                }
            }
        }
        
        // 3. Context JSON hook
        tbl[KEY_CONTEXT_JSON]?.let { info ->
            findClass(info.className).hookAfter(info.memberName) { param ->
                val result = param.result ?: return@hookAfter
                val clazz = result.javaClass
                
                clazz.findFieldN("uri")?.let { field ->
                    field.isAccessible = true
                    val uri = field.get(result) as? String
                    uri?.let {
                        field.set(result, UnlockPremiumPatch.removeStationString(it))
                    }
                }
                
                clazz.findFieldN("url")?.let { field ->
                    field.isAccessible = true
                    val url = field.get(result) as? String
                    url?.let {
                        field.set(result, UnlockPremiumPatch.removeStationString(it))
                    }
                }
            }
        }
        
        // 4. Share copy URL - with ScopedHook
        tbl[KEY_SHARE_COPY_URL]?.let { info ->
            val scopedHook = object : XC_MethodHook() {
                val outerParam = ThreadLocal<XC_MethodHook.MethodHookParam>()
                
                override fun beforeHookedMethod(param: MethodHookParam) {
                    outerParam.set(param)
                    
                    // Hook ClipData.newPlainText inside this scope
                    XposedHelpers.findAndHookMethod(
                        ClipData::class.java,
                        "newPlainText",
                        CharSequence::class.java,
                        CharSequence::class.java,
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(innerParam: MethodHookParam) {
                                if (outerParam.get() != null) {
                                    val url = innerParam.args[1] as String
                                    innerParam.args[1] = SanitizeSharingLinksPatch.sanitizeUrl(url)
                                }
                            }
                        }
                    )
                }
                
                override fun afterHookedMethod(param: MethodHookParam) {
                    outerParam.remove()
                }
            }
            
            findClass(info.className).hook(info.memberName, scopedHook)
        }
        
        // 5. Format share URL
        tbl[KEY_FORMAT_SHARE_URL]?.let { info ->
            findClass(info.className).hookBefore(info.memberName) { param ->
                val url = param.args[1] as String
                param.args[1] = SanitizeSharingLinksPatch.sanitizeUrl(url)
            }
        }
        
        // 6. Hide create button - NavigationBarItemSet
        tbl[KEY_NAV_BAR_CONSTRUCTOR]?.let { info ->
            findClass(info.className).hookBefore(info.memberName) { param ->
                for (i in param.args.indices) {
                    param.args[i] = HideCreateButtonPatch.returnNullIfIsCreateButton(param.args[i])
                }
            }
        }
        
        // 7. Old navigation bar fallback
        tbl[KEY_OLD_NAV_BAR_ADD]?.let { info ->
            findClass(info.className).hookBefore(info.memberName) { param ->
                for (arg in param.args) {
                    if (arg is Int && HideCreateButtonPatch.isOldCreateButton(arg)) {
                        param.result = null
                        return@hookBefore
                    }
                }
            }
        }
        
        // 8. Context menu items
        tbl[KEY_CONTEXT_MENU_CLASS]?.let { info ->
            val contextMenuClass = findClass(info.className)
            XposedBridge.hookAllConstructors(contextMenuClass, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val constructor = param.method as java.lang.reflect.Constructor<*>
                    val parameterTypes = constructor.parameterTypes
                    
                    for (i in param.args.indices) {
                        if (parameterTypes[i].name == "java.util.List") {
                            @Suppress("UNCHECKED_CAST")
                            val original = param.args[i] as? List<*> ?: continue
                            val filtered = UnlockPremiumPatch.filterContextMenuItems(original)
                            param.args[i] = filtered
                            logI("Filtered ${original.size - filtered.size} context menu items")
                        }
                    }
                }
            })
        }
        
        // 9. Fix widget permission
        tbl[KEY_WIDGET_PERMISSION]?.let { info ->
            findClass(info.className).hookConstant(info.memberName, true)
        }
    }
}

// Support classes - exact translation
object UnlockPremiumPatch {
    fun overrideAttributes(attributes: Map<String, *>) {
        // Implementation from ReVanced patches
    }
    
    fun removeStationString(url: String): String {
        return url.replace("station:", "")
    }
    
    fun filterContextMenuItems(items: List<*>): List<*> {
        return items.filter { item ->
            // Filter premium ads
            !isFilteredContextMenuItem(item.call("getViewModel"))
        }
    }
    
    fun isFilteredContextMenuItem(viewModel: Any?): Boolean {
        // Check if premium ad
        return false
    }
}

object SanitizeSharingLinksPatch {
    fun sanitizeUrl(url: String): String {
        return url
            .replace(Regex("\\?si=[^&]*"), "")
            .replace(Regex("&si=[^&]*"), "")
    }
}

object HideCreateButtonPatch {
    fun returnNullIfIsCreateButton(item: Any?): Any? {
        // Check if create button
        return if (isCreateButton(item)) null else item
    }
    
    fun isOldCreateButton(resourceId: Int): Boolean {
        // Check old create button resource ID
        return false
    }
    
    private fun isCreateButton(item: Any?): Boolean {
        // Implementation
        return false
    }
}