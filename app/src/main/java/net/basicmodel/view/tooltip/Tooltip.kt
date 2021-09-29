package net.basicmodel.view.tooltip

import android.animation.Animator
import androidx.annotation.StringRes
import android.content.res.ColorStateList
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.os.Build
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.TextView
import android.animation.ValueAnimator
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.animation.ObjectAnimator
import android.animation.Animator.AnimatorListener
import android.content.Context
import android.content.res.Resources
import android.text.Html
import android.view.animation.AccelerateDecelerateInterpolator
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.Rect
import net.basicmodel.R
import android.graphics.Typeface
import android.os.Handler
import android.view.*
import net.basicmodel.getActivity
import net.basicmodel.getRectContains
import java.lang.ref.WeakReference
import java.util.*

object Tooltip {
    @JvmStatic
    fun make(context: Context, builder: Builder): TooltipView {
        return TooltipViewImpl(context, builder)
    }

    class ClosePolicy internal constructor(private val policy: Int) {
        fun build(): Int {
            return policy
        }

        companion object {
            const val NONE = 0
            const val TOUCH_INSIDE = 1 shl 1
            const val TOUCH_OUTSIDE = 1 shl 2
            const val CONSUME_INSIDE = 1 shl 3
            const val CONSUME_OUTSIDE = 1 shl 4

            @JvmField
            val TOUCH_ANYWHERE_NO_CONSUME = ClosePolicy(TOUCH_INSIDE or TOUCH_OUTSIDE)
            fun touchInside(value: Int): Boolean {
                return value and TOUCH_INSIDE == TOUCH_INSIDE
            }

            fun touchOutside(value: Int): Boolean {
                return value and TOUCH_OUTSIDE == TOUCH_OUTSIDE
            }

            fun consumeInside(value: Int): Boolean {
                return value and CONSUME_INSIDE == CONSUME_INSIDE
            }

            fun consumeOutside(value: Int): Boolean {
                return value and CONSUME_OUTSIDE == CONSUME_OUTSIDE
            }
        }
    }

    enum class Gravity {
        LEFT, RIGHT, TOP, BOTTOM, CENTER
    }

    interface TooltipView {
        fun show()
        fun setText(text: CharSequence?)
        fun setText(@StringRes resId: Int)
        fun setTextColor(color: Int)
        fun setTextColor(color: ColorStateList?)
    }

    interface Callback {
        fun onTooltipClose(tooltip: TooltipView?, fromUser: Boolean, containsTouch: Boolean)
        fun onTooltipFailed(view: TooltipView?)
        fun onTooltipShown(view: TooltipView?)
        fun onTooltipHidden(view: TooltipView?)
    }

