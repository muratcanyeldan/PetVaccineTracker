package com.muratcan.apps.petvaccinetracker.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";

    public static boolean needsExactAlarmPermission(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager == null || !alarmManager.canScheduleExactAlarms();
        }
        return false;
    }

    public static Intent getExactAlarmSettingsIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return null;
    }

    public static void scheduleNotification(Context context, String petName, String vaccineName, String dueDate) {
        try {
            if (needsExactAlarmPermission(context)) {
                Logger.error(TAG, "Cannot schedule exact alarms - permission not granted");
                return;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = dateFormat.parse(dueDate);
            if (date == null) {
                Logger.error(TAG, "Failed to parse due date: " + dueDate);
                return;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.HOUR_OF_DAY, 9); // Set notification time to 9 AM
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            Intent intent = new Intent(context, VaccineNotificationReceiver.class);
            intent.putExtra("petName", petName);
            intent.putExtra("vaccineName", vaccineName);

            // Create a unique ID for each notification based on pet name and vaccine name
            int notificationId = (petName + vaccineName).hashCode();
            intent.putExtra("notificationId", notificationId);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                    );
                    Logger.info(TAG, String.format(Locale.getDefault(),
                        "Scheduled notification for pet: %s, vaccine: %s, due: %s",
                        petName, vaccineName, dueDate));
                } catch (SecurityException e) {
                    Logger.error(TAG, "SecurityException when scheduling exact alarm - permission might have been revoked", e);
                }
            }
        } catch (Exception e) {
            Logger.error(TAG, "Failed to schedule notification", e);
        }
    }

    public static void cancelNotificationsForPet(Context context, String petName) {
        try {
            // Since we can't know all vaccine names, we'll need to cancel any pending intents
            // that match the pet name pattern. This is a bit of a workaround.
            for (int i = 0; i < 100; i++) { // Arbitrary limit to prevent infinite loop
                Intent intent = new Intent(context, VaccineNotificationReceiver.class);
                intent.putExtra("petName", petName);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    i,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                );

                if (pendingIntent != null) {
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    if (alarmManager != null) {
                        try {
                            alarmManager.cancel(pendingIntent);
                            pendingIntent.cancel();
                        } catch (SecurityException e) {
                            Logger.error(TAG, "SecurityException when canceling alarm - permission might have been revoked", e);
                        }
                    }
                }
            }
            Logger.info(TAG, String.format(Locale.getDefault(),
                "Cancelled all notifications for pet: %s", petName));
        } catch (Exception e) {
            Logger.error(TAG, String.format(Locale.getDefault(),
                "Failed to cancel notifications for pet: %s", petName), e);
        }
    }
} 