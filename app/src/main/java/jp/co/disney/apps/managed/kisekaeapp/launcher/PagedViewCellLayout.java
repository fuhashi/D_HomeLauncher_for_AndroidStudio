/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;

/**
 * An abstraction of the original CellLayout which supports laying out items
 * which span multiple cells into a grid-like layout.  Also supports dimming
 * to give a preview of its contents.
 */
public class PagedViewCellLayout extends ViewGroup implements Page {
    static final String TAG = "PagedViewCellLayout";

    private int mCellCountX;
    private int mCellCountY;
    private int mOriginalCellWidth;
    private int mOriginalCellHeight;
    private int mCellWidth;
    private int mCellHeight;
    private int mOriginalWidthGap;
    private int mOriginalHeightGap;
    private int mWidthGap;
    private int mHeightGap;
    private int mMaxGap;
    protected PagedViewCellLayoutChildren mChildren;

    public PagedViewCellLayout(Context context, Bitmap glowBmp, Bitmap edgeBmp,
            Bitmap glowRightBmp, Bitmap edgeRightBmp) {
        super(context);

        setAlwaysDrawnWithCacheEnabled(false);

        // setup default cell parameters
        Resources resources = context.getResources();
        mOriginalCellWidth = mCellWidth =
            resources.getDimensionPixelSize(R.dimen.apps_customize_cell_width);
        mOriginalCellHeight = mCellHeight =
            resources.getDimensionPixelSize(R.dimen.apps_customize_cell_height);
        mCellCountX = LauncherModel.getCellCountX();
        mCellCountY = LauncherModel.getCellCountY();
        mOriginalWidthGap = mOriginalHeightGap = mWidthGap = mHeightGap = -1;
        mMaxGap = resources.getDimensionPixelSize(R.dimen.apps_customize_max_gap);

        mChildren = new PagedViewCellLayoutChildren(context);
        mChildren.setCellDimensions(mCellWidth, mCellHeight);
        mChildren.setGap(mWidthGap, mHeightGap);

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

        addView(mChildren);
    }

    public int getCellWidth() {
        return mCellWidth;
    }

