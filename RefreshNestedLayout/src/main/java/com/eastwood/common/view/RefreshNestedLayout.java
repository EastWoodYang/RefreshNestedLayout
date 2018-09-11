package com.eastwood.common.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ListViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListView;

public class RefreshNestedLayout extends FrameLayout implements NestedScrollingParent, NestedScrollingChild {

    private String LOG_TAG = this.getClass().getSimpleName();

    private Context mContext;
    private AttributeSet mAttributeSet;

    public static final int SMOOTH_SCROLL_DURATION_MS = 250;

    private static final int INVALID_POINTER = -1;
    private static float DRAG_RATE = .6125f;

    protected boolean mRefreshing;

    protected State mCurrentState = State.NONE;

    private int mPullMaxDistance;
    private int mRefreshingDistance;

    // If nested scrolling is enabled, the total amount that needed to be
    // consumed by this as the nested scrolling parent is used in place of the
    // overScroll determined by MOVE events in the onTouch handler
    private float mTotalUnconsumed;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private boolean mNestedScrollInProgress;

    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;

    private boolean mScrollWhenRefreshingEnabled;

    private int mTouchSlop;
    private float mInitialMotionY;
    private float mInitialDownY;

    private View mRefreshableView;
    private RefreshHeaderLayout mHeaderLayout;

    private SmoothScrollRunnable mCurrentSmoothScrollRunnable;
    private Interpolator mScrollAnimationInterpolator;

    private OnRefreshListener mOnRefreshListener;

    private OnChildScrollUpCallback mChildScrollUpCallback;

    public RefreshNestedLayout(Context context) {
        this(context, null);
    }

    public RefreshNestedLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        ViewConfiguration config = ViewConfiguration.get(context);
        mTouchSlop = config.getScaledTouchSlop();

        mAttributeSet = attrs;

        // Styleables from XML
        TypedArray a = mContext.obtainStyledAttributes(mAttributeSet, R.styleable.RefreshNestedLayout);

        mPullMaxDistance = a.getDimensionPixelSize(R.styleable.RefreshNestedLayout_maxPullDistance, getResources().getDimensionPixelOffset(R.dimen.default_pull_max_distance));
        mRefreshingDistance = a.getDimensionPixelSize(R.styleable.RefreshNestedLayout_refreshingDistance, getResources().getDimensionPixelOffset(R.dimen.default_refreshing_distance));

        mScrollWhenRefreshingEnabled = a.getBoolean(R.styleable.RefreshNestedLayout_scrollWhenRefreshingEnabled, true);

        boolean refreshHeader = a.hasValue(R.styleable.RefreshNestedLayout_refresh_header);
        if (refreshHeader) {
            mHeaderLayout = RefreshHeaderLayout.parseRefreshHeader(context, attrs, a.getString(R.styleable.RefreshNestedLayout_refresh_header));
        } else {
            throw new RuntimeException("refresh_header Attribute not specified.");
        }

        a.recycle();

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        LayoutParams flp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        flp.gravity = Gravity.TOP | Gravity.LEFT;
        setLayoutParams(flp);

        if (mRefreshableView == null) {
            ensureTarget();
            ViewCompat.setNestedScrollingEnabled(mRefreshableView, true);
        }

