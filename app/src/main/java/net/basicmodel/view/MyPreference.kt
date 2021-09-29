package net.basicmodel.view

import android.content.Context
import android.preference.Preference
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.widget.AppCompatSeekBar
import net.basicmodel.interfaces.ISeekBar
import net.basicmodel.R
import net.basicmodel.view.MyPreference
import android.view.ViewGroup
import android.widget.LinearLayout
import android.view.ViewParent
import android.widget.SeekBar
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.lang.Exception
import kotlin.math.log

open class MyPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    Preference(context, attrs, defStyleAttr), OnSeekBarChangeListener {
    private var maxValue = 255
    private var minValue = 0
    private var interval = 1
    private var currentValue = 0
    private var seekBar: AppCompatSeekBar? = null

    var iSeekBar: ISeekBar? = null

    init {
        initPreference(context, attrs)
    }

    private fun initPreference(context: Context, attrs: AttributeSet?) {
        setValuesFromXml(attrs)
        seekBar = AppCompatSeekBar(context, attrs)
        seekBar!!.max = maxValue
        seekBar!!.setOnSeekBarChangeListener(this)
        widgetLayoutResource = R.layout.preference_seek_bar
    }

    private fun setValuesFromXml(attrs: AttributeSet?) {
        attrs?.let {
            maxValue = attrs.getAttributeIntValue(ANDROID_SCHEMAS, "max", DEFAULT_VALUE)
            minValue = attrs.getAttributeIntValue(APPLICATION, "min", 0)
            try {
                val newInterval = attrs.getAttributeValue(APPLICATION, "interval")
                if (newInterval != null) interval = newInterval.toInt()
            } catch (ignored: Exception) {
            }
        }
    }

    override fun onCreateView(parent: ViewGroup?): View {
        val view = super.onCreateView(parent)
        val layout = view as LinearLayout
        layout.orientation = LinearLayout.VERTICAL
        return view
    }

    public override fun onBindView(view: View?) {
        super.onBindView(view)
        val newContainer = view?.findViewById<View>(R.id.seekBarPrefBarContainer) as ViewGroup
        val oldContainer = seekBar!!.parent
        try {
            if (oldContainer !== newContainer) {
                if (oldContainer != null) {
                    (oldContainer as ViewGroup).removeView(seekBar)
                }
                newContainer.removeAllViews()
                newContainer.addView(
                    seekBar, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        } catch (ignored: Exception) {
        }
        if (!view.isEnabled) {
            seekBar!!.isEnabled = false
        }
        updateView()
    }

    private fun updateView() {
        try {
            seekBar!!.progress = currentValue
            iSeekBar?.onProgress(currentValue)
        } catch (ignored: Exception) {
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        var newValue = progress + minValue
        if (newValue > maxValue) newValue = maxValue else if (newValue < minValue) newValue =
            minValue else if (interval != 1 && newValue % interval != 0) newValue = Math.round(
            newValue.toFloat() / interval
        ) * interval
        if (!callChangeListener(newValue)) {
            seekBar?.progress = currentValue - minValue
            return
        }
        currentValue = newValue
        persistInt(newValue)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        updateView()
        notifyChanged()
    }

    override fun onGetDefaultValue(ta: TypedArray?, index: Int): Any {
        return ta?.getInt(index, DEFAULT_VALUE) ?: 0
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        if (restoreValue) {
            currentValue = getPersistedInt(currentValue)
        } else {
            var temp = 0
            try {
                temp = defaultValue as Int
            } catch (ignored: Exception) {
            }
            persistInt(temp)
            currentValue = temp
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        seekBar!!.isEnabled = enabled
    }

    override fun onDependencyChanged(dependency: Preference?, disableDependent: Boolean) {
        super.onDependencyChanged(dependency, disableDependent)
        if (seekBar != null) {
            seekBar!!.isEnabled = !disableDependent
        }
    }

    companion object {
        private const val ANDROID_SCHEMAS = "http://schemas.android.com/apk/res/android"
        private const val APPLICATION = "smobile"
        private const val DEFAULT_VALUE = 100
    }
}