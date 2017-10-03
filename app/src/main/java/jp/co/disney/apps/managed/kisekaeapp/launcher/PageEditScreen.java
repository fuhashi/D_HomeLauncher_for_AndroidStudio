package jp.co.disney.apps.managed.kisekaeapp.launcher;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsTypeValue;

public class PageEditScreen extends LinearLayout implements LauncherTransitionable, DragSource, DropTarget,
    View.OnLongClickListener, PageEditItemView.OnIconClickListener {
    private static final String TAG = "PageEditScreen";

    public static final int DRAG_BITMAP_PADDING = 0;
//    public static final int DRAG_BITMAP_PADDING = 2;

    private Launcher mLauncher;
    private DragController mDragController;

    private final int[] mTempXY = new int[2];
    private final Rect mTempRect = new Rect();

    private int mNumPages = 0;

    /**
     * CellInfo for the cell that is currently being dragged
     */
    private BaseCellLayout3.CellInfo mDragInfo;

    private boolean mInTransition;
    private boolean mTransitioningToWorkspace;
    private boolean mResetAfterTransition;

    private PageEditCellLayout mContent;

    private int mItemBorderWidth;
    private int mItemPaddingX;

    private int mTouchSlop;

    private float mLastMotionX;
    private float mLastMotionY;
    private int mActivePointerId;
    private boolean mAllowLongPress;

    private PageEditItemView mSelectedView;
//    private Switch mLoopSwitch;
    private ImageView mLoopSwitch;
    private boolean mIsLoop = false;

    private float mItemAspectRatio;

    private int mMarginX;
    private int mMarginTop;

    private boolean mOriginalPageLoop;

    private boolean mLockIconClick = false;

    private int[] mPageMap = new int[Workspace.MAX_SCREEN_COUNT];
    private String[] mEditedWpPaths = new String[Workspace.MAX_SCREEN_COUNT];

    private int mStatusbarHeight;

    boolean pageLoop() {
        return mIsLoop;
//        return mLoopSwitch.isChecked();
    }

    int getNumPages() {
        return mNumPages;
    }

    String[] getEditedWpPaths() {
        return mEditedWpPaths;
    }

    int[] getPageMap() {
        return mPageMap;
    }

    int getDefaultPageIndex() {

        final int childCount = mContent.getContainerChildCount();;
        for (int i = 0; i < childCount; i++) {
            final PageEditItemView child = (PageEditItemView) mContent.getChildAt(i % 3, i / 3);
            if (child.isDefaultPage()) return i;
        }

        return 0;
    }

    public PageEditScreen(Context context, AttributeSet attrs) {
        super(context, attrs);

        int width =  context.getResources().getDisplayMetrics().widthPixels;
        mMarginX = (int) Math.ceil(width * 36 / 1080f);
        mMarginTop = (int) Math.ceil(width * 18 / 1080f);

        mItemBorderWidth = (int) Math.ceil(width * 6 / 1080f);
        mItemPaddingX = (int) Math.ceil(width * 18 / 1080f);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();

        for (int i = 0; i < mPageMap.length; i++) {
            mPageMap[i] = -1;
        }
    }

    void setup(Launcher launcher, DragController dragController) {
        mLauncher = launcher;
        mDragController = dragController;
    }

    @Override
    protected void onFinishInflate() {

        mContent = (PageEditCellLayout) findViewById(R.id.page_edit_content);
        ((LinearLayout.LayoutParams) mContent.getLayoutParams()).setMargins(mMarginX, mMarginTop, mMarginX, mMarginTop * 4);

//        mContent.setReorderHintAnimationMagnitude(mItemBorderWidth * 4f);

//        mLoopSwitch = (Switch) findViewById(R.id.loop_switch);
        mLoopSwitch = (ImageView) findViewById(R.id.loop_switch);
        mLoopSwitch.setClickable(true);

        setOnLongClickListener(this);
    }

    private Integer mMainWp;

    private int getMainWp(Context context, String rootDir, boolean inAppTheme) {

        final JSONObject infoJson = ThemeUtils.loadThemeInfoJson(getContext(), rootDir, inAppTheme);
        if (infoJson == null) {
            return 0;
        }

        int mainWp = 0;
        try {
            mainWp = infoJson.getInt(ThemeUtils.JSON_KEY_MAIN_WP);

        } catch (JSONException e) {
        }

        return mainWp;
    }

    void addNewPage(int copied) {

        final Context context = getContext();
        final Workspace workspace = mLauncher.getWorkspace();
        final boolean isThemeMode = mLauncher.isThemeMode();

        PageEditItemView iv = new PageEditItemView(context, mItemBorderWidth, mItemPaddingX, isThemeMode);
        if (isThemeMode) {

            final String themeId = mLauncher.getThemeId();
            final boolean inAppTheme = mLauncher.inAppTheme();

            String dir = ThemeUtils.getThemeRootDirectory(context, themeId, inAppTheme,
                    ContentsTypeValue.CONTENTS_TYPE_THEME.getValue());

            String wpPath;
            if (copied < 0) {
                if (mMainWp == null) {
                    mMainWp = getMainWp(context, dir, inAppTheme);
                }
                wpPath = ThemeUtils.getThemeWallpaperPath(context, dir, mMainWp, inAppTheme);
            } else {
                wpPath = mEditedWpPaths[copied];
            }

            Bitmap bmp = ThemeUtils.loadThemeBackground(context, wpPath, workspace.getMeasuredWidth() / 3);

            Bitmap pageBmp = null;
            if (bmp != null) {

                if (mStatusbarHeight == 0) {
                    mStatusbarHeight = Utilities.getStatusBarHeight(getResources());
                }
                final int statusBarHeight = mStatusbarHeight;

                final int workWidth = workspace.getMeasuredWidth();
                final int workHeight = workspace.getMeasuredHeight();

                int top = statusBarHeight * bmp.getWidth() / workWidth;
                int h = workHeight * bmp.getWidth() / workWidth;

                final int bmpWidth = (int) Math.ceil(getMeasuredWidth() / 3f);
                final int bmpHeight = (int) Math.ceil(getMeasuredWidth() / (3 * mItemAspectRatio));
                pageBmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(pageBmp);
                int bottom = top + h;
                if (bottom > bmp.getHeight()) {
                    bottom = bmp.getHeight();
                }
                canvas.drawBitmap(bmp, new Rect(0, top, bmp.getWidth(), bottom),
                        new Rect(0, 0, pageBmp.getWidth(), pageBmp.getHeight()), new Paint());

                bmp.recycle();
            }
            iv.setImageBitmap(pageBmp);

            mEditedWpPaths[mNumPages] = wpPath;
            mPageMap[mNumPages] = -2;
        }
        iv.setItemAspectRatio(mItemAspectRatio);
        iv.setOnIconClickListener(this);

        final int pageNo = mNumPages;
        BaseCellLayout.LayoutParams lp = new BaseCellLayout.LayoutParams(pageNo % 3, pageNo / 3, 1, 1);
        lp.tmpCellX = lp.cellX;
        lp.tmpCellY = lp.cellY;
        mContent.addViewToCellLayout(iv, 0, pageNo, lp, true);
        mNumPages++;

        if (mNumPages > 1) {
            PageEditItemView v0 = (PageEditItemView) mContent.getChildAt(0, 0);
            v0.hideDeleteIcon(false);
        }

//        if (isThemeMode && mNumPages >= Workspace.MAX_SCREEN_COUNT) {
//            for (int i = 0; i < mNumPages; i++) {
//                final PageEditItemView child = (PageEditItemView) mContent.getChildAt(i % 3, i / 3);
//                child.hideCopyIcon(true);
//            }
//        }
    }

    void init(Bitmap[] pageBmps) {

        mMainWp = null;

        final boolean isThemeMode = mLauncher.isThemeMode();
        final Workspace workspace = mLauncher.getWorkspace();
        mItemAspectRatio = workspace.getMeasuredWidth() / (float) workspace.getMeasuredHeight();
        setCurrentPages(pageBmps, isThemeMode);
        addDummyPageForAddingPage(isThemeMode);

        final int defaultPage = mLauncher.getDefaultPage();
        if (defaultPage >= 0) {
            PageEditItemView child = (PageEditItemView) mContent.getChildAt(defaultPage % 3, defaultPage / 3);
            if (child != null) {
                child.setIsDefaultPage(true);
            } else {
                PageEditItemView child0 = (PageEditItemView) mContent.getChildAt(0, 0);
                child0.setIsDefaultPage(true);
            }

        } else {
            PageEditItemView child = (PageEditItemView) mContent.getChildAt(0, 0);
            child.setIsDefaultPage(true);
        }

        mOriginalPageLoop = mLauncher.pageLoop();
//        mLoopSwitch.setChecked(mOriginalPageLoop);

        mIsLoop = mOriginalPageLoop;
        if (mIsLoop) {
            mLoopSwitch.setBackgroundResource(R.drawable.btn_addpanel_toggle_on);
        } else {
            mLoopSwitch.setBackgroundResource(R.drawable.btn_addpanel_toggle_off);
        }
        mLoopSwitch.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mIsLoop) {
                    mLoopSwitch.setBackgroundResource(R.drawable.btn_addpanel_toggle_off);
                } else {
                    mLoopSwitch.setBackgroundResource(R.drawable.btn_addpanel_toggle_on);
                }
                mIsLoop = !mIsLoop;
            }
        });
    }

    private void setCurrentPages(Bitmap[] pageBmps, boolean isThemeMode) {

        mContent.removeAllViews();

        final int count = pageBmps.length;
        for (int i = 0; i < count; i++) {
            PageEditItemView iv = new PageEditItemView(getContext(), mItemBorderWidth, mItemPaddingX, isThemeMode);
            iv.setImageBitmap(pageBmps[i]);
            iv.setItemAspectRatio(mItemAspectRatio);
            iv.setOnIconClickListener(this);
//            if (isThemeMode) {
//                iv.hideCopyIcon(count >= Workspace.MAX_SCREEN_COUNT);
//            }

            BaseCellLayout.LayoutParams lp = new BaseCellLayout.LayoutParams(i % 3, i / 3, 1, 1);
            lp.tmpCellX = lp.cellX;
            lp.tmpCellY = lp.cellY;
            lp.setup(mContent.getCellWidth(), mContent.getCellHeight());
            mContent.addViewToCellLayout(iv, 0, i, lp, true);

            if (isThemeMode) {
                mEditedWpPaths[i] = mLauncher.getWpPath(i);
            }
            mPageMap[i] = i;
        }
        mNumPages = count;

        if (mNumPages == 1) {
            PageEditItemView v0 = (PageEditItemView) mContent.getChildAt(0, 0);
            v0.hideDeleteIcon(true);
        }
    }

    private void addDummyPageForAddingPage(boolean isThemeMode) {

        PageEditItemView iv = new PageEditItemView(getContext(),
                mItemBorderWidth, mItemPaddingX, isThemeMode, true);

        iv.setItemAspectRatio(mItemAspectRatio);
        iv.setOnIconClickListener(this);

        final int pageNo = mNumPages;
        BaseCellLayout.LayoutParams lp = new BaseCellLayout.LayoutParams(pageNo % 3, pageNo / 3, 1, 1);
        lp.tmpCellX = lp.cellX;
        lp.tmpCellY = lp.cellY;
        lp.setup(mContent.getCellWidth(), mContent.getCellHeight());
        mContent.addViewToCellLayout(iv, 0, pageNo, lp, true);
    }

    @Override
    public void onIconClick(PageEditItemView v, int iconType) {

        if (mLockIconClick) return;

        mLockIconClick = true;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mLockIconClick = false;
            }
        }, 400);

        if (iconType == PageEditItemView.ICON_TYPE_COPY) {
            // 複製

            if (mNumPages >= Workspace.MAX_SCREEN_COUNT) {
                // 追加不可
                final Context ctx = getContext();
                Toast.makeText(ctx, ctx.getResources().getString(R.string.has_reached_max_panel), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!mLauncher.isThemeMode()) {

                // テーマモードでない場合は通常の追加
                final View addPanel = mContent.getChildAt(mNumPages % 3, mNumPages / 3);
                final int newIndex = mNumPages + 1;
                if (newIndex < Workspace.MAX_SCREEN_COUNT) {
                    mContent.animateChildToPosition(addPanel, newIndex % 3, newIndex / 3, 400, 0, true, true);
                } else {
                    mContent.removeView(addPanel);
                }

                addNewPage(-1);
                return;
            }

            int index = -1;

            final int childCount = mContent.getContainerChildCount();
            for (int i = 0; i < childCount; i++) {
                if (mContent.getChildAt(i % 3, i / 3) == v) {
                    index = i;
                    break;
                }
            }
            if (index < 0) return;

            final View addPanel = mContent.getChildAt(mNumPages % 3, mNumPages / 3);

            final int newIndex = mNumPages + 1;
            if (newIndex < Workspace.MAX_SCREEN_COUNT) {
                mContent.animateChildToPosition(addPanel, newIndex % 3, newIndex / 3, 400, 0, true, true);
            } else {
                mContent.removeView(addPanel);
            }

            addNewPage(index);

        } else if (iconType == PageEditItemView.ICON_TYPE_DELETE) {
            // 削除
            if (mNumPages == 1) return;

            int index = -1;

            final int childCount = mContent.getContainerChildCount();
            for (int i = 0; i < childCount; i++) {
                if (mContent.getChildAt(i % 3, i / 3) == v) {
                    index = i;
                    break;
                }
            }

            if (index < 0) return;

            if (v.isDefaultPage()) {
                // デフォルトページを移動
                if (index > 0) {
                    PageEditItemView v2 = (PageEditItemView) mContent.getChildAt((index - 1) % 3, (index - 1) / 3);
                    if (v2 != null) {
                        v2.setIsDefaultPage(true);
                        v2.invalidate();
                    }
                } else {
                    // 最初のページを削除時
                    PageEditItemView v2 = (PageEditItemView) mContent.getChildAt(1, 0);
                    if (v2 != null) {
                        v2.setIsDefaultPage(true);
                        v2.invalidate();
                    }
                }
            }

            mContent.removeView(v);

            final boolean isThemeMode = mLauncher.isThemeMode();
            for (int i = index + 1; i < childCount; i++) {
                final View child = mContent.getChildAt(i % 3, i / 3);
                if (child == null) break;
                mContent.animateChildToPosition(child,
                        (i - 1) % 3, (i - 1) / 3, 400, 0, true, true);

                if (isThemeMode) {
                    mEditedWpPaths[i - 1] = mEditedWpPaths[i];
                    mEditedWpPaths[i] = null;
                }
                mPageMap[i - 1] = mPageMap[i];
                mPageMap[i] = -1;
            }

            mNumPages--;

            if (mNumPages == 1) {
                PageEditItemView v0 = (PageEditItemView) mContent.getChildAt(0, 0);
                v0.hideDeleteIcon(true);
            } else if (mNumPages == Workspace.MAX_SCREEN_COUNT - 1) {
                addDummyPageForAddingPage(mLauncher.isThemeMode());
            }

//            if (isThemeMode && mNumPages < Workspace.MAX_SCREEN_COUNT) {
//                for (int i = 0; i < mNumPages; i++) {
//                    final PageEditItemView child = (PageEditItemView) mContent.getChildAt(i % 3, i / 3);
//                    child.hideCopyIcon(false);
//                }
//            }

        } else if (iconType == PageEditItemView.ICON_TYPE_HOME) {
            // ホーム設定
            final int childCount = mContent.getContainerChildCount();;
            for (int i = 0; i < childCount; i++) {
                final PageEditItemView child = (PageEditItemView) mContent.getContainerChildAt(i);
                child.setIsDefaultPage(false);
                child.invalidate();
            }

            v.setIsDefaultPage(true);
            v.invalidate();

        } else if (iconType == PageEditItemView.ICON_TYPE_ADD) {
            // 追加
            final int newIndex = mNumPages + 1;
            if (newIndex < Workspace.MAX_SCREEN_COUNT) {
                mContent.animateChildToPosition(v, newIndex % 3, newIndex / 3, 400, 0, true, true);
            } else {
                mContent.removeView(v);
            }

            addNewPage(-1);
        }
    }

    @Override
    public View getContent() {
        return mContent;
    }

    /* LauncherTransitionable overrides */
    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {

        // パネルをドラッグされたまま終了された場合の対策
        mDragController.cancelDrag();

        mInTransition = true;
        mTransitioningToWorkspace = toWorkspace;

        if (toWorkspace) {
            setVisibilityOfSiblingsWithLowerZOrder(VISIBLE);
        } else {
            mContent.setVisibility(VISIBLE);
        }

        if (mResetAfterTransition) {
            mResetAfterTransition = false;
        }
    }

    @Override
    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace) {
        if (animated) {
            enableAndBuildHardwareLayer();
        }
    }

    @Override
    public void onLauncherTransitionStep(Launcher l, float t) {
        // Do nothing
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace) {

        mInTransition = false;
        if (animated) {
            setLayerType(LAYER_TYPE_NONE, null);
        }

        if (!toWorkspace) {
            // Going from Workspace -> All Apps
            setVisibilityOfSiblingsWithLowerZOrder(INVISIBLE);
        } else {

            final int count = mContent.getCellItemContainer().getChildCount();
            for (int i = 0; i < count; i++) {
                final ImageView iv = (ImageView) mContent.getCellItemContainer().getChildAt(i);
                Drawable d = iv.getDrawable();
                if (d instanceof BitmapDrawable) {
                    BitmapDrawable bd = (BitmapDrawable) d;
                    Bitmap bitmap = bd.getBitmap();
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                }
            }

        }
    }

    @Override
    public boolean isMainView() {
        return false;
    };

    private void enableAndBuildHardwareLayer() {
        // isHardwareAccelerated() checks if we're attached to a window and if that
        // window is HW accelerated-- we were sometimes not attached to a window
        // and buildLayer was throwing an IllegalStateException
        if (isHardwareAccelerated()) {
            // Turn on hardware layers for performance
            setLayerType(LAYER_TYPE_HARDWARE, null);

            // force building the layer, so you don't get a blip early in an animation
            // when the layer is created layer
            buildLayer();
        }
    }

    private void setVisibilityOfSiblingsWithLowerZOrder(int visibility) {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) return;

        final int count = parent.getChildCount();
        if (!isChildrenDrawingOrderEnabled()) {
            for (int i = 0; i < count; i++) {
                final View child = parent.getChildAt(i);
                if (child == this) {
                    break;
                } else {
                    if (child.getVisibility() == GONE) {
                        continue;
                    }

                    if (child instanceof LauncherTransitionable) {
                        if (!((LauncherTransitionable) child).isMainView()) {
                            continue;
                        }
                    }

                    child.setVisibility(visibility);
                }
            }
        } else {
            throw new RuntimeException("Failed; can't get z-order of views");
        }
    }

    @Override
    public boolean onLongClick(View v) {

        // Return early if this is not initiated from a touch
        if (!v.isInTouchMode()) return false;

        // Return if global dragging is not enabled
        if (!mLauncher.isDraggingEnabled()) return false;

        BaseCellLayout3.CellInfo longClickCellInfo = (BaseCellLayout3.CellInfo) mContent.getTag();
        if (longClickCellInfo == null) {
            return false;
        }
        final View itemUnderLongClick = longClickCellInfo.cell;
        if (mAllowLongPress && !mDragController.isDragging()) {
            if (itemUnderLongClick != null) {
                startDrag(longClickCellInfo);
                return true;
            }
        }
        return false;
    }

    void startDrag(BaseCellLayout3.CellInfo cellInfo) {

        View child = cellInfo.cell;

        // Make sure the drag was started by a long press as opposed to a long click.
        if (!child.isInTouchMode()) {
            return;
        }

        if (((PageEditItemView) child).isDummy()) {
            return;
        }

        mDragInfo = cellInfo;
        child.setVisibility(INVISIBLE);
        PageEditCellLayout layout = (PageEditCellLayout) child.getParent().getParent();
        layout.prepareChildForDrag(child);

        child.clearFocus();
        child.setPressed(false);

        beginDragShared(child, this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:

            float downX = ev.getX();
            float downY = ev.getY();

            Rect r = new Rect();
            mContent.getHitRect(r);
            if (!r.contains((int) downX, (int) downY)) {
                return false;
            }

            mLastMotionX = downX;
            mLastMotionY = downY;
            mActivePointerId = ev.getPointerId(0);
            mAllowLongPress = true;

            BaseCellLayout3.CellInfo cellInfo = (BaseCellLayout3.CellInfo) mContent.getTag();
            if (cellInfo != null) {
                mSelectedView = (PageEditItemView) cellInfo.cell;
                if (mSelectedView != null) {
                    mSelectedView.changeState(true);
                }
            }

            break;

        case MotionEvent.ACTION_MOVE:

            final int pointerIndex = ev.findPointerIndex(mActivePointerId);
            if (pointerIndex != -1) {

                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);
                final int xDiff = (int) Math.abs(x - mLastMotionX);
                final int yDiff = (int) Math.abs(y - mLastMotionY);

                final int touchSlop = Math.round(5.0f * mTouchSlop);

                boolean xMoved = xDiff > touchSlop;
                boolean yMoved = yDiff > touchSlop;

                if (xMoved || yMoved) {
                    mAllowLongPress = false;
                }
            }
            break;

        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            mActivePointerId = -1;
            mAllowLongPress = false;
            if (mSelectedView != null) {
                mSelectedView.changeState(false);
            }

            break;

        case MotionEvent.ACTION_POINTER_UP:
            final int pointerIndex_up = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
            final int pointerId = ev.getPointerId(pointerIndex_up);
            if (pointerId == mActivePointerId) {
                final int newPointerIndex = pointerIndex_up == 0 ? 1 : 0;
                mLastMotionX = ev.getX(newPointerIndex);
                mLastMotionY = ev.getY(newPointerIndex);
                mActivePointerId = ev.getPointerId(newPointerIndex);
                mAllowLongPress = false;
                if (mSelectedView != null) {
                    mSelectedView.changeState(false);
                }
            }
            break;
        }

        return super.onTouchEvent(ev);
    }

    public void beginDragShared(View child, DragSource source) {

        // The drag bitmap follows the touch point around on the screen
        final Bitmap b = createDragBitmap(child, new Canvas(), DRAG_BITMAP_PADDING);

        final int bmpWidth = b.getWidth();
        final int bmpHeight = b.getHeight();

        float scale = mLauncher.getDragLayer().getLocationInDragLayer(child, mTempXY);
        int dragLayerX =
                Math.round(mTempXY[0] - (bmpWidth - scale * child.getWidth()) / 2);
        int dragLayerY =
                Math.round(mTempXY[1] - (bmpHeight - scale * bmpHeight) / 2
                        - DRAG_BITMAP_PADDING / 2);

        Point dragVisualizeOffset = null;
        Rect dragRect = null;

        dragRect = new Rect(0, 0, child.getWidth(), child.getHeight());

        mDragController.startDrag(b, dragLayerX, dragLayerY, source, child.getTag(),
                DragController.DRAG_ACTION_MOVE, dragVisualizeOffset, dragRect, scale);
        b.recycle();
    }

    /**
     * Returns a new bitmap to show when the given View is being dragged around.
     * Responsibility for the bitmap is transferred to the caller.
     */
    public Bitmap createDragBitmap(View v, Canvas canvas, int padding) {

        Bitmap b = Bitmap.createBitmap(
                v.getWidth() + padding, v.getHeight() + padding, Bitmap.Config.ARGB_8888);

        final ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof BaseCellLayout.LayoutParams) {

            final BaseCellLayout.LayoutParams cellLp = (BaseCellLayout.LayoutParams) lp;
            boolean requireScale = (cellLp.drawingScale > 0f);
            if (requireScale) {
                int saveCount = canvas.save();
                canvas.scale(cellLp.drawingScale, cellLp.drawingScale, v.getWidth() / 2, v.getHeight() / 2);
                canvas.setBitmap(b);
                drawDragView(v, canvas, padding, true);
                canvas.setBitmap(null);
                canvas.restoreToCount(saveCount);

                return b;
            }
        }

        canvas.setBitmap(b);
        drawDragView(v, canvas, padding, true);
        canvas.setBitmap(null);

        return b;
    }

    /**
     * Draw the View v into the given Canvas.
     *
     * @param v the view to draw
     * @param destCanvas the canvas to draw on
     * @param padding the horizontal and vertical padding to use when drawing
     */
    private void drawDragView(View v, Canvas destCanvas, int padding, boolean pruneToDrawable) {
        final Rect clipRect = mTempRect;
        v.getDrawingRect(clipRect);

        destCanvas.save();
        if (v instanceof TextView && pruneToDrawable) {
            Drawable d = ((TextView) v).getCompoundDrawables()[1];
            Rect r = d.getBounds();
            clipRect.set(0, 0, r.right - r.left + padding, r.bottom - r.top + padding);
            destCanvas.translate(padding / 2, padding / 2);
            d.draw(destCanvas);
        } else {

            destCanvas.translate(-v.getScrollX() + padding / 2, -v.getScrollY() + padding / 2);
            destCanvas.clipRect(clipRect, Op.REPLACE);
            v.draw(destCanvas);
        }
        destCanvas.restore();
    }

    @Override
    public void onDropCompleted(View target, DragObject d, boolean success) {

        mLockIconClick = true;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mLockIconClick = false;
            }
        }, 400);

        if (!success && mDragInfo != null) {
            mContent.onDropChild(mDragInfo.cell);
        }

        if (d.cancelled &&  mDragInfo.cell != null) {
            mDragInfo.cell.setVisibility(VISIBLE);
        }

        mDragInfo = null;
    }

    @Override
    public boolean isDropEnabled() {
        return mLauncher.isPageEditVisible();
    }

    private float[] mDragViewVisualCenter = new float[2];
    private int[] mTargetCell = new int[2];

    // This is used to compute the visual center of the dragView. This point is then
    // used to visualize drop locations and determine where to drop an item. The idea is that
    // the visual center represents the user's interpretation of where the item is, and hence
    // is the appropriate point to use when determining drop location.
    private float[] getDragViewVisualCenter(int x, int y, int xOffset, int yOffset,
            DragView dragView, float[] recycle) {
        float res[];
        if (recycle == null) {
            res = new float[2];
        } else {
            res = recycle;
        }

        // First off, the drag view has been shifted in a way that is not represented in the
        // x and y values or the x/yOffsets. Here we account for that shift.
        x += getResources().getDimensionPixelSize(R.dimen.dragViewOffsetX);
        y += getResources().getDimensionPixelSize(R.dimen.dragViewOffsetY);

        // These represent the visual top and left of drag view if a dragRect was provided.
        // If a dragRect was not provided, then they correspond to the actual view left and
        // top, as the dragRect is in that case taken to be the entire dragView.
        // R.dimen.dragViewOffsetY.
        int left = x - xOffset;
        int top = y - yOffset;

        // In order to find the visual center, we shift by half the dragRect
        res[0] = left + dragView.getDragRegion().width() / 2;
        res[1] = top + dragView.getDragRegion().height() / 2;

        return res;
    }

    /*
    *
    * Convert the 2D coordinate xy from the parent View's coordinate space to this CellLayout's
    * coordinate space. The argument xy is modified with the return result.
    *
    * if cachedInverseMatrix is not null, this method will just use that matrix instead of
    * computing it itself; we use this to avoid redundant matrix inversions in
    * findMatchingPageForDragOver
    *
    */
   void mapPointFromSelfToChild(View v, float[] xy, Matrix cachedInverseMatrix) {
       if (cachedInverseMatrix == null) {
           v.getMatrix().invert(mTempInverseMatrix);
           cachedInverseMatrix = mTempInverseMatrix;
       }

       xy[0] = xy[0] - v.getLeft();
       xy[1] = xy[1] - v.getTop();
       cachedInverseMatrix.mapPoints(xy);
   }

   private Matrix mTempInverseMatrix = new Matrix();

    @Override
    public void onDrop(final DragObject d) {

        mDragViewVisualCenter = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset, d.dragView,
                mDragViewVisualCenter);

        final PageEditCellLayout dropTargetLayout = mContent;

        // We want the point to be mapped to the dragTarget.
        mapPointFromSelfToChild(dropTargetLayout, mDragViewVisualCenter, null);

        if (d.dragSource != this) {
            return;
        }

        if (mDragInfo != null) {

            final View cell = mDragInfo.cell;

            int spanX = mDragInfo != null ? mDragInfo.spanX : 1;
            int spanY = mDragInfo != null ? mDragInfo.spanY : 1;
            // First we find the cell nearest to point at which the item is
            // dropped, without any consideration to whether there is an item there.

            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0], (int)
                    mDragViewVisualCenter[1], spanX, spanY, dropTargetLayout, mTargetCell);

            int minSpanX = 1;
            int minSpanY = 1;

            int[] resultSpan = new int[2];
            mTargetCell = dropTargetLayout.createArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY, cell,
                    mTargetCell, resultSpan, BaseCellLayout2.MODE_ON_DROP, mNumPages, mPageMap, mEditedWpPaths);

            boolean foundCell = false;
            if (mTargetCell[0] >= 0 && mTargetCell[1] >= 0) {
                foundCell = true;
            }

            BaseCellLayout.LayoutParams lp = (BaseCellLayout.LayoutParams) cell.getLayoutParams();
            if (foundCell) {
                lp.cellX = lp.tmpCellX = mTargetCell[0];
                lp.cellY = lp.tmpCellY = mTargetCell[1];
                lp.cellHSpan = 1;
                lp.cellVSpan = 1;
                lp.isLockedToGrid = true;
                cell.setId(LauncherModel.getCellLayoutChildId(0, mDragInfo.screen,
                        mTargetCell[0], mTargetCell[1], mDragInfo.spanX, mDragInfo.spanY));
            } else {
                // 元の位置に戻す
                mTargetCell[0] = lp.cellX;
                mTargetCell[1] = lp.cellY;
                PageEditCellLayout layout = (PageEditCellLayout) cell.getParent().getParent();
                layout.markCellsAsOccupiedForView(cell);
            }

            // Prepare it to be animated into its new position
            // This must be called after the view has been re-parented
            final Runnable onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    mAnimatingViewIntoPlace = false;
                }
            };
            mAnimatingViewIntoPlace = true;
            if (d.dragView.hasDrawn()) {
                int duration = -1;
                mLauncher.getDragLayer().animateViewIntoPosition2(d.dragView, cell, duration,
                        onCompleteRunnable, this);

            } else {
                d.deferDragViewCleanupPostAnimation = false;
                cell.setVisibility(VISIBLE);
            }
        }
    }

    boolean mAnimatingViewIntoPlace = false;

    /**
     * Calculate the nearest cell where the given object would be dropped.
     *
     * pixelX and pixelY should be in the coordinate system of layout
     */
    private int[] findNearestArea(int pixelX, int pixelY,
            int spanX, int spanY, BaseCellLayout3 layout, int[] recycle) {
        return layout.findNearestArea(
                pixelX, pixelY, spanX, spanY, recycle);
    }

    @Override
    public void onDragEnter(DragObject dragObject) {
        mContent.onDragEnter();
    }

    @Override
    public void onDragOver(DragObject d) {

        mDragViewVisualCenter = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset,
            d.dragView, mDragViewVisualCenter);

        final View child = (mDragInfo == null) ? null : mDragInfo.cell;
        // Identify whether we have dragged over a side page

        PageEditCellLayout mDragTargetLayout = mContent;

        mapPointFromSelfToChild(mDragTargetLayout, mDragViewVisualCenter, null);

        mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                (int) mDragViewVisualCenter[1], 1, 1, mDragTargetLayout, mTargetCell);

        boolean nearestDropOccupied = mDragTargetLayout.isNearestDropLocationOccupied((int)
                mDragViewVisualCenter[0], (int) mDragViewVisualCenter[1], 1, 1, child, mTargetCell);
        if (!nearestDropOccupied) {
            mDragTargetLayout.revertTempState();

        } else if ((mDragMode == DRAG_MODE_NONE || mDragMode == DRAG_MODE_REORDER)
                && !mReorderAlarm.alarmPending() && (mLastReorderX != mTargetCell[0] ||
                mLastReorderY != mTargetCell[1])) {

            // Otherwise, if we aren't adding to or creating a folder and there's no pending
            // reorder, then we schedule a reorder
            ReorderAlarmListener listener = new ReorderAlarmListener(mDragViewVisualCenter,
                    1, 1, 1, 1, d.dragView, child);
            mReorderAlarm.setOnAlarmListener(listener);
            mReorderAlarm.setAlarm(REORDER_TIMEOUT);
        }
    }

    private static final int DRAG_MODE_NONE = 0;
    private static final int DRAG_MODE_REORDER = 3;
    private int mDragMode = DRAG_MODE_NONE;
    private int mLastReorderX = -1;
    private int mLastReorderY = -1;

    private static final int REORDER_TIMEOUT = 250;
    private final Alarm mReorderAlarm = new Alarm();

    class ReorderAlarmListener implements OnAlarmListener {
        float[] dragViewCenter;
        int minSpanX, minSpanY, spanX, spanY;
        DragView dragView;
        View child;

        public ReorderAlarmListener(float[] dragViewCenter, int minSpanX, int minSpanY, int spanX,
                int spanY, DragView dragView, View child) {
            this.dragViewCenter = dragViewCenter;
            this.minSpanX = minSpanX;
            this.minSpanY = minSpanY;
            this.spanX = spanX;
            this.spanY = spanY;
            this.child = child;
            this.dragView = dragView;
        }

        public void onAlarm(Alarm alarm) {
            int[] resultSpan = new int[2];
            mTargetCell = findNearestArea((int) mDragViewVisualCenter[0],
                    (int) mDragViewVisualCenter[1], spanX, spanY, mContent, mTargetCell);
            mLastReorderX = mTargetCell[0];
            mLastReorderY = mTargetCell[1];

            mTargetCell = mContent.createArea((int) mDragViewVisualCenter[0],
                (int) mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY,
                child, mTargetCell, resultSpan, BaseCellLayout2.MODE_DRAG_OVER, mNumPages, mPageMap, mEditedWpPaths);

            if (mTargetCell[0] < 0 || mTargetCell[1] < 0) {
                mContent.revertTempState();
            } else {
                setDragMode(DRAG_MODE_REORDER);
            }
        }
    }

    void setDragMode(int dragMode) {
        if (dragMode != mDragMode) {
            if (dragMode == DRAG_MODE_NONE) {
                cleanupReorder(false);
            }
            mDragMode = dragMode;
        }
    }

    private void cleanupReorder(boolean cancelAlarm) {
        // Any pending reorders are canceled
        if (cancelAlarm) {
            mReorderAlarm.cancelAlarm();
        }
        mLastReorderX = -1;
        mLastReorderY = -1;
    }

    @Override
    public void onDragExit(DragObject dragObject) {
        mContent.revertTempState();
        mContent.onDragExit();
    }

    @Override
    public DropTarget getDropTargetDelegate(DragObject dragObject) {
        return null;
    }

    @Override
    public boolean acceptDrop(DragObject d) {
        // If it's an external drop (e.g. from All Apps), check if it should be accepted
        return true;
    }

    @Override
    public void getLocationInDragLayer(int[] loc) {
        mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
    }
}
