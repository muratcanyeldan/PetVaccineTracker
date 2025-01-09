package com.muratcan.apps.petvaccinetracker.util;

import android.util.Log;

import java.util.Locale;

public class Logger {
    public static void info(String tag, String message) {
        Log.i(tag, String.format(Locale.getDefault(), "[%s] %s", getCallerInfo(), message));
    }

    public static void error(String tag, String message) {
        Log.e(tag, String.format(Locale.getDefault(), "[%s] %s", getCallerInfo(), message));
    }

    public static void error(String tag, String message, Throwable throwable) {
        Log.e(tag, String.format(Locale.getDefault(), "[%s] %s", getCallerInfo(), message), throwable);
    }

    private static String getCallerInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length >= 4) {
            StackTraceElement caller = stackTrace[3]; // 0 is getStackTrace, 1 is getCallerInfo, 2 is log method, 3 is caller
            return String.format(Locale.getDefault(), "%s.%s:%d", 
                caller.getClassName().substring(caller.getClassName().lastIndexOf('.') + 1),
                caller.getMethodName(),
                caller.getLineNumber());
        }
        return "Unknown";
    }
} 