        mHeaderLayout.setHeight(0);  // hidden
        addView(mHeaderLayout, -1);

    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mRefreshableView == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mHeaderLayout)) {
                    mRefreshableView = child;
                    break;
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        ensureTarget();
        if (mNestedScrollInProgress) {
            return false;
        }

        if (mCurrentState == State.SCROLL_TO_BACK || mCurrentState == State.SCROLL_TO_REFRESH) {
            return true;
        }

        final int action = event.getActionMasked();
        int pointerIndex;

        if (mCurrentState == State.REFRESHING) {

        } else if (!isEnabled() || canChildScrollUp() || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        } else {

        }

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mActivePointerId = event.getPointerId(0);
                mIsBeingDragged = false;
                pointerIndex = event.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mInitialDownY = event.getY(pointerIndex);

                if (mCurrentState == State.REFRESHING) {
                    mInitialDownY -= mRefreshingDistance / DRAG_RATE;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "onInterceptTouchEvent, Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }
                pointerIndex = event.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                float y = event.getY(pointerIndex);
                if (mCurrentState == State.REFRESHING) {
                    float yDiff = y - (mInitialDownY + mRefreshingDistance / DRAG_RATE);
                    if (Math.abs(yDiff) > mTouchSlop && !mIsBeingDragged) {
                        if (yDiff > 0) {
                            mInitialMotionY = mInitialDownY + mTouchSlop;
                        } else {
                            mInitialMotionY = mInitialDownY - mTouchSlop;
                        }
                        mIsBeingDragged = true;
                    }
                } else {
                    float yDiff = y - mInitialDownY;
                    startDragging(yDiff);
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                onSecondaryPointerUp(event);
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                if (mCurrentState != State.REFRESHING && mHeaderLayout.getHeight() != 0) {
                    // TODO scrollBody directly ?
                    onReset();
                }
                break;
            }
        }
        return mIsBeingDragged;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);

            float activePointerY = ev.getY(newPointerIndex);
            mInitialDownY = activePointerY - mHeaderLayout.getHeight() / DRAG_RATE - mTouchSlop;
            mIsBeingDragged = false;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((android.os.Build.VERSION.SDK_INT < 21 && mRefreshableView instanceof AbsListView)
                || (mRefreshableView != null && !ViewCompat.isNestedScrollingEnabled(mRefreshableView))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();

        if (mNestedScrollInProgress) {
            return false;
        }

        int pointerIndex = -1;

        if (mCurrentState == State.SCROLL_TO_BACK || mCurrentState == State.SCROLL_TO_REFRESH) {
            mActivePointerId = event.getPointerId(0);
            mIsBeingDragged = false;
            pointerIndex = event.findPointerIndex(mActivePointerId);
            if (pointerIndex < 0) {
                return false;
            }
            mInitialDownY = event.getY(pointerIndex);
            if (mCurrentSmoothScrollRunnable != null) {
                mCurrentSmoothScrollRunnable.stop();
            }
            mCurrentState = State.MANUAL_SCROLLING;
            mInitialDownY += -mHeaderLayout.getHeight() / DRAG_RATE - mTouchSlop;
            mInitialMotionY = mInitialDownY;
            return true;
        }

        if (mCurrentState == State.REFRESHING) {
            mIsBeingDragged = true;
        } else if (!isEnabled() || canChildScrollUp() || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mActivePointerId = event.getPointerId(0);
                mIsBeingDragged = false;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                pointerIndex = event.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = event.getY(pointerIndex);
                final float yDiff = y - mInitialDownY;
                startDragging(yDiff);

                final float overScrollTop = (mInitialMotionY - y) * DRAG_RATE;
                if (mIsBeingDragged) {
                    if (overScrollTop <= 0) {
                        moveHeader((int) overScrollTop);
                    } else {
                        if (mCurrentSmoothScrollRunnable != null) {
                            mCurrentSmoothScrollRunnable.stop();
                        }
                        mIsBeingDragged = false;
                        setBodyScroll(0);
                        return false;
                    }
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                pointerIndex = event.getActionIndex();
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return false;
                }
                float secondPointerY = event.getY(pointerIndex);
                int secondPointerId = event.getPointerId(pointerIndex);

                mInitialDownY = secondPointerY - mHeaderLayout.getHeight() / DRAG_RATE - mTouchSlop;
                mActivePointerId = secondPointerId;
                mIsBeingDragged = false;
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                onSecondaryPointerUp(event);
                break;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                pointerIndex = event.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    onReset();
                    return false;
                }

                if (mIsBeingDragged) {
                    final float y = event.getY(pointerIndex);
                    final float overScrollTop = (mInitialMotionY - y) * DRAG_RATE;
                    mIsBeingDragged = false;
                    finishHeader(overScrollTop);
                }
                mActivePointerId = INVALID_POINTER;
                return false;
            }
        }
        return true;
    }

    private void startDragging(float y) {
        if (y > mTouchSlop && !mIsBeingDragged) {
            mInitialMotionY = mInitialDownY + mTouchSlop;
            mIsBeingDragged = true;
        }
    }

    public final boolean isRefreshing() {
        return mCurrentState == State.REFRESHING;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            onReset();
        }
    }

    private void callRefreshListener() {
        if (null != mOnRefreshListener) {
            setState(State.REFRESHING);
            mHeaderLayout.onRefreshBegin();
            if (!mRefreshing) {
                mRefreshing = true;
                mOnRefreshListener.onRefresh();
            }
        } else {
            mRefreshing = false;
            setState(State.SCROLL_TO_BACK);
        }
    }

    public View getRefreshableView() {
        return mRefreshableView;
    }

    protected int getPullToRefreshScrollDuration() {
        return SMOOTH_SCROLL_DURATION_MS;
    }

    private void smoothScrollTo(int scrollValue) {
        smoothScrollTo(scrollValue, getPullToRefreshScrollDuration());
    }

    private void smoothScrollTo(int scrollValue, long duration) {
        smoothScrollTo(scrollValue, duration, 0, null);
    }

    private void smoothScrollTo(int scrollValue, OnSmoothScrollFinishedListener listener) {
        smoothScrollTo(scrollValue, getPullToRefreshScrollDuration(), 0, listener);
    }

    private void smoothScrollTo(int scrollValue, long delayMillis, OnSmoothScrollFinishedListener listener) {
        smoothScrollTo(scrollValue, getPullToRefreshScrollDuration(), delayMillis, listener);
    }

    private void smoothScrollTo(int newScrollValue, long duration, long delayMillis, OnSmoothScrollFinishedListener listener) {
        if (null != mCurrentSmoothScrollRunnable) {
            mCurrentSmoothScrollRunnable.stop();
        }

        final int oldScrollValue = -mHeaderLayout.getHeight();

        if (oldScrollValue != newScrollValue) {
            if (null == mScrollAnimationInterpolator) {
                // Default interpolator is a Decelerate Interpolator
                mScrollAnimationInterpolator = new DecelerateInterpolator();
            }
            mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(oldScrollValue, newScrollValue, duration, listener);

            if (delayMillis > 0) {
                postDelayed(mCurrentSmoothScrollRunnable, delayMillis);
            } else {
                post(mCurrentSmoothScrollRunnable);
            }
        } else {
            mCurrentState = State.NONE;
        }
    }

    private void moveHeader(float needToScrollValue) {
        if (needToScrollValue > 0) {
            return;
        }
        if (needToScrollValue == 0) {
            setBodyScroll(0);
            return;
        }

        float actualScrolledValue;
        if (needToScrollValue < -mPullMaxDistance) {
            actualScrolledValue = -mPullMaxDistance;
        } else {
            actualScrolledValue = needToScrollValue;
        }
        mCurrentState = State.MANUAL_SCROLLING;
        setBodyScroll((int) actualScrolledValue);

        if (actualScrolledValue != 0 && !isRefreshing()) {
            mHeaderLayout.onPull(-needToScrollValue);
        }

        if (actualScrolledValue < mPullMaxDistance && actualScrolledValue > -mPullMaxDistance) {
            mHeaderLayout.alreadyToRefresh(false);
        } else {
            mHeaderLayout.alreadyToRefresh(true);
        }
    }

    private void finishHeader(float needToScrollValue) {
        if (needToScrollValue <= -mPullMaxDistance) {
            setState(State.SCROLL_TO_REFRESH);
        } else {
            setState(State.SCROLL_TO_BACK);
        }
    }

    private void setBodyScroll(int value) {
        if (mHeaderLayout.isMovable()) {
            scrollTo(0, value);
            mHeaderLayout.setMargins(0, value, 0, 0);
        }
        mHeaderLayout.setHeight(-value);
    }

    final class SmoothScrollRunnable implements Runnable {

        private final Interpolator mInterpolator;
        private final int mScrollToY;
        private final int mScrollFromY;
        private final long mDuration;
        private OnSmoothScrollFinishedListener mListener;

        private boolean mContinueRunning = true;
        private long mStartTime = -1;
        private int mCurrentY = -1;

        public SmoothScrollRunnable(int fromY, int toY, long duration, OnSmoothScrollFinishedListener listener) {
            mScrollFromY = fromY;
            mScrollToY = toY;
            mInterpolator = mScrollAnimationInterpolator;
            mDuration = duration;
            mListener = listener;
        }

        @Override
        public void run() {
            /**
             * Only set mStartTime if this is the first time we're starting,
             * else actually calculate the Y delta
             */
            if (mStartTime == -1) {
                mStartTime = System.currentTimeMillis();
            } else if (mIsBeingDragged) {
                return;
            } else {
                /**
                 * We do do all calculations in long to reduce software float
                 * calculations. We use 1000 as it gives us good accuracy and
                 * small rounding errors
                 */
                long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime)) / mDuration;
                normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

                final int deltaY = Math.round((mScrollFromY - mScrollToY) * mInterpolator.getInterpolation(normalizedTime / 1000f));
                mCurrentY = mScrollFromY - deltaY;
                setBodyScroll(mCurrentY);
            }

            // If we're not at the target Y, keep going...
            if (mContinueRunning && mScrollToY != mCurrentY) {
                ViewCompat.postOnAnimation(RefreshNestedLayout.this, this);
            } else {
                mHeaderLayout.onRefreshCancel();
                mCurrentState = State.NONE;
                if (null != mListener) {
                    mListener.onSmoothScrollFinished();
                }
            }
        }

        public void stop() {
            mContinueRunning = false;
            removeCallbacks(this);
        }
    }

    private OnSmoothScrollFinishedListener mOnSmoothScrollFinishedListener = new OnSmoothScrollFinishedListener() {

        @Override
        public void onSmoothScrollFinished() {
            callRefreshListener();
        }
    };

    public boolean canChildScrollUp() {
        if (mChildScrollUpCallback != null) {
            return mChildScrollUpCallback.canChildScrollUp(this, mRefreshableView);
        }

        if (mRefreshableView instanceof ListView) {
            return ListViewCompat.canScrollList((ListView) mRefreshableView, -1);
        }
        return mRefreshableView.canScrollVertically(-1);
    }

    /**
     * Set a callback to override {@link RefreshNestedLayout#canChildScrollUp()} method. Non-null
     * callback will return the value provided by the callback and ignore all internal logic.
     *
     * @param callback Callback that should be called when canChildScrollUp() is called.
     */
    public void setOnChildScrollUpCallback(@Nullable OnChildScrollUpCallback callback) {
        mChildScrollUpCallback = callback;
    }

    interface OnSmoothScrollFinishedListener {
        void onSmoothScrollFinished();
    }

    public interface OnRefreshListener {

        void onRefresh();
    }

    /**
     * Classes that wish to override {@link RefreshNestedLayout#canChildScrollUp()} method
     * behavior should implement this interface.
     */
    public interface OnChildScrollUpCallback {
        /**
         * Callback that will be called when {@link RefreshNestedLayout#canChildScrollUp()} method
         * is called to allow the implementer to override its behavior.
         *
         * @param parent SwipeRefreshLayout that this callback is overriding.
         * @param child  The child view of SwipeRefreshLayout.
         * @return Whether it is possible for the child view of parent layout to scroll up.
         */
        boolean canChildScrollUp(@NonNull RefreshNestedLayout parent, @Nullable View child);
    }

    public final void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    public enum State {

        NONE,

        SCROLL_TO_BACK,

        AUTO_REFRESH,

        SCROLL_TO_REFRESH,

        MANUAL_SCROLLING,

        REFRESHING;

    }

    protected void setState(State state) {
        switch (state) {
            case SCROLL_TO_BACK:
                onReset();
                break;
            case SCROLL_TO_REFRESH:
                mCurrentState = State.SCROLL_TO_REFRESH;
                smoothScrollTo(-mRefreshingDistance, mOnSmoothScrollFinishedListener);
                break;
            case AUTO_REFRESH:
                mCurrentState = State.SCROLL_TO_REFRESH;
                smoothScrollTo(-mPullMaxDistance, 350, 0, new OnSmoothScrollFinishedListener() {

                    @Override
                    public void onSmoothScrollFinished() {
                        mCurrentState = State.SCROLL_TO_REFRESH;
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                setState(State.SCROLL_TO_REFRESH);
                            }
                        }, 150);
                    }
                });
                break;
            case REFRESHING:
                mIsBeingDragged = false;
                mCurrentState = State.REFRESHING;
                break;
        }
    }

    protected void onReset() {
        mIsBeingDragged = false;
        mCurrentState = State.SCROLL_TO_BACK;
        mActivePointerId = INVALID_POINTER;
        smoothScrollTo(0);
    }

    public void onAutoRefresh() {
        setState(State.AUTO_REFRESH);
    }

    public void onRefreshComplete() {
        mRefreshing = false;

        if (mCurrentState == State.MANUAL_SCROLLING) {
            return;
        }
        if (mHeaderLayout != null) {
            mHeaderLayout.onRefreshFinish();
        }
        setState(State.SCROLL_TO_BACK);
    }

    // NestedScrollingParent

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        boolean result = isEnabled() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        if (result) {
            if (!mRefreshableView.canScrollVertically(1)) {
                return false;
            }
        }
        return result;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = mCurrentState == State.REFRESHING ? -mRefreshingDistance / DRAG_RATE : 0;
        mNestedScrollInProgress = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0) {
            if (mTotalUnconsumed > 0) {
                if (dy > mTotalUnconsumed) {
                    consumed[1] = dy - (int) mTotalUnconsumed;
                    mTotalUnconsumed = 0;
                } else {
                    mTotalUnconsumed -= dy;
                    consumed[1] = dy;
                }
            } else {
                if (dy > Math.abs(mTotalUnconsumed)) {
                    consumed[1] = (int) mTotalUnconsumed - dy;
                    mTotalUnconsumed = 0;
                } else {
                    mTotalUnconsumed += dy;
                    consumed[1] = dy;
                }
            }
            moveHeader(mTotalUnconsumed * DRAG_RATE);
        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;

        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed != 0 && mCurrentState != State.REFRESHING) {
            finishHeader(mTotalUnconsumed * DRAG_RATE);
            mTotalUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed, final int dxUnconsumed, final int dyUnconsumed) {
        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (!canChildScrollUp()) {
            mTotalUnconsumed += dy;
            moveHeader(mTotalUnconsumed * DRAG_RATE);
        }
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

}
