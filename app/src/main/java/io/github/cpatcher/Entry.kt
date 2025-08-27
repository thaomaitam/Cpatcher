package io.github.cpatcher

import android.content.res.XModuleResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.github.cpatcher.bridge.LoadPackageParam
import io.github.cpatcher.handlers.QslockHandler
import io.github.cpatcher.handlers.TermuxHandler

class Entry : IXposedHookLoadPackage, IXposedHookZygoteInit {
    companion object {
        lateinit var modulePath: String
        val moduleRes: XModuleResources by lazy {
            XModuleResources.createInstance(
                modulePath,
                null
            )
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        logI("Cpatcher: ${lpparam.packageName} ${lpparam.processName}")
        val handler = when (lpparam.packageName) {
            "com.termux" -> TermuxHandler()
            "com.android.systemui" -> QslockHandler()
        else -> return
        }
        logPrefix = "[${handler.javaClass.simpleName}] "
        handler.hook(LoadPackageParam(lpparam))
    }
}