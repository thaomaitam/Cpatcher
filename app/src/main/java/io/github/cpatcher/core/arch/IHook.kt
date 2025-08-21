// core/arch/IHook.kt
package io.github.cpatcher.core.arch

import io.github.cpatcher.core.bridge.LoadPackageParam
import io.github.cpatcher.core.utils.Logger

abstract class IHook {
    lateinit var classLoader: ClassLoader
        private set
    lateinit var loadPackageParam: LoadPackageParam
        private set

    open fun hook(param: LoadPackageParam) {
        loadPackageParam = param
        classLoader = param.classLoader
        try {
            onHook()
        } catch (t: Throwable) {
            Logger.e("Hook failed: ${this.javaClass.simpleName}", t)
        }
    }

    fun subHook(hook: IHook) {
        hook.hook(loadPackageParam)
    }

    protected fun findClass(name: String): Class<*> = classLoader.loadClass(name)
    protected fun findClassOrNull(name: String): Class<*>? = try {
        classLoader.loadClass(name)
    } catch (e: ClassNotFoundException) {
        null
    }

    protected abstract fun onHook()
}