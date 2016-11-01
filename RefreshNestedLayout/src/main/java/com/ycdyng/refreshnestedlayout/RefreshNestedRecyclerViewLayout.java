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

package com.ycdyng.refreshnestedlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.ycdyng.refreshnestedlayout.kernel.RefreshHeaderLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshNestedLayout;
import com.ycdyng.refreshnestedlayout.custom.DefaultHeaderLayout;
import com.ycdyng.refreshnestedlayout.kernel.RecycleViewDivider;
import com.ycdyng.refreshnestedlayout.kernel.RefreshRecyclerView;
import com.ycdyng.refreshnestedlayout.widget.adapter.WrapRecyclerAdapter;

public class RefreshNestedRecyclerViewLayout extends RefreshNestedLayout<RefreshRecyclerView> {

    private WrapRecyclerAdapter mWrapRecyclerAdapter;
    private OnAutoLoadListener mOnAutoLoadListener;
    private boolean mDisableDivider = true;
    private int mDividerHeight;
    private int mDividerColor;

    private int mAutoLoadResId;
    private int mClickableResId;
    private int mLoadEndResId;
    private int mLoadErrorResId;

    public RefreshNestedRecyclerViewLayout(Context context) {
        super(context);
    }

    public RefreshNestedRecyclerViewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected RefreshRecyclerView createRefreshableView(Context context, AttributeSet attrs) {
        return createRecyclerView(context, attrs);
    }

    @Override
    protected RefreshHeaderLayout createHeaderLayout(Context context) {
        RefreshHeaderLayout refreshHeaderLayout;
        View headerView = findViewById(R.id.header_layout);
        if (headerView != null) {
            if (headerView instanceof RefreshHeaderLayout) {
                refreshHeaderLayout = (RefreshHeaderLayout) headerView;
                refreshHeaderLayout.setHeight(0);  // hidden
            } else {
                throw new IllegalArgumentException("Header View must be extended BaseHeaderLayout");
            }
        } else {
            refreshHeaderLayout = new DefaultHeaderLayout(context);
            refreshHeaderLayout.setHeight(0);  // hidden
        }
        return refreshHeaderLayout;
    }

    @Override
    protected void handleStyledAttributes(TypedArray a) {
        super.handleStyledAttributes(a);
        mDisableDivider = a.getBoolean(R.styleable.RefreshNestedLayout_disableDivider, true);
        mDividerHeight = a.getDimensionPixelOffset(R.styleable.RefreshNestedLayout_dividerHeight, getResources().getDimensionPixelOffset(R.dimen.default_divider_height));
        mDividerColor = a.getColor(R.styleable.RefreshNestedLayout_dividerColor, getResources().getColor(R.color.default_divider_color));

        mAutoLoadResId = a.getResourceId(R.styleable.RefreshNestedLayout_autoLoadLayout, R.layout.default_footer_loading_layout);
        mClickableResId = a.getResourceId(R.styleable.RefreshNestedLayout_clickableLayout, R.layout.default_footer_clickable_layout);
        mLoadEndResId = a.getResourceId(R.styleable.RefreshNestedLayout_loadEndLayout, R.layout.default_footer_end_layout);
        mLoadErrorResId = a.getResourceId(R.styleable.RefreshNestedLayout_loadErrorLayout, R.layout.default_footer_error_layout);
    }

