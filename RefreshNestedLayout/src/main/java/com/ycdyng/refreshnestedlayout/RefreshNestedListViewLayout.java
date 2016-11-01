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
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.HeaderViewListAdapter;

import com.ycdyng.refreshnestedlayout.kernel.RefreshHeaderLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshNestedLayout;
import com.ycdyng.refreshnestedlayout.custom.DefaultHeaderLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshListView;
import com.ycdyng.refreshnestedlayout.widget.adapter.AutoAdapter;

public class RefreshNestedListViewLayout extends RefreshNestedLayout<RefreshListView> {

    private AutoAdapter mAutoAdapter;
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
        mAutoLoadResId = a.getResourceId(R.styleable.RefreshNestedLayout_autoLoadLayout, R.layout.default_footer_loading_layout);
        mClickableResId = a.getResourceId(R.styleable.RefreshNestedLayout_clickableLayout, R.layout.default_footer_clickable_layout);
        mLoadEndResId = a.getResourceId(R.styleable.RefreshNestedLayout_loadEndLayout, R.layout.default_footer_end_layout);
        mLoadErrorResId = a.getResourceId(R.styleable.RefreshNestedLayout_loadErrorLayout, R.layout.default_footer_error_layout);
    }

    protected RefreshListView createListView(Context context, AttributeSet attrs) {
        RefreshListView internalListView = null;
        View refreshableView = findViewById(R.id.refreshable_layout);
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
                if ((firstVisibleItem + visibleItemCount >= totalItemCount) &&
                        (mCurrentState == State.RESET || mCurrentState == State.AUTO_SCROLLING || mCurrentState == State.LOADING_ERROR ||
                                mCurrentState == State.LOADING_END || mCurrentState == State.SCROLL_TO_REFRESH)) {
                    if (mOnAutoLoadListener != null && getAutoLoadUsable()) {
                        if (isAutoLoad()) {
                            if (mCurrentState == State.LOADING_END) {
                                // do nothing...
                            } else if (mCurrentState == State.LOADING_ERROR) {
                                // do nothing...
                            } else {
                                setState(State.AUTO_LOADING);
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
        if (baseAdapter instanceof AutoAdapter) {
            mAutoAdapter = (AutoAdapter) baseAdapter;
            mAutoAdapter.setOnLastItemClickListener(mLastItemOnClickListener);
            mAutoAdapter.setAutoLoadResId(mAutoLoadResId);
            mAutoAdapter.setClickableResId(mClickableResId);
            mAutoAdapter.setLoadEndResId(mLoadEndResId);
            mAutoAdapter.setLoadErrorResId(mLoadErrorResId);
            mRefreshableView.setAdapter(mAutoAdapter);
        } else {
            throw new IllegalArgumentException("Adapter must be extends QuickAdapter or WrapAdapter");
        }
    }

    public boolean getAutoLoadUsable() {
        return mAutoAdapter.getAutoLoadUsable();
    }

    public void setAutoLoadUsable(boolean usable) {
        if (getMode() == Mode.AUTO_LOAD || getMode() == Mode.BOTH) {
            mAutoAdapter.setAutoLoadUsable(usable);
        }
    }

    public boolean isAutoLoad() {
        return mAutoAdapter.isAutoLoad();
    }

    public void setAutoLoad(boolean autoLoad) {
        mAutoAdapter.setAutoLoad(autoLoad);
    }

    public void setOnScrollListener(OnScrollListener listener) {
        // Don't set listener to super.setOnScrollListener().
        // listener receives all events through mScrollListener.
        mOriginalScrollListener = listener;
    }

    public void setOnAutoLoadListener(OnAutoLoadListener listener) {
        mOnAutoLoadListener = listener;
    }

    private boolean isFirstItemVisible() {
        final Adapter adapter = mRefreshableView.getAdapter();
        if (null == adapter || adapter.isEmpty()) {
            if (adapter instanceof HeaderViewListAdapter) {
                if (mRefreshableView.getFirstVisiblePosition() == 0) {
                    final View firstVisibleChild = mRefreshableView.getChildAt(0);
                    if (firstVisibleChild != null) {
                        return firstVisibleChild.getTop() >= mRefreshableView.getTop();
                    }
                }
            }
            return true;
        } else {
            if (mRefreshableView.getFirstVisiblePosition() == 0) {
                final View firstVisibleChild = mRefreshableView.getChildAt(0);
                if (firstVisibleChild != null) {
                    return firstVisibleChild.getTop() >= mRefreshableView.getTop();
                }
            }
        }
        return false;
    }

    public boolean isLastItemVisible() {
        final Adapter adapter = mRefreshableView.getAdapter();
        if (null == adapter || adapter.isEmpty()) {
            if (adapter instanceof HeaderViewListAdapter) {
                final int lastVisiblePosition = mRefreshableView.getLastVisiblePosition();
                final int count = mRefreshableView.getCount();
                if (lastVisiblePosition == count - 1) {
                    final int childIndex = lastVisiblePosition - mRefreshableView.getFirstVisiblePosition();
                    final View lastVisibleChild = mRefreshableView.getChildAt(childIndex);
                    if (lastVisibleChild != null) {
                        return lastVisibleChild.getBottom() <= mRefreshableView.getBottom();
                    }
                }
            }
            return true;
        } else {
            final int lastVisiblePosition = mRefreshableView.getLastVisiblePosition();
            final int count = mRefreshableView.getCount();
            if (lastVisiblePosition == count - 1) {
                final int childIndex = lastVisiblePosition - mRefreshableView.getFirstVisiblePosition();
                final View lastVisibleChild = mRefreshableView.getChildAt(childIndex);
                if (lastVisibleChild != null) {
                    return lastVisibleChild.getBottom() <= mRefreshableView.getBottom();
                }
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
        if (showEmpty && mAutoAdapter.isEmpty() && !mAutoAdapter.getAutoLoadUsable()) {
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
                mAutoAdapter.setLoadEnd(false);
                mAutoAdapter.setLoadError(false);
                mAutoAdapter.setLoading(false);
                mAutoAdapter.setAutoLoadUsable(false);
            } else {
                mAutoAdapter.setLoadEnd(true);
                mAutoAdapter.setLoadError(false);
                mAutoAdapter.setLoading(false);
                mAutoAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onAutoLoadingError() {
        super.onAutoLoadingError();
        mAutoAdapter.setLoadEnd(false);
        mAutoAdapter.setLoadError(true);
        mAutoAdapter.setLoading(false);
        mAutoAdapter.notifyDataSetChanged();
    }

    private void resetLastItemView() {
        if (mAutoAdapter.isLoadEnd() || mAutoAdapter.isLoadError()) {
            mAutoAdapter.setLoadEnd(false);
            mAutoAdapter.setLoadError(false);
            mAutoAdapter.setLoading(false);
            mAutoAdapter.notifyDataSetChanged();
        }
    }

    private View.OnClickListener mLastItemOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setState(State.AUTO_LOADING);
            if (!mAutoAdapter.isAutoLoad()) {
                mAutoAdapter.setLoadEnd(false);
                mAutoAdapter.setLoading(true);
                mAutoAdapter.setLoadError(false);
                mAutoAdapter.notifyDataSetChanged();
            } else {
                resetLastItemView();
            }
            mOnAutoLoadListener.onLoading();
        }
    };
}
