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

import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseQuickRecyclerAdapter<T, VH extends RecyclerAdapterHelper> extends AutoRecyclerAdapter<VH> {

    protected final Context context;

    private SparseArray<Integer> layoutResIds = new SparseArray<>();

    protected final List<T> data;

    BaseQuickRecyclerAdapter(Context context) {
        this(context, null);
    }

    BaseQuickRecyclerAdapter(Context context, List<T> data) {
        this.data = data == null ? new ArrayList<T>() : data;
        this.context = context;
    }

    @Override
    protected int getBodyCount() {
        return data.size();
    }

    @Override
    protected int getBodyItemViewType(int position) {
        return getItemType(position);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        if (isHeader(viewType)) {
            int position = Math.abs(viewType - HEADER_VIEW_TYPE);
            view = mHeaders.get(position);
        } else if (isFooter(viewType)) {
            int position = Math.abs(viewType - FOOTER_VIEW_TYPE);
            view = mFooters.get(position);
        } else if (isAutoLoadView(viewType)) {
            view = createAutoLoadView(parent);
        } else {
            view = getItemView(getLayoutId(viewType), parent);
        }
        return createViewHolder(view);
    }

    @Override
    protected void onBodyBindViewHolder(VH viewHolder, int position) {
        convert(position, viewHolder, data.get(position));
    }

    private View createAutoLoadView(ViewGroup parent) {
        View autoLoadView = null;
        if (loadError) {
            autoLoadView = getItemView(loadErrorResId, parent);
            autoLoadView.setOnClickListener(mOnLastItemClickListener);
        } else if (loadEnd) {
            autoLoadView = getItemView(loadEndResId, parent);
        } else {
            if (!manualLoad || loading) {
                autoLoadView = getItemView(autoLoadResId, parent);
            } else {
                autoLoadView = getItemView(clickableResId, parent);
                autoLoadView.setOnClickListener(mOnLastItemClickListener);
            }
        }
        return autoLoadView;
    }

    private int getLayoutId(int viewType) {
        return layoutResIds.get(viewType);
    }

    public void addItemType(int type, int layoutResId) {
        if (layoutResIds == null) {
            layoutResIds = new SparseArray<>();
        }
        layoutResIds.put(type, layoutResId);
    }

    @SuppressWarnings("unchecked")
    private VH createViewHolder(View view) {
        return (VH) new RecyclerAdapterHelper(context, view);
    }

    public void add(T elem) {
        data.add(elem);
        notifyDataSetChanged();
    }

    public void addAll(List<T> elem) {
        data.addAll(elem);
        notifyDataSetChanged();
    }

    public void set(T oldElem, T newElem) {
        set(data.indexOf(oldElem), newElem);
    }

    public void set(int index, T elem) {
        data.set(index, elem);
        notifyDataSetChanged();
    }

    public void remove(T elem) {
        data.remove(elem);
        notifyDataSetChanged();
    }

    public void remove(int index) {
        data.remove(index);
        notifyDataSetChanged();
    }

    public void replaceAll(List<T> elem) {
        data.clear();
        data.addAll(elem);
        notifyDataSetChanged();
    }

    public boolean contains(T elem) {
        return data.contains(elem);
    }

    /** Clear data list */
    public void clear() {
        data.clear();
        notifyDataSetChanged();
    }

    /**
     * Implement this method and use the helper to adapt the view to the given item.
     * @param helper A fully initialized helper.
     * @param item   The item that needs to be displayed.
     */
    protected abstract void convert(int position, VH helper, T item);

    protected abstract int getItemType(int position);

}
