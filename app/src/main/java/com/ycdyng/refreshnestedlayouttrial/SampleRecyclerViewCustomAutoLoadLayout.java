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

public class SampleRecyclerViewCustomAutoLoadLayout extends AppCompatActivity {

    private RefreshNestedRecyclerViewLayout mRefresher;
    private SampleRecyclerAdapter mSimpleAdapter;
    private WrapRecyclerAdapter<SampleRecyclerAdapter> mQuickAdapter;
    private List<SampleModel> mDataList = new ArrayList<SampleModel>();
    private int mAutoLoadCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_recycler_view_auto_load); // set autoLoad mode
        mRefresher = (RefreshNestedRecyclerViewLayout) findViewById(R.id.refresh_layout);

        for(int i = 0; i < 20; i++) {
            SampleModel sampleMode = new SampleModel();
            sampleMode.setValues("RecyclerView item_"+i);
            mDataList.add(sampleMode);
        }

        mSimpleAdapter = new SampleRecyclerAdapter(this, mDataList);
        mQuickAdapter = new WrapRecyclerAdapter<SampleRecyclerAdapter>(mSimpleAdapter);
        mQuickAdapter.setAutoLoadViewResId(R.layout.loading_layout);

        mRefresher.setAdapter(mQuickAdapter);
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
