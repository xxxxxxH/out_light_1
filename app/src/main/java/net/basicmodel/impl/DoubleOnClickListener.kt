package net.basicmodel.impl

import android.os.Handler
import android.view.View
import java.util.*

abstract class DoubleOnClickListener : View.OnClickListener {

    private var lastClickTime: Long = 0

    override fun onClick(v: View) {
        val clickTime = System.currentTimeMillis()
        if (clickTime - lastClickTime < 300) {
            handleDoubleClick(v)
        } else {
            handleSingleClick(v)
        }
        lastClickTime = clickTime
    }

    abstract fun onSingleClick(v: View?)
    abstract fun onDoubleClick(v: View?)

    private val timer by lazy {
        Timer()
    }

    private fun handleSingleClick(v: View?) {
        val handler = Handler()
        timer.schedule(object : TimerTask() {
            override fun run() {
                handler.post {
                    onSingleClick(v)
                }
            }
        }, 400L)
    }

    private fun handleDoubleClick(v: View?) {
        timer.cancel()
        timer.purge()
        onDoubleClick(v)
    }
}