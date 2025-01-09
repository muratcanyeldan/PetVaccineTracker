package com.muratcan.apps.petvaccinetracker.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.muratcan.apps.petvaccinetracker.R;

import java.util.Locale;

public class VaccineNotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "VaccineNotifications";
    private static final String CHANNEL_NAME = "Vaccine Due Notifications";
    private static final String TAG = "VaccineNotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String petName = intent.getStringExtra("petName");
            String vaccineName = intent.getStringExtra("vaccineName");
            int notificationId = intent.getIntExtra("notificationId", 0);

            if (petName == null || vaccineName == null) {
                Logger.error(TAG, "Missing required notification data");
                return;
            }

            createNotificationChannel(context);

            String title = "Vaccine Due Today";
            String message = String.format(Locale.getDefault(), 
                "%s's %s vaccine is due today!", petName, vaccineName);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager != null) {
                notificationManager.notify(notificationId, builder.build());
                Logger.info(TAG, String.format(Locale.getDefault(),
                    "Showed notification for pet: %s, vaccine: %s", petName, vaccineName));
            }
        } catch (Exception e) {
            Logger.error(TAG, "Failed to show notification", e);
        }
    }

    private void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        );
        
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }
} 