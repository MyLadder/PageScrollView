package com.rexy.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.OverScroller;

import com.rexy.pagescrollview.R;

/**
 * TODO:功能说明
 *
 * @author: renzheng
 * @date: 2017-04-25 09:32
 */
public class PageScrollView extends ViewGroup {
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SETTLING = 2;

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    private static final int MAX_DURATION = 600;
    private static final int FLOAT_VIEW_SCROLL = 1;

    protected int mOrientation;
    protected int mGravity;
    protected int mMaxWidth = -1;
    protected int mMaxHeight = -1;
    protected int mMiddleMargin = 0;
    protected float mSizeFixedPercent = 0;
    protected boolean isViewPagerStyle = false;


    protected int mFloatViewStart = -1;
    protected int mFloatViewEnd = -1;
    protected boolean isChildCenter = false;
    protected boolean isChildFillParent = false;
    protected boolean mAttachLayout = false;

    protected int mSwapViewIndex = -1;
    protected int mFloatViewStartIndex = -1;
    protected int mFloatViewEndIndex = -1;

    protected int mFloatViewStartMode = 0;
    protected int mFloatViewEndMode = 0;

    //目前只保证 pageHeader pageFooter 在item View 添加完后再设置。
    protected View mPageHeaderView;
    protected View mPageFooterView;

    private int mContentWidth;
    private int mContentHeight;


    int mTouchSlop;
    int mMinDistance;
    int mMinimumVelocity;
    int mMaximumVelocity;
    private int mOverFlingDistance;

    int mCurrItem = 0;
    int mPrevItem = -1;
    int mFirstVisiblePosition = -1;
    int mLastVisiblePosition = -1;
    int mVirtualCount = 0;
    int mScrollState = SCROLL_STATE_IDLE;
    boolean mIsBeingDragged = false;
    boolean mScrollerUsed = false;
    boolean mNeedResolveFloatOffset = false;

    PointF mPointDown = new PointF();
    PointF mPointLast = new PointF();
    //index,offset,duration,center
    Rect mScrollInfo = new Rect(-1, -1, -1, -1);
    VelocityTracker mVelocityTracker = null;
    OverScroller mScrollerScrollView = null;
    OverScroller mScrollerPageView = null;

    OnScrollChangeListener mScrollListener;
    PageTransformer mPageTransformer;
    OnPageChangeListener mPageListener = null;
    OnVisibleRangeChangeListener mOnVisibleRangeChangeListener = null;

    public PageScrollView(Context context) {
        super(context);
        init(context, null);
    }

    public PageScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PageScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public PageScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void print(CharSequence msg) {
        if (mLogEnable) {
            Log.d("PageScrollView", String.valueOf(msg));
        }
    }

    boolean mLogEnable;

    public void setLogEnable(boolean logEnable) {
        mLogEnable = logEnable;
    }

    private void init(Context context, AttributeSet attributeSet) {
        final float density = context.getResources().getDisplayMetrics().density;
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = (int) (350 * density);
        mMinDistance = (int) (mTouchSlop * 1.2f);
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mOverFlingDistance = configuration.getScaledOverflingDistance() * 2;
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        TypedArray attr = attributeSet == null ? null : context.obtainStyledAttributes(attributeSet, R.styleable.PageScrollView);
        if (attr != null) {
            mGravity = attr.getInt(R.styleable.PageScrollView_android_gravity, mGravity);
            mMaxWidth = attr.getDimensionPixelSize(R.styleable.PageScrollView_android_maxWidth, mMaxWidth);
            mMaxHeight = attr.getDimensionPixelSize(R.styleable.PageScrollView_android_maxHeight, mMaxHeight);
            mOrientation = attr.getInt(R.styleable.PageScrollView_android_orientation, mOrientation);
            mMiddleMargin = attr.getDimensionPixelSize(R.styleable.PageScrollView_middleMargin, mMiddleMargin);
            mSizeFixedPercent = attr.getFloat(R.styleable.PageScrollView_sizeFixedPercent, mSizeFixedPercent);
            isViewPagerStyle = attr.getBoolean(R.styleable.PageScrollView_viewPagerStyle, isViewPagerStyle);
            mFloatViewStart = attr.getInt(R.styleable.PageScrollView_floatViewStartIndex, mFloatViewStart);
            mFloatViewEnd = attr.getInt(R.styleable.PageScrollView_floatViewEndIndex, mFloatViewEnd);
            isChildCenter = attr.getBoolean(R.styleable.PageScrollView_childCenter, isChildCenter);
            isChildFillParent = attr.getBoolean(R.styleable.PageScrollView_childFillParent, isChildFillParent);
            mOverFlingDistance = attr.getDimensionPixelSize(R.styleable.PageScrollView_overFlingDistance, mOverFlingDistance);
        }
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int orientation) {
        if (mOrientation != orientation && (orientation == HORIZONTAL || orientation == VERTICAL)) {
            mOrientation = orientation;
            if (!isViewPagerStyle) {
                boolean oldHorizontal = mOrientation == VERTICAL;
                mCurrItem = mAttachLayout && mFirstVisiblePosition >= 0 ? mFirstVisiblePosition : 0;
                resetPositionForFloatView(mFloatViewStartIndex, oldHorizontal);
                resetPositionForFloatView(mFloatViewEndIndex, oldHorizontal);
                mFloatViewStartIndex = -1;
                mSwapViewIndex = -1;
                mFloatViewStartMode = 0;
                mFloatViewEndIndex = -1;
                mFloatViewEndMode = 0;
            }
            mScrollInfo.set(mCurrItem, 0, 0, isViewPagerStyle ? 1 : 0);
            mAttachLayout = false;
            requestLayout();
        }
    }

