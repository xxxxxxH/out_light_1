package net.basicmodel.ui.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import net.basicmodel.R
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.toolbar_layout.*
import net.basicmodel.ui.fragment.ConfigFragment

class ConfigActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setting_activity)
        toolbar.title = getString(R.string.st_title)
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        toolbar.setNavigationOnClickListener { finish() }
        fragmentManager.beginTransaction().replace(
            R.id.fragment_container,
            ConfigFragment()
        ).commit()
    }
}