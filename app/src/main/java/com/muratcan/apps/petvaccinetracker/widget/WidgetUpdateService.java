package com.muratcan.apps.petvaccinetracker.widget;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;

public class WidgetUpdateService extends IntentService {
    private static final String ACTION_UPDATE_WIDGET = "com.muratcan.apps.petvaccinetracker.UPDATE_WIDGET";

    public WidgetUpdateService() {
        super("WidgetUpdateService");
    }

    public static Intent createUpdateIntent(android.content.Context context) {
        Intent intent = new Intent(context, WidgetUpdateService.class);
        intent.setAction(ACTION_UPDATE_WIDGET);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ACTION_UPDATE_WIDGET.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            ComponentName componentName = new ComponentName(this, VaccineWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
            
            VaccineWidgetProvider widgetProvider = new VaccineWidgetProvider();
            widgetProvider.onUpdate(this, appWidgetManager, appWidgetIds);
        }
    }
} 