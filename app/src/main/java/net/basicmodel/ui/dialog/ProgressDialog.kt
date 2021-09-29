package net.basicmodel.ui.dialog

import android.content.Context
import net.basicmodel.ui.dialog.BaseDialog
import net.basicmodel.R

class ProgressDialog(context: Context?) : BaseDialog<ProgressDialog>(context) {

    init {
        setCancelable(false)
    }

    override val layout: Int
        get() = R.layout.dialog_progress
}