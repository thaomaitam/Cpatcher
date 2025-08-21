// core/bridge/HookParam.java (Updated)
package io.github.cpatcher.core.bridge;

public class HookParam {
    private de.robv.android.xposed.XC_MethodHook.MethodHookParam methodHookParam;
    
    public final Object thisObject;
    public final Object[] args;
    
    public HookParam(de.robv.android.xposed.XC_MethodHook.MethodHookParam param) {
        this.methodHookParam = param;
        this.thisObject = param.thisObject;
        this.args = param.args;
    }
    
    public Object getResult() {
        return methodHookParam.getResult();
    }
    
    public void setResult(Object result) {
        methodHookParam.setResult(result);
    }
    
    public Throwable getThrowable() {
        return methodHookParam.getThrowable();
    }
    
    public void setThrowable(Throwable throwable) {
        methodHookParam.setThrowable(throwable);
    }
    
    public boolean hasThrowable() {
        return methodHookParam.hasThrowable();
    }
    
    // Convenience properties for Kotlin
    public Object result;
    public Throwable throwable;
    
    // Sync with underlying param
    public void syncResult() {
        if (result != null) {
            setResult(result);
        }
        if (throwable != null) {
            setThrowable(throwable);
        }
    }
}