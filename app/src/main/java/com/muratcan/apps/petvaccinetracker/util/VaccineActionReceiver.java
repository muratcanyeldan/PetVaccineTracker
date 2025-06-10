package com.muratcan.apps.petvaccinetracker.util;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.muratcan.apps.petvaccinetracker.R;
import com.muratcan.apps.petvaccinetracker.database.AppDatabase;
import com.muratcan.apps.petvaccinetracker.model.Vaccine;

import java.util.Calendar;
import java.util.Date;

import timber.log.Timber;

public class VaccineActionReceiver extends BroadcastReceiver {
    private static final String ACTION_MARK_DONE = "com.muratcan.apps.petvaccinetracker.MARK_VACCINE_DONE";
    private static final String ACTION_POSTPONE = "com.muratcan.apps.petvaccinetracker.POSTPONE_VACCINE";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        long vaccineId = intent.getLongExtra("vaccineId", -1);
        long petId = intent.getLongExtra("petId", -1);
        int notificationId = intent.getIntExtra("notificationId", -1);
        String petName = intent.getStringExtra("petName");
        String vaccineName = intent.getStringExtra("vaccineName");

        if (vaccineId == -1 || petId == -1 || notificationId == -1) {
            Timber.e("Invalid action data received");
            return;
        }

        // Dismiss the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);

        switch (action) {
            case ACTION_MARK_DONE:
                handleMarkDone(context, vaccineId, petId, petName, vaccineName);
                break;
            case ACTION_POSTPONE:
                handlePostpone(context, vaccineId, petId, petName, vaccineName);
                break;
        }
    }

    private void handleMarkDone(Context context, long vaccineId, long petId, String petName, String vaccineName) {
        new Thread(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(context);
                Vaccine vaccine = database.vaccineDao().getVaccineById(vaccineId);

                if (vaccine != null) {
                    // Mark as administered today
                    vaccine.setDateAdministered(new Date());

                    // If it's a recurring vaccine, calculate next due date using stored recurrence period
                    if (vaccine.isRecurring() && vaccine.getRecurrenceMonths() > 0) {
                        Calendar nextDue = Calendar.getInstance();
                        nextDue.setTime(new Date()); // From today
                        nextDue.add(Calendar.MONTH, vaccine.getRecurrenceMonths()); // Use stored period!

                        vaccine.setNextDueDate(nextDue.getTime());

                        // Schedule new notifications for the next due date
                        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                        NotificationHelper.scheduleNotification(
                                context,
                                petName,
                                vaccineName,
                                dateFormat.format(nextDue.getTime()),
                                petId,
                                vaccineId
                        );

                        showToast(context, context.getString(R.string.vaccine_marked_done_recurring, vaccineName));
                    } else {
                        // One-time vaccine, just mark as done
                        vaccine.setNextDueDate(null);
                        showToast(context, context.getString(R.string.vaccine_marked_done, vaccineName));
                    }

                    database.vaccineDao().update(vaccine);

                    // Cancel any remaining notifications for this vaccine
                    cancelRemainingNotifications(context, vaccineId);

                    Timber.d("Vaccine marked as done: %s for pet %s", vaccineName, petName);
                }
            } catch (Exception e) {
                Timber.e(e, "Failed to mark vaccine as done");
                showToast(context, context.getString(R.string.error_marking_vaccine_done));
            }
        }).start();
    }

    private void handlePostpone(Context context, long vaccineId, long petId, String petName, String vaccineName) {
        new Thread(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(context);
                Vaccine vaccine = database.vaccineDao().getVaccineById(vaccineId);

                if (vaccine != null && vaccine.getNextDueDate() != null) {
                    // Postpone by 7 days
                    Calendar postponedDate = Calendar.getInstance();
                    postponedDate.setTime(vaccine.getNextDueDate());
                    postponedDate.add(Calendar.DAY_OF_MONTH, 7);

                    vaccine.setNextDueDate(postponedDate.getTime());
                    database.vaccineDao().update(vaccine);

                    // Cancel existing notifications
                    cancelRemainingNotifications(context, vaccineId);

                    // Schedule new notifications for postponed date
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                    NotificationHelper.scheduleNotification(
                            context,
                            petName,
                            vaccineName,
                            dateFormat.format(postponedDate.getTime()),
                            petId,
                            vaccineId
                    );

                    showToast(context, context.getString(R.string.vaccine_postponed, vaccineName));
                    Timber.d("Vaccine postponed: %s for pet %s", vaccineName, petName);
                }
            } catch (Exception e) {
                Timber.e(e, "Failed to postpone vaccine");
                showToast(context, context.getString(R.string.error_postponing_vaccine));
            }
        }).start();
    }

    private void cancelRemainingNotifications(Context context, long vaccineId) {
        // Cancel all notifications for this vaccine (all reminder days)
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int[] reminderDays = {3, 1, 0}; // Same as in NotificationHelper

        for (int reminderDay : reminderDays) {
            int notificationId = (int) (vaccineId * 1000 + reminderDay);
            notificationManager.cancel(notificationId);
        }
    }

    private void showToast(Context context, String message) {
        // Show toast on main thread
        android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    // Public static methods to create intents
    public static Intent createMarkDoneIntent(Context context, long vaccineId, long petId, int notificationId, String petName, String vaccineName) {
        Intent intent = new Intent(context, VaccineActionReceiver.class);
        intent.setAction(ACTION_MARK_DONE);
        intent.putExtra("vaccineId", vaccineId);
        intent.putExtra("petId", petId);
        intent.putExtra("notificationId", notificationId);
        intent.putExtra("petName", petName);
        intent.putExtra("vaccineName", vaccineName);
        return intent;
    }

    public static Intent createPostponeIntent(Context context, long vaccineId, long petId, int notificationId, String petName, String vaccineName) {
        Intent intent = new Intent(context, VaccineActionReceiver.class);
        intent.setAction(ACTION_POSTPONE);
        intent.putExtra("vaccineId", vaccineId);
        intent.putExtra("petId", petId);
        intent.putExtra("notificationId", notificationId);
        intent.putExtra("petName", petName);
        intent.putExtra("vaccineName", vaccineName);
        return intent;
    }
}
