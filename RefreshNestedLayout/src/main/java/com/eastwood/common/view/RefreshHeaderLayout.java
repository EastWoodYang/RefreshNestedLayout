package com.eastwood.common.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;

import java.lang.reflect.Constructor;

public abstract class RefreshHeaderLayout extends FrameLayout implements RefreshHeader {

    public RefreshHeaderLayout(Context context) {
        this(context, null);
    }

    public RefreshHeaderLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutParams flp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        flp.gravity = Gravity.TOP | Gravity.LEFT;
        setLayoutParams(flp);
    }

    @Override
    public void setHeight(int height) {
        LayoutParams lp = (LayoutParams) getLayoutParams();
        lp.height = height;
        requestLayout();
    }

    @Override
    public void setMargins(int left, int top, int right, int bottom) {
        LayoutParams lp = (LayoutParams) getLayoutParams();
        lp.setMargins(left, top, right, bottom);
        requestLayout();
    }

    static final Class<?>[] CONSTRUCTOR_PARAMS = new Class<?>[]{
            Context.class,
            AttributeSet.class
    };

    static RefreshHeaderLayout parseRefreshHeader(Context context, AttributeSet attrs, String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }

        try {
            final Class<RefreshHeaderLayout> clazz = (Class<RefreshHeaderLayout>) Class.forName(name, true, context.getClassLoader());
            Constructor<RefreshHeaderLayout> c = clazz.getConstructor(CONSTRUCTOR_PARAMS);
            c.setAccessible(true);
            return c.newInstance(context, attrs);
        } catch (Exception e) {
            throw new RuntimeException("Could not inflate RefreshHeaderLayout subclass " + name, e);
        }
    }

}
