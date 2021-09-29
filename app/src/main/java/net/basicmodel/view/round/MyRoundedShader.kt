package net.basicmodel.view.round

import android.content.Context
import android.graphics.RectF
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import net.basicmodel.R

class MyRoundedShader : MyShaderHelper() {
    private val borderRect = RectF()
    private val imageRect = RectF()
    var radius = 0
    private var bitmapRadius = 0

    override fun init(context: Context, attrs: AttributeSet?, defStyle: Int) {
        super.init(context, attrs, defStyle)
        borderPaint.strokeWidth = (borderWidth * 2).toFloat()
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShaderImageView, defStyle, 0)
            radius = typedArray.getDimensionPixelSize(R.styleable.ShaderImageView_siRadius, radius)
            typedArray.recycle()
        }
    }

    override fun draw(canvas: Canvas?, imagePaint: Paint?, borderPaint: Paint?) {
        canvas!!.drawRoundRect(borderRect, radius.toFloat(), radius.toFloat(), borderPaint!!)
        canvas.save()
        canvas.concat(matrix)
        canvas.drawRoundRect(imageRect, bitmapRadius.toFloat(), bitmapRadius.toFloat(), imagePaint!!)
        canvas.restore()
    }

    override fun onSizeChanged(width: Int, height: Int) {
        super.onSizeChanged(width, height)
        borderRect[borderWidth.toFloat(), borderWidth.toFloat(), (viewWidth - borderWidth).toFloat()] =
            (viewHeight - borderWidth).toFloat()
    }

    override fun calculate(
        bitmapWidth: Int, bitmapHeight: Int,
        width: Float, height: Float,
        scale: Float,
        translateX: Float, translateY: Float
    ) {
        imageRect[-translateX, -translateY, bitmapWidth + translateX] = bitmapHeight + translateY
        bitmapRadius = Math.round(radius / scale)
    }

    override fun reset() {
        imageRect[0f, 0f, 0f] = 0f
        bitmapRadius = 0
    }
}