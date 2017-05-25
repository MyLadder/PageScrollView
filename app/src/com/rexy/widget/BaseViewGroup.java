package com.rexy.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

/**
 * @author: renzheng
 * @date: 2017-04-25 09:32
 */
public abstract class BaseViewGroup extends ViewGroup {
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SETTLING = 2;
    private static final int[] ATTRS_MAX_SIZE_GRAVITY = new int[]
            {android.R.attr.gravity, android.R.attr.maxWidth, android.R.attr.maxHeight};

    private int mGravity;
    private int mMaxWidth = -1;
    private int mMaxHeight = -1;
    protected Rect mVisibleBounds = new Rect();
    private int[] mContentSizeState = new int[]{0, 0, 0};
    private OnScrollChangeListener mScrollListener;

    protected int mScrollState = SCROLL_STATE_IDLE;
    protected boolean mAttachLayout = false;

    private String mLogTag;
    private long mTimeMeasureStart, mTimeLayoutStart;

    public BaseViewGroup(Context context) {
        super(context);
        init(context, null);
    }

    public BaseViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BaseViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public BaseViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, ATTRS_MAX_SIZE_GRAVITY);
            mGravity = a.getInt(0, mGravity);
            mMaxWidth = a.getDimensionPixelSize(1, mMaxWidth);
            mMaxHeight = a.getDimensionPixelSize(2, mMaxHeight);
            a.recycle();
        }
    }

    public void setLogTag(String logTag) {
        mLogTag = logTag;
    }

    public boolean isLogAccess() {
        return mLogTag != null;
    }

    protected void print(CharSequence msg) {
        if (mLogTag != null && mLogTag.length() > 0) {
            Log.d(mLogTag, String.valueOf(msg));
        }
    }

    public int getContentWidth() {
        return mContentSizeState[0];
    }

    public int getContentHeight() {
        return mContentSizeState[1];
    }

    public int getMeasureState() {
        return mContentSizeState[2];
    }

    protected void setContentSize(int contentWidth, int contentHeight) {
        mContentSizeState[0] = contentWidth;
        mContentSizeState[1] = contentHeight;
    }

    protected void setMeasureState(int childMeasureState) {
        mContentSizeState[2] = childMeasureState;
    }

    public int getGravity() {
        return mGravity;
    }

    public int getMaxWidth() {
        return mMaxWidth;
    }

    public int getMaxHeight() {
        return mMaxHeight;
    }

    public int getScrollState() {
        return mScrollState;
    }

    public OnScrollChangeListener getScrollChangeListener() {
        return mScrollListener;
    }

    public boolean isAttachLayoutFinished() {
        return mAttachLayout;
    }

    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            requestLayout();
        }
    }

    public void setMaxWidth(int maxWidth) {
        if (mMaxWidth != maxWidth) {
            mMaxWidth = maxWidth;
            requestLayout();
        }
    }

    public void setMaxHeight(int maxHeight) {
        if (mMaxHeight != maxHeight) {
            mMaxHeight = maxHeight;
            requestLayout();
        }
    }

    public int getWidthWithoutPadding() {
        return mVisibleBounds.width();
    }

    public int getHeightWithoutPadding() {
        return mVisibleBounds.height();
    }

    public void setOnScrollChangeListener(OnScrollChangeListener l) {
        mScrollListener = l;
    }

    public Rect getVisibleBounds() {
        return mVisibleBounds;
    }

    protected void markAsWillDragged(boolean disallowParentTouch) {
        if (disallowParentTouch) {
            ViewParent parent = getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
        }
        setScrollState(SCROLL_STATE_DRAGGING);
    }

    protected void markAsWillScroll() {
        setScrollState(SCROLL_STATE_SETTLING);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    protected void markAsWillIdle() {
        setScrollState(SCROLL_STATE_IDLE);
    }

    private void setScrollState(int newState) {
        if (mScrollState != newState) {
            int preState = mScrollState;
            mScrollState = newState;
            if (isLogAccess()) {
                print(String.format("stateChanged: %d to %d", preState, newState));
            }
            onScrollStateChanged(newState, preState);
            if (mScrollListener != null) {
                mScrollListener.onScrollStateChanged(mScrollState, preState);
            }
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int ol, int ot) {
        super.onScrollChanged(l, t, ol, ot);
        if (mScrollState != SCROLL_STATE_IDLE) {
            awakenScrollBars();
        }
        computeVisibleBounds(l, t, true);
        if (mScrollListener != null) {
            mScrollListener.onScrollChanged(l, t, ol, ot);
        }
    }

    protected void onScrollStateChanged(int newState, int prevState) {
    }

    protected void onScrollChanged(int scrollX, int scrollY, Rect visibleBounds, boolean fromScrollChanged) {
        if (isLogAccess()) {
            StringBuilder sb = new StringBuilder(32);
            sb.append("scrollChanged: scrollX=").append(scrollX);
            sb.append(",scrollY=").append(scrollY).append(",visibleBounds=").append(visibleBounds);
            sb.append(",scrollChanged=").append(fromScrollChanged);
            print(sb);
        }
    }

    @Override
    protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mTimeMeasureStart = System.currentTimeMillis();
        mContentSizeState[0] = mContentSizeState[1] = mContentSizeState[2] = 0;
        final int paddingHorizontal = getPaddingLeft() + getPaddingRight();
        final int paddingVertical = getPaddingTop() + getPaddingBottom();
        final int mostWidthNoPadding = Math.max(MeasureSpec.getSize(widthMeasureSpec) - paddingHorizontal, 0);
        final int mostHeightNoPadding = Math.max(MeasureSpec.getSize(heightMeasureSpec) - paddingVertical, 0);
        dispatchMeasure(
                MeasureSpec.makeMeasureSpec(mostWidthNoPadding, MeasureSpec.getMode(widthMeasureSpec)),
                MeasureSpec.makeMeasureSpec(mostHeightNoPadding, MeasureSpec.getMode(heightMeasureSpec)),
                mostWidthNoPadding,
                mostHeightNoPadding
        );
        int contentWidth = mContentSizeState[0], contentHeight = mContentSizeState[1], childState = mContentSizeState[2];
        int maxWidth = Math.max(contentWidth + paddingHorizontal, getSuggestedMinimumWidth());
        int maxHeight = Math.max(contentHeight + paddingVertical, getSuggestedMinimumHeight());
        if (mMaxWidth > 0 && maxWidth > mMaxWidth) {
            maxWidth = mMaxWidth;
        }
        if (mMaxHeight > 0 && maxHeight > mMaxHeight) {
            maxHeight = mMaxHeight;
        }
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT));
        doAfterMeasure(getMeasuredWidth(), getMeasuredHeight(), contentWidth, contentHeight);
    }

    @Override
    protected final void onLayout(boolean changed, int l, int t, int r, int b) {
        mTimeLayoutStart = System.currentTimeMillis();
        int left = getPaddingLeft(), top = getPaddingTop();
        int right = getPaddingRight(), bottom = getPaddingBottom();
        int selfWidth = r - l, selfHeight = b - t;
        int contentLeft = getContentStart(left, selfWidth - right, getContentWidth(), mGravity, true);
        int contentTop = getContentStart(top, selfHeight - bottom, getContentHeight(), mGravity, false);
        dispatchLayout(contentLeft, contentTop, left, top, selfWidth, selfHeight);
        boolean firstAttachLayout = false;
        if (!mAttachLayout) {
            firstAttachLayout = mAttachLayout = true;
            int scrollX = getScrollX(), scrollY = getScrollY();
            computeVisibleBounds(scrollX, scrollY, false);
        }
        doAfterLayout(firstAttachLayout);
    }

    /**
     * in this function must call setContentSize(contentWidth,contentHeight);
     *
     * @param widthMeasureSpecNoPadding  widthMeasureSpec with out padding
     * @param heightMeasureSpecNoPadding heightMeasureSpec with out padding
     * @param maxSelfWidthNoPadding      max width with out padding can allowed by its child.
     * @param maxSelfHeightNoPadding     max height with out padding can allowed by its child.
     */
    protected abstract void dispatchMeasure(int widthMeasureSpecNoPadding, int heightMeasureSpecNoPadding, int maxSelfWidthNoPadding, int maxSelfHeightNoPadding);

    protected void doAfterMeasure(int measuredWidth, int measuredHeight, int contentWidth, int contentHeight) {
        if (isLogAccess()) {
            print(String.format("measure(%d ms): [measuredWidth=%d , measuredHeight=%d],[contentWidth=%d,contentHeight=%d]", (System.currentTimeMillis() - mTimeMeasureStart), measuredWidth, measuredHeight, contentWidth, contentHeight));
        }
    }

    protected abstract void dispatchLayout(int contentleft, int contentTop, int paddingLeft, int paddingTop, int selfWidth, int selfHeight);

    protected void doAfterLayout(boolean firstAttachLayout) {
        if (isLogAccess()) {
            print(String.format("layout(%d ms): firstAttachLayout=%s", (System.currentTimeMillis() - mTimeLayoutStart), firstAttachLayout));
        }
    }

    protected boolean isChildNotGone(View child) {
        return child != null && child.getVisibility() != View.GONE && child.getParent() == BaseViewGroup.this;
    }

    protected boolean skipChild(View child) {
        return child == null || child.getVisibility() == View.GONE;
    }

    protected int getContentStart(int containerStart, int containerEnd, int contentWillSize, int contentGravity, boolean horizontalDirection) {
        int start = containerStart;
        if (contentGravity != -1) {
            final int mask = horizontalDirection ? Gravity.HORIZONTAL_GRAVITY_MASK : Gravity.VERTICAL_GRAVITY_MASK;
            final int maskCenter = horizontalDirection ? Gravity.CENTER_HORIZONTAL : Gravity.CENTER_VERTICAL;
            final int maskEnd = horizontalDirection ? Gravity.RIGHT : Gravity.BOTTOM;
            final int okGravity = contentGravity & mask;
            if (maskCenter == okGravity) {
                start = containerStart + (containerEnd - containerStart - contentWillSize) / 2;
            } else if (maskEnd == okGravity) {
                start = containerEnd - contentWillSize;
            }
        }
        return start;
    }

    protected int offsetX(View child, boolean centreInVisibleBounds, boolean marginInclude) {
        int current;
        MarginLayoutParams marginLp = (marginInclude && child.getLayoutParams() instanceof MarginLayoutParams) ? (MarginLayoutParams) child.getLayoutParams() : null;
        if (centreInVisibleBounds) {
            current = (child.getLeft() + child.getRight()) >> 1;
            if (marginLp != null) {
                current = current + (marginLp.rightMargin - marginLp.leftMargin) / 2;
            }
            return current - mVisibleBounds.centerX() + mVisibleBounds.left - getPaddingLeft();
        } else {
            current = child.getLeft();
            if (marginLp != null) {
                current = current - marginLp.leftMargin;
            }
            return current - getPaddingLeft();
        }
    }

    protected int offsetY(View child, boolean centreInVisibleBounds, boolean marginInclude) {
        int current;
        MarginLayoutParams marginLp = (marginInclude && child.getLayoutParams() instanceof MarginLayoutParams) ? (MarginLayoutParams) child.getLayoutParams() : null;
        if (centreInVisibleBounds) {
            current = (child.getTop() + child.getBottom()) >> 1;
            if (marginLp != null) {
                current = current + (marginLp.bottomMargin - marginLp.topMargin) / 2;
            }
            return current - mVisibleBounds.centerY() + mVisibleBounds.top - getPaddingTop();
        } else {
            current = child.getTop();
            if (marginLp != null) {
                current = current - marginLp.topMargin;
            }
            return current - getPaddingTop();
        }
    }

    protected int getVerticalScrollRange() {
        int scrollRange = 0, contentSize = getContentHeight();
        if (contentSize > 0) {
            scrollRange = contentSize - mVisibleBounds.height();
            if (scrollRange < 0) {
                scrollRange = 0;
            }
        }
        return scrollRange;
    }

    protected int getHorizontalScrollRange() {
        int scrollRange = 0, contentSize = getContentWidth();
        if (contentSize > 0) {
            scrollRange = contentSize - mVisibleBounds.width();
            if (scrollRange < 0) {
                scrollRange = 0;
            }
        }
        return scrollRange;
    }

    protected int getScrollRange(boolean horizontal) {
        return horizontal ? getHorizontalScrollRange() : getVerticalScrollRange();
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return Math.max(0, super.computeHorizontalScrollOffset());
    }

    @Override
    protected int computeVerticalScrollRange() {
        final int count = getChildCount();
        final int paddingTop = getPaddingTop();
        final int contentHeight = mVisibleBounds.height();
        if (count == 0) {
            return contentHeight;
        }
        int scrollRange = paddingTop + getContentHeight();
        final int scrollY = getScrollY();
        final int overScrollBottom = Math.max(0, scrollRange - contentHeight);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overScrollBottom) {
            scrollRange += scrollY - overScrollBottom;
        }
        return scrollRange;
    }

    @Override
    protected int computeHorizontalScrollRange() {
        final int count = getChildCount();
        final int paddingLeft = getPaddingLeft();
        final int contentWidth = mVisibleBounds.width();
        if (count == 0) {
            return contentWidth;
        }
        int scrollRange = paddingLeft + getContentWidth();
        final int scrollX = getScrollX();
        final int overScrollRight = Math.max(0, scrollRange - contentWidth);
        if (scrollX < 0) {
            scrollRange -= scrollX;
        } else if (scrollX > overScrollRight) {
            scrollRange += scrollX - overScrollRight;
        }
        return scrollRange;
    }

    protected void computeVisibleBounds(int scrollX, int scrollY, boolean scrollChanged) {
        int beforeHash = mVisibleBounds.hashCode(), width = getWidth(), height = getHeight();
        if (width <= 0) {
            width = getMeasuredWidth();
        }
        if (height <= 0) {
            height = getMeasuredHeight();
        }
        mVisibleBounds.left = getPaddingLeft() + scrollX;
        mVisibleBounds.top = getPaddingTop() + scrollY;
        mVisibleBounds.right = mVisibleBounds.left + width - getPaddingLeft() - getPaddingRight();
        mVisibleBounds.bottom = mVisibleBounds.top + height - getPaddingTop() - getPaddingBottom();
        if (beforeHash != mVisibleBounds.hashCode()) {
            onScrollChanged(scrollX, scrollY, mVisibleBounds, scrollChanged);
        }
    }

    @Override
    public void removeAllViewsInLayout() {
        super.removeAllViewsInLayout();
        mAttachLayout = false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachLayout = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachLayout = false;
    }

    public interface OnScrollChangeListener {
        void onScrollChanged(int scrollX, int scrollY, int oldScrollX, int oldScrollY);

        void onScrollStateChanged(int state, int oldState);
    }
}
