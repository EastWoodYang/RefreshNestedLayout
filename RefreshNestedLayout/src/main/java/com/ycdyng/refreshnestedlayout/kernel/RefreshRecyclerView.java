package com.ycdyng.refreshnestedlayout.kernel;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.ycdyng.refreshnestedlayout.widget.adapter.WrapRecyclerAdapter;
import com.ycdyng.refreshnestedlayout.widget.observable.ObservableRecyclerView;

/**
 * Modified from WrapRecyclerAdapter
 * http://www.littlerobots.nl/blog/Handle-Android-RecyclerView-Clicks/
 */
public class RefreshRecyclerView extends ObservableRecyclerView {

    private final RecyclerView mRecyclerView;
    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;

    public RefreshRecyclerView(Context context) {
        this(context, null);
    }

    public RefreshRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRecyclerView = this;
        setOverScrollMode(OVER_SCROLL_NEVER);
        addOnChildAttachStateChangeListener(mAttachListener);
    }

    private RecyclerView.OnChildAttachStateChangeListener mAttachListener = new RecyclerView.OnChildAttachStateChangeListener() {
        @Override
        public void onChildViewAttachedToWindow(View view) {
            if (mOnItemClickListener != null) {
                view.setOnClickListener(mOnClickListener);
            }
            if (mOnItemLongClickListener != null) {
                view.setOnLongClickListener(mOnLongClickListener);
            }
        }

        @Override
        public void onChildViewDetachedFromWindow(View view) {

        }
    };

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mOnItemClickListener != null) {
                RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(v);
                int position = holder.getAdapterPosition();
                Adapter adapter = mRecyclerView.getAdapter();
                if (adapter instanceof WrapRecyclerAdapter) {
                    WrapRecyclerAdapter baseRecyclerAdapter = (WrapRecyclerAdapter) adapter;
                    int itemViewType = baseRecyclerAdapter.getItemViewType(position);
                    if(itemViewType >= 0) {
                        int headerCount = baseRecyclerAdapter.getHeaderCount();
                        position -= headerCount;
                        mOnItemClickListener.onItemClicked(mRecyclerView, position, v);
                    }
                } else {
                    mOnItemClickListener.onItemClicked(mRecyclerView, position, v);
                }
            }
        }
    };
    private View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (mOnItemLongClickListener != null) {
                RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(v);
                int position = holder.getAdapterPosition();
                Adapter adapter = mRecyclerView.getAdapter();
                if (adapter instanceof WrapRecyclerAdapter) {
                    WrapRecyclerAdapter baseRecyclerAdapter = (WrapRecyclerAdapter) adapter;
                    int itemViewType = baseRecyclerAdapter.getItemViewType(position);
                    if(itemViewType >= 0) {
                        int headerCount = baseRecyclerAdapter.getHeaderCount();
                        position -= headerCount;
                        mOnItemLongClickListener.onItemLongClicked(mRecyclerView, position, v);
                    }
                } else {
                    return mOnItemLongClickListener.onItemLongClicked(mRecyclerView, position, v);
                }
            }
            return false;
        }
    };

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.mOnItemLongClickListener = onItemLongClickListener;
    }

    public interface OnItemClickListener {

        void onItemClicked(RecyclerView recyclerView, int position, View v);
    }

    public interface OnItemLongClickListener {

        boolean onItemLongClicked(RecyclerView recyclerView, int position, View v);
    }

}
