package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;

public class FolderCellLayout extends CellLayout {

    public FolderCellLayout(Context context) {
        this(context, null);
    }

    public FolderCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FolderCellLayout(Context context, AttributeSet attrs, int defStyle) {
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

        mReorderHintAnimationMagnitude = (REORDER_HINT_MAGNITUDE * mIconSize);

        mShortcutsAndWidgets.setCellDimensions(mfCellWidth, mfCellHeight, mCountX);
    }

    @Override
    public boolean addViewToCellLayout(View child, int index, int childId, LayoutParams params,
            boolean markCells) {

        final LayoutParams lp = params;

        child.setScaleX(getChildrenScale());
        child.setScaleY(getChildrenScale());

        // Generate an id for each view, this assumes we have at most 256x256 cells
        // per workspace screen
        if (lp.cellX >= 0 && lp.cellX <= mCountX - 1 && lp.cellY >= 0 && lp.cellY <= mCountY - 1) {
            // If the horizontal or vertical span is set to -1, it is taken to
            // mean that it spans the extent of the CellLayout
            if (lp.cellHSpan < 0) lp.cellHSpan = mCountX;
            if (lp.cellVSpan < 0) lp.cellVSpan = mCountY;

            child.setId(childId);

            mShortcutsAndWidgets.addView(child, index, lp);

            if (markCells) markCellsAsOccupiedForView(child);

            return true;
        }
        return false;
    }

    private static int calcIconSize(float cellWidth) {
        return (int) Math.floor(cellWidth * BASE_ICON_SIZE / (BASE_CELL_WIDTH * Workspace.ICON_SPAN));
    }

    private static int calcCellPaddingTop(float cellWidth) {
        return (int) Math.ceil(cellWidth * 30 / (BASE_CELL_WIDTH * Workspace.ICON_SPAN));
    }

    private static int calcCellTextPadding(float cellWidth) {
        return (int) Math.ceil(cellWidth * 24 / (BASE_CELL_WIDTH * Workspace.ICON_SPAN));
    }
}
