package io.github.cpatcher.handlers

import android.content.Context
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.getObj
import io.github.cpatcher.arch.hookAfter
import io.github.cpatcher.bridge.HookParam  // Critical import addition
import io.github.cpatcher.logE
import io.github.cpatcher.logI

/**
 * QslockHandler: Quick Settings Lockscreen Security Enhancement
 * Version 2: Compilation-compliant implementation
 * 
 * Technical Architecture:
 * - Intercepts KeyguardDisplayManager state transitions
 * - Dynamically controls Quick Settings panel availability
 * - Ensures secure lockscreen environment
 */
class QslockHandler : IHook() {
    companion object {
        // StatusBarManager disable2 flags (Android Framework Constants)
        private const val DISABLE2_NONE = 0
        private const val DISABLE2_QUICK_SETTINGS = 1
        
        // Target Framework Components
        private const val KEYGUARD_DISPLAY_MANAGER = "com.android.keyguard.KeyguardDisplayManager"
        private const val METHOD_UPDATE_DISPLAYS = "updateDisplays"
    }
    
    override fun onHook() {
        // Verification: SystemUI context only
        if (loadPackageParam.packageName != "com.android.systemui") {
            logI("QslockHandler: Skipping - not SystemUI context")
            return
        }
        
        try {
            // Direct framework class targeting - no obfuscation expected
            val keyguardDisplayManager = findClass(KEYGUARD_DISPLAY_MANAGER)
            
            // Hook implementation: Post-execution interception strategy
            keyguardDisplayManager.hookAfter(
                METHOD_UPDATE_DISPLAYS,
                Boolean::class.java  // Single boolean parameter
            ) { param ->  // Implicit HookParam type inference
                executeQuickSettingsControl(param)
            }
            
            logI("QslockHandler: Successfully initialized lockscreen security enhancement")
            
        } catch (e: ClassNotFoundException) {
            logE("QslockHandler: KeyguardDisplayManager not found - possible AOSP variant", e)
        } catch (e: NoSuchMethodException) {
            logE("QslockHandler: updateDisplays method signature mismatch", e)
        } catch (e: Throwable) {
            logE("QslockHandler: Unexpected initialization failure", e)
        }
    }
    
    /**
     * Core Control Logic: Quick Settings State Management
     * 
     * Technical Flow:
     * 1. Extract lockscreen visibility state from parameter
     * 2. Acquire StatusBarManager service reference
     * 3. Apply appropriate disable2 flag configuration
     * 4. Implement comprehensive error isolation
     * 
     * @param param HookParam containing method invocation context
     */
    private fun executeQuickSettingsControl(param: HookParam) {
        try {
            // Extract lockscreen state from first boolean parameter
            val isLockscreenShowing = param.args[0] as? Boolean ?: return
            
            // Acquire system context for service access
            val context = param.thisObject?.getObj("mContext") as? Context
                ?: return logE("QslockHandler: Unable to acquire system context")
            
            // Obtain StatusBarManager service via reflection
            val statusBarManager = context.getSystemService("statusbar")
                ?: return logE("QslockHandler: StatusBarManager service unavailable")
            
            // Determine appropriate disable flag based on lockscreen state
            val disableFlag = if (isLockscreenShowing) {
                DISABLE2_QUICK_SETTINGS  // Lockscreen active - disable Quick Settings
            } else {
                DISABLE2_NONE            // Unlocked - restore Quick Settings
            }
            
            // Execute control operation via reflection
            applyDisableFlag(statusBarManager, disableFlag)
            
            logI("QslockHandler: Quick Settings state updated - " +
                "Lockscreen: $isLockscreenShowing, Flag: $disableFlag")
            
        } catch (e: ClassCastException) {
            logE("QslockHandler: Parameter type mismatch in updateDisplays", e)
        } catch (e: SecurityException) {
            logE("QslockHandler: Permission denied for StatusBarManager access", e)
        } catch (e: Throwable) {
            // Comprehensive error isolation - prevent SystemUI crash propagation
            logE("QslockHandler: Unexpected error in control logic", e)
        }
    }
    
    /**
     * Reflection-based StatusBarManager.disable2() Invocation
     * 
     * Technical Rationale:
     * - Cross-version compatibility via reflection
     * - Graceful degradation on API variations
     * - Isolated failure domain
     * 
     * @param statusBarManager Service instance obtained via getSystemService
     * @param flag DISABLE2_* constant for state control
     */
    private fun applyDisableFlag(statusBarManager: Any, flag: Int) {
        try {
            // Method signature: disable2(int state)
            val disable2Method = statusBarManager::class.java.getDeclaredMethod(
                "disable2", 
                Int::class.javaPrimitiveType
            )
            
            disable2Method.isAccessible = true
            disable2Method.invoke(statusBarManager, flag)
            
        } catch (e: NoSuchMethodException) {
            logE("QslockHandler: disable2 method not found - API version incompatibility", e)
        } catch (e: IllegalAccessException) {
            logE("QslockHandler: Access denied to disable2 method", e)
        } catch (e: Throwable) {
            logE("QslockHandler: Failed to invoke disable2", e)
        }
    }
}