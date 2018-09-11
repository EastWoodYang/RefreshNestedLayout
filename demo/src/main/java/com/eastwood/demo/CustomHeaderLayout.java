package com.eastwood.demo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.eastwood.common.view.RefreshHeaderLayout;

public class CustomHeaderLayout extends RefreshHeaderLayout {

    private TextView mTipTextView;

    public CustomHeaderLayout(Context context) {
        this(context, null);
    }

    public CustomHeaderLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.custom_header_layout, this);
        mTipTextView = (TextView) findViewById(R.id.tip_text_view);
    }

    @Override
    public void onPull(float scrollValue) {

    }

    @Override
    public void alreadyToRefresh(boolean alreadyToRefresh) {
        if(alreadyToRefresh) {
            mTipTextView.setText(R.string.release_to_refresh);
        } else {
            mTipTextView.setText(R.string.pull_to_refresh);
        }
    }

    @Override
    public void onRefreshBegin() {
        mTipTextView.setText(R.string.refreshing);
    }

    @Override
    public void onRefreshFinish() {
        mTipTextView.setText("");
    }

    @Override
    public void onRefreshCancel() {

    }

    @Override
    public boolean isMovable() {
        return true;
    }

}