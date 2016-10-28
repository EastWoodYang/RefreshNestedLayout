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

import com.ycdyng.refreshnestedlayout.R;

public abstract class AutoAdapter extends BaseAdapter {

    protected boolean autoLoadUsable;

    protected boolean autoLoad = true;

    protected boolean loadError;

    protected boolean loadEnd;

    protected boolean loading;

    protected int autoLoadViewResId;

    protected int clickableViewResId;

    protected int endViewResId;

    protected int errorViewResId;

    protected View.OnClickListener mOnLastItemClickListener;

    protected View createAutoLoadView(View convertView, ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        if (loadError) {
            if (errorViewResId != 0) {
                convertView = layoutInflater.inflate(errorViewResId, parent, false);
            } else {
                convertView = layoutInflater.inflate(R.layout.default_footer_error_layout, parent, false);
            }
            convertView.setOnClickListener(mOnLastItemClickListener);
        } else if (loadEnd) {
            if (endViewResId != 0) {
                convertView = layoutInflater.inflate(endViewResId, parent, false);
            } else {
                convertView = layoutInflater.inflate(R.layout.default_footer_end_layout, parent, false);
            }
        } else {
            if (autoLoad || loading) {
                if (autoLoadViewResId != 0) {
                    convertView = layoutInflater.inflate(autoLoadViewResId, parent, false);
                } else {
                    convertView = layoutInflater.inflate(R.layout.default_footer_loading_layout, parent, false);
                }
            } else {
                if (clickableViewResId != 0) {
                    convertView = layoutInflater.inflate(clickableViewResId, parent, false);
                } else {
                    convertView = layoutInflater.inflate(R.layout.default_footer_clickable_layout, parent, false);
                }
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

    public boolean isAutoLoad() {
        return autoLoad;
    }

    public void setAutoLoad(boolean autoLoad) {
        this.autoLoad = autoLoad;
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

    public void setAutoLoadViewResId(int resId) {
        this.autoLoadViewResId = resId;
    }

    public int getAutoLoadViewResId() {
        return autoLoadViewResId;
    }

    public void setClickableViewResId(int resId) {
        this.clickableViewResId = resId;
    }

    public int getClickableViewResId() {
        return clickableViewResId;
    }

    public int getEndViewResId() {
        return endViewResId;
    }

    public void setEndViewResId(int endViewResId) {
        this.endViewResId = endViewResId;
    }

    public int getErrorViewResId() {
        return errorViewResId;
    }

    public void setErrorViewResId(int errorViewResId) {
        this.errorViewResId = errorViewResId;
    }

    public void setOnLastItemClickListener(View.OnClickListener listener) {
        this.mOnLastItemClickListener = listener;
    }
}
