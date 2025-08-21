package io.github.cpatcher

import android.util.Log
import de.robv.android.xposed.XposedBridge

// Giữ lại các hàm cũ của bạn, nhưng dùng XposedBridge.log sẽ tốt hơn cho module Xposed
var logPrefix: String = "[Cpatcher] "

fun logI(msg: String) {
    XposedBridge.log("$logPrefix$msg")
}

fun logE(msg: String, t: Throwable? = null) {
    XposedBridge.log("$logPrefix$msg")
    if (t != null) {
        XposedBridge.log(t)
    }
}

// THÊM ĐỐI TƯỢNG NÀY VÀO
object Logger {
    fun i(msg: String) {
        logI(msg)
    }

    fun e(msg: String, t: Throwable? = null) {
        logE(msg, t)
    }
}