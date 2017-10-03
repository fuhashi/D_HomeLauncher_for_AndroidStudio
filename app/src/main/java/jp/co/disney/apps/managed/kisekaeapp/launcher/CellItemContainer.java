package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

public class CellItemContainer extends ViewGroup {
    static final String TAG = "CellItemContainer";

    private float mCellWidth;
    private float mCellHeight;

    public CellItemContainer(Context context) {
        super(context);
    }

    public void setCellDimensions(float cellWidth, float cellHeight) {
        mCellWidth = cellWidth;
        mCellHeight = cellHeight;
    }

    public View getChildAt(int x, int y) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final BaseCellLayout.LayoutParams lp = (BaseCellLayout.LayoutParams) child.getLayoutParams();
            if ((lp.cellX <= x) && (x < lp.cellX + lp.cellHSpan) &&
                    (lp.cellY <= y) && (y < lp.cellY + lp.cellVSpan)) {
                return child;
            }
        }
        return null;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        @SuppressWarnings("all") // suppress dead code warning
        final boolean debug = false;
        if (debug) {
            // Debug drawing for hit space
            final Paint p = new Paint();
            p.setColor(0x6600FF00);
            final int count = getChildCount();
            for (int i = count - 1; i >= 0; i--) {
                final View child = getChildAt(i);
                final BaseCellLayout.LayoutParams lp = (BaseCellLayout.LayoutParams) child.getLayoutParams();
                canvas.drawRect(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height, p);
            }
        }
        super.dispatchDraw(canvas);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View view, long l) {

        if (view.getVisibility() != View.VISIBLE) return false;

        final BaseCellLayout.LayoutParams lp = (BaseCellLayout.LayoutParams) view.getLayoutParams();

        final boolean requireScale = (lp.drawingScale > 0f);
        if (requireScale) {
            canvas.save();
            canvas.scale(lp.drawingScale, lp.drawingScale,
                    view.getX() + view.getWidth() / 2, view.getY() + view.getHeight() / 2);
            final boolean ret = super.drawChild(canvas, view, l);
            canvas.restore();
            return ret;
        }

        return super.drawChild(canvas, view, l);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            measureChild(child);
        }
        final int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(widthSpecSize, heightSpecSize);
    }

    public void measureChild(View child) {
        final float cellWidth = mCellWidth;
        final float cellHeight = mCellHeight;
        final BaseCellLayout.LayoutParams lp = (BaseCellLayout.LayoutParams) child.getLayoutParams();
        lp.setup(cellWidth, cellHeight);

        final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
        final int childheightMeasureSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        child.measure(childWidthMeasureSpec, childheightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final BaseCellLayout.LayoutParams lp = (BaseCellLayout.LayoutParams) child.getLayoutParams();
                final int childLeft = lp.x;
                final int childTop = lp.y;
                child.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);

                if (mOnLayoutChildListener != null) {
                    mOnLayoutChildListener.onLayoutChild(child);
                }
            }
        }
    }

    public void setupLp(BaseCellLayout.LayoutParams lp) {
        lp.setup(mCellWidth, mCellHeight);
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (child != null) {
            final Rect r = new Rect();
            child.getDrawingRect(r);
            requestRectangleOnScreen(r);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.cancelLongPress();
        }
    }

    @Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            view.setDrawingCacheEnabled(enabled);
            if (!view.isHardwareAccelerated() && enabled) {
                view.buildDrawingCache(true);
            }
        }
    }

    @Override
    protected void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        super.setChildrenDrawnWithCacheEnabled(enabled);
    }

    public interface OnLayoutChildListener {
        void onLayoutChild(View child);
    }

    private OnLayoutChildListener mOnLayoutChildListener;

    public void setOnLayoutChildListener(OnLayoutChildListener onLayoutChildListener) {
        mOnLayoutChildListener = onLayoutChildListener;
    }
}
