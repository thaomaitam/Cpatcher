// core/bridge/MethodHookCallback.java  
package io.github.cpatcher.core.bridge;

public abstract class MethodHookCallback {
    public void beforeHook(HookParam param) {
        // Override if needed
    }
    
    public void afterHook(HookParam param) {
        // Override if needed
    }
}