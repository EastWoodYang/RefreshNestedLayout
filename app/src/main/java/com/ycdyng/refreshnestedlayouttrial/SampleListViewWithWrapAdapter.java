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

import com.ycdyng.refreshnestedlayout.RefreshNestedListViewLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshNestedLayout;
import com.ycdyng.refreshnestedlayout.widget.adapter.WrapAdapter;

import java.util.ArrayList;

public class SampleListViewWithWrapAdapter extends AppCompatActivity {

    private RefreshNestedListViewLayout mRefresher;

    private SampleAdapter mSampleAdapter;
    private WrapAdapter<SampleAdapter> mWrapAdapter;
    private ArrayList<SampleModel> mDataList = new ArrayList<SampleModel>();
    private int mAutoLoadCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        for (int i = 0; i < 20; i++) {
            SampleModel sampleModel = new SampleModel();
            sampleModel.setValues("ListView item_" + i);
            mDataList.add(sampleModel);
        }

        mSampleAdapter = new SampleAdapter(this, mDataList);
        mWrapAdapter = new WrapAdapter<SampleAdapter>(mSampleAdapter);

        setContentView(R.layout.sample_list_view_auto_load); // set autoLoad mode
        mRefresher = (RefreshNestedListViewLayout) findViewById(R.id.refresh_layout);
        mRefresher.setAdapter(mWrapAdapter);
        mRefresher.setOnAutoLoadListener(new RefreshNestedLayout.OnAutoLoadListener() {
            @Override
            public void onLoading() {
                handleAutoLoadEvent();
            }
        });

        mRefresher.setAutoLoadUsable(true);
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
                mWrapAdapter.notifyDataSetChanged();
                if (mAutoLoadCount < 5) {
                    mAutoLoadCount++;
                    mRefresher.onAutoLoadingComplete(true);
                } else {
                    mRefresher.onAutoLoadingComplete(false);
                }
            }
        }, 1500);
    }

}
