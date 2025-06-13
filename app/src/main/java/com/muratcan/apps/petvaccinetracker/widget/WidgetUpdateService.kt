package com.muratcan.apps.petvaccinetracker.widget

import android.app.IntentService
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class WidgetUpdateService : IntentService("WidgetUpdateService") {
    companion object {
        private const val ACTION_UPDATE_WIDGET = "com.muratcan.apps.petvaccinetracker.UPDATE_WIDGET"

        fun updateWidget(context: Context) {
            val intent = Intent(context, WidgetUpdateService::class.java)
            context.startService(intent)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        if (ACTION_UPDATE_WIDGET == intent?.action) {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val componentName = ComponentName(this, VaccineWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            val widgetProvider = VaccineWidgetProvider()
            widgetProvider.onUpdate(this, appWidgetManager, appWidgetIds)
        }
    }
} 