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
import com.ycdyng.refreshnestedlayout.widget.adapter.QuickRecyclerAdapter;
import com.ycdyng.refreshnestedlayout.widget.adapter.RecyclerAdapterHelper;

import java.util.ArrayList;
import java.util.List;

public class SampleRecyclerViewWithHeaderViewAndFooterView extends AppCompatActivity {

    private RefreshNestedRecyclerViewLayout mRefresher;
    private QuickRecyclerAdapter<SampleModel> mQuickRecyclerAdapter;
    private List<SampleModel> mDataList = new ArrayList<>();

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

        View headerView = LayoutInflater.from(this).inflate(R.layout.item_recycler_view_header, null);
        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "onClick", Toast.LENGTH_SHORT).show();
            }
        });
        mQuickRecyclerAdapter.addHeader(headerView);

        View footerView = LayoutInflater.from(this).inflate(R.layout.item_recycler_view_footer, null);
        footerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "onClick", Toast.LENGTH_SHORT).show();
            }
        });
        mQuickRecyclerAdapter.addFooter(footerView);

        setContentView(R.layout.sample_recycler_view);
        mRefresher = (RefreshNestedRecyclerViewLayout) findViewById(R.id.refresh_layout);
        mRefresher.setAdapter(mQuickRecyclerAdapter);
    }

}
