package net.basicmodel.ui.fragment

import net.basicmodel.utils.ToastUtils.showToast
import android.preference.PreferenceFragment
import android.preference.Preference.OnPreferenceClickListener
import net.basicmodel.interfaces.ISeekBar
import android.preference.CheckBoxPreference
import android.os.Bundle
import net.basicmodel.R
import android.preference.Preference
import net.basicmodel.ui.dialog.WebDialog
import android.os.Build
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import net.basicmodel.BuildConfig
import net.basicmodel.constants.Constants
import net.basicmodel.getVersionName
import net.basicmodel.utils.SaveUtils
import net.basicmodel.view.MyPreference

class ConfigFragment : PreferenceFragment(), OnPreferenceClickListener, ISeekBar {

    private var checkBoxPreference: CheckBoxPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.sharedPreferencesName = Constants.FLASH_LIGHT_NAME
        addPreferencesFromResource(R.xml.settings)
        initUI()
    }

    private fun initUI() {
        val preference = findPreference(Constants.APP_VERSION_KEY)
        val myPreference = findPreference(Constants.BATTERY_KEY) as MyPreference
        checkBoxPreference = findPreference(Constants.IN_SAVE_MODE_KEY) as CheckBoxPreference
        preference.summary = getVersionName(activity)
        myPreference.iSeekBar = this
        checkBoxPreference?.summary = getString(
            R.string.st_about_app_power_saving_mode_description,
            SaveUtils.getInt(Constants.BATTERY_KEY)
        )
    }

    val deviceName: String
        get() {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.startsWith(manufacturer)) {
                capitalize(model)
            } else {
                capitalize(manufacturer) + " " + model
            }
        }

    private fun capitalize(s: String?): String {
        if (s == null || s.isEmpty()) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first).toString() + s.substring(1)
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val key = preference.key
        if (Constants.GOOGLE_KEY == key) {
            val url = "https://plus.google.com/b/115825835888801940774/115825835888801940774/about"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
            return true
        } else if (Constants.FEEDBACK_KEY == key) {
            val feedbackIntent = Intent(
                Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", "anhson.duong@gmail.com", null
                )
            )
            feedbackIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                getString(R.string.app_name) + " ("
                        + getVersionName(activity) + "|"
                        + deviceName + "): "
                        + getString(R.string.about_app_feedback_title)
            )
            try {
                this.startActivity(Intent.createChooser(feedbackIntent, getString(R.string.feedback) + "..."))
            } catch (ex: ActivityNotFoundException) {
                showToast(activity, getString(R.string.app_feedback_exception_no_app_handle))
            }
            return true
        } else if (Constants.RATE_KEY == key) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$APP_NAME")))
        } else if (Constants.LICENSE_KEY == key) {
            WebDialog(activity).setTopColorRes(R.color.darkBlueGrey)
                .setIcon(R.drawable.ic_info_outline_white_36dp)
                .show()
        }
        return false
    }

    override fun onProgress(value: Int) {
        checkBoxPreference?.summary = getString(R.string.st_about_app_power_saving_mode_description, value)
    }

    companion object {
        private const val APP_NAME = BuildConfig.APPLICATION_ID
    }
}