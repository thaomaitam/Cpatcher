// Fully Compliant QslockHandler Implementation
package io.github.cpatcher.handlers

import android.content.Context
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.call      // CRITICAL: Add missing import
import io.github.cpatcher.arch.getObj
import io.github.cpatcher.arch.hookAfter
import io.github.cpatcher.bridge.HookParam
import io.github.cpatcher.logE
import io.github.cpatcher.logI

class QslockHandler : IHook() {
    companion object {
        private const val DISABLE2_NONE = 0
        private const val DISABLE2_QUICK_SETTINGS = 1
        private const val KEYGUARD_DISPLAY_MANAGER = "com.android.keyguard.KeyguardDisplayManager"
        private const val METHOD_UPDATE_DISPLAYS = "updateDisplays"
    }
    
    override fun onHook() {
        if (loadPackageParam.packageName != "com.android.systemui") {
            logI("${this::class.simpleName}: Skipping - not SystemUI context")
            return
        }
        
        runCatching {
            val keyguardDisplayManager = findClass(KEYGUARD_DISPLAY_MANAGER)
            
            keyguardDisplayManager.hookAfter(
                METHOD_UPDATE_DISPLAYS,
                Boolean::class.java
            ) { param ->
                executeQuickSettingsControl(param)
            }
            
            logI("${this::class.simpleName}: Successfully initialized lockscreen security enhancement")
            
        }.onFailure { t ->
            logE("${this::class.simpleName}: Initialization failed", t)
        }
    }
    
    private fun executeQuickSettingsControl(param: HookParam) {
        runCatching {
            // FIXED: Safe parameter access pattern
            val isLockscreenShowing = param.args.getOrNull(0) as? Boolean 
                ?: return@runCatching
            
            val context = param.thisObject?.getObj("mContext") as? Context
                ?: return@runCatching logE("${this::class.simpleName}: Unable to acquire system context")
            
            val statusBarManager = context.getSystemService("statusbar")
                ?: return@runCatching logE("${this::class.simpleName}: StatusBarManager service unavailable")
            
            val disableFlag = if (isLockscreenShowing) {
                DISABLE2_QUICK_SETTINGS
            } else {
                DISABLE2_NONE
            }
            
            // FIXED: Using project utility for method invocation
            statusBarManager.call("disable2", disableFlag)
            
            logI("${this::class.simpleName}: Quick Settings state updated - " +
                "Lockscreen: $isLockscreenShowing, Flag: $disableFlag")
            
        }.onFailure { t ->
            logE("${this::class.simpleName}: Control logic failure", t)
        }
    }
}