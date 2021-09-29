package net.basicmodel.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.basicmodel.constants.Constants
import net.basicmodel.utils.SaveUtils
import net.basicmodel.manager.MyNotificationManager

class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            if (SaveUtils
                    .getBoolean(Constants.HAVE_CONTROLLER_KEY, true)
            ) {
                MyNotificationManager(context).buildNotification()
            }
        }
    }
}