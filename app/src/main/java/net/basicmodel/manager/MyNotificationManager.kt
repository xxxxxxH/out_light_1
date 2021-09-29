package net.basicmodel.manager

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.app.PendingIntent
import android.content.Context
import android.view.View
import androidx.core.app.NotificationCompat
import net.basicmodel.R
import net.basicmodel.utils.SaveUtils
import android.widget.RemoteViews
import net.basicmodel.dpToPx
import net.basicmodel.ui.activity.LowPowerActivity
import net.basicmodel.ui.activity.CompassActivity
import net.basicmodel.ui.activity.MainActivity
import net.basicmodel.ui.activity.ScreenLightActivity
import net.basicmodel.view.tooltip.Tooltip

class MyNotificationManager(private val context: Context) {
    private val NOTIFICATION_SHOW_TIPS = "NOTIFICATION_SHOW_TIPS"
    var view: View? = null
    private var showTips = false

    fun buildNotification(vararg isUp: Boolean) {
        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationIntent = Intent()
        val pIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_CODE_FLASHLIGHT,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        var builder = NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_light_statusbar)
            .setAutoCancel(true)
            .setContentIntent(pIntent)
        builder = if (isUp.size > 0) {
            builder.setContent(getComplexNotificationView(isUp[0]))
        } else {
            builder.setContent(getComplexNotificationView(false))
        }
        val notification = builder.build()
        notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        notification.priority = Notification.PRIORITY_MAX
        mNotificationManager.notify(NOTIFICATION_CODE_FLASHLIGHT, notification)
        if (SaveUtils.getBoolean(NOTIFICATION_SHOW_TIPS, true)) {
            if (view != null) {
                showTooltips(view, context.getString(R.string.have_controller_key))
            }
            SaveUtils.putBoolean(NOTIFICATION_SHOW_TIPS, false)
        }
    }

    fun setShowTips(showTips: Boolean) {
        this.showTips = showTips
    }

    private fun showTooltips(v: View?, content: String) {
        Tooltip.make(
            context,
            Tooltip.Builder(101)
                .anchor(v, Tooltip.Gravity.BOTTOM)
                .closePolicy(Tooltip.ClosePolicy.TOUCH_ANYWHERE_NO_CONSUME, 3000)
                .text(content)
                .fadeDuration(400)
                .fitToScreen(true)
                .maxWidth(dpToPx(170))
                .showDelay(400)
                .withArrow(true)
                .withOverlay(showTips)
                .build()
        ).show()
    }

    private fun getComplexNotificationView(isUp: Boolean): RemoteViews {
        val notificationView: RemoteViews
        if (isUp) {
            notificationView = RemoteViews(context.packageName, R.layout.notification_upsm_layout)
            val turnOnFlashIntent = Intent(context, LowPowerActivity::class.java)
            turnOnFlashIntent.action = NOTIFICATION_ACTION_ON_KEY
            val pendingIntent = PendingIntent.getActivity(
                context, 1,
                turnOnFlashIntent, 0
            )
            notificationView.setOnClickPendingIntent(R.id.notification_flashlight, pendingIntent)
        } else {
            notificationView = RemoteViews(context.packageName, R.layout.notification_layout)
            val screenLightIntent = Intent(context, ScreenLightActivity::class.java)
            screenLightIntent.action = NOTIFICATION_ACTION_SCREEN_KEY
            val screenLightPendingIntent = PendingIntent.getActivity(
                context, 2,
                screenLightIntent, 0
            )
            notificationView.setOnClickPendingIntent(R.id.notification_screenlight, screenLightPendingIntent)
            val glassIntent = Intent(context, CompassActivity::class.java)
            val glassPendingIntent = PendingIntent.getActivity(
                context, 3,
                glassIntent, 0
            )
            notificationView.setOnClickPendingIntent(R.id.notification_glass, glassPendingIntent)
            val turnOnFlashIntent = Intent(context, MainActivity::class.java)
            turnOnFlashIntent.action = NOTIFICATION_ACTION_ON_KEY
            val turnOnFlashPendingIntent = PendingIntent.getActivity(
                context, 1,
                turnOnFlashIntent, 0
            )
            notificationView.setOnClickPendingIntent(R.id.notification_flashlight, turnOnFlashPendingIntent)
        }
        return notificationView
    }

    fun hideNotification(notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
        if (view != null) {
            showTooltips(view, context.getString(R.string.msg_disable_quick_controller))
        }
        SaveUtils.putBoolean(NOTIFICATION_SHOW_TIPS, true)
    }

    companion object {
        const val NOTIFICATION_CODE_FLASHLIGHT = 2596
        const val NOTIFICATION_ACTION_ON_KEY = "NOTIFICATION_ACTION_ON_KEY"
        const val NOTIFICATION_ACTION_SCREEN_KEY = "NOTIFICATION_ACTION_SCREEN_KEY"
    }
}