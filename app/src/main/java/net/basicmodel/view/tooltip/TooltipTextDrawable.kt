package net.basicmodel.view.tooltip

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.graphics.*
import android.graphics.drawable.Drawable
import net.basicmodel.R

internal class TooltipTextDrawable(context: Context, builder: Tooltip.Builder) : Drawable() {
    private val rectF: RectF
    private val path: Path
    private val tempPoint = Point()
    private val outlineRect = Rect()
    private var backgroundPaint: Paint? = null
    private var strokePaint: Paint? = null
    private val arrowRatio: Float
    val radius: Float
    private var point: Point? = null
    private var padding = 0
    private var arrowWeight = 0
    private var gravity: Tooltip.Gravity? = null

    override fun draw(canvas: Canvas) {
        if (null != backgroundPaint) {
            canvas.drawPath(path, backgroundPaint!!)
        }
        if (null != strokePaint) {
            canvas.drawPath(path, strokePaint!!)
        }
    }

    fun setAnchor(gravity: Tooltip.Gravity, padding: Int, point: Point?) {
        if (gravity != this.gravity || padding != this.padding || this.point != point) {
            this.gravity = gravity
            this.padding = padding
            arrowWeight = (padding.toFloat() / arrowRatio).toInt()
            if (null != point) {
                this.point = Point(point)
            } else {
                this.point = null
            }
            val bounds = bounds
            if (!bounds.isEmpty) {
                calculatePath(getBounds())
                invalidateSelf()
            }
        }
    }

    fun calculatePath(outBounds: Rect) {
        val left = outBounds.left + padding
        val top = outBounds.top + padding
        val right = outBounds.right - padding
        val bottom = outBounds.bottom - padding
        val maxY = bottom - radius
        val maxX = right - radius
        val minY = top + radius
        val minX = left + radius
        if (null != point && null != gravity) {
            calculatePathWithGravity(outBounds, left, top, right, bottom, maxY, maxX, minY, minX)
        } else {
            rectF[left.toFloat(), top.toFloat(), right.toFloat()] = bottom.toFloat()
            path.addRoundRect(rectF, radius, radius, Path.Direction.CW)
        }
    }

    private fun calculatePathWithGravity(
        outBounds: Rect, left: Int, top: Int, right: Int, bottom: Int, maxY: Float, maxX: Float,
        minY: Float, minX: Float
    ) {
        val drawPoint =
            isDrawPoint(left, top, right, bottom, maxY, maxX, minY, minX, tempPoint, point, gravity, arrowWeight)
        clampPoint(left, top, right, bottom, tempPoint)
        path.reset()
        path.moveTo(left + radius, top.toFloat())
        if (drawPoint && gravity == Tooltip.Gravity.BOTTOM) {
            path.lineTo((left + tempPoint.x - arrowWeight).toFloat(), top.toFloat())
            path.lineTo((left + tempPoint.x).toFloat(), outBounds.top.toFloat())
            path.lineTo((left + tempPoint.x + arrowWeight).toFloat(), top.toFloat())
        }
        path.lineTo(right - radius, top.toFloat())
        path.quadTo(right.toFloat(), top.toFloat(), right.toFloat(), top + radius)
        if (drawPoint && gravity == Tooltip.Gravity.LEFT) {
            path.lineTo(right.toFloat(), (top + tempPoint.y - arrowWeight).toFloat())
            path.lineTo(outBounds.right.toFloat(), (top + tempPoint.y).toFloat())
            path.lineTo(right.toFloat(), (top + tempPoint.y + arrowWeight).toFloat())
        }
        path.lineTo(right.toFloat(), bottom - radius)
        path.quadTo(right.toFloat(), bottom.toFloat(), right - radius, bottom.toFloat())
        if (drawPoint && gravity == Tooltip.Gravity.TOP) {
            path.lineTo((left + tempPoint.x + arrowWeight).toFloat(), bottom.toFloat())
            path.lineTo((left + tempPoint.x).toFloat(), outBounds.bottom.toFloat())
            path.lineTo((left + tempPoint.x - arrowWeight).toFloat(), bottom.toFloat())
        }
        path.lineTo(left + radius, bottom.toFloat())
        path.quadTo(left.toFloat(), bottom.toFloat(), left.toFloat(), bottom - radius)
        if (drawPoint && gravity == Tooltip.Gravity.RIGHT) {
            path.lineTo(left.toFloat(), (top + tempPoint.y + arrowWeight).toFloat())
            path.lineTo(outBounds.left.toFloat(), (top + tempPoint.y).toFloat())
            path.lineTo(left.toFloat(), (top + tempPoint.y - arrowWeight).toFloat())
        }
        path.lineTo(left.toFloat(), top + radius)
        path.quadTo(left.toFloat(), top.toFloat(), left + radius, top.toFloat())
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        calculatePath(bounds)
    }

