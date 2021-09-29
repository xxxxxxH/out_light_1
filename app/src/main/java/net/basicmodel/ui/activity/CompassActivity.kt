package net.basicmodel.ui.activity

import net.basicmodel.manager.MySoundManager.clickSound
import android.app.Activity
import android.widget.SeekBar.OnSeekBarChangeListener
import net.basicmodel.interfaces.ICompass
import net.basicmodel.impl.MyEventListener.CompassAssistantListener
import net.basicmodel.view.MySurfaceView
import android.os.Bundle
import android.view.WindowManager
import net.basicmodel.app.App
import android.graphics.Bitmap
import android.view.animation.RotateAnimation
import android.os.AsyncTask
import android.content.Intent
import android.hardware.Camera
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import net.basicmodel.ui.dialog.SimpleDialog
import android.widget.SeekBar
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.android.synthetic.main.glass_activity.*
import net.basicmodel.*
import net.basicmodel.constants.Constants
import net.basicmodel.impl.MyEventListener
import net.basicmodel.impl.TakePictureCallback
import net.basicmodel.ui.dialog.ProgressDialog
import net.basicmodel.utils.SaveUtils
import java.lang.Exception

class CompassActivity : Activity(), View.OnClickListener, OnSeekBarChangeListener, ICompass, CompassAssistantListener {

    private var surfaceView: MySurfaceView? = null
    private var myCamera: Camera? = null
    private var params: Camera.Parameters? = null
    private var mRotation = 0
    private var callBack: TakePictureCallback? = null
    private var myEventListener: MyEventListener? = null
    private var currentDegree = 0f
    private var isStopped = false
    private var currentId = Camera.CameraInfo.CAMERA_FACING_BACK

