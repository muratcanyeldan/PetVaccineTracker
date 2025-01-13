package com.muratcan.apps.petvaccinetracker.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.muratcan.apps.petvaccinetracker.PetDetailActivity;
import com.muratcan.apps.petvaccinetracker.R;

import java.util.Locale;

import timber.log.Timber;

public class VaccineNotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "VaccineNotifications";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        String petName = intent.getStringExtra("petName");
        String vaccineName = intent.getStringExtra("vaccineName");
        long petId = intent.getLongExtra("petId", -1);
        long vaccineId = intent.getLongExtra("vaccineId", -1);

        if (petName == null || vaccineName == null || petId == -1 || vaccineId == -1) {
            Timber.e("Invalid notification data received");
            return;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent openPetIntent = new Intent(context, PetDetailActivity.class);
        openPetIntent.putExtra("petId", petId);
        openPetIntent.putExtra("vaccineId", vaccineId);
        openPetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openPetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.vaccine_due_notification_title))
                .setContentText(String.format(context.getString(R.string.vaccine_due_notification_text), petName, vaccineName))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) vaccineId, builder.build());
        Timber.d("Notification sent for pet %s, vaccine %s", petName, vaccineName);
    }
} 