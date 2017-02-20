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

package com.ycdyng.refreshnestedlayout.widget.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public abstract class AutoRecyclerAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    protected LayoutInflater mLayoutInflater;

    protected View.OnClickListener mOnLastItemClickListener;

    protected boolean autoLoadUsable;

    protected boolean manualLoad;
    protected boolean loadError;
    protected boolean loadEnd;
    protected boolean loading;

    protected boolean showNoMoreDataItem = true;

    protected int autoLoadResId;
    protected int clickableResId;
    protected int loadEndResId;
    protected int loadErrorResId;

    public static final int HEADER_VIEW_TYPE = -1000;
    public static final int FOOTER_VIEW_TYPE = -2000;

    public static final int AUTO_LOAD_VIEW_TYPE = -3001;
    public static final int CLICKABLE_VIEW_TYPE = -3002;
    public static final int ERROR_VIEW_TYPE = -3003;
    public static final int END_VIEW_TYPE = -3004;

    public final List<View> mHeaders = new ArrayList<View>();
    public final List<View> mFooters = new ArrayList<View>();

    protected abstract int getBodyCount();
    protected abstract int getBodyItemViewType(int position);
    protected abstract void onBodyBindViewHolder(VH viewHolder, int position);

    @Override
    public int getItemCount() {
        int extra = autoLoadUsable || (showNoMoreDataItem && loadEnd) ? 1 : 0;
        return mHeaders.size() + getBodyCount() + mFooters.size() + extra;
    }

    @Override
    public void onBindViewHolder(VH viewHolder, int position) {
        if (position < mHeaders.size()) {
            // Headers don't need anything special
        } else if (position < mHeaders.size() + getBodyCount()) {
            // This is a real position, not a header or footer. Bind it.
            onBodyBindViewHolder(viewHolder, position - mHeaders.size());
        } else {
            // Footers don't need anything special
            if ((autoLoadUsable || (showNoMoreDataItem && loadEnd)) && position + 1 == getItemCount()) {
                if (loadError) {
                    viewHolder.itemView.setOnClickListener(mOnLastItemClickListener);
                } else if (loadEnd) {
                    viewHolder.itemView.setOnClickListener(null);
                } else {
                    if (!manualLoad || loading) {
                        viewHolder.itemView.setOnClickListener(null);
                    } else {
                        viewHolder.itemView.setOnClickListener(mOnLastItemClickListener);
                    }
                }
            } else {
                // Footers don't need anything special
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < mHeaders.size()) {
            return HEADER_VIEW_TYPE + position;
        } else if (position < (mHeaders.size() + getBodyCount())) {
            return getBodyItemViewType(position - mHeaders.size());
        } else {
            if ((autoLoadUsable || (showNoMoreDataItem && loadEnd)) && position + 1 == getItemCount()) {
                if (loadError) {
                    return ERROR_VIEW_TYPE;
                } else if (loadEnd) {
                    return END_VIEW_TYPE;
                } else {
                    if (!manualLoad || loading) {
                        return AUTO_LOAD_VIEW_TYPE;
                    } else {
                        return CLICKABLE_VIEW_TYPE;
                    }
                }
            } else {
                return FOOTER_VIEW_TYPE + position - mHeaders.size() - getBodyCount();
            }
        }
    }

    /**
     * Adds a header view.
     */
    public void addHeader(View view) {
        if (view == null) {
            throw new IllegalArgumentException("You can't have a null header!");
        }
        mHeaders.add(view);
    }

    /**
     * Adds a footer view.
     */
    public void addFooter(View view) {
        if (view == null) {
            throw new IllegalArgumentException("You can't have a null footer!");
        }
        mFooters.add(view);
    }

    /**
     * Toggles the visibility of the header views.
     */
    public void setHeaderVisibility(boolean shouldShow) {
        for (View header : mHeaders) {
            header.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Toggles the visibility of the footer views.
     */
    public void setFooterVisibility(boolean shouldShow) {
        for (View footer : mFooters) {
            footer.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * @return the number of headers.
     */
    public int getHeaderCount() {
        return mHeaders.size();
    }

    /**
     * @return the number of footers.
     */
    public int getFooterCount() {
        return mFooters.size();
    }

    /**
     * Gets the indicated header, or null if it doesn't exist.
     */
    public View getHeader(int i) {
        return i < mHeaders.size() ? mHeaders.get(i) : null;
    }

    /**
     * Gets the indicated footer, or null if it doesn't exist.
     */
    public View getFooter(int i) {
        return i < mFooters.size() ? mFooters.get(i) : null;
    }

    protected boolean isHeader(int viewType) {
        return viewType >= HEADER_VIEW_TYPE && viewType < (HEADER_VIEW_TYPE + mHeaders.size());
    }

    protected boolean isFooter(int viewType) {
        return viewType >= FOOTER_VIEW_TYPE && viewType < (FOOTER_VIEW_TYPE + mFooters.size());
    }

    protected boolean isAutoLoadView(int viewType) {
        return viewType == AUTO_LOAD_VIEW_TYPE || viewType == CLICKABLE_VIEW_TYPE || viewType == ERROR_VIEW_TYPE || viewType == END_VIEW_TYPE;
    }

    protected View getItemView(int layoutResId, ViewGroup parent) {
        if(mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(parent.getContext());
        }
        return mLayoutInflater.inflate(layoutResId, parent, false);
    }

    public int getLastItemViewType() {
        int count = getItemCount();
        if(count == 0) {
            return END_VIEW_TYPE;
        } else {
            return getItemViewType(count - 1);
        }
    }

    public void setAutoLoadUsable(boolean display) {
        if (display == autoLoadUsable) return;
        autoLoadUsable = display;
        notifyDataSetChanged();
    }

    public boolean getAutoLoadUsable() {
        return autoLoadUsable;
    }

    public boolean isManualLoad() {
        return manualLoad;
    }

    public void setManualLoad(boolean manualLoad) {
        this.manualLoad = manualLoad;
    }

    public boolean isLoadError() {
        return loadError;
    }

    public void setLoadError(boolean loadError) {
        this.loadError = loadError;
    }

    public boolean isLoadEnd() {
        return loadEnd;
    }

    public void setLoadEnd(boolean loadEnd) {
        this.loadEnd = loadEnd;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public boolean isShowNoMoreDataItem() {
        return showNoMoreDataItem;
    }

    public void setShowNoMoreDataItem(boolean showNoMoreDataItem) {
        this.showNoMoreDataItem = showNoMoreDataItem;
    }

    public void setAutoLoadResId(int resId) {
        this.autoLoadResId = resId;
    }

    public int getAutoLoadResId() {
        return autoLoadResId;
    }

    public void setClickableResId(int resId) {
        this.clickableResId = resId;
    }

    public int getClickableResId() {
        return clickableResId;
    }

    public int getLoadEndResId() {
        return loadEndResId;
    }

    public void setLoadEndResId(int loadEndResId) {
        this.loadEndResId = loadEndResId;
    }

    public int getLoadErrorResId() {
        return loadErrorResId;
    }

    public void setLoadErrorResId(int loadErrorResId) {
        this.loadErrorResId = loadErrorResId;
    }

    public void setOnLastItemClickListener(View.OnClickListener listener) {
        this.mOnLastItemClickListener = listener;
    }

}
