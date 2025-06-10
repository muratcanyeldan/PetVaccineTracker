package com.muratcan.apps.petvaccinetracker.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.muratcan.apps.petvaccinetracker.PetDetailActivity;
import com.muratcan.apps.petvaccinetracker.R;

import timber.log.Timber;

public class VaccineNotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "VaccineNotifications";

    @Override
    public void onReceive(Context context, Intent intent) {
        String petName = intent.getStringExtra("petName");
        String vaccineName = intent.getStringExtra("vaccineName");
        long petId = intent.getLongExtra("petId", -1);
        long vaccineId = intent.getLongExtra("vaccineId", -1);
        int daysRemaining = intent.getIntExtra("daysRemaining", -1);

        if (petName == null || vaccineName == null || petId == -1 || vaccineId == -1 || daysRemaining == -1) {
            Timber.e("Invalid notification data received");
            return;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Main action - open pet detail
        Intent openPetIntent = new Intent(context, PetDetailActivity.class);
        openPetIntent.putExtra("petId", petId);
        openPetIntent.putExtra("vaccineId", vaccineId);
        openPetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent openPetPendingIntent = PendingIntent.getActivity(context, 0, openPetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create different notification content based on days remaining
        String notificationTitle = getNotificationTitle(context, daysRemaining);
        String notificationText = getNotificationText(context, petName, vaccineName, daysRemaining);

        // Use unique notification ID so multiple notifications can exist
        int notificationId = (int) (vaccineId * 1000 + daysRemaining);

        // Create action buttons
        PendingIntent markDonePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId * 10 + 1, // Unique ID for mark done action
                VaccineActionReceiver.createMarkDoneIntent(context, vaccineId, petId, notificationId, petName, vaccineName),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent postponePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId * 10 + 2, // Unique ID for postpone action
                VaccineActionReceiver.createPostponeIntent(context, vaccineId, petId, notificationId, petName, vaccineName),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openPetPendingIntent)
                // Add action buttons
                .addAction(R.drawable.ic_check, context.getString(R.string.mark_done), markDonePendingIntent)
                .addAction(R.drawable.ic_schedule, context.getString(R.string.postpone), postponePendingIntent);

        notificationManager.notify(notificationId, builder.build());

        Timber.d("Notification with actions sent for pet %s, vaccine %s, %d days remaining", petName, vaccineName, daysRemaining);
    }

    private String getNotificationTitle(Context context, int daysRemaining) {
        return switch (daysRemaining) {
            case 0 -> context.getString(R.string.vaccine_due_today_title);
            case 1 -> context.getString(R.string.vaccine_due_tomorrow_title);
            default -> context.getString(R.string.vaccine_due_soon_title);
        };
    }

    private String getNotificationText(Context context, String petName, String vaccineName, int daysRemaining) {
        return switch (daysRemaining) {
            case 0 ->
                    String.format(context.getString(R.string.vaccine_due_today_text), petName, vaccineName);
            case 1 ->
                    String.format(context.getString(R.string.vaccine_due_tomorrow_text), petName, vaccineName);
            default ->
                    String.format(context.getString(R.string.vaccine_due_in_days_text), petName, vaccineName, daysRemaining);
        };
    }
} 