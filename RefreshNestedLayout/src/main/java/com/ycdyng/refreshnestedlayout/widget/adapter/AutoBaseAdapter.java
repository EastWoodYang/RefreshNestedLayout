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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class AutoBaseAdapter extends BaseAdapter {

    protected boolean autoLoadUsable;

    protected boolean manualLoad;
    protected boolean loadError;
    protected boolean loadEnd;
    protected boolean loading;

    protected boolean showLoadEnd;

    protected int autoLoadResId;
    protected int clickableResId;
    protected int loadEndResId;
    protected int loadErrorResId;

    protected View.OnClickListener mOnLastItemClickListener;

    public int getLastItemViewType() {
        int count = getCount();
        if(count == 0) {
            return 1;
        } else {
            return getItemViewType(count - 1);
        }
    }

    protected View createAutoLoadView(View convertView, ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        if (loadError) {
            convertView = layoutInflater.inflate(loadErrorResId, parent, false);
            convertView.setOnClickListener(mOnLastItemClickListener);
        } else if (loadEnd) {
            convertView = layoutInflater.inflate(loadEndResId, parent, false);
        } else {
            if (!manualLoad || loading) {
                convertView = layoutInflater.inflate(autoLoadResId, parent, false);
            } else {
                convertView = layoutInflater.inflate(clickableResId, parent, false);
                convertView.setOnClickListener(mOnLastItemClickListener);
            }
        }
        return convertView;
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

    public boolean isShowLoadEnd() {
        return showLoadEnd;
    }

    public void setShowLoadEnd(boolean showLoadEnd) {
        this.showLoadEnd = showLoadEnd;
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
