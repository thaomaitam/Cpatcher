// core/bridge/Unhook.kt
package io.github.cpatcher.core.bridge

interface Unhook {
    fun unhook()
}

internal class UnhookImpl(
    private val hook: de.robv.android.xposed.XC_MethodHook,
    private val member: java.lang.reflect.Member
) : Unhook {
    override fun unhook() {
        de.robv.android.xposed.XposedBridge.unhookMethod(member, hook)
    }
}