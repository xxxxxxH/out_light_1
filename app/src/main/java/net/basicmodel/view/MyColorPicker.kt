/*
 * Copyright 2013 Piotr Adamus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.basicmodel.view

import net.basicmodel.interfaces.IColor
import android.view.View.MeasureSpec
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Shader.TileMode
import android.view.MotionEvent
import android.os.Parcelable
import android.os.Bundle
import android.util.AttributeSet
import android.view.View

class MyColorPicker : View {
    private var colorWheelPaint: Paint? = null
    private var colorViewPaint: Paint? = null
    private var colorPointerPaint: Paint? = null
    private var colorPointerRects: RectF? = null
    private var outerWheelRect: RectF? = null
    private var innerWheelRect: RectF? = null
    private var colorViewPath: Path? = null
    private var valueSliderPath: Path? = null
    private var colorWheelBitmap: Bitmap? = null
    private var colorWheelRadius = 0
    private var colorHSV: FloatArray? = floatArrayOf(0f, 0f, 1f)
    private var iColor: IColor? = null
    fun SetIColor(iColor: IColor?) {
        this.iColor = iColor
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    private fun init() {
        colorPointerPaint = Paint()
        colorPointerPaint!!.style = Paint.Style.STROKE
        colorPointerPaint!!.strokeWidth = 2f
        colorPointerPaint!!.setARGB(128, 0, 0, 0)
        val valuePointerPaint = Paint()
        valuePointerPaint.style = Paint.Style.STROKE
        valuePointerPaint.strokeWidth = 2f
        colorWheelPaint = Paint()
        colorWheelPaint!!.isAntiAlias = true
        colorWheelPaint!!.isDither = true
        val valueSliderPaint = Paint()
        valueSliderPaint.isAntiAlias = true
        valueSliderPaint.isDither = true
        colorViewPaint = Paint()
        colorViewPaint!!.isAntiAlias = true
        colorViewPath = Path()
        valueSliderPath = Path()
        outerWheelRect = RectF()
        innerWheelRect = RectF()
        colorPointerRects = RectF()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val size = Math.min(widthSize, heightSize)
        setMeasuredDimension(size, size)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val centerX = width / 2
        val centerY = height / 2
        canvas.drawBitmap(
            colorWheelBitmap!!,
            (centerX - colorWheelRadius).toFloat(),
            (centerY - colorWheelRadius).toFloat(),
            null
        )
        colorViewPaint!!.color = Color.HSVToColor(colorHSV)
        canvas.drawPath(colorViewPath!!, colorViewPaint!!)
        val hueAngle = Math.toRadians(colorHSV!![0].toDouble()).toFloat()
        val colorPointX = (-Math.cos(hueAngle.toDouble()) * colorHSV!![1] * colorWheelRadius).toInt() + centerX
        val colorPointY = (-Math.sin(hueAngle.toDouble()) * colorHSV!![1] * colorWheelRadius).toInt() + centerY
        val pointerRadius = 0.075f * colorWheelRadius
        val pointerX = (colorPointX - pointerRadius / 2).toInt()
        val pointerY = (colorPointY - pointerRadius / 2).toInt()
        colorPointerRects!![pointerX.toFloat(), pointerY.toFloat(), pointerX + pointerRadius] = pointerY + pointerRadius
        canvas.drawOval(colorPointerRects!!, colorPointerPaint!!)
    }

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        val centerX = width / 2
        val centerY = height / 2
        val innerPadding = 0
        val outerPadding = 0
        val arrowPointerSize = 0
        val valueSliderWidth = 0
        val outerWheelRadius = width / 2 - outerPadding - arrowPointerSize
        val innerWheelRadius = outerWheelRadius - valueSliderWidth
        colorWheelRadius = innerWheelRadius - innerPadding
        outerWheelRect!![(centerX - outerWheelRadius).toFloat(), (centerY - outerWheelRadius).toFloat(), (centerX + outerWheelRadius).toFloat()] =
            (centerY + outerWheelRadius).toFloat()
        innerWheelRect!![(centerX - innerWheelRadius).toFloat(), (centerY - innerWheelRadius).toFloat(), (centerX + innerWheelRadius).toFloat()] =
            (centerY + innerWheelRadius).toFloat()
        colorWheelBitmap = createColorWheelBitmap(colorWheelRadius * 2, colorWheelRadius * 2)
        val gradientRotationMatrix = Matrix()
        gradientRotationMatrix.preRotate(270f, (width / 2).toFloat(), (height / 2).toFloat())
        colorViewPath!!.arcTo(outerWheelRect!!, 270f, -180f)
        colorViewPath!!.arcTo(innerWheelRect!!, 90f, 180f)
        valueSliderPath!!.arcTo(outerWheelRect!!, 270f, 180f)
        valueSliderPath!!.arcTo(innerWheelRect!!, 90f, -180f)
    }

    private fun createColorWheelBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val colorCount = 12
        val colorAngleStep = 360 / 12
        val colors = IntArray(colorCount + 1)
        val hsv = floatArrayOf(0f, 1f, 1f)
        for (i in colors.indices) {
            hsv[0] = ((i * colorAngleStep + 180) % 360).toFloat()
            colors[i] = Color.HSVToColor(hsv)
        }
        colors[colorCount] = colors[0]
        val sweepGradient = SweepGradient((width / 2).toFloat(), (height / 2).toFloat(), colors, null)
        val radialGradient =
            RadialGradient(
                (width / 2).toFloat(),
                (height / 2).toFloat(),
                colorWheelRadius.toFloat(),
                -0x1,
                0x00FFFFFF,
                TileMode.CLAMP
            )
        val composeShader = ComposeShader(sweepGradient, radialGradient, PorterDuff.Mode.SRC_OVER)
        colorWheelPaint!!.shader = composeShader
        val canvas = Canvas(bitmap)
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), colorWheelRadius.toFloat(), colorWheelPaint!!)
        return bitmap
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val x = event.x.toInt()
                val y = event.y.toInt()
                val cx = x - width / 2
                val cy = y - height / 2
                val d = Math.sqrt((cx * cx + cy * cy).toDouble())
                if (d <= colorWheelRadius) {
                    colorHSV!![0] = (Math.toDegrees(Math.atan2(cy.toDouble(), cx.toDouble())) + 180f).toFloat()
                    colorHSV!![1] = Math.max(0f, Math.min(1f, (d / colorWheelRadius).toFloat()))
                    invalidate()
                    iColor!!.onColorChange(color)
                } else {
                    iColor!!.onClick()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    var color: Int
        get() = Color.HSVToColor(colorHSV)
        set(color) {
            Color.colorToHSV(color, colorHSV)
        }

    override fun onSaveInstanceState(): Parcelable? {
        val state = Bundle()
        state.putFloatArray("color", colorHSV)
        state.putParcelable("super", super.onSaveInstanceState())
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            val bundle = state
            colorHSV = bundle.getFloatArray("color")
            super.onRestoreInstanceState(bundle.getParcelable("super"))
        } else {
            super.onRestoreInstanceState(state)
        }
    }
}