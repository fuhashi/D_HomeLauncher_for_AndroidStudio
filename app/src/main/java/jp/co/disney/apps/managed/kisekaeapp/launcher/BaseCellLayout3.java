package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class BaseCellLayout3 extends BaseCellLayout2 {

    protected final Rect mRect = new Rect();
    protected final CellInfo mCellInfo = new CellInfo();

    protected boolean mLastDownOnOccupiedCell = false;

    protected boolean mDragging = false;
    protected final int[] mDragCell = new int[2];

    protected OnTouchListener mInterceptTouchListener;

    public BaseCellLayout3(Context context) {
        this(context, null);
    }

    public BaseCellLayout3(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseCellLayout3(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnInterceptTouchListener(View.OnTouchListener listener) {
        mInterceptTouchListener = listener;
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mCellInfo.screen = ((ViewGroup) getParent()).indexOfChild(this);
    }

    public void setTagToCellInfoForPoint(int touchX, int touchY) {
        final CellInfo cellInfo = mCellInfo;
        Rect frame = mRect;
        final int x = touchX + getScrollX();
        final int y = touchY + getScrollY();
        final int count = mCellItemContainer.getChildCount();

        boolean found = false;
        for (int i = count - 1; i >= 0; i--) {
            final View child = mCellItemContainer.getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if ((child.getVisibility() == VISIBLE || child.getAnimation() != null) &&
                    lp.isLockedToGrid) {
                child.getHitRect(frame);

                float scale = child.getScaleX();
                frame = new Rect(child.getLeft(), child.getTop(), child.getRight(),
                        child.getBottom());
                // The child hit rect is relative to the CellLayoutChildren parent, so we need to
                // offset that by this CellLayout's padding to test an (x,y) point that is relative
                // to this view.
                frame.offset(getPaddingLeft(), getPaddingTop());
                frame.inset((int) (frame.width() * (1f - scale) / 2),
                        (int) (frame.height() * (1f - scale) / 2));

                if (frame.contains(x, y)) {
                    cellInfo.cell = child;
                    cellInfo.cellX = lp.cellX;
                    cellInfo.cellY = lp.cellY;
                    cellInfo.spanX = lp.cellHSpan;
                    cellInfo.spanY = lp.cellVSpan;
                    found = true;
                    break;
                }
            }
        }

        mLastDownOnOccupiedCell = found;

        if (!found) {
            final int cellXY[] = mTmpXY;
            pointToCellExact(x, y, cellXY);

            cellInfo.cell = null;
            cellInfo.cellX = cellXY[0];
            cellInfo.cellY = cellXY[1];
            cellInfo.spanX = 1;
            cellInfo.spanY = 1;
        }

        setTag(cellInfo);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // First we clear the tag to ensure that on every touch down we start with a fresh slate,
        // even in the case where we return early. Not clearing here was causing bugs whereby on
        // long-press we'd end up picking up an item from a previous drag operation.
        final int action = ev.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            clearTagCellInfo();
        }

        if (mInterceptTouchListener != null && mInterceptTouchListener.onTouch(this, ev)) {
            return true;
        }

        if (action == MotionEvent.ACTION_DOWN) {
            setTagToCellInfoForPoint((int) ev.getX(), (int) ev.getY());
        }

        return false;
    }

    public void prepareChildForDrag(View child) {
        markCellsAsUnoccupiedForView(child);
    }

    /**
     * A drag event has begun over this layout.
     * It may have begun over this layout (in which case onDragChild is called first),
     * or it may have begun on another layout.
     */
    void onDragEnter() {
//        mDragEnforcer.onDragEnter();
        mDragging = true;
    }

    /**
     * Called when drag has left this CellLayout or has been completed (successfully or not)
     */
    void onDragExit() {
//        mDragEnforcer.onDragExit();
        // This can actually be called when we aren't in a drag, e.g. when adding a new
        // item to this layout via the customize drawer.
        // Guard against that case.
        if (mDragging) {
            mDragging = false;
        }

        // Invalidate the drag data
        mDragCell[0] = mDragCell[1] = -1;
//        mDragOutlineAnims[mDragOutlineCurrent].animateOut();
//        mDragOutlineCurrent = (mDragOutlineCurrent + 1) % mDragOutlineAnims.length;
        revertTempState();
    }

    /**
     * Mark a child as having been dropped.
     * At the beginning of the drag operation, the child may have been on another
     * screen, but it is re-parented before this method is called.
     *
     * @param child The child that is being dropped
     */
    void onDropChild(View child) {
        if (child != null) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.dropped = true;
            child.requestLayout();
        }
    }

    protected void clearTagCellInfo() {
        final CellInfo cellInfo = mCellInfo;
        cellInfo.cell = null;
        cellInfo.cellX = -1;
        cellInfo.cellY = -1;
        cellInfo.spanX = 0;
        cellInfo.spanY = 0;
        setTag(cellInfo);
    }

    public CellInfo getTag() {
        return (CellInfo) super.getTag();
    }

    public boolean lastDownOnOccupiedCell() {
        return mLastDownOnOccupiedCell;
    }

    // This class stores info for two purposes:
    // 1. When dragging items (mDragInfo in Workspace), we store the View, its cellX & cellY,
    //    its spanX, spanY, and the screen it is on
    // 2. When long clicking on an empty cell in a CellLayout, we save information about the
    //    cellX and cellY coordinates and which page was clicked. We then set this as a tag on
    //    the CellLayout that was long clicked
    static final class CellInfo {
        View cell;
        int cellX = -1;
        int cellY = -1;
        int spanX;
        int spanY;
        int screen;
        long container;

        @Override
        public String toString() {
            return "Cell[view=" + (cell == null ? "null" : cell.getClass())
                    + ", x=" + cellX + ", y=" + cellY + "]";
        }
    }
}
