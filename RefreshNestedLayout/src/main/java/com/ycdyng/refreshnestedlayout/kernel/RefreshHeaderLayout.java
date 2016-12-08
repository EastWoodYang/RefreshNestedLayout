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

package com.ycdyng.refreshnestedlayout.kernel;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.ycdyng.refreshnestedlayout.R;

public abstract class RefreshHeaderLayout extends FrameLayout implements RefreshHeaderHelper {

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
        setId(R.id.header_layout);
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

}
