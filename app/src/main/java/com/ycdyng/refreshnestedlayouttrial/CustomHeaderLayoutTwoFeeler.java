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

package com.ycdyng.refreshnestedlayouttrial;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ycdyng.refreshnestedlayout.kernel.RefreshHeaderLayout;

public class CustomHeaderLayoutTwoFeeler extends RefreshHeaderLayout {

    private TextView mRefreshTipTextView;
    private ImageView mFeelerLeftImageView;
    private ImageView mFeelerRightImageView;
    private ObjectAnimator mFeelerLeftObjectAnimator;
    private ObjectAnimator mFeelerRightObjectAnimator;

    private static float SHAKE_DISTANCE = 1.5f;

    public CustomHeaderLayoutTwoFeeler(Context context) {
        this(context, null);
    }

    public CustomHeaderLayoutTwoFeeler(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.custom_header_layout_two_feeler, this);
        mRefreshTipTextView = (TextView) findViewById(R.id.refresh_tip_text_view);
        mFeelerLeftImageView = (ImageView) findViewById(R.id.feeler_left_image_view);
        mFeelerRightImageView = (ImageView) findViewById(R.id.feeler_right_image_view);
        if (Build.VERSION.SDK_INT >= 18) {
            mFeelerLeftObjectAnimator = nope(mFeelerLeftImageView);
            mFeelerLeftObjectAnimator.setRepeatCount(30);
            mFeelerLeftObjectAnimator.setAutoCancel(true);
            mFeelerRightObjectAnimator = nope(mFeelerRightImageView);
            mFeelerRightObjectAnimator.setRepeatCount(30);
            mFeelerRightObjectAnimator.setAutoCancel(true);
        }
    }

    @Override
    public void onPull(float scrollValue) {

    }

    @Override
    public void alreadyToRefresh(boolean alreadyToRefresh) {

    }

    @Override
    public void onRefreshBegin() {
        if (Build.VERSION.SDK_INT >= 18) {
            mFeelerLeftObjectAnimator.start();
            mFeelerRightObjectAnimator.start();
        }
    }

    @Override
    public void onRefreshFinish() {
        if (Build.VERSION.SDK_INT >= 18) {
            mFeelerLeftObjectAnimator.cancel();
            mFeelerRightObjectAnimator.cancel();
        }
    }

    @Override
    public void onRefreshCancel() {

    }


    public static ObjectAnimator nope(View view) {
        if (Build.VERSION.SDK_INT >= 11) {
            int delta = convertDpToPixel(view.getContext(), SHAKE_DISTANCE);
            PropertyValuesHolder pvhTranslateX = PropertyValuesHolder.ofKeyframe("translationX",
                    Keyframe.ofFloat(0f, 0),
                    Keyframe.ofFloat(.10f, -delta),
                    Keyframe.ofFloat(.26f, delta),
                    Keyframe.ofFloat(.42f, -delta),
                    Keyframe.ofFloat(.58f, delta),
                    Keyframe.ofFloat(.74f, -delta),
                    Keyframe.ofFloat(.90f, delta),
                    Keyframe.ofFloat(1f, 0f)
            );
            return ObjectAnimator.ofPropertyValuesHolder(view, pvhTranslateX).setDuration(500);
        } else {
            return null;
        }
    }

    private static int convertDpToPixel(Context context, float sizeInDip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeInDip, context.getResources().getDisplayMetrics());
        //return TypedValue.complexToDimensionPixelSize(sizeInDip, getResources().getDisplayMetrics());
    }
}
