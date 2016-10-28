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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.ycdyng.refreshnestedlayout.RefreshNestedRecyclerViewLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshNestedLayout;
import com.ycdyng.refreshnestedlayout.widget.adapter.WrapRecyclerAdapter;

import java.util.ArrayList;
import java.util.List;

public class SampleRecyclerViewNested extends Activity implements View.OnTouchListener {

    private CoordinatorLayout mCoordinatorLayout;
    private AppBarLayout mAppBarLayout;
    private RefreshNestedRecyclerViewLayout mRefresher;
    private SampleRecyclerAdapter mSimpleAdapter;
    private WrapRecyclerAdapter<SampleRecyclerAdapter> mWrapRecyclerAdapter;
    private List<SampleModel> mDataList = new ArrayList<SampleModel>();
    private int mAutoLoadCount;
    private int mRefreshCount;

    private int mVerticalOffset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_recycler_view_nested);
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
        for(int i = 0; i < 30; i++) {
            SampleModel sampleMode = new SampleModel();
            sampleMode.setValues("RecyclerView item_"+i);
            mDataList.add(sampleMode);
        }

        mSimpleAdapter = new SampleRecyclerAdapter(this, mDataList);
        mWrapRecyclerAdapter = new WrapRecyclerAdapter<SampleRecyclerAdapter>(mSimpleAdapter);

        mRefresher.setAdapter(mWrapRecyclerAdapter);
        mRefresher.setOnRefreshListener(new RefreshNestedLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                mRefresher.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(mRefreshCount >= 5) {
                            mRefresher.setRefreshUsable(false); // disable pull-to-refresh
                            mRefresher.onRefreshComplete();
                            return;
                        }
                        mRefreshCount++;

                        mDataList.clear();
                        ArrayList<String> items = new ArrayList<String>();
                        for (int i = 0; i < 30; i++) {
                            SampleModel sampleMode = new SampleModel();
                            sampleMode.setValues("RecyclerView add item_" + i + " by Pull-To-Refresh");
                            mDataList.add(sampleMode);
                        }
                        if (!mRefresher.getAutoLoadUsable()) {
                            mRefresher.setAutoLoadUsable(true);
                        } else {
                            mWrapRecyclerAdapter.notifyDataSetChanged();
                        }
                        mRefresher.onRefreshComplete();
                    }
                }, 3000);
            }
        });
        mRefresher.setOnAutoLoadListener(new RefreshNestedLayout.OnAutoLoadListener() {
            @Override
            public void onLoading() {
                Log.d("OnAutoLoadListener", "onLoading");
                mRefresher.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(mAutoLoadCount % 3 == 0) {
                            mAutoLoadCount++;
                            mRefresher.onAutoLoadingError();
                            return;
                        }
                        for (int i = 0; i < 20; i++) {
                            SampleModel sampleMode = new SampleModel();
                            sampleMode.setValues("RecyclerView add item_" + i + " by Auto-Load");
                            mDataList.add(sampleMode);
                        }
                        mWrapRecyclerAdapter.notifyDataSetChanged();
                        if (mAutoLoadCount < 2) {
                            mAutoLoadCount++;
                            mRefresher.onAutoLoadingComplete(true);
                        } else {
                            mRefresher.onAutoLoadingComplete(false);
                        }
                    }
                }, 1500);
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
}
