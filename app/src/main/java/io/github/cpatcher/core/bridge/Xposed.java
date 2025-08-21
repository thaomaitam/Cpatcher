// core/bridge/Xposed.java
package io.github.cpatcher.core.bridge;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class Xposed {
    
    public static Set<Unhook> hookAllMethods(Class<?> clazz, String methodName, MethodHookCallback callback) {
        Set<Unhook> unhooks = new HashSet<>();
        
        // Hook all methods with the given name
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                XC_MethodHook hook = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        callback.beforeHook(new HookParam(param));
                    }
                    
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        callback.afterHook(new HookParam(param));
                    }
                };
                
                XposedBridge.hookMethod(method, hook);
                unhooks.add(new UnhookImpl(hook, method));
            }
        }
        
        return unhooks;
    }
    
    public static Unhook hookMethod(Method method, MethodHookCallback callback) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                callback.beforeHook(new HookParam(param));
            }
            
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                callback.afterHook(new HookParam(param));
            }
        };
        
        XposedBridge.hookMethod(method, hook);
        return new UnhookImpl(hook, method);
    }
    
    public static Unhook hookConstructor(Constructor<?> constructor, MethodHookCallback callback) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                callback.beforeHook(new HookParam(param));
            }
            
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                callback.afterHook(new HookParam(param));
            }
        };
        
        XposedBridge.hookMethod(constructor, hook);
        return new UnhookImpl(hook, constructor);
    }
    
    public static Object getAdditionalInstanceField(Object obj, String key) {
        return XposedHelpers.getAdditionalInstanceField(obj, key);
    }
    
    public static Object setAdditionalInstanceField(Object obj, String key, Object value) {
        return XposedHelpers.setAdditionalInstanceField(obj, key, value);
    }
    
    public static void log(String message) {
        XposedBridge.log("[Cpatcher] " + message);
    }
    
    public static void log(String message, Throwable t) {
        XposedBridge.log("[Cpatcher] " + message);
        XposedBridge.log(t);
    }
    
    // Helper methods
    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return XposedHelpers.findMethodExact(clazz, methodName, parameterTypes);
    }
    
    public static Method findMethodBest(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return XposedHelpers.findMethodBestMatch(clazz, methodName, parameterTypes);
    }
    
    public static Constructor<?> findConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        return XposedHelpers.findConstructorExact(clazz, parameterTypes);
    }
}