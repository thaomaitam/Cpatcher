package io.github.cpatcher.handlers

import android.app.Activity
import android.view.View
import io.github.cpatcher.arch.IHook
import io.github.cpatcher.arch.getObjAs
import io.github.cpatcher.arch.hookAllAfter
import io.github.cpatcher.arch.hookAllNop
import io.github.cpatcher.arch.hookBefore

class TermuxHandler : IHook() {
    override fun onHook() {
        if (loadPackageParam.packageName != "com.termux") return
        val mainActivity = findClass("com.termux.app.TermuxActivity")
        Activity::class.java.hookBefore("finish") { param ->
            if (param.thisObject.javaClass == mainActivity) {
                (param.thisObject as Activity).finishAndRemoveTask()
                param.result = null
            }
        }

        mainActivity.hookAllAfter("onCreate") { param ->
            param.thisObject.getObjAs<View>("mTerminalView").setAutofillHints(null)
        }
        // fix termux view infinite update loop
        // https://github.com/termux/termux-app/blob/2f40df91e54662190befe3b981595209944348e8/app/src/main/java/com/termux/app/terminal/TermuxActivityRootView.java#L120
        findClass("com.termux.app.terminal.TermuxActivityRootView").hookAllNop("onGlobalLayout")
    }
}
