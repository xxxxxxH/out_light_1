package net.basicmodel.view.round

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import net.basicmodel.view.round.MyShaderHelper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet

abstract class MyShaderImageView : AppCompatImageView {
    protected var pathHelper: MyShaderHelper? = null
        protected get() {
            if (field == null) {
                field = createImageViewHelper()
            }
            return field
        }
        private set

    constructor(context: Context) : super(context) {
        setup(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setup(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        setup(context, attrs, defStyle)
    }

    private fun setup(context: Context, attrs: AttributeSet?, defStyle: Int) {
        pathHelper!!.init(context, attrs, defStyle)
    }

    protected abstract fun createImageViewHelper(): MyShaderHelper?
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (pathHelper!!.isSquare) {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        pathHelper!!.onImageDrawableReset(drawable)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        pathHelper!!.onImageDrawableReset(getDrawable())
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        pathHelper!!.onImageDrawableReset(drawable)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        pathHelper!!.onSizeChanged(w, h)
    }

    public override fun onDraw(canvas: Canvas) {
        if (!pathHelper!!.onDraw(canvas)) {
            super.onDraw(canvas)
        }
    }
}