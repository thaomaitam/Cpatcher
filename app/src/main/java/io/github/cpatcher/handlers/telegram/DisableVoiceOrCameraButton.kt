package io.github.cpatcher.handlers.telegram

import io.github.cpatcher.arch.getObj
import io.github.cpatcher.arch.hookAllCAfter
import io.github.cpatcher.arch.hookAllConstantIf
import java.util.concurrent.atomic.AtomicBoolean

// 禁用音频 / 摄像头按钮，防止误触
class DisableVoiceOrCameraButton : DynHook() {
    override fun isFeatureEnabled(): Boolean = TelegramHandler.settings.disableVoiceOrCameraButton

    override fun onHook() {
        val subHookFound = AtomicBoolean(false)
        findClass("org.telegram.ui.Components.ChatActivityEnterView").hookAllCAfter { param ->
            if (!isEnabled()) return@hookAllCAfter
            if (subHookFound.get()) return@hookAllCAfter
            val audioVideoButtonContainer =
                param.thisObject.getObj("audioVideoButtonContainer") ?: return@hookAllCAfter
            audioVideoButtonContainer.javaClass.hookAllConstantIf("onTouchEvent", true) {
                isEnabled()
            }
            subHookFound.set(true)
        }
    }
}
