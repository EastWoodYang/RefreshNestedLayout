package com.eastwood.common.view;

public interface RefreshHeader {

    void setHeight(int height);

    void setMargins(int left, int top, int right, int bottom);

    void onPull(float scrollValue);

    void alreadyToRefresh(boolean alreadyToRefresh);

    void onRefreshBegin();

    void onRefreshFinish();

    void onRefreshCancel();

    boolean isMovable();

}
