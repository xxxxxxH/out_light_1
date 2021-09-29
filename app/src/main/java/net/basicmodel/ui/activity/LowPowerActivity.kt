package net.basicmodel.ui.activity

import net.basicmodel.manager.MySoundManager.warningSound
import androidx.appcompat.app.AppCompatActivity
import net.basicmodel.interfaces.IAction
import net.basicmodel.interfaces.ITimer
import net.basicmodel.manager.MyCameraManager
import android.os.Bundle
import net.basicmodel.R
import androidx.core.content.ContextCompat
import net.basicmodel.app.App
import net.basicmodel.manager.MyNotificationManager
import android.content.IntentFilter
import android.content.Intent
import net.basicmodel.ui.dialog.SimpleDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.os.BatteryManager
import android.view.*
import kotlinx.android.synthetic.main.power_save_flashlight_activity.*
import net.basicmodel.constants.Constants
import net.basicmodel.countDownLastTime
import net.basicmodel.getLastTime
import net.basicmodel.utils.SaveUtils
import java.io.IOException

class LowPowerActivity : AppCompatActivity(), View.OnClickListener, IAction, SurfaceHolder.Callback, ITimer {

    private var tempStatus = 0
    private var tempTime: Long = 0
    private var myCameraManager: MyCameraManager? = null
    private var holder: SurfaceHolder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.power_save_flashlight_activity)
        toolbar.title = getString(R.string.upsm_toolbar_title)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        setSupportActionBar(toolbar)
        initUI()
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
        myCameraManager = MyCameraManager.instance
        myCameraManager!!.bindCamera()
        if (App.isFlashOpen
            || MyNotificationManager.NOTIFICATION_ACTION_ON_KEY == intent.action
        ) {
            myCameraManager!!.openFlashLight(this)
            upsm_btn_power.isSelected = true
        }
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        this.registerReceiver(PowerConnectionReceiver, iFilter)
    }

    override fun onStop() {
        unregisterReceiver(PowerConnectionReceiver)
        if (App.isFlashOpen && !SaveUtils.getBoolean(Constants.ENABLE_AUTOMATIC_LED_KEY)) {
            if (myCameraManager != null) {
                myCameraManager!!.turnOffFlash(this)
                upsm_btn_power.isSelected = false
            }
        }
        super.onStop()
    }

    private fun initUI() {
        holder = PREVIEW.holder
        holder?.addCallback(this)
        upsm_btn_power.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.upsm_btn_power -> if (!upsm_btn_power.isSelected) {
                upsm_btn_power.isSelected = true
                myCameraManager!!.openFlashLight(this)
            } else {
                upsm_btn_power.isSelected = false
                myCameraManager!!.turnOffFlash(this)
            }
        }
    }

    override fun actionOn() {}
    override fun actionOff() {}
    override fun actionFail() {
        val dialog = SimpleDialog(this)
        dialog.setTopColorRes(R.color.darkBlueGrey)
        dialog.setIcon(R.drawable.ic_warning)
        dialog.setTitle(getString(R.string.warning_title))
        dialog.setMessage(R.string.msg_warning_device_not_support_flashlight)
        dialog.setYesButton(getString(R.string.string_ok)) { finish() }
        dialog.setCancelable(false)
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.in_save_mode_key, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.disable_mode_menu -> {
                SaveUtils.putBoolean(Constants.IN_SAVE_MODE_KEY, false)
                if (App.isFlashOpen && !SaveUtils
                        .getBoolean(Constants.ENABLE_AUTOMATIC_LED_KEY)
                ) {
                    myCameraManager!!.turnOffFlash(this)
                }
                SaveUtils.putBoolean(Constants.ENABLE_UP_KEY, false)
                finish()
                startActivity(Intent(this, MainActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        holder = surfaceHolder
        if (myCameraManager != null && myCameraManager!!.camera != null) {
            try {
                myCameraManager!!.camera!!.setPreviewDisplay(holder)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        if (myCameraManager != null && myCameraManager!!.camera != null) {
            myCameraManager!!.camera!!.stopPreview()
            holder = null
        }
    }

    override fun onBackPressed() {
        if (myCameraManager != null) {
            myCameraManager!!.resetCamera()
        }
        super.onBackPressed()
    }

    private val PowerConnectionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val defaultLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val percentage = defaultLevel / scale.toFloat()
            val level = (percentage * 100).toInt()
            upsm_txt_battery_percent.text = "$level%"
            val time1 = System.currentTimeMillis()
            getLastTime(this@LowPowerActivity, level, tempStatus, time1, tempTime)
            tempStatus = level
            tempTime = System.currentTimeMillis()
            if (level == 5 || level == 4 || level == 3 || level == 2) {
                warningSound()
            }
        }
    }

    override fun onTimeChange(time: Long) {
        upsm_txt_usage_time_percent.text = countDownLastTime(time)
    }

    companion object {
        const val DEFAULT_UP_DATA = 20
    }
}