    override fun getAlpha(): Int {
        return backgroundPaint!!.alpha
    }

    override fun setAlpha(alpha: Int) {
        backgroundPaint!!.alpha = alpha
        strokePaint!!.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {}
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun getOutline(outline: Outline) {
        copyBounds(outlineRect)
        outlineRect.inset(padding, padding)
        outline.setRoundRect(outlineRect, radius)
        if (alpha < 255) {
            outline.alpha = 0f
        }
    }

    companion object {
        private fun isDrawPoint(
            left: Int, top: Int, right: Int, bottom: Int, maxY: Float, maxX: Float, minY: Float,
            minX: Float, tempPoint: Point, point: Point?, gravity: Tooltip.Gravity?,
            arrowWeight: Int
        ): Boolean {
            var drawPoint = false
            tempPoint[point!!.x] = point.y
            if (gravity == Tooltip.Gravity.RIGHT || gravity == Tooltip.Gravity.LEFT) {
                if (tempPoint.y >= top && tempPoint.y <= bottom) {
                    if (top + tempPoint.y + arrowWeight > maxY) {
                        tempPoint.y = (maxY - arrowWeight - top).toInt()
                    } else if (top + tempPoint.y - arrowWeight < minY) {
                        tempPoint.y = (minY + arrowWeight - top).toInt()
                    }
                    drawPoint = true
                }
            } else {
                if (tempPoint.x >= left && tempPoint.x <= right) {
                    if (tempPoint.x >= left && tempPoint.x <= right) {
                        if (left + tempPoint.x + arrowWeight > maxX) {
                            tempPoint.x = (maxX - arrowWeight - left).toInt()
                        } else if (left + tempPoint.x - arrowWeight < minX) {
                            tempPoint.x = (minX + arrowWeight - left).toInt()
                        }
                        drawPoint = true
                    }
                }
            }
            return drawPoint
        }

        private fun clampPoint(
            left: Int, top: Int, right: Int, bottom: Int, tempPoint: Point
        ) {
            if (tempPoint.y < top) {
                tempPoint.y = top
            } else if (tempPoint.y > bottom) {
                tempPoint.y = bottom
            }
            if (tempPoint.x < left) {
                tempPoint.x = left
            }
            if (tempPoint.x > right) {
                tempPoint.x = right
            }
        }
    }

    init {
        val theme = context.theme.obtainStyledAttributes(
            null,
            R.styleable.TooltipLayout,
            builder.defStyleAttr,
            builder.defStyleRes
        )
        radius = theme.getDimensionPixelSize(R.styleable.TooltipLayout_ttlm_cornerRadius, 4).toFloat()
        val strokeWidth = theme.getDimensionPixelSize(R.styleable.TooltipLayout_ttlm_strokeWeight, 2)
        val backgroundColor = theme.getColor(R.styleable.TooltipLayout_ttlm_backgroundColor, 0)
        val strokeColor = theme.getColor(R.styleable.TooltipLayout_ttlm_strokeColor, 0)
        val ratio = 1.4f
        arrowRatio = theme.getFloat(R.styleable.TooltipLayout_ttlm_arrowRatio, ratio)
        theme.recycle()
        rectF = RectF()
        if (backgroundColor != 0) {
            backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            backgroundPaint!!.color = backgroundColor
            backgroundPaint!!.style = Paint.Style.FILL
        } else {
            backgroundPaint = null
        }
        if (strokeColor != 0) {
            strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            strokePaint!!.color = strokeColor
            strokePaint!!.style = Paint.Style.STROKE
            strokePaint!!.strokeWidth = strokeWidth.toFloat()
        } else {
            strokePaint = null
        }
        path = Path()
    }
}