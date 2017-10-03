package jp.co.disney.apps.managed.kisekaeapp.launcher;

import java.util.Stack;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;

public class DockCellLayout extends CellLayout {

    protected float mHotseatScale = 1f;

    public DockCellLayout(Context context) {
        this(context, null);
    }

    public DockCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DockCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        Point displaySize = new Point();
        Display display = mLauncher.getWindowManager().getDefaultDisplay();
        display.getSize(displaySize);

        mfCellWidth = displaySize.x / (float) mCountX;
        mCellWidth = (int) Math.floor(mfCellWidth);
        mfCellHeight = mfCellWidth * BASE_CELL_HEIGHT_HOTSEAT / BASE_CELL_WIDTH_HOTSEAT;
        mCellHeight = (int) Math.floor(mfCellHeight);

        mScale = displaySize.x / 1080f;
        mIconSize = calcIconSize(mfCellWidth);
        mCellPadTop = calcCellPaddingTop(mfCellWidth);
        mTextPad = calcCellTextPadding(mfCellWidth);
        mFolderIconSize = calcFolderIconSize(mfCellWidth);
        mFolderCellPadTop = calcFolderCellPaddingTop(mfCellWidth);

        mReorderHintAnimationMagnitude = (REORDER_HINT_MAGNITUDE * mIconSize);

