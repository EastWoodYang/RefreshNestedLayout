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
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;

import com.ycdyng.refreshnestedlayout.custom.DefaultHeaderLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshHeaderLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshListView;
import com.ycdyng.refreshnestedlayout.kernel.RefreshNestedLayout;
import com.ycdyng.refreshnestedlayout.widget.adapter.AutoBaseAdapter;

public class RefreshNestedListViewLayout extends RefreshNestedLayout<RefreshListView> {

    private AutoBaseAdapter mAutoBaseAdapter;
    private RefreshNestedLayout.OnAutoLoadListener mOnAutoLoadListener;
    private OnScrollListener mOriginalScrollListener;

    private int mAutoLoadResId;
    private int mClickableResId;
    private int mLoadEndResId;
    private int mLoadErrorResId;

    public RefreshNestedListViewLayout(Context context) {
        super(context);
    }

    public RefreshNestedListViewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected RefreshListView createRefreshableView(Context context, AttributeSet attrs) {
        return createListView(context, attrs);
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
                throw new IllegalArgumentException("Header View must be extended RefreshHeaderLayout");
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
        mAutoLoadResId = a.getResourceId(R.styleable.RefreshNestedLayout_autoLoadLayout, R.layout.default_footer_loading_layout);
        mClickableResId = a.getResourceId(R.styleable.RefreshNestedLayout_clickableLayout, R.layout.default_footer_clickable_layout);
        mLoadEndResId = a.getResourceId(R.styleable.RefreshNestedLayout_loadEndLayout, R.layout.default_footer_end_layout);
        mLoadErrorResId = a.getResourceId(R.styleable.RefreshNestedLayout_loadErrorLayout, R.layout.default_footer_error_layout);
    }

