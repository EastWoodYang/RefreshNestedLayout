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
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.View;

import com.ycdyng.refreshnestedlayout.custom.DefaultHeaderLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshHeaderLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshNestedLayout;

public class RefreshNestedScrollViewLayout extends RefreshNestedLayout<NestedScrollView> {

    public RefreshNestedScrollViewLayout(Context context) {
        this(context, null);
    }

    public RefreshNestedScrollViewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMode(Mode.PULL_TO_REFRESH);
    }

    @Override
    protected NestedScrollView createRefreshableView(Context context, AttributeSet attrs) {
        return createScrollView(context, attrs);
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
                throw new IllegalArgumentException("Header View must be extended RefreshHeaderLayout");
            }
        } else {
            refreshHeaderLayout = new DefaultHeaderLayout(context);
            refreshHeaderLayout.setHeight(0);  // hidden
        }
        return refreshHeaderLayout;
    }

    protected NestedScrollView createScrollView(Context context, AttributeSet attrs) {
        NestedScrollView internalScrollView = null;
        View refreshableView = findViewById(R.id.refreshable_view);
        if (refreshableView == null) {
            internalScrollView = new NestedScrollView(context, attrs);
        } else {
            if (refreshableView instanceof NestedScrollView) {
                internalScrollView = (NestedScrollView) refreshableView;
            } else {
                throw new IllegalArgumentException("Refreshable View must be extends NestedScrollView");
            }
        }
        return internalScrollView;
    }

    @Override
    protected void checkBody() {

    }

    public void onRefreshComplete() {
        onRefreshComplete(true);
    }

    public void onLoadingComplete() {
        onLoadingComplete(false);
    }

    @Override
    public void onLoadingComplete(boolean loadable) {
        crossFading(mRefreshableView, mLoadingLayout);
    }
}
