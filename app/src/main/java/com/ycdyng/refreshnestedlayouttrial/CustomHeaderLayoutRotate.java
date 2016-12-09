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

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.ycdyng.refreshnestedlayout.kernel.RefreshHeaderLayout;

public class CustomHeaderLayoutRotate extends RefreshHeaderLayout {

    static final int ROTATION_ANIMATION_DURATION = 800;
    static final Interpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();
    private final Animation mRotateAnimation;

    private boolean mAlreadyToRefresh;

    private final Matrix mHeaderImageMatrix;
    private float mRotationPivotX, mRotationPivotY;

    private ImageView mArrowImageView;
    private TextView mTipTextView;

    public CustomHeaderLayoutRotate(Context context) {
        this(context, null);
    }

    public CustomHeaderLayoutRotate(Context context, AttributeSet attrs) {
        super(context, attrs);

        mRotateAnimation = new RotateAnimation(0, 720, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
        mRotateAnimation.setDuration(ROTATION_ANIMATION_DURATION);
        mRotateAnimation.setRepeatCount(Animation.INFINITE);
        mRotateAnimation.setRepeatMode(Animation.RESTART);

        LayoutInflater.from(context).inflate(R.layout.custom_header_layout_rotate, this);
        mArrowImageView = (ImageView) findViewById(R.id.anim_image_view);
        mTipTextView = (TextView) findViewById(R.id.tip_text_view);

        mArrowImageView.setScaleType(ImageView.ScaleType.MATRIX);
        mHeaderImageMatrix = new Matrix();
        mArrowImageView.setImageMatrix(mHeaderImageMatrix);

        Drawable imageDrawable = context.getResources().getDrawable(R.drawable.ic_rotate);
        mArrowImageView.setImageDrawable(imageDrawable);
        mRotationPivotX = Math.round(imageDrawable.getIntrinsicWidth() / 2f);
        mRotationPivotY = Math.round(imageDrawable.getIntrinsicHeight() / 2f);
    }

    @Override
    public void onPull(float scrollValue) {
        if (!mAlreadyToRefresh) {
            float angle = -scrollValue * 2f;
            mHeaderImageMatrix.setRotate(angle, mRotationPivotX, mRotationPivotY);
            mArrowImageView.setImageMatrix(mHeaderImageMatrix);
        }
    }

    @Override
    public void alreadyToRefresh(boolean alreadyToRefresh) {
        if (alreadyToRefresh) {
            mTipTextView.setText(R.string.release_to_refresh);
        } else {
            mTipTextView.setText(R.string.pull_to_refresh);
        }
        mAlreadyToRefresh = alreadyToRefresh;
    }

    @Override
    public void onRefreshBegin() {
        mTipTextView.setText(R.string.refreshing);
        mArrowImageView.startAnimation(mRotateAnimation);
    }

    @Override
    public void onRefreshFinish() {
        mTipTextView.setText("");
        mArrowImageView.clearAnimation();
        if (null != mHeaderImageMatrix) {
            mHeaderImageMatrix.reset();
            mArrowImageView.setImageMatrix(mHeaderImageMatrix);
        }
    }

    @Override
    public void onRefreshCancel() {

    }
}