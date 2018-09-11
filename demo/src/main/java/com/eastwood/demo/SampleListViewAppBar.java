package com.eastwood.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

import com.eastwood.common.adapter.QuickAdapter;
import com.eastwood.common.adapter.ViewHelper;
import com.eastwood.common.view.RefreshNestedLayout;

import java.util.ArrayList;

public class SampleListViewAppBar extends Activity implements View.OnTouchListener {

    private CoordinatorLayout mCoordinatorLayout;
    private AppBarLayout mAppBarLayout;
    private RefreshNestedLayout mRefresher;

    private ListView mListView;

    private QuickAdapter<SampleModel> mQuickAdapter;
    private ArrayList<SampleModel> mDataList;
    private int mVerticalOffset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            SampleModel sampleMode = new SampleModel();
            sampleMode.setValues("ListView item_" + i);
            mDataList.add(sampleMode);
        }
        mQuickAdapter = new QuickAdapter<SampleModel>(this, R.layout.list_item, mDataList) {

            @Override
            protected void convert(int position, ViewHelper helper, SampleModel item) {
                helper.setText(R.id.textView1, item.getValues());
            }
        };

        setContentView(R.layout.sample_list_view_app_bar);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mCoordinatorLayout.setOnTouchListener(this);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                mVerticalOffset = Math.abs(verticalOffset);
            }
        });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Title");

        mRefresher = (RefreshNestedLayout) findViewById(R.id.refresh_layout);
        mRefresher.setOnTouchListener(this);
        mRefresher.getRefreshableView().setOnTouchListener(this);
        mRefresher.setOnRefreshListener(new RefreshNestedLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                handleRefreshingEvent();
            }
        });

        mListView = findViewById(R.id.list_view);
        mListView.setAdapter(mQuickAdapter);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mVerticalOffset != 0) {
                    if (mVerticalOffset > 100) {
                        mAppBarLayout.setExpanded(false);
                    } else {
                        mAppBarLayout.setExpanded(true);
                    }
                }
                break;
        }
        return false;
    }

    private void handleRefreshingEvent() {
        mRefresher.postDelayed(new Runnable() {
            @Override
            public void run() {
                mDataList.clear();
                for (int i = 0; i < 20; i++) {
                    SampleModel sampleModel = new SampleModel();
                    sampleModel.setValues("ListView item_" + i + "  add by pull-to-refresh");
                    mDataList.add(sampleModel);
                }

                mQuickAdapter.notifyDataSetChanged();
                mRefresher.onRefreshComplete();
            }
        }, 8000);
    }

}