        mShortcutsAndWidgets.setCellDimensions(mfCellWidth, mfCellHeight, mCountX);
    }

    @Override
    public float getChildrenScale() {
        return mHotseatScale;
    }

    @Override
    public boolean addViewToCellLayout(View child, int index, int childId, LayoutParams params,
            boolean markCells) {
        final LayoutParams lp = params;

        // Hotseat icons - remove text
        if (child instanceof BubbleTextView) {
            BubbleTextView bubbleChild = (BubbleTextView) child;
            bubbleChild.setShortcutLayout(mCellWidth * 2, mIconSize, mCellPadTop, mTextPad);
            bubbleChild.setTextColor(ContextCompat.getColor(getContext(), android.R.color.transparent));

        } else if (child instanceof FolderIcon) {
            FolderIcon folderIcon = (FolderIcon) child;
            folderIcon.setFolderLayout(mFolderIconSize , mFolderCellPadTop, mScale, mCellWidth * Workspace.ICON_SPAN, mCellPadTop);
        }

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
        return (int) Math.floor(cellWidth * BASE_ICON_SIZE / BASE_CELL_WIDTH_HOTSEAT);
    }

    private static int calcCellPaddingTop(float cellWidth) {
        return (int) Math.ceil(cellWidth * 36 / BASE_CELL_WIDTH_HOTSEAT);
    }

    private static int calcCellTextPadding(float cellWidth) {
        return (int) Math.ceil(cellWidth * 24 / BASE_CELL_WIDTH);
    }

    private static int calcFolderIconSize(float cellWidth) {
        return (int) Math.floor(cellWidth * BASE_FOLDER_ICON_SIZE / BASE_CELL_WIDTH_HOTSEAT);
    }

    private static int calcFolderCellPaddingTop(float cellWidth) {
        return (int) Math.ceil(cellWidth * 18 / BASE_CELL_WIDTH_HOTSEAT);
    }

    @Override
    int[] findNearestArea(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX, int spanY,
            View ignoreView, boolean ignoreOccupied, int[] result, int[] resultSpan,
            boolean[][] occupied) {
        lazyInitTempRectStack();
        // mark space take by ignoreView as available (method checks if ignoreView is null)
        markCellsAsUnoccupiedForView(ignoreView, occupied);

        // For items with a spanX / spanY > 1, the passed in point (pixelX, pixelY) corresponds
        // to the center of the item, but we are searching based on the top-left cell, so
        // we translate the point over to correspond to the top-left.
        pixelX -= Math.floor(mfCellWidth * (spanX - 1)) / 2f;
        pixelY -= Math.floor(mfCellHeight * (spanY - 1)) / 2f;

        // Keep track of best-scoring drop area
        final int[] bestXY = result != null ? result : new int[2];
        double bestDistance = Double.MAX_VALUE;
        final Rect bestRect = new Rect(-1, -1, -1, -1);
        final Stack<Rect> validRegions = new Stack<Rect>();

        final int countX = mCountX;
        final int countY = mCountY;

        if (minSpanX <= 0 || minSpanY <= 0 || spanX <= 0 || spanY <= 0 ||
                spanX < minSpanX || spanY < minSpanY) {
            return bestXY;
        }

        for (int y = 0; y < countY - (minSpanY - 1); y += Workspace.ICON_SPAN) {
            inner:
            for (int x = 0; x < countX - (minSpanX - 1); x += Workspace.ICON_SPAN) {
                int ySize = -1;
                int xSize = -1;
                if (ignoreOccupied) {
                    // First, let's see if this thing fits anywhere
                    for (int i = 0; i < minSpanX; i++) {
                        for (int j = 0; j < minSpanY; j++) {
                            if (occupied[x + i][y + j]) {
                                continue inner;
                            }
                        }
                    }
                    xSize = minSpanX;
                    ySize = minSpanY;

                    // We know that the item will fit at _some_ acceptable size, now let's see
                    // how big we can make it. We'll alternate between incrementing x and y spans
                    // until we hit a limit.
                    boolean incX = true;
                    boolean hitMaxX = xSize >= spanX;
                    boolean hitMaxY = ySize >= spanY;
                    while (!(hitMaxX && hitMaxY)) {
                        if (incX && !hitMaxX) {
                            for (int j = 0; j < ySize; j++) {
                                if (x + xSize > countX -1 || occupied[x + xSize][y + j]) {
                                    // We can't move out horizontally
                                    hitMaxX = true;
                                }
                            }
                            if (!hitMaxX) {
                                xSize++;
                            }
                        } else if (!hitMaxY) {
                            for (int i = 0; i < xSize; i++) {
                                if (y + ySize > countY - 1 || occupied[x + i][y + ySize]) {
                                    // We can't move out vertically
                                    hitMaxY = true;
                                }
                            }
                            if (!hitMaxY) {
                                ySize++;
                            }
                        }
                        hitMaxX |= xSize >= spanX;
                        hitMaxY |= ySize >= spanY;
                        incX = !incX;
                    }
                    incX = true;
                    hitMaxX = xSize >= spanX;
                    hitMaxY = ySize >= spanY;
                }
                final int[] cellXY = mTmpXY;
                cellToCenterPoint(x, y, cellXY);

                // We verify that the current rect is not a sub-rect of any of our previous
                // candidates. In this case, the current rect is disqualified in favour of the
                // containing rect.
                Rect currentRect = mTempRectStack.pop();
                currentRect.set(x, y, x + xSize, y + ySize);
                boolean contained = false;
                for (Rect r : validRegions) {
                    if (r.contains(currentRect)) {
                        contained = true;
                        break;
                    }
                }
                validRegions.push(currentRect);
                double distance = Math.sqrt(Math.pow(cellXY[0] - pixelX, 2)
                        + Math.pow(cellXY[1] - pixelY, 2));

                if ((distance <= bestDistance && !contained) ||
                        currentRect.contains(bestRect)) {
                    bestDistance = distance;
                    bestXY[0] = x;
                    bestXY[1] = y;
                    if (resultSpan != null) {
                        resultSpan[0] = xSize;
                        resultSpan[1] = ySize;
                    }
                    bestRect.set(currentRect);
                }
            }
        }
        // re-mark space taken by ignoreView as occupied
        markCellsAsOccupiedForView(ignoreView, occupied);

        // Return -1, -1 if no suitable location found
        if (bestDistance == Double.MAX_VALUE) {
            bestXY[0] = -1;
            bestXY[1] = -1;
        }
        recycleTempRects(validRegions);
        return bestXY;
    }
}
