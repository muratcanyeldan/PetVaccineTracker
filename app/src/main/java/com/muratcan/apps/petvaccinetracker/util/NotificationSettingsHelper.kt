package com.muratcan.apps.petvaccinetracker.util

import android.content.Context
import android.content.SharedPreferences

class NotificationSettingsHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "notification_settings"
        private const val KEY_REMINDER_DAYS = "reminder_days"
        private const val KEY_NOTIFICATION_TIME_HOUR = "notification_hour"
        private const val KEY_NOTIFICATION_TIME_MINUTE = "notification_minute"

        // Default reminder days: 7, 3, 1, 0 days before
        private val DEFAULT_REMINDER_DAYS = setOf("7", "3", "1", "0")
        private const val DEFAULT_HOUR = 9
        private const val DEFAULT_MINUTE = 0

        // Get all available reminder day options
        fun getAllAvailableReminderDays(): IntArray {
            return intArrayOf(14, 7, 3, 1, 0) // 2 weeks, 1 week, 3 days, 1 day, due date
        }

        // Get display text for reminder days
        fun getReminderDayDisplayText(context: Context, days: Int): String {
            return when (days) {
                0 -> "On due date"
                1 -> "1 day before"
                7 -> "1 week before"
                14 -> "2 weeks before"
                else -> "$days days before"
            }
        }
    }

    // Get reminder days selected by user
    fun getReminderDays(): IntArray {
        val reminderDaysSet =
            prefs.getStringSet(KEY_REMINDER_DAYS, DEFAULT_REMINDER_DAYS) ?: DEFAULT_REMINDER_DAYS
        val reminderDaysList = mutableListOf<Int>()

        for (dayStr in reminderDaysSet) {
            try {
                reminderDaysList.add(dayStr.toInt())
            } catch (e: NumberFormatException) {
                // Skip invalid entries
            }
        }

        // Convert to array and sort in ascending order (0, 1, 3, 7)
        return reminderDaysList.sorted().toIntArray()
    }

    // Save reminder days selected by user
    fun setReminderDays(reminderDays: IntArray) {
        val reminderDaysSet = reminderDays.map { it.toString() }.toSet()

        prefs.edit()
            .putStringSet(KEY_REMINDER_DAYS, reminderDaysSet)
            .apply()
    }

    // Get notification time (hour)
    fun getNotificationHour(): Int {
        return prefs.getInt(KEY_NOTIFICATION_TIME_HOUR, DEFAULT_HOUR)
    }

    // Get notification time (minute)
    fun getNotificationMinute(): Int {
        return prefs.getInt(KEY_NOTIFICATION_TIME_MINUTE, DEFAULT_MINUTE)
    }

    // Save notification time
    fun setNotificationTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_NOTIFICATION_TIME_HOUR, hour)
            .putInt(KEY_NOTIFICATION_TIME_MINUTE, minute)
            .apply()
    }

    // Check if specific reminder day is enabled
    fun isReminderDayEnabled(day: Int): Boolean {
        val enabledDays = getReminderDays()
        return enabledDays.contains(day)
    }
} 