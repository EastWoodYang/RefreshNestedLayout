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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
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
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.ycdyng.refreshnestedlayout.R;

import java.util.Arrays;

public abstract class RefreshNestedLayout<T extends View> extends FrameLayout implements NestedScrollingParent,
        NestedScrollingChild {

    private String LOG_TAG = this.getClass().getSimpleName();
    private Context mContext;
    private AttributeSet mAttributeSet;

    public static final int SMOOTH_SCROLL_DURATION_MS = 250;

    private static final String STATE_MODE = "rnl_mode";
    private static final String STATE_ABLE = "rnl_abel";
    private static final String STATE_SUPER = "rnl_super";

    private static final int INVALID_POINTER = -1;
    private static float DRAG_RATE = .6125f;


    protected boolean mLoading;
    protected boolean mRefreshing;

    protected State mCurrentState = State.NONE;
    protected LoadState mCurrentLoadState = LoadState.RESET;
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

    private boolean mRefreshEnabled = true;
    private boolean mScrollWhenRefreshingEnabled;

    private int mTouchSlop;
    private float mInitialMotionY;
    private float mInitialDownY;

    protected T mRefreshableView;
    private RefreshHeaderLayout mHeaderLayout;
    protected RefreshLoadingLayout mLoadingLayout;
    protected RefreshEmptyLayout mEmptyLayout;

    protected boolean mShowEmptyLayout = true;

    private SmoothScrollRunnable mCurrentSmoothScrollRunnable;
    private Interpolator mScrollAnimationInterpolator;

    private OnRefreshListener mOnRefreshListener;

    private boolean crossFading;
    private OnLoadingStartListener mOnLoadingStartListener;

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

        mScrollWhenRefreshingEnabled = a.getBoolean(R.styleable.RefreshNestedLayout_scrollWhenRefreshingEnabled, true);
        crossFading = a.getBoolean(R.styleable.RefreshNestedLayout_crossFading, true);

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
            ViewCompat.setNestedScrollingEnabled(mRefreshableView, true);
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
                mRefreshEnabled = false;
                break;
            }
        }

        mCurrentMode = mMode;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        Log.e(LOG_TAG, "-------------------------------onInterceptTouchEvent");
        Log.e(LOG_TAG, "onInterceptTouchEvent: " + event.actionToString(event.getAction()) + ", mCurrentState: " + mCurrentState.name());

        if (!mRefreshEnabled || mLoading || mMode == Mode.DISABLED || mMode == Mode.AUTO_LOAD) {
            return false;
        }

        if(mNestedScrollInProgress) {
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
            Log.e(LOG_TAG, "nestedScrollInProgress 1 ...");
            return false;
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
                if (mCurrentState != State.REFRESHING && getScrollY() != 0) {
                    // TODO scrollBody directly ?
                    onReset();
                }
                break;
            }
        }
        Log.e(LOG_TAG, "onInterceptTouchEvent: 8 return " + mIsBeingDragged);
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
            mInitialDownY = activePointerY + getScrollY() / DRAG_RATE - mTouchSlop;
            mIsBeingDragged = false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.e(LOG_TAG, "-------------------------------onTouchEvent");
        Log.e(LOG_TAG, "onTouchEvent: " + event.actionToString(event.getAction()) + ", mCurrentState: " + mCurrentState.name());

        if(mNestedScrollInProgress) {
            return false;
        }

        if (mCurrentState == State.SCROLL_TO_BACK || mCurrentState == State.SCROLL_TO_REFRESH) {
            Log.e(LOG_TAG, "onTouchEvent, return true, mCurrentState: " + mCurrentState.name());
            return true;
        }

        final int action = event.getActionMasked();
        int pointerIndex = -1;

        if (mCurrentState == State.REFRESHING) {
            mIsBeingDragged = true;
            Log.e(LOG_TAG, "onTouchEvent: mIsBeingDragged = true");
        } else if (!isEnabled() || canChildScrollUp() || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            Log.e(LOG_TAG, "nestedScrollInProgress 1 ...");
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
                    Log.e(LOG_TAG, "onTouchEvent: 3 return " + false);
                    return false;
                }

                final float y = event.getY(pointerIndex);
                final float yDiff = y - mInitialDownY;

                Log.e(LOG_TAG, "onTouchEvent, mInitialDownY: " + mInitialDownY + ", mCurrentDownY: " + y);
                Log.e(LOG_TAG, "onTouchEvent, yDiff: " + yDiff);

                startDragging(yDiff);

                final float overScrollTop = (mInitialMotionY - y) * DRAG_RATE;
                Log.e(LOG_TAG, "onTouchEvent, overScrollTop: " + overScrollTop);
                if (mIsBeingDragged) {
                    if (overScrollTop <= 0) {
                        Log.e(LOG_TAG, "onTouchEvent, overScrollTop: " + overScrollTop);
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

                mInitialDownY = secondPointerY + getScrollY() / DRAG_RATE - mTouchSlop;
                mActivePointerId = secondPointerId;
                mIsBeingDragged = false;
                Log.e(LOG_TAG, "onTouchEvent, ACTION_POINTER_DOWN mActivePointerId: " + mActivePointerId);
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
                    Log.e(LOG_TAG, "onTouchEvent: 6 return " + false);
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
                Log.e(LOG_TAG, "onTouchEvent: 7 return " + false);
                return false;
            }
        }
        Log.e(LOG_TAG, "onTouchEvent: 8 return " + true);
        return true;
    }

    private void startDragging(float y) {
        if (y > mTouchSlop && !mIsBeingDragged) {
            mInitialMotionY = mInitialDownY + mTouchSlop;
            mIsBeingDragged = true;
            Log.e(LOG_TAG, "startDragging, mIsBeingDragged = true");
        }
    }

    public final boolean isRefreshing() {
        return mCurrentState == State.REFRESHING;
    }

    public final boolean isScrolling() {
        return mCurrentState == State.SCROLL_TO_REFRESH;
    }

    public final boolean isAutoLoading() {
        return mCurrentLoadState == LoadState.AUTO_LOADING;
    }

    public boolean isLoading() {
        return mLoading;
    }

    public final boolean isManualScrolling() {
        return mCurrentState == State.MANUAL_SCROLLING;
    }

    public void setRefreshEnabled(boolean enabled) {
        mRefreshEnabled = enabled;
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

    public T getRefreshableView() {
        return mRefreshableView;
    }

    public View getEmptyLayout() {
        return mEmptyLayout;
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

        final int oldScrollValue = getScrollY();

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
        if (mCurrentMode == Mode.DISABLED || mCurrentMode == Mode.AUTO_LOAD || needToScrollValue > 0) {
            Log.e(LOG_TAG, "moveHeader, return 1");
            return;
        }
        if (needToScrollValue == 0) {
            setBodyScroll(0);
            Log.e(LOG_TAG, "moveHeader, return 2");
            return;
        }

        float actualScrolledValue;
        if (needToScrollValue < -mPullMaxDistance) {
            actualScrolledValue = -mPullMaxDistance;
        } else {
            actualScrolledValue = needToScrollValue;
        }
        Log.e(LOG_TAG, "moveHeader, actualScrolledValue: " + actualScrolledValue);
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
        Log.e(LOG_TAG, "setBodyScroll(" + value + ")");
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
            Log.e(LOG_TAG, "SmoothScrollRunnable, run");
            /**
             * Only set mStartTime if this is the first time we're starting,
             * else actually calculate the Y delta
             */
            if (mStartTime == -1) {
                mStartTime = System.currentTimeMillis();
            } else if (mIsBeingDragged) {
                Log.e(LOG_TAG, "SmoothScrollRunnable, return");
                return;
            } else {
                Log.e(LOG_TAG, "SmoothScrollRunnable, setBodyScroll");
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
                if (mCurrentLoadState == LoadState.AUTO_LOADING && (mMode == Mode.BOTH || mMode == Mode.PULL_TO_REFRESH)) {
                    mHeaderLayout.onRefreshCancel();
                }
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
        View loadingLayout = findViewById(R.id.loading_layout);
        if (loadingLayout != null) {
            if (loadingLayout instanceof RefreshLoadingLayout) {
                refreshLoadingLayout = (RefreshLoadingLayout) loadingLayout;
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

    public boolean isShowEmptyLayout() {
        return mShowEmptyLayout;
    }

    public void setShowEmptyLayout(boolean showEmptyLayout) {
        this.mShowEmptyLayout = showEmptyLayout;
    }

    public void setEmptyLayoutResourceId(int resource) {
        View view = LayoutInflater.from(mContext).inflate(resource, null);
        setEmptyLayout(view);
    }

    public void setEmptyLayout(View view) {
        mEmptyLayout.removeAllViews();
        mEmptyLayout.addView(view);
    }

    public void setEmptyLayoutTextContent(int resId) {
        String content = mContext.getResources().getText(resId).toString();
        setEmptyLayoutTextContent(content);
    }

    public void setEmptyLayoutTextContent(String content) {
        setEmptyLayoutTextContent(R.id.content_text_view, content);
    }

    public void setEmptyLayoutTextContent(int viewId, int resId) {
        String content = mContext.getResources().getText(resId).toString();
        setEmptyLayoutTextContent(viewId, content);
    }

    public void setEmptyLayoutTextContent(int viewId, String content) {
        if (mEmptyLayout == null) {
            return;
        }
        View contentView = mEmptyLayout.findViewById(viewId);
        if (contentView instanceof TextView) {
            TextView contentTextView = (TextView) contentView;
            contentTextView.setText(content);
        }
    }

    public void setLoadingLayoutResourceId(int resource) {
        View view = LayoutInflater.from(mContext).inflate(resource, null);
        setLoadingView(view);
    }

    public void setLoadingView(View view) {
        mLoadingLayout.removeAllViews();
        mLoadingLayout.addView(view);
    }

    public boolean canChildScrollUp() {
        if (mRefreshableView.getVisibility() == View.GONE) {
            return false;
        }

        if (mCurrentState != State.NONE) {
            return false;
        }

        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mRefreshableView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mRefreshableView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return mRefreshableView.canScrollVertically(-1) || mRefreshableView.getScrollY() > 0;
            }
        } else {
            return mRefreshableView.canScrollVertically(-1);
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

    public interface OnLoadingStartListener {

        void onLoadingStart();
    }

    public final void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    public final void setOnLoadingStartListener(OnLoadingStartListener listener) {
        this.mOnLoadingStartListener = listener;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mMode = Mode.mapIntToValue(bundle.getInt(STATE_MODE));
            mRefreshEnabled = bundle.getBoolean(STATE_ABLE);
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
        bundle.putInt(STATE_MODE, mMode.getIntValue());
        bundle.putBoolean(STATE_ABLE, mRefreshEnabled);
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

        NONE(0x1),

        SCROLL_TO_BACK(0x3),

        AUTO_REFRESH(0x4),

        SCROLL_TO_REFRESH(0x5),

        MANUAL_SCROLLING(0x7),

        REFRESHING(0x8);

        static State mapIntToValue(final int stateInt) {
            for (State value : State.values()) {
                if (stateInt == value.getIntValue()) {
                    return value;
                }
            }
            return NONE;
        }

        private int mIntValue;

        State(int intValue) {
            mIntValue = intValue;
        }

        int getIntValue() {
            return mIntValue;
        }
    }

    public enum LoadState {

        RESET(0x0),

        AUTO_LOADING(0x10),

        LOADING_ERROR(0x11),

        LOADING_END(0x12);

        static LoadState mapIntToValue(final int stateInt) {
            for (LoadState value : LoadState.values()) {
                if (stateInt == value.getIntValue()) {
                    return value;
                }
            }
            return RESET;
        }

        private int mIntValue;

        LoadState(int intValue) {
            mIntValue = intValue;
        }

        int getIntValue() {
            return mIntValue;
        }
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

    protected void setLoadState(boolean loadable) {
        if (loadable) {
            mCurrentLoadState = LoadState.RESET;
        } else {
            mCurrentLoadState = LoadState.LOADING_END;
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
        mCurrentState = State.SCROLL_TO_BACK;
        mActivePointerId = INVALID_POINTER;
        smoothScrollTo(0);
    }

    public void onAutoLoadingComplete(boolean loadable) {
        setLoadState(loadable);
    }

    public void onAutoLoadingError() {
        mCurrentLoadState = LoadState.LOADING_ERROR;
    }

    public void onAutoRefresh() {
        setState(State.AUTO_REFRESH);
    }

    public void onRefreshComplete(boolean loadable) {
        mRefreshing = false;
        setLoadState(loadable);

        if (mCurrentState == State.MANUAL_SCROLLING) {
            return;
        }
        if (mHeaderLayout != null) {
            mHeaderLayout.onRefreshFinish();
        }
        setState(State.SCROLL_TO_BACK);
        checkBody();
    }

    public void onLoadingStart() {
        mLoading = true;
        mRefreshableView.setVisibility(GONE);
        mEmptyLayout.setVisibility(GONE);
        if (mOnLoadingStartListener != null) {
            mOnLoadingStartListener.onLoadingStart();
        } else {
            if (Build.VERSION.SDK_INT >= 12) {
                mLoadingLayout.setAlpha(1f);
            }
            mLoadingLayout.setVisibility(VISIBLE);
        }
    }

    public void onLoadingComplete(boolean loadable) {
        setLoadState(loadable);
        mLoading = false;
        if (mOnLoadingStartListener != null) {
            crossFading(mRefreshableView);
        } else {
            crossFading(mRefreshableView, mLoadingLayout);
        }
        checkBody();
    }

    public void showEmptyLayout() {
        crossFading(mEmptyLayout, mRefreshableView);
    }

    public void hiddenEmptyLayout() {
        crossFading(mRefreshableView, mEmptyLayout);
    }

    public void showLoadingLayout() {
        crossFading(mLoadingLayout, mRefreshableView);
    }

    public void hiddenLoadingLayout() {
        crossFading(mRefreshableView, mLoadingLayout);
    }

    protected abstract void checkBody();

    protected void crossFading(View showView) {
        if (Build.VERSION.SDK_INT >= 12 && crossFading) {
            if (showView.getVisibility() == VISIBLE) {
                return;
            }
            showView.setAlpha(0f);
            showView.setVisibility(VISIBLE);
            showView.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .setListener(null);
        } else {
            showView.setVisibility(VISIBLE);
        }
    }

    protected void crossFading(View showView, final View hideView) {
        if (Build.VERSION.SDK_INT >= 12 && crossFading) {
            if (showView.getVisibility() == VISIBLE) {
                hideView.setVisibility(GONE);
                return;
            }
            showView.setAlpha(0f);
            showView.setVisibility(VISIBLE);
            showView.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .setListener(null);

            hideView.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            hideView.setVisibility(GONE);
                        }
                    });
        } else {
            showView.setVisibility(VISIBLE);
            hideView.setVisibility(GONE);
        }
    }

    // NestedScrollingParent

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        boolean result = isEnabled() && mRefreshEnabled && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        if (result) {
//            if(mCurrentState == State.REFRESHING) {
//                Log.e(LOG_TAG, "onStartNestedScroll: is refreshing");
//                return false;
//            }
            if(!mRefreshableView.canScrollVertically(1)) {
                Log.e(LOG_TAG, "onStartNestedScroll, canScrollDown: " + false);
                return false;
            }
        }
        Log.e(LOG_TAG, "onStartNestedScroll: " + result);
        return result;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = mCurrentState == State.REFRESHING ? - mRefreshingDistance / DRAG_RATE : 0;
        Log.e(LOG_TAG, "onNestedScrollAccepted, mTotalUnconsumed: " + mTotalUnconsumed);
        mNestedScrollInProgress = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        Log.e(LOG_TAG, "onNestedPreScroll, mTotalUnconsumed: " + mTotalUnconsumed + ", dy: " + dy);
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
            Log.e(LOG_TAG, "onNestedPreScroll, moveHeader: " + mTotalUnconsumed * DRAG_RATE);
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
        Log.e(LOG_TAG, "onStopNestedScroll");
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
        Log.e(LOG_TAG, "onNestedScroll: " + dxConsumed + ", " + dyConsumed + ", " + dxUnconsumed + ", " + dyUnconsumed);
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
            Log.e(LOG_TAG, "onNestedScroll, moveHeader: " + mTotalUnconsumed * DRAG_RATE);
            moveHeader(mTotalUnconsumed * DRAG_RATE);
        }
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        Log.e(LOG_TAG, "setNestedScrollingEnabled: " + enabled);
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        boolean result = mNestedScrollingChildHelper.isNestedScrollingEnabled();
        Log.e(LOG_TAG, "isNestedScrollingEnabled: " + result);
        return result;
    }

    @Override
    public boolean startNestedScroll(int axes) {
        boolean result = mNestedScrollingChildHelper.startNestedScroll(axes);
        Log.e(LOG_TAG, "startNestedScroll: " + axes + ", result = " + result);
        return result;
    }

    @Override
    public void stopNestedScroll() {
        Log.e(LOG_TAG, "stopNestedScroll");
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        boolean result = mNestedScrollingChildHelper.hasNestedScrollingParent();
        Log.e(LOG_TAG, "hasNestedScrollingParent: " + result);
        return result;
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        Log.e(LOG_TAG, "dispatchNestedScroll: " + dxConsumed + ", " + dyConsumed + ", " + dxUnconsumed + ", " + dyUnconsumed + ", " + Arrays.toString(offsetInWindow));
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        boolean result = mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
        Log.e(LOG_TAG, "dispatchNestedPreScroll: " + dx + ", " + dy + ", " + Arrays.toString(consumed) + ", " + Arrays.toString(offsetInWindow) + ", result = " + result);
        return result;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        boolean result = dispatchNestedPreFling(velocityX, velocityY);
        Log.e(LOG_TAG, "onNestedPreFling: " + velocityX + ", " + velocityY + ", result = " + result);
        return result;
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        boolean result = dispatchNestedFling(velocityX, velocityY, consumed);
        Log.e(LOG_TAG, "onNestedFling: " + velocityX + ", " + velocityY + ", " + consumed + ", result = " + result);
        return result;
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        boolean result = mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
        Log.e(LOG_TAG, "dispatchNestedFling: " + velocityX + ", " + velocityY + ", " + consumed + ", result = " + result);
        return result;
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        boolean result = mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
        Log.e(LOG_TAG, "dispatchNestedPreFling: " + velocityX + ", " + velocityY + ", result = " + result);
        return result;
    }

}
