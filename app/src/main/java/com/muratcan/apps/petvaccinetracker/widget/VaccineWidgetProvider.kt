package com.muratcan.apps.petvaccinetracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.muratcan.apps.petvaccinetracker.MainActivity
import com.muratcan.apps.petvaccinetracker.R
import com.muratcan.apps.petvaccinetracker.database.AppDatabase
import com.muratcan.apps.petvaccinetracker.model.Vaccine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class VaccineWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_WIDGET_REFRESH =
            "com.muratcan.apps.petvaccinetracker.WIDGET_REFRESH"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (ACTION_WIDGET_REFRESH == intent.action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, VaccineWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_vaccine_summary)

        // Set up click intent to open main app
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        // Set up refresh button
        val refreshIntent = Intent(context, VaccineWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

        // Load vaccine data in background thread
        thread {
            try {
                val data = loadWidgetData(context)

                // Update UI on main thread
                val mainHandler = Handler(Looper.getMainLooper())
                mainHandler.post {
                    updateWidgetViews(views, data)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                // Handle error
                val mainHandler = Handler(Looper.getMainLooper())
                mainHandler.post {
                    views.setTextViewText(R.id.widget_status_text, "Error loading data")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }

    private fun loadWidgetData(context: Context): WidgetData {
        val database = AppDatabase.getDatabase(context)
        val data = WidgetData()

        try {
            val pets = database.petDao().getAllPetsSync()

            for (pet in pets) {
                val vaccines = database.vaccineDao().getVaccinesForPetSync(pet.id)

                for (vaccine in vaccines) {
                    vaccine.nextDueDate?.let { dueDate ->
                        val daysUntilDue = getDaysUntilDue(dueDate)

                        when {
                            daysUntilDue < 0 -> data.overdueCount++
                            daysUntilDue <= 7 -> data.dueThisWeekCount++
                        }

                        // Find next upcoming vaccine
                        if (daysUntilDue >= 0 && (data.nextVaccine == null || daysUntilDue < data.nextDaysUntilDue)) {
                            data.nextVaccine = vaccine
                            data.nextPetName = pet.name
                            data.nextDaysUntilDue = daysUntilDue
                        }
                    }
                }
            }
        } catch (e: Exception) {
            data.hasError = true
            data.errorMessage = e.message
        }

        return data
    }

    private fun updateWidgetViews(views: RemoteViews, data: WidgetData) {
        if (data.hasError) {
            views.setTextViewText(R.id.widget_status_text, "Error: ${data.errorMessage}")
            views.setTextViewText(R.id.widget_details_text, "Tap to open app")
            return
        }

        // Update overdue count
        views.setTextViewText(R.id.widget_overdue_count, data.overdueCount.toString())
        if (data.overdueCount > 0) {
            views.setTextColor(R.id.widget_overdue_count, Color.RED)
        } else {
            views.setTextColor(R.id.widget_overdue_count, Color.GREEN)
        }

        // Update this week count
        views.setTextViewText(R.id.widget_this_week_count, data.dueThisWeekCount.toString())

        // Update next vaccine info
        val nextVaccine = data.nextVaccine
        if (nextVaccine != null) {
            val statusText = "${data.nextPetName}'s ${nextVaccine.name}"
            val detailText = when (data.nextDaysUntilDue) {
                0L -> "Due today"
                1L -> "Due tomorrow"
                else -> "Due in ${data.nextDaysUntilDue} days"
            }

            views.setTextViewText(R.id.widget_status_text, statusText)
            views.setTextViewText(R.id.widget_details_text, detailText)
        } else {
            views.setTextViewText(R.id.widget_status_text, "All vaccines up to date!")
            views.setTextViewText(R.id.widget_details_text, "No upcoming vaccines")
        }

        // Update last updated time
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        views.setTextViewText(R.id.widget_last_updated, "Updated: ${timeFormat.format(Date())}")
    }

    private fun getDaysUntilDue(dueDate: Date): Long {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val due = Calendar.getInstance().apply {
            time = dueDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffInMillis = due.timeInMillis - now.timeInMillis
        return diffInMillis / (1000 * 60 * 60 * 24)
    }

    // Helper class to hold widget data
    private data class WidgetData(
        var overdueCount: Int = 0,
        var dueThisWeekCount: Int = 0,
        var nextVaccine: Vaccine? = null,
        var nextPetName: String? = null,
        var nextDaysUntilDue: Long = Long.MAX_VALUE,
        var hasError: Boolean = false,
        var errorMessage: String? = null
    )
} 