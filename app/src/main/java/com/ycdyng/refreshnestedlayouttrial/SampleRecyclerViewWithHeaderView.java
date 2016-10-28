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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.ycdyng.refreshnestedlayout.RefreshNestedRecyclerViewLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshNestedLayout;
import com.ycdyng.refreshnestedlayout.kernel.BaseRecyclerHeaderFooterView;
import com.ycdyng.refreshnestedlayout.widget.adapter.WrapRecyclerAdapter;

import java.util.ArrayList;
import java.util.List;

public class SampleRecyclerViewWithHeaderView extends AppCompatActivity {

    private RefreshNestedRecyclerViewLayout mRefresher;
    private SampleRecyclerAdapter mSimpleAdapter;
    private WrapRecyclerAdapter<SampleRecyclerAdapter> mQuickAdapter;
    private List<SampleModel> mDataList = new ArrayList<SampleModel>();
    private int mRefreshCount;
    private int mAutoLoadCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_recycler_view_default);  // set pullToRefresh mode
        mRefresher = (RefreshNestedRecyclerViewLayout) findViewById(R.id.refresh_layout);

        for(int i = 0; i < 28; i++) {
            SampleModel sampleMode = new SampleModel();
            sampleMode.setValues("RecyclerView item_"+i);
            mDataList.add(sampleMode);
        }

        mSimpleAdapter = new SampleRecyclerAdapter(this, mDataList);
        mQuickAdapter = new WrapRecyclerAdapter<SampleRecyclerAdapter>(mSimpleAdapter);

        BaseRecyclerHeaderFooterView view = (BaseRecyclerHeaderFooterView) LayoutInflater.from(this).inflate(R.layout.sample_recycler_view_header, null);
        view.setOnClickListener(new BaseRecyclerHeaderFooterView.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "onClick", Toast.LENGTH_SHORT).show();
            }
        });
        mQuickAdapter.addHeader(view);

        mRefresher.setAdapter(mQuickAdapter);
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
                            mQuickAdapter.notifyDataSetChanged();
                        }
                        mRefresher.onRefreshComplete();
                    }
                }, 3000);
            }
        });
        mRefresher.setOnAutoLoadListener(new RefreshNestedLayout.OnAutoLoadListener() {
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
                            sampleMode.setValues("RecyclerView add item_" + i + " by Auto-Load");
                            mDataList.add(sampleMode);
                        }
                        mQuickAdapter.onNotifyItemRangeChanged(mDataList.size() - 20, 20);
                        if (mAutoLoadCount < 2) {
                            mAutoLoadCount++;
                            mRefresher.onAutoLoadingComplete(true);
                        } else {
                            mRefresher.onAutoLoadingComplete(false);
                        }
                    }
                }, 2000);
            }
        });
        mRefresher.setAutoLoadUsable(true);
    }
}
