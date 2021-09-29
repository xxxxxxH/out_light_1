package net.basicmodel.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.OverScroller;

import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;

import net.basicmodel.R;
import net.basicmodel.interfaces.ISelect;

import java.util.ArrayList;
import java.util.List;

public class MyLightView extends View implements GestureDetector.OnGestureListener {

    private Paint paint;
    private TextPaint textPaint;
    private Bitmap bitmap;

    private int highLightColor;
    private int markColor, fadeColor;

    private List<String> data;
    private String content;
    private ISelect iSelect;
    private float factor = 1.2F;
    private float ratio = 0.7F;

    private int count;
    private final Path path = new Path();
    private float size;
    private int scopeSize;

    private OverScroller scroller;
    private float maxDistance;
    private RectF rect;
    private boolean isFling = false;
    private float centerTextSize, normalTextSize;
    private float topSpace, bottomSpace;
    private float intervalDis;
    private float centerWidth;
    private GestureDetectorCompat gestureDetector;

    private int lastIndex = -1;
    private int currentIndex = -1;

    private int minIndex = Integer.MIN_VALUE;
    private int maxIndex = Integer.MAX_VALUE;

    public MyLightView(Context context) {
        super(context);
    }

    public MyLightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MyLightView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    protected void init(AttributeSet attrs) {
        float density = getResources().getDisplayMetrics().density;
        centerWidth = (int) (density * 3.5f + 0.5f);

        highLightColor = 0xFFF74C39;
        markColor = 0xFFEEEEEE;
        size = density * 18;
        centerTextSize = density * 22;
        normalTextSize = density * 18;
        bottomSpace = density * 6;

        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.strobe_line);

        TypedArray ta = attrs == null ? null : getContext().obtainStyledAttributes(attrs, R.styleable.lwvWheelView);
        if (ta != null) {
            highLightColor = ta.getColor(R.styleable.lwvWheelView_lwvHighlightColor, highLightColor);
            markColor = ta.getColor(R.styleable.lwvWheelView_lwvMarkColor, markColor);
            factor = ta.getFloat(R.styleable.lwvWheelView_lwvIntervalFactor, factor);
            ratio = ta.getFloat(R.styleable.lwvWheelView_lwvMarkRatio, ratio);
            content = ta.getString(R.styleable.lwvWheelView_lwvAdditionalCenterMark);
            centerTextSize = ta.getDimension(R.styleable.lwvWheelView_lwvCenterMarkTextSize, centerTextSize);
            normalTextSize = ta.getDimension(R.styleable.lwvWheelView_lwvMarkTextSize, normalTextSize);
            size = ta.getDimension(R.styleable.lwvWheelView_lwvCursorSize, size);
        }
        fadeColor = highLightColor & 0xAAFFFFFF;
        factor = Math.max(0.5f, factor);
        ratio = Math.min(1, ratio);
        topSpace = size + density * 2;

        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(highLightColor);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(markColor);
        paint.setStrokeWidth(centerWidth);

        textPaint.setTextSize(centerTextSize);
        calcIntervalDis();

        scroller = new OverScroller(getContext());
        rect = new RectF();

        gestureDetector = new GestureDetectorCompat(getContext(), this);

