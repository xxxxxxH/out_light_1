package net.basicmodel.ui.dialog

import android.content.Context
import android.view.View
import android.widget.Button
import net.basicmodel.ui.dialog.BaseDialog
import net.basicmodel.ui.dialog.SimpleDialog
import net.basicmodel.R
import androidx.annotation.StringRes
import net.basicmodel.ui.dialog.BaseDialog.AutoDismissClickListener

class SimpleDialog(context: Context?) : BaseDialog<SimpleDialog>(context) {

    private var yesButton: Button? = null
    private var noButton: Button? = null

    fun setYesButton(@StringRes text: Int, listener: View.OnClickListener?): SimpleDialog {
        return setYesButton(string(text), listener)
    }

    fun setYesButton(text: String?, listener: View.OnClickListener?): SimpleDialog {
        yesButton?.visibility = View.VISIBLE
        yesButton?.text = text
        yesButton?.setOnClickListener(AutoDismissClickListener(listener, true))
        return this
    }

    fun setNoButton(@StringRes text: Int, listener: View.OnClickListener?): SimpleDialog {
        return setNoButton(string(text), listener)
    }

    fun setNoButton(text: String?, listener: View.OnClickListener?): SimpleDialog {
        noButton?.visibility = View.VISIBLE
        noButton?.text = text
        noButton?.setOnClickListener(AutoDismissClickListener(listener, true))
        return this
    }

    init {
        yesButton = findView(R.id.ld_btn_yes)
        noButton = findView(R.id.ld_btn_no)
    }

    override val layout: Int
        get() = R.layout.dialog_standard
}