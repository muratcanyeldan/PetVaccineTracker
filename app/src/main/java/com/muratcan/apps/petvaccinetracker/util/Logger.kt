package com.muratcan.apps.petvaccinetracker.util

import android.util.Log
import java.util.Locale

object Logger {
    fun info(tag: String, message: String) {
        Log.i(tag, String.format(Locale.getDefault(), "[%s] %s", getCallerInfo(), message))
    }

    fun error(tag: String, message: String) {
        Log.e(tag, String.format(Locale.getDefault(), "[%s] %s", getCallerInfo(), message))
    }

    fun error(tag: String, message: String, throwable: Throwable) {
        Log.e(
            tag,
            String.format(Locale.getDefault(), "[%s] %s", getCallerInfo(), message),
            throwable
        )
    }

    private fun getCallerInfo(): String {
        val stackTrace = Thread.currentThread().stackTrace
        return if (stackTrace.size >= 4) {
            val caller =
                stackTrace[3] // 0 is getStackTrace, 1 is getCallerInfo, 2 is log method, 3 is caller
            String.format(
                Locale.getDefault(), "%s.%s:%d",
                caller.className.substring(caller.className.lastIndexOf('.') + 1),
                caller.methodName,
                caller.lineNumber
            )
        } else {
            "Unknown"
        }
    }
} 