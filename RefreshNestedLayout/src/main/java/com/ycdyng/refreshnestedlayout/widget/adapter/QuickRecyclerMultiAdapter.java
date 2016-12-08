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

public abstract class QuickRecyclerMultiAdapter<T> extends BaseQuickRecyclerAdapter<T, RecyclerAdapterHelper> {

    public QuickRecyclerMultiAdapter(Context context) {
        super(context);
        setItemViewType();
    }

    public QuickRecyclerMultiAdapter(Context context, List<T> data) {
        super(context, data);
        setItemViewType();
    }

    protected abstract void setItemViewType();

}
