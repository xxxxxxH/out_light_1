package net.basicmodel.ui.dialog

import android.content.Context
import net.basicmodel.ui.dialog.BaseDialog
import net.basicmodel.ui.dialog.WebDialog
import android.webkit.WebViewClient
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.widget.Button
import net.basicmodel.R
import net.basicmodel.ui.dialog.BaseDialog.AutoDismissClickListener

class WebDialog(context: Context?) : BaseDialog<WebDialog>(context) {

    private var webView: WebView? = null
    private fun initContent() {
        webView!!.loadUrl("file:///android_asset/licenses.html")
        webView!!.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return if (url != null && url.startsWith("http://") || url != null && url.startsWith("https://")) {
                    view.context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    )
                    true
                } else {
                    false
                }
            }
        }
    }

    init {
        webView = findView(R.id.wv_license_content)
        val button = findView<Button>(R.id.ld_btn_confirm)
        button.setOnClickListener(AutoDismissClickListener(null, true))
        initContent()
    }

    override val layout: Int
        get() = R.layout.dialog_licenses
}