package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Display;
import jp.co.disney.apps.managed.kisekaeapp.R;

public class HomeGridLayout extends CellLayout {

    private int mForegroundAlpha = 0;
    private float mBackgroundAlpha;
    private float mBackgroundAlphaMultiplier = 1.0f;

    private Drawable mNormalBackground;
    private Drawable mActiveGlowBackground;

    private Rect mBackgroundRect;
    private Rect mForegroundRect;
    private int mForegroundPadding;

    private Drawable mGlow;
    private Drawable mEdge;
    private Drawable mGlowRight;
    private Drawable mEdgeRight;
    private boolean mLeft;

    public HomeGridLayout(Context context) {
        this(context, null);
    }

    public HomeGridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeGridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        Point displaySize = new Point();
        Display display = mLauncher.getWindowManager().getDefaultDisplay();
        display.getSize(displaySize);

        mfCellWidth = displaySize.x / (float) mCountX;
        mCellWidth = (int) Math.floor(mfCellWidth);
        mfCellHeight = mfCellWidth * BASE_CELL_HEIGHT / BASE_CELL_WIDTH;
        mCellHeight = (int) Math.floor(mfCellHeight);

        mScale = displaySize.x / 1080f;
        mIconSize = calcIconSize(mfCellWidth);
        mCellPadTop = calcCellPaddingTop(mfCellWidth);
        mTextPad = calcCellTextPadding(mfCellWidth);
        mFolderIconSize = calcFolderIconSize(mfCellWidth);
        mFolderCellPadTop = calcFolderCellPaddingTop(mfCellWidth);

        mReorderHintAnimationMagnitude = (REORDER_HINT_MAGNITUDE * mIconSize);

        final Resources res = getResources();
        mForegroundPadding =
                res.getDimensionPixelSize(R.dimen.workspace_overscroll_drawable_padding);

        mBackgroundRect = new Rect();
        mForegroundRect = new Rect();

        mShortcutsAndWidgets.setCellDimensions(mfCellWidth, mfCellHeight, mCountX);
    }

    public void init(Drawable normalBackground, Drawable activeGlowBackground,
            Drawable glow, Drawable edge, Drawable glowRight, Drawable edgeRight) {
        mNormalBackground = normalBackground;
        mActiveGlowBackground = activeGlowBackground;
        mGlow = glow;
        mEdge = edge;
        mGlowRight = glowRight;
        mEdgeRight = edgeRight;
    }

    void setOverScrollAmount(float r, boolean left) {
        mForegroundAlpha = (int) Math.round((r * 255));
        mGlow.setAlpha(mForegroundAlpha);
        mEdge.setAlpha(mForegroundAlpha);
        mGlowRight.setAlpha(mForegroundAlpha);
        mEdgeRight.setAlpha(mForegroundAlpha);
        mLeft = left;

        invalidate();
    }

    void setIsDragOverlapping(boolean isDragOverlapping) {
        if (mIsDragOverlapping != isDragOverlapping) {
            mIsDragOverlapping = isDragOverlapping;
            invalidate();
        }
    }

    boolean getIsDragOverlapping() {
        return mIsDragOverlapping;
    }

    public void setOverscrollTransformsDirty(boolean dirty) {
        mScrollingTransformsDirty = dirty;
    }

    public void resetOverscrollTransforms() {
        if (mScrollingTransformsDirty) {
            setOverscrollTransformsDirty(false);
            setTranslationX(0);
            setRotationY(0);
            // It doesn't matter if we pass true or false here, the important thing is that we
            // pass 0, which results in the overscroll drawable not being drawn any more.
            setOverScrollAmount(0, false);
            setPivotX(getMeasuredWidth() / 2);
            setPivotY(getMeasuredHeight() / 2);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (mBackgroundAlpha > 0.0f) {
            // ドロワーからアイテムドロップ時の枠線を描画
            Drawable bg;
            if (mIsDragOverlapping) {
                // In the mini case, we draw the active_glow bg *over* the active background
                bg = mActiveGlowBackground;
            } else {
                bg = mNormalBackground;
            }
            bg.setAlpha((int) (mBackgroundAlpha * mBackgroundAlphaMultiplier * 255));
            bg.setBounds(mBackgroundRect);
            bg.draw(canvas);
        }

        super.onDraw(canvas);
    }

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
        mBackgroundRect.set(0, 0, w, h);
        mForegroundRect.set(mForegroundPadding, mForegroundPadding,
                w - mForegroundPadding, h - mForegroundPadding);
    }

    public float getBackgroundAlpha() {
        return mBackgroundAlpha;
    }

    public void setBackgroundAlphaMultiplier(float multiplier) {
        if (mBackgroundAlphaMultiplier != multiplier) {
            mBackgroundAlphaMultiplier = multiplier;
            invalidate();
        }
    }

    public float getBackgroundAlphaMultiplier() {
        return mBackgroundAlphaMultiplier;
    }

    public void setBackgroundAlpha(float alpha) {
        if (mBackgroundAlpha != alpha) {
            mBackgroundAlpha = alpha;
            invalidate();
        }
    }

    void onDragExit() {
        super.onDragExit();
        setIsDragOverlapping(false);
    }

    private static int calcIconSize(float cellWidth) {
        return (int) Math.floor(cellWidth * BASE_ICON_SIZE / BASE_CELL_WIDTH);
    }

    private static int calcCellPaddingTop(float cellWidth) {
        return (int) Math.ceil(cellWidth * 36 / BASE_CELL_WIDTH);
    }

    private static int calcCellTextPadding(float cellWidth) {
        return (int) Math.ceil(cellWidth * 24 / BASE_CELL_WIDTH);
    }

    private static int calcFolderIconSize(float cellWidth) {
        return (int) Math.floor(cellWidth * BASE_FOLDER_ICON_SIZE / BASE_CELL_WIDTH);
    }

    private static int calcFolderCellPaddingTop(float cellWidth) {
        return (int) Math.ceil(cellWidth * 18 / BASE_CELL_WIDTH);
    }
}
