package io.github.cpatcher.handlers.telegram

import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import io.github.cpatcher.Entry
import io.github.cpatcher.R
import io.github.cpatcher.arch.getObj
import io.github.cpatcher.arch.getObjAs
import io.github.cpatcher.arch.hookAllAfter

// 标记双向联系人（↑↓图标）
class MutualContact : DynHook() {
    override fun isFeatureEnabled(): Boolean = TelegramHandler.settings.mutualContact

    @Suppress("DEPRECATION")
    override fun onHook() {
        val drawable = Entry.moduleRes.getDrawable(R.drawable.ic_mutual_contact)
        val tlUser = findClass("org.telegram.tgnet.TLRPC\$TL_user")
        findClass("org.telegram.ui.Cells.UserCell").hookAllAfter(
            "update",
            cond = ::isEnabled
        ) { param ->
            val d = param.thisObject.getObjAs<Int>("currentDrawable")
            if (d != 0) {
                return@hookAllAfter
            }
            val current = param.thisObject.getObj("currentObject")
            if (!tlUser.isInstance(current)) return@hookAllAfter
            val imageView = param.thisObject.getObjAs<ImageView>("imageView")
            val mutual = current.getObjAs<Boolean>("mutual_contact")
            if (mutual) {
                imageView.setImageDrawable(drawable)
                imageView.visibility = View.VISIBLE
                (imageView.layoutParams as FrameLayout.LayoutParams).apply {
                    val resource = imageView.context.resources
                    gravity =
                        (gravity and Gravity.HORIZONTAL_GRAVITY_MASK.inv()) or Gravity.RIGHT
                    rightMargin =
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            8f,
                            resource.displayMetrics
                        ).toInt()
                    leftMargin
                }
            }
        }
    }
}
