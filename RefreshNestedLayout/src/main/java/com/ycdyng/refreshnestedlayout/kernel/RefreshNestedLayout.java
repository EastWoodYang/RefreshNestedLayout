/*
 * Copyright 2016 EastWood Yang
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

package com.ycdyng.refreshnestedlayout.kernel;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import com.ycdyng.refreshnestedlayout.R;

public abstract class RefreshNestedLayout<T extends View> extends FrameLayout implements NestedScrollingParent,
        NestedScrollingChild {

    private String LOG_TAG = this.getClass().getSimpleName();
    private Context mContext;
    private AttributeSet mAttributeSet;

    public static final int SMOOTH_SCROLL_DURATION_MS = 250;

    private static final String STATE_STATE = "ptr_state";
    private static final String STATE_MODE = "ptr_mode";
    private static final String STATE_ABLE = "ptr_abel";
    private static final String STATE_SUPER = "ptr_super";

    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .6125f;

    private boolean mDisableScrollWhenRefreshing;

    protected State mCurrentState = State.RESET;
    protected Mode mMode = Mode.getDefault();
    protected Mode mCurrentMode;

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

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;
    private boolean mRefreshing = false;
    private boolean mIsMoving;

    private boolean mIsDisable;

    private int mTouchSlop;
    private float mInitialMotionY;
    private float mInitialDownY;

    protected T mRefreshableView;
    private RefreshHeaderLayout mHeaderLayout;
    protected RefreshLoadingLayout mLoadingLayout;
    protected RefreshEmptyLayout mEmptyLayout;

    protected boolean showEmpty;

    private SmoothScrollRunnable mCurrentSmoothScrollRunnable;
    private Interpolator mScrollAnimationInterpolator;

    private OnRefreshListener mOnRefreshListener;

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

        if (a.hasValue(R.styleable.RefreshNestedLayout_mode)) {
            mMode = Mode.mapIntToValue(a.getInteger(R.styleable.RefreshNestedLayout_mode, 0));
        } else {
            mMode = Mode.getDefault();
        }

        mPullMaxDistance = a.getDimensionPixelSize(R.styleable.RefreshNestedLayout_pullMaxDistance, getResources().getDimensionPixelOffset(R.dimen.default_pull_max_distance));
        mRefreshingDistance = a.getDimensionPixelSize(R.styleable.RefreshNestedLayout_refreshingDistance, getResources().getDimensionPixelOffset(R.dimen.default_refreshing_distance));

        mDisableScrollWhenRefreshing = a.getBoolean(R.styleable.RefreshNestedLayout_disableScrollWhenRefreshing, true);

        handleStyledAttributes(a);

        mRefreshableView = createRefreshableView(mContext, mAttributeSet);
        mEmptyLayout = createEmptyView(mContext, mAttributeSet);
        mLoadingLayout = createLoadingLayout(mContext, mAttributeSet);

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

        T refreshableView = createRefreshableView(mContext, null);
        if (refreshableView != null && refreshableView.getParent() != null) {
            mRefreshableView = null;
            mRefreshableView = refreshableView;
        } else {
            refreshableView = null;
            addView(mRefreshableView, -1);
        }

        RefreshEmptyLayout emptyLayout = createEmptyView(mContext, mAttributeSet);
        if (emptyLayout != null && emptyLayout.getParent() != null) {
            mEmptyLayout = null;
            mEmptyLayout = emptyLayout;
        } else {
            emptyLayout = null;
            addView(mEmptyLayout, -1);
        }

        RefreshLoadingLayout loadingLayout = createLoadingLayout(mContext, mAttributeSet);
        if (loadingLayout != null && loadingLayout.getParent() != null) {
            mLoadingLayout = null;
            mLoadingLayout = loadingLayout;
        } else {
            loadingLayout = null;
            addView(mLoadingLayout, -1);
        }

        switch (mMode) {
            case BOTH:
            case PULL_TO_REFRESH: {
                mHeaderLayout = createHeaderLayout(mContext);
                if (mHeaderLayout.getParent() == null) {
                    addView(mHeaderLayout, -1);
                }
                break;
            }
            case AUTO_LOAD:
            case DISABLED: {
                mIsDisable = true;
                break;
            }
        }

        mCurrentMode = mMode;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);

        if (action == MotionEvent.ACTION_DOWN && mDisableScrollWhenRefreshing && (isRefreshing() || mCurrentState == State.SCROLL_TO_REFRESH)) {
            return true;
        }

        if (!isEnabled() || mIsDisable || mCurrentState == State.LOADING_DATA || mMode == Mode.DISABLED || mMode == Mode.AUTO_LOAD
                || mNestedScrollInProgress || canChildScrollUp()) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        if (mReturningToStart) {
            mActivePointerId = MotionEventCompat.getPointerId(event, 0);
            mIsBeingDragged = false;
            final float initialDownY = getMotionEventY(event, mActivePointerId);
            if (initialDownY == -1) {
                return false;
            }
            mInitialDownY = initialDownY;
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(event, 0);
                mIsBeingDragged = false;
                final float initialDownY = getMotionEventY(event, mActivePointerId);
                if (initialDownY == -1) {
                    return false;
                }
                mInitialDownY = initialDownY;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final float y = getMotionEventY(event, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                final float yDiff = y - mInitialDownY;
                if (yDiff > mTouchSlop && !mIsBeingDragged) {
                    mInitialMotionY = mInitialDownY + mTouchSlop;
                    mIsBeingDragged = true;
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                if (getScrollY() != 0) {
                    mIsMoving = false;
                    smoothScrollTo(0);
                }
                break;
        }

        return mIsBeingDragged;
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        int pointerIndex = -1;

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (isRefreshing()) {
            return true;
        }

        if (!isEnabled() || mReturningToStart || canChildScrollUp() || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(event, 0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                pointerIndex = MotionEventCompat.findPointerIndex(event, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(event, pointerIndex);
                final float overScrollTop = (mInitialMotionY - y) * DRAG_RATE;
                if (mIsBeingDragged) {
                    if (overScrollTop <= 0) {
                        moveHeader((int) overScrollTop);
                    } else {
                        mIsBeingDragged = false;
                        mIsMoving = false;
                        setBodyScroll(0);
                        // Apps can set the interception target other than the direct parent.
                        final ViewGroup parent = this;

                        // Get offset to parents. If the parent is not the direct parent,
                        // we should aggregate offsets from all of the parents.
                        float offsetX = 0;
                        float offsetY = 0;
                        for (View v = this; v != null && v != parent; ) {
                            offsetX += v.getLeft() - v.getScrollX();
                            offsetY += v.getTop() - v.getScrollY();
                            try {
                                v = (View) v.getParent();
                            } catch (ClassCastException ex) {
                                break;
                            }
                        }
                        final MotionEvent ev = MotionEvent.obtainNoHistory(event);
                        ev.offsetLocation(offsetX, offsetY);

                        // If the parent wants to intercept ACTION_MOVE events,
                        // we pass ACTION_DOWN event to the parent
                        // as if these touch events just have began now.
                        ev.setAction(MotionEvent.ACTION_DOWN);
                        // Return this onTouchEvent() first and set ACTION_DOWN event for parent
                        // to the queue, to keep events sequence.
                        post(new Runnable() {
                            @Override
                            public void run() {
                                mCurrentState = State.RESET;
                                parent.dispatchTouchEvent(ev);
                            }
                        });
                        return false;
                    }
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                pointerIndex = MotionEventCompat.getActionIndex(event);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return false;
                }
                mActivePointerId = MotionEventCompat.getPointerId(event, pointerIndex);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                pointerIndex = MotionEventCompat.findPointerIndex(event, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(event, pointerIndex);
                final float needToScrollValue = (mInitialMotionY - y) * DRAG_RATE;
                mIsBeingDragged = false;
                finishHeader(needToScrollValue);
                mActivePointerId = INVALID_POINTER;
                return false;
            }
        }

        return true;
    }

    public final boolean isRefreshing() {
        return mCurrentState == State.REFRESHING;
    }

    public final boolean isScrolling() {
        return mCurrentState == State.AUTO_SCROLLING || mCurrentState == State.SCROLL_TO_REFRESH;
    }

    public final boolean isAutoLoading() {
        return mCurrentState == State.AUTO_LOADING;
    }

    public final boolean isManualScrolling() {
        return mCurrentState == State.MANUAL_SCROLLING;
    }

    public void setRefreshUsable(boolean usable) {
        mIsDisable = !usable;
    }

    private void callRefreshListener() {
        if (null != mOnRefreshListener) {
            setState(State.REFRESHING);
            mHeaderLayout.onRefreshBegin();
            mOnRefreshListener.onRefresh();
        } else {
            mCurrentState = State.RESET;
            setState(State.RESET);
        }
    }

    public T getRefreshableView() {
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

    private void smoothScrollTo(int newScrollValue, long duration, long delayMillis, OnSmoothScrollFinishedListener listener) {
        if (null != mCurrentSmoothScrollRunnable) {
            mCurrentSmoothScrollRunnable.stop();
        }

        final int oldScrollValue = getScrollY();

        if (oldScrollValue != newScrollValue) {
            if (mCurrentState != State.SCROLL_TO_REFRESH) {
                mCurrentState = State.AUTO_SCROLLING;
            }
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
            mCurrentState = State.RESET;
            mReturningToStart = false;
        }
    }

    private void moveHeader(int needToScrollValue) {
        if (mCurrentMode == Mode.DISABLED || mCurrentMode == Mode.AUTO_LOAD || needToScrollValue > 0) {
            return;
        }
        if (needToScrollValue == 0) {
            setBodyScroll(0);
            return;
        }

        mIsMoving = true;

        int actualScrolledValue;
        if (needToScrollValue < -mPullMaxDistance) {
            actualScrolledValue = -mPullMaxDistance;
        } else {
            actualScrolledValue = needToScrollValue;
        }

        mCurrentState = State.MANUAL_SCROLLING;
        setBodyScroll(actualScrolledValue);

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
            setState(State.RESET);
        }
    }

    private void setBodyScroll(int value) {
        scrollTo(0, value);
        mHeaderLayout.setMargins(0, value, 0, 0);
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
                if (mCurrentState == State.AUTO_LOADING && (mMode == Mode.BOTH || mMode == Mode.PULL_TO_REFRESH)) {
                    mHeaderLayout.onRefreshCancel();
                }
                mCurrentState = State.RESET;
                mReturningToStart = false;
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

    protected void handleStyledAttributes(TypedArray a) {

    }

    protected abstract T createRefreshableView(Context context, AttributeSet attrs);

    protected abstract RefreshHeaderLayout createHeaderLayout(Context context);

    private RefreshEmptyLayout createEmptyView(Context context, AttributeSet attrs) {
        RefreshEmptyLayout refreshEmptyLayout;
        View headerView = findViewById(R.id.empty_layout);
        if (headerView != null) {
            if (headerView instanceof RefreshEmptyLayout) {
                refreshEmptyLayout = (RefreshEmptyLayout) headerView;
                refreshEmptyLayout.setVisibility(GONE);  // hidden
            } else {
                throw new IllegalArgumentException("Empty View must be extended RefreshEmptyLayout");
            }
        } else {
            refreshEmptyLayout = new RefreshEmptyLayout(context, attrs);
            refreshEmptyLayout.setVisibility(GONE);  // hidden
        }
        return refreshEmptyLayout;
    }

    private RefreshLoadingLayout createLoadingLayout(Context context, AttributeSet attrs) {
        RefreshLoadingLayout refreshLoadingLayout;
        View headerView = findViewById(R.id.loading_layout);
        if (headerView != null) {
            if (headerView instanceof RefreshLoadingLayout) {
                refreshLoadingLayout = (RefreshLoadingLayout) headerView;
                refreshLoadingLayout.setVisibility(GONE);  // hidden
            } else {
                throw new IllegalArgumentException("Loading View must be extended RefreshLoadingLayout");
            }
        } else {
            refreshLoadingLayout = new RefreshLoadingLayout(context, attrs);
            refreshLoadingLayout.setVisibility(GONE);  // hidden
        }
        return refreshLoadingLayout;
    }

    public void setEmptyLayoutResId(int resId) {
        View emptyView = LayoutInflater.from(mContext).inflate(resId, null);
        setEmptyView(emptyView);
    }

    public void setEmptyView(View emptyView) {
        mEmptyLayout.removeAllViews();
        mEmptyLayout.addView(emptyView);
    }

    public void setLoadingLayoutResId(int resId) {
        View loadingView = LayoutInflater.from(mContext).inflate(resId, null);
        setEmptyView(loadingView);
    }

    public void setLoadingView(View loadingView) {
        mEmptyLayout.removeAllViews();
        mEmptyLayout.addView(loadingView);
    }

    public boolean canChildScrollUp() {
        if (mRefreshableView.getVisibility() == View.GONE) {
            return false;
        }
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mRefreshableView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mRefreshableView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mRefreshableView, -1) || mRefreshableView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mRefreshableView, -1);
        }
    }

    interface OnSmoothScrollFinishedListener {
        void onSmoothScrollFinished();
    }

    public interface OnRefreshListener {

        void onRefresh();
    }

    public interface OnAutoLoadListener {

        void onLoading();
    }

    public final void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mCurrentState = State.mapIntToValue(bundle.getInt(STATE_STATE));
            mMode = Mode.mapIntToValue(bundle.getInt(STATE_MODE));
            mIsDisable = bundle.getBoolean(STATE_ABLE);
            super.onRestoreInstanceState(bundle.getParcelable(STATE_SUPER));
            return;
        }
        try {
            super.onRestoreInstanceState(state);
        } catch (Exception e) {

        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putInt(STATE_STATE, mCurrentState.getIntValue());
        bundle.putInt(STATE_MODE, mMode.getIntValue());
        bundle.putBoolean(STATE_ABLE, mIsDisable);
        bundle.putParcelable(STATE_SUPER, super.onSaveInstanceState());
        return bundle;
    }

    public enum Mode {

        /**
         * Both pull-to-refresh and auto-load
         */
        BOTH(0x0),

        /**
         * Only pull-to-refresh and disable auto-load
         */
        PULL_TO_REFRESH(0x1),

        /**
         * Only auto-load and disable pull-to-refresh
         */
        AUTO_LOAD(0x2),

        /**
         * Disable pull-to-refresh and auto-load
         */
        DISABLED(0x3);

        /**
         * Maps an int to a specific mode. This is needed when saving state, or
         * inflating the view from XML where the mode is given through a attr
         * int.
         *
         * @param modeInt - int to map a Person to
         * @return Person that modeInt maps to, or PULL_FROM_START by default.
         */
        static Mode mapIntToValue(final int modeInt) {
            for (Mode value : Mode.values()) {
                if (modeInt == value.getIntValue()) {
                    return value;
                }
            }
            return getDefault();
        }

        static Mode getDefault() {
            return BOTH;
        }

        private int mIntValue;

        // The modeInt values need to match those from attrs.xml
        Mode(int modeInt) {
            mIntValue = modeInt;
        }

        int getIntValue() {
            return mIntValue;
        }

    }

    public enum State {

        RESET(0x0),

        LOADING_DATA(0x4),

        SCROLL_TO_REFRESH(0x5),

        AUTO_SCROLLING(0x6),

        MANUAL_SCROLLING(0x7),

        REFRESHING(0x8),

        AUTO_LOADING(0x10),

        LOADING_ERROR(0x11),

        LOADING_END(0x12);

        static State mapIntToValue(final int stateInt) {
            for (State value : State.values()) {
                if (stateInt == value.getIntValue()) {
                    return value;
                }
            }
            return RESET;
        }

        private int mIntValue;

        State(int intValue) {
            mIntValue = intValue;
        }

        int getIntValue() {
            return mIntValue;
        }
    }

    protected void setState(State state) {
        switch (state) {
            case AUTO_SCROLLING:
            case RESET:
                mCurrentState = State.AUTO_SCROLLING;
                onReset();
                break;
            case SCROLL_TO_REFRESH:
                mCurrentState = State.SCROLL_TO_REFRESH;
                mReturningToStart = true;
                smoothScrollTo(-mRefreshingDistance, mOnSmoothScrollFinishedListener);
                break;
            case REFRESHING:
                mCurrentState = State.REFRESHING;
                break;
            case AUTO_LOADING:
                mCurrentState = State.AUTO_LOADING;
                break;
        }
    }

    public Mode getMode() {
        return mMode;
    }

    public void setMode(Mode mode) {
        if (mode != mMode) {
            mMode = mode;
        }
    }

    protected void onReset() {
        mIsBeingDragged = false;
        mIsMoving = false;
        mReturningToStart = true;
        smoothScrollTo(0);
    }

    public void onAutoLoadingComplete(boolean usable) {
        if (mCurrentState == State.AUTO_LOADING) {
            if (usable) {
                mCurrentState = State.RESET;
            } else {
                mCurrentState = State.LOADING_ERROR;
            }
        }
    }

    public void onAutoLoadingError() {
        if (mCurrentState == State.AUTO_LOADING) {
            mCurrentState = State.LOADING_ERROR;
        }
    }

    public void onRefreshComplete() {
        if (mHeaderLayout != null) {
            mHeaderLayout.onRefreshFinish();
        }
        setState(State.AUTO_SCROLLING);
        checkBody();
    }

    public void onLoadingDataStart() {
        mCurrentState = State.LOADING_DATA;
        mRefreshableView.setVisibility(GONE);
        mLoadingLayout.setVisibility(VISIBLE);
        mEmptyLayout.setVisibility(GONE);
    }

    public void onLoadingDataComplete() {
        mCurrentState = State.RESET;
        mRefreshableView.setVisibility(VISIBLE);
        mLoadingLayout.setVisibility(GONE);
        mEmptyLayout.setVisibility(GONE);
        checkBody();
    }

    protected abstract void checkBody();

    public boolean isShowEmpty() {
        return showEmpty;
    }

    public void setShowEmpty(boolean showEmpty) {
        this.showEmpty = showEmpty;
    }

    // NestedScrollingParent

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return isEnabled() && !mReturningToStart && !isRefreshing() && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;
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
            moveHeader((int) (mTotalUnconsumed * DRAG_RATE));
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
        if (mTotalUnconsumed != 0) {
            finishHeader(mTotalUnconsumed);
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
            moveHeader((int) (mTotalUnconsumed * DRAG_RATE));
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
