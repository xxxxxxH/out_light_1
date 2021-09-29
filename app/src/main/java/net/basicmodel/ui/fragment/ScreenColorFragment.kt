package net.basicmodel.ui.fragment

import android.graphics.Color
import net.basicmodel.interfaces.IColor
import android.widget.RelativeLayout
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.color_screen_fragment.*
import net.basicmodel.R
import net.basicmodel.view.MyColorPicker
import net.basicmodel.impl.DoubleOnClickListener

class ScreenColorFragment : Fragment(), IColor {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.color_screen_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        root.setBackgroundColor(Color.WHITE)
        color_picker.SetIColor(this)
        root.setOnClickListener(object : DoubleOnClickListener() {
            override fun onSingleClick(v: View?) {}
            override fun onDoubleClick(v: View?) {
                activity?.onBackPressed()
            }
        })
    }

    override fun onColorChange(color: Int) {
        root.setBackgroundColor(color)
    }

    override fun onClick() {
        activity?.onBackPressed()
    }
}