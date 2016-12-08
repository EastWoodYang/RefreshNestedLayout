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
import android.view.View;
import android.view.ViewGroup;

public class WrapRecyclerAdapter<T extends RecyclerView.Adapter> extends AutoRecyclerAdapter<RecyclerView.ViewHolder> {

    private final T mBase;

    public WrapRecyclerAdapter(T base) {
        super();
        mBase = base;
    }

    public T getWrappedAdapter() {
        return mBase;
    }

    @Override
    protected int getBodyCount() {
        return mBase.getItemCount();
    }

    @Override
    protected int getBodyItemViewType(int position) {
        return mBase.getItemViewType(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (isHeader(viewType)) {
            int position = Math.abs(viewType - HEADER_VIEW_TYPE);
            return createBaseViewHolder(mHeaders.get(position));
        } else if (isFooter(viewType)) {
            int position = Math.abs(viewType - FOOTER_VIEW_TYPE);
            return createBaseViewHolder(mFooters.get(position));
        } else if (isAutoLoadView(viewType)) {
            return createAutoLoadViewHolder(viewGroup);
        } else {
            return mBase.onCreateViewHolder(viewGroup, viewType);
        }
    }

    protected RecyclerView.ViewHolder createBaseViewHolder(View view) {
        return new RecyclerView.ViewHolder(view) {
        };
    }

    @Override
    protected void onBodyBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        mBase.onBindViewHolder(viewHolder, position);
    }

    private RecyclerView.ViewHolder createAutoLoadViewHolder(ViewGroup viewGroup) {
        View autoLoadView = null;
        if (loadError) {
            autoLoadView = getItemView(loadErrorResId, viewGroup);
            autoLoadView.setOnClickListener(mOnLastItemClickListener);
        } else if (loadEnd) {
            autoLoadView = getItemView(loadEndResId, viewGroup);
        } else {
            if (!manualLoad || loading) {
                autoLoadView = getItemView(autoLoadResId, viewGroup);
            } else {
                autoLoadView = getItemView(clickableResId, viewGroup);
                autoLoadView.setOnClickListener(mOnLastItemClickListener);
            }
        }
        return createBaseViewHolder(autoLoadView);
    }

    // NotifyItemChanged

    public final void onNotifyItemChanged(int position) {
        notifyItemChanged(position + getHeaderCount());
    }

    public final void onNotifyItemChanged(int position, Object payload) {
        notifyItemChanged(position + getHeaderCount(), payload);
    }

    public final void onNotifyItemRangeChanged(int positionStart, int itemCount) {
        notifyItemRangeChanged(positionStart + getHeaderCount(), itemCount);
    }

    public final void onNotifyItemRangeChanged(int positionStart, int itemCount, Object payload) {
        notifyItemRangeChanged(positionStart + getHeaderCount(), itemCount, payload);
    }

    public final void onNotifyItemRemoved(int position) {
        notifyItemRemoved(position + getHeaderCount());
    }

    public final void onNotifyItemMoved(int fromPosition, int toPosition) {
        notifyItemMoved(fromPosition + getHeaderCount(), toPosition + getHeaderCount());
    }

    public final void onNotifyItemRangeRemoved(int positionStart, int itemCount) {
        notifyItemRangeRemoved(positionStart + getHeaderCount(), itemCount);
    }

    public final void onNotifyItemInserted(int position) {
        notifyItemInserted(position + getHeaderCount());
    }

    public final void onNotifyItemRangeInserted(int positionStart, int itemCount) {
        notifyItemRangeInserted(positionStart + getHeaderCount(), itemCount);
    }

    public final void onNotifyDataSetChanged() {
        notifyDataSetChanged();
    }

}