    public int getGravity() {
        return mGravity;
    }

    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            requestLayout();
        }
    }

    public int getMaxWidth() {
        return mMaxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        if (mMaxWidth != maxWidth) {
            mMaxWidth = maxWidth;
            requestLayout();
        }
    }

    public int getMaxHeight() {
        return mMaxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        if (mMaxHeight != maxHeight) {
            mMaxHeight = maxHeight;
            requestLayout();
        }
    }

    public int getMiddleMargin() {
        return mMiddleMargin;
    }

    public void setMiddleMargin(int middleMargin) {
        if (mMiddleMargin != middleMargin) {
            mMiddleMargin = middleMargin;
            requestLayout();
        }
    }

    public int getFloatViewStartIndex() {
        return mFloatViewStart;
    }

    public void setFloatViewStartIndex(int floatStartIndex) {
        if (mFloatViewStart != floatStartIndex) {
            resetPositionForFloatView(mFloatViewStartIndex, mOrientation == HORIZONTAL);
            mFloatViewStart = floatStartIndex;
            if (mFloatViewStart >= 0) {
                mNeedResolveFloatOffset = true;
            }
            mSwapViewIndex = -1;
            mFloatViewStartIndex = -1;
            mFloatViewStartMode = 0;
            requestLayout();
        }
    }

    public int getFloatViewEndIndex() {
        return mFloatViewEnd;
    }

    public void setFloatViewEndIndex(int floatEndIndex) {
        if (mFloatViewEnd != floatEndIndex) {
            resetPositionForFloatView(mFloatViewEndIndex, mOrientation == HORIZONTAL);
            mFloatViewEnd = floatEndIndex;
            if (mFloatViewEnd >= 0) {
                mNeedResolveFloatOffset = true;
            }
            mFloatViewEndIndex = -1;
            mFloatViewEndMode = 0;
            requestLayout();
        }
    }

    public float getSizeFixedPercent() {
        return mSizeFixedPercent;
    }

    public void setSizeFixedPercent(float percent) {
        if (mSizeFixedPercent != percent && percent >= 0 && percent <= 0) {
            mSizeFixedPercent = percent;
            requestLayout();
        }
    }

    public void setOverFlingDistance(int overFlingDistance) {
        mOverFlingDistance = overFlingDistance;
    }

    public int getOverFlingDistance() {
        return mOverFlingDistance;
    }

    public boolean isViewPagerStyle() {
        return isViewPagerStyle;
    }

    public void setViewPagerStyle(boolean viewPagerStyle) {
        if (isViewPagerStyle != viewPagerStyle) {
            isViewPagerStyle = viewPagerStyle;
        }
    }

    public boolean isChildCenter() {
        return isChildCenter;
    }

    public void setChildCenter(boolean centerChild) {
        if (this.isChildCenter != centerChild) {
            this.isChildCenter = centerChild;
            if (mAttachLayout) {
                requestLayout();
            }
        }
    }

    public boolean isChildFillParent(){
        return isChildFillParent;
    }

    public void setChildFillParent(boolean childFillParent){
        if(isChildFillParent!=childFillParent){
            this.isChildFillParent = childFillParent;
            if (mAttachLayout) {
                requestLayout();
            }
        }
    }

    public View getPageHeaderView() {
        return mPageHeaderView;
    }

    public void setPageHeaderView(View headView) {
        if (mPageHeaderView != headView) {
            if (mPageHeaderView != null) {
                removeViewInLayout(mPageHeaderView);
            }
            mPageHeaderView = headView;
            if (mPageHeaderView != null) {
                addView(mPageHeaderView);
                mNeedResolveFloatOffset = true;
            }
            requestLayout();
        }
    }

    public View getPageFooterView() {
        return mPageFooterView;
    }

    public void setPageFooterView(View pageFooterView) {
        if (mPageFooterView != pageFooterView) {
            if (mPageFooterView != null) {
                removeViewInLayout(mPageFooterView);
            }
            mPageFooterView = pageFooterView;
            if (mPageFooterView != null) {
                addView(mPageFooterView);
                mNeedResolveFloatOffset = true;
            }
            requestLayout();
        }
    }

    public PageTransformer getPageTransformer() {
        return mPageTransformer;
    }

    public void setPageTransformer(PageTransformer transformer) {
        if (mPageTransformer != transformer) {
            PageTransformer oldTransformer = mPageTransformer;
            mPageTransformer = transformer;
            if (mAttachLayout) {
                if (oldTransformer != null && mPageTransformer == null) {
                    boolean horizontal = mOrientation == HORIZONTAL;
                    for (int i = 0; i < mVirtualCount; i++) {
                        oldTransformer.recoverTransformPage(getItemView(i), horizontal);
                    }
                }
                if (mPageTransformer != null) {
                    resolvePageOffset(mOrientation == HORIZONTAL ? getScrollX() : getScrollY(), mOrientation == HORIZONTAL);
                }
            }
        }
    }

    public OnPageChangeListener getPageChangeListener() {
        return mPageListener;
    }

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        mPageListener = listener;
    }

    public OnVisibleRangeChangeListener getVisibleRangeChangeListener() {
        return mOnVisibleRangeChangeListener;
    }

    public void setOnVisibleRangeChangeListener(OnVisibleRangeChangeListener l) {
        mOnVisibleRangeChangeListener = l;
    }
    public OnScrollChangeListener getScrollChangeListener() {
        return mScrollListener;
    }

    public void setOnScrollChangeListener(OnScrollChangeListener l) {
        mScrollListener = l;
    }

    public boolean hasPageHeaderView() {
        return isChildNotGone(mPageHeaderView);
    }

    public boolean hasPageFooterView() {
        return isChildNotGone(mPageFooterView);
    }

    public int getCurrentItem() {
        return mCurrItem;
    }

    public int getPrevItem() {
        return mPrevItem;
    }

    public int getScrollState() {
        return mScrollState;
    }

    protected OverScroller getScroller() {
        if (isViewPagerStyle) {
            if (mScrollerPageView == null) {
                mScrollerPageView = new OverScroller(getContext(), new Interpolator() {
                    @Override
                    public float getInterpolation(float t) {
                        t -= 1.0f;
                        return t * t * t * t * t + 1.0f;
                    }
                });
            }
            return mScrollerPageView;
        } else {
            if (mScrollerScrollView == null) {
                mScrollerScrollView = new OverScroller(getContext());
            }
            return mScrollerScrollView;
        }
    }

    protected boolean isChildNotGone(View child) {
        return child != null && child.getVisibility() != View.GONE && child.getParent() == PageScrollView.this;
    }

    protected View getVirtualChildAt(int index, boolean withoutGone) {
        int virtualCount = 0;
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if ((withoutGone && child.getVisibility() == View.GONE) || (child == mPageHeaderView || child == mPageFooterView))
                continue;
            if (virtualCount == index) {
                return child;
            }
            virtualCount++;
        }
        return null;
    }

    protected int getVirtualChildCount(boolean withoutGone) {
        int virtualCount = 0;
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if ((withoutGone && child.getVisibility() == View.GONE) || (child == mPageHeaderView || child == mPageFooterView))
                continue;
            virtualCount++;
        }
        return virtualCount;
    }

    public int getItemCount() {
        int pageCount = mVirtualCount;
        if (pageCount == 0) {
            pageCount = mVirtualCount = getVirtualChildCount(true);
        }
        return pageCount;
    }

    public View getItemView(int index) {
        View result = null;
        int pageCount = getItemCount();
        if (index >= 0 && index < pageCount) {
            result = getVirtualChildAt(index, true);
        }
        return result;
    }

    public int indexOfItemView(View view) {
        if (view != null) {
            int virtualIndex = 0;
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if ((child.getVisibility() == View.GONE) || (child == mPageHeaderView || child == mPageFooterView))
                    continue;
                if (view == child) {
                    return virtualIndex;
                }
                virtualIndex++;
            }
        }
        return -1;
    }

    protected boolean floatViewScrollNeeded(View view, boolean horizontal) {
        boolean scrollOk = view != null;
        if (scrollOk) {
            int scrollRange = 0, viewSize;
            if (horizontal) {
                if (mContentWidth > 0) {
                    scrollRange = Math.max(0, mContentWidth - (getMeasuredWidth() - getPaddingLeft() - getPaddingRight()));
                }
                viewSize = view.getMeasuredWidth() + ((PageScrollView.LayoutParams) view.getLayoutParams()).getMarginHorizontal();
            } else {
                if (mContentHeight > 0) {
                    scrollRange = Math.max(0, mContentHeight - (getMeasuredHeight() - getPaddingTop() - getPaddingBottom()));
                }
                viewSize = view.getMeasuredHeight() + ((PageScrollView.LayoutParams) view.getLayoutParams()).getMarginVertical();
            }
            scrollOk = scrollRange >= viewSize;
        }
        return scrollOk;
    }

    protected int translateMeasure(int spec, int padding, boolean limitedSize) {
        int specMode = MeasureSpec.getMode(spec);
        int specSize = MeasureSpec.getSize(spec);
        int size = limitedSize ? Math.max(0, specSize - padding) : Integer.MAX_VALUE;
        return MeasureSpec.makeMeasureSpec(size, specMode);
    }

    protected void measureFloatViewStart(int itemIndex, int virtualCount) {
        mFloatViewStartIndex = -1;
        boolean measureNeeded = itemIndex >= 0 && itemIndex < virtualCount && virtualCount >= 2 && (mPageHeaderView == null && mPageFooterView == null);
        if (measureNeeded) {
            View view = getVirtualChildAt(itemIndex, true);
            measureNeeded = (getVirtualChildAt(itemIndex, false) == view);
            if (measureNeeded && floatViewScrollNeeded(view, mOrientation == HORIZONTAL)) {
                mFloatViewStartIndex = indexOfChild(view);
                mFloatViewStartMode = FLOAT_VIEW_SCROLL;
            }
        }
    }

    protected void measureFloatViewEnd(int itemIndex, int virtualCount) {
        mFloatViewEndIndex = -1;
        boolean measureNeeded = itemIndex >= 0 && itemIndex < virtualCount && virtualCount >= 2 && (mPageHeaderView == null && mPageFooterView == null);
        if (measureNeeded) {
            View view = getVirtualChildAt(itemIndex, true);
            measureNeeded = (getVirtualChildAt(itemIndex, false) == view);
            if (measureNeeded && floatViewScrollNeeded(view, mOrientation == HORIZONTAL)) {
                mFloatViewEndIndex = indexOfChild(view);
                mFloatViewEndMode = FLOAT_VIEW_SCROLL;
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean horizontal = mOrientation == HORIZONTAL;
        dispatchMeasure(widthMeasureSpec, heightMeasureSpec, horizontal);
        if (isChildFillParent) {
            int selfWidth = getMeasuredWidth(), selfHeight = getMeasuredHeight();
            int contentWidthWithPadding = mContentWidth + getPaddingLeft() + getPaddingRight();
            int contentHeightWithPadding = mContentHeight + getPaddingTop() + getPaddingBottom();
            int adjustTotal = horizontal ? (selfWidth - contentWidthWithPadding) : (selfHeight - contentHeightWithPadding);
            if (adjustTotal > 0 && adjustTotal > mVirtualCount) {
                int adjustSize = adjustTotal / mVirtualCount, childCount = getChildCount();
                adjustTotal = adjustSize * mVirtualCount;
                for (int i = 0; i < childCount; i++) {
                    final View child = getChildAt(i);
                    if (child.getVisibility() == View.GONE || (child == mPageHeaderView || child == mPageFooterView))
                        continue;
                    int fillWidth = child.getMeasuredWidth(), fillHeight = child.getMeasuredHeight();
                    if (horizontal) {
                        fillWidth += adjustSize;
                    } else {
                        fillHeight += adjustSize;
                    }
                    child.measure(MeasureSpec.makeMeasureSpec(fillWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(fillHeight, MeasureSpec.EXACTLY));
                }
                if (horizontal) {
                    mContentWidth += adjustTotal;
                } else {
                    mContentHeight += adjustTotal;
                }
            }
        }
        measureFloatViewStart(mFloatViewStart, mVirtualCount);
        measureFloatViewEnd(mFloatViewEnd, mVirtualCount);
    }

    protected void dispatchMeasure(int widthMeasureSpec, int heightMeasureSpec, boolean horizontal) {
        int childState = 0, headerExtraWidth = 0, headerExtraHeight = 0;
        final int paddingHorizontal = getPaddingLeft() + getPaddingRight();
        final int paddingVertical = getPaddingTop() + getPaddingBottom();
        mContentWidth = 0;
        mContentHeight = 0;
        mVirtualCount = getVirtualChildCount(true);
        if (mPageHeaderView != null || mPageFooterView != null) {
            final int measureSpecWidth = translateMeasure(widthMeasureSpec, paddingHorizontal, true);
            final int measureSpecHeight = translateMeasure(heightMeasureSpec, paddingVertical, true);
            childState = measureExtraView(mPageHeaderView, measureSpecWidth, measureSpecHeight, horizontal) | childState;
            childState = measureExtraView(mPageFooterView, measureSpecWidth, measureSpecHeight, horizontal) | childState;
            if (mContentWidth > 0 || mContentHeight > 0) {
                if (horizontal) {
                    headerExtraHeight = mContentHeight;
                } else {
                    headerExtraWidth = mContentWidth;
                }
            }
        }
        if (mVirtualCount > 0) {
            final int measureSpecWidth = translateMeasure(widthMeasureSpec, paddingHorizontal + headerExtraWidth, !horizontal);
            final int measureSpecHeight = translateMeasure(heightMeasureSpec, paddingVertical + headerExtraHeight, horizontal);
            int fixedSize = 0, scrollParentRealSize;
            if (horizontal) {
                scrollParentRealSize = Math.max(0, MeasureSpec.getSize(widthMeasureSpec) - paddingHorizontal);
                if (mSizeFixedPercent > 0 && mSizeFixedPercent <= 1) {
                    fixedSize = (int) (scrollParentRealSize * mSizeFixedPercent);
                }
                childState = measureMiddleViewHorizontal(measureSpecWidth, measureSpecHeight, fixedSize, scrollParentRealSize) | childState;
            } else {
                scrollParentRealSize = Math.max(0, MeasureSpec.getSize(heightMeasureSpec) - paddingVertical);
                if (mSizeFixedPercent > 0 && mSizeFixedPercent <= 1) {
                    fixedSize = (int) (scrollParentRealSize * mSizeFixedPercent);
                }
                childState = measureMiddleViewVertical(measureSpecWidth, measureSpecHeight, fixedSize, scrollParentRealSize) | childState;
            }
        }
        int maxWidth = Math.max(mContentWidth + paddingHorizontal, getSuggestedMinimumWidth());
        int maxHeight = Math.max(mContentHeight + paddingVertical, getSuggestedMinimumWidth());
        if (mMaxWidth > 0 && maxWidth > mMaxWidth) {
            maxWidth = mMaxWidth;
        }
        if (mMaxHeight > 0 && maxHeight > mMaxHeight) {
            maxHeight = mMaxHeight;
        }
        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    protected int measureExtraView(View view, int widthMeasureSpec, int heightMeasureSpec, boolean horizontal) {
        int childState = 0;
        if (isChildNotGone(view)) {
            PageScrollView.LayoutParams params = (PageScrollView.LayoutParams) view.getLayoutParams();
            int childMarginHorizontal = params.getMarginHorizontal();
            int childMarginVertical = params.getMarginVertical();
            int extraMeasureWidthSpec = getChildMeasureSpec(widthMeasureSpec, childMarginHorizontal, params.width);
            int extraMeasureHeightSpec = getChildMeasureSpec(heightMeasureSpec, childMarginVertical, params.height);
            if (horizontal) {
                if (params.width == -1) {
                    extraMeasureWidthSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(extraMeasureWidthSpec), MeasureSpec.EXACTLY);
                }
                view.measure(extraMeasureWidthSpec, extraMeasureHeightSpec);
                int contentHeight = view.getMeasuredHeight() + childMarginVertical;
                mContentWidth = Math.max(mContentWidth, view.getMeasuredWidth() + childMarginHorizontal);
                if (contentHeight > 0) {
                    mContentHeight += contentHeight;
                }
            } else {
                if (params.height == -1) {
                    extraMeasureHeightSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(extraMeasureHeightSpec), MeasureSpec.EXACTLY);
                }
                view.measure(extraMeasureWidthSpec, extraMeasureHeightSpec);
                int contentWidth = view.getMeasuredWidth() + childMarginHorizontal;
                if (contentWidth > 0) {
                    mContentWidth += contentWidth;
                }
                mContentHeight = Math.max(mContentHeight, view.getMeasuredHeight() + childMarginVertical);
            }
            childState = view.getMeasuredState();
        }
        return childState;
    }

    protected int measureMiddleViewVertical(int widthMeasureSpec, int heightMeasureSpec, int childFixedSize, int parentRealSize) {
        final int childCount = getChildCount();
        int childFixedHeightSpec = childFixedSize <= 0 ? 0 : MeasureSpec.makeMeasureSpec(childFixedSize, MeasureSpec.EXACTLY);
        int contentWidth = 0;
        int contentHeight = 0;
        int measuredCount = 0;
        int childState = 0;
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.GONE || (child == mPageHeaderView || child == mPageFooterView))
                continue;
            PageScrollView.LayoutParams params = (PageScrollView.LayoutParams) child.getLayoutParams();
            int childMarginHorizontal = params.getMarginHorizontal();
            int childMarginVertical = params.getMarginVertical();
            int childWidthSpec = getMiddleChildMeasureSpec(widthMeasureSpec, 0, childMarginHorizontal, params.width);
            int childHeightSpec = childFixedHeightSpec == 0 ? getMiddleChildMeasureSpec(heightMeasureSpec, parentRealSize, childMarginVertical, params.height) : childFixedHeightSpec;
            child.measure(childWidthSpec, childHeightSpec);
            if (mMiddleMargin > 0 && measuredCount > 0) {
                contentHeight += mMiddleMargin;
            }
            contentHeight += (child.getMeasuredHeight() + childMarginVertical);
            int itemWidth = child.getMeasuredWidth() + childMarginHorizontal;
            if (contentWidth < itemWidth) {
                contentWidth = itemWidth;
            }
            childState |= child.getMeasuredState();
            measuredCount++;
        }
        mContentWidth += contentWidth;
        mContentHeight = Math.max(mContentHeight, contentHeight);
        return childState;
    }

    protected int measureMiddleViewHorizontal(int widthMeasureSpec, int heightMeasureSpec, int childFixedSize, final int parentRealSize) {
        final int childCount = getChildCount();
        int childFixedWidthSpec = childFixedSize <= 0 ? 0 : MeasureSpec.makeMeasureSpec(childFixedSize, MeasureSpec.EXACTLY);
        int contentWidth = 0;
        int contentHeight = 0;
        int measuredCount = 0;
        int childState = 0;
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.GONE || (child == mPageHeaderView || child == mPageFooterView))
                continue;
            PageScrollView.LayoutParams params = (PageScrollView.LayoutParams) child.getLayoutParams();
            int childMarginHorizontal = params.getMarginHorizontal();
            int childMarginVertical = params.getMarginVertical();
            int childWidthSpec = childFixedWidthSpec == 0 ? getMiddleChildMeasureSpec(widthMeasureSpec, parentRealSize, childMarginHorizontal, params.width) : childFixedWidthSpec;
            int childHeightSpec = getMiddleChildMeasureSpec(heightMeasureSpec, 0, childMarginVertical, params.height);
            child.measure(childWidthSpec, childHeightSpec);
            if (mMiddleMargin > 0 && measuredCount > 0) {
                contentWidth += mMiddleMargin;
            }
            contentWidth += (child.getMeasuredWidth() + childMarginHorizontal);
            int itemHeight = child.getMeasuredHeight() + childMarginVertical;
            if (contentHeight < itemHeight) {
                contentHeight = itemHeight;
            }
            childState |= child.getMeasuredState();
            measuredCount++;
        }
        mContentWidth = Math.max(mContentWidth, contentWidth);
        mContentHeight += contentHeight;
        return childState;
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

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingLeft();
        int right = getPaddingRight();
        int top = getPaddingTop();
        int bottom = getPaddingBottom();
        int width = r - l, height = b - t;
        int baseLeft = getContentStart(left, width - right, mContentWidth, mGravity, true);
        int baseTop = getContentStart(top, height - bottom, mContentHeight, mGravity, false);
        dispatchLayout(width, height, baseLeft, baseTop);
    }

    protected void dispatchLayout(int width, int height, int baseLeft, int baseTop) {
        if (mOrientation == HORIZONTAL) {
            int left = getPaddingLeft(), right = getPaddingRight();
            onLayoutHorizontal(Math.max(baseLeft, left), baseTop, width - left - right);
        } else {
            int top = getPaddingTop(), bottom = getPaddingBottom();
            onLayoutVertical(baseLeft, Math.max(baseTop, top), height - top - bottom);
        }
        if (mAttachLayout == false) {
            mAttachLayout = true;
            doAfterAttachLayout();
        } else {
            if (mNeedResolveFloatOffset) {
                mNeedResolveFloatOffset = false;
                boolean horizontal = mOrientation == HORIZONTAL;
                int scrolled = horizontal ? getScrollX() : getScrollY();
                if (mFloatViewStartIndex >= 0 && mSwapViewIndex < 0) {
                    mSwapViewIndex = computeSwapViewIndex(scrolled, horizontal);
                }
                if (mFloatViewStartMode == FLOAT_VIEW_SCROLL || mFloatViewEndMode == FLOAT_VIEW_SCROLL) {
                    updatePositionForFloatView(scrolled, horizontal);
                }
                if (mPageHeaderView != null || mPageFooterView != null) {
                    updatePositionForHeaderAndFooter(scrolled, horizontal);
                }
            }
        }
    }

    protected void onLayoutVertical(int baseLeft, int baseTop, int accessHeight) {
        int childLeft, childTop, childRight, childBottom;
        int middleWidth = mContentWidth;
        if (isChildNotGone(mPageHeaderView)) {
            PageScrollView.LayoutParams params = (PageScrollView.LayoutParams) mPageHeaderView.getLayoutParams();
            childTop = getPaddingTop() + Math.max(params.topMargin, (accessHeight - (mPageHeaderView.getMeasuredHeight() + params.getMarginVertical())) / 2);
            childBottom = childTop + mPageHeaderView.getMeasuredHeight();
            childLeft = baseLeft + params.leftMargin;
            childRight = childLeft + mPageHeaderView.getMeasuredWidth();
            mPageHeaderView.layout(childLeft, childTop, childRight, childBottom);
            baseLeft = childRight + params.rightMargin;
            middleWidth -= (mPageHeaderView.getMeasuredWidth() + params.getMarginHorizontal());
        }
        if (isChildNotGone(mPageFooterView)) {
            PageScrollView.LayoutParams params = (PageScrollView.LayoutParams) mPageFooterView.getLayoutParams();
            childTop = getPaddingTop() + Math.max(params.topMargin, (accessHeight - (mPageFooterView.getMeasuredHeight() + params.getMarginVertical())) / 2);
            childBottom = childTop + mPageFooterView.getMeasuredHeight();
            childRight = getWidth() - getPaddingRight() - params.rightMargin;
            childLeft = childRight - mPageFooterView.getMeasuredWidth();
            mPageFooterView.layout(childLeft, childTop, childRight, childBottom);
            middleWidth -= (mPageFooterView.getMeasuredWidth() + params.getMarginHorizontal());
        }

        final int count = getChildCount();
        final int baseRight = baseLeft + middleWidth;
        childTop = baseTop;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.GONE || (child == mPageHeaderView || child == mPageFooterView))
                continue;
            PageScrollView.LayoutParams params = (PageScrollView.LayoutParams) child.getLayoutParams();
            childTop += params.topMargin;
            childBottom = childTop + child.getMeasuredHeight();
            childLeft = getContentStart(baseLeft, baseRight, child.getMeasuredWidth() + params.getMarginHorizontal(), isChildCenter ? Gravity.CENTER : params.gravity, true);
            childRight = childLeft + child.getMeasuredWidth();
            child.layout(childLeft, childTop, childRight, childBottom);
            childTop = childBottom + params.bottomMargin;
            if (mMiddleMargin > 0) {
                childTop += mMiddleMargin;
            }
        }
    }

    protected void onLayoutHorizontal(int baseLeft, int baseTop, int accessWidth) {
        int childLeft, childTop, childRight, childBottom;
        int middleHeight = mContentHeight;
        if (isChildNotGone(mPageHeaderView)) {
            PageScrollView.LayoutParams params = (PageScrollView.LayoutParams) mPageHeaderView.getLayoutParams();
            childLeft = getPaddingLeft() + Math.max(params.leftMargin, (accessWidth - (mPageHeaderView.getMeasuredWidth() + params.getMarginHorizontal())) / 2);
            childRight = childLeft + mPageHeaderView.getMeasuredWidth();
            childTop = baseTop + params.topMargin;
            childBottom = childTop + mPageHeaderView.getMeasuredHeight();
            mPageHeaderView.layout(childLeft, childTop, childRight, childBottom);
            baseTop = childBottom + params.bottomMargin;
            middleHeight -= (mPageHeaderView.getMeasuredHeight() + params.getMarginVertical());
        }
        if (isChildNotGone(mPageFooterView)) {
            PageScrollView.LayoutParams params = (PageScrollView.LayoutParams) mPageFooterView.getLayoutParams();
            childLeft = getPaddingLeft() + Math.max(params.leftMargin, (accessWidth - (mPageFooterView.getMeasuredWidth() + params.getMarginHorizontal())) / 2);
            childRight = childLeft + mPageFooterView.getMeasuredWidth();
            childBottom = getHeight() - getPaddingBottom() - params.bottomMargin;
            childTop = childBottom - mPageFooterView.getMeasuredHeight();
            mPageFooterView.layout(childLeft, childTop, childRight, childBottom);
            middleHeight -= (mPageFooterView.getMeasuredHeight() + params.getMarginVertical());
        }

        final int count = getChildCount();
        final int baseBottom = baseTop + middleHeight;
        childLeft = baseLeft;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.GONE || (child == mPageHeaderView || child == mPageFooterView))
                continue;
            PageScrollView.LayoutParams params = (PageScrollView.LayoutParams) child.getLayoutParams();
            childLeft += params.leftMargin;
            childRight = childLeft + child.getMeasuredWidth();
            childTop = getContentStart(baseTop, baseBottom, child.getMeasuredHeight() + params.getMarginVertical(), isChildCenter ? Gravity.CENTER : params.gravity, false);
            childBottom = childTop + child.getMeasuredHeight();
            child.layout(childLeft, childTop, childRight, childBottom);
            childLeft = childRight + params.rightMargin;
            if (mMiddleMargin > 0) {
                childLeft += mMiddleMargin;
            }
        }
    }

    protected void doAfterAttachLayout() {
        boolean willScrolled = false;
        if (mScrollInfo.left >= 0 || mPrevItem == -1) {
            if (mScrollInfo.left >= 0) {
                View pageView = getItemView(mScrollInfo.left);
                if (pageView != null) {
                    willScrolled = scrollTo(pageView, mScrollInfo.top, mScrollInfo.right, 1 == mScrollInfo.bottom);
                }
                mScrollInfo.set(-1, -1, -1, -1);
            } else {
                if (mPrevItem == -1 && mVirtualCount > 0) {
                    setCurrentItem(mCurrItem);
                }
            }
        }
        if (!willScrolled) {
            boolean horizontal = mOrientation == HORIZONTAL;
            int scrolled = horizontal ? getScrollX() : getScrollY();
            resolveVisiblePosition(scrolled, horizontal);
            mNeedResolveFloatOffset = false;
            if (mFloatViewStartMode == FLOAT_VIEW_SCROLL || mFloatViewEndMode == FLOAT_VIEW_SCROLL) {
                updatePositionForFloatView(scrolled, horizontal);
            }
            if (mPageHeaderView != null || mPageFooterView != null) {
                updatePositionForHeaderAndFooter(scrolled, horizontal);
            }
            if ((mPageListener != null || mPageTransformer != null)) {
                resolvePageOffset(scrolled, horizontal);
            }
        }
    }
    @Override
    protected void dispatchDraw(Canvas canvas) {
        boolean swapIndexEnable = mFloatViewStartIndex >= 0 && mSwapViewIndex >= 0;
        if (swapIndexEnable && isChildrenDrawingOrderEnabled() == false) {
            setChildrenDrawingOrderEnabled(true);
        } else {
            if (swapIndexEnable == false) {
                setChildrenDrawingOrderEnabled(false);
            }
        }
        super.dispatchDraw(canvas);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        int order = i;
        if (mFloatViewStartIndex >= 0 && mSwapViewIndex >= 0) {
            if (mFloatViewStartIndex == i) {
                return mSwapViewIndex;
            }
            if (i == mSwapViewIndex) {
                return mFloatViewStartIndex;
            }
        }
        return order;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mVirtualCount == 0 || !isEnabled()) {
            return super.onTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
            return false;
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        final int action = event.getAction() & MotionEventCompat.ACTION_MASK;
        if (action == MotionEvent.ACTION_MOVE) {
            handleTouchActionMove(event);
        } else {
            if (action == MotionEvent.ACTION_DOWN) {
                handleTouchActionDown(event);
            }
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                handleTouchActionUp(event);
            }
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mVirtualCount== 0 || !isEnabled()) {
            mIsBeingDragged = false;
        } else {
            final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
            if (action == MotionEvent.ACTION_MOVE) {
                handleTouchActionMove(ev);
            } else {
                if (action == MotionEvent.ACTION_DOWN) {
                    handleTouchActionDown(ev);
                }
                if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                    handleTouchActionUp(ev);
                }
            }
        }
        return mIsBeingDragged;
    }

    private void handleTouchActionMove(MotionEvent ev) {
        float x = ev.getX(), y = ev.getY();
        if (mIsBeingDragged) {
            scrollDxDy((int) (mPointLast.x - x), (int) (mPointLast.y - y));
            mPointLast.set(x, y);
        } else {
            int dx = (int) (mPointDown.x - x), dy = (int) (mPointDown.y - y);
            int dxAbs = Math.abs(dx), dyAbs = Math.abs(dy);
            boolean dragged;
            if (mOrientation == HORIZONTAL) {
                dragged = dxAbs > mTouchSlop && (dxAbs * 0.6f) > dyAbs;
                dx = (dx > 0 ? mTouchSlop : -mTouchSlop) >> 2;
                dy = 0;
            } else {
                dragged = dyAbs > mTouchSlop && (dyAbs * 0.6f) > dxAbs;
                dy = (dy > 0 ? mTouchSlop : -mTouchSlop) >> 2;
                dx = 0;
            }
            if (dragged) {
                markAsWillDragged();
                scrollDxDy(dx, dy);
                mPointLast.set(x, y);
            }
        }
    }

    private void handleTouchActionUp(MotionEvent ev) {
        if (mIsBeingDragged) {
            mIsBeingDragged = false;
            mPointLast.x = ev.getX();
            mPointLast.y = ev.getY();
            int velocityX = 0, velocityY = 0;
            final VelocityTracker velocityTracker = mVelocityTracker;
            if (velocityTracker != null) {
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                velocityX = (int) velocityTracker.getXVelocity();
                velocityY = (int) velocityTracker.getYVelocity();
            }
            if (!flingToWhere((int) (mPointLast.x - mPointDown.x), (int) (mPointLast.y - mPointDown.y), -velocityX, -velocityY)) {
                markAsWillIdle();
            }
        }
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void handleTouchActionDown(MotionEvent ev) {
        mPointDown.set(ev.getX(), ev.getY());
        mPointLast.set(mPointDown);
        if (mScrollerUsed) {
            OverScroller scroller = getScroller();
            scroller.computeScrollOffset();
            if (!scroller.isFinished()) {
                scroller.abortAnimation();
                markAsWillDragged();
            }
        }
    }

    private void markAsWillDragged() {
        mIsBeingDragged = true;
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
        setScrollState(SCROLL_STATE_DRAGGING);
    }

    private void markAsWillScroll() {
        mScrollerUsed = true;
        setScrollState(SCROLL_STATE_SETTLING);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    private void markAsWillIdle() {
        mScrollerUsed = false;
        setScrollState(SCROLL_STATE_IDLE);
    }

    public boolean isAttachLayoutFinished() {
        return mAttachLayout;
    }

    private boolean isFlingAllowed(int scrolled, int scrollRange, int velocity) {
        return !(velocity == 0 || (velocity < 0 && scrolled <= 0) || (velocity > 0 && scrolled >= scrollRange));
    }

    private boolean flingToWhere(int movedX, int movedY, int velocityX, int velocityY) {
        int scrolled, scrollRange, velocity, moved;
        boolean horizontal = mOrientation == HORIZONTAL, willScroll;
        if (horizontal) {
            scrolled = getScrollX();
            scrollRange = getScrollRangeHorizontal();
            velocity = velocityX;
            moved = movedX;
        } else {
            scrolled = getScrollY();
            scrollRange = getScrollRangeVertical();
            velocity = velocityY;
            moved = movedY;
        }
        if (velocity == 0 && isViewPagerStyle) {
            velocity = -(int)Math.signum((horizontal ? movedX : movedY));
        }
        if (willScroll = isFlingAllowed(scrolled, scrollRange, velocity)) {
            if (isViewPagerStyle) {
                int targetIndex = mCurrItem;
                int itemSize = horizontal ? getChildAt(mCurrItem).getWidth() : getChildAt(mCurrItem).getHeight();
                int containerSize = horizontal ? (getWidth() - getPaddingLeft() - getPaddingRight()) : (getHeight() - getPaddingTop() - getPaddingBottom());
                int absVelocity = velocity > 0 ? velocity : -velocity;
                int pageItemCount = getItemCount();
                if (Math.abs(moved) > mMinDistance) {
                    int halfItemSize = itemSize / 2;
                    if (absVelocity > mMinimumVelocity) {
                        if (velocity > 0 && mCurrItem < pageItemCount - 1 && (velocity / 10 - moved) > halfItemSize) {
                            targetIndex++;
                        }
                        if (velocity < 0 && mCurrItem > 0 && (moved - velocity / 10) > halfItemSize) {
                            targetIndex--;
                        }
                    } else {
                        if (moved > halfItemSize && mCurrItem > 0) {
                            targetIndex--;
                        }
                        if (moved < -halfItemSize && mCurrItem < pageItemCount - 1) {
                            targetIndex++;
                        }
                    }
                }
                int targetScroll = computeScrollOffset(targetIndex, 0, true, horizontal);
                if (willScroll = (targetScroll != scrolled)) {
                    setCurrentItem(targetIndex);
                    int dScroll = targetScroll - scrolled;
                    int duration = computeScrollDurationForItem(dScroll, absVelocity, itemSize, containerSize);
                    if (horizontal) {
                        getScroller().startScroll(scrolled, getScrollY(), dScroll, 0, duration);
                    } else {
                        getScroller().startScroll(getScrollX(), scrolled, 0, dScroll, duration);
                    }
                    markAsWillScroll();
                }
            } else {
                if (horizontal) {
                    getScroller().fling(scrolled, getScrollY(), velocity, 0, 0, scrollRange, 0, 0, mOverFlingDistance, 0);
                } else {
                    getScroller().fling(getScrollX(), scrolled, 0, velocity, 0, 0, 0, scrollRange, 0, mOverFlingDistance);
                }
                markAsWillScroll();
            }
        }
        return willScroll;
    }

    private void scrollDxDy(int scrollDx, int scrollDy) {
        if (mOrientation == HORIZONTAL) {
            int scrollWant = getScrollX() + scrollDx;
            int scrollRange = getScrollRangeHorizontal();
            if (scrollWant < 0) scrollWant = 0;
            if (scrollWant > scrollRange) scrollWant = scrollRange;
            scrollTo(scrollWant, getScrollY());
        } else {
            int scrollWant = getScrollY() + scrollDy;
            int scrollRange = getScrollRangeVertical();
            if (scrollWant < 0) scrollWant = 0;
            if (scrollWant > scrollRange) scrollWant = scrollRange;
            scrollTo(getScrollX(), scrollWant);
        }
    }

    public boolean scrollTo(int index, int offset, int duration) {
        if (mAttachLayout) {
            return scrollTo(getItemView(index), offset, duration, false);
        } else {
            if (index >= 0) {
                mScrollInfo.set(index, offset, duration, 0);
            }
        }
        return false;
    }

    public boolean scrollToCentre(int index, int offset, int duration) {
        if (mAttachLayout) {
            return scrollTo(getItemView(index), offset, duration, true);
        } else {
            if (index >= 0) {
                mScrollInfo.set(index, offset, duration, 1);
            }
        }
        return false;
    }

    public boolean scrollTo(View child, int offset, int duration, boolean childCenter) {
        int pageIndex = indexOfItemView(child);
        if (pageIndex == -1) return false;
        if (mAttachLayout) {
            if (mScrollInfo.left >= 0) {
                mScrollInfo.set(-1, -1, -1, -1);
            }
            boolean horizontal = mOrientation == HORIZONTAL;
            int paddingStart, containerSize, childStart, childSize;
            int scrolled, scrollRange, targetScroll;
            if (horizontal) {
                paddingStart = getPaddingLeft();
                containerSize = getWidth() - paddingStart - getPaddingRight();
                childStart = child.getLeft();
                childSize = child.getWidth();
                scrolled = getScrollX();
                scrollRange = getScrollRangeHorizontal();
            } else {
                paddingStart = getPaddingTop();
                containerSize = getHeight() - paddingStart - getPaddingBottom();
                childStart = child.getTop();
                childSize = child.getHeight();
                scrolled = getScrollY();
                scrollRange = getScrollRangeVertical();
            }
            targetScroll = childStart - paddingStart + offset + (childCenter ? (childSize - containerSize) / 2 : 0);
            targetScroll = Math.max(0, Math.min(scrollRange, targetScroll));
            if (targetScroll != scrolled) {
                setCurrentItem(pageIndex);
                int dScroll = targetScroll - scrolled;
                if (duration < 0) {
                    duration = computeScrollDuration(Math.abs(dScroll), 0, containerSize, MAX_DURATION);
                }
                if (duration == 0) {
                    if (horizontal) {
                        scrollTo(targetScroll, getScrollY());
                    } else {
                        scrollTo(getScrollX(), targetScroll);
                    }
                } else {
                    if (horizontal) {
                        getScroller().startScroll(scrolled, getScrollY(), dScroll, 0, duration);
                    } else {
                        getScroller().startScroll(getScrollX(), scrolled, 0, dScroll, duration);
                    }
                    markAsWillScroll();
                }
                return true;
            }
        } else {
            mScrollInfo.set(pageIndex, offset, duration, childCenter ? 1 : 0);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p>This version also clamps the scrolling to the bounds of our child.
     */
    @Override
    public void scrollTo(int x, int y) {
        // we rely on the fact the View.scrollBy calls scrollTo.
        if (mVirtualCount > 0) {
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            x = clamp(x, getWidth() - (getPaddingLeft() + getPaddingRight()), mContentWidth);
            y = clamp(y, getHeight() - (getPaddingTop() + getPaddingBottom()), mContentHeight);
            if (x != scrollX || y != scrollY) {
                super.scrollTo(x, y);
            }
        }
    }

    protected int getScrollRangeVertical() {
        int scrollRange = 0;
        if (mContentHeight > 0) {
            scrollRange = Math.max(0, mContentHeight - (getHeight() - getPaddingTop() - getPaddingBottom()));
        }
        return scrollRange;
    }

    protected int getScrollRangeHorizontal() {
        int scrollRange = 0;
        if (mContentWidth > 0) {
            scrollRange = Math.max(0, mContentWidth - (getWidth() - getPaddingLeft() - getPaddingRight()));
        }
        return scrollRange;
    }

    @Override
    protected int computeVerticalScrollRange() {
        final int count = getChildCount();
        final int paddingTop = getPaddingTop();
        final int contentHeight = getHeight() - paddingTop - getPaddingBottom();
        if (count == 0) {
            return contentHeight;
        }
        int scrollRange = paddingTop + mContentHeight;
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
    protected int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    protected int computeHorizontalScrollRange() {
        final int count = getChildCount();
        final int paddingLeft = getPaddingLeft();
        final int contentWidth = getWidth() - paddingLeft - getPaddingRight();
        if (count == 0) {
            return contentWidth;
        }
        int scrollRange = paddingLeft + mContentWidth;
        final int scrollX = getScrollX();
        final int overScrollRight = Math.max(0, scrollRange - contentWidth);
        if (scrollX < 0) {
            scrollRange -= scrollX;
        } else if (scrollX > overScrollRight) {
            scrollRange += scrollX - overScrollRight;
        }
        return scrollRange;
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return Math.max(0, super.computeHorizontalScrollOffset());
    }

    protected int computeScrollOffset(View child, int offset, boolean centreWithParent, boolean horizontal) {
        int paddingStart, childStart;
        int scrollRange, targetScroll = offset;
        if (horizontal) {
            paddingStart = getPaddingLeft();
            childStart = child.getLeft();
            targetScroll += (childStart - paddingStart);
            if (centreWithParent) {
                targetScroll += (child.getWidth() - (getWidth() - paddingStart - getPaddingRight())) / 2;
            }
            scrollRange = getScrollRangeHorizontal();
        } else {
            paddingStart = getPaddingTop();
            childStart = child.getTop();
            targetScroll += (childStart - paddingStart);
            if (centreWithParent) {
                targetScroll += (child.getHeight() - (getHeight() - paddingStart - getPaddingBottom())) / 2;
            }
            scrollRange = getScrollRangeVertical();
        }
        return Math.max(0, Math.min(scrollRange, targetScroll));
    }

    protected int computeScrollOffset(int childPosition, int offset, boolean centreWithParent, boolean horizontal) {
        View child = getVirtualChildAt(childPosition, true);
        return child == null ? 0 : computeScrollOffset(child, offset, centreWithParent, horizontal);
    }

    protected int computeScrollDurationForItem(int willMoved, int absVelocity, int itemSized, int containerSize) {
        if (itemSized <= 0) {
            return computeScrollDuration(Math.abs(willMoved), absVelocity, containerSize, MAX_DURATION);
        }
        int duration;
        if (absVelocity > 0) {
            int halfWidth = containerSize / 2;
            float distanceRatio = distanceInfluenceForSnapDuration(Math.min(1f, Math.abs(willMoved) / (float) itemSized));
            float distance = halfWidth + halfWidth * distanceRatio;
            duration = 5 * Math.round(1000 * Math.abs(distance / absVelocity));
        } else {
            final float pageDelta = (float) Math.abs(willMoved) / itemSized;
            duration = (int) ((pageDelta + 1) * MAX_DURATION / 2);
        }
        return Math.min(duration, MAX_DURATION);
    }

    private int computeScrollDuration(int absWillMoved, int absVelocity, int containerSize, int maxDuration) {
        final int halfContainerSize = containerSize / 2;
        final float distanceRatio = Math.min(1.f, 1.f * absWillMoved / containerSize);
        final float distance = halfContainerSize + halfContainerSize *
                distanceInfluenceForSnapDuration(distanceRatio);
        final int duration;
        if (absVelocity > 0) {
            duration = 4 * Math.round(1000 * Math.abs(distance / absVelocity));
        } else {
            duration = (int) (((absWillMoved / (float) containerSize) + 1) * maxDuration / 2);
        }
        return Math.min(duration, maxDuration);
    }

    @Override
    public void computeScroll() {
        OverScroller scroller = mScrollerUsed ? getScroller() : null;
        if (scroller != null && scroller.computeScrollOffset()) {
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = scroller.getCurrX();
            int y = scroller.getCurrY();
            if (oldX != x || oldY != y) {
                scrollTo(x, y);
            }
            ViewCompat.postInvalidateOnAnimation(this);
        } else {
            if (mScrollerUsed) {
                markAsWillIdle();
            }
        }
    }

    private float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

    private void enableLayers(boolean enable) {
        final int childCount = getChildCount();
        final int layerType = enable ? ViewCompat.LAYER_TYPE_HARDWARE : ViewCompat.LAYER_TYPE_NONE;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child != mPageHeaderView && child != mPageFooterView) {
                ViewCompat.setLayerType(child, layerType, null);
            }
        }
    }

    private boolean setCurrentItem(int willItem) {
        if (mCurrItem != willItem || mPrevItem == -1) {
            int preItem = mCurrItem == willItem ? mPrevItem : mCurrItem;
            mPrevItem = mCurrItem;
            mCurrItem = willItem;
            if(mLogEnable){
                print(String.format("selectChanged  $$$$:%d >>>>>>>>> %d", preItem, mCurrItem));
            }
            if (mPageListener != null) {
                mPageListener.onPageSelected(willItem, preItem);
            }
            return true;
        }
        return false;
    }

    private boolean setScrollState(int newState) {
        if (mScrollState != newState) {
            int preState = mScrollState;
            mScrollState = newState;
            if(mLogEnable){
                print(String.format("stateChanged  ####:%d >>>>>>>>> %d", preState, mScrollState));
            }
            if (mScrollListener != null) {
                mScrollListener.onScrollStateChanged(mScrollState, preState);
            }
            if (mPageListener != null) {
                mPageListener.onScrollStateChanged(mScrollState, preState);
            }
            if (mPageTransformer != null) {
                // PageTransformers can do complex things that benefit from hardware layers.
                enableLayers(newState != SCROLL_STATE_IDLE);
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onScrollChanged(int l, int t, int ol, int ot) {
        super.onScrollChanged(l, t, ol, ot);
        if (mScrollState != SCROLL_STATE_IDLE) {
            awakenScrollBars();
        }

        if (mScrollListener != null) {
            mScrollListener.onScrollChanged(l, t, ol, ot);
        }
        mNeedResolveFloatOffset = false;
        boolean horizontal = mOrientation == HORIZONTAL;
        int scrolled = horizontal ? l : t;
        resolveVisiblePosition(scrolled, horizontal);

        if (mFloatViewStartIndex >= 0) {
            mSwapViewIndex = computeSwapViewIndex(scrolled, horizontal);
        }
        if (mPageHeaderView != null || mPageFooterView != null) {
            updatePositionForHeaderAndFooter(scrolled, horizontal);
        }
        if (mFloatViewStartMode == FLOAT_VIEW_SCROLL || mFloatViewEndMode == FLOAT_VIEW_SCROLL) {
            updatePositionForFloatView(scrolled, horizontal);
        }
        if (mPageListener != null || mPageTransformer != null) {
            resolvePageOffset(scrolled, horizontal);
        }
    }

    protected int computeSwapViewIndex(int scrolled, boolean horizontal) {
        if (mFloatViewStartIndex >= 0) {
            int count = getChildCount(), baseLine;
            View view = getChildAt(mFloatViewStartIndex);
            baseLine = (horizontal ? view.getRight() : view.getBottom()) + scrolled;
            for (int i = mFloatViewStartIndex + 1; i < count; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() == View.GONE || (child == mPageHeaderView || child == mPageFooterView))
                    continue;
                if (horizontal) {
                    if (child.getRight() >= baseLine) {
                        return i;
                    }
                } else {
                    if (child.getBottom() >= baseLine) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    protected void resetPositionForFloatView(int realChildIndex, boolean horizontal) {
        View child = realChildIndex >= 0 ? getChildAt(realChildIndex) : null;
        if (child != null) {
            child.setTranslationX(0);
            child.setTranslationY(0);
        }
    }

    private void updatePositionForHeaderAndFooter(int scrolled, boolean horizontal) {
        if (mPageHeaderView != null && mPageHeaderView.getParent() == this) {
            if (horizontal) {
                mPageHeaderView.setTranslationX(scrolled);
            } else {
                mPageHeaderView.setTranslationY(scrolled);
            }
        }
        if (mPageFooterView != null && mPageFooterView.getParent() == this) {
            if (horizontal) {
                mPageFooterView.setTranslationX(scrolled);
            } else {
                mPageFooterView.setTranslationY(scrolled);
            }
        }
    }

    private void updatePositionForFloatView(int scrolled, boolean horizontal) {
        float viewTranslated;
        int wantTranslated;
        if (mFloatViewStartMode == FLOAT_VIEW_SCROLL) {
            View view = getItemView(mFloatViewStartIndex);
            PageScrollView.LayoutParams params = (LayoutParams) view.getLayoutParams();
            if (horizontal) {
                wantTranslated = scrolled - (view.getLeft() + params.leftMargin);
                viewTranslated = view.getTranslationX();
            } else {
                wantTranslated = scrolled - (view.getTop() + params.topMargin);
                viewTranslated = view.getTranslationY();
            }
            wantTranslated = Math.max(0, wantTranslated);
            if (wantTranslated != viewTranslated) {
                if (horizontal) {
                    view.setTranslationX(wantTranslated);
                } else {
                    view.setTranslationY(wantTranslated);
                }
            }
        }
        if (mFloatViewEndMode == FLOAT_VIEW_SCROLL) {
            View view = getItemView(mFloatViewEndIndex);
            PageScrollView.LayoutParams params = (LayoutParams) view.getLayoutParams();
            int scrollRange;
            if (horizontal) {
                scrollRange = getScrollRangeHorizontal();
                wantTranslated = scrolled - scrollRange + (mContentWidth - (view.getRight() + params.rightMargin));
                viewTranslated = view.getTranslationX();
            } else {
                scrollRange = getScrollRangeVertical();
                wantTranslated = scrolled - scrollRange + (mContentHeight - (view.getBottom() + params.bottomMargin));
                viewTranslated = view.getTranslationY();
            }
            wantTranslated = Math.min(0, wantTranslated);
            if (wantTranslated != viewTranslated) {
                if (horizontal) {
                    view.setTranslationX(wantTranslated);
                } else {
                    view.setTranslationY(wantTranslated);
                }
            }
        }
    }

    private void resolveVisiblePosition(int scrolled, boolean horizontal) {
        int visibleStart, visibleEnd;
        if (horizontal) {
            visibleStart = getPaddingLeft() + scrolled;
            visibleEnd = getWidth() - getPaddingRight() + scrolled;
        } else {
            visibleStart = getPaddingTop() + scrolled;
            visibleEnd = getHeight() - getPaddingBottom() + scrolled;
        }
        int childCount = getChildCount(), counted = 0;
        int firstVisible = -1, lastVisible = -1;
        boolean visible;
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == View.GONE || (child == mPageHeaderView || child == mPageFooterView))
                continue;
            if (horizontal) {
                visible = !((child.getRight() <= visibleStart) || child.getLeft() >= visibleEnd);
            } else {
                visible = !((child.getBottom() <= visibleStart) || child.getTop() >= visibleEnd);
            }
            if (visible) {
                if (firstVisible == -1) {
                    firstVisible = counted;
                }
                lastVisible = counted;
            } else {
                if (firstVisible >= 0) {
                    break;
                }
            }
            counted++;
        }
        if (firstVisible != -1) {
            if (firstVisible != mFirstVisiblePosition || lastVisible != mLastVisiblePosition) {
                int oldFirstVisible = mFirstVisiblePosition;
                int oldLastVisible = mLastVisiblePosition;
                mFirstVisiblePosition = firstVisible;
                mLastVisiblePosition = lastVisible;
                if(mLogEnable){
                    print(String.format("visibleRangeChanged  ****:[%d , %d]", firstVisible, lastVisible));
                }
                if (mOnVisibleRangeChangeListener != null) {
                    mOnVisibleRangeChangeListener.onVisibleRangeChanged(firstVisible, lastVisible, oldFirstVisible, oldLastVisible);
                }
            }
        }
    }

    private void resolvePageOffset(int scrolled, boolean horizontal) {
        int targetOffset = computeScrollOffset(mCurrItem, 0, true, horizontal);
        int prevIndex = mCurrItem;
        if (scrolled > targetOffset && prevIndex < mVirtualCount - 1) {
            prevIndex++;
        }
        if (scrolled < targetOffset && prevIndex > 0) {
            prevIndex--;
        }
        int minIndex, maxIndex, minOffset, maxOffset;
        if (prevIndex > mCurrItem) {
            minIndex = mCurrItem;
            minOffset = targetOffset;
            maxIndex = prevIndex;
            maxOffset = maxIndex == minIndex ? minOffset : computeScrollOffset(maxIndex, 0, true, horizontal);
        } else {
            maxIndex = mCurrItem;
            maxOffset = targetOffset;
            minIndex = prevIndex;
            minOffset = minIndex == maxIndex ? maxOffset : computeScrollOffset(minIndex, 0, true, horizontal);
        }
        int distance = maxOffset - minOffset;
        int positionOffsetPixels = 0;
        float positionOffset = 0;
        if (distance > 0) {
            positionOffsetPixels = scrolled - minOffset;
            positionOffset = positionOffsetPixels / (float) distance;
        }
        if (mPageListener != null) {
            mPageListener.onPageScrolled(minIndex, positionOffset, positionOffsetPixels);
        }
        if (mPageTransformer != null) {
            dispatchTransformPosition(scrolled, horizontal);
        }
    }

    private void dispatchTransformPosition(int scrolled, boolean horizontal) {
        int pageItemIndex = 0, childCount = getChildCount();
        int pageItemStart = Math.max(0, mFirstVisiblePosition - 1);
        int pageItemEnd = Math.min(mVirtualCount - 1, mLastVisiblePosition + 1);
        for (int i = 0; i < childCount && pageItemIndex <= pageItemEnd; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE || child == mPageHeaderView || child == mPageFooterView)
                continue;
            if (pageItemIndex >= pageItemStart) {
                PageScrollView.LayoutParams params = (LayoutParams) child.getLayoutParams();
                int contentLength = horizontal ? (child.getWidth() + params.getMarginHorizontal()) : (child.getHeight() + params.getMarginVertical());
                if (mMiddleMargin > 0) {
                    if (pageItemIndex == 0 || pageItemIndex == mVirtualCount - 1) {
                        contentLength += (mMiddleMargin / 2);
                    } else {
                        contentLength += mMiddleMargin;
                    }
                }
                float transformerPosition = (scrolled - computeScrollOffset(child, 0, true, horizontal)) / (float) contentLength;
                mPageTransformer.transformPage(child, transformerPosition, horizontal);
            }
            pageItemIndex++;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }
    @Override
    public void removeAllViewsInLayout() {
        super.removeAllViewsInLayout();
        mFirstVisiblePosition = -1;
        mLastVisiblePosition = -1;
        mCurrItem = 0;
        mPrevItem = -1;
        mVirtualCount=0;
        mAttachLayout = false;
        mScrollInfo.set(-1, -1, -1, -1);
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

    private static int clamp(int n, int my, int child) {
        if (my >= child || n < 0) {
            /* my >= child is this case:
             *                    |--------------- me ---------------|
             *     |------ child ------|
             * or
             *     |--------------- me ---------------|
             *            |------ child ------|
             * or
             *     |--------------- me ---------------|
             *                                  |------ child ------|
             *
             * n < 0 is this case:
             *     |------ me ------|
             *                    |-------- child --------|
             *     |-- mScrollX --|
             */
            return 0;
        }
        if ((my + n) > child) {
            /* this case:
             *                    |------ me ------|
             *     |------ child ------|
             *     |-- mScrollX --|
             */
            return child - my;
        }
        return n;
    }

    public static int getMiddleChildMeasureSpec(int spec, int parentSize, int padding, int childDimension) {
        if (ViewGroup.LayoutParams.MATCH_PARENT == childDimension && parentSize > 0) {
            childDimension = parentSize;
        }
        return getChildMeasureSpec(spec, padding, childDimension);
    }

    @Override
    public PageScrollView.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new PageScrollView.LayoutParams(getContext(), attrs);
    }

    @Override
    protected PageScrollView.LayoutParams generateDefaultLayoutParams() {
        return new PageScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected PageScrollView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new PageScrollView.LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof PageScrollView.LayoutParams;
    }

    public static class LayoutParams extends MarginLayoutParams {
        public int gravity = -1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.PageScrollView);
            gravity = a.getInt(R.styleable.PageScrollView_android_layout_gravity, -1);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
            if (source instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) source;
                gravity = lp.gravity;
            }
            if (source instanceof PageScrollView.LayoutParams) {
                PageScrollView.LayoutParams lp = (PageScrollView.LayoutParams) source;
                gravity = lp.gravity;
            }
        }

        public int getMarginHorizontal() {
            return leftMargin + rightMargin;
        }

        public int getMarginVertical() {
            return topMargin + bottomMargin;
        }
    }

    public interface PageTransformer {
        void transformPage(View view, float position, boolean horizontal);

        void recoverTransformPage(View view, boolean horizontal);
    }

    public interface OnScrollChangeListener {

        void onScrollChanged(int scrollX, int scrollY, int oldScrollX, int oldScrollY);

        void onScrollStateChanged(int state, int oldState);

    }

    public interface OnPageChangeListener extends OnScrollChangeListener {

        void onPageScrolled(int position, float positionOffset, int positionOffsetPixels);

        void onPageSelected(int position, int oldPosition);

    }

    public interface OnVisibleRangeChangeListener {
        void onVisibleRangeChanged(int firstVisible, int lastVisible, int oldFirstVisible, int oldLastVisible);
    }
}