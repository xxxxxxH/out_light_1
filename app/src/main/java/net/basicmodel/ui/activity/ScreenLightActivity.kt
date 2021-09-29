package net.basicmodel.ui.activity

import net.basicmodel.manager.MySoundManager.clickSound
import net.basicmodel.view.tooltip.Tooltip.make
import androidx.fragment.app.FragmentActivity
import net.basicmodel.interfaces.IScreen
import android.widget.SeekBar.OnSeekBarChangeListener
import net.basicmodel.utils.SaveUtils
import net.basicmodel.constants.ScreenLightEnum
import net.basicmodel.ui.fragment.ScreenColorFragment
import android.widget.SeekBar
import android.os.Bundle
import android.view.View
import net.basicmodel.manager.MyNotificationManager
import android.view.WindowManager
import net.basicmodel.impl.DoubleOnClickListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.screen_light_activity.*
import net.basicmodel.*
import net.basicmodel.constants.Constants
import net.basicmodel.view.tooltip.Tooltip

class ScreenLightActivity : FragmentActivity(), View.OnClickListener, IScreen, OnSeekBarChangeListener {

    private var isFromWidget = false
    private var enumData = ScreenLightEnum.LIGHT

    protected fun setFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.addToBackStack(fragment.tag)
        fragmentTransaction.replace(android.R.id.content, fragment)
        fragmentTransaction.commit()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.screenlight_img_back -> {
                if (SaveUtils.getBoolean(Constants.ENABLE_SOUND_KEY)) {
                    clickSound()
                }
                finish()
                if (!isFromWidget) {
                    overridePendingTransition(R.anim.activity_fadein, R.anim.activity_fadeout)
                }
            }
            R.id.screenlight_img_power -> {
                showTooltips(screenlight_guide, getString(R.string.tips_how_to_exit))
                if (screenlight.visibility == View.GONE) {
                    when (enumData) {
                        ScreenLightEnum.SOS -> startSOSLight(this)
                        ScreenLightEnum.COLOR -> setFragment(ScreenColorFragment())
                        else -> openLightScreen()
                    }
                    screenlight.visibility = View.VISIBLE
                    screenlight_btn_sos.visibility = View.GONE
                    screenlight_btn_color.visibility = View.GONE
                } else {
                    screenlight.visibility = View.GONE
                    screenlight_btn_sos.visibility = View.VISIBLE
                    screenlight_btn_color.visibility = View.VISIBLE
                }
            }
            R.id.screenlight_btn_sos -> if (!screenlight_btn_sos.isSelected) {
                screenlight_btn_sos.isSelected = true
                screenlight_btn_color.isSelected = false
                enumData = ScreenLightEnum.SOS
            } else {
                screenlight_btn_sos.isSelected = false
                enumData = ScreenLightEnum.LIGHT
            }
            R.id.screenlight_btn_color -> if (!screenlight_btn_color.isSelected) {
                screenlight_btn_color.isSelected = true
                screenlight_btn_sos.isSelected = false
                enumData = ScreenLightEnum.COLOR
            } else {
                screenlight_btn_color.isSelected = false
                enumData = ScreenLightEnum.LIGHT
            }
            else -> {
            }
        }
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.activity_fadein, R.anim.activity_fadeout)
        setContentView(R.layout.screen_light_activity)
        initView()
        val bundle = intent.extras
        if (bundle != null) {
            isFromWidget = bundle.getBoolean("ACTION_LIGHT_WIDGET", false)
            openLightScreen()
        }
        if (MyNotificationManager.NOTIFICATION_ACTION_SCREEN_KEY == intent.action) {
            openLightScreen()
        }
    }

    override fun onStart() {
        super.onStart()
        if (SaveUtils.getBoolean(Constants.SCREEN_KEY)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progresValue: Int, b: Boolean) {
        var progresValue = progresValue
        if (progresValue <= 10) {
            progresValue = 10
        }
        val backLightValue = progresValue.toFloat() / 100
        val layoutParams = window.attributes
        layoutParams.screenBrightness = backLightValue
        window.attributes = layoutParams
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    private fun initView() {
        screenlight_sb_brightestlevel.setOnSeekBarChangeListener(this)
        screenlight_sb_brightestlevel.progress = 100
        screenlight_img_power.setOnClickListener(this)
        screenlight_img_back.setOnClickListener(this)
        screenlight_btn_sos.setOnClickListener(this)
        screenlight_btn_color.setOnClickListener(this)
        screenlight.setOnClickListener(object : DoubleOnClickListener() {
            override fun onSingleClick(v: View?) {}
            override fun onDoubleClick(v: View?) {
                if (enumData === ScreenLightEnum.SOS) {
                    interceptSOSLight = true
                    stopSOSLight()
                }
                screenlight_btn_color.visibility = View.VISIBLE
                screenlight_btn_sos.visibility = View.VISIBLE
                screenlight.setVisibility(View.GONE)
            }
        })
    }

    private fun showTooltips(v: View, content: String) {
        make(
            this,
            Tooltip.Builder(102)
                .anchor(v, Tooltip.Gravity.BOTTOM)
                .closePolicy(Tooltip.ClosePolicy.TOUCH_ANYWHERE_NO_CONSUME, 3000)
                .text(content)
                .fadeDuration(200)
                .fitToScreen(true)
                .maxWidth(dpToPx(310))
                .showDelay(200)
                .withArrow(false)
                .withOverlay(false)
                .build()
        ).show()
    }

    override fun onBackPressed() {
        when (enumData) {
            ScreenLightEnum.COLOR -> {
                supportFragmentManager.popBackStack()
                screenlight.visibility = View.GONE
                screenlight_btn_sos.visibility = View.VISIBLE
                screenlight_btn_color.visibility = View.VISIBLE
            }
            ScreenLightEnum.SOS -> {
                screenlight.visibility = View.GONE
                screenlight_btn_sos.visibility = View.VISIBLE
                screenlight_btn_color.visibility = View.VISIBLE
                interceptSOSLight = true
                stopSOSLight()
            }
            else -> if (screenlight.visibility == View.VISIBLE) {
                screenlight.visibility = View.GONE
                screenlight_btn_sos.visibility = View.VISIBLE
                screenlight_btn_color.visibility = View.VISIBLE
            } else {
                if (!isFromWidget) {
                    overridePendingTransition(R.anim.activity_fadein, R.anim.activity_fadeout)
                }
                super.onBackPressed()
            }
        }
    }

    override fun onScreenChange(color: Int) {
        runOnUiThread {
            screenlight.setBackgroundColor(color)
            screenlight_btn_sos.visibility = View.GONE
            screenlight_btn_color.visibility = View.GONE
        }
    }

    override fun openLightScreen() {
        showTooltips(screenlight_guide, getString(R.string.tips_how_to_exit))
        screenlight.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
        screenlight.visibility = View.VISIBLE
        screenlight_btn_sos.visibility = View.GONE
        screenlight_btn_color.visibility = View.GONE
        enumData = ScreenLightEnum.LIGHT
    }
}