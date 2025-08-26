package io.github.cpatcher.handlers.telegram

import android.view.View
import io.github.cpatcher.arch.call
import io.github.cpatcher.arch.getObj
import io.github.cpatcher.arch.hookAllAfter

// 添加联系人时自动取消勾选分享手机号码（原行为是默认勾选）
class AutoUncheckSharePhoneNumber : DynHook() {
    override fun isFeatureEnabled(): Boolean = TelegramHandler.settings.autoUncheckSharePhoneNumber

    override fun onHook() {
        findClass("org.telegram.ui.ContactAddActivity").hookAllAfter(
            "createView",
            cond = ::isEnabled
        ) { param ->
            val checkBox = param.thisObject.getObj("checkBoxCell") as? View ?: return@hookAllAfter
            if (checkBox.call("isChecked") == true) {
                checkBox.performClick()
            }
        }
    }
}