    public int getCellHeight() {
        return mCellHeight;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        // Cancel long press for all children
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.cancelLongPress();
        }
    }

    public boolean addViewToCellLayout(View child, int index, int childId,
            PagedViewCellLayout.LayoutParams params) {
        final PagedViewCellLayout.LayoutParams lp = params;

        // Generate an id for each view, this assumes we have at most 256x256 cells
        // per workspace screen
        if (lp.cellX >= 0 && lp.cellX <= (mCellCountX - 1) &&
                lp.cellY >= 0 && (lp.cellY <= mCellCountY - 1)) {
            // If the horizontal or vertical span is set to -1, it is taken to
            // mean that it spans the extent of the CellLayout
            if (lp.cellHSpan < 0) lp.cellHSpan = mCellCountX;
            if (lp.cellVSpan < 0) lp.cellVSpan = mCellCountY;

            child.setId(childId);
            mChildren.addView(child, index, lp);

            return true;
        }
        return false;
    }

    @Override
    public void removeAllViewsOnPage() {
        mChildren.removeAllViews();
        setLayerType(LAYER_TYPE_NONE, null);
    }

    @Override
    public void removeViewOnPageAt(int index) {
        mChildren.removeViewAt(index);
    }

    /**
     * Clears all the key listeners for the individual icons.
     */
    public void resetChildrenOnKeyListeners() {
        int childCount = mChildren.getChildCount();
        for (int j = 0; j < childCount; ++j) {
            mChildren.getChildAt(j).setOnKeyListener(null);
        }
    }

    @Override
    public int getPageChildCount() {
        return mChildren.getChildCount();
    }

    public PagedViewCellLayoutChildren getChildrenLayout() {
        return mChildren;
    }

    @Override
    public View getChildOnPageAt(int i) {
        return mChildren.getChildAt(i);
    }

    @Override
    public int indexOfChildOnPage(View v) {
        return mChildren.indexOfChild(v);
    }

    public int getCellCountX() {
        return mCellCountX;
    }

    public int getCellCountY() {
        return mCellCountY;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }

        int numWidthGaps = mCellCountX - 1;
        int numHeightGaps = mCellCountY - 1;

        if (mOriginalWidthGap < 0 || mOriginalHeightGap < 0) {
            int hSpace = widthSpecSize - getPaddingLeft() - getPaddingRight();
            int vSpace = heightSpecSize - getPaddingTop() - getPaddingBottom();
            int hFreeSpace = hSpace - (mCellCountX * mOriginalCellWidth);
            int vFreeSpace = vSpace - (mCellCountY * mOriginalCellHeight);
            mWidthGap = Math.min(mMaxGap, numWidthGaps > 0 ? (hFreeSpace / numWidthGaps) : 0);
            mHeightGap = Math.min(mMaxGap,numHeightGaps > 0 ? (vFreeSpace / numHeightGaps) : 0);

            mChildren.setGap(mWidthGap, mHeightGap);
        } else {
            mWidthGap = mOriginalWidthGap;
            mHeightGap = mOriginalHeightGap;
        }

        // Initial values correspond to widthSpecMode == MeasureSpec.EXACTLY
        int newWidth = widthSpecSize;
        int newHeight = heightSpecSize;
        if (widthSpecMode == MeasureSpec.AT_MOST) {
            newWidth = getPaddingLeft() + getPaddingRight() + (mCellCountX * mCellWidth) +
                ((mCellCountX - 1) * mWidthGap);
            newHeight = getPaddingTop() + getPaddingBottom() + (mCellCountY * mCellHeight) +
                ((mCellCountY - 1) * mHeightGap);
            setMeasuredDimension(newWidth, newHeight);
        }

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childWidthMeasureSpec =
                MeasureSpec.makeMeasureSpec(newWidth - getPaddingLeft() -
                        getPaddingRight(), MeasureSpec.EXACTLY);
            int childheightMeasureSpec =
                MeasureSpec.makeMeasureSpec(newHeight - getPaddingTop() -
                        getPaddingBottom(), MeasureSpec.EXACTLY);
            child.measure(childWidthMeasureSpec, childheightMeasureSpec);
        }

        setMeasuredDimension(newWidth, newHeight);
    }

    int getContentWidth() {
        return getWidthBeforeFirstLayout() + getPaddingLeft() + getPaddingRight();
    }

    int getContentHeight() {
        if (mCellCountY > 0) {
            return mCellCountY * mCellHeight + (mCellCountY - 1) * Math.max(0, mHeightGap);
        }
        return 0;
    }

    int getWidthBeforeFirstLayout() {
        if (mCellCountX > 0) {
            return mCellCountX * mCellWidth + (mCellCountX - 1) * Math.max(0, mWidthGap);
        }
        return 0;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            child.layout(getPaddingLeft(), getPaddingTop(),
                r - l - getPaddingRight(), b - t - getPaddingBottom());
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
            int numRows = (int) Math.ceil((float) getPageChildCount() / getCellCountX());
            if (numRows < getCellCountY()) {
                // Add a little bit of buffer if there is room for another row
                bottom += mCellHeight / 2;
            }
            result = result || (event.getY() < bottom);
        }
        return result;
    }

    public void enableCenteredContent(boolean enabled) {
        mChildren.enableCenteredContent(enabled);
    }

    @Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        mChildren.setChildrenDrawingCacheEnabled(enabled);
    }

    public void setCellCount(int xCount, int yCount) {
        mCellCountX = xCount;
        mCellCountY = yCount;
        requestLayout();
    }

    public void setGap(int widthGap, int heightGap) {
        mOriginalWidthGap = mWidthGap = widthGap;
        mOriginalHeightGap = mHeightGap = heightGap;
        mChildren.setGap(widthGap, heightGap);
    }

    public int[] getCellCountForDimensions(int width, int height) {
        int spanX = (width + mCellWidth) / mCellWidth;
        int spanY = (height + mCellHeight) / mCellHeight;
        return new int[] { spanX, spanY };
    }

    /**
     * Start dragging the specified child
     *
     * @param child The child that is being dragged
     */
    void onDragChild(View child) {
        PagedViewCellLayout.LayoutParams lp = (PagedViewCellLayout.LayoutParams) child.getLayoutParams();
        lp.isDragging = true;
    }

    /**
     * Estimates the number of cells that the specified width would take up.
     */
    public int estimateCellHSpan(int width) {
        // We don't show the next/previous pages any more, so we use the full width, minus the
        // padding
        int availWidth = width - (getPaddingLeft() + getPaddingRight());

        // We know that we have to fit N cells with N-1 width gaps, so we just juggle to solve for N
        int n = Math.max(1, (availWidth + mWidthGap) / (mCellWidth + mWidthGap));

        // We don't do anything fancy to determine if we squeeze another row in.
        return n;
    }

    /**
     * Estimates the number of cells that the specified height would take up.
     */
    public int estimateCellVSpan(int height) {
        // The space for a page is the height - top padding (current page) - bottom padding (current
        // page)
        int availHeight = height - (getPaddingTop() + getPaddingBottom());

        // We know that we have to fit N cells with N-1 height gaps, so we juggle to solve for N
        int n = Math.max(1, (availHeight + mHeightGap) / (mCellHeight + mHeightGap));

        // We don't do anything fancy to determine if we squeeze another row in.
        return n;
    }

    /** Returns an estimated center position of the cell at the specified index */
    public int[] estimateCellPosition(int x, int y) {
        return new int[] {
                getPaddingLeft() + (x * mCellWidth) + (x * mWidthGap) + (mCellWidth / 2),
                getPaddingTop() + (y * mCellHeight) + (y * mHeightGap) + (mCellHeight / 2)
        };
    }

    public void calculateCellCount(int width, int height, int maxCellCountX, int maxCellCountY) {
        mCellCountX = Math.min(maxCellCountX, estimateCellHSpan(width));
        mCellCountY = Math.min(maxCellCountY, estimateCellVSpan(height));
        requestLayout();
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

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new PagedViewCellLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof PagedViewCellLayout.LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new PagedViewCellLayout.LayoutParams(p);
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

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        /**
         * Horizontal location of the item in the grid.
         */
        @ViewDebug.ExportedProperty
        public int cellX;

        /**
         * Vertical location of the item in the grid.
         */
        @ViewDebug.ExportedProperty
        public int cellY;

        /**
         * Number of cells spanned horizontally by the item.
         */
        @ViewDebug.ExportedProperty
        public int cellHSpan;

        /**
         * Number of cells spanned vertically by the item.
         */
        @ViewDebug.ExportedProperty
        public int cellVSpan;

        /**
         * Is this item currently being dragged
         */
        public boolean isDragging;

        // a data object that you can bind to this layout params
        private Object mTag;

        // X coordinate of the view in the layout.
        @ViewDebug.ExportedProperty
        int x;
        // Y coordinate of the view in the layout.
        @ViewDebug.ExportedProperty
        int y;

        public LayoutParams() {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.cellX = source.cellX;
            this.cellY = source.cellY;
            this.cellHSpan = source.cellHSpan;
            this.cellVSpan = source.cellVSpan;
        }

        public LayoutParams(int cellX, int cellY, int cellHSpan, int cellVSpan) {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            this.cellX = cellX;
            this.cellY = cellY;
            this.cellHSpan = cellHSpan;
            this.cellVSpan = cellVSpan;
        }

        public void setup(int cellWidth, int cellHeight, int widthGap, int heightGap,
                int hStartPadding, int vStartPadding) {

            final int myCellHSpan = cellHSpan;
            final int myCellVSpan = cellVSpan;
            final int myCellX = cellX;
            final int myCellY = cellY;

            width = myCellHSpan * cellWidth + ((myCellHSpan - 1) * widthGap) -
                    leftMargin - rightMargin;
            height = myCellVSpan * cellHeight + ((myCellVSpan - 1) * heightGap) -
                    topMargin - bottomMargin;

            if (LauncherApplication.isScreenLarge()) {
                x = hStartPadding + myCellX * (cellWidth + widthGap) + leftMargin;
                y = vStartPadding + myCellY * (cellHeight + heightGap) + topMargin;
            } else {
                x = myCellX * (cellWidth + widthGap) + leftMargin;
                y = myCellY * (cellHeight + heightGap) + topMargin;
            }
        }

        public Object getTag() {
            return mTag;
        }

        public void setTag(Object tag) {
            mTag = tag;
        }

        public String toString() {
            return "(" + this.cellX + ", " + this.cellY + ", " +
                this.cellHSpan + ", " + this.cellVSpan + ")";
        }
    }
}

