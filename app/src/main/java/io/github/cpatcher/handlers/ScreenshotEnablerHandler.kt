package io.github.cpatcher.handlers

import android.os.Build
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.hookAllConstant
import io.github.cpatcher.arch.hookBefore
import io.github.cpatcher.arch.hookReplace
import io.github.cpatcher.arch.findClassN
import io.github.cpatcher.arch.getObj
import io.github.cpatcher.bridge.HookParam
import io.github.cpatcher.logE
import io.github.cpatcher.logI
import io.github.cpatcher.logW
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * ScreenshotEnablerHandler: Advanced FLAG_SECURE Bypass & Detection Evasion System
 * Version 3.0: Multi-vector interception with comprehensive OEM support
 * 
 * Technical Architecture:
 * - Neutralizes FLAG_SECURE at multiple framework layers
 * - Disables screenshot/recording detection mechanisms
 * - Ensures secure layer capture capability
 * - Maintains cross-version compatibility (Android 8.0-15)
 */
class ScreenshotEnablerHandler : IHook() {
    companion object {
        // Framework class constants
        private const val WINDOW_STATE = "com.android.server.wm.WindowState"
        private const val WINDOW_MANAGER_SERVICE = "com.android.server.wm.WindowManagerService"
        private const val ACTIVITY_TASK_MANAGER = "com.android.server.wm.ActivityTaskManagerService"
        
        // Method signatures
        private const val METHOD_IS_SECURE_LOCKED = "isSecureLocked"
        private const val METHOD_REGISTER_OBSERVER = "registerScreenCaptureObserver"
        private const val METHOD_REGISTER_RECORDING = "registerScreenRecordingCallback"
        
        // Field names for reflection
        private const val FIELD_CAPTURE_SECURE_LAYERS = "mCaptureSecureLayers"
        
        // Capture argument determination based on SDK version
        private val CAPTURE_ARGS_CLASS = if (Build.VERSION.SDK_INT >= 34) {
            "android.window.ScreenCapture\$CaptureArgs"
        } else {
            "android.view.SurfaceControl\$CaptureArgs"
        }
        
        private val SCREENSHOT_BUFFER_CLASS = if (Build.VERSION.SDK_INT >= 34) {
            "android.window.ScreenCapture\$ScreenshotHardwareBuffer"
        } else {
            "android.view.SurfaceControl\$ScreenshotHardwareBuffer"
        }
    }
    
    private var captureSecureLayersField: Field? = null
    
