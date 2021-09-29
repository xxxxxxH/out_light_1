package net.basicmodel.provider

import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import android.widget.RemoteViews
import net.basicmodel.R
import android.content.Intent
import net.basicmodel.ui.activity.MainActivity
import android.app.PendingIntent
import android.content.Context
import android.provider.Settings
import net.basicmodel.ui.activity.CompassActivity
import net.basicmodel.ui.activity.ConfigActivity
import net.basicmodel.ui.activity.ScreenLightActivity

class ActionProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_full_layout)
        val flashIntent = Intent(context, MainActivity::class.java)
        flashIntent.putExtra("ACTION_TURN_ON", true)
        val flashPendingIntent = PendingIntent.getActivity(context, 11001, flashIntent, 0)
        remoteViews.setOnClickPendingIntent(R.id.widget_ll_power, flashPendingIntent)
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
        val mirrorIntent = Intent(context, CompassActivity::class.java)
        val mirrorPendingIntent = PendingIntent.getActivity(context, 11003, mirrorIntent, 0)
        remoteViews.setOnClickPendingIntent(R.id.widget_ll_glass, mirrorPendingIntent)
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
        val settingIntent = Intent(context, ConfigActivity::class.java)
        val settingPendingIntent = PendingIntent.getActivity(context, 11002, settingIntent, 0)
        remoteViews.setOnClickPendingIntent(R.id.widget_ll_setting, settingPendingIntent)
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
        val wifiIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
        val wifiPendingIntent = PendingIntent.getActivity(context, 11005, wifiIntent, 0)
        remoteViews.setOnClickPendingIntent(R.id.widget_ll_wifi_setting, wifiPendingIntent)
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
        val lightIntent = Intent(context, ScreenLightActivity::class.java)
        lightIntent.putExtra("ACTION_LIGHT_WIDGET", true)
        val lightPendingIntent = PendingIntent.getActivity(context, 11004, lightIntent, 0)
        remoteViews.setOnClickPendingIntent(R.id.widget_ll_screenlight, lightPendingIntent)
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
    }
}