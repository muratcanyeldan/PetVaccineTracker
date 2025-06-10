package com.muratcan.apps.petvaccinetracker.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import com.muratcan.apps.petvaccinetracker.widget.WidgetUpdateService;

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

    public static void scheduleNotification(Context context, String petName, String vaccineName, String dueDate, long petId, long vaccineId) {
        // Get user's preferred reminder days from settings
        NotificationSettingsHelper settingsHelper = new NotificationSettingsHelper(context);
        int[] reminderDays = settingsHelper.getReminderDays(); // USER'S CUSTOM SETTINGS!

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

            Calendar dueDateCalendar = Calendar.getInstance();
            dueDateCalendar.setTime(date);
            dueDateCalendar.set(Calendar.HOUR_OF_DAY, settingsHelper.getNotificationHour()); // USER'S PREFERRED TIME
            dueDateCalendar.set(Calendar.MINUTE, settingsHelper.getNotificationMinute());     // USER'S PREFERRED TIME
            dueDateCalendar.set(Calendar.SECOND, 0);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Logger.error(TAG, "AlarmManager is null");
                return;
            }

            // Schedule notification for each reminder day
            for (int reminderDay : reminderDays) {
                Calendar notificationCalendar = (Calendar) dueDateCalendar.clone();
                notificationCalendar.add(Calendar.DAY_OF_MONTH, -reminderDay); // Subtract days from due date

                // Skip if notification time is in the past
                if (notificationCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    Logger.info(TAG, String.format("Skipping past notification for %d days before due date", reminderDay));
                    continue;
                }

                Intent intent = new Intent(context, VaccineNotificationReceiver.class);
                intent.putExtra("petName", petName);
                intent.putExtra("vaccineName", vaccineName);
                intent.putExtra("petId", petId);
                intent.putExtra("vaccineId", vaccineId);
                intent.putExtra("daysRemaining", reminderDay); // ADD THIS: tells receiver how many days remaining

                // Create unique ID for each notification (vaccine + reminder day)
                int notificationId = (int) (vaccineId * 1000 + reminderDay); // Unique ID per vaccine per reminder
                intent.putExtra("notificationId", notificationId);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        notificationId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                try {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            notificationCalendar.getTimeInMillis(),
                            pendingIntent
                    );

                    String reminderText = reminderDay == 0 ? "due today" : reminderDay + " days before due date";
                    Logger.info(TAG, String.format(Locale.getDefault(),
                            "Scheduled notification for pet: %s, vaccine: %s, %s at %s",
                            petName, vaccineName, reminderText,
                            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(notificationCalendar.getTime())));

                } catch (SecurityException e) {
                    Logger.error(TAG, "SecurityException when scheduling exact alarm - permission might have been revoked", e);
                }
            }
        } catch (Exception e) {
            Logger.error(TAG, "Failed to schedule notification", e);
        }

        // Update widgets after scheduling
        updateWidgets(context);
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

    public static void updateWidgets(Context context) {
        Intent updateIntent = WidgetUpdateService.createUpdateIntent(context);
        context.startService(updateIntent);
    }
} 