    override fun onHook() {
        // Verification: SystemUI or system_server context expected
        if (loadPackageParam.packageName != "android" && 
            loadPackageParam.packageName != "com.android.systemui") {
            logI("ScreenshotEnablerHandler: Activating for ${loadPackageParam.packageName}")
        }
        
        try {
            // Phase 1: Core security bypass - WindowState.isSecureLocked()
            executeWindowStateBypass()
            
            // Phase 2: Capture parameter modification - enable secure layers
            executeCaptureParameterModification()
            
            // Phase 3: Detection mechanism neutralization (SDK-specific)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                executeDetectionNeutralizationU()
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                executeRecordingDetectionBypassV()
            }
            
            // Phase 4: Hardware buffer secure flag bypass
            executeHardwareBufferBypass()
            
            // Phase 5: OEM-specific implementations
            executeOEMSpecificPatches()
            
            logI("ScreenshotEnablerHandler: Successfully initialized comprehensive bypass system")
            
        } catch (e: Throwable) {
            logE("ScreenshotEnablerHandler: Critical initialization failure", e)
        }
    }
    
    /**
     * Phase 1: WindowState Security Bypass
     * 
     * Technical Rationale:
     * - isSecureLocked() is the primary gatekeeper for FLAG_SECURE
     * - Called during surface creation and relayout operations
     * - Must selectively return false to avoid breaking legitimate security
     */
    private fun executeWindowStateBypass() {
        try {
            val windowStateClass = findClassOrNull(WINDOW_STATE) ?: run {
                logW("WindowState class not found - possible AOSP variant")
                return
            }
            
            windowStateClass.hookReplace(METHOD_IS_SECURE_LOCKED) { param ->
                // Stack analysis to determine calling context
                val stackTrace = Thread.currentThread().stackTrace
                
                // Allow security for initial surface creation (maintain system stability)
                for (i in 4..minOf(8, stackTrace.size - 1)) {
                    val methodName = stackTrace[i].methodName
                    if (methodName == "setInitialSurfaceControlProperties" || 
                        methodName == "createSurfaceLocked") {
                        // Return original value for surface initialization
                        return@hookReplace XposedBridge.invokeOriginalMethod(
                            param.method,
                            param.thisObject,
                            param.args
                        )
                    }
                }
                
                // Default: Bypass security check
                false
            }
            
            logI("WindowState security bypass established")
            
        } catch (e: NoSuchMethodException) {
            logE("isSecureLocked method signature mismatch", e)
        } catch (e: Throwable) {
            logE("WindowState bypass failed", e)
        }
    }
    
    /**
     * Phase 2: Capture Parameter Modification
     * 
     * Technical Flow:
     * 1. Intercept native capture methods
     * 2. Force mCaptureSecureLayers = true
     * 3. Enable secure content in screenshots
     */
    private fun executeCaptureParameterModification() {
        try {
            // Determine capture class based on SDK version
            val captureClass = if (Build.VERSION.SDK_INT >= 34) {
                findClassOrNull("android.window.ScreenCapture")
            } else {
                findClassOrNull("android.view.SurfaceControl")
            } ?: return logW("Capture class not found")
            
            // Get CaptureArgs class and secure layers field
            val captureArgsClass = findClass(CAPTURE_ARGS_CLASS)
            captureSecureLayersField = captureArgsClass.getDeclaredField(FIELD_CAPTURE_SECURE_LAYERS).apply {
                isAccessible = true
            }
            
            // Hook native capture methods
            listOf("nativeCaptureDisplay", "nativeCaptureLayers").forEach { methodName ->
                captureClass.declaredMethods
                    .filter { it.name == methodName }
                    .forEach { method ->
                        method.hookBefore { param ->
                            // First parameter is CaptureArgs
                            val captureArgs = param.args[0]
                            captureSecureLayersField?.set(captureArgs, true)
                        }
                    }
            }
            
            logI("Capture parameter modification established")
            
        } catch (e: ClassNotFoundException) {
            logW("CaptureArgs class not found - SDK variant", e)
        } catch (e: NoSuchFieldException) {
            logE("mCaptureSecureLayers field not found", e)
        } catch (e: Throwable) {
            logE("Capture parameter modification failed", e)
        }
    }
    
    /**
     * Phase 3: Screenshot Detection Neutralization (Android 14+)
     * 
     * Mechanism: registerScreenCaptureObserver returns null to prevent detection
     */
    private fun executeDetectionNeutralizationU() {
        try {
            val activityTaskManager = findClassOrNull(ACTIVITY_TASK_MANAGER) ?: return
            
            activityTaskManager.hookReplace(
                METHOD_REGISTER_OBSERVER,
                findClass("android.os.IBinder"),
                findClass("android.app.IScreenCaptureObserver")
            ) { _ ->
                // Return null to prevent observer registration
                null
            }
            
            logI("Screenshot detection neutralized (Android 14+)")
            
        } catch (e: Throwable) {
            logW("Detection neutralization failed (non-critical)", e)
        }
    }
    
    /**
     * Phase 4: Screen Recording Detection Bypass (Android 15+)
     * 
     * Mechanism: registerScreenRecordingCallback returns false
     */
    private fun executeRecordingDetectionBypassV() {
        try {
            val windowManagerService = findClassOrNull(WINDOW_MANAGER_SERVICE) ?: return
            
            windowManagerService.hookConstant(
                METHOD_REGISTER_RECORDING,
                findClass("android.window.IScreenRecordingCallback"),
                false
            )
            
            logI("Recording detection bypassed (Android 15+)")
            
        } catch (e: Throwable) {
            logW("Recording detection bypass failed (non-critical)", e)
        }
    }
    
    /**
     * Phase 5: Hardware Buffer Security Bypass
     * 
     * Purpose: ScreenshotHardwareBuffer.containsSecureLayers() always returns false
     */
    private fun executeHardwareBufferBypass() {
        try {
            val screenshotBufferClass = findClassOrNull(SCREENSHOT_BUFFER_CLASS) ?: return
            
            screenshotBufferClass.hookConstant("containsSecureLayers", false)
            
            logI("Hardware buffer security bypassed")
            
        } catch (e: Throwable) {
            logW("Hardware buffer bypass failed (non-critical)", e)
        }
    }
    
    /**
     * Phase 6: OEM-Specific Patch Implementation
     * 
     * Targets:
     * - Samsung OneUI: WmScreenshotController
     * - Xiaomi HyperOS: WindowManagerServiceImpl
     * - Oppo/OnePlus: OplusLongshotMainWindow
     */
    private fun executeOEMSpecificPatches() {
        // Samsung OneUI
        findClassOrNull("com.android.server.wm.WmScreenshotController")?.let { clazz ->
            try {
                clazz.hookAllConstant("canBeScreenshotTarget", true)
                logI("OneUI screenshot target bypass applied")
            } catch (e: Throwable) {
                logW("OneUI patch failed", e)
            }
        }
        
        // Xiaomi HyperOS (MIUI 14+)
        if (Build.VERSION.SDK_INT >= 34) {
            findClassOrNull("com.android.server.wm.WindowManagerServiceImpl")?.let { clazz ->
                try {
                    clazz.hookAllConstant("notAllowCaptureDisplay", false)
                    logI("HyperOS capture restriction bypassed")
                } catch (e: Throwable) {
                    logW("HyperOS patch failed", e)
                }
            }
        }
        
        // Oppo/OnePlus ColorOS
        findClassOrNull("com.android.server.wm.OplusLongshotMainWindow")?.let { clazz ->
            try {
                clazz.hookAllConstant("hasSecure", false)
                logI("ColorOS secure flag bypassed")
            } catch (e: Throwable) {
                logW("ColorOS patch failed", e)
            }
        }
    }
}