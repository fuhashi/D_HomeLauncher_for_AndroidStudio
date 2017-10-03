package jp.co.disney.apps.managed.kisekaeapp.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Stack;

import jp.co.disney.apps.managed.kisekaeapp.system.view.animation.ViewAnimUtils;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class BaseCellLayout2 extends BaseCellLayout {

    public static final int MODE_DRAG_OVER = 0;
    public static final int MODE_ON_DROP = 1;
    public static final int MODE_ON_DROP_EXTERNAL = 2;
    public static final int MODE_ACCEPT_DROP = 3;

    protected static final boolean DESTRUCTIVE_REORDER = false;
    protected static final int INVALID_DIRECTION = -100;
    protected static final int REORDER_ANIMATION_DURATION = 300;

    protected boolean[][] mTmpOccupied;
    protected int[] mTempLocation = new int[2];
    protected final int[] mTmpXY = new int[2];

    protected ArrayList<View> mIntersectingViews = new ArrayList<View>();
    protected Rect mOccupiedRect = new Rect();
    protected int[] mDirectionVector = new int[2];
    protected int[] mPreviousReorderDirection = new int[2];

    protected HashMap<LayoutParams, Animator> mReorderAnimators = new
            HashMap<LayoutParams, Animator>();
    protected HashMap<View, ReorderHintAnimation>
            mShakeAnimators = new HashMap<View, ReorderHintAnimation>();

    protected float mReorderHintAnimationMagnitude;

    protected boolean mItemPlacementDirty = false;

    protected final Stack<Rect> mTempRectStack = new Stack<Rect>();
    protected void lazyInitTempRectStack() {
        if (mTempRectStack.isEmpty()) {
            for (int i = 0; i < mCountX * mCountY; i++) {
                mTempRectStack.push(new Rect());
            }
        }
    }

    public void setReorderHintAnimationMagnitude(float val) {
        mReorderHintAnimationMagnitude = val;
    }

    public BaseCellLayout2(Context context) {
        this(context, null);
    }

    public BaseCellLayout2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseCellLayout2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mTmpOccupied = new boolean[mCountX][mCountY];
    }

    @Override
    public void updateGridSize(int x, int y) {
        mTmpOccupied = new boolean[x][y];
        mTempRectStack.clear();
        super.updateGridSize(x, y);
    }

    boolean createAreaForResize(int cellX, int cellY, int spanX, int spanY,
            View dragView, int[] direction, boolean commit) {
        int[] pixelXY = new int[2];
        regionToCenterPoint(cellX, cellY, spanX, spanY, pixelXY);

        // First we determine if things have moved enough to cause a different layout
        ItemConfiguration swapSolution = simpleSwap(pixelXY[0], pixelXY[1], spanX, spanY,
                 spanX,  spanY, direction, dragView,  true,  new ItemConfiguration());

        setUseTempCoords(true);
        if (swapSolution != null && swapSolution.isSolution) {
            // If we're just testing for a possible location (MODE_ACCEPT_DROP), we don't bother
            // committing anything or animating anything as we just want to determine if a solution
            // exists
            copySolutionToTempState(swapSolution, dragView);
            setItemPlacementDirty(true);
            animateItemsToSolution(swapSolution, dragView, commit);

            if (commit) {
                commitTempPlacement();
                completeAndClearReorderHintAnimations();
                setItemPlacementDirty(false);
            } else {
                beginOrAdjustHintAnimations(swapSolution, dragView,
                        REORDER_ANIMATION_DURATION);
            }
            mCellItemContainer.requestLayout();
        }
        return swapSolution.isSolution;
    }

    int[] createArea(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX, int spanY,
            View dragView, int[] result, int resultSpan[], int mode) {
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

        ItemConfiguration swapSolution = simpleSwap(pixelX, pixelY, minSpanX, minSpanY,
                 spanX,  spanY, mDirectionVector, dragView,  true,  new ItemConfiguration());

        // We attempt the approach which doesn't shuffle views at all
        ItemConfiguration noShuffleSolution = findConfigurationNoShuffle(pixelX, pixelY, minSpanX,
                minSpanY, spanX, spanY, dragView, new ItemConfiguration());

        ItemConfiguration finalSolution = null;
        if (swapSolution.isSolution && swapSolution.area() >= noShuffleSolution.area()) {
            finalSolution = swapSolution;
        } else if (noShuffleSolution.isSolution) {
            finalSolution = noShuffleSolution;
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

    protected void completeAndClearReorderHintAnimations() {
        for (ReorderHintAnimation a: mShakeAnimators.values()) {
            a.completeAnimationImmediately();
        }
        mShakeAnimators.clear();
    }

    protected void commitTempPlacement() {
        for (int i = 0; i < mCountX; i++) {
            for (int j = 0; j < mCountY; j++) {
                mOccupied[i][j] = mTmpOccupied[i][j];
            }
        }
        int childCount = mCellItemContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mCellItemContainer.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            ItemInfo info = (ItemInfo) child.getTag();
            // We do a null check here because the item info can be null in the case of the
            // AllApps button in the hotseat.
            if (info != null) {
                if (info.cellX != lp.tmpCellX || info.cellY != lp.tmpCellY ||
                        info.spanX != lp.cellHSpan || info.spanY != lp.cellVSpan) {
                    info.requiresDbUpdate = true;
                }
                info.cellX = lp.cellX = lp.tmpCellX;
                info.cellY = lp.cellY = lp.tmpCellY;
                info.spanX = lp.cellHSpan;
                info.spanY = lp.cellVSpan;
            } else {
                lp.cellX = lp.tmpCellX;
                lp.cellY = lp.tmpCellY;
            }
        }
    }

    void revertTempState() {
        if (!isItemPlacementDirty() || DESTRUCTIVE_REORDER) return;
        final int count = mCellItemContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = mCellItemContainer.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.cellY) {
                lp.tmpCellX = lp.cellX;
                lp.tmpCellY = lp.cellY;
                animateChildToPosition(child, lp.cellX, lp.cellY, REORDER_ANIMATION_DURATION,
                        0, false, false);
            }
        }
        completeAndClearReorderHintAnimations();
        setItemPlacementDirty(false);
    }

    boolean isNearestDropLocationOccupied(int pixelX, int pixelY, int spanX, int spanY,
            View dragView, int[] result) {
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result);
        getViewsIntersectingRegion(result[0], result[1], spanX, spanY, dragView, null,
                mIntersectingViews);
        return !mIntersectingViews.isEmpty();
    }

    ItemConfiguration findConfigurationNoShuffle(int pixelX, int pixelY, int minSpanX, int minSpanY,
            int spanX, int spanY, View dragView, ItemConfiguration solution) {
        int[] result = new int[2];
        int[] resultSpan = new int[2];
        findNearestVacantArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, null, result,
                resultSpan);
        if (result[0] >= 0 && result[1] >= 0) {
            copyCurrentStateToSolution(solution, false);
            solution.dragViewX = result[0];
            solution.dragViewY = result[1];
            solution.dragViewSpanX = resultSpan[0];
            solution.dragViewSpanY = resultSpan[1];
            solution.isSolution = true;
        } else {
            solution.isSolution = false;
        }
        return solution;
    }

    /* This seems like it should be obvious and straight-forward, but when the direction vector
    needs to match with the notion of the dragView pushing other views, we have to employ
    a slightly more subtle notion of the direction vector. The question is what two points is
    the vector between? The center of the dragView and its desired destination? Not quite, as
    this doesn't necessarily coincide with the interaction of the dragView and items occupying
    those cells. Instead we use some heuristics to often lock the vector to up, down, left
    or right, which helps make pushing feel right.
    */
    protected void getDirectionVectorForDrop(int dragViewCenterX, int dragViewCenterY, int spanX,
            int spanY, View dragView, int[] resultDirection) {
        int[] targetDestination = new int[2];

        findNearestArea(dragViewCenterX, dragViewCenterY, spanX, spanY, targetDestination);
        Rect dragRect = new Rect();
        regionToRect(targetDestination[0], targetDestination[1], spanX, spanY, dragRect);
        dragRect.offset(dragViewCenterX - dragRect.centerX(), dragViewCenterY - dragRect.centerY());

        Rect dropRegionRect = new Rect();
        getViewsIntersectingRegion(targetDestination[0], targetDestination[1], spanX, spanY,
                dragView, dropRegionRect, mIntersectingViews);

        int dropRegionSpanX = dropRegionRect.width();
        int dropRegionSpanY = dropRegionRect.height();

        regionToRect(dropRegionRect.left, dropRegionRect.top, dropRegionRect.width(),
                dropRegionRect.height(), dropRegionRect);

        int deltaX = (dropRegionRect.centerX() - dragViewCenterX) / spanX;
        int deltaY = (dropRegionRect.centerY() - dragViewCenterY) / spanY;

        if (dropRegionSpanX == mCountX || spanX == mCountX) {
            deltaX = 0;
        }
        if (dropRegionSpanY == mCountY || spanY == mCountY) {
            deltaY = 0;
        }

        if (deltaX == 0 && deltaY == 0) {
            // No idea what to do, give a random direction.
            resultDirection[0] = 1;
            resultDirection[1] = 0;
        } else {
            computeDirectionVector(deltaX, deltaY, resultDirection);
        }
    }

    // For a given cell and span, fetch the set of views intersecting the region.
    protected void getViewsIntersectingRegion(int cellX, int cellY, int spanX, int spanY,
            View dragView, Rect boundingRect, ArrayList<View> intersectingViews) {
        if (boundingRect != null) {
            boundingRect.set(cellX, cellY, cellX + spanX, cellY + spanY);
        }
        intersectingViews.clear();
        Rect r0 = new Rect(cellX, cellY, cellX + spanX, cellY + spanY);
        Rect r1 = new Rect();
        final int count = mCellItemContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = mCellItemContainer.getChildAt(i);
            if (child == dragView) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            r1.set(lp.cellX, lp.cellY, lp.cellX + lp.cellHSpan, lp.cellY + lp.cellVSpan);
            if (Rect.intersects(r0, r1)) {
                mIntersectingViews.add(child);
                if (boundingRect != null) {
                    boundingRect.union(r1);
                }
            }
        }
    }

    void setItemPlacementDirty(boolean dirty) {
        mItemPlacementDirty = dirty;
    }
    boolean isItemPlacementDirty() {
        return mItemPlacementDirty;
    }

    public void setUseTempCoords(boolean useTempCoords) {
        final int childCount = mCellItemContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            LayoutParams lp = (LayoutParams) mCellItemContainer.getChildAt(i).getLayoutParams();
            lp.useTmpCoords = useTempCoords;
        }
    }

    ItemConfiguration simpleSwap(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX,
            int spanY, int[] direction, View dragView, boolean decX, ItemConfiguration solution) {
        // Copy the current state into the solution. This solution will be manipulated as necessary.
        copyCurrentStateToSolution(solution, false);
        // Copy the current occupied array into the temporary occupied array. This array will be
        // manipulated as necessary to find a solution.
        copyOccupiedArray(mTmpOccupied);

        // We find the nearest cell into which we would place the dragged item, assuming there's
        // nothing in its way.
        int result[] = new int[2];
        result = findNearestArea(pixelX, pixelY, spanX, spanY, result);

        boolean success = false;
        // First we try the exact nearest position of the item being dragged,
        // we will then want to try to move this around to other neighbouring positions
        success = rearrangementExists(result[0], result[1], spanX, spanY, direction, dragView,
                solution);

        if (!success) {
            // We try shrinking the widget down to size in an alternating pattern, shrink 1 in
            // x, then 1 in y etc.
            if (spanX > minSpanX && (minSpanY == spanY || decX)) {
                return simpleSwap(pixelX, pixelY, minSpanX, minSpanY, spanX - 1, spanY, direction,
                        dragView, false, solution);
            } else if (spanY > minSpanY) {
                return simpleSwap(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY - 1, direction,
                        dragView, true, solution);
            }
            solution.isSolution = false;
        } else {
            solution.isSolution = true;
            solution.dragViewX = result[0];
            solution.dragViewY = result[1];
            solution.dragViewSpanX = spanX;
            solution.dragViewSpanY = spanY;
        }
        return solution;
    }

    protected void copyCurrentStateToSolution(ItemConfiguration solution, boolean temp) {
        int childCount = mCellItemContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mCellItemContainer.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            CellAndSpan c;
            if (temp) {
                c = new CellAndSpan(lp.tmpCellX, lp.tmpCellY, lp.cellHSpan, lp.cellVSpan);
            } else {
                c = new CellAndSpan(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan);
            }
            solution.add(child, c);
        }
    }

    protected void copySolutionToTempState(ItemConfiguration solution, View dragView) {
        for (int i = 0; i < mCountX; i++) {
            for (int j = 0; j < mCountY; j++) {
                mTmpOccupied[i][j] = false;
            }
        }

        int childCount = mCellItemContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mCellItemContainer.getChildAt(i);
            if (child == dragView) continue;
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            CellAndSpan c = solution.map.get(child);
            if (c != null) {
                lp.tmpCellX = c.x;
                lp.tmpCellY = c.y;
                lp.cellHSpan = c.spanX;
                lp.cellVSpan = c.spanY;
                markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
            }
        }
        markCellsForView(solution.dragViewX, solution.dragViewY, solution.dragViewSpanX,
                solution.dragViewSpanY, mTmpOccupied, true);
    }

    protected void animateItemsToSolution(ItemConfiguration solution, View dragView, boolean
            commitDragView) {

        boolean[][] occupied = DESTRUCTIVE_REORDER ? mOccupied : mTmpOccupied;
        for (int i = 0; i < mCountX; i++) {
            for (int j = 0; j < mCountY; j++) {
                occupied[i][j] = false;
            }
        }

        int childCount = mCellItemContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mCellItemContainer.getChildAt(i);
            if (child == dragView) continue;
            CellAndSpan c = solution.map.get(child);
            if (c != null) {
                animateChildToPosition(child, c.x, c.y, REORDER_ANIMATION_DURATION, 0,
                        DESTRUCTIVE_REORDER, false);
                markCellsForView(c.x, c.y, c.spanX, c.spanY, occupied, true);
            }
        }
        if (commitDragView) {
            markCellsForView(solution.dragViewX, solution.dragViewY, solution.dragViewSpanX,
                    solution.dragViewSpanY, occupied, true);
        }
    }

    public boolean animateChildToPosition(final View child, int cellX, int cellY, int duration,
            int delay, boolean permanent, boolean adjustOccupied) {
        CellItemContainer clc = mCellItemContainer;
        boolean[][] occupied = mOccupied;
        if (!permanent) {
            occupied = mTmpOccupied;
        }

        if (clc.indexOfChild(child) != -1) {
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final ItemInfo info = (ItemInfo) child.getTag();

            // We cancel any existing animations
            if (mReorderAnimators.containsKey(lp)) {
                mReorderAnimators.get(lp).cancel();
                mReorderAnimators.remove(lp);
            }

            final int oldX = lp.x;
            final int oldY = lp.y;
            if (adjustOccupied) {
                occupied[lp.cellX][lp.cellY] = false;
                occupied[cellX][cellY] = true;
            }
            lp.isLockedToGrid = true;
            if (permanent) {
                if (info != null) {
                    info.cellX = cellX;
                    info.cellY = cellY;
                }
                lp.cellX = cellX;
                lp.cellY = cellY;
            } else {
                lp.tmpCellX = cellX;
                lp.tmpCellY = cellY;
            }
            clc.setupLp(lp);
            lp.isLockedToGrid = false;
            final int newX = lp.x;
            final int newY = lp.y;

            lp.x = oldX;
            lp.y = oldY;

            // Exit early if we're not actually moving the view
            if (oldX == newX && oldY == newY) {
                lp.isLockedToGrid = true;
                return true;
            }

            ValueAnimator va = ViewAnimUtils.ofFloat(child, 0f, 1f);
            va.setDuration(duration);
            mReorderAnimators.put(lp, va);

            va.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {

                    float r = ((Float) animation.getAnimatedValue()).floatValue();
                    lp.x = (int) ((1 - r) * oldX + r * newX);
                    lp.y = (int) ((1 - r) * oldY + r * newY);
                    child.requestLayout();
                }
            });
            va.addListener(new AnimatorListenerAdapter() {
                boolean cancelled = false;
                public void onAnimationEnd(Animator animation) {
                    // If the animation was cancelled, it means that another animation
                    // has interrupted this one, and we don't want to lock the item into
                    // place just yet.
                    if (!cancelled) {
                        lp.isLockedToGrid = true;
                        child.requestLayout();
                    }
                    if (mReorderAnimators.containsKey(lp)) {
                        mReorderAnimators.remove(lp);
                    }
                }
                public void onAnimationCancel(Animator animation) {
                    cancelled = true;
                }
            });
            va.setStartDelay(delay);
            va.start();
            return true;
        }
        return false;
    }

    // This method starts or changes the reorder hint animations
    protected void beginOrAdjustHintAnimations(ItemConfiguration solution, View dragView, int delay) {
        int childCount = mCellItemContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mCellItemContainer.getChildAt(i);
            if (child == dragView) continue;
            CellAndSpan c = solution.map.get(child);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (c != null) {
                ReorderHintAnimation rha = new ReorderHintAnimation(child, lp.cellX, lp.cellY,
                        c.x, c.y, c.spanX, c.spanY);
                rha.animate();
            }
        }
    }

    protected void copyOccupiedArray(boolean[][] occupied) {
        for (int i = 0; i < mCountX; i++) {
            for (int j = 0; j < mCountY; j++) {
                occupied[i][j] = mOccupied[i][j];
            }
        }
    }

    protected boolean rearrangementExists(int cellX, int cellY, int spanX, int spanY, int[] direction,
            View ignoreView, ItemConfiguration solution) {
        // Return early if get invalid cell positions
        if (cellX < 0 || cellY < 0) return false;

        mIntersectingViews.clear();
        mOccupiedRect.set(cellX, cellY, cellX + spanX, cellY + spanY);

        // Mark the desired location of the view currently being dragged.
        if (ignoreView != null) {
            CellAndSpan c = solution.map.get(ignoreView);
            if (c != null) {
                c.x = cellX;
                c.y = cellY;
            }
        }
        Rect r0 = new Rect(cellX, cellY, cellX + spanX, cellY + spanY);
        Rect r1 = new Rect();
        for (View child: solution.map.keySet()) {
            if (child == ignoreView) continue;
            CellAndSpan c = solution.map.get(child);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            r1.set(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            if (Rect.intersects(r0, r1)) {
                if (!lp.canReorder) {
                    return false;
                }
                mIntersectingViews.add(child);
            }
        }

        // First we try to find a solution which respects the push mechanic. That is,
        // we try to find a solution such that no displaced item travels through another item
        // without also displacing that item.
        if (attemptPushInDirection(mIntersectingViews, mOccupiedRect, direction, ignoreView,
                solution)) {
            return true;
        }

        // Next we try moving the views as a block, but without requiring the push mechanic.
        if (addViewsToTempLocation(mIntersectingViews, mOccupiedRect, direction, ignoreView,
                solution)) {
            return true;
        }

        // Ok, they couldn't move as a block, let's move them individually
        for (View v : mIntersectingViews) {
            if (!addViewToTempLocation(v, mOccupiedRect, direction, solution)) {
                return false;
            }
        }
        return true;
    }

    // This method tries to find a reordering solution which satisfies the push mechanic by trying
    // to push items in each of the cardinal directions, in an order based on the direction vector
    // passed.
    protected boolean attemptPushInDirection(ArrayList<View> intersectingViews, Rect occupied,
            int[] direction, View ignoreView, ItemConfiguration solution) {
        if ((Math.abs(direction[0]) + Math.abs(direction[1])) > 1) {
            // If the direction vector has two non-zero components, we try pushing
            // separately in each of the components.
            int temp = direction[1];
            direction[1] = 0;

            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            direction[1] = temp;
            temp = direction[0];
            direction[0] = 0;

            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Revert the direction
            direction[0] = temp;

            // Now we try pushing in each component of the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            temp = direction[1];
            direction[1] = 0;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }

            direction[1] = temp;
            temp = direction[0];
            direction[0] = 0;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // revert the direction
            direction[0] = temp;
            direction[0] *= -1;
            direction[1] *= -1;

        } else {
            // If the direction vector has a single non-zero component, we push first in the
            // direction of the vector
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Then we try the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Switch the direction back
            direction[0] *= -1;
            direction[1] *= -1;

            // If we have failed to find a push solution with the above, then we try
            // to find a solution by pushing along the perpendicular axis.

            // Swap the components
            int temp = direction[1];
            direction[1] = direction[0];
            direction[0] = temp;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }

            // Then we try the opposite direction
            direction[0] *= -1;
            direction[1] *= -1;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction,
                    ignoreView, solution)) {
                return true;
            }
            // Switch the direction back
            direction[0] *= -1;
            direction[1] *= -1;

            // Swap the components back
            temp = direction[1];
            direction[1] = direction[0];
            direction[0] = temp;
        }
        return false;
    }

    protected boolean pushViewsToTempLocation(ArrayList<View> views, Rect rectOccupiedByPotentialDrop,
            int[] direction, View dragView, ItemConfiguration currentState) {

        ViewCluster cluster = new ViewCluster(views, currentState);
        Rect clusterRect = cluster.getBoundingRect();
        int whichEdge;
        int pushDistance;
        boolean fail = false;

        // Determine the edge of the cluster that will be leading the push and how far
        // the cluster must be shifted.
        if (direction[0] < 0) {
            whichEdge = ViewCluster.LEFT;
            pushDistance = clusterRect.right - rectOccupiedByPotentialDrop.left;
        } else if (direction[0] > 0) {
            whichEdge = ViewCluster.RIGHT;
            pushDistance = rectOccupiedByPotentialDrop.right - clusterRect.left;
        } else if (direction[1] < 0) {
            whichEdge = ViewCluster.TOP;
            pushDistance = clusterRect.bottom - rectOccupiedByPotentialDrop.top;
        } else {
            whichEdge = ViewCluster.BOTTOM;
            pushDistance = rectOccupiedByPotentialDrop.bottom - clusterRect.top;
        }

        // Break early for invalid push distance.
        if (pushDistance <= 0) {
            return false;
        }

        // Mark the occupied state as false for the group of views we want to move.
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        }

        // We save the current configuration -- if we fail to find a solution we will revert
        // to the initial state. The process of finding a solution modifies the configuration
        // in place, hence the need for revert in the failure case.
        currentState.save();

        // The pushing algorithm is simplified by considering the views in the order in which
        // they would be pushed by the cluster. For example, if the cluster is leading with its
        // left edge, we consider sort the views by their right edge, from right to left.
        cluster.sortConfigurationForEdgePush(whichEdge);

        while (pushDistance > 0 && !fail) {
            for (View v: currentState.sortedViews) {
                // For each view that isn't in the cluster, we see if the leading edge of the
                // cluster is contacting the edge of that view. If so, we add that view to the
                // cluster.
                if (!cluster.views.contains(v) && v != dragView) {
                    if (cluster.isViewTouchingEdge(v, whichEdge)) {
                        LayoutParams lp = (LayoutParams) v.getLayoutParams();
                        if (!lp.canReorder) {
                            // The push solution includes the all apps button, this is not viable.
                            fail = true;
                            break;
                        }
                        cluster.addView(v);
                        CellAndSpan c = currentState.map.get(v);

                        // Adding view to cluster, mark it as not occupied.
                        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
                    }
                }
            }
            pushDistance--;

            // The cluster has been completed, now we move the whole thing over in the appropriate
            // direction.
            cluster.shift(whichEdge, 1);
        }

        boolean foundSolution = false;
        clusterRect = cluster.getBoundingRect();

        // Due to the nature of the algorithm, the only check required to verify a valid solution
        // is to ensure that completed shifted cluster lies completely within the cell layout.
        if (!fail && clusterRect.left >= 0 && clusterRect.right <= mCountX && clusterRect.top >= 0 &&
                clusterRect.bottom <= mCountY) {
            foundSolution = true;
        } else {
            currentState.restore();
        }

        // In either case, we set the occupied array as marked for the location of the views
        for (View v: cluster.views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        }

        return foundSolution;
    }

    protected boolean addViewToTempLocation(View v, Rect rectOccupiedByPotentialDrop,
            int[] direction, ItemConfiguration currentState) {
        CellAndSpan c = currentState.map.get(v);
        boolean success = false;
        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        markCellsForRect(rectOccupiedByPotentialDrop, mTmpOccupied, true);

        findNearestArea(c.x, c.y, c.spanX, c.spanY, direction, mTmpOccupied, null, mTempLocation);

        if (mTempLocation[0] >= 0 && mTempLocation[1] >= 0) {
            c.x = mTempLocation[0];
            c.y = mTempLocation[1];
            success = true;
        }
        markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        return success;
    }

    private boolean addViewsToTempLocation(ArrayList<View> views, Rect rectOccupiedByPotentialDrop,
            int[] direction, View dragView, ItemConfiguration currentState) {
        if (views.size() == 0) return true;

        boolean success = false;
        Rect boundingRect = null;
        // We construct a rect which represents the entire group of views passed in
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            if (boundingRect == null) {
                boundingRect = new Rect(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            } else {
                boundingRect.union(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            }
        }

        // Mark the occupied state as false for the group of views we want to move.
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, false);
        }

        boolean[][] blockOccupied = new boolean[boundingRect.width()][boundingRect.height()];
        int top = boundingRect.top;
        int left = boundingRect.left;
        // We mark more precisely which parts of the bounding rect are truly occupied, allowing
        // for interlocking.
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x - left, c.y - top, c.spanX, c.spanY, blockOccupied, true);
        }

        markCellsForRect(rectOccupiedByPotentialDrop, mTmpOccupied, true);

        findNearestArea(boundingRect.left, boundingRect.top, boundingRect.width(),
                boundingRect.height(), direction, mTmpOccupied, blockOccupied, mTempLocation);

        // If we successfuly found a location by pushing the block of views, we commit it
        if (mTempLocation[0] >= 0 && mTempLocation[1] >= 0) {
            int deltaX = mTempLocation[0] - boundingRect.left;
            int deltaY = mTempLocation[1] - boundingRect.top;
            for (View v: views) {
                CellAndSpan c = currentState.map.get(v);
                c.x += deltaX;
                c.y += deltaY;
            }
            success = true;
        }

        // In either case, we set the occupied array as marked for the location of the views
        for (View v: views) {
            CellAndSpan c = currentState.map.get(v);
            markCellsForView(c.x, c.y, c.spanX, c.spanY, mTmpOccupied, true);
        }
        return success;
    }

    protected void markCellsForRect(Rect r, boolean[][] occupied, boolean value) {
        markCellsForView(r.left, r.top, r.width(), r.height(), occupied, value);
    }

    /**
     * Find a starting cell position that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreView Considers space occupied by this view as unoccupied
     * @param result Previously returned value to possibly recycle.
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    int[] findNearestArea(
            int pixelX, int pixelY, int spanX, int spanY, int[] result) {
        return findNearestArea(pixelX, pixelY, spanX, spanY, null, false, result);
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreOccupied If true, the result can be an occupied cell
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    int[] findNearestArea(int pixelX, int pixelY, int spanX, int spanY, View ignoreView,
            boolean ignoreOccupied, int[] result) {
        return findNearestArea(pixelX, pixelY, spanX, spanY,
                spanX, spanY, ignoreView, ignoreOccupied, result, null, mOccupied);
    }

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     *
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param minSpanX The minimum horizontal span required
     * @param minSpanY The minimum vertical span required
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param ignoreOccupied If true, the result can be an occupied cell
     * @param result Array in which to place the result, or null (in which case a new array will
     *        be allocated)
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
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

        for (int y = 0; y < countY - (minSpanY - 1); y++) {
            inner:
            for (int x = 0; x < countX - (minSpanX - 1); x++) {
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

    /**
    * Find a vacant area that will fit the given bounds nearest the requested
    * cell location, and will also weigh in a suggested direction vector of the
    * desired location. This method computers distance based on unit grid distances,
    * not pixel distances.
    *
    * @param cellX The X cell nearest to which you want to search for a vacant area.
    * @param cellY The Y cell nearest which you want to search for a vacant area.
    * @param spanX Horizontal span of the object.
    * @param spanY Vertical span of the object.
    * @param direction The favored direction in which the views should move from x, y
    * @param exactDirectionOnly If this parameter is true, then only solutions where the direction
    *        matches exactly. Otherwise we find the best matching direction.
    * @param occoupied The array which represents which cells in the CellLayout are occupied
    * @param blockOccupied The array which represents which cells in the specified block (cellX,
    *        cellY, spanX, spanY) are occupied. This is used when try to move a group of views.
    * @param result Array in which to place the result, or null (in which case a new array will
    *        be allocated)
    * @return The X, Y cell of a vacant area that can contain this object,
    *         nearest the requested location.
    */
   protected int[] findNearestArea(int cellX, int cellY, int spanX, int spanY, int[] direction,
           boolean[][] occupied, boolean blockOccupied[][], int[] result) {
       // Keep track of best-scoring drop area
       final int[] bestXY = result != null ? result : new int[2];
       float bestDistance = Float.MAX_VALUE;
       int bestDirectionScore = Integer.MIN_VALUE;

       final int countX = mCountX;
       final int countY = mCountY;

       for (int y = 0; y < countY - (spanY - 1); y++) {
           inner:
           for (int x = 0; x < countX - (spanX - 1); x++) {
               // First, let's see if this thing fits anywhere
               for (int i = 0; i < spanX; i++) {
                   for (int j = 0; j < spanY; j++) {
                       if (occupied[x + i][y + j] && (blockOccupied == null || blockOccupied[i][j])) {
                           continue inner;
                       }
                   }
               }

               float distance = (float)
                       Math.sqrt((x - cellX) * (x - cellX) + (y - cellY) * (y - cellY));
               int[] curDirection = mTmpPoint;
               computeDirectionVector(x - cellX, y - cellY, curDirection);
               // The direction score is just the dot product of the two candidate direction
               // and that passed in.
               int curDirectionScore = direction[0] * curDirection[0] +
                       direction[1] * curDirection[1];
               boolean exactDirectionOnly = false;
               boolean directionMatches = direction[0] == curDirection[0] &&
                       direction[0] == curDirection[0];
               if ((directionMatches || !exactDirectionOnly) &&
                       Float.compare(distance,  bestDistance) < 0 || (Float.compare(distance,
                       bestDistance) == 0 && curDirectionScore > bestDirectionScore)) {
                   bestDistance = distance;
                   bestDirectionScore = curDirectionScore;
                   bestXY[0] = x;
                   bestXY[1] = y;
               }
           }
       }

       // Return -1, -1 if no suitable location found
       if (bestDistance == Float.MAX_VALUE) {
           bestXY[0] = -1;
           bestXY[1] = -1;
       }
       return bestXY;
   }

   /**
    * Find a vacant area that will fit the given bounds nearest the requested
    * cell location. Uses Euclidean distance to score multiple vacant areas.
    *
    * @param pixelX The X location at which you want to search for a vacant area.
    * @param pixelY The Y location at which you want to search for a vacant area.
    * @param spanX Horizontal span of the object.
    * @param spanY Vertical span of the object.
    * @param ignoreView Considers space occupied by this view as unoccupied
    * @param result Previously returned value to possibly recycle.
    * @return The X, Y cell of a vacant area that can contain this object,
    *         nearest the requested location.
    */
   int[] findNearestVacantArea(
           int pixelX, int pixelY, int spanX, int spanY, View ignoreView, int[] result) {
       return findNearestArea(pixelX, pixelY, spanX, spanY, ignoreView, true, result);
   }

   /**
    * Find a vacant area that will fit the given bounds nearest the requested
    * cell location. Uses Euclidean distance to score multiple vacant areas.
    *
    * @param pixelX The X location at which you want to search for a vacant area.
    * @param pixelY The Y location at which you want to search for a vacant area.
    * @param minSpanX The minimum horizontal span required
    * @param minSpanY The minimum vertical span required
    * @param spanX Horizontal span of the object.
    * @param spanY Vertical span of the object.
    * @param ignoreView Considers space occupied by this view as unoccupied
    * @param result Previously returned value to possibly recycle.
    * @return The X, Y cell of a vacant area that can contain this object,
    *         nearest the requested location.
    */
   int[] findNearestVacantArea(int pixelX, int pixelY, int minSpanX, int minSpanY,
           int spanX, int spanY, View ignoreView, int[] result, int[] resultSpan) {
       return findNearestArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, ignoreView, true,
               result, resultSpan, mOccupied);
   }

   /*
    * Returns a pair (x, y), where x,y are in {-1, 0, 1} corresponding to vector between
    * the provided point and the provided cell
    */
   protected void computeDirectionVector(float deltaX, float deltaY, int[] result) {
       double angle = Math.atan(((float) deltaY) / deltaX);

       result[0] = 0;
       result[1] = 0;
       if (Math.abs(Math.cos(angle)) > 0.5f) {
           result[0] = (int) Math.signum(deltaX);
       }
       if (Math.abs(Math.sin(angle)) > 0.5f) {
           result[1] = (int) Math.signum(deltaY);
       }
   }

    protected void recycleTempRects(Stack<Rect> used) {
        while (!used.isEmpty()) {
            mTempRectStack.push(used.pop());
        }
    }

    // Class which represents the reorder hint animations. These animations show that an item is
    // in a temporary state, and hint at where the item will return to.
    class ReorderHintAnimation {
        View child;
        float finalDeltaX;
        float finalDeltaY;
        float initDeltaX;
        float initDeltaY;
        float finalScale;
        float initScale;
        private static final int DURATION = 300;
        Animator a;

        public ReorderHintAnimation(View child, int cellX0, int cellY0, int cellX1, int cellY1,
                int spanX, int spanY) {
            regionToCenterPoint(cellX0, cellY0, spanX, spanY, mTmpPoint);
            final int x0 = mTmpPoint[0];
            final int y0 = mTmpPoint[1];
            regionToCenterPoint(cellX1, cellY1, spanX, spanY, mTmpPoint);
            final int x1 = mTmpPoint[0];
            final int y1 = mTmpPoint[1];
            final int dX = x1 - x0;
            final int dY = y1 - y0;
            finalDeltaX = 0;
            finalDeltaY = 0;
            if (dX == dY && dX == 0) {
            } else {
                if (dY == 0) {
                    finalDeltaX = - Math.signum(dX) * mReorderHintAnimationMagnitude;
                } else if (dX == 0) {
                    finalDeltaY = - Math.signum(dY) * mReorderHintAnimationMagnitude;
                } else {
                    double angle = Math.atan( (float) (dY) / dX);
                    finalDeltaX = (int) (- Math.signum(dX) *
                            Math.abs(Math.cos(angle) * mReorderHintAnimationMagnitude));
                    finalDeltaY = (int) (- Math.signum(dY) *
                            Math.abs(Math.sin(angle) * mReorderHintAnimationMagnitude));
                }
            }
            initDeltaX = child.getTranslationX();
            initDeltaY = child.getTranslationY();
            finalScale = getChildrenScale() - 4.0f / child.getWidth();
            initScale = child.getScaleX();
            this.child = child;
        }

        void animate() {
            if (mShakeAnimators.containsKey(child)) {
                ReorderHintAnimation oldAnimation = mShakeAnimators.get(child);
                oldAnimation.cancel();
                mShakeAnimators.remove(child);
                if (finalDeltaX == 0 && finalDeltaY == 0) {
                    completeAnimationImmediately();
                    return;
                }
            }
            if (finalDeltaX == 0 && finalDeltaY == 0) {
                return;
            }
            ValueAnimator va = ViewAnimUtils.ofFloat(child, 0f, 1f);
            a = va;
            va.setRepeatMode(ValueAnimator.REVERSE);
            va.setRepeatCount(ValueAnimator.INFINITE);
            va.setDuration(DURATION);
            va.setStartDelay((int) (Math.random() * 60));
            va.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float r = ((Float) animation.getAnimatedValue()).floatValue();
                    float x = r * finalDeltaX + (1 - r) * initDeltaX;
                    float y = r * finalDeltaY + (1 - r) * initDeltaY;
                    child.setTranslationX(x);
                    child.setTranslationY(y);
                    float s = r * finalScale + (1 - r) * initScale;
                    child.setScaleX(s);
                    child.setScaleY(s);
                }
            });
            va.addListener(new AnimatorListenerAdapter() {
                public void onAnimationRepeat(Animator animation) {
                    // We make sure to end only after a full period
                    initDeltaX = 0;
                    initDeltaY = 0;
                    initScale = getChildrenScale();
                }
            });
            mShakeAnimators.put(child, this);
            va.start();
        }

        private void cancel() {
            if (a != null) {
                a.cancel();
            }
        }

        private void completeAnimationImmediately() {
            if (a != null) {
                a.cancel();
            }

            AnimatorSet s = ViewAnimUtils.createAnimatorSet();
            a = s;
            s.playTogether(
                ViewAnimUtils.ofFloat(child, "scaleX", getChildrenScale()),
                ViewAnimUtils.ofFloat(child, "scaleY", getChildrenScale()),
                ViewAnimUtils.ofFloat(child, "translationX", 0f),
                ViewAnimUtils.ofFloat(child, "translationY", 0f)
            );
            s.setDuration(REORDER_ANIMATION_DURATION);
            s.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
            s.start();
        }
    }

    protected class CellAndSpan {
        int x, y;
        int spanX, spanY;

        public CellAndSpan() {
        }

        public void copy(CellAndSpan copy) {
            copy.x = x;
            copy.y = y;
            copy.spanX = spanX;
            copy.spanY = spanY;
        }

        public CellAndSpan(int x, int y, int spanX, int spanY) {
            this.x = x;
            this.y = y;
            this.spanX = spanX;
            this.spanY = spanY;
        }

        public String toString() {
            return "(" + x + ", " + y + ": " + spanX + ", " + spanY + ")";
        }

    }

    protected class ItemConfiguration {
        HashMap<View, CellAndSpan> map = new HashMap<View, CellAndSpan>();
        private HashMap<View, CellAndSpan> savedMap = new HashMap<View, CellAndSpan>();
        ArrayList<View> sortedViews = new ArrayList<View>();
        boolean isSolution = false;
        int dragViewX, dragViewY, dragViewSpanX, dragViewSpanY;

        void save() {
            // Copy current state into savedMap
            for (View v: map.keySet()) {
                map.get(v).copy(savedMap.get(v));
            }
        }

        void restore() {
            // Restore current state from savedMap
            for (View v: savedMap.keySet()) {
                savedMap.get(v).copy(map.get(v));
            }
        }

        void add(View v, CellAndSpan cs) {
            map.put(v, cs);
            savedMap.put(v, new CellAndSpan());
            sortedViews.add(v);
        }

        int area() {
            return dragViewSpanX * dragViewSpanY;
        }
    }

    /**
     * This helper class defines a cluster of views. It helps with defining complex edges
     * of the cluster and determining how those edges interact with other views. The edges
     * essentially define a fine-grained boundary around the cluster of views -- like a more
     * precise version of a bounding box.
     */
    protected class ViewCluster {
        final static int LEFT = 0;
        final static int TOP = 1;
        final static int RIGHT = 2;
        final static int BOTTOM = 3;

        ArrayList<View> views;
        ItemConfiguration config;
        Rect boundingRect = new Rect();

        int[] leftEdge = new int[mCountY];
        int[] rightEdge = new int[mCountY];
        int[] topEdge = new int[mCountX];
        int[] bottomEdge = new int[mCountX];
        boolean leftEdgeDirty, rightEdgeDirty, topEdgeDirty, bottomEdgeDirty, boundingRectDirty;

        @SuppressWarnings("unchecked")
        public ViewCluster(ArrayList<View> views, ItemConfiguration config) {
            this.views = (ArrayList<View>) views.clone();
            this.config = config;
            resetEdges();
        }

        void resetEdges() {
            for (int i = 0; i < mCountX; i++) {
                topEdge[i] = -1;
                bottomEdge[i] = -1;
            }
            for (int i = 0; i < mCountY; i++) {
                leftEdge[i] = -1;
                rightEdge[i] = -1;
            }
            leftEdgeDirty = true;
            rightEdgeDirty = true;
            bottomEdgeDirty = true;
            topEdgeDirty = true;
            boundingRectDirty = true;
        }

        void computeEdge(int which, int[] edge) {
            int count = views.size();
            for (int i = 0; i < count; i++) {
                CellAndSpan cs = config.map.get(views.get(i));
                switch (which) {
                    case LEFT:
                        int left = cs.x;
                        for (int j = cs.y; j < cs.y + cs.spanY; j++) {
                            if (left < edge[j] || edge[j] < 0) {
                                edge[j] = left;
                            }
                        }
                        break;
                    case RIGHT:
                        int right = cs.x + cs.spanX;
                        for (int j = cs.y; j < cs.y + cs.spanY; j++) {
                            if (right > edge[j]) {
                                edge[j] = right;
                            }
                        }
                        break;
                    case TOP:
                        int top = cs.y;
                        for (int j = cs.x; j < cs.x + cs.spanX; j++) {
                            if (top < edge[j] || edge[j] < 0) {
                                edge[j] = top;
                            }
                        }
                        break;
                    case BOTTOM:
                        int bottom = cs.y + cs.spanY;
                        for (int j = cs.x; j < cs.x + cs.spanX; j++) {
                            if (bottom > edge[j]) {
                                edge[j] = bottom;
                            }
                        }
                        break;
                }
            }
        }

        boolean isViewTouchingEdge(View v, int whichEdge) {
            CellAndSpan cs = config.map.get(v);

            int[] edge = getEdge(whichEdge);

            switch (whichEdge) {
                case LEFT:
                    for (int i = cs.y; i < cs.y + cs.spanY; i++) {
                        if (edge[i] == cs.x + cs.spanX) {
                            return true;
                        }
                    }
                    break;
                case RIGHT:
                    for (int i = cs.y; i < cs.y + cs.spanY; i++) {
                        if (edge[i] == cs.x) {
                            return true;
                        }
                    }
                    break;
                case TOP:
                    for (int i = cs.x; i < cs.x + cs.spanX; i++) {
                        if (edge[i] == cs.y + cs.spanY) {
                            return true;
                        }
                    }
                    break;
                case BOTTOM:
                    for (int i = cs.x; i < cs.x + cs.spanX; i++) {
                        if (edge[i] == cs.y) {
                            return true;
                        }
                    }
                    break;
            }
            return false;
        }

        void shift(int whichEdge, int delta) {
            for (View v: views) {
                CellAndSpan c = config.map.get(v);
                switch (whichEdge) {
                    case LEFT:
                        c.x -= delta;
                        break;
                    case RIGHT:
                        c.x += delta;
                        break;
                    case TOP:
                        c.y -= delta;
                        break;
                    case BOTTOM:
                    default:
                        c.y += delta;
                        break;
                }
            }
            resetEdges();
        }

        public void addView(View v) {
            views.add(v);
            resetEdges();
        }

        public Rect getBoundingRect() {
            if (boundingRectDirty) {
                boolean first = true;
                for (View v: views) {
                    CellAndSpan c = config.map.get(v);
                    if (first) {
                        boundingRect.set(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
                        first = false;
                    } else {
                        boundingRect.union(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
                    }
                }
            }
            return boundingRect;
        }

        public int[] getEdge(int which) {
            switch (which) {
                case LEFT:
                    return getLeftEdge();
                case RIGHT:
                    return getRightEdge();
                case TOP:
                    return getTopEdge();
                case BOTTOM:
                default:
                    return getBottomEdge();
            }
        }

        public int[] getLeftEdge() {
            if (leftEdgeDirty) {
                computeEdge(LEFT, leftEdge);
            }
            return leftEdge;
        }

        public int[] getRightEdge() {
            if (rightEdgeDirty) {
                computeEdge(RIGHT, rightEdge);
            }
            return rightEdge;
        }

        public int[] getTopEdge() {
            if (topEdgeDirty) {
                computeEdge(TOP, topEdge);
            }
            return topEdge;
        }

        public int[] getBottomEdge() {
            if (bottomEdgeDirty) {
                computeEdge(BOTTOM, bottomEdge);
            }
            return bottomEdge;
        }

        PositionComparator comparator = new PositionComparator();
        class PositionComparator implements Comparator<View> {
            int whichEdge = 0;
            public int compare(View left, View right) {
                CellAndSpan l = config.map.get(left);
                CellAndSpan r = config.map.get(right);
                switch (whichEdge) {
                    case LEFT:
                        return (r.x + r.spanX) - (l.x + l.spanX);
                    case RIGHT:
                        return l.x - r.x;
                    case TOP:
                        return (r.y + r.spanY) - (l.y + l.spanY);
                    case BOTTOM:
                    default:
                        return l.y - r.y;
                }
            }
        }

        public void sortConfigurationForEdgePush(int edge) {
            comparator.whichEdge = edge;
            Collections.sort(config.sortedViews, comparator);
        }
    }
}
