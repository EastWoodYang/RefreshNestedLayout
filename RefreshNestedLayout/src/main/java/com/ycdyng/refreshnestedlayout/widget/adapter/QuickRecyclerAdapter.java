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

import java.util.List;

public abstract class QuickRecyclerAdapter<T> extends BaseQuickRecyclerAdapter<T, RecyclerAdapterHelper> {

    private static final int DEFAULT_VIEW_TYPE = -0xff;

    public QuickRecyclerAdapter(Context context, int layoutResId) {
        this(context, layoutResId, null);
    }

    public QuickRecyclerAdapter(Context context, int layoutResId, List<T> data) {
        super(context, data);
        addItemType(DEFAULT_VIEW_TYPE, layoutResId);
    }

    @Override
    protected int getItemType(int position) {
        return DEFAULT_VIEW_TYPE;
    }

}
