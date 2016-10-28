package com.ycdyng.refreshnestedlayout.widget.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class WrapAdapter<T extends BaseAdapter> extends AutoAdapter {

    private final T mBase;

    protected boolean alwaysShowHeader = false;

    /**
     * Constructor.
     *
     * @param base the adapter to wrap
     */
    public WrapAdapter(T base) {
        super();
        mBase = base;
    }

    /**
     * Gets the base adapter that this is wrapping.
     */
    public T getWrappedAdapter() {
        return mBase;
    }

    @Override
    public int getCount() {
        int extra = autoLoadUsable ? 1 : 0;
        return mBase.getCount() + extra;
    }

    @Override
    public Object getItem(int position) {
        if (getItemViewType(position) == 0) {
            return mBase.getItem(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == 0) {
            if (convertView != null && convertView.getTag() == null) {
                convertView = null;
            }
            return mBase.getView(position, convertView, parent);
        }
        return createAutoLoadView(convertView, parent);
    }

    @Override
    public int getItemViewType(int position) {
        return position >= mBase.getCount() ? 1 : 0;
    }

    public void setAlwaysShowHeader(boolean alwaysShowHeader) {
        this.alwaysShowHeader = alwaysShowHeader;
    }

    @Override
    public boolean isEmpty() {
        if (alwaysShowHeader) {
            return false;
        }
        return super.isEmpty();
    }
}
