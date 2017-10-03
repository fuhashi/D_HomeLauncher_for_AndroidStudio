package jp.co.disney.apps.managed.kisekaeapp.launcher;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class PageEditCellLayout extends BaseCellLayout3 {

    public PageEditCellLayout(Context context) {
        this(context, null);
    }

    public PageEditCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageEditCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    int[] createArea(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX, int spanY,
            View dragView, int[] result, int resultSpan[], int mode, int numPage, int[] pageMap, String[] editedWpPaths) {
        // First we determine if things have moved enough to cause a different layout
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result);

        if (resultSpan == null) {
            resultSpan = new int[2];
        }

        // When we are checking drop validity or actually dropping, we don't recompute the
        // direction vector, since we want the solution to match the preview, and it's possible
        // that the exact position of the item has changed to result in a new reordering outcome.
        if ((mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL || mode == MODE_ACCEPT_DROP)
               && mPreviousReorderDirection[0] != INVALID_DIRECTION) {
            mDirectionVector[0] = mPreviousReorderDirection[0];
            mDirectionVector[1] = mPreviousReorderDirection[1];
            // We reset this vector after drop
            if (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                mPreviousReorderDirection[0] = INVALID_DIRECTION;
                mPreviousReorderDirection[1] = INVALID_DIRECTION;
            }
        } else {
            getDirectionVectorForDrop(pixelX, pixelY, spanX, spanY, dragView, mDirectionVector);
            mPreviousReorderDirection[0] = mDirectionVector[0];
            mPreviousReorderDirection[1] = mDirectionVector[1];
        }

        ItemConfiguration customSolution = findCustomSolution(pixelX, pixelY, minSpanX, minSpanY,
                 spanX,  spanY, dragView, new ItemConfiguration(), numPage);

        ItemConfiguration finalSolution = null;
        if (customSolution.isSolution) {
            finalSolution = customSolution;
        }

        boolean foundSolution = true;
        if (!DESTRUCTIVE_REORDER) {
            setUseTempCoords(true);
        }

        if (finalSolution != null) {
            result[0] = finalSolution.dragViewX;
            result[1] = finalSolution.dragViewY;
            resultSpan[0] = finalSolution.dragViewSpanX;
            resultSpan[1] = finalSolution.dragViewSpanY;

            // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
            // committing anything or animating anything as we just want to determine if a solution
            // exists
            if (mode == MODE_DRAG_OVER || mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL) {
                if (!DESTRUCTIVE_REORDER) {
                    copySolutionToTempState(finalSolution, dragView);
                }
                setItemPlacementDirty(true);
                animateItemsToSolution(finalSolution, dragView, mode == MODE_ON_DROP);

                if (!DESTRUCTIVE_REORDER &&
                        (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL)) {

                    ArrayList<int[]> changeMap = new ArrayList<int[]>();

                    final int childCount = mCellItemContainer.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View child = mCellItemContainer.getChildAt(i);
                        LayoutParams lp = (LayoutParams) child.getLayoutParams();

                        if (child == dragView) {
                            int oldIndex = lp.cellX + (lp.cellY * 3);
                            int newIndex = result[0] + (result[1] * 3);
                            changeMap.add(new int[] { oldIndex , newIndex });
                        }
                        else if (lp.cellX != lp.tmpCellX || lp.cellY != lp.tmpCellY) {
                            int oldIndex = lp.cellX + (lp.cellY * 3);
                            int newIndex = lp.tmpCellX + (lp.tmpCellY * 3);
                            changeMap.add(new int[] { oldIndex, newIndex });
                        }
                    }

                    int mapCount = changeMap.size();
                    if (mapCount > 0) {
                        int[] pageMapCopy = Arrays.copyOf(pageMap, pageMap.length);
                        String[] editedWpPathsCopy = Arrays.copyOf(editedWpPaths, editedWpPaths.length);

                        for (int i = 0; i < mapCount; i++) {
                            int[] map = changeMap.get(i);
                            pageMap[map[1]] = pageMapCopy[map[0]];
                            editedWpPaths[map[1]] = editedWpPathsCopy[map[0]];
                        }
                    }

                    commitTempPlacement();
                    completeAndClearReorderHintAnimations();
                    setItemPlacementDirty(false);
                } else {
                    beginOrAdjustHintAnimations(finalSolution, dragView,
                            REORDER_ANIMATION_DURATION);
                }
            }
        } else {
            foundSolution = false;
            result[0] = result[1] = resultSpan[0] = resultSpan[1] = -1;
        }

        if ((mode == MODE_ON_DROP || !foundSolution) && !DESTRUCTIVE_REORDER) {
            setUseTempCoords(false);
        }

        mCellItemContainer.requestLayout();
        return result;
    }

    protected ItemConfiguration findCustomSolution(int pixelX, int pixelY,
            int minSpanX, int minSpanY, int spanX, int spanY,
            View dragView, ItemConfiguration solution, int numPage) {

        copyCurrentStateToSolution(solution, false);

        int result[] = new int[2];
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result);
        if (result[0] < 0 || result[1] < 0) {
            solution.isSolution = false;
            return solution;
        }

        int targetIndex = result[0] + (result[1] * 3);
        if (targetIndex >= numPage) {
            solution.isSolution = false;
            return solution;
        }

        CellAndSpan cs = solution.map.get(dragView);
        int srcIndex = cs.x + (cs.y * 3);

        if (srcIndex == targetIndex) {
            solution.isSolution = false;
            return solution;
        }

        CellAndSpan cSrc = solution.map.get(dragView);
        cSrc.x = targetIndex % 3;
        cSrc.y = targetIndex / 3;

        solution.dragViewX = cSrc.x;
        solution.dragViewY = cSrc.y;
        solution.dragViewSpanX = cSrc.spanX;
        solution.dragViewSpanY = cSrc.spanY;

        if (srcIndex < targetIndex) {

            for (CellAndSpan c : solution.map.values()) {
                int index = c.x + (c.y * 3);
                if (index > srcIndex && index <= targetIndex) {
                    c.x = (index - 1) % 3;
                    c.y = (index - 1) / 3;
                }
            }

        } else {

            for (CellAndSpan c : solution.map.values()) {
                int index = c.x + (c.y * 3);
                if (index >= targetIndex && index < srcIndex) {
                    c.x = (index + 1) % 3;
                    c.y = (index + 1) / 3;
                }
            }
        }

        solution.isSolution = true;

        return solution;
    }
}
