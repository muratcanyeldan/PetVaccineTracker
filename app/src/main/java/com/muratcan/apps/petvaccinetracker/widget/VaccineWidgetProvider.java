package com.muratcan.apps.petvaccinetracker.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.muratcan.apps.petvaccinetracker.MainActivity;
import com.muratcan.apps.petvaccinetracker.R;
import com.muratcan.apps.petvaccinetracker.database.AppDatabase;
import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.model.Vaccine;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VaccineWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_WIDGET_REFRESH = "com.muratcan.apps.petvaccinetracker.WIDGET_REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_WIDGET_REFRESH.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, VaccineWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_vaccine_summary);

        // Set up click intent to open main app
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

        // Set up refresh button
        Intent refreshIntent = new Intent(context, VaccineWidgetProvider.class);
        refreshIntent.setAction(ACTION_WIDGET_REFRESH);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent);

        // Load vaccine data in background thread
        new Thread(() -> {
            try {
                WidgetData data = loadWidgetData(context);

                // Update UI on main thread
                android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                mainHandler.post(() -> {
                    updateWidgetViews(views, data);
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                });

            } catch (Exception e) {
                // Handle error
                android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                mainHandler.post(() -> {
                    views.setTextViewText(R.id.widget_status_text, "Error loading data");
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                });
            }
        }).start();
    }

    private WidgetData loadWidgetData(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        WidgetData data = new WidgetData();

        try {
            List<Pet> pets = database.ktPetDao().getAllPetsSync();

            for (Pet pet : pets) {
                List<Vaccine> vaccines = database.vaccineDao().getVaccinesForPetSync(pet.getId());

                for (Vaccine vaccine : vaccines) {
                    if (vaccine.getNextDueDate() != null) {
                        long daysUntilDue = getDaysUntilDue(vaccine.getNextDueDate());

                        if (daysUntilDue < 0) {
                            data.overdueCount++;
                        } else if (daysUntilDue <= 7) {
                            data.dueThisWeekCount++;
                        }

                        // Find next upcoming vaccine
                        if (daysUntilDue >= 0 && (data.nextVaccine == null || daysUntilDue < data.nextDaysUntilDue)) {
                            data.nextVaccine = vaccine;
                            data.nextPetName = pet.getName();
                            data.nextDaysUntilDue = daysUntilDue;
                        }
                    }
                }
            }
        } catch (Exception e) {
            data.hasError = true;
            data.errorMessage = e.getMessage();
        }

        return data;
    }

    private void updateWidgetViews(RemoteViews views, WidgetData data) {
        if (data.hasError) {
            views.setTextViewText(R.id.widget_status_text, "Error: " + data.errorMessage);
            views.setTextViewText(R.id.widget_details_text, "Tap to open app");
            return;
        }

        // Update overdue count
        views.setTextViewText(R.id.widget_overdue_count, String.valueOf(data.overdueCount));
        if (data.overdueCount > 0) {
            views.setTextColor(R.id.widget_overdue_count, android.graphics.Color.RED);
        } else {
            views.setTextColor(R.id.widget_overdue_count, android.graphics.Color.GREEN);
        }

        // Update this week count
        views.setTextViewText(R.id.widget_this_week_count, String.valueOf(data.dueThisWeekCount));

        // Update next vaccine info
        if (data.nextVaccine != null) {
            String statusText = data.nextPetName + "'s " + data.nextVaccine.getName();
            String detailText;

            if (data.nextDaysUntilDue == 0) {
                detailText = "Due today";
            } else if (data.nextDaysUntilDue == 1) {
                detailText = "Due tomorrow";
            } else {
                detailText = "Due in " + data.nextDaysUntilDue + " days";
            }

            views.setTextViewText(R.id.widget_status_text, statusText);
            views.setTextViewText(R.id.widget_details_text, detailText);
        } else {
            views.setTextViewText(R.id.widget_status_text, "All vaccines up to date!");
            views.setTextViewText(R.id.widget_details_text, "No upcoming vaccines");
        }

        // Update last updated time
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        views.setTextViewText(R.id.widget_last_updated, "Updated: " + timeFormat.format(new Date()));
    }

    private long getDaysUntilDue(Date dueDate) {
        Calendar now = Calendar.getInstance();
        Calendar due = Calendar.getInstance();
        due.setTime(dueDate);

        // Reset time to start of day for accurate day calculation
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        due.set(Calendar.HOUR_OF_DAY, 0);
        due.set(Calendar.MINUTE, 0);
        due.set(Calendar.SECOND, 0);
        due.set(Calendar.MILLISECOND, 0);

        long diffInMillis = due.getTimeInMillis() - now.getTimeInMillis();
        return diffInMillis / (1000 * 60 * 60 * 24);
    }

    // Helper class to hold widget data
    private static class WidgetData {
        int overdueCount = 0;
        int dueThisWeekCount = 0;
        Vaccine nextVaccine = null;
        String nextPetName = null;
        long nextDaysUntilDue = Long.MAX_VALUE;
        boolean hasError = false;
        String errorMessage = null;
    }
}