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

import com.ycdyng.refreshnestedlayout.RefreshNestedRecyclerViewLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshNestedLayout;
import com.ycdyng.refreshnestedlayout.widget.adapter.WrapRecyclerAdapter;

import java.util.ArrayList;
import java.util.List;

public class SampleRecyclerViewCustomRefreshHeader extends AppCompatActivity {

    private RefreshNestedRecyclerViewLayout mRefresher;
    private SampleRecyclerAdapter mSimpleAdapter;
    private WrapRecyclerAdapter<SampleRecyclerAdapter> mQuickAdapter;
    private List<SampleModel> mDataList = new ArrayList<SampleModel>();
    private int mRefreshCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_recycler_view_custom_header);  // add custom header
        mRefresher = (RefreshNestedRecyclerViewLayout) findViewById(R.id.refresh_layout);

        for(int i = 0; i < 28; i++) {
            SampleModel sampleMode = new SampleModel();
            sampleMode.setValues("RecyclerView item_"+i);
            mDataList.add(sampleMode);
        }

        mSimpleAdapter = new SampleRecyclerAdapter(this, mDataList);
        mQuickAdapter = new WrapRecyclerAdapter<SampleRecyclerAdapter>(mSimpleAdapter);

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
                            SampleModel sampleModel = new SampleModel();
                            sampleModel.setValues("RecyclerView add item_" + i + " by Pull-To-Refresh");
                            mDataList.add(sampleModel);
                        }
                        mQuickAdapter.notifyDataSetChanged();
                        mRefresher.onRefreshComplete();
                    }
                }, 1500);
            }
        });
    }
}
