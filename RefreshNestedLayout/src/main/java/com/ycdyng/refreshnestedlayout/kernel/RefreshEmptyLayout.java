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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.ycdyng.refreshnestedlayout.R;

public class RefreshEmptyLayout extends FrameLayout {

    public RefreshEmptyLayout(Context context) {
        super(context);
        init(context, null);
    }

    public RefreshEmptyLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // Styleables from XML
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RefreshNestedLayout);
        int emptyLayoutResId = a.getResourceId(R.styleable.RefreshNestedLayout_emptyLayout, R.layout.default_body_empty_layout);
        a.recycle();

        LayoutInflater.from(context).inflate(emptyLayoutResId, this);

        LayoutParams flp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        flp.gravity = Gravity.TOP | Gravity.LEFT;
        setLayoutParams(flp);
        setId(R.id.empty_layout);
        setClickable(true);
    }

}
