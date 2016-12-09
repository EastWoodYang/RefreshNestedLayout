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
import com.ycdyng.refreshnestedlayout.widget.adapter.QuickRecyclerMultiAdapter;
import com.ycdyng.refreshnestedlayout.widget.adapter.RecyclerAdapterHelper;

import java.util.ArrayList;
import java.util.List;

public class SampleRecyclerViewWithQuickRecyclerMultiAdapter extends AppCompatActivity {

    private RefreshNestedRecyclerViewLayout mRefresher;
    private QuickRecyclerMultiAdapter<SampleModel> mQuickRecyclerMultiAdapter;
    private List<SampleModel> mDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        for (int i = 0; i < 20; i++) {
            SampleModel sampleModel = new SampleModel();
            sampleModel.setValues("ListView item_" + i);
            mDataList.add(sampleModel);
        }

        mQuickRecyclerMultiAdapter = new QuickRecyclerMultiAdapter<SampleModel>(this, mDataList) {

            @Override
            protected void setItemViewType() {
                addItemType(1, R.layout.list_item);
                addItemType(2, R.layout.list_item_2);
            }

            @Override
            protected int getItemType(int position) {
                if (position % 2 == 0) {
                    return 1;
                } else {
                    return 2;
                }
            }

            @Override
            protected void convert(int position, RecyclerAdapterHelper helper, SampleModel item) {
                helper.setText(R.id.textView1, item.getValues());
            }
        };

        setContentView(R.layout.sample_recycler_view);
        mRefresher = (RefreshNestedRecyclerViewLayout) findViewById(R.id.refresh_layout);
        mRefresher.setAdapter(mQuickRecyclerMultiAdapter);
    }
}
