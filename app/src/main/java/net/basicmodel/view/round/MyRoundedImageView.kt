package net.basicmodel.view.round

import android.content.Context
import android.util.AttributeSet

class MyRoundedImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    MyShaderImageView(context, attrs, defStyle) {

    private var shader: MyRoundedShader? = null

    public override fun createImageViewHelper() = MyRoundedShader()

    var radius: Int
        get() = if (shader != null) {
            shader!!.radius
        } else 0
        set(radius) {
            if (shader != null) {
                shader!!.radius = radius
                invalidate()
            }
        }
}