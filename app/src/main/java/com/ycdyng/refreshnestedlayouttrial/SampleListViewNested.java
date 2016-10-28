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
import android.support.v7.widget.Toolbar;

import com.ycdyng.refreshnestedlayout.RefreshNestedListViewLayout;
import com.ycdyng.refreshnestedlayout.widget.adapter.BaseAdapterHelper;
import com.ycdyng.refreshnestedlayout.widget.adapter.QuickAdapter;

import java.util.ArrayList;

public class SampleListViewNested extends Activity {

    private RefreshNestedListViewLayout mRefresher;

    private QuickAdapter<SampleModel> mQuickAdapter;
    private ArrayList<SampleModel> mDataList = new ArrayList<SampleModel>();
    private int mAutoLoadCount = 0;
    private int mRefreshCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_list_view_nested);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Title");

        mRefresher = (RefreshNestedListViewLayout) findViewById(R.id.refresh_layout);
        for(int i = 0; i < 40; i++) {
            SampleModel sampleMode = new SampleModel();
            sampleMode.setValues("ListView item_" + i);
            mDataList.add(sampleMode);
        }

        mQuickAdapter = new QuickAdapter<SampleModel>(this, R.layout.list_item) {

            @Override
            protected void convert(int position, BaseAdapterHelper helper, SampleModel item) {
                helper.setText(R.id.textView1, item.getValues());
            }
        };
        mQuickAdapter.addAll(mDataList);

        mRefresher.setAdapter(mQuickAdapter);
        mRefresher.setOnRefreshListener(new RefreshNestedListViewLayout.OnRefreshListener() {

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

                        mQuickAdapter.clear();
                        ArrayList<String> items = new ArrayList<String>();
                        for (int i = 0; i < 30; i++) {
                            SampleModel sampleMode = new SampleModel();
                            sampleMode.setValues("ListView add item_" + i + " by Pull-To-Refresh");
                            mQuickAdapter.add(sampleMode);
                        }
                        if (!mRefresher.getAutoLoadUsable()) {
                            mRefresher.setAutoLoadUsable(true); // able auto-load
                        } else {
                            mQuickAdapter.notifyDataSetChanged();
                        }
                        mRefresher.onRefreshComplete();

                    }
                }, 1500);
            }
        });
        mRefresher.setOnAutoLoadListener(new RefreshNestedListViewLayout.OnAutoLoadListener() {
            @Override
            public void onLoading() {
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
                            sampleMode.setValues("ListView add item_" + i + " by Auto-Load");
                            mQuickAdapter.add(sampleMode);
                        }
                        mQuickAdapter.notifyDataSetChanged();
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

}
