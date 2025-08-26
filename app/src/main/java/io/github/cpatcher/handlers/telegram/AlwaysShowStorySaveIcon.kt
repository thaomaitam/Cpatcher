package io.github.cpatcher.handlers.telegram

import io.github.cpatcher.arch.hookAllAfter
import io.github.cpatcher.arch.setObj

class AlwaysShowStorySaveIcon : DynHook() {
    override fun isFeatureEnabled(): Boolean = TelegramHandler.settings.alwaysShowStorySaveIcon
    override fun onHook() {
        findClass("org.telegram.ui.Stories.PeerStoriesView").hookAllAfter(
            "updatePosition",
            cond = ::isEnabled
        ) { param ->
            param.thisObject.setObj("allowShare", true)
            param.thisObject.setObj("allowRepost", true)
            param.thisObject.setObj("allowShareLink", true)
        }
    }
}
