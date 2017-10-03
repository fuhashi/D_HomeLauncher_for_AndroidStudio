/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.ImageView;

/**
 * We use a custom tab view to process our own focus traversals.
 */
public class AccessibleTabView extends ImageView {

    private BitmapDrawable mTextDrawable;
    private int mTextOffsetY = 0;

    public void setTextDrawable(BitmapDrawable textDrawable) {
        mTextDrawable = textDrawable;
    }

    public void setTextOffsetY(int textOffsetY) {
        mTextOffsetY = textOffsetY;
    }

    public AccessibleTabView(Context context) {
        super(context);
    }

    public AccessibleTabView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessibleTabView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return FocusHelper.handleTabKeyEvent(this, keyCode, event)
                || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return FocusHelper.handleTabKeyEvent(this, keyCode, event)
                || super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mTextDrawable != null) {

            final int cw = getWidth();
            final int ch = getHeight();

            final Bitmap textBmp = mTextDrawable.getBitmap();
            final int tw = textBmp.getWidth();
            final int th = textBmp.getHeight();

            int left = (int) Math.ceil((cw - tw) / 2f);
            int top = (int) Math.ceil((ch - th) / 2f - ch * 2 / 105f) + mTextOffsetY;

            mTextDrawable.setBounds(left, top, left + tw, top + th);
            mTextDrawable.draw(canvas);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (mTextDrawable == null) return;

        if (isSelected() || isPressed()) {
            mTextDrawable.setColorFilter(Color.rgb(255, 39, 166), PorterDuff.Mode.SRC_ATOP);
        } else {
            mTextDrawable.clearColorFilter();
        }
    }
}
