package io.github.cpatcher.handlers.telegram

import io.github.cpatcher.arch.hookAllBefore
import io.github.cpatcher.arch.setObj

// 禁止询问联系人权限
class ContactPermission : DynHook() {
    override fun isFeatureEnabled(): Boolean = TelegramHandler.settings.contactPermission

    override fun onHook() {
        findClass("org.telegram.ui.ContactsActivity").hookAllBefore(
            "onResume",
            cond = ::isEnabled
        ) { param ->
            param.thisObject.setObj("checkPermission", false)
        }
    }
}
