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
import com.ycdyng.refreshnestedlayout.widget.adapter.BaseAdapterHelper;
import com.ycdyng.refreshnestedlayout.widget.adapter.QuickAdapter;

import java.util.ArrayList;

public class SampleListViewLoading extends AppCompatActivity {

    private RefreshNestedListViewLayout mRefresher;

    private QuickAdapter<SampleModel> mQuickAdapter;
    private ArrayList<SampleModel> mDataList = new ArrayList<SampleModel>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mQuickAdapter = new QuickAdapter<SampleModel>(this, R.layout.list_item, mDataList) {

            @Override
            protected void convert(int position, BaseAdapterHelper helper, SampleModel item) {
                helper.setText(R.id.textView1, item.getValues());
            }
        };

        setContentView(R.layout.sample_list_view);
        mRefresher = (RefreshNestedListViewLayout) findViewById(R.id.refresh_layout);
        mRefresher.setAdapter(mQuickAdapter);

        mRefresher.onLoadingDataStart();
        mRefresher.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 20; i++) {
                    SampleModel sampleModel = new SampleModel();
                    sampleModel.setValues("ListView item_" + i);
                    mDataList.add(sampleModel);
                }
                mQuickAdapter.notifyDataSetChanged();

                // if DataSet empty, will show empty layout.
                // if you set custom empty layout, will show it.

                mRefresher.onLoadingDataComplete(false);
            }
        }, 3000);
    }

}