    protected RefreshListView createListView(Context context, AttributeSet attrs) {
        RefreshListView internalListView = null;
        View refreshableView = findViewById(R.id.refreshable_view);
        if (refreshableView == null) {
            internalListView = new RefreshListView(context, attrs);
        } else {
            if (refreshableView instanceof RefreshListView) {
                internalListView = (RefreshListView) refreshableView;
            } else {
                throw new IllegalArgumentException("Refreshable View must be extends RefreshListView");
            }
        }
        internalListView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (mOriginalScrollListener != null) {
                    mOriginalScrollListener.onScrollStateChanged(view, scrollState);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (mOriginalScrollListener != null) {
                    mOriginalScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
                }
                if (mCurrentLoadState != LoadState.AUTO_LOADING && (firstVisibleItem + visibleItemCount >= totalItemCount) &&
                        (mCurrentState == State.NONE || mCurrentState == State.SCROLL_TO_REFRESH ||
                                mCurrentLoadState == LoadState.LOADING_ERROR || mCurrentLoadState == LoadState.LOADING_END)) {
                    if (mOnAutoLoadListener != null && getAutoLoadUsable()) {
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
                }
            }
        });
        return internalListView;
    }

    public void setAdapter(BaseAdapter baseAdapter) {
        if (baseAdapter instanceof AutoBaseAdapter) {
            mAutoBaseAdapter = (AutoBaseAdapter) baseAdapter;
            mAutoBaseAdapter.setOnLastItemClickListener(mLastItemOnClickListener);
            mAutoBaseAdapter.setAutoLoadResId(mAutoLoadResId);
            mAutoBaseAdapter.setClickableResId(mClickableResId);
            mAutoBaseAdapter.setLoadEndResId(mLoadEndResId);
            mAutoBaseAdapter.setLoadErrorResId(mLoadErrorResId);
            mRefreshableView.setAdapter(mAutoBaseAdapter);
        } else {
            throw new IllegalArgumentException("Adapter must be extends QuickAdapter or WrapAdapter");
        }
    }

    public boolean getAutoLoadUsable() {
        return mAutoBaseAdapter.getAutoLoadUsable();
    }

    public void setAutoLoadUsable(boolean usable) {
        mAutoBaseAdapter.setAutoLoadUsable(usable);
    }

    public void setShowNoMoreDataItem(boolean show) {
        mAutoBaseAdapter.setShowNoMoreDataItem(show);
    }

    public boolean isManualLoad() {
        return mAutoBaseAdapter.isManualLoad();
    }

    public void setManualLoad(boolean autoLoad) {
        mAutoBaseAdapter.setManualLoad(autoLoad);
    }

    public void setOnScrollListener(OnScrollListener listener) {
        // Don't set listener to super.setOnScrollListener().
        // listener receives all events through mScrollListener.
        mOriginalScrollListener = listener;
    }

    public void setOnAutoLoadListener(OnAutoLoadListener listener) {
        mOnAutoLoadListener = listener;
    }

    @Override
    protected void checkBody() {
        if (mShowEmptyLayout && !mAutoBaseAdapter.getAutoLoadUsable() &&
                ((mCurrentLoadState == LoadState.RESET && mAutoBaseAdapter.isEmpty()) ||
                (mCurrentLoadState == LoadState.LOADING_END && mAutoBaseAdapter.getCount() <= 1 && mAutoBaseAdapter.getLastItemViewType() == 1))) {
            crossFading(mEmptyLayout, mRefreshableView);
        } else {
            crossFading(mRefreshableView, mEmptyLayout);
        }
    }

    public void onLoadingComplete() {
        this.onLoadingComplete(false);
    }

    @Override
    public void onLoadingComplete(boolean loadable) {
        super.onLoadingComplete(loadable);
        if (!loadable) {
            mAutoBaseAdapter.setLoadEnd(true);
            mAutoBaseAdapter.setLoadError(false);
            mAutoBaseAdapter.setLoading(false);
            mAutoBaseAdapter.notifyDataSetChanged();
        }
    }

    public void onRefreshComplete() {
        this.onRefreshComplete(false);
    }

    @Override
    public void onRefreshComplete(boolean loadable) {
        if(loadable) {
            super.onRefreshComplete(true);
            resetLastItemView();
        } else {
            mAutoBaseAdapter.setLoadEnd(true);
            mAutoBaseAdapter.setLoadError(false);
            mAutoBaseAdapter.setLoading(false);
            mAutoBaseAdapter.notifyDataSetChanged();
            super.onRefreshComplete(false);
        }
    }

    @Override
    public void onAutoLoadingComplete(boolean loadable) {
        super.onAutoLoadingComplete(loadable);
        if (loadable) {
            resetLastItemView();
        } else {
            mAutoBaseAdapter.setLoadEnd(true);
            mAutoBaseAdapter.setLoadError(false);
            mAutoBaseAdapter.setLoading(false);
            mAutoBaseAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onAutoLoadingError() {
        super.onAutoLoadingError();
        mAutoBaseAdapter.setLoadEnd(false);
        mAutoBaseAdapter.setLoadError(true);
        mAutoBaseAdapter.setLoading(false);
        mAutoBaseAdapter.notifyDataSetChanged();
    }

    private void resetLastItemView() {
        if (mAutoBaseAdapter.isManualLoad() || mAutoBaseAdapter.isLoadEnd() || mAutoBaseAdapter.isLoadError()) {
            mAutoBaseAdapter.setLoadEnd(false);
            mAutoBaseAdapter.setLoadError(false);
            mAutoBaseAdapter.setLoading(false);
            mAutoBaseAdapter.notifyDataSetChanged();
        }
    }

    private View.OnClickListener mLastItemOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCurrentLoadState = LoadState.AUTO_LOADING;
            if (mAutoBaseAdapter.isManualLoad()) {
                mAutoBaseAdapter.setLoadEnd(false);
                mAutoBaseAdapter.setLoading(true);
                mAutoBaseAdapter.setLoadError(false);
                mAutoBaseAdapter.notifyDataSetChanged();
            } else {
                resetLastItemView();
            }
            mOnAutoLoadListener.onLoading();
        }
    };
}