    var isFlashButtonSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        myEventListener = MyEventListener(this)
        if (hasCameraPermissions(this)) {
            MyTask().execute()
        } else {
            requestPermission()
        }
    }

    private fun requestPermission() {
        requestCameraPermission(this, object : OnPermissionCallback {
            override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                MyTask().execute()
            }

            override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                if (never) {
                    showExplainCameraPermission(Permission.CAMERA)
                } else {
                    SimpleDialog(this@CompassActivity)
                        .setTopColorRes(R.color.darkBlueGrey)
                        .setIcon(android.R.drawable.stat_sys_warning)
                        .setTitle(R.string.warning_title)
                        .setMessage(R.string.msg_explain_permission_camera_for_glass)
                        .setCancelable(false)
                        .setYesButton(R.string.string_ok) { finish() }
                        .setNoButton(R.string.button_retry) {
                            requestPermission()
                        }.show()
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (SaveUtils.getBoolean(Constants.SCREEN_KEY)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun turnOnFlash() {
        if (!App.isFlashOpen) {
            if (myCamera == null) {
                return
            }
            params = myCamera!!.parameters
            params?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
            myCamera!!.parameters = params
            App.isFlashOpen = true
        }
    }

    override fun turnOffFlash() {
        if (App.isFlashOpen) {
            if (myCamera == null || params == null) {
                return
            }
            params = myCamera!!.parameters
            params?.flashMode = Camera.Parameters.FLASH_MODE_OFF
            myCamera!!.parameters = params
            App.isFlashOpen = false
        }
    }

    override fun updateImageForGalleryView(bitmap: Bitmap?) {
        bitmap?.let {
            thumbnail.setImageBitmap(bitmap)
        }
    }

    override fun onResume() {
        super.onResume()
        if (myEventListener != null) {
            if (myEventListener!!.isSupported) {
                myEventListener!!.start()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (myEventListener != null) {
            myEventListener!!.stop()
        }
    }

    override fun onNewDegreesToNorth(degrees: Float) {}
    override fun onNewSmoothedDegreesToNorth(degrees: Float) {
        val ra = RotateAnimation(
            currentDegree,
            degrees,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        ra.duration = 210
        ra.fillAfter = true
        runOnUiThread {
            if (!isStopped) {
                imageViewCompass.startAnimation(ra)
            }
        }
        currentDegree = degrees
    }

    override fun onCompassStopped() {
        isStopped = true
        imageViewCompass.clearAnimation()
        imageViewCompass.visibility = View.GONE
        glass_activity_txt_true_north.visibility = View.GONE
    }

    override fun onCompassStarted() {
        isStopped = false
        imageViewCompass.visibility = View.VISIBLE
        glass_activity_txt_true_north.visibility = View.VISIBLE
    }

    override fun updateBearingText(bearing: String?) {
        glass_activity_txt_true_north?.text = bearing
    }

    private inner class MyTask : AsyncTask<String?, Int?, Boolean?>() {
        var pd: ProgressDialog? = null
        override fun onPreExecute() {
            pd = ProgressDialog(this@CompassActivity)
            pd!!.setTopColorRes(R.color.darkBlueGrey)
            pd!!.setIcon(R.drawable.ic_progress)
            pd!!.setTitle(getString(R.string.msg_loading))
            pd!!.setCancelable(false)
            pd!!.show()
        }

        override fun onPostExecute(result: Boolean?) {
            if (pd != null) {
                pd!!.dismiss()
            }
            setContentView(R.layout.glass_activity)
            initView()
            surfaceView = MySurfaceView(this@CompassActivity, this@CompassActivity, myCamera)
            flPreview.addView(surfaceView)
            mRotation = surfaceView!!.cameraDisplayOrientation
            callBack = TakePictureCallback(this@CompassActivity, myCamera!!, this@CompassActivity)
            callBack!!.setGlassViewInterface(this@CompassActivity)
            callBack!!.setView(btnFlash)
            callBack!!.setRotation(mRotation)
            if (myEventListener!!.isSupported) {
                myEventListener!!.addListener(this@CompassActivity)
            } else {
                imageViewCompass.visibility = View.GONE
                glass_activity_txt_true_north.visibility = View.GONE
                btn_compass.visibility = View.GONE
            }
        }

        override fun doInBackground(vararg params: String?): Boolean? {
            myCamera = cameraInstance
            return true
        }
    }

    fun initView() {
        sbZoomCamera.max = maxZoomCamera
        btnBack.setOnClickListener(this)
        btnFlash.setOnClickListener(this)
        btnCaptureImage.setOnClickListener(this)
        sbZoomCamera.setOnSeekBarChangeListener(this)
        thumbnail.setOnClickListener(this)
        btn_compass.setOnClickListener(this)
        btn_rotate_camera.setOnClickListener(this)
        showCompass()
    }

    private fun showCompass() {
        myEventListener!!.start()
        btn_compass.isSelected = true
    }

    private fun hideCompass() {
        myEventListener!!.stop()
        btn_compass.isSelected = false
    }

    val maxZoomCamera: Int
        get() = if (myCamera != null) {
            val parameters = myCamera!!.parameters
            parameters.maxZoom
        } else {
            10
        }

    fun zoom(zoomSize: Int) {
        if (myCamera != null) {
            val parameters = myCamera!!.parameters
            val size = parameters.maxZoom
            if (zoomSize < size) {
                parameters.zoom = zoomSize
                myCamera!!.parameters = parameters
            }
        }
    }

    private val cameraInstance: Camera?
        private get() {
            var c: Camera? = null
            try {
                c = Camera.open(currentId)
            } catch (e: Exception) {
            }
            return c
        }

    private fun releaseCamera() {
        val handler = Handler()
        handler.post {
            if (myCamera != null) {
                myCamera!!.stopPreview()
                myCamera!!.setPreviewCallback(null)
                myCamera!!.release()
                surfaceView!!.isPreview = false
                App.isFlashOpen = false
                myCamera = null
            }
        }
    }

    override fun onBackPressed() {
        releaseCamera()
        super.onBackPressed()
    }

    override fun onClick(v: View) {
        if (SaveUtils.getBoolean(Constants.ENABLE_SOUND_KEY)) {
            clickSound()
        }
        when (v.id) {
            R.id.btnBack -> {
                releaseCamera()
                finish()
            }
            R.id.btnFlash -> if (isFlashButtonSelected) {
                isFlashButtonSelected = false
                turnOffFlash()
                btnFlash.isSelected = false
            } else {
                isFlashButtonSelected = true
                turnOnFlash()
                btnFlash.isSelected = true
            }
            R.id.btnCaptureImage -> if (!hasStoragePermissions(this)) {
                requestStoragePermissionInner()
            } else {
                myCamera!!.takePicture(null, null, callBack)
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            R.id.thumbnail -> {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_VIEW
                startActivity(intent)
            }
            R.id.btn_compass -> if (btn_compass.isSelected) {
                hideCompass()
            } else {
                showCompass()
            }
            R.id.btn_rotate_camera -> {
                if (surfaceView!!.isPreview) {
                    surfaceView!!.surfaceDestroyed(surfaceView!!.holder)
                    surfaceView!!.holder.removeCallback(surfaceView)
                    surfaceView!!.destroyDrawingCache()
                    flPreview.removeView(surfaceView)
                    myCamera!!.stopPreview()
                    myCamera!!.setPreviewCallback(null)
                    myCamera!!.release()
                    surfaceView!!.isPreview = false
                }
                if (currentId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    currentId = Camera.CameraInfo.CAMERA_FACING_FRONT
                    btnFlash.visibility = View.INVISIBLE
                } else {
                    currentId = Camera.CameraInfo.CAMERA_FACING_BACK
                    btnFlash.visibility = View.VISIBLE
                }
                myCamera = cameraInstance
                surfaceView = MySurfaceView(this, this, myCamera)
                surfaceView!!.setCurrentCameraId(currentId)
                flPreview.addView(surfaceView)
                mRotation = surfaceView!!.cameraDisplayOrientation
                callBack!!.setMyCamera(myCamera!!)
                callBack!!.setCameraId(currentId)
                callBack!!.setRotation(mRotation)
                sbZoomCamera.progress = 0 // reset Seekbar
            }
            else -> {
            }
        }
    }

    private fun requestStoragePermissionInner() {
        requestStoragePermission(this, object : OnPermissionCallback {
            override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                myCamera!!.takePicture(null, null, callBack)
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }

            override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                if (never) {
                    showExplainCameraPermission(Permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    SimpleDialog(this@CompassActivity)
                        .setTopColorRes(R.color.darkBlueGrey)
                        .setIcon(android.R.drawable.stat_sys_warning)
                        .setTitle(R.string.warning_title)
                        .setMessage(R.string.msg_explain_permission_storage)
                        .setCancelable(false)
                        .setYesButton(R.string.string_ok, null)
                        .setNoButton(R.string.button_retry) {
                            requestStoragePermissionInner()
                        }.show()
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == XXPermissions.REQUEST_CODE) {
            if (hasCameraPermissions(this)) {
                MyTask().execute()
            } else {
                showExplainCameraPermission(Permission.CAMERA)
            }
        }
    }

    private fun showExplainCameraPermission(permission: String) {
        SimpleDialog(this)
            .setTopColorRes(R.color.darkBlueGrey)
            .setIcon(android.R.drawable.stat_sys_warning)
            .setTitle(R.string.warning_title)
            .setMessage(if (permission == Permission.CAMERA) R.string.msg_explain_permission_camera_for_glass else R.string.msg_explain_permission_storage)
            .setCancelable(false)
            .setYesButton(R.string.string_ok) { finish() }
            .setNoButton(R.string.button_retry) {
                jumpToAppPermissionDetail(this, permission)
            }.show()
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        zoom(progress)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            releaseCamera()
            finish()
        }
        return true
    }

    override fun onDestroy() {
        releaseCamera()
        super.onDestroy()
    }
}