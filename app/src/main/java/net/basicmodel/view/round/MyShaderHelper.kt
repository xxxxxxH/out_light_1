package net.basicmodel.view.round

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import net.basicmodel.R
import net.basicmodel.view.round.MyShaderHelper
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet

abstract class MyShaderHelper {
    @JvmField
    protected var viewWidth = 0

    @JvmField
    protected var viewHeight = 0
    protected var borderColor = Color.BLACK

    @JvmField
    protected var borderWidth = 0
    protected var borderAlpha = 1f
    var isSquare = false
        protected set

    @JvmField
    protected val borderPaint: Paint
    protected val imagePaint: Paint
    protected var shader: BitmapShader? = null
    protected var drawable: Drawable? = null

    @JvmField
    protected val matrix = Matrix()
    abstract fun draw(canvas: Canvas?, imagePaint: Paint?, borderPaint: Paint?)
    abstract fun reset()
    abstract fun calculate(
        bitmapWidth: Int,
        bitmapHeight: Int,
        width: Float,
        height: Float,
        scale: Float,
        translateX: Float,
        translateY: Float
    )

    open fun init(context: Context, attrs: AttributeSet?, defStyle: Int) {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShaderImageView, defStyle, 0)
            borderColor = typedArray.getColor(R.styleable.ShaderImageView_siBorderColor, borderColor)
            borderWidth = typedArray.getDimensionPixelSize(R.styleable.ShaderImageView_siBorderWidth, borderWidth)
            borderAlpha = typedArray.getFloat(R.styleable.ShaderImageView_siBorderAlpha, borderAlpha)
            isSquare = typedArray.getBoolean(R.styleable.ShaderImageView_siSquare, isSquare)
            typedArray.recycle()
        }
        borderPaint.color = borderColor
        borderPaint.alpha = java.lang.Float.valueOf(borderAlpha * ALPHA_MAX).toInt()
        borderPaint.strokeWidth = borderWidth.toFloat()
    }

    fun onDraw(canvas: Canvas?): Boolean {
        if (shader == null) {
            createShader()
        }
        if (shader != null && viewWidth > 0 && viewHeight > 0) {
            draw(canvas, imagePaint, borderPaint)
            return true
        }
        return false
    }

    open fun onSizeChanged(width: Int, height: Int) {
        if (viewWidth == width && viewHeight == height) return
        viewWidth = width
        viewHeight = height
        if (isSquare) {
            viewHeight = Math.min(width, height)
            viewWidth = viewHeight
        }
        if (shader != null) {
            calculateDrawableSizes()
        }
    }

    fun calculateDrawableSizes(): Bitmap? {
        val bitmap = bitmap
        if (bitmap != null) {
            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            if (bitmapWidth > 0 && bitmapHeight > 0) {
                val width = Math.round(viewWidth - 2f * borderWidth).toFloat()
                val height = Math.round(viewHeight - 2f * borderWidth).toFloat()
                val scale: Float
                var translateX = 0f
                var translateY = 0f
                if (bitmapWidth * height > width * bitmapHeight) {
                    scale = height / bitmapHeight
                    translateX = Math.round((width / scale - bitmapWidth) / 2f).toFloat()
                } else {
                    scale = width / bitmapWidth.toFloat()
                    translateY = Math.round((height / scale - bitmapHeight) / 2f).toFloat()
                }
                matrix.setScale(scale, scale)
                matrix.preTranslate(translateX, translateY)
                matrix.postTranslate(borderWidth.toFloat(), borderWidth.toFloat())
                calculate(bitmapWidth, bitmapHeight, width, height, scale, translateX, translateY)
                return bitmap
            }
        }
        reset()
        return null
    }

    fun onImageDrawableReset(drawable: Drawable?) {
        this.drawable = drawable
        shader = null
        imagePaint.shader = null
    }

    protected fun createShader() {
        val bitmap = calculateDrawableSizes()
        if (bitmap != null && bitmap.width > 0 && bitmap.height > 0) {
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            imagePaint.shader = shader
        }
    }

    protected val bitmap: Bitmap?
        protected get() {
            var bitmap: Bitmap? = null
            if (drawable != null) {
                if (drawable is BitmapDrawable) {
                    bitmap = (drawable as BitmapDrawable).bitmap
                }
            }
            return bitmap
        }

    companion object {
        private const val ALPHA_MAX = 255
    }

    init {
        borderPaint = Paint()
        borderPaint.style = Paint.Style.STROKE
        borderPaint.isAntiAlias = true
        imagePaint = Paint()
        imagePaint.isAntiAlias = true
    }
}