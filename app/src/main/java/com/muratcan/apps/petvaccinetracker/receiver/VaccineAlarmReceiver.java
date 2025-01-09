package com.muratcan.apps.petvaccinetracker.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.muratcan.apps.petvaccinetracker.MainActivity;
import com.muratcan.apps.petvaccinetracker.R;

public class VaccineAlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "vaccine_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String petName = intent.getStringExtra("pet_name");
        String vaccineName = intent.getStringExtra("vaccine_name");
        long petId = intent.getLongExtra("pet_id", -1);

        // Create notification channel
        createNotificationChannel(context);

        // Create intent for notification click
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.vaccine_due_notification_title))
            .setContentText(context.getString(R.string.vaccine_due_notification_text, petName, vaccineName))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        // Show notification
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) petId, builder.build());
    }

    private void createNotificationChannel(Context context) {
        CharSequence name = context.getString(R.string.channel_name);
        String description = context.getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }
} 