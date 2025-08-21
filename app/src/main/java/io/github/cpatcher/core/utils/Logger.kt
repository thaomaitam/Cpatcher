// core/utils/Logger.kt
package io.github.cpatcher.core.utils

import android.util.Log

object Logger {
    private const val TAG = "Cpatcher"
    
    fun d(msg: String, t: Throwable? = null) {
        Log.d(TAG, msg, t)
    }
    
    fun i(msg: String, t: Throwable? = null) {
        Log.i(TAG, msg, t)
    }
    
    fun w(msg: String, t: Throwable? = null) {
        Log.w(TAG, msg, t)
    }
    
    fun e(msg: String, t: Throwable? = null) {
        Log.e(TAG, msg, t)
    }
}