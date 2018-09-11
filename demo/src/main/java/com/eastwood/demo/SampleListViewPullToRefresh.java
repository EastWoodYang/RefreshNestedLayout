package com.eastwood.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.eastwood.common.adapter.QuickAdapter;
import com.eastwood.common.adapter.ViewHelper;
import com.eastwood.common.view.RefreshNestedLayout;

import java.util.ArrayList;

public class SampleListViewPullToRefresh extends AppCompatActivity {

    private RefreshNestedLayout mRefresher;
    private ListView mListView;

    private QuickAdapter<SampleModel> mQuickAdapter;
    private ArrayList<SampleModel> mDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataList = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            SampleModel sampleModel = new SampleModel();
            sampleModel.setValues("ListView item_" + i);
            mDataList.add(sampleModel);
        }
        mQuickAdapter = new QuickAdapter<SampleModel>(this, R.layout.list_item, mDataList) {

            @Override
            protected void convert(int position, ViewHelper helper, SampleModel item) {
                helper.setText(R.id.textView1, item.getValues());
            }
        };

        setContentView(R.layout.sample_list_view);
        mRefresher = findViewById(R.id.refresh_layout);
        mRefresher.setOnRefreshListener(new RefreshNestedLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                handleRefreshingEvent();
            }
        });

        mListView = findViewById(R.id.list_view);
        mListView.setAdapter(mQuickAdapter);
    }

    private void handleRefreshingEvent() {
        mRefresher.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDataList.clear();
                for (int i = 0; i < 30; i++) {
                    SampleModel sampleModel = new SampleModel();
                    sampleModel.setValues("ListView item_" + i + "  add by pull-to-refresh");
                    mDataList.add(sampleModel);
                }

                mQuickAdapter.notifyDataSetChanged();
                mRefresher.onRefreshComplete();
            }
        }, 5000);
    }

}
