package net.basicmodel.view.tooltip

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import kotlin.jvm.JvmOverloads
import net.basicmodel.R
import net.basicmodel.view.tooltip.TooltipOverlayDrawable
import android.content.res.TypedArray
import android.util.AttributeSet

class TooltipOverlay : AppCompatImageView {
    var layoutMargins = 0
        private set

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = R.style.ToolTipOverlayDefaultStyle
    ) : super(context, attrs, defStyleAttr) {
        init(context, R.style.ToolTipLayoutDefaultStyle)
    }

    private fun init(context: Context, defStyleResId: Int) {
        val drawable = TooltipOverlayDrawable(context, defStyleResId)
        setImageDrawable(drawable)
        val array = context.theme.obtainStyledAttributes(defStyleResId, R.styleable.TooltipOverlay)
        layoutMargins = array.getDimensionPixelSize(R.styleable.TooltipOverlay_android_layout_margin, 0)
        array.recycle()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleResId: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, defStyleResId)
    }
}