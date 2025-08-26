package io.github.cpatcher.handlers

import android.content.Context
import android.graphics.Rect
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import de.robv.android.xposed.XC_MethodHook
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.createObfsTable
import io.github.cpatcher.arch.findClassN
import io.github.cpatcher.arch.getObj
import io.github.cpatcher.arch.getObjAs
import io.github.cpatcher.arch.getObjAsN
import io.github.cpatcher.arch.hook
import io.github.cpatcher.arch.hookAfter
import io.github.cpatcher.arch.hookAllAfter
import io.github.cpatcher.arch.hookAllBefore
import io.github.cpatcher.arch.hookAllConstant
import io.github.cpatcher.arch.hookBefore
import io.github.cpatcher.arch.setObj
import io.github.cpatcher.arch.toObfsInfo
import io.github.cpatcher.bridge.HookParam
import io.github.cpatcher.logE
import io.github.cpatcher.logI
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

/**
 * GboardHandler: IME Navigation Bar Spacer Elimination Module
 * Version: 1.0
 * 
 * Technical Architecture:
 * - Intercepts InputMethodService window inset calculations
 * - Modifies navigation bar padding computations
 * - Ensures gesture navigation compatibility while minimizing spacer
 * 
 * Primary Attack Vectors:
 * 1. Window inset manipulation during keyboard attachment
 * 2. Direct padding override in keyboard view hierarchy
 * 3. Navigation bar height zeroing for IME context
 */
class GboardHandler : IHook() {
    companion object {
        // Table versioning for cache invalidation
        private const val TABLE_VERSION = 1
        
        // Logical keys for obfuscated method mapping
        private const val KEY_COMPUTE_INSETS = "compute_insets"
        private const val KEY_UPDATE_PADDING = "update_keyboard_padding"
        private const val KEY_NAVIGATION_HEIGHT = "get_navigation_bar_height"
        private const parameter KEY_APPLY_WINDOW_INSETS = "apply_window_insets"
        
        // Configuration constants
        private const val MINIMAL_BOTTOM_PADDING = 0 // Pixels
        private const val MAX_ALLOWED_SPACER = 20 // Maximum allowed spacer in dp
    }
    
    override fun onHook() {
        // Verification: Gboard context only
        if (loadPackageParam.packageName != "com.google.android.inputmethod.latin") {
            logI("GboardHandler: Skipping - not Gboard context")
            return
        }
        
        try {
            // Phase 1: Fingerprint obfuscated methods
            val obfsTable = createObfsTable("gboard", TABLE_VERSION) { bridge ->
                generateObfuscationTable(bridge)
            }
            
            // Phase 2: Apply multi-vector hooking strategy
            implementPrimaryHooks(obfsTable)
            implementSecondaryHooks()
            implementFallbackStrategy()
            
            logI("GboardHandler: Successfully initialized IME spacer elimination module")
            
        } catch (t: Throwable) {
            logE("GboardHandler: Critical initialization failure", t)
            // Attempt emergency fallback
            implementEmergencyFallback()
        }
    }
    