    private RefreshRecyclerView createRecyclerView(Context context, AttributeSet attrs) {
        RefreshRecyclerView internalRecyclerView = null;
        View refreshableView = findViewById(R.id.refreshable_layout);
        if (refreshableView == null) {
            internalRecyclerView = new RefreshRecyclerView(context, attrs);
        } else {
            if (refreshableView instanceof RefreshRecyclerView) {
                internalRecyclerView = (RefreshRecyclerView) refreshableView;
            } else {
                throw new IllegalArgumentException("Refreshable View must be extends RefreshRecyclerView");
            }
        }
        internalRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy >= 0 && mOnAutoLoadListener != null && isAutoLoad()) { // scroll down
                    RecyclerView.LayoutManager layoutManager = mRefreshableView.getLayoutManager();
                    if (layoutManager != null && layoutManager instanceof LinearLayoutManager) {
                        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                        int visibleItemCount = linearLayoutManager.getChildCount();
                        int totalItemCount = linearLayoutManager.getItemCount();
                        int firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();
                        if ((visibleItemCount + firstVisibleItem) >= totalItemCount && (mCurrentState == State.RESET || mCurrentState == State.AUTO_SCROLLING ||
                                mCurrentState == State.LOADING_END || mCurrentState == State.LOADING_ERROR || mCurrentState == State.SCROLL_TO_REFRESH)) {
                            if (mCurrentState == State.LOADING_END) {
                                // do nothing...
                            } else if (mCurrentState == State.LOADING_ERROR) {
                                // do nothing...
                            } else {
                                setState(State.AUTO_LOADING);
                                mOnAutoLoadListener.onLoading();
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("LayoutManager must be LinearLayoutManager");
                    }
                }
            }
        });

        if (!mDisableDivider) {
            internalRecyclerView.addItemDecoration(new RecycleViewDivider(mDividerHeight, mDividerColor));
        }
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        internalRecyclerView.setLayoutManager(linearLayoutManager);
        return internalRecyclerView;
    }

    public void addItemDecoration() {
        mDisableDivider = false;
        getRefreshableView().addItemDecoration(new RecycleViewDivider(mDividerHeight, mDividerColor));
    }

    public void addItemDecoration(int dividerHeight) {
        mDisableDivider = false;
        mDividerHeight = dividerHeight;
        getRefreshableView().addItemDecoration(new RecycleViewDivider(mDividerHeight, mDividerColor));
    }

    public void addItemDecoration(int dividerHeight, int dividerColor) {
        mDisableDivider = false;
        mDividerHeight = dividerHeight;
        mDividerColor = dividerColor;
        getRefreshableView().addItemDecoration(new RecycleViewDivider(mDividerHeight, mDividerColor));
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        if (adapter instanceof WrapRecyclerAdapter) {
            mWrapRecyclerAdapter = (WrapRecyclerAdapter) adapter;
            mWrapRecyclerAdapter.setOnLastItemClickListener(mLastItemOnClickListener);
            mWrapRecyclerAdapter.setAutoLoadResId(mAutoLoadResId);
            mWrapRecyclerAdapter.setClickableResId(mClickableResId);
            mWrapRecyclerAdapter.setLoadEndResId(mLoadEndResId);
            mWrapRecyclerAdapter.setLoadErrorResId(mLoadErrorResId);
            mRefreshableView.setAdapter(mWrapRecyclerAdapter);
        } else {
            throw new IllegalArgumentException("RecyclerView.Adapter must be extends WrapRecyclerAdapter");
        }
    }


    public boolean getAutoLoadUsable() {
        return mWrapRecyclerAdapter.getAutoLoadUsable();
    }

    public void setAutoLoadUsable(boolean usable) {
        mWrapRecyclerAdapter.setAutoLoadUsable(usable);
    }

    public void setOnScrollListener(RecyclerView.OnScrollListener listener) {
        mRefreshableView.addOnScrollListener(listener);
    }

    public void setOnAutoLoadListener(OnAutoLoadListener listener) {
        mOnAutoLoadListener = listener;
    }

    private boolean isFirstItemVisible() {
        final RecyclerView.Adapter adapter = mRefreshableView.getAdapter();
        if (null == adapter || adapter.getItemCount() == 0) {
            return true;
        } else {
            RecyclerView.LayoutManager layoutManager = mRefreshableView.getLayoutManager();
            if (layoutManager != null && layoutManager instanceof LinearLayoutManager) {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                if (linearLayoutManager.findFirstVisibleItemPosition() <= 1) {
                    final View firstVisibleChild = mRefreshableView.getChildAt(0);
                    if (firstVisibleChild != null) {
                        return firstVisibleChild.getTop() >= mRefreshableView.getTop();
                    }
                }
            } else {
                throw new IllegalArgumentException("LayoutManager must be LinearLayoutManager");
            }
        }
        return false;
    }

    private boolean isLastItemVisible() {
        final RecyclerView.Adapter adapter = mRefreshableView.getAdapter();
        if (null == adapter || adapter.getItemCount() == 0) {
            return true;
        } else {
            RecyclerView.LayoutManager layoutManager = mRefreshableView.getLayoutManager();
            if (layoutManager != null && layoutManager instanceof LinearLayoutManager) {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                int totalItemCount = linearLayoutManager.getItemCount();
                final int lastItemPosition = totalItemCount - 1;
                final int lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition();
                if (lastVisiblePosition >= lastItemPosition - 1) {
                    final int firstVisiblePosition = linearLayoutManager.findFirstVisibleItemPosition();
                    final int childIndex = lastVisiblePosition - firstVisiblePosition;
                    final View lastVisibleChild = mRefreshableView.getChildAt(childIndex);
                    if (lastVisibleChild != null) {
                        return lastVisibleChild.getBottom() <= mRefreshableView.getBottom();
                    }
                }
            } else {
                throw new IllegalArgumentException("LayoutManager must be LinearLayoutManager");
            }
        }
        return false;
    }

    @Override
    public void onRefreshComplete() {
        super.onRefreshComplete();
        resetLastItemView();
    }

    @Override
    protected void checkBody() {
        if (showEmpty && mWrapRecyclerAdapter.getItemCount() > 0 && !mWrapRecyclerAdapter.getAutoLoadUsable()) {
            mRefreshableView.setVisibility(GONE);
            mEmptyLayout.setVisibility(VISIBLE);
        } else {
            mRefreshableView.setVisibility(VISIBLE);
            mEmptyLayout.setVisibility(GONE);
        }
    }

    @Override
    public void onAutoLoadingComplete(boolean usable) {
        super.onAutoLoadingComplete(usable);
        if (usable) {
            resetLastItemView();
        } else {
            if (isFirstItemVisible() && isLastItemVisible()) {
                if (mWrapRecyclerAdapter != null) {
                    mWrapRecyclerAdapter.setLoadEnd(false);
                    mWrapRecyclerAdapter.setLoadError(false);
                    mWrapRecyclerAdapter.setLoading(false);
                    mWrapRecyclerAdapter.setAutoLoadUsable(false);
                }
            } else {
                if (mWrapRecyclerAdapter != null) {
                    mWrapRecyclerAdapter.setLoadEnd(true);
                    mWrapRecyclerAdapter.setLoadError(false);
                    mWrapRecyclerAdapter.setLoading(false);
                    mWrapRecyclerAdapter.notifyItemChanged(mWrapRecyclerAdapter.getItemCount());
                }
            }
        }
    }

    @Override
    public void onAutoLoadingError() {
        super.onAutoLoadingError();
        mWrapRecyclerAdapter.setLoadEnd(false);
        mWrapRecyclerAdapter.setLoadError(true);
        mWrapRecyclerAdapter.setLoading(false);
        mWrapRecyclerAdapter.notifyItemChanged(mWrapRecyclerAdapter.getItemCount());
    }

    public boolean isAutoLoad() {
        if (mWrapRecyclerAdapter != null) {
            return mWrapRecyclerAdapter.isAutoLoad();
        }
        return false;
    }

    public void setAutoLoad(boolean autoLoad) {
        if (mWrapRecyclerAdapter != null) {
            mWrapRecyclerAdapter.setAutoLoad(autoLoad);
        }
    }

    private void resetLastItemView() {
        if (mWrapRecyclerAdapter.isLoadEnd() || mWrapRecyclerAdapter.isLoadError()) {
            mWrapRecyclerAdapter.setLoadEnd(false);
            mWrapRecyclerAdapter.setLoadError(false);
            mWrapRecyclerAdapter.setLoading(false);
            mWrapRecyclerAdapter.notifyItemChanged(mWrapRecyclerAdapter.getItemCount());
        }
    }

    private View.OnClickListener mLastItemOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setState(State.AUTO_LOADING);
            if (!mWrapRecyclerAdapter.isAutoLoad()) {
                mWrapRecyclerAdapter.setLoadEnd(false);
                mWrapRecyclerAdapter.setLoadError(false);
                mWrapRecyclerAdapter.setLoading(true);
                mWrapRecyclerAdapter.notifyItemChanged(mWrapRecyclerAdapter.getItemCount());
            } else {
                resetLastItemView();
            }
            mOnAutoLoadListener.onLoading();
        }
    };
}