    @SuppressLint("ViewConstructor")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    internal class TooltipViewImpl @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1) constructor(
        context: Context,
        builder: Builder
    ) : ViewGroup(context), TooltipView {
        private val viewGravities: MutableList<Gravity?> = ArrayList(GRAVITY_LIST)
        private val mShowDelay: Long
        private val mTextAppearance: Int
        private val mTextGravity: Int
        private val mToolTipId: Int
        private val mDrawRect: Rect
        private val mShowDuration: Long
        private val mClosePolicy: Int
        private var mPoint: Point? = null
        private val mTextResId: Int
        private val mTopRule: Int
        private val mMaxWidth: Int
        private val mHideArrow: Boolean
        private val mActivateDelay: Long
        private val mRestrict: Boolean
        private val mFadeDuration: Long
        private var mDrawable: TooltipTextDrawable? = null
        private val mTempRect = Rect()
        private val mTempLocation = IntArray(2)
        private val mHandler = Handler()
        private val mScreenRect = Rect()
        private val mTmpPoint = Point()
        private val mHitRect = Rect()
        private val mTextViewElevation: Float
        private var mCallback: Callback?
        private var mOldLocation: IntArray? = null
        private var mGravity: Gravity?
        private var mShowAnimation: Animator? = null
        private var mShowing = false
        private var mViewAnchor: WeakReference<View?>? = null
        private var mAttached = false
        private val mAttachedStateListener: OnAttachStateChangeListener = object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}

            @TargetApi(17)
            override fun onViewDetachedFromWindow(v: View) {
                removeViewListeners(v)
                if (!mAttached) {
                    return
                }
                val activity = getActivity(getContext())
                if (null != activity) {
                    if (activity.isFinishing) {
                        return
                    }
                    if (activity.isDestroyed) {
                        return
                    }
                    onClose(false, false, true)
                }
            }
        }
        private val hideRunnable = Runnable { onClose(false, false, false) }
        private var mInitialized = false
        private var mActivated = false
        var activateRunnable = Runnable { mActivated = true }
        private val mPadding: Int
        private var mText: CharSequence?
        private var mViewRect: Rect? = null
        private var mView: View? = null
        private var mViewOverlay: TooltipOverlay? = null
        private val mPreDrawListener = OnPreDrawListener {
            if (!mAttached) {
                removePreDrawObserver(null)
                return@OnPreDrawListener true
            }
            if (null != mViewAnchor) {
                val view = mViewAnchor!!.get()
                if (null != view) {
                    view.getLocationOnScreen(mTempLocation)
                    if (mOldLocation == null) {
                        mOldLocation = intArrayOf(mTempLocation[0], mTempLocation[1])
                    }
                    if (mOldLocation!![0] != mTempLocation[0] || mOldLocation!![1] != mTempLocation[1]) {
                        mView!!.translationX = mTempLocation[0] - mOldLocation!![0] + mView!!.translationX
                        mView!!.translationY = mTempLocation[1] - mOldLocation!![1] + mView!!.translationY
                        if (null != mViewOverlay) {
                            mViewOverlay!!.translationX =
                                mTempLocation[0] - mOldLocation!![0] + mViewOverlay!!.translationX
                            mViewOverlay!!.translationY =
                                mTempLocation[1] - mOldLocation!![1] + mViewOverlay!!.translationY
                        }
                    }
                    mOldLocation!![0] = mTempLocation[0]
                    mOldLocation!![1] = mTempLocation[1]
                }
            }
            true
        }
        private var mTextView: TextView? = null
        private val mSizeTolerance: Int
        private var mAnimator: ValueAnimator? = null
        private val mFloatingAnimation: AnimationBuilder?
        private var mAlreadyCheck = false
        private val mGlobalLayoutListener = OnGlobalLayoutListener {
            if (!mAttached) {
                removeGlobalLayoutObserver(null)
                return@OnGlobalLayoutListener
            }
            if (null != mViewAnchor) {
                val view = mViewAnchor!!.get()
                if (null != view) {
                    view.getHitRect(mTempRect)
                    view.getLocationOnScreen(mTempLocation)
                    if (mTempRect != mHitRect) {
                        mHitRect.set(mTempRect)
                        mTempRect.offsetTo(mTempLocation[0], mTempLocation[1])
                        mViewRect!!.set(mTempRect)
                        calculatePositions()
                    }
                }
            }
        }
        private var mIsCustomView = false
        override fun show() {
            if (parent == null) {
                val act = getActivity(context)
                val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                if (act != null) {
                    val rootView: ViewGroup
                    rootView = act.window.decorView as ViewGroup
                    rootView.addView(this, params)
                }
            }
        }

        private fun hide(fadeDuration: Long) {
            fadeOut(fadeDuration)
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        protected fun fadeOut(fadeDuration: Long) {
            if (!mShowing) {
                return
            }
            if (null != mShowAnimation) {
                mShowAnimation!!.cancel()
            }
            mShowing = false
            if (fadeDuration > 0) {
                val alpha = alpha
                mShowAnimation = ObjectAnimator.ofFloat(this, "alpha", alpha, 0f)
                mShowAnimation!!.setDuration(fadeDuration)
                mShowAnimation!!.addListener(
                    object : AnimatorListener {
                        var cancelled = false
                        override fun onAnimationStart(animation: Animator) {
                            cancelled = false
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            if (cancelled) {
                                return
                            }
                            if (null != mCallback) {
                                mCallback!!.onTooltipHidden(this@TooltipViewImpl)
                            }
                            mShowAnimation = null
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            cancelled = true
                        }

                        override fun onAnimationRepeat(animation: Animator) {}
                    }
                )
                mShowAnimation!!.start()
            } else {
                visibility = INVISIBLE
            }
        }

        override fun setText(@StringRes resId: Int) {
            if (null != mView) {
                setText(resources.getString(resId))
            }
        }

        override fun setTextColor(color: Int) {
            if (null != mTextView) {
                mTextView!!.setTextColor(color)
            }
        }

        override fun setTextColor(color: ColorStateList?) {
            if (null != mTextView) {
                mTextView!!.setTextColor(color)
            }
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            mAttached = true
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay
            display.getRectSize(mScreenRect)
            initializeView()
            showInternal()
        }

        override fun onDetachedFromWindow() {
            removeListeners()
            stopFloatingAnimations()
            mAttached = false
            mViewAnchor = null
            super.onDetachedFromWindow()
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        override fun onVisibilityChanged(changedView: View, visibility: Int) {
            super.onVisibilityChanged(changedView, visibility)
            if (null != mAnimator) {
                if (visibility == VISIBLE) {
                    mAnimator!!.start()
                } else {
                    mAnimator!!.cancel()
                }
            }
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            if (null != mView) {
                mView!!.layout(mView!!.left, mView!!.top, mView!!.measuredWidth, mView!!.measuredHeight)
            }
            if (null != mViewOverlay) {
                mViewOverlay!!.layout(
                    mViewOverlay!!.left,
                    mViewOverlay!!.top,
                    mViewOverlay!!.measuredWidth,
                    mViewOverlay!!.measuredHeight
                )
            }
            if (changed) {
                if (mViewAnchor != null) {
                    val view = mViewAnchor!!.get()
                    if (null != view) {
                        view.getHitRect(mTempRect)
                        view.getLocationOnScreen(mTempLocation)
                        mTempRect.offsetTo(mTempLocation[0], mTempLocation[1])
                        mViewRect!!.set(mTempRect)
                    }
                }
                calculatePositions()
            }
        }

        private fun removeListeners() {
            mCallback = null
            if (null != mViewAnchor) {
                val view = mViewAnchor!!.get()
                removeViewListeners(view)
            }
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private fun stopFloatingAnimations() {
            if (null != mAnimator) {
                mAnimator!!.cancel()
                mAnimator = null
            }
        }

        private fun removeViewListeners(view: View?) {
            removeGlobalLayoutObserver(view)
            removePreDrawObserver(view)
            removeOnAttachStateObserver(view)
        }

        private fun removeGlobalLayoutObserver(view: View?) {
            var view = view
            if (null == view && null != mViewAnchor) {
                view = mViewAnchor!!.get()
            }
            if (null != view && view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnGlobalLayoutListener(mGlobalLayoutListener)
            }
        }

        private fun removePreDrawObserver(view: View?) {
            var view = view
            if (null == view && null != mViewAnchor) {
                view = mViewAnchor!!.get()
            }
            if (null != view && view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnPreDrawListener(mPreDrawListener)
            }
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
        private fun removeOnAttachStateObserver(view: View?) {
            var view = view
            if (null == view && null != mViewAnchor) {
                view = mViewAnchor!!.get()
            }
            view?.removeOnAttachStateChangeListener(mAttachedStateListener)
        }

        private fun initializeView() {
            if (mInitialized) {
                return
            }
            mInitialized = true
            val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            mView = LayoutInflater.from(context).inflate(mTextResId, this, false)
            mView!!.setLayoutParams(params)
            mTextView = mView!!.findViewById<View>(android.R.id.text1) as TextView
            mTextView!!.text = Html.fromHtml(mText as String?)
            if (mMaxWidth > -1) {
                mTextView!!.maxWidth = mMaxWidth
            }
            if (0 != mTextAppearance) {
                mTextView!!.setTextAppearance(context, mTextAppearance)
            }
            mTextView!!.gravity = mTextGravity
            if (null != mDrawable) {
                mTextView!!.setBackgroundDrawable(mDrawable)
                if (mHideArrow) {
                    mTextView!!.setPadding(mPadding / 2, mPadding / 2, mPadding / 2, mPadding / 2)
                } else {
                    mTextView!!.setPadding(mPadding, mPadding, mPadding, mPadding)
                }
            }
            this.addView(mView)
            if (null != mViewOverlay) {
                this.addView(mViewOverlay)
            }
            if (!mIsCustomView && mTextViewElevation > 0 && Build.VERSION.SDK_INT >= 21) {
                setupElevation()
            }
        }

        private fun showInternal() {
            fadeIn(mFadeDuration)
        }

        @SuppressLint("NewApi")
        private fun setupElevation() {
            mTextView!!.elevation = mTextViewElevation
            mTextView!!.outlineProvider = ViewOutlineProvider.BACKGROUND
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        protected fun fadeIn(fadeDuration: Long) {
            if (mShowing) {
                return
            }
            if (null != mShowAnimation) {
                mShowAnimation!!.cancel()
            }
            mShowing = true
            if (fadeDuration > 0) {
                mShowAnimation = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f)
                mShowAnimation!!.setDuration(fadeDuration)
                if (mShowDelay > 0) {
                    mShowAnimation!!.setStartDelay(mShowDelay)
                }
                mShowAnimation!!.addListener(
                    object : AnimatorListener {
                        var cancelled = false
                        override fun onAnimationStart(animation: Animator) {
                            visibility = VISIBLE
                            cancelled = false
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            if (!cancelled) {
                                if (null != mCallback) {
                                    mCallback!!.onTooltipShown(this@TooltipViewImpl)
                                }
                                postActivate(mActivateDelay)
                            }
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            cancelled = true
                        }

                        override fun onAnimationRepeat(animation: Animator) {}
                    }
                )
                mShowAnimation!!.start()
            } else {
                visibility = VISIBLE
                if (!mActivated) {
                    postActivate(mActivateDelay)
                }
            }
            if (mShowDuration > 0) {
                mHandler.removeCallbacks(hideRunnable)
                mHandler.postDelayed(hideRunnable, mShowDuration)
            }
        }

        fun postActivate(ms: Long) {
            if (ms <= 0) {
                mActivated = true
            }
        }

        private fun calculatePositions(restrict: Boolean = mRestrict) {
            viewGravities.clear()
            viewGravities.addAll(GRAVITY_LIST)
            viewGravities.remove(mGravity)
            viewGravities.add(0, mGravity)
            calculatePositions(viewGravities.filterNotNull().toMutableList(), restrict)
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private fun calculatePositions(gravities: MutableList<Gravity>, checkEdges: Boolean) {
            if (gravities.size < 1) {
                if (null != mCallback) {
                    mCallback!!.onTooltipFailed(this)
                }
                visibility = GONE
                return
            }
            val gravity: Gravity = gravities.removeAt(0)
            val statusBarHeight = mScreenRect.top
            val overlayWidth: Int
            val overlayHeight: Int
            if (null != mViewOverlay && gravity != Gravity.CENTER) {
                val margin = mViewOverlay!!.layoutMargins
                overlayWidth = mViewOverlay!!.width / 2 + margin
                overlayHeight = mViewOverlay!!.height / 2 + margin
            } else {
                overlayWidth = 0
                overlayHeight = 0
            }
            if (mViewRect == null) {
                mViewRect = Rect()
                mViewRect!![mPoint!!.x, mPoint!!.y + statusBarHeight, mPoint!!.x] = mPoint!!.y + statusBarHeight
            }
            val screenTop = mScreenRect.top + mTopRule
            val width = mView!!.width
            val height = mView!!.height
            if (gravity == Gravity.BOTTOM) {
                if (calculatePositionBottom(checkEdges, overlayHeight, screenTop, width, height)) {
                    calculatePositions(gravities, checkEdges)
                    return
                }
            } else if (gravity == Gravity.TOP) {
                if (calculatePositionTop(checkEdges, overlayHeight, screenTop, width, height)) {
                    calculatePositions(gravities, checkEdges)
                    return
                }
            } else if (gravity == Gravity.RIGHT) {
                if (calculatePositionRight(checkEdges, overlayWidth, screenTop, width, height)) {
                    calculatePositions(gravities, checkEdges)
                    return
                }
            } else if (gravity == Gravity.LEFT) {
                if (calculatePositionLeft(checkEdges, overlayWidth, screenTop, width, height)) {
                    calculatePositions(gravities, checkEdges)
                    return
                }
            } else if (gravity == Gravity.CENTER) {
                calculatePositionCenter(checkEdges, screenTop, width, height)
            }
            if (gravity != mGravity) {
                mGravity = gravity
                if (gravity == Gravity.CENTER && null != mViewOverlay) {
                    removeView(mViewOverlay)
                    mViewOverlay = null
                }
            }
            if (null != mViewOverlay) {
                mViewOverlay!!.translationX = (mViewRect!!.centerX() - mViewOverlay!!.width / 2).toFloat()
                mViewOverlay!!.translationY = (mViewRect!!.centerY() - mViewOverlay!!.height / 2).toFloat()
            }
            mView!!.translationX = mDrawRect.left.toFloat()
            mView!!.translationY = mDrawRect.top.toFloat()
            if (null != mDrawable) {
                getAnchorPoint(gravity, mTmpPoint)
                mDrawable!!.setAnchor(gravity, if (mHideArrow) 0 else mPadding / 2, if (mHideArrow) null else mTmpPoint)
            }
            if (!mAlreadyCheck) {
                mAlreadyCheck = true
                startFloatingAnimations()
            }
        }

        private fun calculatePositionCenter(checkEdges: Boolean, screenTop: Int, width: Int, height: Int) {
            mDrawRect[mViewRect!!.centerX() - width / 2, mViewRect!!.centerY() - height / 2, mViewRect!!.centerX() + width / 2] =
                mViewRect!!.centerY() + height / 2
            if (checkEdges && !getRectContains(mScreenRect, mDrawRect, mSizeTolerance)) {
                if (mDrawRect.bottom > mScreenRect.bottom) {
                    mDrawRect.offset(0, mScreenRect.bottom - mDrawRect.bottom)
                } else if (mDrawRect.top < screenTop) {
                    mDrawRect.offset(0, screenTop - mDrawRect.top)
                }
                if (mDrawRect.right > mScreenRect.right) {
                    mDrawRect.offset(mScreenRect.right - mDrawRect.right, 0)
                } else if (mDrawRect.left < mScreenRect.left) {
                    mDrawRect.offset(mScreenRect.left - mDrawRect.left, 0)
                }
            }
        }

        private fun calculatePositionLeft(
            checkEdges: Boolean, overlayWidth: Int, screenTop: Int,
            width: Int, height: Int
        ): Boolean {
            mDrawRect[mViewRect!!.left - width, mViewRect!!.centerY() - height / 2, mViewRect!!.left] =
                mViewRect!!.centerY() + height / 2
            if (mViewRect!!.width() / 2 < overlayWidth) {
                mDrawRect.offset(-(overlayWidth - mViewRect!!.width() / 2), 0)
            }
            if (checkEdges && !getRectContains(mScreenRect, mDrawRect, mSizeTolerance)) {
                if (mDrawRect.bottom > mScreenRect.bottom) {
                    mDrawRect.offset(0, mScreenRect.bottom - mDrawRect.bottom)
                } else if (mDrawRect.top < screenTop) {
                    mDrawRect.offset(0, screenTop - mDrawRect.top)
                }
                if (mDrawRect.left < mScreenRect.left) {
                    // this means there's no enough space!
                    return true
                } else if (mDrawRect.right > mScreenRect.right) {
                    mDrawRect.offset(mScreenRect.right - mDrawRect.right, 0)
                }
            }
            return false
        }

        private fun calculatePositionRight(
            checkEdges: Boolean, overlayWidth: Int, screenTop: Int,
            width: Int, height: Int
        ): Boolean {
            mDrawRect[mViewRect!!.right, mViewRect!!.centerY() - height / 2, mViewRect!!.right + width] =
                mViewRect!!.centerY() + height / 2
            if (mViewRect!!.width() / 2 < overlayWidth) {
                mDrawRect.offset(overlayWidth - mViewRect!!.width() / 2, 0)
            }
            if (checkEdges && !getRectContains(mScreenRect, mDrawRect, mSizeTolerance)) {
                if (mDrawRect.bottom > mScreenRect.bottom) {
                    mDrawRect.offset(0, mScreenRect.bottom - mDrawRect.bottom)
                } else if (mDrawRect.top < screenTop) {
                    mDrawRect.offset(0, screenTop - mDrawRect.top)
                }
                if (mDrawRect.right > mScreenRect.right) {
                    return true
                } else if (mDrawRect.left < mScreenRect.left) {
                    mDrawRect.offset(mScreenRect.left - mDrawRect.left, 0)
                }
            }
            return false
        }

        private fun calculatePositionTop(
            checkEdges: Boolean, overlayHeight: Int, screenTop: Int,
            width: Int, height: Int
        ): Boolean {
            mDrawRect[mViewRect!!.centerX() - width / 2, mViewRect!!.top - height, mViewRect!!.centerX() + width / 2] =
                mViewRect!!.top
            if (mViewRect!!.height() / 2 < overlayHeight) {
                mDrawRect.offset(0, -(overlayHeight - mViewRect!!.height() / 2))
            }
            if (checkEdges && !getRectContains(mScreenRect, mDrawRect, mSizeTolerance)) {
                if (mDrawRect.right > mScreenRect.right) {
                    mDrawRect.offset(mScreenRect.right - mDrawRect.right, 0)
                } else if (mDrawRect.left < mScreenRect.left) {
                    mDrawRect.offset(-mDrawRect.left, 0)
                }
                if (mDrawRect.top < screenTop) {
                    return true
                } else if (mDrawRect.bottom > mScreenRect.bottom) {
                    mDrawRect.offset(0, mScreenRect.bottom - mDrawRect.bottom)
                }
            }
            return false
        }

        private fun calculatePositionBottom(
            checkEdges: Boolean, overlayHeight: Int, screenTop: Int,
            width: Int, height: Int
        ): Boolean {
            mDrawRect[mViewRect!!.centerX() - width / 2, mViewRect!!.bottom, mViewRect!!.centerX() + width / 2] =
                mViewRect!!.bottom + height
            if (mViewRect!!.height() / 2 < overlayHeight) {
                mDrawRect.offset(0, overlayHeight - mViewRect!!.height() / 2)
            }
            if (checkEdges && !getRectContains(mScreenRect, mDrawRect, mSizeTolerance)) {
                if (mDrawRect.right > mScreenRect.right) {
                    mDrawRect.offset(mScreenRect.right - mDrawRect.right, 0)
                } else if (mDrawRect.left < mScreenRect.left) {
                    mDrawRect.offset(-mDrawRect.left, 0)
                }
                if (mDrawRect.bottom > mScreenRect.bottom) {
                    // this means there's no enough space!
                    return true
                } else if (mDrawRect.top < screenTop) {
                    mDrawRect.offset(0, screenTop - mDrawRect.top)
                }
            }
            return false
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        private fun startFloatingAnimations() {
            if (mTextView === mView || null == mFloatingAnimation) {
                return
            }
            val endValue = mFloatingAnimation.radius.toFloat()
            val duration = mFloatingAnimation.duration
            val direction: Int
            direction = if (mFloatingAnimation.direction == 0) {
                if (mGravity == Gravity.TOP || mGravity == Gravity.BOTTOM) 2 else 1
            } else {
                mFloatingAnimation.direction
            }
            val property = if (direction == 2) "translationY" else "translationX"
            mAnimator = ObjectAnimator.ofFloat(mTextView, property, -endValue, endValue)
            mAnimator!!.setDuration(duration)
            mAnimator!!.setInterpolator(AccelerateDecelerateInterpolator())
            mAnimator!!.setRepeatCount(ValueAnimator.INFINITE)
            mAnimator!!.setRepeatMode(ValueAnimator.REVERSE)
            mAnimator!!.start()
        }

        fun getAnchorPoint(gravity: Gravity?, outPoint: Point) {
            if (gravity == Gravity.BOTTOM) {
                outPoint.x = mViewRect!!.centerX()
                outPoint.y = mViewRect!!.bottom
            } else if (gravity == Gravity.TOP) {
                outPoint.x = mViewRect!!.centerX()
                outPoint.y = mViewRect!!.top
            } else if (gravity == Gravity.RIGHT) {
                outPoint.x = mViewRect!!.right
                outPoint.y = mViewRect!!.centerY()
            } else if (gravity == Gravity.LEFT) {
                outPoint.x = mViewRect!!.left
                outPoint.y = mViewRect!!.centerY()
            } else if (mGravity == Gravity.CENTER) {
                outPoint.x = mViewRect!!.centerX()
                outPoint.y = mViewRect!!.centerY()
            }
            outPoint.x -= mDrawRect.left
            outPoint.y -= mDrawRect.top
            if (!mHideArrow) {
                if (gravity == Gravity.LEFT || gravity == Gravity.RIGHT) {
                    outPoint.y -= mPadding / 2
                } else if (gravity == Gravity.TOP || gravity == Gravity.BOTTOM) {
                    outPoint.x -= mPadding / 2
                }
            }
        }

        override fun setText(text: CharSequence?) {
            mText = text
            if (null != mTextView) {
                mTextView!!.text = Html.fromHtml(text as String?)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (!mAttached || !mShowing || !isShown || mClosePolicy == ClosePolicy.NONE) {
                return false
            }
            val action = event.actionMasked
            if (!mActivated && mActivateDelay > 0) {
                return false
            }
            if (action == MotionEvent.ACTION_DOWN) {
                val outRect = Rect()
                mView!!.getGlobalVisibleRect(outRect)
                var containsTouch = outRect.contains(event.x.toInt(), event.y.toInt())
                if (null != mViewOverlay) {
                    mViewOverlay!!.getGlobalVisibleRect(outRect)
                    containsTouch = containsTouch or outRect.contains(event.x.toInt(), event.y.toInt())
                }
                if (containsTouch) {
                    if (ClosePolicy.touchInside(mClosePolicy)) {
                        onClose(true, true, false)
                    }
                    return ClosePolicy.consumeInside(mClosePolicy)
                }
                if (ClosePolicy.touchOutside(mClosePolicy)) {
                    onClose(true, false, false)
                }
                return ClosePolicy.consumeOutside(mClosePolicy)
            }
            return false
        }

        override fun onDraw(canvas: Canvas) {
            if (!mAttached) {
                return
            }
            super.onDraw(canvas)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            var myWidth = 0
            var myHeight = 0
            val widthMode = MeasureSpec.getMode(widthMeasureSpec)
            val heightMode = MeasureSpec.getMode(heightMeasureSpec)
            val widthSize = MeasureSpec.getSize(widthMeasureSpec)
            val heightSize = MeasureSpec.getSize(heightMeasureSpec)
            if (widthMode != MeasureSpec.UNSPECIFIED) {
                myWidth = widthSize
            }
            if (heightMode != MeasureSpec.UNSPECIFIED) {
                myHeight = heightSize
            }
            if (null != mView) {
                if (mView!!.visibility != GONE) {
                    val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(myWidth, MeasureSpec.AT_MOST)
                    val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(myHeight, MeasureSpec.AT_MOST)
                    mView!!.measure(childWidthMeasureSpec, childHeightMeasureSpec)
                } else {
                    myWidth = 0
                    myHeight = 0
                }
            }
            if (null != mViewOverlay && mViewOverlay!!.visibility != GONE) {
                val childWidthMeasureSpec: Int
                val childHeightMeasureSpec: Int
                childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.AT_MOST)
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST)
                mViewOverlay!!.measure(childWidthMeasureSpec, childHeightMeasureSpec)
            }
            setMeasuredDimension(myWidth, myHeight)
        }

        private fun onClose(fromUser: Boolean, containsTouch: Boolean, immediate: Boolean) {
            if (null != mCallback) {
                mCallback!!.onTooltipClose(this, fromUser, containsTouch)
            }
            hide(if (immediate) 0 else mFadeDuration)
        }

        companion object {
            const val TOLERANCE_VALUE = 10
            private val GRAVITY_LIST: List<Gravity?> =
                ArrayList(Arrays.asList(Gravity.LEFT, Gravity.RIGHT, Gravity.TOP, Gravity.BOTTOM, Gravity.CENTER))
        }

        init {
            val theme = context.theme
                .obtainStyledAttributes(null, R.styleable.TooltipLayout, builder.defStyleAttr, builder.defStyleRes)
            mPadding = theme.getDimensionPixelSize(R.styleable.TooltipLayout_ttlm_padding, 30)
            mTextAppearance = theme.getResourceId(R.styleable.TooltipLayout_android_textAppearance, 0)
            mTextGravity = theme
                .getInt(
                    R.styleable.TooltipLayout_android_gravity,
                    android.view.Gravity.TOP or android.view.Gravity.START
                )
            mTextViewElevation = theme.getDimension(R.styleable.TooltipLayout_ttlm_elevation, 0f)
            val overlayStyle =
                theme.getResourceId(R.styleable.TooltipLayout_ttlm_overlayStyle, R.style.ToolTipOverlayDefaultStyle)
            theme.recycle()
            mToolTipId = builder.id
            mText = builder.text
            mGravity = builder.gravity
            mTextResId = builder.textResId
            mMaxWidth = builder.maxWidth
            mTopRule = builder.actionbarSize
            mClosePolicy = builder.closePolicy
            mShowDuration = builder.showDuration
            mShowDelay = builder.showDelay
            mHideArrow = builder.hideArrow
            mActivateDelay = builder.activateDelay
            mRestrict = builder.restrictToScreenEdges
            mFadeDuration = builder.fadeDuration
            mCallback = builder.closeCallback
            mFloatingAnimation = builder.floatingAnimation
            mSizeTolerance = (context.resources.displayMetrics.density * TOLERANCE_VALUE).toInt()
            clipChildren = false
            clipToPadding = false
            if (null != builder.point) {
                mPoint = Point(builder.point!!)
                mPoint!!.y += mTopRule
            } else {
                mPoint = null
            }
            mDrawRect = Rect()
            if (null != builder.view) {
                mViewRect = Rect()
                builder.view!!.getHitRect(mHitRect)
                builder.view!!.getLocationOnScreen(mTempLocation)
                mViewRect!!.set(mHitRect)
                mViewRect!!.offsetTo(mTempLocation[0], mTempLocation[1])
                mViewAnchor = WeakReference(builder.view)
                if (builder.view!!.viewTreeObserver.isAlive) {
                    builder.view!!.viewTreeObserver.addOnGlobalLayoutListener(mGlobalLayoutListener)
                    builder.view!!.viewTreeObserver.addOnPreDrawListener(mPreDrawListener)
                    builder.view!!.addOnAttachStateChangeListener(mAttachedStateListener)
                }
            }
            if (builder.overlay) {
                mViewOverlay = TooltipOverlay(getContext(), null, 0, overlayStyle)
                mViewOverlay!!.adjustViewBounds = true
                mViewOverlay!!.layoutParams = LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                )
            }
            if (!builder.isCustomView) {
                mDrawable = TooltipTextDrawable(context, builder)
            } else {
                mDrawable = null
                mIsCustomView = true
            }
            visibility = INVISIBLE
        }
    }

    class AnimationBuilder {
        var radius = 8
        var direction = 0
        var duration: Long = 400
        var completed = false
        fun setRadius(value: Int): AnimationBuilder {
            throwIfCompleted()
            radius = value
            return this
        }

        private fun throwIfCompleted() {
            check(!completed) { "Builder cannot be modified" }
        }

        fun build(): AnimationBuilder {
            throwIfCompleted()
            completed = true
            return this
        }
    }

    class Builder(var id: Int) {
        var text: CharSequence? = null
        var view: View? = null
        var gravity: Gravity? = null
        var actionbarSize = 0
        var textResId = R.layout.tooltip_textview
        var closePolicy = ClosePolicy.NONE
        var showDuration: Long = 0
        var point: Point? = null
        var showDelay: Long = 0
        var hideArrow = false
        var maxWidth = -1
        var defStyleRes = R.style.ToolTipLayoutDefaultStyle
        var defStyleAttr = R.attr.ttlm_defaultStyle
        var activateDelay: Long = 0
        var isCustomView = false
        var restrictToScreenEdges = true
        var fadeDuration: Long = 200
        var closeCallback: Callback? = null
        var completed = false
        var overlay = true
        var floatingAnimation: AnimationBuilder? = null
        var typeface: Typeface? = null
        private fun throwIfCompleted() {
            check(!completed) { "Builder cannot be modified" }
        }

        fun fitToScreen(value: Boolean): Builder {
            throwIfCompleted()
            restrictToScreenEdges = value
            return this
        }

        fun fadeDuration(ms: Long): Builder {
            throwIfCompleted()
            fadeDuration = ms
            return this
        }

        fun text(res: Resources, @StringRes resId: Int): Builder {
            return text(res.getString(resId))
        }

        fun text(text: CharSequence?): Builder {
            throwIfCompleted()
            this.text = text
            return this
        }

        fun maxWidth(maxWidth: Int): Builder {
            throwIfCompleted()
            this.maxWidth = maxWidth
            return this
        }

        fun withOverlay(value: Boolean): Builder {
            throwIfCompleted()
            overlay = value
            return this
        }

        fun anchor(view: View?, gravity: Gravity?): Builder {
            throwIfCompleted()
            point = null
            this.view = view
            this.gravity = gravity
            return this
        }

        @Deprecated("")
        fun toggleArrow(show: Boolean): Builder {
            return withArrow(show)
        }

        fun withArrow(show: Boolean): Builder {
            throwIfCompleted()
            hideArrow = !show
            return this
        }

        fun closePolicy(policy: ClosePolicy, milliseconds: Long): Builder {
            throwIfCompleted()
            closePolicy = policy.build()
            showDuration = milliseconds
            return this
        }

        fun showDelay(ms: Long): Builder {
            throwIfCompleted()
            showDelay = ms
            return this
        }

        fun build(): Builder {
            throwIfCompleted()
            if (floatingAnimation != null) {
                check(floatingAnimation!!.completed) { "Builder not closed" }
            }
            completed = true
            overlay = overlay && gravity != Gravity.CENTER
            return this
        }

        companion object {
            private const val sNextId = 0
        }
    }
}