    /**
     * Phase 1: Dynamic Method Fingerprinting
     * 
     * Strategy: Identify obfuscated methods through stable signatures
     * Technique: Multi-criteria DexKit pattern matching
     */
    private fun generateObfuscationTable(bridge: DexKitBridge): Map<String, ObfsInfo> {
        val table = mutableMapOf<String, ObfsInfo>()
        
        // Fingerprint 1: Window inset computation method
        runCatching {
            bridge.findMethod {
                matcher {
                    // InputMethodService typically contains "onComputeInsets"
                    usingStrings = listOf("onComputeInsets", "contentTopInsets", "visibleTopInsets")
                    returnType = "void"
                    paramTypes = listOf("android.inputmethodservice.InputMethodService\$Insets")
                }
            }.firstOrNull()?.let {
                table[KEY_COMPUTE_INSETS] = it.toObfsInfo()
                logI("GboardHandler: Located compute insets method - ${it.className}.${it.methodName}")
            }
        }.onFailure {
            logE("GboardHandler: Failed to fingerprint compute insets", it)
        }
        
        // Fingerprint 2: Keyboard padding update mechanism
        runCatching {
            bridge.findMethod {
                matcher {
                    usingStrings = listOf("keyboard_height", "padding_bottom", "updatePadding")
                    modifiers = org.luckypray.dexkit.query.enums.FieldModifier.PUBLIC
                }
            }.firstOrNull()?.let {
                table[KEY_UPDATE_PADDING] = it.toObfsInfo()
                logI("GboardHandler: Located padding update method - ${it.className}.${it.methodName}")
            }
        }.onFailure {
            logE("GboardHandler: Failed to fingerprint padding update", it)
        }
        
        // Fingerprint 3: Navigation bar height retrieval
        runCatching {
            bridge.findMethod {
                matcher {
                    usingStrings = listOf("navigation_bar_height", "nav_bar_height")
                    returnType = "int"
                    paramCount = 0..1
                }
            }.firstOrNull()?.let {
                table[KEY_NAVIGATION_HEIGHT] = it.toObfsInfo()
                logI("GboardHandler: Located navigation height method - ${it.className}.${it.methodName}")
            }
        }.onFailure {
            logE("GboardHandler: Failed to fingerprint navigation height", it)
        }
        
        // Fingerprint 4: Window insets application
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                bridge.findMethod {
                    matcher {
                        usingStrings = listOf("WindowInsets", "systemWindowInsets")
                        paramTypes = listOf("android.view.WindowInsets")
                    }
                }.firstOrNull()?.let {
                    table[KEY_APPLY_WINDOW_INSETS] = it.toObfsInfo()
                    logI("GboardHandler: Located window insets application - ${it.className}.${it.methodName}")
                }
            }
        }
        
        return table
    }
    
    /**
     * Phase 2A: Primary Hook Implementation
     * 
     * Strategy: Direct interception of identified obfuscated methods
     * Objective: Surgical modification of spacer calculations
     */
    private fun implementPrimaryHooks(obfsTable: Map<String, ObfsInfo>) {
        // Hook 1: Intercept compute insets for IME
        obfsTable[KEY_COMPUTE_INSETS]?.let { info ->
            try {
                val targetClass = findClass(info.className)
                targetClass.hookAfter(info.memberName) { param ->
                    // Modify the Insets object to eliminate bottom spacer
                    val insetsObj = param.args.getOrNull(0) ?: return@hookAfter
                    
                    // Zero out contentTopInsets to minimize spacer
                    insetsObj.setObj("contentTopInsets", 0)
                    insetsObj.setObj("visibleTopInsets", 0)
                    
                    logI("GboardHandler: Modified compute insets - spacer eliminated")
                }
            } catch (t: Throwable) {
                logE("GboardHandler: Failed to hook compute insets", t)
            }
        }
        
        // Hook 2: Override keyboard padding updates
        obfsTable[KEY_UPDATE_PADDING]?.let { info ->
            try {
                val targetClass = findClass(info.className)
                targetClass.hookBefore(info.memberName) { param ->
                    // Intercept padding values and minimize
                    param.args.indices.forEach { i ->
                        when (val arg = param.args[i]) {
                            is Int -> {
                                // If this looks like a padding value, minimize it
                                if (arg > MAX_ALLOWED_SPACER) {
                                    param.args[i] = MINIMAL_BOTTOM_PADDING
                                }
                            }
                            is Rect -> {
                                // Minimize bottom rect padding
                                arg.bottom = MINIMAL_BOTTOM_PADDING
                            }
                        }
                    }
                    logI("GboardHandler: Intercepted padding update - values minimized")
                }
            } catch (t: Throwable) {
                logE("GboardHandler: Failed to hook padding update", t)
            }
        }
        
        // Hook 3: Force navigation bar height to zero for IME
        obfsTable[KEY_NAVIGATION_HEIGHT]?.let { info ->
            try {
                val targetClass = findClass(info.className)
                targetClass.hookAllConstant(info.memberName, 0)
                logI("GboardHandler: Navigation bar height forced to zero")
            } catch (t: Throwable) {
                logE("GboardHandler: Failed to hook navigation height", t)
            }
        }
        
        // Hook 4: Modern window insets manipulation (Android Q+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            obfsTable[KEY_APPLY_WINDOW_INSETS]?.let { info ->
                try {
                    val targetClass = findClass(info.className)
                    targetClass.hookBefore(info.memberName) { param ->
                        (param.args[0] as? WindowInsets)?.let { insets ->
                            // Create modified insets with zero navigation bar
                            val modifiedInsets = WindowInsets.Builder(insets)
                                .setSystemWindowInsets(
                                    insets.systemWindowInsetLeft,
                                    insets.systemWindowInsetTop,
                                    insets.systemWindowInsetRight,
                                    0 // Zero bottom inset
                                )
                                .build()
                            param.args[0] = modifiedInsets
                        }
                    }
                    logI("GboardHandler: Window insets modified - bottom eliminated")
                } catch (t: Throwable) {
                    logE("GboardHandler: Failed to hook window insets", t)
                }
            }
        }
    }
    
    /**
     * Phase 2B: Secondary Hook Implementation
     * 
     * Strategy: Broad-spectrum interception of known IME classes
     * Technique: Pattern-based hooking without obfuscation dependency
     */
    private fun implementSecondaryHooks() {
        // Secondary Hook 1: InputMethodService direct modification
        runCatching {
            InputMethodService::class.java.hookAllAfter("onCreateInputView") { param ->
                val view = param.result as? View ?: return@hookAllAfter
                
                // Recursively minimize bottom padding in view hierarchy
                minimizeViewHierarchyPadding(view)
                
                // Force layout parameters adjustment
                view.layoutParams?.let { layoutParams ->
                    if (layoutParams is ViewGroup.MarginLayoutParams) {
                        layoutParams.bottomMargin = MINIMAL_BOTTOM_PADDING
                    }
                }
                
                logI("GboardHandler: InputMethodService view hierarchy optimized")
            }
        }.onFailure {
            logE("GboardHandler: Failed to hook InputMethodService", it)
        }
        
        // Secondary Hook 2: Window attachment interception
        runCatching {
            Window::class.java.hookBefore("setNavigationBarColor") { param ->
                // During IME window setup, intercept navigation bar configuration
                val window = param.thisObject as? Window
                window?.decorView?.let { decorView ->
                    decorView.setOnApplyWindowInsetsListener { view, insets ->
                        // Modify insets to eliminate bottom spacing
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            WindowInsets.Builder(insets)
                                .setSystemWindowInsets(
                                    insets.systemWindowInsetLeft,
                                    insets.systemWindowInsetTop,
                                    insets.systemWindowInsetRight,
                                    0
                                )
                                .build()
                        } else {
                            insets
                        }
                    }
                }
            }
        }.onFailure {
            logE("GboardHandler: Failed to hook Window navigation bar", it)
        }
    }
    
    /**
     * Phase 3: Emergency Fallback Strategy
     * 
     * Last-resort mechanism when primary strategies fail
     * Technique: Aggressive view hierarchy manipulation
     */
    private fun implementFallbackStrategy() {
        // Fallback: Hook all setPadding calls in Gboard context
        runCatching {
            View::class.java.hookBefore(
                "setPadding",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            ) { param ->
                // Only modify if this is keyboard-related view
                val view = param.thisObject as? View
                if (isKeyboardRelatedView(view)) {
                    // Force minimal bottom padding
                    param.args[3] = MINIMAL_BOTTOM_PADDING
                    logI("GboardHandler: Intercepted setPadding - bottom minimized")
                }
            }
        }.onFailure {
            logE("GboardHandler: Failed to implement setPadding fallback", it)
        }
    }
    
    /**
     * Emergency Fallback Implementation
     * 
     * Ultra-aggressive last resort when all other methods fail
     */
    private fun implementEmergencyFallback() {
        logI("GboardHandler: Initiating emergency fallback protocol")
        
        // Nuclear option: Hook all View measurements in IME context
        runCatching {
            ViewGroup::class.java.hookAllBefore("onMeasure") { param ->
                val view = param.thisObject as? ViewGroup ?: return@hookAllBefore
                
                // Check if this is the main keyboard container
                if (view.id == android.R.id.inputArea || 
                    view.javaClass.name.contains("keyboard", ignoreCase = true)) {
                    
                    // Force remeasurement with reduced height
                    view.post {
                        view.setPadding(
                            view.paddingLeft,
                            view.paddingTop,
                            view.paddingRight,
                            MINIMAL_BOTTOM_PADDING
                        )
                    }
                }
            }
            logI("GboardHandler: Emergency fallback activated")
        }.onFailure {
            logE("GboardHandler: Emergency fallback failed", it)
        }
    }
    
    /**
     * Utility: Recursive view hierarchy padding minimization
     */
    private fun minimizeViewHierarchyPadding(view: View) {
        // Minimize current view padding
        if (view.paddingBottom > MAX_ALLOWED_SPACER) {
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                MINIMAL_BOTTOM_PADDING
            )
        }
        
        // Recurse through children
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                minimizeViewHierarchyPadding(view.getChildAt(i))
            }
        }
    }
    
    /**
     * Utility: Identify keyboard-related views
     */
    private fun isKeyboardRelatedView(view: View?): Boolean {
        view ?: return false
        
        val className = view.javaClass.name.lowercase()
        return className.contains("keyboard") ||
               className.contains("input") ||
               className.contains("ime") ||
               view.id == android.R.id.inputArea
    }
}