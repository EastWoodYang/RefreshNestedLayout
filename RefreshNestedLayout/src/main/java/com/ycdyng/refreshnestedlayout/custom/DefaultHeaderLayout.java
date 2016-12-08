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

package com.ycdyng.refreshnestedlayout.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.ycdyng.refreshnestedlayout.R;
import com.ycdyng.refreshnestedlayout.kernel.RefreshHeaderLayout;

public class DefaultHeaderLayout extends RefreshHeaderLayout {

    private TextView mTipTextView;

    public DefaultHeaderLayout(Context context) {
        this(context, null);
    }

    public DefaultHeaderLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.default_header_layout, this);
        mTipTextView = (TextView) findViewById(R.id.tip_text_view);
    }

    @Override
    public void onPull(float scrollValue) {

    }

    @Override
    public void alreadyToRefresh(boolean alreadyToRefresh) {
        if(alreadyToRefresh) {
            mTipTextView.setText(R.string.release_to_refresh);
        } else {
            mTipTextView.setText(R.string.pull_to_refresh);
        }
    }

    @Override
    public void onRefreshBegin() {
        mTipTextView.setText(R.string.refreshing);
    }

    @Override
    public void onRefreshFinish() {
        mTipTextView.setText("");
    }

    @Override
    public void onRefreshCancel() {

    }
}