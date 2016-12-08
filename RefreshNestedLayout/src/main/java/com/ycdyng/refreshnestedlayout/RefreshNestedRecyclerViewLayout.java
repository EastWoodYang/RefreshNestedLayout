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

import com.ycdyng.refreshnestedlayout.custom.DefaultHeaderLayout;
import com.ycdyng.refreshnestedlayout.kernel.RecycleViewDivider;
import com.ycdyng.refreshnestedlayout.kernel.RefreshHeaderLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshNestedLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshRecyclerView;
import com.ycdyng.refreshnestedlayout.widget.adapter.AutoRecyclerAdapter;
import com.ycdyng.refreshnestedlayout.widget.adapter.WrapRecyclerAdapter;

public class RefreshNestedRecyclerViewLayout extends RefreshNestedLayout<RefreshRecyclerView> {

    private AutoRecyclerAdapter mAutoRecyclerAdapter;
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
        View refreshableView = findViewById(R.id.refreshable_view);
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
                if (dy >= 0 && mOnAutoLoadListener != null && getAutoLoadUsable()) { // scroll down
                    RecyclerView.LayoutManager layoutManager = mRefreshableView.getLayoutManager();
                    if (layoutManager != null && layoutManager instanceof LinearLayoutManager) {
                        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                        int visibleItemCount = linearLayoutManager.getChildCount();
                        int totalItemCount = linearLayoutManager.getItemCount();
                        int firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();
                        if (mCurrentLoadState != LoadState.AUTO_LOADING && (visibleItemCount + firstVisibleItem) >= totalItemCount &&
                                (mCurrentState == State.RESET || mCurrentState == State.AUTO_SCROLLING || mCurrentState == State.SCROLL_TO_REFRESH ||
                                        mCurrentLoadState == LoadState.LOADING_END || mCurrentLoadState == LoadState.LOADING_ERROR)) {
                            if (!isManualLoad()) {
                                if (mCurrentLoadState == LoadState.LOADING_END) {
                                    // do nothing...
                                } else if (mCurrentLoadState == LoadState.LOADING_ERROR) {
                                    // do nothing...
                                } else {
                                    mCurrentLoadState = LoadState.AUTO_LOADING;
                                    mOnAutoLoadListener.onLoading();
                                }
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
        if (adapter instanceof AutoRecyclerAdapter) {
            mAutoRecyclerAdapter = (AutoRecyclerAdapter) adapter;
            mAutoRecyclerAdapter.setOnLastItemClickListener(mLastItemOnClickListener);
            mAutoRecyclerAdapter.setAutoLoadResId(mAutoLoadResId);
            mAutoRecyclerAdapter.setClickableResId(mClickableResId);
            mAutoRecyclerAdapter.setLoadEndResId(mLoadEndResId);
            mAutoRecyclerAdapter.setLoadErrorResId(mLoadErrorResId);
            mRefreshableView.setAdapter(mAutoRecyclerAdapter);
        } else {
            throw new IllegalArgumentException("Adapter must be extends QuickRecyclerAdapter or WrapRecyclerAdapter");
        }
    }

    public boolean getAutoLoadUsable() {
        return mAutoRecyclerAdapter.getAutoLoadUsable();
    }

    public void setAutoLoadUsable(boolean usable) {
        mAutoRecyclerAdapter.setAutoLoadUsable(usable);
    }

    public void setShowLoadEnd(boolean usable) {
        mAutoRecyclerAdapter.setShowLoadEnd(usable);
    }

    public boolean isManualLoad() {
        return mAutoRecyclerAdapter.isManualLoad();
    }

    public void setManualLoad(boolean autoLoad) {
        mAutoRecyclerAdapter.setManualLoad(autoLoad);
    }

    public void setOnScrollListener(RecyclerView.OnScrollListener listener) {
        mRefreshableView.addOnScrollListener(listener);
    }

    public void setOnAutoLoadListener(OnAutoLoadListener listener) {
        mOnAutoLoadListener = listener;
    }

    @Override
    protected void checkBody() {
        if (mShowEmptyLayout && !mAutoRecyclerAdapter.getAutoLoadUsable() &&
                ((mCurrentLoadState == LoadState.RESET && mAutoRecyclerAdapter.getItemCount() == 0) ||
                (mCurrentLoadState == LoadState.LOADING_END && mAutoRecyclerAdapter.getItemCount() <= 1 && mAutoRecyclerAdapter.getLastItemViewType() == AutoRecyclerAdapter.END_VIEW_TYPE))) {
            crossFading(mEmptyLayout, mRefreshableView);
        } else {
            crossFading(mRefreshableView, mEmptyLayout);
        }
    }

    @Override
    public void onLoadingDataComplete(boolean loadable) {
        super.onLoadingDataComplete(loadable);
        if(!loadable) {
            mAutoRecyclerAdapter.setLoadEnd(true);
            mAutoRecyclerAdapter.setLoadError(false);
            mAutoRecyclerAdapter.setLoading(false);
            mAutoRecyclerAdapter.notifyItemChanged(mAutoRecyclerAdapter.getItemCount());
        }
    }

    @Override
    public void onRefreshComplete(boolean loadable) {
        if(loadable) {
            super.onRefreshComplete(true);
            resetLastItemView();
        } else {
            mAutoRecyclerAdapter.setLoadEnd(true);
            mAutoRecyclerAdapter.setLoadError(false);
            mAutoRecyclerAdapter.setLoading(false);
            mAutoRecyclerAdapter.notifyItemChanged(mAutoRecyclerAdapter.getItemCount());
            super.onRefreshComplete(false);
        }
    }

    @Override
    public void onAutoLoadingComplete(boolean usable) {
        super.onAutoLoadingComplete(usable);
        if (usable) {
            resetLastItemView();
        } else {
            mAutoRecyclerAdapter.setLoadEnd(true);
            mAutoRecyclerAdapter.setLoadError(false);
            mAutoRecyclerAdapter.setLoading(false);
            mAutoRecyclerAdapter.notifyItemChanged(mAutoRecyclerAdapter.getItemCount());
        }
    }

    @Override
    public void onAutoLoadingError() {
        super.onAutoLoadingError();
        mAutoRecyclerAdapter.setLoadEnd(false);
        mAutoRecyclerAdapter.setLoadError(true);
        mAutoRecyclerAdapter.setLoading(false);
        mAutoRecyclerAdapter.notifyItemChanged(mAutoRecyclerAdapter.getItemCount());
    }

    private void resetLastItemView() {
        if (mAutoRecyclerAdapter.isManualLoad() || mAutoRecyclerAdapter.isLoadEnd() || mAutoRecyclerAdapter.isLoadError()) {
            mAutoRecyclerAdapter.setLoadEnd(false);
            mAutoRecyclerAdapter.setLoadError(false);
            mAutoRecyclerAdapter.setLoading(false);
            mAutoRecyclerAdapter.notifyItemChanged(mAutoRecyclerAdapter.getItemCount());
        }
    }

    private View.OnClickListener mLastItemOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCurrentLoadState = LoadState.AUTO_LOADING;
            if (mAutoRecyclerAdapter.isManualLoad()) {
                mAutoRecyclerAdapter.setLoadEnd(false);
                mAutoRecyclerAdapter.setLoadError(false);
                mAutoRecyclerAdapter.setLoading(true);
                mAutoRecyclerAdapter.notifyItemChanged(mAutoRecyclerAdapter.getItemCount());
            } else {
                resetLastItemView();
            }
            mOnAutoLoadListener.onLoading();
        }
    };
}
