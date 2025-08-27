// Entry.kt modification
package io.github.cpatcher

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.cpatcher.bridge.LoadPackageParam
import io.github.cpatcher.handlers.UniversalCachePurgeHandler

class Entry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // CRITICAL: Package isolation enforcement
        // Only affects packages explicitly scoped in LSPosed Manager
        
        logI("CachePurge: ${lpparam.packageName} ${lpparam.processName}")
        
        // System package exclusion
        if (lpparam.packageName == "android" || 
            lpparam.packageName.startsWith("com.android.systemui")) {
            return
        }
        
        // Universal cache handler - applies ONLY to scoped packages
        val cacheHandler = UniversalCachePurgeHandler()
        logPrefix = "[CachePurge] "
        cacheHandler.hook(LoadPackageParam(lpparam))
    }
}