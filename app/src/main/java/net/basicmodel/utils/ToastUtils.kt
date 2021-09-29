package net.basicmodel.utils

import android.widget.Toast
import android.content.Context

object ToastUtils {
    private var toast: Toast? = null

    fun showToast(context: Context?, message: String?) {
        if (toast == null) {
            toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
        } else {
            toast?.setText(message)
        }
        toast?.show()
    }

}