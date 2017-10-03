package jp.co.disney.apps.managed.kisekaeapp.launcher;

import jp.co.disney.apps.managed.kisekaeapp.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;

public class BaseCellLayout extends ViewGroup {
    static final String TAG = "BaseCellLayout";

    protected static final int DEFAULT_COUNT_X = 4;
    protected static final int DEFAULT_COUNT_Y = 4;

    protected int mCellWidth;
    protected int mCellHeight;
    // 論理上のセルサイズが整数でない場合の考慮
    // セルサイズが必要な計算ではこちらの値を使用する
    protected float mfCellWidth;
    protected float mfCellHeight;

    protected int mCountX;
    protected int mCountY;

    boolean[][] mOccupied;

    protected final int[] mTmpPoint = new int[2];

    protected CellItemContainer mCellItemContainer;

    protected final static Paint sPaint = new Paint();

    public BaseCellLayout(Context context) {
        this(context, null);
    }

    public BaseCellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setWillNotDraw(false);
        setClipToPadding(false);

        setAlwaysDrawnWithCacheEnabled(false);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BaseCellLayout, defStyle, 0);
        mCountX = a.getInt(R.styleable.BaseCellLayout_cellCountX, DEFAULT_COUNT_X);
        mCountY = a.getInt(R.styleable.BaseCellLayout_cellCountY, DEFAULT_COUNT_X);
        a.recycle();

        mOccupied = new boolean[mCountX][mCountY];

        mCellItemContainer = new CellItemContainer(context);
        addView(mCellItemContainer);
    }

    public int getCellWidth() {
        return mCellWidth;
    }

    public int getCellHeight() {
        return mCellHeight;
    }

    public int getCountX() {
        return mCountX;
    }

    public int getCountY() {
        return mCountY;
    }

    public float getExactCellWidth() {
        return mfCellWidth;
    }

    public float getExactCellHeight() {
        return mfCellHeight;
    }

    public void setCellSize(int width, int height) {
        mCellWidth = width;
        mCellHeight = height;
        mfCellWidth = (float) width;
        mfCellHeight = (float) height;
        mCellItemContainer.setCellDimensions(mfCellWidth, mfCellHeight);
    }

    public void setCellSize(float width, float height) {
        mCellWidth = (int) Math.floor(width);
        mCellHeight = (int) Math.floor(height);
        mfCellWidth = width;
        mfCellHeight = height;
        mCellItemContainer.setCellDimensions(mfCellWidth, mfCellHeight);
    }

    public void updateGridSize(int x, int y) {
        mCountX = x;
        mCountY = y;
        mOccupied = new boolean[mCountX][mCountY];

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            BaseCellLayout.LayoutParams lp = (BaseCellLayout.LayoutParams) child.getLayoutParams();
            child.setLayoutParams(new BaseCellLayout.LayoutParams(lp));
        }
        requestLayout();
    }

    public CellItemContainer getCellItemContainer() {
        return mCellItemContainer;
    }

    public View getChildAt(int x, int y) {
        return mCellItemContainer.getChildAt(x, y);
    }

    public View getContainerChildAt(int index) {
        return mCellItemContainer.getChildAt(index);
    }

    public int getContainerChildCount() {
        return mCellItemContainer.getChildCount();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }

        // グリッド数と領域のサイズに応じてセルサイズを決める
        float cellWidth = (float) (widthSpecSize - getPaddingLeft() - getPaddingRight()) / mCountX;
        float cellHeight = (float) (heightSpecSize - getPaddingTop() - getPaddingBottom()) / mCountY;
        setCellSize(cellWidth, cellHeight);

        // 子供のサイズは、パディングを差し引いた領域全体
        // 描画処理で表示を調整
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                    widthSpecSize - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY);
            final int childheightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    heightSpecSize - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY);
            child.measure(childWidthMeasureSpec, childheightMeasureSpec);
        }
        setMeasuredDimension(widthSpecSize, heightSpecSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.layout(getPaddingLeft(), getPaddingTop(),
                    r - l - getPaddingRight(), b - t - getPaddingBottom());
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public boolean isOccupied(int x, int y) {
        if (x < mCountX && y < mCountY) {
            return mOccupied[x][y];
        } else {
            throw new RuntimeException("Position exceeds the bound of this CellLayout");
        }
    }

    public void markCellsAsOccupiedForView(View view) {
        markCellsAsOccupiedForView(view, mOccupied);
    }

    public void markCellsAsOccupiedForView(View view, boolean[][] occupied) {
        if (view == null || view.getParent() != mCellItemContainer) return;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, occupied, true);
    }

    public void markCellsAsUnoccupiedForView(View view) {
        markCellsAsUnoccupiedForView(view, mOccupied);
    }
    public void markCellsAsUnoccupiedForView(View view, boolean occupied[][]) {
        if (view == null || view.getParent() != mCellItemContainer) return;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, occupied, false);
    }

    protected void markCellsForView(int cellX, int cellY, int spanX, int spanY,
            boolean[][] occupied, boolean value) {
        if (cellX < 0 || cellY < 0) return;
        for (int x = cellX; x < cellX + spanX && x < mCountX; x++) {
            for (int y = cellY; y < cellY + spanY && y < mCountY; y++) {
                occupied[x][y] = value;
            }
        }
    }

    protected void clearOccupiedCells() {
        for (int x = 0; x < mCountX; x++) {
            for (int y = 0; y < mCountY; y++) {
                mOccupied[x][y] = false;
            }
        }
    }

    public boolean addViewToCellLayout(View child, int index, int childId,
            LayoutParams lp, boolean markCells) {

        child.setScaleX(getChildrenScale());
        child.setScaleY(getChildrenScale());

        // Generate an id for each view, this assumes we have at most 256x256 cells
        if (lp.cellX >= 0 && lp.cellX <= mCountX - 1 && lp.cellY >= 0 && lp.cellY <= mCountY - 1) {
            // If the horizontal or vertical span is set to -1, it is taken to
            // mean that it spans the extent of the CellLayout
            if (lp.cellHSpan < 0) lp.cellHSpan = mCountX;
            if (lp.cellVSpan < 0) lp.cellVSpan = mCountY;

            child.setId(childId);

            mCellItemContainer.addView(child, index, lp);

            if (markCells) markCellsAsOccupiedForView(child);

            return true;
        }
        return false;
    }

    @Override
    public void removeAllViews() {
        clearOccupiedCells();
        mCellItemContainer.removeAllViews();
    }

    @Override
    public void removeAllViewsInLayout() {
        if (mCellItemContainer.getChildCount() > 0) {
            clearOccupiedCells();
            mCellItemContainer.removeAllViewsInLayout();
        }
    }

    public void removeViewWithoutMarkingCells(View view) {
        mCellItemContainer.removeView(view);
    }

    @Override
    public void removeView(View view) {
        markCellsAsUnoccupiedForView(view);
        mCellItemContainer.removeView(view);
    }

    @Override
    public void removeViewAt(int index) {
        markCellsAsUnoccupiedForView(mCellItemContainer.getChildAt(index));
        mCellItemContainer.removeViewAt(index);
    }

    @Override
    public void removeViewInLayout(View view) {
        markCellsAsUnoccupiedForView(view);
        mCellItemContainer.removeViewInLayout(view);
    }

    @Override
    public void removeViews(int start, int count) {
        for (int i = start; i < start + count; i++) {
            markCellsAsUnoccupiedForView(mCellItemContainer.getChildAt(i));
        }
        mCellItemContainer.removeViews(start, count);
    }

    @Override
    public void removeViewsInLayout(int start, int count) {
        for (int i = start; i < start + count; i++) {
            markCellsAsUnoccupiedForView(mCellItemContainer.getChildAt(i));
        }
        mCellItemContainer.removeViewsInLayout(start, count);
    }

    /**
     * Given a point, return the cell that strictly encloses that point
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    void pointToCellExact(int x, int y, int[] result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();

        result[0] = (int) Math.floor((x - hStartPadding) / mfCellWidth);
        result[1] = (int) Math.floor((y - vStartPadding) / mfCellHeight);

        final int xAxis = mCountX;
        final int yAxis = mCountY;

        if (result[0] < 0) result[0] = 0;
        if (result[0] >= xAxis) result[0] = xAxis - 1;
        if (result[1] < 0) result[1] = 0;
        if (result[1] >= yAxis) result[1] = yAxis - 1;
    }

    /**
     * Given a point, return the cell that most closely encloses that point
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    void pointToCellRounded(int x, int y, int[] result) {
        pointToCellExact(x + (mCellWidth / 2), y + (mCellHeight / 2), result);
    }

    /**
     * Given a cell coordinate, return the point that represents the upper left corner of that cell
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    void cellToPoint(int cellX, int cellY, int[] result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();

        result[0] = hStartPadding + (int) Math.ceil(cellX * mfCellWidth);
        result[1] = vStartPadding + (int) Math.ceil(cellY * mfCellHeight);
    }

    /**
     * Given a cell coordinate, return the point that represents the center of the cell
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    void cellToCenterPoint(int cellX, int cellY, int[] result) {
        regionToCenterPoint(cellX, cellY, 1, 1, result);
    }

    /**
     * Given a cell coordinate and span return the point that represents the center of the regio
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     *
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    void regionToCenterPoint(int cellX, int cellY, int spanX, int spanY, int[] result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();
        result[0] = hStartPadding + (int) Math.ceil(cellX * mfCellWidth) + (int) Math.floor(spanX * mfCellWidth) / 2;
        result[1] = vStartPadding + (int) Math.ceil(cellY * mfCellHeight) + (int) Math.floor(spanY * mfCellHeight) / 2;
    }

     /**
     * Given a cell coordinate and span fills out a corresponding pixel rect
     *
     * @param cellX X coordinate of the cell
     * @param cellY Y coordinate of the cell
     * @param result Rect in which to write the result
     */
     void regionToRect(int cellX, int cellY, int spanX, int spanY, Rect result) {
        final int hStartPadding = getPaddingLeft();
        final int vStartPadding = getPaddingTop();
        final int left = hStartPadding + (int) Math.ceil(cellX * mfCellWidth);
        final int top = vStartPadding + (int) Math.ceil(cellY * mfCellHeight);
        result.set(left, top, left + (int) Math.floor(spanX * mfCellWidth), top + (int) Math.floor(spanY * mfCellHeight));
    }

    public float getDistanceFromCell(float x, float y, int[] cell) {
        cellToCenterPoint(cell[0], cell[1], mTmpPoint);
        float distance = (float) Math.sqrt( Math.pow(x - mTmpPoint[0], 2) +
                Math.pow(y - mTmpPoint[1], 2));
        return distance;
    }

    public float getDistanceFromCell(float x, float y, int[] cell, int[] span) {
        regionToCenterPoint(cell[0], cell[1], span[0], span[1], mTmpPoint);
        float distance = (float) Math.sqrt( Math.pow(x - mTmpPoint[0], 2) +
                Math.pow(y - mTmpPoint[1], 2));
        return distance;
    }

    public float getChildrenScale() {
        // TODO: 固定でない場合を考える
        return 1.0f;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new BaseCellLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new BaseCellLayout.LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof BaseCellLayout.LayoutParams;
    }

    @Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        mCellItemContainer.setChildrenDrawingCacheEnabled(enabled);
    }

    @Override
    protected void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        mCellItemContainer.setChildrenDrawnWithCacheEnabled(enabled);
    }

    public void enableHardwareLayers() {
        mCellItemContainer.setLayerType(LAYER_TYPE_HARDWARE, sPaint);
    }

    public void disableHardwareLayers() {
        mCellItemContainer.setLayerType(LAYER_TYPE_NONE, sPaint);
    }

    public void buildHardwareLayer() {
        mCellItemContainer.buildLayer();
    }

    public void restoreInstanceState(SparseArray<Parcelable> states) {
        dispatchRestoreInstanceState(states);
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        @ViewDebug.ExportedProperty
        public int cellX;

        @ViewDebug.ExportedProperty
        public int cellY;

        public int tmpCellX;

        public int tmpCellY;

        public boolean useTmpCoords;

        @ViewDebug.ExportedProperty
        public int cellHSpan;

        @ViewDebug.ExportedProperty
        public int cellVSpan;

        public boolean isLockedToGrid = true;

        public boolean canReorder = true;

        @ViewDebug.ExportedProperty
        int x;

        @ViewDebug.ExportedProperty
        int y;

        boolean dropped;

        public float drawingScale = -1.0f;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
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

        public void setup(float cellWidth, float cellHeight) {
            if (isLockedToGrid) {
                final int myCellHSpan = cellHSpan;
                final int myCellVSpan = cellVSpan;
                int myCellX = useTmpCoords ? tmpCellX : cellX;
                int myCellY = useTmpCoords ? tmpCellY : cellY;

                width = (int) Math.floor(myCellHSpan * cellWidth) - leftMargin - rightMargin;
                height = (int) Math.floor(myCellVSpan * cellHeight) - topMargin - bottomMargin;
                x = (int) (Math.ceil(myCellX * cellWidth) + leftMargin);
                y = (int) (Math.ceil(myCellY * cellHeight) + topMargin);
            }
        }

        public String toString() {
            return "(" + this.cellX + ", " + this.cellY + ")";
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getWidth() {
            return width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getHeight() {
            return height;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getX() {
            return x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getY() {
            return y;
        }
    }
}
