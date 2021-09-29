package net.basicmodel

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Vibrator
import android.provider.MediaStore
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import net.basicmodel.app.App
import net.basicmodel.interfaces.IScreen
import net.basicmodel.interfaces.ITimer
import java.lang.RuntimeException
import java.lang.StringBuilder

fun bitmapRotate(bitmap: Bitmap, degree: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val mtx = Matrix()
    mtx.setRotate(degree.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true)
}

fun addImageToGallery(filePath: String?, context: Context) {
    val values = ContentValues()
    values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    values.put(MediaStore.MediaColumns.DATA, filePath)
    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
}

fun scaleBitmap(bitmap: Bitmap?, newWidth: Int, newHeight: Int): Bitmap {
    return Bitmap.createScaledBitmap(bitmap!!, newWidth, newHeight, true)
}

fun getActivity(context: Context?): Activity? {
    if (context == null) {
        return null
    } else if (context is Activity) {
        return context
    } else if (context is ContextWrapper) {
        return getActivity(context.baseContext)
    }
    return null
}

fun getRectContains(parentRect: Rect, childRect: Rect, t: Int): Boolean {
    return parentRect.contains(childRect.left + t, childRect.top + t, childRect.right - t, childRect.bottom - t)
}

fun isSupportFlashLight(context: Context): Boolean {
    val pm = context.packageManager
    return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
}

fun sleep(delay: Int) {
    try {
        Thread.sleep(delay.toLong())
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
}

fun getVersionName(context: Context): String {
    var versionName = "1.0"
    try {
        versionName = context.packageManager.getPackageInfo(
            context.packageName, 0
        ).versionName
    } catch (ignored: PackageManager.NameNotFoundException) {
    }
    return versionName
}

fun calculateSecond(second: Int): Int {
    return if (second != 0) {
        Math.round((1000 / second).toFloat())
    } else {
        0
    }
}

fun dpToPx(dp: Int): Int {
    return (Resources.getSystem().displayMetrics.density * dp + 0.5f).toInt()
}

fun vibrate(context: Context) {
    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    v.vibrate(20)
}

fun countDownLastTime(millisecond: Long): String {
    var milliseconds = millisecond
    val second = 1000
    val minute = 60 * second
    val hour = 60 * minute
    val day = 24 * hour
    val content = StringBuilder()
    if (milliseconds > day) {
        content.append(milliseconds / day).append("d ")
        milliseconds %= day.toLong()
    }
    if (milliseconds > hour) {
        content.append(milliseconds / hour).append("h ")
        milliseconds %= hour.toLong()
    }
    if (milliseconds > minute) {
        content.append(milliseconds / minute).append("m ")
        milliseconds %= minute.toLong()
    }
    if (milliseconds > second) {
        content.append(milliseconds / second).append("s ")
    }
    return content.toString()
}

fun hasCameraPermissions(context: Context): Boolean {
    return XXPermissions.isGranted(context, Permission.CAMERA)
}

fun hasStoragePermissions(context: Context): Boolean {
    return XXPermissions.isGranted(context, Permission.WRITE_EXTERNAL_STORAGE)
}

fun requestCameraPermission(context: Context, onPermissionCallback: OnPermissionCallback) {
    XXPermissions.with(context)
        .permission(Permission.CAMERA)
        .request(onPermissionCallback)
}

fun requestStoragePermission(context: Context, onPermissionCallback: OnPermissionCallback) {
    XXPermissions.with(context)
        .permission(Permission.WRITE_EXTERNAL_STORAGE)
        .request(onPermissionCallback)
}

fun jumpToAppPermissionDetail(context: Context, vararg permissions: String) {
    XXPermissions.startPermissionActivity(context, permissions)
}

fun getLastTime(iTimer: ITimer, status: Int, tempStatus: Int, time: Long, tempTime: Long) {
    val timeEstimate: Long
    val totalTime: Long = 216000000
    if (tempStatus == 0) {
        timeEstimate = totalTime * status / 100
        iTimer.onTimeChange(timeEstimate)
    } else {
        val speedBattery = status - tempStatus
        val speedTime = Math.abs(time - tempTime)
        if (speedBattery < 0) {
            timeEstimate = status * speedTime / Math.abs(speedBattery)
            iTimer.onTimeChange(timeEstimate)
        } else if (speedBattery != 0) {
            iTimer.onTimeChange(totalTime)
        }
    }
}

var interceptSOSLight = false
private var mThread: Thread? = null

fun startSOSLight(iScreen: IScreen) {
    mThread = Thread {
        interceptSOSLight = false
        while (!interceptSOSLight) {
            try {
                val delay = calculateSecond(7)
                iScreen.onScreenChange(Color.WHITE)
                Thread.sleep(delay.toLong())
                iScreen.onScreenChange(Color.BLACK)
                Thread.sleep(delay.toLong())
            } catch (ex: InterruptedException) {
                ex.printStackTrace()
            } catch (ex: RuntimeException) {
                interceptSOSLight = true
                ex.printStackTrace()
            }
        }
        interceptSOSLight = false
    }
    mThread?.start()
}

fun stopSOSLight() {
    if (mThread != null) {
        mThread!!.interrupt()
        mThread = null
    }
}

val app
    get() = Ktx.getInstance().app
