package com.muratcan.apps.petvaccinetracker.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationSettingsHelper {
    private static final String PREFS_NAME = "notification_settings";
    private static final String KEY_REMINDER_DAYS = "reminder_days";
    private static final String KEY_NOTIFICATION_TIME_HOUR = "notification_hour";
    private static final String KEY_NOTIFICATION_TIME_MINUTE = "notification_minute";

    // Default reminder days: 7, 3, 1, 0 days before
    private static final String[] DEFAULT_REMINDER_DAYS = {"7", "3", "1", "0"};
    private static final int DEFAULT_HOUR = 9;
    private static final int DEFAULT_MINUTE = 0;

    private final SharedPreferences prefs;

    public NotificationSettingsHelper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Get reminder days selected by user
    public int[] getReminderDays() {
        Set<String> defaultSet = new HashSet<>(Arrays.asList(DEFAULT_REMINDER_DAYS));

        Set<String> reminderDaysSet = prefs.getStringSet(KEY_REMINDER_DAYS, defaultSet);
        List<Integer> reminderDaysList = new ArrayList<>();

        for (String dayStr : reminderDaysSet) {
            try {
                reminderDaysList.add(Integer.parseInt(dayStr));
            } catch (NumberFormatException e) {
                // Skip invalid entries
            }
        }

        // Convert to array and sort in descending order (7, 3, 1, 0)
        return reminderDaysList.stream()
                .mapToInt(Integer::intValue)
                .sorted()
                .toArray();
    }

    // Save reminder days selected by user
    public void setReminderDays(int[] reminderDays) {
        Set<String> reminderDaysSet = new HashSet<>();
        for (int day : reminderDays) {
            reminderDaysSet.add(String.valueOf(day));
        }

        prefs.edit()
                .putStringSet(KEY_REMINDER_DAYS, reminderDaysSet)
                .apply();
    }

    // Get notification time (hour)
    public int getNotificationHour() {
        return prefs.getInt(KEY_NOTIFICATION_TIME_HOUR, DEFAULT_HOUR);
    }

    // Get notification time (minute)
    public int getNotificationMinute() {
        return prefs.getInt(KEY_NOTIFICATION_TIME_MINUTE, DEFAULT_MINUTE);
    }

    // Save notification time
    public void setNotificationTime(int hour, int minute) {
        prefs.edit()
                .putInt(KEY_NOTIFICATION_TIME_HOUR, hour)
                .putInt(KEY_NOTIFICATION_TIME_MINUTE, minute)
                .apply();
    }

    // Check if specific reminder day is enabled
    public boolean isReminderDayEnabled(int day) {
        int[] enabledDays = getReminderDays();
        for (int enabledDay : enabledDays) {
            if (enabledDay == day) {
                return true;
            }
        }
        return false;
    }

    // Get all available reminder day options
    public static int[] getAllAvailableReminderDays() {
        return new int[]{14, 7, 3, 1, 0}; // 2 weeks, 1 week, 3 days, 1 day, due date
    }

    // Get display text for reminder days
    public static String getReminderDayDisplayText(Context context, int days) {
        if (days == 0) {
            return "On due date";
        } else if (days == 1) {
            return "1 day before";
        } else if (days == 7) {
            return "1 week before";
        } else if (days == 14) {
            return "2 weeks before";
        } else {
            return days + " days before";
        }
    }
}