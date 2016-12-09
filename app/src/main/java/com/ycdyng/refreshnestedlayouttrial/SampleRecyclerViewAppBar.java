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

package com.ycdyng.refreshnestedlayouttrial;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;

import com.ycdyng.refreshnestedlayout.RefreshNestedRecyclerViewLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshNestedLayout;
import com.ycdyng.refreshnestedlayout.widget.adapter.QuickRecyclerAdapter;
import com.ycdyng.refreshnestedlayout.widget.adapter.RecyclerAdapterHelper;

import java.util.ArrayList;
import java.util.List;

public class SampleRecyclerViewAppBar extends Activity implements View.OnTouchListener {

    private CoordinatorLayout mCoordinatorLayout;
    private AppBarLayout mAppBarLayout;
    private RefreshNestedRecyclerViewLayout mRefresher;
    private QuickRecyclerAdapter<SampleModel> mQuickRecyclerAdapter;
    private List<SampleModel> mDataList = new ArrayList<>();
    private int mAutoLoadCount;

    private int mVerticalOffset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        for (int i = 0; i < 20; i++) {
            SampleModel sampleMode = new SampleModel();
            sampleMode.setValues("RecyclerView item_" + i);
            mDataList.add(sampleMode);
        }

        mQuickRecyclerAdapter = new QuickRecyclerAdapter<SampleModel>(this, R.layout.list_item, mDataList) {
            @Override
            protected void convert(int position, RecyclerAdapterHelper helper, SampleModel item) {
                helper.setText(R.id.textView1, item.getValues());
            }
        };

        setContentView(R.layout.sample_recycler_view_app_bar);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mCoordinatorLayout.setOnTouchListener(this);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                mVerticalOffset = Math.abs(verticalOffset);
            }
        });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Title");

        mRefresher = (RefreshNestedRecyclerViewLayout) findViewById(R.id.refresh_layout);
        mRefresher.setOnTouchListener(this);
        mRefresher.getRefreshableView().setOnTouchListener(this);
        mRefresher.setAdapter(mQuickRecyclerAdapter);
        mRefresher.setOnRefreshListener(new RefreshNestedLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                handleRefreshingEvent();
            }
        });
        mRefresher.setOnAutoLoadListener(new RefreshNestedLayout.OnAutoLoadListener() {
            @Override
            public void onLoading() {
                handleAutoLoadEvent();
            }
        });
        mRefresher.setAutoLoadUsable(true);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if(mVerticalOffset != 0) {
                    if(mVerticalOffset > 100) {
                        mAppBarLayout.setExpanded(false);
                    } else {
                        mAppBarLayout.setExpanded(true);
                    }
                }
                break;
        }
        return false;
    }

    private void handleRefreshingEvent() {
        mRefresher.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDataList.clear();
                for (int i = 0; i < 20; i++) {
                    SampleModel sampleModel = new SampleModel();
                    sampleModel.setValues("ListView item_" + i + "  add by pull-to-refresh");
                    mDataList.add(sampleModel);
                }

                mQuickRecyclerAdapter.notifyDataSetChanged();
                mRefresher.onRefreshComplete(false);
            }
        }, 1500);
    }

    private void handleAutoLoadEvent() {
        mRefresher.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 20; i++) {
                    SampleModel sampleMode = new SampleModel();
                    sampleMode.setValues("ListView add item_" + (20 * (mAutoLoadCount + 1) + i) + " by Auto-Load");
                    mDataList.add(sampleMode);
                }
                mQuickRecyclerAdapter.notifyDataSetChanged();

                mAutoLoadCount++;
                mRefresher.onAutoLoadingComplete(true);
            }
        }, 1500);
    }
}
