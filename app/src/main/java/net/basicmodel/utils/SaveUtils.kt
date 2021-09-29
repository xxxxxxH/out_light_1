package net.basicmodel.utils

import android.content.Context
import android.content.SharedPreferences
import net.basicmodel.app
import net.basicmodel.constants.Constants
import net.basicmodel.utils.SaveUtils

object SaveUtils {

    private val sharedPreferences: SharedPreferences by lazy {
        app.getSharedPreferences(Constants.FLASH_LIGHT_NAME, Context.MODE_PRIVATE)
    }

    fun putBoolean(name: String?, value: Boolean) {
        sharedPreferences.edit().putBoolean(name, value).apply()
    }

    fun getBoolean(name: String?): Boolean {
        return if (Constants.ENABLE_SOUND_KEY.equals(name, ignoreCase = true)) {
            sharedPreferences.getBoolean(name, true)
        } else sharedPreferences.getBoolean(name, false)
    }

    fun getBoolean(name: String?, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(name, defaultValue)
    }

    fun getInt(name: String?): Int {
        return sharedPreferences.getInt(name, -1)
    }

    fun getInt(name: String?, defaultValue: Int): Int {
        return sharedPreferences.getInt(name, defaultValue)
    }
}