package io.github.cpatcher.handlers.telegram

import io.github.cpatcher.arch.call
import io.github.cpatcher.arch.hookAllBefore
import io.github.cpatcher.arch.hookAllCBefore
import io.github.cpatcher.arch.setObj

class RemoveArchiveFolder : DynHook() {
    override fun isFeatureEnabled(): Boolean = TelegramHandler.settings.removeArchiveFolder
    override fun onHook() {
        findClass("org.telegram.messenger.MessagesController").hookAllBefore(
            "getDialogs",
            cond = ::isEnabled
        ) { param ->
            param.thisObject.call("removeFolder", 1)
        }
        val specialPackageNames = listOf(
            "com.exteragram.messenger",
            "com.radolyn.ayugram",
        )
        if (loadPackageParam.packageName in specialPackageNames) {
            findClass("com.exteragram.messenger.utils.ChatUtils").hookAllBefore(
                "hasArchivedChats",
                cond = ::isEnabled
            ) { param ->
                param.result = true
            }
            findClass("com.exteragram.messenger.ExteraConfig").hookAllCBefore(
                cond = ::isEnabled
            ) { param ->
                param.thisObject.setObj("archivedChats", true)
            }
        }
    }
}
