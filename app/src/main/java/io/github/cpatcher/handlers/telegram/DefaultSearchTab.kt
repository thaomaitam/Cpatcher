package io.github.cpatcher.handlers.telegram

import io.github.cpatcher.arch.call
import io.github.cpatcher.arch.hook
import io.github.cpatcher.arch.hookBefore
import io.github.cpatcher.arch.setObj

// 强制在频道中点击 hash tag 时默认搜索本频道（原行为是搜索「全部帖子」）
class DefaultSearchTab : DynHook() {
    override fun isFeatureEnabled(): Boolean = TelegramHandler.settings.defaultSearchTab

    override fun onHook() {
        val chatActivity = findClass("org.telegram.ui.ChatActivity")
        val viewPagerFixedTabsView =
            findClass("org.telegram.ui.Components.ViewPagerFixed\$TabsView")
        val inOpenHashTagSearch = ThreadLocal<Boolean>()
        chatActivity.hook(
            "openHashtagSearch", String::class.java, java.lang.Boolean.TYPE,
            cond = ::isEnabled,
            before = { param ->
                inOpenHashTagSearch.set(true)
            },
            after = { param ->
                inOpenHashTagSearch.set(false)
                param.thisObject.setObj("defaultSearchPage", 0)
            }
        )
        viewPagerFixedTabsView.hookBefore(
            "scrollToTab",
            Integer.TYPE, Integer.TYPE
        ) { param ->
            if (inOpenHashTagSearch.get() == true) {
                if (param.thisObject.call("getCurrentPosition") != 0) {
                    param.args[0] = 0
                    param.args[1] = 0
                } else {
                    param.result = null
                }
            }
        }
    }
}
