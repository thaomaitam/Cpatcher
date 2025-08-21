// core/arch/HookUtils.kt (Updated)
package io.github.cpatcher.core.arch

import io.github.cpatcher.core.bridge.HookParam
import io.github.cpatcher.core.bridge.MethodHookCallback
import io.github.cpatcher.core.bridge.Unhook
import io.github.cpatcher.core.bridge.Xposed

typealias HookCallback = (HookParam) -> Unit

fun Class<*>.hookAllAfter(
    name: String,
    cond: () -> Boolean = { true },
    fn: HookCallback
): MutableSet<Unhook> = Xposed.hookAllMethods(this, name, object : MethodHookCallback() {
    override fun afterHook(param: HookParam) {
        if (cond()) {
            fn(param)
            param.syncResult() // Sync any result changes
        }
    }
})

fun Class<*>.hookAllBefore(
    name: String,
    cond: () -> Boolean = { true },
    fn: HookCallback  
): MutableSet<Unhook> = Xposed.hookAllMethods(this, name, object : MethodHookCallback() {
    override fun beforeHook(param: HookParam) {
        if (cond()) {
            fn(param)
            param.syncResult() // Sync any result changes
        }
    }
})

fun Class<*>.hookAllReplace(
    name: String,
    cond: () -> Boolean = { true },
    replacement: (HookParam) -> Any?
): MutableSet<Unhook> = Xposed.hookAllMethods(this, name, object : MethodHookCallback() {
    override fun beforeHook(param: HookParam) {
        if (cond()) {
            try {
                param.result = replacement(param)
                param.syncResult()
            } catch (t: Throwable) {
                param.throwable = t
                param.syncResult()
            }
        }
    }
})

fun Class<*>.hookAllNop(name: String): MutableSet<Unhook> = 
    hookAllReplace(name) { null }