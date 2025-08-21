package io.github.cpatcher.arch

import io.github.cpatcher.bridge.Xposed
import kotlin.reflect.KProperty

class ExtraField<T>(private val bound: Any, private val name: String, private val defValue: T) {
    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        // It should be a `T?`
        return Xposed.getAdditionalInstanceField(bound, name) as T? ?: defValue
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        Xposed.setAdditionalInstanceField(bound, name, value)
    }
}

fun <T> extraField(bound: Any, name: String, def: T): ExtraField<T> = ExtraField(bound, name, def)
