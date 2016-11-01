package com.ycdyng.refreshnestedlayout.widget.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Modify Bookends(https://github.com/tumblr/Bookends)
 * -add auto loading
 */
public class WrapRecyclerAdapter<T extends RecyclerView.Adapter> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final T mBase;

    protected boolean autoLoadUsable;

    protected boolean autoLoad = true;

    protected boolean loadError;

    protected boolean loadEnd;

    protected boolean loading;

    protected int autoLoadResId;

    protected int clickableResId;

    protected int loadEndResId;

    protected int loadErrorResId;

    private View.OnClickListener mOnLastItemClickListener;

    /**
     * Defines available view type integers for headers and footers.
     * <p/>
     * How this works:
     * - Regular views use view types starting from 0, counting upwards
     * - Header views use view types starting from -1000, counting upwards
     * - Footer views use view types starting from -2000, counting upwards
     * <p/>
     * This means that you're safe as long as the base adapter doesn't use negative view types,
     * and as long as you have fewer than 1000 headers and footers
     */
    private static final int HEADER_VIEW_TYPE = -1000;
    private static final int FOOTER_VIEW_TYPE = -2000;

    private static final int AUTO_LOAD_VIEW_TYPE = -3001;
    private static final int CLICKABLE_VIEW_TYPE = -3002;
    private static final int ERROR_VIEW_TYPE = -3003;
    private static final int END_VIEW_TYPE = -3004;

    private final List<View> mHeaders = new ArrayList<View>();
    private final List<View> mFooters = new ArrayList<View>();

    /**
     * Constructor.
     *
     * @param base the adapter to wrap
     */
    public WrapRecyclerAdapter(T base) {
        super();
        mBase = base;
    }

    /**
     * Gets the base adapter that this is wrapping.
     */
    public T getWrappedAdapter() {
        return mBase;
    }

    /**
     * Adds a header view.
     */
    public void addHeader(@NonNull View view) {
        if (view == null) {
            throw new IllegalArgumentException("You can't have a null header!");
        }
        mHeaders.add(view);
    }

    /**
     * Adds a footer view.
     */
    public void addFooter(@NonNull View view) {
        if (view == null) {
            throw new IllegalArgumentException("You can't have a null footer!");
        }
        mFooters.add(view);
    }

    /**
     * Toggles the visibility of the header views.
     */
    public void setHeaderVisibility(boolean shouldShow) {
        for (View header : mHeaders) {
            header.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Toggles the visibility of the footer views.
     */
    public void setFooterVisibility(boolean shouldShow) {
        for (View footer : mFooters) {
            footer.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * @return the number of headers.
     */
    public int getHeaderCount() {
        return mHeaders.size();
    }

    /**
     * @return the number of footers.
     */
    public int getFooterCount() {
        return mFooters.size();
    }

    /**
     * Gets the indicated header, or null if it doesn't exist.
     */
    public View getHeader(int i) {
        return i < mHeaders.size() ? mHeaders.get(i) : null;
    }

    /**
     * Gets the indicated footer, or null if it doesn't exist.
     */
    public View getFooter(int i) {
        return i < mFooters.size() ? mFooters.get(i) : null;
    }

    private boolean isHeader(int viewType) {
        return viewType >= HEADER_VIEW_TYPE && viewType < (HEADER_VIEW_TYPE + mHeaders.size());
    }

    private boolean isFooter(int viewType) {
        return viewType >= FOOTER_VIEW_TYPE && viewType < (FOOTER_VIEW_TYPE + mFooters.size());
    }

    private boolean isAutoLoadView(int viewType) {
        return viewType == AUTO_LOAD_VIEW_TYPE || viewType == CLICKABLE_VIEW_TYPE || viewType == ERROR_VIEW_TYPE || viewType == END_VIEW_TYPE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (isHeader(viewType)) {
            int whichHeader = Math.abs(viewType - HEADER_VIEW_TYPE);
            View headerView = mHeaders.get(whichHeader);
            return new RecyclerView.ViewHolder(headerView) {
            };
        } else if (isFooter(viewType)) {
            int whichFooter = Math.abs(viewType - FOOTER_VIEW_TYPE);
            View footerView = mFooters.get(whichFooter);
            return new RecyclerView.ViewHolder(footerView) {
            };
        } else if (isAutoLoadView(viewType)) {
            return createAutoLoadViewHolder(viewGroup);
        } else {
            return mBase.onCreateViewHolder(viewGroup, viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (position < mHeaders.size()) {
            // Headers don't need anything special
        } else if (position < mHeaders.size() + mBase.getItemCount()) {
            // This is a real position, not a header or footer. Bind it.
            mBase.onBindViewHolder(viewHolder, position - mHeaders.size());
        } else {
            // Footers don't need anything special
            if (autoLoadUsable && position + 1 == getItemCount()) {
                if (loadError) {
                    viewHolder.itemView.setOnClickListener(mOnLastItemClickListener);
                } else if (loadEnd) {
                    viewHolder.itemView.setOnClickListener(null);
                } else {
                    if (autoLoad || loading) {
                        viewHolder.itemView.setOnClickListener(null);
                    } else {
                        viewHolder.itemView.setOnClickListener(mOnLastItemClickListener);
                    }
                }
            } else {
                // Footers don't need anything special
            }
        }
    }

    @Override
    public int getItemCount() {
        int extra = autoLoadUsable ? 1 : 0;
        return mHeaders.size() + mBase.getItemCount() + mFooters.size() + extra;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < mHeaders.size()) {
            return HEADER_VIEW_TYPE + position;
        } else if (position < (mHeaders.size() + mBase.getItemCount())) {
            return mBase.getItemViewType(position - mHeaders.size());
        } else {
            if (autoLoadUsable && position + 1 == getItemCount()) {
                if (loadError) {
                    return ERROR_VIEW_TYPE;
                } else if (loadEnd) {
                    return END_VIEW_TYPE;
                } else {
                    if (autoLoad || loading) {
                        return AUTO_LOAD_VIEW_TYPE;
                    } else {
                        return CLICKABLE_VIEW_TYPE;
                    }
                }
            } else {
                return FOOTER_VIEW_TYPE + position - mHeaders.size() - mBase.getItemCount();
            }
        }
    }

    private RecyclerView.ViewHolder createAutoLoadViewHolder(ViewGroup viewGroup) {
        View lastItemView = null;
        if (loadError) {
            lastItemView = LayoutInflater.from(viewGroup.getContext()).inflate(loadErrorResId, viewGroup, false);
        } else if (loadEnd) {
            lastItemView = LayoutInflater.from(viewGroup.getContext()).inflate(loadEndResId, viewGroup, false);
        } else {
            if (autoLoad || loading) {
                lastItemView = LayoutInflater.from(viewGroup.getContext()).inflate(autoLoadResId, viewGroup, false);
            } else {
                lastItemView = LayoutInflater.from(viewGroup.getContext()).inflate(clickableResId, viewGroup, false);
            }
        }
        return new RecyclerView.ViewHolder(lastItemView) {
        };
    }

    public void setAutoLoadUsable(boolean display) {
        if (display == autoLoadUsable) return;
        autoLoadUsable = display;
        notifyItemChanged(getItemCount());
    }

    public boolean getAutoLoadUsable() {
        return autoLoadUsable;
    }

    public boolean isAutoLoad() {
        return autoLoad;
    }

    public void setAutoLoad(boolean autoLoad) {
        this.autoLoad = autoLoad;
    }

    public boolean isLoadError() {
        return loadError;
    }

    public void setLoadError(boolean loadError) {
        this.loadError = loadError;
    }

    public boolean isLoadEnd() {
        return loadEnd;
    }

    public void setLoadEnd(boolean loadEnd) {
        this.loadEnd = loadEnd;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public void setAutoLoadResId(int resId) {
        this.autoLoadResId = resId;
    }

    public int getAutoLoadResId() {
        return autoLoadResId;
    }

    public void setClickableResId(int resId) {
        this.clickableResId = resId;
    }

    public int getClickableResId() {
        return clickableResId;
    }

    public int getLoadEndResId() {
        return loadEndResId;
    }

    public void setLoadEndResId(int loadEndResId) {
        this.loadEndResId = loadEndResId;
    }

    public int getLoadErrorResId() {
        return loadErrorResId;
    }

    public void setLoadErrorResId(int loadErrorResId) {
        this.loadErrorResId = loadErrorResId;
    }

    public void setOnLastItemClickListener(View.OnClickListener listener) {
        this.mOnLastItemClickListener = listener;
    }

    public final void onNotifyItemChanged(int position) {
        notifyItemChanged(position + getHeaderCount());
    }

    public final void onNotifyItemChanged(int position, Object payload) {
        notifyItemChanged(position + getHeaderCount(), payload);
    }

    public final void onNotifyItemRangeChanged(int positionStart, int itemCount) {
        notifyItemRangeChanged(positionStart + getHeaderCount(), itemCount);
    }

    public final void onNotifyItemRangeChanged(int positionStart, int itemCount, Object payload) {
        notifyItemRangeChanged(positionStart + getHeaderCount(), itemCount, payload);
    }

    public final void onNotifyItemRemoved(int position) {
        notifyItemRemoved(position + getHeaderCount());
    }

    public final void onNotifyItemMoved(int fromPosition, int toPosition) {
        notifyItemMoved(fromPosition + getHeaderCount(), toPosition + getHeaderCount());
    }

    public final void onNotifyItemRangeRemoved(int positionStart, int itemCount) {
        notifyItemRangeRemoved(positionStart + getHeaderCount(), itemCount);
    }

    public final void onNotifyItemInserted(int position) {
        notifyItemInserted(position + getHeaderCount());
    }

    public final void onNotifyItemRangeInserted(int positionStart, int itemCount) {
        notifyItemRangeInserted(positionStart + getHeaderCount(), itemCount);
    }

    public final void onNotifyDataSetChanged() {
        notifyDataSetChanged();
    }

}