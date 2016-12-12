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
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.ycdyng.refreshnestedlayout.RefreshNestedRecyclerViewLayout;
import com.ycdyng.refreshnestedlayout.kernel.RefreshRecyclerView;
import com.ycdyng.refreshnestedlayout.widget.adapter.QuickRecyclerAdapter;
import com.ycdyng.refreshnestedlayout.widget.adapter.RecyclerAdapterHelper;

import java.util.ArrayList;
import java.util.List;

public class SampleRecyclerViewItemListener extends AppCompatActivity {

    private RefreshNestedRecyclerViewLayout mRefresher;
    private QuickRecyclerAdapter<SampleModel> mQuickRecyclerAdapter;
    private List<SampleModel> mDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        for (int i = 0; i < 28; i++) {
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

        setContentView(R.layout.sample_recycler_view);
        mRefresher = (RefreshNestedRecyclerViewLayout) findViewById(R.id.refresh_layout);
        mRefresher.setAdapter(mQuickRecyclerAdapter);

        mRefresher.getRefreshableView().setOnItemLongClickListener(new RefreshRecyclerView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, int position, View v) {
                Toast.makeText(getApplicationContext(), "onItemLongClicked position: " + position, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        mRefresher.getRefreshableView().setOnItemClickListener(new RefreshRecyclerView.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                Toast.makeText(getApplicationContext(), "onItemClicked position: " + position, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