        final List<String> items = new ArrayList<>();
        int NUMBER_MARK = 9;
        for (int i = 0; i <= NUMBER_MARK; i++) {
            items.add(String.valueOf(i));
        }
        setItems(items);
        int SELECT_INDEX = 0;
        selectIndex(SELECT_INDEX);
    }

    private void calcIntervalDis() {
        if (textPaint == null) {
            return;
        }
        String defaultText = "888888";
        Rect temp = new Rect();
        int max = 0;
        if (data != null && data.size() > 0) {
            for (String i : data) {
                textPaint.getTextBounds(i, 0, i.length(), temp);
                if (temp.width() > max) {
                    max = temp.width();
                }
            }
        } else {
            textPaint.getTextBounds(defaultText, 0, defaultText.length(), temp);
            max = temp.width();
        }

        if (!TextUtils.isEmpty(content)) {
            textPaint.setTextSize(normalTextSize);
            textPaint.getTextBounds(content, 0, content.length(), temp);
            max += temp.width();
        }

        intervalDis = max * factor;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int widthMeasureSpec) {
        int measureMode = MeasureSpec.getMode(widthMeasureSpec);
        int measureSize = MeasureSpec.getSize(widthMeasureSpec);
        int result = getSuggestedMinimumWidth();
        switch (measureMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                result = measureSize;
                break;
            default:
                break;
        }
        return result;
    }

    private int measureHeight(int heightMeasure) {
        int measureMode = MeasureSpec.getMode(heightMeasure);
        int measureSize = MeasureSpec.getSize(heightMeasure);
        int result = (int) (bottomSpace + topSpace * 3 + centerTextSize);
        switch (measureMode) {
            case MeasureSpec.EXACTLY:
                result = Math.max(result, measureSize);
                break;
            case MeasureSpec.AT_MOST:
                result = Math.min(result, measureSize);
                break;
            default:
                break;
        }
        return result;
    }

    public void fling(int velocityX, int velocityY) {
        scroller.fling(getScrollX(), getScrollY(),
                velocityX, velocityY,
                (int) (-maxDistance + minIndex * intervalDis), (int) (rect.width() - maxDistance - (count - 1 - maxIndex) * intervalDis),
                0, 0,
                (int) maxDistance, 0);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            maxDistance = w / 2.f;
            rect.set(0, 0, (count - 1) * intervalDis, h);
            scopeSize = (int) Math.ceil(maxDistance / intervalDis);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int start = currentIndex - scopeSize;
        int end = currentIndex + scopeSize + 1;

        start = Math.max(start, -scopeSize * 2);
        end = Math.min(end, count + scopeSize * 2);

        if (currentIndex == maxIndex) {
            end += scopeSize;
        } else if (currentIndex == minIndex) {
            start -= scopeSize;
        }

        float x = start * intervalDis;

        for (int i = start; i < end; i++) {
            float tempDis = intervalDis / 5f;
            for (int offset = -2; offset < 3; offset++) {
                float ox = x + offset * tempDis;

                if (i >= 0 && i <= count && currentIndex == i) {
                    int tempOffset = Math.abs(offset);
                    if (tempOffset == 0) {
                        paint.setColor(highLightColor);
                    } else if (tempOffset == 1) {
                        paint.setColor(fadeColor);
                    } else {
                        paint.setColor(markColor);
                    }
                } else {
                    paint.setColor(markColor);
                }

                if (offset == 0) {
                    paint.setStrokeWidth(centerWidth);
                    canvas.drawBitmap(bitmap, ox - bitmap.getWidth() / 2f, -10, null);
                }
            }
            x += intervalDis;
        }
        path.reset();
        float sizeDiv2 = size / 2f;
        float sizeDiv3 = size / 3f;
        path.moveTo(maxDistance - sizeDiv2 + getScrollX(), 0);
        path.rLineTo(0, sizeDiv3);
        path.rLineTo(sizeDiv2, sizeDiv2);
        path.rLineTo(sizeDiv2, -sizeDiv2);
        path.rLineTo(0, -sizeDiv3);
        path.close();

        paint.setColor(highLightColor);
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (data == null || data.size() == 0 || !isEnabled()) {
            return false;
        }
        boolean ret = gestureDetector.onTouchEvent(event);
        if (!isFling && MotionEvent.ACTION_UP == event.getAction()) {
            autoSettle();
            ret = true;
        }
        return ret || super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            refreshCenter();
            invalidate();
        } else {
            if (isFling) {
                isFling = false;
                autoSettle();
            }
        }
    }

    private void autoSettle() {
        int sx = getScrollX();
        float dx = currentIndex * intervalDis - sx - maxDistance;
        scroller.startScroll(sx, 0, (int) dx, 0);
        postInvalidate();
        if (lastIndex != currentIndex) {
            lastIndex = currentIndex;
            if (null != iSelect) {
                iSelect.onSelected(this, currentIndex);
            }
        }
    }

    private int safeCenter(int center) {
        if (center < minIndex) {
            center = minIndex;
        } else if (center > maxIndex) {
            center = maxIndex;
        }
        return center;
    }

    private void refreshCenter(int offsetX) {
        int offset = (int) (offsetX + maxDistance);
        int tempIndex = Math.round(offset / intervalDis);
        tempIndex = safeCenter(tempIndex);
        if (currentIndex == tempIndex) {
            return;
        }
        currentIndex = tempIndex;
        if (null != iSelect) {
            iSelect.onChanged(this, currentIndex);
        }
    }

    private void refreshCenter() {
        refreshCenter(getScrollX());
    }

    public void selectIndex(int index) {
        currentIndex = index;
        post(new Runnable() {
            @Override
            public void run() {
                scrollTo((int) (currentIndex * intervalDis - maxDistance), 0);
                invalidate();
                refreshCenter();
            }
        });
    }

    public List<String> getItems() {
        return data;
    }

    public void setItems(List<String> items) {
        if (data == null) {
            data = new ArrayList<>();
        } else {
            data.clear();
        }
        data.addAll(items);
        count = null == data ? 0 : data.size();
        if (count > 0) {
            minIndex = Math.max(minIndex, 0);
            maxIndex = Math.min(maxIndex, count - 1);
        }
        rect.set(0, 0, (count - 1) * intervalDis, getMeasuredHeight());
        currentIndex = Math.min(currentIndex, count);
        calcIntervalDis();
        invalidate();
    }

    public int getSelectedPosition() {
        return currentIndex;
    }

    public void setISelect(ISelect iSelect) {
        this.iSelect = iSelect;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (!scroller.isFinished()) {
            scroller.forceFinished(false);
        }
        isFling = false;
        if (null != getParent()) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        playSoundEffect(SoundEffectConstants.CLICK);
        refreshCenter((int) (getScrollX() + e.getX() - maxDistance));
        autoSettle();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float dis = distanceX;
        float scrollX = getScrollX();
        if (scrollX < minIndex * intervalDis - 2 * maxDistance) {
            dis = 0;
        } else if (scrollX < minIndex * intervalDis - maxDistance) {
            dis = distanceX / 4.f;
        } else if (scrollX > rect.width() - (count - maxIndex - 1) * intervalDis) {
            dis = 0;
        } else if (scrollX > rect.width() - (count - maxIndex - 1) * intervalDis - maxDistance) {
            dis = distanceX / 4.f;
        }
        scrollBy((int) dis, 0);
        refreshCenter();
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float scrollX = getScrollX();
        if (scrollX < -maxDistance + minIndex * intervalDis || scrollX > rect.width() - maxDistance - (count - 1 - maxIndex) * intervalDis) {
            return false;
        } else {
            isFling = true;
            fling((int) -velocityX, 0);
            return true;
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.index = getSelectedPosition();
        savedState.min = minIndex;
        savedState.max = maxIndex;
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        minIndex = savedState.min;
        maxIndex = savedState.max;
        selectIndex(savedState.index);
        requestLayout();
    }

    static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int index;
        int min;
        int max;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            index = in.readInt();
            min = in.readInt();
            max = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(index);
            out.writeInt(min);
            out.writeInt(max);
        }

        @Override
        public String toString() {
            return "WheelView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " index=" + index + " min=" + min + " max=" + max + "}";
        }
    }


}

