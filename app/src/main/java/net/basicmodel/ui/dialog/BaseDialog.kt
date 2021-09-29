package net.basicmodel.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import net.basicmodel.ui.dialog.BaseDialog
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.annotation.DrawableRes
import androidx.annotation.ColorInt
import net.basicmodel.R
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.annotation.LayoutRes

abstract class BaseDialog<T : BaseDialog<T>>(context: Context?) {
    private var iconView: ImageView? = null
    private var contentView: TextView? = null
    private var messageView: TextView? = null
    private var dialog: Dialog? = null

    private var view: View? = null

    protected fun string(@StringRes res: Int): String {
        return view?.context?.getString(res) ?: ""
    }

    protected fun color(@ColorRes colorRes: Int): Int {
        return context?.let {
            ContextCompat.getColor(it, colorRes)
        } ?: 0
    }

    protected val context: Context?
        get() = view?.context

    protected fun <K : View?> findView(id: Int): K {
        return view!!.findViewById<View>(id) as K
    }

    protected open inner class AutoDismissClickListener(
        private val clickListener: View.OnClickListener?,
        private val autoDismiss: Boolean
    ) : View.OnClickListener {
        override fun onClick(v: View) {
            clickListener?.onClick(v)
            if (autoDismiss) {
                dismiss()
            }
        }
    }

    fun setIcon(@DrawableRes iconRes: Int): T {
        iconView!!.visibility = View.VISIBLE
        iconView!!.setImageResource(iconRes)
        return this as T
    }

    private fun setTopColor(@ColorInt topColor: Int): T {
        findView<View>(R.id.ld_color_area).setBackgroundColor(topColor)
        return this as T
    }

    fun setTopColorRes(@ColorRes topColoRes: Int): T {
        return setTopColor(color(topColoRes))
    }

    fun setCancelable(cancelable: Boolean): T {
        dialog!!.setCancelable(cancelable)
        return this as T
    }

    fun show(): Dialog? {
        dialog!!.show()
        return dialog
    }

    fun dismiss() {
        dialog!!.dismiss()
    }

    private fun init(dialogBuilder: AlertDialog.Builder) {
        view = LayoutInflater.from(dialogBuilder.context).inflate(layout, null)
        dialog = dialogBuilder.setView(view).create()
        iconView = findView<ImageView>(R.id.ld_icon)
        contentView = findView<TextView>(R.id.ld_title)
        messageView = findView<TextView>(R.id.ld_message)
    }

    @get:LayoutRes
    protected abstract val layout: Int

    fun setMessage(@StringRes message: Int): T {
        return setMessage(string(message))
    }

    fun setTitle(@StringRes title: Int): T {
        return setTitle(string(title))
    }

    fun setTitle(title: CharSequence?): T {
        contentView!!.visibility = View.VISIBLE
        contentView!!.text = title
        return this as T
    }

    fun setMessage(message: CharSequence?): T {
        messageView!!.visibility = View.VISIBLE
        messageView!!.text = message
        return this as T
    }

    init {
        init(AlertDialog.Builder(context))
    }
}