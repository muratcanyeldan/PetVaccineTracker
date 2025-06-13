package com.muratcan.apps.petvaccinetracker.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.muratcan.apps.petvaccinetracker.database.AppDatabase
import com.muratcan.apps.petvaccinetracker.widget.WidgetUpdateService
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object NotificationHelper {
    private const val TAG = "NotificationHelper"

    fun needsExactAlarmPermission(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager == null || !alarmManager.canScheduleExactAlarms()
        } else {
            false
        }
    }

    fun getExactAlarmSettingsIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            null
        }
    }

    fun scheduleNotification(
        context: Context,
        petName: String,
        vaccineName: String,
        dueDate: String,
        petId: Long,
        vaccineId: Long
    ) {
        // Get user's preferred reminder days from settings
        val settingsHelper = NotificationSettingsHelper(context)
        val reminderDays = settingsHelper.getReminderDays() // USER'S CUSTOM SETTINGS!

        try {
            if (needsExactAlarmPermission(context)) {
                Timber.e(TAG, "Cannot schedule exact alarms - permission not granted")
                return
            }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = dateFormat.parse(dueDate)
            if (date == null) {
                Timber.e(TAG, "Failed to parse due date: $dueDate")
                return
            }

            val dueDateCalendar = Calendar.getInstance().apply {
                time = date
                set(
                    Calendar.HOUR_OF_DAY,
                    settingsHelper.getNotificationHour()
                ) // USER'S PREFERRED TIME
                set(
                    Calendar.MINUTE,
                    settingsHelper.getNotificationMinute()
                )     // USER'S PREFERRED TIME
                set(Calendar.SECOND, 0)
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager == null) {
                Timber.e(TAG, "AlarmManager is null")
                return
            }

            // Schedule notification for each reminder day
            for (reminderDay in reminderDays) {
                val notificationCalendar = dueDateCalendar.clone() as Calendar
                notificationCalendar.add(
                    Calendar.DAY_OF_MONTH,
                    -reminderDay
                ) // Subtract days from due date

                // Skip if notification time is in the past
                if (notificationCalendar.timeInMillis <= System.currentTimeMillis()) {
                    Timber.d(
                        TAG,
                        "Skipping past notification for $reminderDay days before due date"
                    )
                    continue
                }

                val intent = Intent(context, VaccineNotificationReceiver::class.java).apply {
                    putExtra("petName", petName)
                    putExtra("vaccineName", vaccineName)
                    putExtra("petId", petId)
                    putExtra("vaccineId", vaccineId)
                    putExtra(
                        "daysRemaining",
                        reminderDay
                    ) // ADD THIS: tells receiver how many days remaining
                }

                // Create unique ID for each notification (vaccine + reminder day)
                val notificationId =
                    (vaccineId * 1000 + reminderDay).toInt() // Unique ID per vaccine per reminder
                intent.putExtra("notificationId", notificationId)

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        notificationCalendar.timeInMillis,
                        pendingIntent
                    )

                    val reminderText =
                        if (reminderDay == 0) "due today" else "$reminderDay days before due date"
                    Timber.d(
                        TAG, String.format(
                            Locale.getDefault(),
                            "Scheduled notification for pet: %s, vaccine: %s, %s at %s",
                            petName, vaccineName, reminderText,
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(
                                notificationCalendar.time
                            )
                        )
                    )
                } catch (e: SecurityException) {
                    Timber.e(
                        TAG,
                        "SecurityException when scheduling exact alarm - permission might have been revoked",
                        e
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(TAG, "Failed to schedule notification", e)
        }

        // Update widgets after scheduling
        updateWidgets(context)
    }

    fun cancelNotificationsForPet(context: Context, petName: String) {
        try {
            // Since we can't know all vaccine names, we'll need to cancel any pending intents
            // that match the pet name pattern. This is a bit of a workaround.
            for (i in 0 until 100) { // Arbitrary limit to prevent infinite loop
                val intent = Intent(context, VaccineNotificationReceiver::class.java).apply {
                    putExtra("petName", petName)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    i,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )

                if (pendingIntent != null) {
                    val alarmManager =
                        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    alarmManager?.let {
                        try {
                            it.cancel(pendingIntent)
                            pendingIntent.cancel()
                        } catch (e: SecurityException) {
                            Timber.e(
                                TAG,
                                "SecurityException when canceling alarm - permission might have been revoked",
                                e
                            )
                        }
                    }
                }
            }
            Timber.d(TAG, "Cancelled all notifications for pet: $petName")
        } catch (e: Exception) {
            Timber.e(TAG, "Failed to cancel notifications for pet: $petName", e)
        }
    }

    fun cancelNotificationsForVaccine(context: Context, vaccineId: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager == null) {
                Timber.e("AlarmManager is null")
                return
            }

            // Cancel all reminder day notifications for this vaccine
            val allPossibleReminderDays = NotificationSettingsHelper.getAllAvailableReminderDays()

            for (reminderDay in allPossibleReminderDays) {
                val notificationId = (vaccineId * 1000 + reminderDay).toInt()

                val intent = Intent(context, VaccineNotificationReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )

                if (pendingIntent != null) {
                    try {
                        alarmManager.cancel(pendingIntent)
                        pendingIntent.cancel()
                    } catch (e: SecurityException) {
                        Timber.e(
                            e,
                            "SecurityException when canceling alarm - permission might have been revoked"
                        )
                    }
                }
            }

            Timber.d("Cancelled all notifications for vaccine ID: $vaccineId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel notifications for vaccine: $vaccineId")
        }
    }

    fun rescheduleAllExistingNotifications(context: Context) {
        Thread {
            try {
                Timber.d("Starting to reschedule all existing notifications with new settings")

                val database = AppDatabase.getDatabase(context)
                val futureVaccines = database.vaccineDao().getFutureVaccinesWithPetNames()

                Timber.d("Found ${futureVaccines.size} future vaccines to reschedule")

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                for (vaccineWithPet in futureVaccines) {
                    try {
                        // Cancel existing notifications for this vaccine
                        cancelNotificationsForVaccine(context, vaccineWithPet.vaccine.id)

                        // Reschedule with new settings
                        vaccineWithPet.vaccine.nextDueDate?.let { dueDate: Date ->
                            val dueDateString = dateFormat.format(dueDate)
                            scheduleNotification(
                                context,
                                vaccineWithPet.petName ?: "Unknown Pet",
                                vaccineWithPet.vaccine.name ?: "Unknown Vaccine",
                                dueDateString,
                                vaccineWithPet.vaccine.petId,
                                vaccineWithPet.vaccine.id
                            )

                            Timber.d("Rescheduled notifications for vaccine: ${vaccineWithPet.vaccine.name ?: "Unknown"} (Pet: ${vaccineWithPet.petName ?: "Unknown"})")
                        }
                    } catch (e: Exception) {
                        Timber.e(
                            e,
                            "Failed to reschedule vaccine: ${vaccineWithPet.vaccine.name ?: "Unknown"}"
                        )
                    }
                }

                Timber.d("Completed rescheduling all existing notifications")
            } catch (e: Exception) {
                Timber.e(e, "Failed to reschedule existing notifications")
            }
        }.start()
    }

    fun updateWidgets(context: Context) {
        WidgetUpdateService.updateWidget(context)
    }

    fun createNotificationChannel(context: Context) {
        // Implementation of createNotificationChannel method
    }
} 