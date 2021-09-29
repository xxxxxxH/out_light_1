package net.basicmodel.ui.activity

import net.basicmodel.manager.MyCameraManager.Companion.instance
import net.basicmodel.manager.MyLightManager.Companion.getInstance
import net.basicmodel.manager.MySoundManager.clickSound
import net.basicmodel.utils.ToastUtils.showToast
import net.basicmodel.manager.MySoundManager.warningSound
import net.basicmodel.manager.MySoundManager.moveSound
import android.app.Activity
import net.basicmodel.interfaces.ISelect
import net.basicmodel.interfaces.IAction
import android.widget.TextView
import android.widget.FrameLayout
import net.basicmodel.manager.MyCameraManager
import net.basicmodel.manager.MyLightManager
import net.basicmodel.manager.MyNotificationManager
import android.os.Bundle
import android.content.Intent
import net.basicmodel.ui.dialog.SimpleDialog
import androidx.core.content.ContextCompat
import android.util.TypedValue
import android.view.animation.AnimationUtils
import android.content.IntentFilter
import net.basicmodel.app.App
import android.content.BroadcastReceiver
import android.content.Context
import android.graphics.Color
import android.os.BatteryManager
import android.view.*
import android.widget.Toast
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.android.synthetic.main.battery_information_layout.*
import kotlinx.android.synthetic.main.flashlight_activity.*
import net.basicmodel.*
import net.basicmodel.constants.Constants
import net.basicmodel.utils.SaveUtils
import net.basicmodel.view.MyLightView
import java.io.IOException
import java.lang.NullPointerException

class MainActivity : Activity(), View.OnClickListener, ISelect, IAction, SurfaceHolder.Callback {

