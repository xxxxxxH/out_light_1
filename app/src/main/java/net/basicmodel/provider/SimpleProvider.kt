package net.basicmodel.provider

import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import android.widget.RemoteViews
import net.basicmodel.R
import android.content.Intent
import net.basicmodel.ui.activity.MainActivity
import android.app.PendingIntent
import android.content.Context

class SimpleProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_lite_layout)
        val flashIntent = Intent(context, MainActivity::class.java)
        flashIntent.putExtra("ACTION_TURN_ON", true)
        val flashPendingIntent = PendingIntent.getActivity(context, 11002, flashIntent, 0)
        remoteViews.setOnClickPendingIntent(R.id.widget_lite_btn_power, flashPendingIntent)
        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
    }
}