package com.example.utils

import android.util.Log

object AppLogger {
    private const val TAG_PREFIX = "WifiTools"

    fun d(tag: String, message: String) {
        runCatching { Log.d("$TAG_PREFIX/$tag", message) }
    }

    fun i(tag: String, message: String) {
        runCatching { Log.i("$TAG_PREFIX/$tag", message) }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable != null) Log.w("$TAG_PREFIX/$tag", message, throwable)
            else Log.w("$TAG_PREFIX/$tag", message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable != null) Log.e("$TAG_PREFIX/$tag", message, throwable)
            else Log.e("$TAG_PREFIX/$tag", message)
        }
    }
}