    private var mContext: Context? = null
    private var number = 0
    private var myCameraManager: MyCameraManager? = null
    private var myLightManager: MyLightManager? = null
    private var thread: Thread? = null
    private var holder: SurfaceHolder? = null
    private var homePress = false
    private var flashOpenForHome = false
    private var iAction: IAction? = null
    private var myNotificationManager: MyNotificationManager? = null
    private var isStartUPSM = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        if (SaveUtils.getBoolean(Constants.ENABLE_UP_KEY)) {
            isStartUPSM = true
            startActivity(Intent(this, LowPowerActivity::class.java))
            finish()
            return
        }
        iAction = this
        setContentView(R.layout.flashlight_activity)
        myNotificationManager = MyNotificationManager(this)
        initView()
        initData()
        if (!isSupportFlashLight(this)) {
            sleep(300)
            showNoFlashAlert()
        } else {
            if (!hasCameraPermissions(this)) {
                requestPermission()
            } else {
                myCameraManager = instance
                myCameraManager!!.bindCamera()
                myLightManager = getInstance(myCameraManager)
                myLightManager!!.bindAction(this)
                actionAutoFlash()
            }
        }
        val bundle = intent.extras
        if (bundle != null) {
            val isWidgetTurnon = bundle.getBoolean("ACTION_TURN_ON", false)
            if (isWidgetTurnon) {
                activeFlashlight()
            }
        }
        if (MyNotificationManager.NOTIFICATION_ACTION_ON_KEY == intent.action) {
            activeFlashlight()
        }
        val HAVE_CONTROLLER_KEY = SaveUtils
            .getBoolean(Constants.HAVE_CONTROLLER_KEY, true)
        flashlight_btn_quick_controller.isSelected = HAVE_CONTROLLER_KEY
        if (HAVE_CONTROLLER_KEY) {
            myNotificationManager!!.view = flashlight_btn_quick_controller
            myNotificationManager!!.setShowTips(true)
            myNotificationManager!!.buildNotification()
        }
    }

    private fun requestPermission() {
        requestCameraPermission(this, object : OnPermissionCallback {
            override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                myCameraManager = instance
                myCameraManager!!.bindCamera()
                myLightManager = getInstance(myCameraManager)
                myLightManager!!.bindAction(this@MainActivity)
                holder = PREVIEW.holder
                holder?.addCallback(this@MainActivity)
                holder?.let {
                    surfaceCreated(it)
                }
                actionAutoFlash()
            }

            override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                if (never) {
                    showExplainPermission()
                } else {
                    SimpleDialog(this@MainActivity)
                        .setTopColorRes(R.color.darkBlueGrey)
                        .setIcon(R.drawable.ic_warning)
                        .setTitle(getString(R.string.warning_title))
                        .setMessage(getString(R.string.msg_explain_permission_camera))
                        .setCancelable(false)
                        .setYesButton(R.string.string_ok) {
                            startActivity(Intent(mContext, ScreenLightActivity::class.java))
                            finish()
                        }
                        .setNoButton(R.string.button_retry) {
                            requestPermission()
                        }
                        .show()
                }
            }
        })
    }

    private fun activeFlashlight() {
        try {
            flashlight_btn_power.isSelected = true
            if (number > 0) {
                changeMode(number)
            } else {
                myCameraManager!!.openFlashLight(iAction!!)
            }
            myLightManager!!.needTurnOffLight = false
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == XXPermissions.REQUEST_CODE) {
            if (hasCameraPermissions(this)) {
                myCameraManager = instance
                myCameraManager!!.bindCamera()
                myLightManager = getInstance(myCameraManager)
                myLightManager!!.bindAction(this)
                holder = PREVIEW.holder
                holder?.addCallback(this)
                holder?.let {
                    surfaceCreated(it)
                }
            } else {
                showExplainPermission()
            }
        }
    }

    private fun actionAutoFlash() {
        if (SaveUtils.getBoolean(Constants.ENABLE_AUTOMATIC_LED_KEY)) {
            myCameraManager!!.openFlashLight(this)
            flashlight_btn_power.isSelected = true
        }
    }

    private fun initView() {
        holder = PREVIEW.holder
        holder?.addCallback(this)
        holder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        flashlight_btn_power.setOnClickListener(this)
        flashlight_strobeview.setISelect(this)
        flashlight_txt_numberstrobe.setFactory {
            val myText = TextView(applicationContext)
            myText.gravity = Gravity.CENTER
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            myText.layoutParams = params
            myText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
            myText.setTextColor(Color.WHITE)
            myText
        }
        flashlight_txt_numberstrobe.inAnimation = AnimationUtils.loadAnimation(
            this,
            R.anim.push_up_in
        )
        flashlight_txt_numberstrobe.setOutAnimation(this, R.anim.push_up_out)
        flashlight_btn_screenlight.setOnClickListener(this)
        flashlight_btn_glass.setOnClickListener(this)
        flashlight_btn_setting.setOnClickListener(this)
        flashlight_btn_lock.setOnClickListener(this)
        flashlight_btn_quick_controller.setOnClickListener(this)
    }

    private fun changeMode(numberStrobe: Int) {
        myLightManager!!.delayTime = numberStrobe
        if (thread == null) {
            thread = Thread(myLightManager)
            thread!!.start()
        }
    }

    private fun initData() {
        flashlight_txt_numberstrobe.setText("" + number)
    }

    override fun onClick(v: View) {
        if (SaveUtils.getBoolean(Constants.ENABLE_SOUND_KEY)) {
            clickSound()
        }
        when (v.id) {
            R.id.flashlight_btn_power -> {
                if (SaveUtils.getBoolean(Constants.VIBRATION_KEY)) {
                    vibrate(this)
                }
                if (!flashlight_btn_power.isSelected) {
                    flashlight_btn_power.isSelected = true
                    if (number > 0) {
                        changeMode(number)
                    } else {
                        myCameraManager!!.openFlashLight(iAction!!)
                    }
                    myLightManager!!.needTurnOffLight = false
                } else {
                    flashlight_btn_power.isSelected = false
                    flashlight_btn_power.isPressed = true
                    myLightManager!!.needIntercept = true
                    myLightManager!!.needTurnOffLight = true
                    releaseStrobeMode()
                    myCameraManager!!.turnOffFlash(iAction!!)
                }
            }
            R.id.flashlight_btn_screenlight -> startActivity(Intent(this, ScreenLightActivity::class.java))
            R.id.flashlight_btn_glass -> {
                myLightManager!!.needIntercept = true
                myLightManager!!.needTurnOffLight = true
                releaseStrobeMode()
                myCameraManager!!.turnOffFlash(this)
                myCameraManager!!.resetCamera()
                flashlight_btn_power.isSelected = false
                flashlight_btn_indicator.isSelected = false
                startActivity(Intent(this, CompassActivity::class.java))
                overridePendingTransition(R.anim.activity_fadein, R.anim.activity_fadeout)
            }
            R.id.flashlight_btn_setting -> startActivity(Intent(this, ConfigActivity::class.java))
            R.id.flashlight_btn_lock -> {
                flashlight_btn_lock.isSelected = !flashlight_btn_lock.isSelected
                lockApp(flashlight_btn_lock.isSelected)
            }
            R.id.flashlight_btn_quick_controller -> if (flashlight_btn_quick_controller.isSelected) {
                SaveUtils.putBoolean(
                    Constants.HAVE_CONTROLLER_KEY, false
                )
                flashlight_btn_quick_controller.isSelected = false
                myNotificationManager!!.view = v
                myNotificationManager!!.setShowTips(false)
                myNotificationManager!!.hideNotification(MyNotificationManager.NOTIFICATION_CODE_FLASHLIGHT)
            } else {
                SaveUtils.putBoolean(
                    Constants.HAVE_CONTROLLER_KEY, true
                )
                flashlight_btn_quick_controller.isSelected = true
                myNotificationManager!!.view = v
                myNotificationManager!!.setShowTips(false)
                myNotificationManager!!.buildNotification()
            }
            else -> {
            }
        }
    }

    private fun releaseStrobeMode() {
        if (thread != null) thread!!.interrupt()
        thread = null
    }

    override fun onStart() {
        super.onStart()
        if (SaveUtils.getBoolean(Constants.SCREEN_KEY)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onResume() {
        super.onResume()
        if (myCameraManager != null && myCameraManager!!.camera == null) {
            myCameraManager = instance
            myCameraManager!!.bindCamera()
            myLightManager = getInstance(myCameraManager)
            myLightManager!!.bindAction(this)
        }
        if (homePress && flashOpenForHome) {
            homePress = false
            flashOpenForHome = false
            myCameraManager = instance
            myCameraManager!!.bindCamera()
            myLightManager = getInstance(myCameraManager)
            myLightManager!!.bindAction(this)
            if (number > 0) {
                changeMode(number)
            } else {
                myCameraManager!!.openFlashLight(this)
            }
            flashlight_btn_power.isSelected = true
            myLightManager!!.needTurnOffLight = false
        }
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        mContext!!.registerReceiver(PowerConnectionReceiver, iFilter)
    }

    override fun onStop() {
        if (App.isFlashOpen
            || number > 0 && flashlight_btn_power.isSelected
        ) {
            homePress = true
            flashOpenForHome = true
            if (myLightManager != null) {
                myLightManager!!.needIntercept = true
                myLightManager!!.needTurnOffLight = true
            }
            releaseStrobeMode()
            if (!SaveUtils.getBoolean(Constants.IN_SAVE_MODE_KEY)) {
                if (myCameraManager != null) {
                    myCameraManager!!.resetCamera()
                }
            }
        }
        unregisterReceiver(PowerConnectionReceiver)
        super.onStop()
    }

    override fun onDestroy() {
        if (myLightManager != null) {
            myLightManager!!.needIntercept = true
            myLightManager!!.needTurnOffLight = true
        }
        releaseStrobeMode()
        if (myCameraManager != null && !isStartUPSM) {
            myCameraManager!!.resetCamera()
            isStartUPSM = false
        }
        homePress = false
        super.onDestroy()
    }

    fun showNoFlashAlert() {
        val dialog = SimpleDialog(this)
        dialog.setTopColorRes(R.color.darkBlueGrey)
        dialog.setIcon(R.drawable.ic_warning)
        dialog.setTitle(getString(R.string.warning_title))
        dialog.setMessage(R.string.msg_warning_device_not_support_flashlight)
        dialog.setYesButton(getString(R.string.string_ok)) {
            finish()
            startActivity(Intent(mContext, ScreenLightActivity::class.java))
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun showExplainPermission() {
        SimpleDialog(this)
            .setTopColorRes(R.color.darkBlueGrey)
            .setIcon(R.drawable.ic_warning)
            .setTitle(getString(R.string.warning_title))
            .setMessage(getString(R.string.msg_explain_permission_camera))
            .setCancelable(false)
            .setYesButton(R.string.string_ok) {
                startActivity(Intent(mContext, ScreenLightActivity::class.java))
                finish()
            }
            .setNoButton(R.string.button_retry) {
                jumpToAppPermissionDetail(this, Permission.CAMERA)
            }
            .show()
    }

    override fun actionOn() {
        runOnUiThread { if (App.isFlashOpen) flashlight_btn_indicator.isSelected = true }
    }

    override fun actionOff() {
        runOnUiThread { if (!App.isFlashOpen) flashlight_btn_indicator.isSelected = false }
    }

    override fun actionFail() {
        runOnUiThread {
            myLightManager!!.needIntercept = true
            myLightManager!!.needTurnOffLight = true
            number = 0
            flashlight_strobeview.selectIndex(0)
            flashlight_txt_numberstrobe.setText("0")
            flashlight_btn_power.isSelected = false
            showToast(mContext, getString(R.string.error_flashlight_busy))
        }
        val intent = Intent(this, ScreenLightActivity::class.java)
        intent.putExtra("isFlashNotSupport", true)
        startActivity(intent)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        this.holder = holder
        if (myCameraManager != null && myCameraManager!!.camera != null) {
            try {
                myCameraManager!!.camera!!.setPreviewDisplay(holder)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (myCameraManager != null && myCameraManager!!.camera != null) {
            myCameraManager!!.camera!!.stopPreview()
            this.holder = null
        }
    }

    private fun lockApp(isLock: Boolean) {
        flashlight_fl_disableclick.visibility = if (isLock) View.VISIBLE else View.GONE
    }

    override fun onBackPressed() {
        if (SaveUtils.getBoolean(Constants.QUIT_KEY)) {
            SimpleDialog(this)
                .setTopColorRes(R.color.darkBlueGrey)
                .setIcon(R.drawable.ic_warning)
                .setTitle(getString(R.string.quit))
                .setMessage(getString(R.string.quit_confirm_message))
                .setYesButton(getString(R.string.string_exit)) { finish() }
                .setNoButton(getString(R.string.string_cancel), null)
                .show()
        } else {
            super.onBackPressed()
        }
    }

    private val PowerConnectionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val origin = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val percentage = origin / scale.toFloat()
            val level = (percentage * 100).toInt()
            battery_txt_percent.text = "$level%"
            if (hasCameraPermissions(mContext!!)
                && SaveUtils.getBoolean(Constants.IN_SAVE_MODE_KEY, true)
                && level <= SaveUtils.getInt(
                    Constants.BATTERY_KEY,
                    LowPowerActivity.DEFAULT_UP_DATA
                )
            ) {
                Toast.makeText(
                    mContext, mContext!!.getString(
                        R.string.upsm_msg_enable_mode,
                        SaveUtils.getInt(Constants.BATTERY_KEY)
                    ),
                    Toast.LENGTH_LONG
                ).show()

                if (SaveUtils.getBoolean(Constants.HAVE_CONTROLLER_KEY, true)) {
                    myNotificationManager!!.buildNotification(true)
                }
                SaveUtils.putBoolean(Constants.ENABLE_UP_KEY, true)
                isStartUPSM = true
                startActivity(Intent(mContext, LowPowerActivity::class.java))
                finish()
            }
            if (level <= 5) {
                battery_txt_percent.setTextColor(ContextCompat.getColor(context, R.color.red_btn_bg_color))
                val animation: Animation = AlphaAnimation(1F, 0F)
                animation.duration = 300
                animation.interpolator = LinearInterpolator()
                animation.repeatCount = Animation.INFINITE
                animation.repeatMode = Animation.REVERSE
                battery_img_icon.startAnimation(animation)
            } else {
                battery_txt_percent.setTextColor(ContextCompat.getColor(context, R.color.white))
                battery_img_icon.clearAnimation()
            }
            if (level == 5 || level == 4 || level == 3 || level == 2) {
                warningSound()
            }
        }
    }

    override fun onChanged(myLightView: MyLightView?, position: Int) {
        if (SaveUtils.getBoolean(Constants.ENABLE_SOUND_KEY)) {
            moveSound()
        }
        flashlight_txt_numberstrobe.setText("" + flashlight_strobeview.items!!.get(position))
    }

    override fun onSelected(myLightView: MyLightView?, position: Int) {
        number = (myLightView?.items?.get(position) ?: "0").toInt()
        if (flashlight_btn_power.isSelected) {
            if (number != 0) {
                changeMode(number)
            } else {
                myLightManager!!.needIntercept = true
                releaseStrobeMode()
                sleep(200)
                myCameraManager!!.openFlashLight(this)
            }
        }
    }
}