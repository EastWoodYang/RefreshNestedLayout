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

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class WrapAdapter<T extends BaseAdapter> extends AutoBaseAdapter {

    private final T mBase;

    /**
     * Constructor.
     *
     * @param base the adapter to wrap
     */
    public WrapAdapter(T base) {
        super();
        mBase = base;
    }

    /**
     * Gets the base adapter that this is wrapping.
     */
    public T getWrappedAdapter() {
        return mBase;
    }

    @Override
    public int getCount() {
        int extra = autoLoadUsable || (showNoMoreDataItem && loadEnd) ? 1 : 0;
        return mBase.getCount() + extra;
    }

    @Override
    public Object getItem(int position) {
        if (getItemViewType(position) == 0) {
            return mBase.getItem(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == 0) {
            if (convertView != null && convertView.getTag() == null) {
                convertView = null;
            }
            return mBase.getView(position, convertView, parent);
        }
        return createAutoLoadView(convertView, parent);
    }

    @Override
    public int getItemViewType(int position) {
        return position >= mBase.getCount() ? 1 : 0;
    }
}
