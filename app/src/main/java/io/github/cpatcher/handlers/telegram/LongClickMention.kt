package io.github.cpatcher.handlers.telegram

import android.text.SpannableString
import android.text.Spanned
import io.github.cpatcher.arch.call
import io.github.cpatcher.arch.callS
import io.github.cpatcher.arch.getObj
import io.github.cpatcher.arch.hookAllAfter
import io.github.cpatcher.arch.newInst
import io.github.cpatcher.logE
import java.lang.reflect.Proxy

// 在 at 列表中，长按以强制使用无用户名的 at 形式
class LongClickMention : DynHook() {
    override fun isFeatureEnabled(): Boolean = TelegramHandler.settings.longClickMention

    override fun onHook() {
        val longClickListenerClass =
            findClass("org.telegram.ui.Components.RecyclerListView\$OnItemLongClickListener")
        val tlUser = findClass("org.telegram.tgnet.TLRPC\$TL_user")
        val userObjectClass = findClass("org.telegram.messenger.UserObject")
        val classURLSpanUserMention = findClass("org.telegram.ui.Components.URLSpanUserMention")
        findClass("org.telegram.ui.ChatActivity").hookAllAfter(
            "createView",
            cond = ::isEnabled
        ) { param ->
            val obj = Object()
            val thiz = param.thisObject
            val mentionContainer = thiz.getObj("mentionContainer")
            val listView = mentionContainer.call("getListView") // RecyclerListView
            val proxy = Proxy.newProxyInstance(
                classLoader, arrayOf(longClickListenerClass)
            ) { _, method, args ->
                if (method.name == "onItemClick") {
                    runCatching {
                        var position = args[1] as Int
                        if (position == 0) return@newProxyInstance false
                        position--
                        val adapter = mentionContainer.call("getAdapter")
                        val item = adapter.call("getItem", position)
                        if (!tlUser.isInstance(item)) return@newProxyInstance false
                        val start = adapter.call("getResultStartPosition")
                        val len = adapter.call("getResultLength")
                        val name = userObjectClass.callS(
                            "getFirstName",
                            item,
                            false
                        )
                        val spannable = SpannableString("$name ")
                        val span = classURLSpanUserMention.newInst(
                            item.getObj("id").toString(),
                            3
                        )
                        spannable.setSpan(
                            span,
                            0,
                            spannable.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        val chatActivityEnterView =
                            thiz.getObj("chatActivityEnterView")
                        chatActivityEnterView.call(
                            "replaceWithText",
                            start,
                            len,
                            spannable,
                            false
                        )
                        return@newProxyInstance true
                    }.onFailure { logE("onItemLongClicked: error", it) }
                    return@newProxyInstance false
                }
                return@newProxyInstance method.invoke(obj, args)
            }
            listView.call("setOnItemLongClickListener", proxy)
        }
    }
}
