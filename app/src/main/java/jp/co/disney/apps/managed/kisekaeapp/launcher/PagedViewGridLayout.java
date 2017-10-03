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

import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.system.graphics.FastBitmapDrawable;
import jp.co.disney.apps.managed.kisekaeapp.system.view.pagedview.Page;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;

/**
 * The grid based layout used strictly for the widget/wallpaper tab of the AppsCustomize pane
 */
public class PagedViewGridLayout extends GridLayout implements Page {
    static final String TAG = "PagedViewGridLayout";

    private int mCellCountX;
    private int mCellCountY;
    private Runnable mOnLayoutListener;

    private int mCellWidth;
    private int mCellHeight;

    public PagedViewGridLayout(Context context, int cellCountX, int cellCountY, Bitmap glowBmp, Bitmap edgeBmp,
             Bitmap glowRightBmp, Bitmap edgeRightBmp) {
        super(context, null, 0);
        mCellCountX = cellCountX;
        mCellCountY = cellCountY;

        final Resources resources = context.getResources();
        mForegroundRect = new Rect();
        mForegroundPadding =
                resources.getDimensionPixelSize(R.dimen.workspace_overscroll_drawable_padding);

        if (glowBmp != null) {
            mGlow = new FastBitmapDrawable(glowBmp);
        }
        if (edgeBmp != null) {
            mEdge = new FastBitmapDrawable(edgeBmp);;
        }
        if (glowRightBmp != null) {
            mGlowRight = new FastBitmapDrawable(glowRightBmp);
        }
        if (edgeRightBmp != null) {
            mEdgeRight = new FastBitmapDrawable(edgeRightBmp);
        }
    }

    int getCellCountX() {
        return mCellCountX;
    }

    int getCellCountY() {
        return mCellCountY;
    }

    void setCellSize(int cellWidth, int cellHeight) {
        mCellWidth = cellWidth;
        mCellHeight = cellHeight;
    }

    /**
     * Estimates the width that the number of hSpan cells will take up.
     */
    public int estimateCellWidth(int hSpan) {
        // TODO: we need to take widthGap into effect
        return hSpan * mCellWidth;
    }

    /**
     * Estimates the height that the number of vSpan cells will take up.
     */
    public int estimateCellHeight(int vSpan) {
        // TODO: we need to take heightGap into effect
        return vSpan * mCellHeight;
    }

    /**
     * Clears all the key listeners for the individual widgets.
     */
    public void resetChildrenOnKeyListeners() {
        int childCount = getChildCount();
        for (int j = 0; j < childCount; ++j) {
            getChildAt(j).setOnKeyListener(null);
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // PagedView currently has issues with different-sized pages since it calculates the
        // offset of each page to scroll to before it updates the actual size of each page
        // (which can change depending on the content if the contents aren't a fixed size).
        // We work around this by having a minimum size on each widget page).
        int widthSpecSize = Math.min(getSuggestedMinimumWidth(),
                MeasureSpec.getSize(widthMeasureSpec));
        int widthSpecMode = MeasureSpec.EXACTLY;
        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSpecSize, widthSpecMode),
                heightMeasureSpec);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mOnLayoutListener = null;
    }

    public void setOnLayoutListener(Runnable r) {
        mOnLayoutListener = r;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mOnLayoutListener != null) {
            mOnLayoutListener.run();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        int count = getPageChildCount();
        if (count > 0) {
            // We only intercept the touch if we are tapping in empty space after the final row
            View child = getChildOnPageAt(count - 1);
            int bottom = child.getBottom();
            result = result || (event.getY() < bottom);
        }
        return result;
    }

    @Override
    public void removeAllViewsOnPage() {
        removeAllViews();
        mOnLayoutListener = null;
        setLayerType(LAYER_TYPE_NONE, null);
    }

    @Override
    public void removeViewOnPageAt(int index) {
        removeViewAt(index);
    }

    @Override
    public int getPageChildCount() {
        return getChildCount();
    }

    @Override
    public View getChildOnPageAt(int i) {
        return getChildAt(i);
    }

    @Override
    public int indexOfChildOnPage(View v) {
        return indexOfChild(v);
    }

    private Drawable mGlow;
    private Drawable mEdge;
    private Drawable mGlowRight;
    private Drawable mEdgeRight;
    private boolean mLeft;

    private Rect mForegroundRect;
    private int mForegroundPadding;
    private int mForegroundAlpha = 0;

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mForegroundAlpha > 0) {

            if (mGlow != null) {
                final int width = mGlow.getIntrinsicWidth();
                final int height = mGlow.getIntrinsicHeight();

                int w = (mForegroundRect.bottom - mForegroundRect.top) * width / height;

                mGlow.setBounds(mForegroundRect.left, mForegroundRect.top,
                        mForegroundRect.left + w, mForegroundRect.bottom);

                mGlow.setColorFilter(0xffffffff, Mode.SRC_ATOP);
            }
            if (mGlowRight != null) {
                final int width = mGlowRight.getIntrinsicWidth();
                final int height = mGlowRight.getIntrinsicHeight();

                int w = (mForegroundRect.bottom - mForegroundRect.top) * width / height;

                mGlowRight.setBounds(mForegroundRect.right - w, mForegroundRect.top,
                        mForegroundRect.right, mForegroundRect.bottom);

                mGlowRight.setColorFilter(0xffffffff, Mode.SRC_ATOP);
            }

            if (mEdge != null) {
                final int width = mEdge.getIntrinsicWidth();
                final int height = mEdge.getIntrinsicHeight();

                int w = (mForegroundRect.bottom - mForegroundRect.top) * width / height;

                mEdge.setBounds(mForegroundRect.left, mForegroundRect.top,
                        mForegroundRect.left + w, mForegroundRect.bottom);

                mEdge.setColorFilter(0xffffffff, Mode.SRC_ATOP);
            }
            if (mEdgeRight != null) {
                final int width = mEdgeRight.getIntrinsicWidth();
                final int height = mEdgeRight.getIntrinsicHeight();

                int w = (mForegroundRect.bottom - mForegroundRect.top) * width / height;

                mEdgeRight.setBounds(mForegroundRect.right - w, mForegroundRect.top,
                        mForegroundRect.right, mForegroundRect.bottom);

                mEdgeRight.setColorFilter(0xffffffff, Mode.SRC_ATOP);
            }

            if (mLeft) {
                if (mEdge != null) mEdge.draw(canvas);
                if (mGlow != null) {
                    mGlow.draw(canvas);
                    mGlow.draw(canvas);
                }
            } else {
                if (mEdgeRight != null) mEdgeRight.draw(canvas);
                if (mGlowRight != null) {
                    mGlowRight.draw(canvas);
                    mGlowRight.draw(canvas);
                }
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mForegroundRect.set(mForegroundPadding, mForegroundPadding,
                w - mForegroundPadding, h - mForegroundPadding);
    }

    void setOverScrollAmount(float r, boolean left) {
        mForegroundAlpha = (int) Math.round((r * 255));
        if (mGlow != null) {
            mGlow.setAlpha(mForegroundAlpha);
        }
        if (mEdge != null) {
            mEdge.setAlpha(mForegroundAlpha);
        }
        if (mGlowRight != null) {
            mGlowRight.setAlpha(mForegroundAlpha);
        }
        if (mEdgeRight != null) {
            mEdgeRight.setAlpha(mForegroundAlpha);
        }
        mLeft = left;
        invalidate();
    }

    public static class LayoutParams extends FrameLayout.LayoutParams {
        public LayoutParams(int width, int height) {
            super(width, height);
        }
    }
}
