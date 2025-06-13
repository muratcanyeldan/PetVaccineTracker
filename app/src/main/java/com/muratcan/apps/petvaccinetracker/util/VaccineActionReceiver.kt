package com.muratcan.apps.petvaccinetracker.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.widget.Toast
import com.muratcan.apps.petvaccinetracker.R
import com.muratcan.apps.petvaccinetracker.database.AppDatabase
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class VaccineActionReceiver : BroadcastReceiver() {
    companion object {
        private const val ACTION_MARK_DONE = "com.muratcan.apps.petvaccinetracker.MARK_VACCINE_DONE"
        private const val ACTION_POSTPONE = "com.muratcan.apps.petvaccinetracker.POSTPONE_VACCINE"

        // Public static methods to create intents
        fun createMarkDoneIntent(
            context: Context,
            vaccineId: Long,
            petId: Long,
            notificationId: Int,
            petName: String,
            vaccineName: String
        ): Intent {
            return Intent(context, VaccineActionReceiver::class.java).apply {
                action = ACTION_MARK_DONE
                putExtra("vaccineId", vaccineId)
                putExtra("petId", petId)
                putExtra("notificationId", notificationId)
                putExtra("petName", petName)
                putExtra("vaccineName", vaccineName)
            }
        }

        fun createPostponeIntent(
            context: Context,
            vaccineId: Long,
            petId: Long,
            notificationId: Int,
            petName: String,
            vaccineName: String
        ): Intent {
            return Intent(context, VaccineActionReceiver::class.java).apply {
                action = ACTION_POSTPONE
                putExtra("vaccineId", vaccineId)
                putExtra("petId", petId)
                putExtra("notificationId", notificationId)
                putExtra("petName", petName)
                putExtra("vaccineName", vaccineName)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val vaccineId = intent.getLongExtra("vaccineId", -1)
        val petId = intent.getLongExtra("petId", -1)
        val notificationId = intent.getIntExtra("notificationId", -1)
        val petName = intent.getStringExtra("petName")
        val vaccineName = intent.getStringExtra("vaccineName")

        if (vaccineId == -1L || petId == -1L || notificationId == -1) {
            Timber.e("Invalid action data received")
            return
        }

        // Dismiss the notification
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        when (action) {
            ACTION_MARK_DONE -> handleMarkDone(context, vaccineId, petId, petName, vaccineName)
            ACTION_POSTPONE -> handlePostpone(context, vaccineId, petId, petName, vaccineName)
        }
    }

    private fun handleMarkDone(
        context: Context,
        vaccineId: Long,
        petId: Long,
        petName: String?,
        vaccineName: String?
    ) {
        Thread {
            try {
                val database = AppDatabase.getDatabase(context)
                val vaccine = database.vaccineDao().getVaccineById(vaccineId)

                if (vaccine != null) {
                    // Mark as administered today
                    vaccine.dateAdministered = Date()

                    // If it's a recurring vaccine, calculate next due date using stored recurrence period
                    if (vaccine.isRecurring && vaccine.recurrenceMonths > 0) {
                        val nextDue = Calendar.getInstance().apply {
                            time = Date() // From today
                            add(Calendar.MONTH, vaccine.recurrenceMonths) // Use stored period!
                        }

                        vaccine.nextDueDate = nextDue.time

                        // Schedule new notifications for the next due date
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        NotificationHelper.scheduleNotification(
                            context,
                            petName ?: "",
                            vaccineName ?: "",
                            dateFormat.format(nextDue.time),
                            petId,
                            vaccineId
                        )

                        showToast(
                            context,
                            context.getString(R.string.vaccine_marked_done_recurring, vaccineName)
                        )
                    } else {
                        // One-time vaccine, just mark as done
                        vaccine.nextDueDate = null
                        showToast(
                            context,
                            context.getString(R.string.vaccine_marked_done, vaccineName)
                        )
                    }

                    database.vaccineDao().update(vaccine)

                    // Cancel any remaining notifications for this vaccine
                    cancelRemainingNotifications(context, vaccineId)

                    Timber.d("Vaccine marked as done: %s for pet %s", vaccineName, petName)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to mark vaccine as done")
                showToast(context, context.getString(R.string.error_marking_vaccine_done))
            }
        }.start()
    }

    private fun handlePostpone(
        context: Context,
        vaccineId: Long,
        petId: Long,
        petName: String?,
        vaccineName: String?
    ) {
        Thread {
            try {
                val database = AppDatabase.getDatabase(context)
                val vaccine = database.vaccineDao().getVaccineById(vaccineId)

                if (vaccine != null && vaccine.nextDueDate != null) {
                    // Postpone by 7 days
                    val postponedDate = Calendar.getInstance().apply {
                        time = vaccine.nextDueDate!!
                        add(Calendar.DAY_OF_MONTH, 7)
                    }

                    vaccine.nextDueDate = postponedDate.time
                    database.vaccineDao().update(vaccine)

                    // Cancel existing notifications
                    cancelRemainingNotifications(context, vaccineId)

                    // Schedule new notifications for postponed date
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    NotificationHelper.scheduleNotification(
                        context,
                        petName ?: "",
                        vaccineName ?: "",
                        dateFormat.format(postponedDate.time),
                        petId,
                        vaccineId
                    )

                    showToast(context, context.getString(R.string.vaccine_postponed, vaccineName))
                    Timber.d("Vaccine postponed: %s for pet %s", vaccineName, petName)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to postpone vaccine")
                showToast(context, context.getString(R.string.error_postponing_vaccine))
            }
        }.start()
    }

    private fun cancelRemainingNotifications(context: Context, vaccineId: Long) {
        // Cancel all notifications for this vaccine (all reminder days)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val reminderDays = intArrayOf(3, 1, 0) // Same as in NotificationHelper

        for (reminderDay in reminderDays) {
            val notificationId = (vaccineId * 1000 + reminderDay).toInt()
            notificationManager.cancel(notificationId)
        }
    }

    private fun showToast(context: Context, message: String) {
        // Show toast on main thread
        val mainHandler = Handler(context.mainLooper)
        mainHandler.post { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }
} 