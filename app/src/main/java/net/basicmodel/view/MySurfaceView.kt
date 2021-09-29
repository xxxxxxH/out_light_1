package net.basicmodel.view

import android.app.Activity
import android.view.SurfaceView
import android.view.SurfaceHolder
import android.graphics.ImageFormat
import android.hardware.Camera.CameraInfo
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Rect
import android.hardware.Camera
import android.os.Build
import android.hardware.Camera.AutoFocusCallback
import android.view.MotionEvent
import android.view.Surface
import java.io.IOException
import java.lang.Exception
import java.lang.RuntimeException
import java.util.ArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MySurfaceView(private val activity: Activity, context: Context?, private val camera: Camera?) :
    SurfaceView(context), SurfaceHolder.Callback {
    private val mHolder: SurfaceHolder
    private var isAutoFocusing = false
    private var isSurfaceCreated = false
    var isPreview = false
    private var mCurrentCameraId = CameraInfo.CAMERA_FACING_BACK
    fun setCurrentCameraId(currentCameraId: Int) {
        mCurrentCameraId = currentCameraId
    }

    private val mRotation: Int
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, weight: Int, height: Int) {
        if (mHolder.surface == null) {
            return
        }
        try {
            camera!!.stopPreview()
        } catch (ignored: Exception) {
        }
        try {
            camera!!.setPreviewDisplay(mHolder)
            camera.startPreview()
            isPreview = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val param: Camera.Parameters
        param = camera!!.parameters
        try {
            camera.stopPreview()
            camera.setDisplayOrientation(mRotation)
            val bestSize = findBestCameraSize()
            val supportedPreviewFormats = param.supportedPreviewFormats
            for (previewFormat in supportedPreviewFormats) {
                if (previewFormat == ImageFormat.YV12) {
                    param.previewFormat = previewFormat
                }
            }
            param.setPreviewSize(bestSize.width, bestSize.height)
            param.setPictureSize(bestSize.width, bestSize.height)
            camera.parameters = param
            camera.setPreviewDisplay(holder)
            camera.startPreview()
            isPreview = true
            isSurfaceCreated = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceCreated = false
    }

    val cameraDisplayOrientation: Int
        get() {
            val info = CameraInfo()
            Camera.getCameraInfo(mCurrentCameraId, info)
            val rotation = activity.windowManager.defaultDisplay.rotation
            var degrees = 0
            when (rotation) {
                Surface.ROTATION_0 -> degrees = 0
                Surface.ROTATION_90 -> degrees = 90
                Surface.ROTATION_180 -> degrees = 180
                Surface.ROTATION_270 -> degrees = 270
            }
            var result: Int
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360
                result = (360 - result) % 360
            } else {
                result = (info.orientation - degrees + 360) % 360
            }
            return result
        }

    private fun findBestCameraSize(): Camera.Size {
        val param = camera!!.parameters
        var size: Camera.Size
        val sizeList = param.supportedPreviewSizes
        size = sizeList[0]
        for (i in 1 until sizeList.size) {
            if (sizeList[i].width * sizeList[i].height > size.width * size.height) {
                size = sizeList[i]
            }
        }
        return size
    }

    private fun calculateFocusArea(x: Float, y: Float, width: Int, height: Int): Rect {
        val focusSize = 300
        val left = clamp(java.lang.Float.valueOf(x / width * 2000 - 1000).toInt(), focusSize)
        val top = clamp(java.lang.Float.valueOf(y / height * 2000 - 1000).toInt(), focusSize)
        return Rect(left, top, left + focusSize, top + focusSize)
    }

    private fun clamp(touchCoordinateInCamera: Int, focusAreaSize: Int): Int {
        val result: Int
        result = if (Math.abs(touchCoordinateInCamera) + focusAreaSize / 2 > 1000) {
            if (touchCoordinateInCamera > 0) {
                1000 - focusAreaSize / 2
            } else {
                -1000 + focusAreaSize / 2
            }
        } else {
            touchCoordinateInCamera - focusAreaSize / 2
        }
        return result
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    fun focusOnTouch(rect: Rect?, callback: AutoFocusCallback?) {
        if (camera != null && !isAutoFocusing && isSurfaceCreated) {
            val parameters = camera.parameters
            if (parameters.maxNumMeteringAreas > 0) {
                isAutoFocusing = true
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
                val meteringAreas: MutableList<Camera.Area> = ArrayList()
                meteringAreas.add(Camera.Area(rect, 800))
                parameters.focusAreas = meteringAreas
                try {
                    camera.parameters = parameters
                } catch (ignored: RuntimeException) {
                }
                val myScheduledExecutorService = Executors.newScheduledThreadPool(1)
                myScheduledExecutorService.schedule({ camera.autoFocus(callback) }, 500, TimeUnit.MILLISECONDS)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            val targetFocusRect = calculateFocusArea(x, y, width, height)
            focusOnTouch(targetFocusRect, mAutoFocusTakePictureCallback)
        }
        return false
    }

    private val mAutoFocusTakePictureCallback = AutoFocusCallback { success, camera ->
        if (success) {
            isAutoFocusing = false
        }
    }

    init {
        mHolder = holder
        mHolder.addCallback(this)
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        mRotation = cameraDisplayOrientation
    }
}