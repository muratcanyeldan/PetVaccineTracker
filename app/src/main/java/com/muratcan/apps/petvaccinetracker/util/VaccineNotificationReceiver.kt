package com.muratcan.apps.petvaccinetracker.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.muratcan.apps.petvaccinetracker.PetDetailActivity
import com.muratcan.apps.petvaccinetracker.R
import timber.log.Timber

class VaccineNotificationReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "VaccineNotifications"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val petName = intent.getStringExtra("petName")
        val vaccineName = intent.getStringExtra("vaccineName")
        val petId = intent.getLongExtra("petId", -1)
        val vaccineId = intent.getLongExtra("vaccineId", -1)
        val daysRemaining = intent.getIntExtra("daysRemaining", -1)

        if (petName == null || vaccineName == null || petId == -1L || vaccineId == -1L || daysRemaining == -1) {
            Timber.e("Invalid notification data received")
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Main action - open pet detail
        val openPetIntent = Intent(context, PetDetailActivity::class.java).apply {
            putExtra("petId", petId)
            putExtra("vaccineId", vaccineId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openPetPendingIntent = PendingIntent.getActivity(
            context, 0, openPetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create different notification content based on days remaining
        val notificationTitle = getNotificationTitle(context, daysRemaining)
        val notificationText = getNotificationText(context, petName, vaccineName, daysRemaining)

        // Use unique notification ID so multiple notifications can exist
        val notificationId = (vaccineId * 1000 + daysRemaining).toInt()

        // Create action buttons
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1, // Unique ID for mark done action
            VaccineActionReceiver.createMarkDoneIntent(
                context,
                vaccineId,
                petId,
                notificationId,
                petName,
                vaccineName
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val postponePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2, // Unique ID for postpone action
            VaccineActionReceiver.createPostponeIntent(
                context,
                vaccineId,
                petId,
                notificationId,
                petName,
                vaccineName
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPetPendingIntent)
            // Add action buttons
            .addAction(
                R.drawable.ic_check,
                context.getString(R.string.mark_done),
                markDonePendingIntent
            )
            .addAction(
                R.drawable.ic_schedule,
                context.getString(R.string.postpone),
                postponePendingIntent
            )

        notificationManager.notify(notificationId, builder.build())

        Timber.d(
            "Notification with actions sent for pet %s, vaccine %s, %d days remaining",
            petName,
            vaccineName,
            daysRemaining
        )
    }

    private fun getNotificationTitle(context: Context, daysRemaining: Int): String {
        return when (daysRemaining) {
            0 -> context.getString(R.string.vaccine_due_today_title)
            1 -> context.getString(R.string.vaccine_due_tomorrow_title)
            else -> context.getString(R.string.vaccine_due_soon_title)
        }
    }

    private fun getNotificationText(
        context: Context,
        petName: String,
        vaccineName: String,
        daysRemaining: Int
    ): String {
        return when (daysRemaining) {
            0 -> String.format(
                context.getString(R.string.vaccine_due_today_text),
                petName,
                vaccineName
            )

            1 -> String.format(
                context.getString(R.string.vaccine_due_tomorrow_text),
                petName,
                vaccineName
            )

            else -> String.format(
                context.getString(R.string.vaccine_due_in_days_text),
                petName,
                vaccineName,
                daysRemaining
            )
        }
    }
} 