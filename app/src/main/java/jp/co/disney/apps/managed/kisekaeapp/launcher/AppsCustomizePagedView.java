/*
 * Copyright (C) 2011 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Process;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;
import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.launcher.DropTarget.DragObject;
import jp.co.disney.apps.managed.kisekaeapp.system.graphics.BitmapUtils;
import jp.co.disney.apps.managed.kisekaeapp.system.graphics.FastBitmapDrawable;
import jp.co.disney.apps.managed.kisekaeapp.system.view.animation.ViewAnimUtils;
import jp.co.disney.apps.managed.kisekaeapp.system.view.pagedview.PagedView;

/**
 * A simple callback interface which also provides the results of the task.
 */
interface AsyncTaskCallback {
    void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data);
}

/**
 * The data needed to perform either of the custom AsyncTasks.
 */
class AsyncTaskPageData {
    enum Type {
        LoadWidgetPreviewData
    }

    AsyncTaskPageData(int p, ArrayList<Object> l, int cw, int ch, AsyncTaskCallback bgR,
            AsyncTaskCallback postR, WidgetPreviewLoader w) {
        page = p;
        items = l;
        generatedImages = new ArrayList<Bitmap>();
        maxImageWidth = cw;
        maxImageHeight = ch;
        doInBackgroundCallback = bgR;
        postExecuteCallback = postR;
        widgetPreviewLoader = w;
    }
    void cleanup(boolean cancelled) {
        // Clean up any references to source/generated bitmaps
        if (generatedImages != null) {
            if (cancelled) {
                for (int i = 0; i < generatedImages.size(); i++) {
                    widgetPreviewLoader.recycleBitmap(items.get(i), generatedImages.get(i));
                }
            }
            generatedImages.clear();
        }
    }
    int page;
    ArrayList<Object> items;
    ArrayList<Bitmap> sourceImages;
    ArrayList<Bitmap> generatedImages;
    int maxImageWidth;
    int maxImageHeight;
    AsyncTaskCallback doInBackgroundCallback;
    AsyncTaskCallback postExecuteCallback;
    WidgetPreviewLoader widgetPreviewLoader;
}

/**
 * A generic template for an async task used in AppsCustomize.
 */
class AppsCustomizeAsyncTask extends AsyncTask<AsyncTaskPageData, Void, AsyncTaskPageData> {
    AppsCustomizeAsyncTask(int p, AsyncTaskPageData.Type ty) {
        page = p;
        threadPriority = Process.THREAD_PRIORITY_DEFAULT;
        dataType = ty;
    }
    @Override
    protected AsyncTaskPageData doInBackground(AsyncTaskPageData... params) {
        if (params.length != 1) return null;
        // Load each of the widget previews in the background
        params[0].doInBackgroundCallback.run(this, params[0]);
        return params[0];
    }
    @Override
    protected void onPostExecute(AsyncTaskPageData result) {
        // All the widget previews are loaded, so we can just callback to inflate the page
        result.postExecuteCallback.run(this, result);
    }

    void setThreadPriority(int p) {
        threadPriority = p;
    }
    void syncThreadPriority() {
        Process.setThreadPriority(threadPriority);
    }

    // The page that this async task is associated with
    AsyncTaskPageData.Type dataType;
    int page;
    int threadPriority;
}

/**
 * The Apps/Customize page that displays all the applications, widgets, and shortcuts.
 */
public class AppsCustomizePagedView extends PagedViewWithDraggableItems implements
        View.OnClickListener, View.OnKeyListener, DragSource,
        PagedViewIcon.PressedCallback, PagedViewWidget.ShortPressListener,
        LauncherTransitionable {
    static final String TAG = "AppsCustomizePagedView";

    /**
     * The different content types that this paged view can show.
     */
    public enum ContentType {
        Applications,
        Widgets,
        Disney
    }

    // Refs
    private Launcher mLauncher;
    private DragController mDragController;
    private final LayoutInflater mLayoutInflater;
    private final PackageManager mPackageManager;

    // Save and Restore
    private int mSaveInstanceStateItemIndex = -1;
    private PagedViewIcon mPressedIcon;

    // Content
    private ArrayList<ApplicationInfo> mApps;
    private ArrayList<Object> mWidgets;
    private ArrayList<ApplicationInfo> mDisneyApps;

    // Caching
    private Canvas mCanvas;
    private IconCache mIconCache;

    // Dimens
    private int mContentWidth;
    private int mMaxAppCellCountX, mMaxAppCellCountY;
    private int mWidgetCountX, mWidgetCountY;
    private int mWidgetWidthGap, mWidgetHeightGap;

    private int mNumAppsPages;
    private int mNumWidgetPages;
    private int mNumDisneyPages;

    // Relating to the scroll and overscroll effects
    Workspace.ZInterpolator mZInterpolator = new Workspace.ZInterpolator(0.5f);
    private static float CAMERA_DISTANCE = 6500;
    private static float TRANSITION_SCALE_FACTOR = 0.74f;
    private static float TRANSITION_PIVOT = 0.65f;
    private static float TRANSITION_MAX_ROTATION = 22;
    private static final boolean PERFORM_OVERSCROLL_ROTATION = true;
    private AccelerateInterpolator mAlphaInterpolator = new AccelerateInterpolator(0.9f);
    private DecelerateInterpolator mLeftScreenAlphaInterpolator = new DecelerateInterpolator(4);

    // Previews & outlines
    ArrayList<AppsCustomizeAsyncTask> mRunningTasks;
    private static final int sPageSleepDelay = 200;

    private Runnable mInflateWidgetRunnable = null;
    private Runnable mBindWidgetRunnable = null;
    static final int WIDGET_NO_CLEANUP_REQUIRED = -1;
    static final int WIDGET_PRELOAD_PENDING = 0;
    static final int WIDGET_BOUND = 1;
    static final int WIDGET_INFLATED = 2;
    int mWidgetCleanupState = WIDGET_NO_CLEANUP_REQUIRED;
    int mWidgetLoadingId = -1;
    PendingAddWidgetInfo mCreateWidgetInfo = null;
    private boolean mDraggingWidget = false;

    private Toast mWidgetInstructionToast;

    // Deferral of loading widget previews during launcher transitions
    private boolean mInTransition;
    private ArrayList<AsyncTaskPageData> mDeferredSyncWidgetPageItems =
        new ArrayList<AsyncTaskPageData>();
    private ArrayList<Runnable> mDeferredPrepareLoadWidgetPreviewsTasks =
        new ArrayList<Runnable>();

    private Rect mTmpRect = new Rect();

    // Used for drawing shortcut previews
    BitmapCache mCachedShortcutPreviewBitmap = new BitmapCache();
    PaintCache mCachedShortcutPreviewPaint = new PaintCache();
    CanvasCache mCachedShortcutPreviewCanvas = new CanvasCache();

    // Used for drawing widget previews
    CanvasCache mCachedAppWidgetPreviewCanvas = new CanvasCache();
    RectCache mCachedAppWidgetPreviewSrcRect = new RectCache();
    RectCache mCachedAppWidgetPreviewDestRect = new RectCache();
    PaintCache mCachedAppWidgetPreviewPaint = new PaintCache();

    WidgetPreviewLoader mWidgetPreviewLoader;

    private boolean mInBulkBind;
    private boolean mNeedToUpdatePageCountsAndInvalidateData;

    private int mCellCountX = 0;
    private int mCellCountY = 0;

    private static final String[] mAlphabets;
    private static final String[][] mKanas;
    private static final String[][] mHiraganas;
    static {

        mAlphabets = new String[] {
                "A", "B", "C", "D", "E",
                "F", "G", "H", "I", "J",
                "K", "L", "M", "N", "O",
                "P", "Q", "R", "S", "T",
                "U", "V", "W", "X", "Y",
                "Z" };

        mKanas = new String[][] {
                { "ア", "イ", "ウ", "エ", "オ", "ァ", "ィ", "ゥ", "ェ", "ォ", "ヴ", "ｱ", "ｲ", "ｳ", "ｴ", "ｵ" },
                { "カ", "キ", "ク", "ケ", "コ", "ガ", "ギ", "グ", "ゲ", "ゴ", "ｶ", "ｷ", "ｸ", "ｹ", "ｺ" },
                { "サ", "シ", "ス", "セ", "ソ", "ザ", "ジ", "ズ", "ゼ", "ゾ", "ｻ", "ｼ", "ｽ", "ｾ", "ｿ" },
                { "タ", "チ", "ツ", "テ", "ト", "ダ", "ヂ", "ヅ", "デ", "ド", "ッ", "ﾀ", "ﾁ", "ﾂ", "ﾃ", "ﾄ" },
                { "ナ", "ニ", "ヌ", "ネ", "ノ", "ﾅ", "ﾆ", "ﾇ", "ﾈ", "ﾉ" },
                { "ハ", "ヒ", "フ", "ヘ", "ホ", "バ", "ビ", "ブ", "ベ", "ボ", "パ", "ピ", "プ", "ペ", "ポ", "ﾊ", "ﾋ", "ﾌ", "ﾍ", "ﾎ" },
                { "マ", "ミ", "ム", "メ", "モ", "ﾏ", "ﾐ", "ﾑ", "ﾒ", "ﾓ" },
                { "ヤ", "ユ", "ヨ", "ャ", "ュ", "ョ", "ﾔ", "ﾕ", "ﾖ" },
                { "ラ", "リ", "ル", "レ", "ロ", "ﾗ", "ﾘ", "ﾙ", "ﾚ", "ﾛ" },
                { "ワ", "ヲ", "ン", "ﾜ", "ｦ", "ﾝ" }
        };

        mHiraganas = new String[][] {
                { "あ", "い", "う", "え", "お", "ぁ", "ぃ", "ぅ", "ぇ", "ぉ", "ゐ", "ゑ" },
                { "か", "き", "く", "け", "こ", "が", "ぎ", "ぐ", "げ", "ご" },
                { "さ", "し", "す", "せ", "そ", "ざ", "じ", "ず", "ぜ", "ぞ" },
                { "た", "ち", "つ", "て", "と", "だ", "ぢ", "づ", "で", "ど", "っ" },
                { "な", "に", "ぬ", "ね", "の" },
                { "は", "ひ", "ふ", "へ", "ほ", "は", "び", "ぶ", "べ", "ぼ", "ぱ", "ぴ", "ぷ", "ぺ", "ぽ" },
                { "ま", "み", "む", "め", "も" },
                { "や", "ゆ", "よ", "ゃ", "ゅ", "ょ" },
                { "ら", "り", "る", "れ", "ろ" },
                { "わ", "を", "ん" }
        };
    }

    private int mFilterCategory = 0;
    private int mFilterAlphabet = -1;
    private int mFilterKana = -1;

    private Bitmap mGlowBmp;
    private Bitmap mEdgeBmp;
    private Bitmap mGlowRightBmp;
    private Bitmap mEdgeRightBmp;

    @SuppressLint("NewApi")
    public AppsCustomizePagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLayoutInflater = LayoutInflater.from(context);
        mPackageManager = context.getPackageManager();
        mApps = new ArrayList<ApplicationInfo>();
        mDisneyApps = new ArrayList<ApplicationInfo>();
        mWidgets = new ArrayList<Object>();
        mIconCache = ((LauncherApplication) context.getApplicationContext()).getIconCache();
        mCanvas = new Canvas();
        mRunningTasks = new ArrayList<AppsCustomizeAsyncTask>();

        // Save the default widget preview background
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AppsCustomizePagedView, 0, 0);
        mMaxAppCellCountX = a.getInt(R.styleable.AppsCustomizePagedView_maxAppCellCountX, -1);
        mMaxAppCellCountY = a.getInt(R.styleable.AppsCustomizePagedView_maxAppCellCountY, -1);
        mWidgetWidthGap =
            a.getDimensionPixelSize(R.styleable.AppsCustomizePagedView_widgetCellWidthGap, 0);
        mWidgetHeightGap =
            a.getDimensionPixelSize(R.styleable.AppsCustomizePagedView_widgetCellHeightGap, 0);
        mWidgetCountX = a.getInt(R.styleable.AppsCustomizePagedView_widgetCountX, 2);
        mWidgetCountY = a.getInt(R.styleable.AppsCustomizePagedView_widgetCountY, 2);
        a.recycle();

        Bitmap glowBmp = BitmapUtils.decodeToBitmap(context, R.drawable.overscroll_glow);
        Bitmap glowBmp2 = highlightImage(glowBmp);
        glowBmp.recycle();
        mGlowBmp = glowBmp2;

        mEdgeBmp = BitmapUtils.decodeToBitmap(context, R.drawable.overscroll_edge);

        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1);
        mGlowRightBmp = Bitmap.createBitmap(mGlowBmp, 0, 0, mGlowBmp.getWidth(), mGlowBmp.getHeight(), matrix, false);
        mEdgeRightBmp = Bitmap.createBitmap(mEdgeBmp, 0, 0, mEdgeBmp.getWidth(), mEdgeBmp.getHeight(), matrix, false);

        mPageSpacing = (int) Math.floor(30 * LauncherApplication.getScreenDensity());

        // The padding on the non-matched dimension for the default widget preview icons
        // (top + bottom)
        mFadeInAdjacentScreens = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Unless otherwise specified this view is important for accessibility.
            if (getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
                setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        mCenterPagesVertically = false;

        Context context = getContext();
        Resources r = context.getResources();
        setDragSlopeThreshold(r.getInteger(R.integer.config_appsCustomizeDragSlopeThreshold)/100f);
    }

    public Bitmap highlightImage(Bitmap src) {
        // create new bitmap, which will be painted and becomes result image
        Bitmap bmOut = Bitmap.createBitmap(src.getWidth() + 96, src.getHeight() + 96, Bitmap.Config.ARGB_8888);
        // setup canvas for painting
        Canvas canvas = new Canvas(bmOut);
        // setup default color
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        // create a blur paint for capturing alpha
        Paint ptBlur = new Paint();
        ptBlur.setMaskFilter(new BlurMaskFilter(15, Blur.NORMAL));
        int[] offsetXY = new int[2];
        // capture alpha into a bitmap
        Bitmap bmAlpha = src.extractAlpha(ptBlur, offsetXY);
        // create a color paint
        Paint ptAlphaColor = new Paint();
        ptAlphaColor.setColor(0xFFFFFFFF);
        // paint color for captured alpha region (bitmap)
        canvas.drawBitmap(bmAlpha, offsetXY[0], offsetXY[1], ptAlphaColor);
        // free memory
        bmAlpha.recycle();

        // paint the image source
        canvas.drawBitmap(src, 0, 0, null);

        // return out final image
        return bmOut;
    }

    private Integer mThemeTextColor;

    public void setThemeTextColor(String textColor) {
        if (textColor == null) return;

        try {
             mThemeTextColor = Color.parseColor(textColor);
        } catch (IllegalArgumentException e) {
            mThemeTextColor = null;
        }
    }

    /** Returns the item index of the center item on this page so that we can restore to this
     *  item index when we rotate. */
    private int getMiddleComponentIndexOnCurrentPage() {
        int i = -1;
        if (getPageCount() > 0) {
            int currentPage = getCurrentPage();
            if (currentPage < mNumAppsPages) {
                PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(currentPage);
                PagedViewCellLayoutChildren childrenLayout = layout.getChildrenLayout();
                int numItemsPerPage = mCellCountX * mCellCountY;
                int childCount = childrenLayout.getChildCount();
                if (childCount > 0) {
                    i = (currentPage * numItemsPerPage) + (childCount / 2);
                }
            } else if (currentPage < mNumAppsPages + mNumDisneyPages) {
                int numApps = mApps.size();
                PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(currentPage);
                PagedViewCellLayoutChildren childrenLayout = layout.getChildrenLayout();
                int numItemsPerPage = mCellCountX * mCellCountY;
                int childCount = childrenLayout.getChildCount();
                if (childCount > 0) {
                    i = numApps + ((currentPage - mNumAppsPages) * numItemsPerPage) + (childCount / 2);
                }
            } else {
                int numApps = mApps.size();
                int numDisney = mDisneyApps.size();
                PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(currentPage);
                int numItemsPerPage = mWidgetCountX * mWidgetCountY;
                int childCount = layout.getChildCount();
                if (childCount > 0) {
                    i = numApps + numDisney +
                        ((currentPage - mNumAppsPages - mNumDisneyPages) * numItemsPerPage) + (childCount / 2);
                }
            }
        }
        return i;
    }

    /** Get the index of the item to restore to if we need to restore the current page. */
    int getSaveInstanceStateIndex() {
        if (mSaveInstanceStateItemIndex == -1) {
            mSaveInstanceStateItemIndex = getMiddleComponentIndexOnCurrentPage();
        }
        return mSaveInstanceStateItemIndex;
    }

    /** Returns the page which is expected to contain the specified
     *  item index. */
    int getPageForComponent(int index) {
        if (index < 0) return 0;

        if (index < mApps.size()) {
            int numItemsPerPage = mCellCountX * mCellCountY;
            return (index / numItemsPerPage);
        } else if (index < mApps.size() + mDisneyApps.size()) {
            int numItemsPerPage = mCellCountX * mCellCountY;
            return mNumAppsPages + ((index - mApps.size()) / numItemsPerPage);
        } else {
            int numItemsPerPage = mWidgetCountX * mWidgetCountY;
            return mNumAppsPages + mNumDisneyPages + ((index - mApps.size() - mDisneyApps.size()) / numItemsPerPage);
        }
    }

    /** Restores the page for an item at the specified index */
    void restorePageForIndex(int index) {
        if (index < 0) return;
        mSaveInstanceStateItemIndex = index;
    }

    public void updatePageCounts() {

        AppsCustomizeTabHost tabHost = getTabHost();
        if (tabHost != null) {
            String tag = tabHost.getCurrentTabTag();
            if (tag != null) {

                if (tag.equals(tabHost.getTabTagForContentType(ContentType.Widgets))) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        mNumWidgetPages = (int) Math.ceil(mWidgets.size() /
                                (float) (mWidgetCountX * mWidgetCountY));
                        if (mNumWidgetPages == 0) mNumWidgetPages = 1;
                    } else {
                        // Android 4.0ではウィジェットページを表示しない
                        mNumWidgetPages = 0;
                    }

                    mNumAppsPages = 0;
                    mNumDisneyPages = 0;

                } else if (tag.equals(tabHost.getTabTagForContentType(ContentType.Disney))) {

                    mNumDisneyPages = (int) Math.ceil((float) mDisneyApps.size() / (mCellCountX * mCellCountY));
                    if (mNumDisneyPages == 0) mNumDisneyPages = 1;

                    mNumAppsPages = 0;
                    mNumWidgetPages = 0;

                } else if (tag.equals(tabHost.getTabTagForContentType(ContentType.Applications))) {

                    int count = 0;
                    for (ApplicationInfo info : mApps) {
                        if (filterApp(info)) {
                            count++;
                        }
                    }
                    mNumAppsPages = (int) Math.ceil((float) count / (mCellCountX * mCellCountY));
                    if (mNumAppsPages == 0) mNumAppsPages = 1;

                    mNumDisneyPages = 0;
                    mNumWidgetPages = 0;
                }
            }
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//            mNumWidgetPages = (int) Math.ceil(mWidgets.size() /
//                    (float) (mWidgetCountX * mWidgetCountY));
//            if (mNumWidgetPages == 0) mNumWidgetPages = 1;
//        } else {
//            // Android 4.0ではウィジェットページを表示しない
//            mNumWidgetPages = 0;
//        }
//
//        int count = 0;
//        for (ApplicationInfo info : mApps) {
//            if (filterApp(info)) {
//                count++;
//            }
//        }
//        mNumAppsPages = (int) Math.ceil((float) count / (mCellCountX * mCellCountY));
//        if (mNumAppsPages == 0) mNumAppsPages = 1;
//
//        mNumDisneyPages = (int) Math.ceil((float) mDisneyApps.size() / (mCellCountX * mCellCountY));
//        if (mNumDisneyPages == 0) mNumDisneyPages = 1;

        if (mPageIndicator != null) {
            if (mCurrentPage < mNumAppsPages) {
                mPageIndicator.setOffset(0);
                mPageIndicator.updateNumPages(mNumAppsPages);
            } else if (mCurrentPage < mNumAppsPages + mNumDisneyPages) {
                mPageIndicator.setOffset(mNumAppsPages);
                mPageIndicator.updateNumPages(mNumDisneyPages);
            } else {
                mPageIndicator.setOffset(mNumAppsPages + mNumDisneyPages);
                mPageIndicator.updateNumPages(mNumWidgetPages);
            }
        }
    }

    public void updateFilterState(int filterCategory, int filterAlphabet, int filterKana) {

        mFilterCategory = filterCategory;
        mFilterAlphabet = filterAlphabet;
        mFilterKana = filterKana;

        updatePageCounts();

        if (mPageIndicator != null) {
            mPageIndicator.setOffset(0);
            mPageIndicator.updateNumPages(mNumAppsPages);
        }
        invalidatePageData(0, false);
    }

    private boolean filterApp(ApplicationInfo info) {

        final int filterCategory = mFilterCategory;

        if (filterCategory == 0) {
            return true;

        } else if (filterCategory == 1) {

            final String appTitle = info.title.toString();
            final int filterAlphabet = mFilterAlphabet;

            if (filterAlphabet < 0 || filterAlphabet >= 26) {
                for (String alphabet : mAlphabets) {
                    if (appTitle.startsWith(alphabet)
                            || appTitle.startsWith(alphabet.toLowerCase())) {
                        return true;
                    }
                }
                return false;
            }

            if (appTitle.startsWith(mAlphabets[filterAlphabet])
                    || appTitle.startsWith(mAlphabets[filterAlphabet].toLowerCase())) {
                return true;
            } else {
                return false;
            }

        } else if (filterCategory == 2) {

            final String appTitle = info.title.toString();
            final int filterKana = mFilterKana;

            if (filterKana < 0 || filterKana >= 10) {
                for(String[] kanas : mKanas) {
                    for (String kana : kanas) {
                        if (appTitle.startsWith(kana)) {
                            return true;
                        }
                    }
                }
                for(String[] hiraganas : mHiraganas) {
                    for (String hiragana : hiraganas) {
                        if (appTitle.startsWith(hiragana)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            for (String kana : mKanas[filterKana]) {
                if (appTitle.startsWith(kana)) {
                    return true;
                }
            }
            for (String hiragana : mHiraganas[filterKana]) {
                if (appTitle.startsWith(hiragana)) {
                    return true;
                }
            }

            return false;
        }

        return true;
    }

    protected void onDataReady(int width, int height) {
        if (mWidgetPreviewLoader == null) {
            mWidgetPreviewLoader = new WidgetPreviewLoader(mLauncher);
        }

        // Note that we transpose the counts in portrait so that we get a similar layout
        boolean isLandscape = getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
        int maxCellCountX = Integer.MAX_VALUE;
        int maxCellCountY = Integer.MAX_VALUE;
        if (LauncherApplication.isScreenLarge()) {
            maxCellCountX = (isLandscape ? LauncherModel.getCellCountX() :
                LauncherModel.getCellCountY());
            maxCellCountY = (isLandscape ? LauncherModel.getCellCountY() :
                LauncherModel.getCellCountX());
        }
        if (mMaxAppCellCountX > -1) {
            maxCellCountX = Math.min(maxCellCountX, mMaxAppCellCountX);
        }
        // Temp hack for now: only use the max cell count Y for widget layout
        int maxWidgetCellCountY = maxCellCountY;
        if (mMaxAppCellCountY > -1) {
            maxWidgetCellCountY = Math.min(maxWidgetCellCountY, mMaxAppCellCountY);
        }

        // Now that the data is ready, we can calculate the content width, the number of cells to
        // use for each page
        final PagedViewCellLayout pagedViewCellLayout = new PagedViewCellLayout(getContext(), null, null, null, null);
        pagedViewCellLayout.setGap(mPageLayoutWidthGap, mPageLayoutHeightGap);
        pagedViewCellLayout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);
        pagedViewCellLayout.calculateCellCount(width, height, maxCellCountX, maxCellCountY);
        mCellCountX = pagedViewCellLayout.getCellCountX();
        mCellCountY = pagedViewCellLayout.getCellCountY();
        updatePageCounts();

        // Force a measure to update recalculate the gaps
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        pagedViewCellLayout.calculateCellCount(width, height, maxCellCountX, maxWidgetCellCountY);
        pagedViewCellLayout.measure(widthSpec, heightSpec);
        mContentWidth = pagedViewCellLayout.getContentWidth();

        AppsCustomizeTabHost host = (AppsCustomizeTabHost) getTabHost();
        final boolean hostIsTransitioning = host.isTransitioning();

        // Restore the page
        int page = getPageForComponent(mSaveInstanceStateItemIndex);
        invalidatePageData(Math.max(0, page), hostIsTransitioning);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (!isDataReady()) {
            if (mApps.size() > 0 && !mWidgets.isEmpty()) {
                setDataIsReady();
                setMeasuredDimension(width, height);
                onDataReady(width, height);
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void onPackagesUpdated(ArrayList<Object> widgetsAndShortcuts) {
        // Get the list of widgets and shortcuts
        mWidgets.clear();
        for (Object o : widgetsAndShortcuts) {
            if (o instanceof AppWidgetProviderInfo) {
                AppWidgetProviderInfo widget = (AppWidgetProviderInfo) o;
                if (widget.minWidth > 0 && widget.minHeight > 0) {
                    // Ensure that all widgets we show can be added on a workspace of this size
                    int[] spanXY = Launcher.getSpanForWidget(mLauncher, widget);
                    int[] minSpanXY = Launcher.getMinSpanForWidget(mLauncher, widget);
                    int minSpanX = Math.min(spanXY[0], minSpanXY[0]);
                    int minSpanY = Math.min(spanXY[1], minSpanXY[1]);
                    if (minSpanX <= LauncherModel.getCellCountX() &&
                        minSpanY <= LauncherModel.getCellCountY()) {
                        mWidgets.add(widget);
                    } else {
                        Log.e(TAG, "Widget " + widget.provider + " can not fit on this device (" +
                              widget.minWidth + ", " + widget.minHeight + ")");
                    }
                } else {
                    Log.e(TAG, "Widget " + widget.provider + " has invalid dimensions (" +
                          widget.minWidth + ", " + widget.minHeight + ")");
                }
            } else {
                // just add shortcuts
                mWidgets.add(o);
            }
        }
        updatePageCountsAndInvalidateData();
    }

    public void setBulkBind(boolean bulkBind) {
        if (bulkBind) {
            mInBulkBind = true;
        } else {
            mInBulkBind = false;
            if (mNeedToUpdatePageCountsAndInvalidateData) {
                updatePageCountsAndInvalidateData();
            }
        }
    }

    private void updatePageCountsAndInvalidateData() {
        if (mInBulkBind) {
            mNeedToUpdatePageCountsAndInvalidateData = true;
        } else {
            updatePageCounts();
            invalidateOnDataChange();
            mNeedToUpdatePageCountsAndInvalidateData = false;
        }
    }

    @Override
    public void onClick(View v) {
        // When we have exited all apps or are in transition, disregard clicks
        if (!mLauncher.isAllAppsVisible() ||
                mLauncher.getWorkspace().isSwitchingState()) return;

        if (v instanceof PagedViewIcon) {
            // Animate some feedback to the click
            final ApplicationInfo appInfo = (ApplicationInfo) v.getTag();

            DebugLog.instance.outputLog(TAG, "[Application Info]: title = " + appInfo.title.toString()
                    + ", component info = " + appInfo.componentName.toString());

            // Lock the drawable state to pressed until we return to Launcher
            if (mPressedIcon != null) {
                mPressedIcon.lockDrawableState();
            }

            // NOTE: We want all transitions from launcher to act as if the wallpaper were enabled
            // to be consistent.  So re-enable the flag here, and we will re-disable it as necessary
            // when Launcher resumes and we are still in AllApps.
            mLauncher.updateWallpaperVisibility(true);
            mLauncher.startActivitySafely(v, appInfo.intent, appInfo);

        } else if (v instanceof PagedViewWidget) {
            // Let the user know that they have to long press to add a widget
            if (mWidgetInstructionToast != null) {
                mWidgetInstructionToast.cancel();
            }
            mWidgetInstructionToast = Toast.makeText(getContext(),R.string.long_press_widget_to_add,
                Toast.LENGTH_SHORT);
            mWidgetInstructionToast.show();

            // Create a little animation to show that the widget can move
            float offsetY = getResources().getDimensionPixelSize(R.dimen.dragViewOffsetY);
            final ImageView p = (ImageView) v.findViewById(R.id.widget_preview);
            AnimatorSet bounce = ViewAnimUtils.createAnimatorSet();
            ValueAnimator tyuAnim = ViewAnimUtils.ofFloat(p, "translationY", offsetY);
            tyuAnim.setDuration(125);
            ValueAnimator tydAnim = ViewAnimUtils.ofFloat(p, "translationY", 0f);
            tydAnim.setDuration(100);
            bounce.play(tyuAnim).before(tydAnim);
            bounce.setInterpolator(new AccelerateInterpolator());
            bounce.start();
        }
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return FocusHelper.handleAppsCustomizeKeyEvent(v,  keyCode, event);
    }

    /*
     * PagedViewWithDraggableItems implementation
     */
    @Override
    protected void determineDraggingStart(android.view.MotionEvent ev) {
        // Disable dragging by pulling an app down for now.
    }

    private void beginDraggingApplication(View v) {
        mLauncher.getWorkspace().onDragStartedWithItem(v);
        mLauncher.getWorkspace().beginDragShared(v, this);
    }

    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void preloadWidget(final PendingAddWidgetInfo info) {
        final AppWidgetProviderInfo pInfo = info.info;

        if (pInfo.configure != null) {
            return;
        }

        mWidgetCleanupState = WIDGET_PRELOAD_PENDING;
        mBindWidgetRunnable = new Runnable() {
            @Override
            public void run() {
                mWidgetLoadingId = mLauncher.getAppWidgetHost().allocateAppWidgetId();

                AppWidgetManager widgetManager = AppWidgetManager.getInstance(mLauncher);

                boolean success = widgetManager.bindAppWidgetIdIfAllowed(mWidgetLoadingId, info.componentName);
                if (success) {
                    mWidgetCleanupState = WIDGET_BOUND;
                }
            }
        };
        post(mBindWidgetRunnable);

        mInflateWidgetRunnable = new Runnable() {
            @Override
            public void run() {
                if (mWidgetCleanupState != WIDGET_BOUND) {
                    return;
                }
                AppWidgetHostView hostView = mLauncher.
                        getAppWidgetHost().createView(getContext(), mWidgetLoadingId, pInfo);
                info.boundWidget = hostView;
                mWidgetCleanupState = WIDGET_INFLATED;
                hostView.setVisibility(INVISIBLE);
                int[] unScaledSize = mLauncher.getWorkspace().estimateItemSize(info.spanX,
                        info.spanY, info, false);

                // We want the first widget layout to be the correct size. This will be important
                // for width size reporting to the AppWidgetManager.
                DragLayer.LayoutParams lp = new DragLayer.LayoutParams(unScaledSize[0],
                        unScaledSize[1]);
                lp.x = lp.y = 0;
                lp.customPosition = true;
                hostView.setLayoutParams(lp);
                mLauncher.getDragLayer().addView(hostView);
            }
        };
        post(mInflateWidgetRunnable);
    }

    @Override
    public void onShortPress(View v) {
        // We are anticipating a long press, and we use this time to load bind and instantiate
        // the widget. This will need to be cleaned up if it turns out no long press occurs.
        if (mCreateWidgetInfo != null) {
            // Just in case the cleanup process wasn't properly executed. This shouldn't happen.
            cleanupWidgetPreloading(false);
        }
        mCreateWidgetInfo = new PendingAddWidgetInfo((PendingAddWidgetInfo) v.getTag());
        preloadWidget(mCreateWidgetInfo);
    }

    private void cleanupWidgetPreloading(boolean widgetWasAdded) {
        if (!widgetWasAdded) {
            // If the widget was not added, we may need to do further cleanup.
            PendingAddWidgetInfo info = mCreateWidgetInfo;
            mCreateWidgetInfo = null;

            if (mWidgetCleanupState == WIDGET_PRELOAD_PENDING) {
                // We never did any preloading, so just remove pending callbacks to do so
                removeCallbacks(mBindWidgetRunnable);
                removeCallbacks(mInflateWidgetRunnable);
            } else if (mWidgetCleanupState == WIDGET_BOUND) {
                 // Delete the widget id which was allocated
                if (mWidgetLoadingId != -1) {
                    mLauncher.getAppWidgetHost().deleteAppWidgetId(mWidgetLoadingId);
                }

                // We never got around to inflating the widget, so remove the callback to do so.
                removeCallbacks(mInflateWidgetRunnable);
            } else if (mWidgetCleanupState == WIDGET_INFLATED) {
                // Delete the widget id which was allocated
                if (mWidgetLoadingId != -1) {
                    mLauncher.getAppWidgetHost().deleteAppWidgetId(mWidgetLoadingId);
                }

                // The widget was inflated and added to the DragLayer -- remove it.
                AppWidgetHostView widget = info.boundWidget;
                mLauncher.getDragLayer().removeView(widget);
            }
        }
        mWidgetCleanupState = WIDGET_NO_CLEANUP_REQUIRED;
        mWidgetLoadingId = -1;
        mCreateWidgetInfo = null;
        PagedViewWidget.resetShortPressTarget();
    }

    @Override
    public void cleanUpShortPress(View v) {
        if (!mDraggingWidget) {
            cleanupWidgetPreloading(false);
        }
    }

    private boolean beginDraggingWidget(View v) {
        mDraggingWidget = true;
        // Get the widget preview as the drag representation
        ImageView image = (ImageView) v.findViewById(R.id.widget_preview);
        PendingAddItemInfo createItemInfo = (PendingAddItemInfo) v.getTag();

        // If the ImageView doesn't have a drawable yet, the widget preview hasn't been loaded and
        // we abort the drag.
        if (image.getDrawable() == null) {
            mDraggingWidget = false;
            return false;
        }

        // Compose the drag image
        Bitmap preview;
        Bitmap outline;
        float scale = 1f;
        Point previewPadding = null;

        if (createItemInfo instanceof PendingAddWidgetInfo) {
            // This can happen in some weird cases involving multi-touch. We can't start dragging
            // the widget if this is null, so we break out.
            if (mCreateWidgetInfo == null) {
                return false;
            }

            PendingAddWidgetInfo createWidgetInfo = mCreateWidgetInfo;
            createItemInfo = createWidgetInfo;
            int spanX = createItemInfo.spanX;
            int spanY = createItemInfo.spanY;
            int[] size = mLauncher.getWorkspace().estimateItemSize(spanX, spanY,
                    createWidgetInfo, true);

            FastBitmapDrawable previewDrawable = (FastBitmapDrawable) image.getDrawable();
            float minScale = 1.25f;
            int maxWidth, maxHeight;
            maxWidth = Math.min((int) (previewDrawable.getIntrinsicWidth() * minScale), size[0]);
            maxHeight = Math.min((int) (previewDrawable.getIntrinsicHeight() * minScale), size[1]);

            int[] previewSizeBeforeScale = new int[1];

            preview = mWidgetPreviewLoader.generateWidgetPreview(createWidgetInfo.info, spanX,
                    spanY, maxWidth, maxHeight, null, previewSizeBeforeScale);

            // Compare the size of the drag preview to the preview in the AppsCustomize tray
            int previewWidthInAppsCustomize = Math.min(previewSizeBeforeScale[0],
                    mWidgetPreviewLoader.maxWidthForWidgetPreview(spanX));
            scale = previewWidthInAppsCustomize / (float) preview.getWidth();

            // The bitmap in the AppsCustomize tray is always the the same size, so there
            // might be extra pixels around the preview itself - this accounts for that
            if (previewWidthInAppsCustomize < previewDrawable.getIntrinsicWidth()) {
                int padding =
                        (previewDrawable.getIntrinsicWidth() - previewWidthInAppsCustomize) / 2;
                previewPadding = new Point(padding, 0);
            }
        } else {
            PendingAddShortcutInfo createShortcutInfo = (PendingAddShortcutInfo) v.getTag();
            Drawable icon = mIconCache.getFullResIcon(createShortcutInfo.shortcutActivityInfo);
            preview = Bitmap.createBitmap(icon.getIntrinsicWidth(),
                    icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

            mCanvas.setBitmap(preview);
            mCanvas.save();
            WidgetPreviewLoader.renderDrawableToBitmap(icon, preview, 0, 0,
                    icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
            mCanvas.restore();
            mCanvas.setBitmap(null);
            createItemInfo.spanX = createItemInfo.spanY = Workspace.ICON_SPAN;
        }

        // Don't clip alpha values for the drag outline if we're using the default widget preview
        boolean clipAlpha = !(createItemInfo instanceof PendingAddWidgetInfo &&
                (((PendingAddWidgetInfo) createItemInfo).info.previewImage == 0));

        // Save the preview for the outline generation, then dim the preview
        outline = Bitmap.createScaledBitmap(preview, preview.getWidth(), preview.getHeight(),
                false);

        // Start the drag
        mLauncher.getWorkspace().onDragStartedWithItem(createItemInfo, outline, clipAlpha);
        mDragController.startDrag(image, preview, this, createItemInfo,
                DragController.DRAG_ACTION_COPY, previewPadding, scale);
        outline.recycle();
        preview.recycle();
        return true;
    }

    @Override
    protected boolean beginDragging(final View v) {
        if (!super.beginDragging(v)) return false;

        if (v instanceof PagedViewIcon) {
            beginDraggingApplication(v);
        } else if (v instanceof PagedViewWidget) {
            if (!beginDraggingWidget(v)) {
                return false;
            }
        }

        // We delay entering spring-loaded mode slightly to make sure the UI
        // thready is free of any work.
        postDelayed(new Runnable() {
            @Override
            public void run() {
                // We don't enter spring-loaded mode if the drag has been cancelled
                if (mLauncher.getDragController().isDragging()) {

                    // Reset the alpha on the dragged icon before we drag
                    resetDrawableState();

                    // Go into spring loaded mode (must happen before we startDrag())
                    mLauncher.enterSpringLoadedDragMode();
                }
            }
        }, 150);

        return true;
    }

    /**
     * Clean up after dragging.
     *
     * @param target where the item was dragged to (can be null if the item was flung)
     */
    private void endDragging(View target, boolean success) {
        if (!success || (target != mLauncher.getWorkspace() &&
                !(target instanceof DeleteDropTarget))) {
            // Exit spring loaded mode if we have not successfully dropped or have not handled the
            // drop in Workspace
            mLauncher.exitSpringLoadedDragMode();
        }
    }

    @Override
    public View getContent() {
        return null;
    }

    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {
        mInTransition = true;
        if (toWorkspace) {
            cancelAllTasks();
        }
    }

    @Override
    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace) {
    }

    @Override
    public void onLauncherTransitionStep(Launcher l, float t) {
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace) {
        mInTransition = false;
        for (AsyncTaskPageData d : mDeferredSyncWidgetPageItems) {
            onSyncWidgetPageItems(d);
        }
        mDeferredSyncWidgetPageItems.clear();
        for (Runnable r : mDeferredPrepareLoadWidgetPreviewsTasks) {
            r.run();
        }
        mDeferredPrepareLoadWidgetPreviewsTasks.clear();
        mForceDrawAllChildrenNextFrame = !toWorkspace;
    }

    @Override
    public boolean isMainView() {
        return false;
    };

    @Override
    public void onDropCompleted(View target, DragObject d, boolean success) {

        endDragging(target, success);

        // Display an error message if the drag failed due to there not being enough space on the
        // target layout we were dropping on.
        if (!success) {
            boolean showOutOfSpaceMessage = false;
            if (target instanceof Workspace) {
                int currentScreen = mLauncher.getCurrentWorkspaceScreen();
                Workspace workspace = (Workspace) target;
                CellLayout layout = (CellLayout) workspace.getChildAt(currentScreen);
                ItemInfo itemInfo = (ItemInfo) d.dragInfo;
                if (layout != null) {
                    layout.calculateSpans(itemInfo, Workspace.ICON_SPAN);
                    showOutOfSpaceMessage =
                            !layout.findCellForSpan(null, itemInfo.spanX, itemInfo.spanY);
                }
            }
            if (showOutOfSpaceMessage) {
                mLauncher.showOutOfSpaceMessage(false);
            }

            d.deferDragViewCleanupPostAnimation = false;
        }
        cleanupWidgetPreloading(success);
        mDraggingWidget = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAllTasks();
    }

    public void clearAllWidgetPages() {
        cancelAllTasks();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View v = getPageAt(i);
            if (v instanceof PagedViewGridLayout) {
                ((PagedViewGridLayout) v).removeAllViewsOnPage();
                mDirtyPageContent.set(i, true);
            }
        }
    }

    private void cancelAllTasks() {
        // Clean up all the async tasks
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            task.cancel(false);
            iter.remove();
            mDirtyPageContent.set(task.page, true);

            // We've already preallocated the views for the data to load into, so clear them as well
            View v = getPageAt(task.page);
            if (v instanceof PagedViewGridLayout) {
                ((PagedViewGridLayout) v).removeAllViewsOnPage();
            }
        }
        mDeferredSyncWidgetPageItems.clear();
        mDeferredPrepareLoadWidgetPreviewsTasks.clear();
    }

    public void setContentType(ContentType type) {
        if (type == ContentType.Widgets) {
            if (mPageIndicator != null) {
                mPageIndicator.setOffset(mNumAppsPages + mNumDisneyPages);
                mPageIndicator.updateNumPages(mNumWidgetPages);
            }
            invalidatePageData(mNumAppsPages + mNumDisneyPages, true);
        } else if (type == ContentType.Applications) {
            if (mPageIndicator != null) {
                mPageIndicator.setOffset(0);
                mPageIndicator.updateNumPages(mNumAppsPages);
            }
            invalidatePageData(0, true);
        } else if (type == ContentType.Disney) {
            if (mPageIndicator != null) {
                mPageIndicator.setOffset(mNumAppsPages);
                mPageIndicator.updateNumPages(mNumDisneyPages);
            }
            invalidatePageData(mNumAppsPages, true);
        }
    }

    @Override
    public void snapToPage(int whichPage, int delta, int duration) {
        super.snapToPage(whichPage, delta, duration);
        updateCurrentTab(whichPage);

        // Update the thread priorities given the direction lookahead
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            int pageIndex = task.page;
            if ((mNextPage > mCurrentPage && pageIndex >= mCurrentPage) ||
                (mNextPage < mCurrentPage && pageIndex <= mCurrentPage)) {
                task.setThreadPriority(getThreadPriorityForPage(pageIndex));
            } else {
                task.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
            }
        }
    }

    private void updateCurrentTab(int currentPage) {
        AppsCustomizeTabHost tabHost = getTabHost();
        if (tabHost != null) {
            String tag = tabHost.getCurrentTabTag();
            if (tag != null) {
                if (currentPage >= mNumAppsPages + mNumDisneyPages &&
                        !tag.equals(tabHost.getTabTagForContentType(ContentType.Widgets))) {
                    if (mPageIndicator != null) {
                        mPageIndicator.setOffset(mNumAppsPages + mNumDisneyPages);
                        mPageIndicator.updateNumPages(mNumWidgetPages);
                    }
                    tabHost.setCurrentTabFromContent(ContentType.Widgets);
                } else if (currentPage >= mNumAppsPages && currentPage < mNumAppsPages + mNumDisneyPages
                        && !tag.equals(tabHost.getTabTagForContentType(ContentType.Disney))) {
                    if (mPageIndicator != null) {
                        mPageIndicator.setOffset(mNumAppsPages);
                        mPageIndicator.updateNumPages(mNumDisneyPages);
                    }
                    tabHost.setCurrentTabFromContent(ContentType.Disney);
                } else if (currentPage < mNumAppsPages &&
                        !tag.equals(tabHost.getTabTagForContentType(ContentType.Applications))) {
                    if (mPageIndicator != null) {
                        mPageIndicator.setOffset(0);
                        mPageIndicator.updateNumPages(mNumAppsPages);
                    }
                    tabHost.setCurrentTabFromContent(ContentType.Applications);
                }
            }
        }
    }

    /*
     * Apps PagedView implementation
     */
    private void setVisibilityOnChildren(ViewGroup layout, int visibility) {
        int childCount = layout.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            layout.getChildAt(i).setVisibility(visibility);
        }
    }
    private void setupPage(PagedViewCellLayout layout) {
        layout.setCellCount(mCellCountX, mCellCountY);
        layout.setGap(mPageLayoutWidthGap, mPageLayoutHeightGap);
        layout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);

        // Note: We force a measure here to get around the fact that when we do layout calculations
        // immediately after syncing, we don't have a proper width.  That said, we already know the
        // expected page width, so we can actually optimize by hiding all the TextView-based
        // children that are expensive to measure, and let that happen naturally later.
        setVisibilityOnChildren(layout, View.GONE);
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        layout.setMinimumWidth(getPageContentWidth());
        layout.measure(widthSpec, heightSpec);
        setVisibilityOnChildren(layout, View.VISIBLE);
    }

    public void syncAppsPageItems(int page, boolean immediate) {

        PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page);
        if (layout == null) return;

        layout.removeAllViewsOnPage();

        final int numCells = mCellCountX * mCellCountY;
        final int appsSize = mApps.size();

        int prevCount = 0;
        int count = 0;
        for (int i = 0; i < appsSize; ++i) {
            ApplicationInfo info = mApps.get(i);

            if (!filterApp(info)) continue;

            // 過去ページ対象判定
            if (prevCount < page * numCells) {
                prevCount++;
                continue;
            }

            DebugLog.instance.outputLog(TAG, "[Application Info]: title = " + info.title.toString()
                    + ", component info = " + info.componentName.toString());

            PagedViewIcon icon = (PagedViewIcon) mLayoutInflater.inflate(
                    R.layout.apps_customize_application, layout, false);
            icon.applyFromApplicationInfo(info, true, this, layout.getCellWidth(), mIconCache, LauncherApplication.getScreenDensity());
            icon.setOnClickListener(this);
            icon.setOnLongClickListener(this);
            icon.setOnTouchListener(this);
            icon.setOnKeyListener(this);
            if (mThemeTextColor != null) {
                icon.setTextColor(mThemeTextColor);
            }

            int x = count % mCellCountX;
            int y = count / mCellCountX;
            layout.addViewToCellLayout(icon, -1, count, new PagedViewCellLayout.LayoutParams(x,y, 1,1));
            count++;

            if (count > numCells) break;
        }

        enableHwLayersOnVisiblePages();
    }

    public void syncDisneyPageItems(int page, boolean immediate) {
        // ensure that we have the right number of items on the pages
        int numCells = mCellCountX * mCellCountY;
        int startIndex = (page - mNumAppsPages) * numCells;
        int endIndex = Math.min(startIndex + numCells, mDisneyApps.size());
        PagedViewCellLayout layout = (PagedViewCellLayout) getPageAt(page);
        if (layout == null) return;

        layout.removeAllViewsOnPage();
        for (int i = startIndex; i < endIndex; ++i) {
            ApplicationInfo info = mDisneyApps.get(i);

            PagedViewIcon icon = (PagedViewIcon) mLayoutInflater.inflate(
                    R.layout.apps_customize_application, layout, false);
            icon.applyFromApplicationInfo(info, true, this, layout.getCellWidth(), mIconCache, LauncherApplication.getScreenDensity());
            icon.setOnClickListener(this);
            icon.setOnLongClickListener(this);
            icon.setOnTouchListener(this);
            icon.setOnKeyListener(this);
            if (mThemeTextColor != null) {
                icon.setTextColor(mThemeTextColor);
            }

            int index = i - startIndex;
            int x = index % mCellCountX;
            int y = index / mCellCountX;
            layout.addViewToCellLayout(icon, -1, i, new PagedViewCellLayout.LayoutParams(x,y, 1,1));
        }

        enableHwLayersOnVisiblePages();
    }

    /**
     * A helper to return the priority for loading of the specified widget page.
     */
    private int getWidgetPageLoadPriority(int page) {
        // If we are snapping to another page, use that index as the target page index
        int toPage = mCurrentPage;
        if (mNextPage > -1) {
            toPage = mNextPage;
        }

        // We use the distance from the target page as an initial guess of priority, but if there
        // are no pages of higher priority than the page specified, then bump up the priority of
        // the specified page.
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        int minPageDiff = Integer.MAX_VALUE;
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            minPageDiff = Math.abs(task.page - toPage);
        }

        int rawPageDiff = Math.abs(page - toPage);
        return rawPageDiff - Math.min(rawPageDiff, minPageDiff);
    }
    /**
     * Return the appropriate thread priority for loading for a given page (we give the current
     * page much higher priority)
     */
    private int getThreadPriorityForPage(int page) {
        // TODO-APPS_CUSTOMIZE: detect number of cores and set thread priorities accordingly below
        int pageDiff = getWidgetPageLoadPriority(page);
        if (pageDiff <= 0) {
            return Process.THREAD_PRIORITY_LESS_FAVORABLE;
        } else if (pageDiff <= 1) {
            return Process.THREAD_PRIORITY_LOWEST;
        } else {
            return Process.THREAD_PRIORITY_LOWEST;
        }
    }
    private int getSleepForPage(int page) {
        int pageDiff = getWidgetPageLoadPriority(page);
        return Math.max(0, pageDiff * sPageSleepDelay);
    }
    /**
     * Creates and executes a new AsyncTask to load a page of widget previews.
     */
    private void prepareLoadWidgetPreviewsTask(int page, ArrayList<Object> widgets,
            int cellWidth, int cellHeight, int cellCountX) {

        // Prune all tasks that are no longer needed
        Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
        while (iter.hasNext()) {
            AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
            int taskPage = task.page;
            if (taskPage < getAssociatedLowerPageBound(mCurrentPage) ||
                    taskPage > getAssociatedUpperPageBound(mCurrentPage)) {
                task.cancel(false);
                iter.remove();
            } else {
                task.setThreadPriority(getThreadPriorityForPage(taskPage));
            }
        }

        // We introduce a slight delay to order the loading of side pages so that we don't thrash
        final int sleepMs = getSleepForPage(page);
        AsyncTaskPageData pageData = new AsyncTaskPageData(page, widgets, cellWidth, cellHeight,
            new AsyncTaskCallback() {
                @Override
                public void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
                    try {
                        try {
                            Thread.sleep(sleepMs);
                        } catch (Exception e) {}
                        loadWidgetPreviewsInBackground(task, data);
                    } finally {
                        if (task.isCancelled()) {
                            data.cleanup(true);
                        }
                    }
                }
            },
            new AsyncTaskCallback() {
                @Override
                public void run(AppsCustomizeAsyncTask task, AsyncTaskPageData data) {
                    mRunningTasks.remove(task);
                    if (task.isCancelled()) return;
                    // do cleanup inside onSyncWidgetPageItems
                    onSyncWidgetPageItems(data);
                }
            }, mWidgetPreviewLoader);

        // Ensure that the task is appropriately prioritized and runs in parallel
        AppsCustomizeAsyncTask t = new AppsCustomizeAsyncTask(page,
                AsyncTaskPageData.Type.LoadWidgetPreviewData);
        t.setThreadPriority(getThreadPriorityForPage(page));
        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pageData);
        mRunningTasks.add(t);
    }

    /*
     * Widgets PagedView implementation
     */
    private void setupPage(PagedViewGridLayout layout) {
        layout.setPadding(mPageLayoutPaddingLeft, mPageLayoutPaddingTop,
                mPageLayoutPaddingRight, mPageLayoutPaddingBottom);

        // Note: We force a measure here to get around the fact that when we do layout calculations
        // immediately after syncing, we don't have a proper width.
        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.AT_MOST);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.AT_MOST);
        layout.setMinimumWidth(getPageContentWidth());
        layout.measure(widthSpec, heightSpec);
    }

    public void syncWidgetPageItems(final int page, final boolean immediate) {
        int numItemsPerPage = mWidgetCountX * mWidgetCountY;

        final PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(page);
        if (layout == null) return;

        // Calculate the dimensions of each cell we are giving to each widget
        final ArrayList<Object> items = new ArrayList<Object>();
        int contentWidth = layout.getMeasuredWidth();
        final int cellWidth = ((contentWidth - mPageLayoutPaddingLeft - mPageLayoutPaddingRight
                - ((mWidgetCountX - 1) * mWidgetWidthGap)) / mWidgetCountX);
//        int contentHeight = mWidgetSpacingLayout.getContentHeight();
        int contentHeight = layout.getMeasuredHeight();

        final int cellHeight = ((contentHeight - mPageLayoutPaddingTop - mPageLayoutPaddingBottom
                - ((mWidgetCountY - 1) * mWidgetHeightGap)) / mWidgetCountY);

        layout.setCellSize(cellWidth, cellHeight);

        // Prepare the set of widgets to load previews for in the background
        int offset = (page - mNumAppsPages - mNumDisneyPages) * numItemsPerPage;
        for (int i = offset; i < Math.min(offset + numItemsPerPage, mWidgets.size()); ++i) {
            items.add(mWidgets.get(i));
        }

        // Prepopulate the pages with the other widget info, and fill in the previews later
        layout.setColumnCount(layout.getCellCountX());
        for (int i = 0; i < items.size(); ++i) {
            Object rawInfo = items.get(i);
            PendingAddItemInfo createItemInfo = null;
            PagedViewWidget widget = (PagedViewWidget) mLayoutInflater.inflate(
                    R.layout.apps_customize_widget, layout, false);
            if (rawInfo instanceof AppWidgetProviderInfo) {
                // Fill in the widget information
                AppWidgetProviderInfo info = (AppWidgetProviderInfo) rawInfo;
                createItemInfo = new PendingAddWidgetInfo(info, null, null);

                // Determine the widget spans and min resize spans.
                int[] spanXY = Launcher.getSpanForWidget(mLauncher, info);
                createItemInfo.spanX = spanXY[0];
                createItemInfo.spanY = spanXY[1];
                int[] minSpanXY = Launcher.getMinSpanForWidget(mLauncher, info);
                createItemInfo.minSpanX = minSpanXY[0];
                createItemInfo.minSpanY = minSpanXY[1];

                widget.applyFromAppWidgetProviderInfo(info, -1, spanXY, mWidgetPreviewLoader, mThemeTextColor);
                widget.setTag(createItemInfo);
                widget.setShortPressListener(this);
            } else if (rawInfo instanceof ResolveInfo) {
                // Fill in the shortcuts information
                ResolveInfo info = (ResolveInfo) rawInfo;
                createItemInfo = new PendingAddShortcutInfo(info.activityInfo);
                createItemInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
                createItemInfo.componentName = new ComponentName(info.activityInfo.packageName,
                        info.activityInfo.name);
                widget.applyFromResolveInfo(mPackageManager, info, mWidgetPreviewLoader, mThemeTextColor);
                widget.setTag(createItemInfo);
            }
            widget.setOnClickListener(this);
            widget.setOnLongClickListener(this);
            widget.setOnTouchListener(this);
            widget.setOnKeyListener(this);

            // Layout each widget
            int ix = i % mWidgetCountX;
            int iy = i / mWidgetCountX;
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(iy, GridLayout.LEFT),
                    GridLayout.spec(ix, GridLayout.TOP));
            lp.width = cellWidth;
            lp.height = cellHeight;
            lp.setGravity(Gravity.TOP | Gravity.LEFT);
            if (ix > 0) lp.leftMargin = mWidgetWidthGap;
            if (iy > 0) lp.topMargin = mWidgetHeightGap;
            layout.addView(widget, lp);
        }

        // wait until a call on onLayout to start loading, because
        // PagedViewWidget.getPreviewSize() will return 0 if it hasn't been laid out
        // TODO: can we do a measure/layout immediately?
        layout.setOnLayoutListener(new Runnable() {
            public void run() {
                // Load the widget previews
                int maxPreviewWidth = cellWidth;
                int maxPreviewHeight = cellHeight;
                if (layout.getChildCount() > 0) {
                    PagedViewWidget w = (PagedViewWidget) layout.getChildAt(0);
                    int[] maxSize = w.getPreviewSize();
                    maxPreviewWidth = maxSize[0];
                    maxPreviewHeight = maxSize[1];
                }

                mWidgetPreviewLoader.setPreviewSize(
                        maxPreviewWidth, maxPreviewHeight, layout);
                if (immediate) {
                    AsyncTaskPageData data = new AsyncTaskPageData(page, items,
                            maxPreviewWidth, maxPreviewHeight, null, null, mWidgetPreviewLoader);
                    loadWidgetPreviewsInBackground(null, data);
                    onSyncWidgetPageItems(data);
                } else {
                    if (mInTransition) {
                        mDeferredPrepareLoadWidgetPreviewsTasks.add(this);
                    } else {
                        prepareLoadWidgetPreviewsTask(page, items,
                                maxPreviewWidth, maxPreviewHeight, mWidgetCountX);
                    }
                }
                layout.setOnLayoutListener(null);
            }
        });
    }
    private void loadWidgetPreviewsInBackground(AppsCustomizeAsyncTask task,
            AsyncTaskPageData data) {
        // loadWidgetPreviewsInBackground can be called without a task to load a set of widget
        // previews synchronously
        if (task != null) {
            // Ensure that this task starts running at the correct priority
            task.syncThreadPriority();
        }

        // Load each of the widget/shortcut previews
        ArrayList<Object> items = data.items;
        ArrayList<Bitmap> images = data.generatedImages;
        int count = items.size();
        for (int i = 0; i < count; ++i) {
            if (task != null) {
                // Ensure we haven't been cancelled yet
                if (task.isCancelled()) break;
                // Before work on each item, ensure that this task is running at the correct
                // priority
                task.syncThreadPriority();
            }

            images.add(mWidgetPreviewLoader.getPreview(items.get(i)));
        }
    }

    private void onSyncWidgetPageItems(AsyncTaskPageData data) {
        if (mInTransition) {
            mDeferredSyncWidgetPageItems.add(data);
            return;
        }
        try {
            int page = data.page;
            PagedViewGridLayout layout = (PagedViewGridLayout) getPageAt(page);

            ArrayList<Object> items = data.items;
            int count = items.size();
            for (int i = 0; i < count; ++i) {
                PagedViewWidget widget = (PagedViewWidget) layout.getChildAt(i);
                if (widget != null) {
                    Bitmap preview = data.generatedImages.get(i);
                    widget.applyPreview(new FastBitmapDrawable(preview), i);
                }
            }

            enableHwLayersOnVisiblePages();

            // Update all thread priorities
            Iterator<AppsCustomizeAsyncTask> iter = mRunningTasks.iterator();
            while (iter.hasNext()) {
                AppsCustomizeAsyncTask task = (AppsCustomizeAsyncTask) iter.next();
                int pageIndex = task.page;
                task.setThreadPriority(getThreadPriorityForPage(pageIndex));
            }
        } finally {
            data.cleanup(false);
        }
    }

    @Override
    public void syncPages() {
        removeAllViews();
        cancelAllTasks();

        Context context = getContext();
        for (int j = 0; j < mNumWidgetPages; ++j) {
            PagedViewGridLayout layout = new PagedViewGridLayout(context, mWidgetCountX,
                    mWidgetCountY, mGlowBmp, mEdgeBmp, mGlowRightBmp, mEdgeRightBmp);
            setupPage(layout);
            addView(layout, new PagedView.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
        }

        for (int k = 0; k < mNumDisneyPages; ++k) {
            PagedViewCellLayout layout = new PagedViewCellLayout(context, mGlowBmp, mEdgeBmp, mGlowRightBmp, mEdgeRightBmp);
            setupPage(layout);
            addView(layout);
        }

        for (int i = 0; i < mNumAppsPages; ++i) {
            PagedViewCellLayout layout = new PagedViewCellLayout(context, mGlowBmp, mEdgeBmp, mGlowRightBmp, mEdgeRightBmp);
            setupPage(layout);
            addView(layout);
        }
    }

    @Override
    public void syncPageItems(int page, boolean immediate) {
        if (page < mNumAppsPages) {
            syncAppsPageItems(page, immediate);
        } else if (page < mNumAppsPages + mNumDisneyPages) {
            syncDisneyPageItems(page, immediate);
        } else {
            syncWidgetPageItems(page, immediate);
        }
    }

    // We want our pages to be z-ordered such that the further a page is to the left, the higher
    // it is in the z-order. This is important to insure touch events are handled correctly.
    @Override
    public View getPageAt(int index) {
        return getChildAt(indexToPage(index));
    }

    @Override
    public int indexToPage(int index) {
        return getChildCount() - index - 1;
    }

    // In apps customize, we have a scrolling effect which emulates pulling cards off of a stack.
    @Override
    protected void screenScrolled(int screenCenter) {
        super.screenScrolled(screenCenter);

        for (int i = 0; i < getChildCount(); i++) {
            View v = getPageAt(i);
            if (v != null) {
                float scrollProgress = getScrollProgress(screenCenter, v, i);

                float translationX;
                float minScrollProgress = Math.min(0, scrollProgress);

                if (scrollProgress < 0) {
                    translationX = 0.0f;
                } else {
                    translationX = minScrollProgress * v.getMeasuredWidth();
                }

                float alpha;
                if (scrollProgress < 0) {
                    alpha = mAlphaInterpolator.getInterpolation(1 - Math.abs(scrollProgress));
                } else {
                    //  On large screens we need to fade the page as it nears its leftmost position
                    alpha = mLeftScreenAlphaInterpolator.getInterpolation(1 - scrollProgress);
                }

                v.setCameraDistance(mDensity * CAMERA_DISTANCE);
//                int pageWidth = v.getMeasuredWidth();
//                int pageHeight = v.getMeasuredHeight();

                if (PERFORM_OVERSCROLL_ROTATION) {
//                    float xPivot = TRANSITION_PIVOT;
                    boolean isOverscrollingFirstPage = scrollProgress < 0;
                    boolean isOverscrollingLastPage = scrollProgress > 0;

                    if (i == 0 && isOverscrollingFirstPage) {

                        if (v instanceof PagedViewCellLayout) {
                            ((PagedViewCellLayout) v).setOverScrollAmount(Math.abs(scrollProgress), true);
                        } else if (v instanceof PagedViewGridLayout) {
                            ((PagedViewGridLayout) v).setOverScrollAmount(Math.abs(scrollProgress), true);
                        }

//                        // Overscroll to the left
//                        v.setPivotX(xPivot * pageWidth);
//                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
//                        alpha = 1.0f;
                        // On the first page, we don't want the page to have any lateral motion
                        translationX = 0;
                    } else if (i == getChildCount() - 1 && isOverscrollingLastPage) {

                        if (v instanceof PagedViewCellLayout) {
                            ((PagedViewCellLayout) v).setOverScrollAmount(Math.abs(scrollProgress), false);
                        } else if (v instanceof PagedViewGridLayout) {
                            ((PagedViewGridLayout) v).setOverScrollAmount(Math.abs(scrollProgress), false);
                        }

//                        // Overscroll to the right
//                        v.setPivotX((1 - xPivot) * pageWidth);
//                        v.setRotationY(-TRANSITION_MAX_ROTATION * scrollProgress);
//                        alpha = 1.0f;
                        // On the last page, we don't want the page to have any lateral motion.
                        translationX = 0;
                    } else {
//                        v.setPivotY(pageHeight / 2.0f);
//                        v.setPivotX(pageWidth / 2.0f);
//                        v.setRotationY(0f);
                        if (v instanceof PagedViewCellLayout) {
                            ((PagedViewCellLayout) v).setOverScrollAmount(0, false);
                        } else if (v instanceof PagedViewGridLayout) {
                            ((PagedViewGridLayout) v).setOverScrollAmount(0, false);
                        }
                    }
                }

                v.setTranslationX(translationX);

                // If the view has 0 alpha, we set it to be invisible so as to prevent
                // it from accepting touches
                if (alpha == 0) {
                    v.setVisibility(INVISIBLE);
                } else if (v.getVisibility() != VISIBLE) {
                    v.setVisibility(VISIBLE);
                }
            }
        }

        enableHwLayersOnVisiblePages();
    }

    private void enableHwLayersOnVisiblePages() {
        final int screenCount = getChildCount();

        getVisiblePages(mTempVisiblePagesRange);
        int leftScreen = mTempVisiblePagesRange[0];
        int rightScreen = mTempVisiblePagesRange[1];
        int forceDrawScreen = -1;
        if (leftScreen == rightScreen) {
            // make sure we're caching at least two pages always
            if (rightScreen < screenCount - 1) {
                rightScreen++;
                forceDrawScreen = rightScreen;
            } else if (leftScreen > 0) {
                leftScreen--;
                forceDrawScreen = leftScreen;
            }
        } else {
            forceDrawScreen = leftScreen + 1;
        }

        for (int i = 0; i < screenCount; i++) {
            final View layout = (View) getPageAt(i);
            if (!(leftScreen <= i && i <= rightScreen &&
                    (i == forceDrawScreen || shouldDrawChild(layout)))) {
                layout.setLayerType(LAYER_TYPE_NONE, null);
            }
        }

        for (int i = 0; i < screenCount; i++) {
            final View layout = (View) getPageAt(i);

            if (leftScreen <= i && i <= rightScreen &&
                    (i == forceDrawScreen || shouldDrawChild(layout))) {
                if (layout.getLayerType() != LAYER_TYPE_HARDWARE) {
                    layout.setLayerType(LAYER_TYPE_HARDWARE, null);
                }
            }
        }
    }

    protected void overScroll(float amount) {
        acceleratedOverScroll(amount);
    }

    /**
     * Used by the parent to get the content width to set the tab bar to
     * @return
     */
    public int getPageContentWidth() {
        return mContentWidth;
    }

    @Override
    protected void onPageEndMoving() {
        super.onPageEndMoving();
        mForceDrawAllChildrenNextFrame = true;
        // We reset the save index when we change pages so that it will be recalculated on next
        // rotation
        mSaveInstanceStateItemIndex = -1;
    }

    /*
     * AllAppsView implementation
     */
    public void setup(Launcher launcher, DragController dragController) {
        mLauncher = launcher;
        mDragController = dragController;
    }

    /**
     * We should call thise method whenever the core data changes (mApps, mWidgets) so that we can
     * appropriately determine when to invalidate the PagedView page data.  In cases where the data
     * has yet to be set, we can requestLayout() and wait for onDataReady() to be called in the
     * next onMeasure() pass, which will trigger an invalidatePageData() itself.
     */
    private void invalidateOnDataChange() {
        if (!isDataReady()) {
            // The next layout pass will trigger data-ready if both widgets and apps are set, so
            // request a layout to trigger the page data when ready.
            requestLayout();
        } else {
            cancelAllTasks();
            invalidatePageData();
        }
    }

    public void setApps(ArrayList<ApplicationInfo> list) {

        mApps = list;
        mDisneyApps = new ArrayList<ApplicationInfo>();
        for (ApplicationInfo info : mApps) {
            if (isDisneyApp(info)) {
                 mDisneyApps.add(info);
            }
        }
        Collections.sort(mApps, LauncherModel.getAppNameComparator());

        updateApplicationInfoIcons();
        updatePageCountsAndInvalidateData();
    }

    private boolean isDisneyApp(ApplicationInfo info) {
        return info.componentName.getPackageName().startsWith("jp.co.disney.apps");
    }

    private void addAppsWithoutInvalidate(ArrayList<ApplicationInfo> list) {
        // We add it in place, in alphabetical order
        int count = list.size();
        for (int i = 0; i < count; ++i) {
            ApplicationInfo info = list.get(i);
            int index = Collections.binarySearch(mApps, info, LauncherModel.getAppNameComparator());
            if (index < 0) {
                mApps.add(-(index + 1), info);
            }

            if (isDisneyApp(info)) {
                index = Collections.binarySearch(mDisneyApps, info, LauncherModel.getAppNameComparator());
                if (index < 0) {
                    mDisneyApps.add(-(index + 1), info);
                }
            }
        }
    }
    public void addApps(ArrayList<ApplicationInfo> list) {
        addAppsWithoutInvalidate(list);
        updateApplicationInfoIcons();
        updatePageCountsAndInvalidateData();
    }
    private int findAppByComponent(List<ApplicationInfo> list, ApplicationInfo item) {
        ComponentName removeComponent = item.intent.getComponent();
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            ApplicationInfo info = list.get(i);
            if (info.intent.getComponent().equals(removeComponent)) {
                return i;
            }
        }
        return -1;
    }
    private void removeAppsWithoutInvalidate(ArrayList<ApplicationInfo> list) {
        // loop through all the apps and remove apps that have the same component
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            ApplicationInfo info = list.get(i);
            int removeIndex = findAppByComponent(mApps, info);
            if (removeIndex > -1) {
                mApps.remove(removeIndex);
            }
            removeIndex = findAppByComponent(mDisneyApps, info);
            if (removeIndex > -1) {
                mDisneyApps.remove(removeIndex);
            }
        }
    }
    public void removeApps(ArrayList<ApplicationInfo> appInfos) {
        removeAppsWithoutInvalidate(appInfos);
        updatePageCountsAndInvalidateData();
    }
    public void updateApps(ArrayList<ApplicationInfo> list) {
        // We remove and re-add the updated applications list because it's properties may have
        // changed (ie. the title), and this will ensure that the items will be in their proper
        // place in the list.
        removeAppsWithoutInvalidate(list);
        addAppsWithoutInvalidate(list);
        updatePageCountsAndInvalidateData();
    }

    public void reset() {
        // If we have reset, then we should not continue to restore the previous state
        mSaveInstanceStateItemIndex = -1;

        AppsCustomizeTabHost tabHost = getTabHost();
        String tag = tabHost.getCurrentTabTag();
        if (tag != null) {
            if (!tag.equals(tabHost.getTabTagForContentType(ContentType.Applications))) {
                tabHost.setCurrentTabFromContent(ContentType.Applications);
            }
            updateFilterState(0, -1, -1);
        }
    }

    public void resetFilter() {
        mFilterCategory = 0;
        mFilterAlphabet = -1;
        mFilterKana = -1;
    }

    private AppsCustomizeTabHost getTabHost() {
        return (AppsCustomizeTabHost) mLauncher.findViewById(R.id.apps_customize_pane);
    }

    public void dumpState() {
        // TODO: Dump information related to current list of Applications, Widgets, etc.
        ApplicationInfo.dumpApplicationInfoList(TAG, "mApps", mApps);
        dumpAppWidgetProviderInfoList(TAG, "mWidgets", mWidgets);
    }

    private void dumpAppWidgetProviderInfoList(String tag, String label,
            ArrayList<Object> list) {
        DebugLog.instance.outputLog(tag, label + " size=" + list.size());
        for (Object i: list) {
            if (i instanceof AppWidgetProviderInfo) {
                AppWidgetProviderInfo info = (AppWidgetProviderInfo) i;
                DebugLog.instance.outputLog(tag, "   label=\"" + info.label + "\" previewImage=" + info.previewImage
                        + " resizeMode=" + info.resizeMode + " configure=" + info.configure
                        + " initialLayout=" + info.initialLayout
                        + " minWidth=" + info.minWidth + " minHeight=" + info.minHeight);
            } else if (i instanceof ResolveInfo) {
                ResolveInfo info = (ResolveInfo) i;
                DebugLog.instance.outputLog(tag, "   label=\"" + info.loadLabel(mPackageManager) + "\" icon="
                        + info.icon);
            }
        }
    }

    public void surrender() {
        // TODO: If we are in the middle of any process (ie. for holographic outlines, etc) we
        // should stop this now.

        // Stop all background tasks
        cancelAllTasks();
    }

    @Override
    public void iconPressed(PagedViewIcon icon) {
        // Reset the previously pressed icon and store a reference to the pressed icon so that
        // we can reset it on return to Launcher (in Launcher.onResume())
        if (mPressedIcon != null) {
            mPressedIcon.resetDrawableState();
        }
        mPressedIcon = icon;
    }

    public void resetDrawableState() {
        if (mPressedIcon != null) {
            mPressedIcon.resetDrawableState();
            mPressedIcon = null;
        }
    }

    /*
     * We load an extra page on each side to prevent flashes from scrolling and loading of the
     * widget previews in the background with the AsyncTasks.
     */
    final static int sLookBehindPageCount = 2;
    final static int sLookAheadPageCount = 2;
    protected int getAssociatedLowerPageBound(int page) {
        final int count = getChildCount();
        int windowSize = Math.min(count, sLookBehindPageCount + sLookAheadPageCount + 1);
        int windowMinIndex = Math.max(Math.min(page - sLookBehindPageCount, count - windowSize), 0);
        return windowMinIndex;
    }
    protected int getAssociatedUpperPageBound(int page) {
        final int count = getChildCount();
        int windowSize = Math.min(count, sLookBehindPageCount + sLookAheadPageCount + 1);
        int windowMaxIndex = Math.min(Math.max(page + sLookAheadPageCount, windowSize - 1),
                count - 1);
        return windowMaxIndex;
    }

    @Override
    protected String getCurrentPageDescription() {
        int page = (mNextPage != INVALID_PAGE) ? mNextPage : mCurrentPage;
        int stringId = R.string.default_scroll_format;
        int count = 0;

        if (page < mNumAppsPages) {
            stringId = R.string.apps_customize_apps_scroll_format;
            count = mNumAppsPages;

        } else if (page < mNumAppsPages + mNumDisneyPages) {
            page -= mNumAppsPages;
            stringId = R.string.apps_customize_apps_scroll_format;
            count = mNumDisneyPages;
        } else {
            page -= mNumAppsPages + mNumDisneyPages;
            stringId = R.string.apps_customize_widgets_scroll_format;
            count = mNumWidgetPages;
        }

        return String.format(getContext().getString(stringId), page + 1, count);
    }

    void updateApplicationInfoIcons() {
        final int size = mApps.size();
        for (int i = 0; i < size; i++) {
            ApplicationInfo info = mApps.get(i);
            ResolveInfo resolveInfo = mPackageManager.resolveActivity(info.intent, 0);
            mIconCache.getTitleAndIcon(info, resolveInfo, null);
        }
    }

    void updatePageIcons() {
        // TODO: ページが初期化されているかどうかの仮判定
        if (mNumAppsPages != Integer.MAX_VALUE) {
            for (int i = 0; i < mNumAppsPages; i++) {
                syncAppsPageItems(i, false);
            }
        }

        if (mNumAppsPages != Integer.MAX_VALUE && mNumDisneyPages != Integer.MAX_VALUE) {
            for (int i = mNumAppsPages; i < mNumAppsPages + mNumDisneyPages; i++) {
                syncDisneyPageItems(i, false);
            }
        }

        if (mNumAppsPages != Integer.MAX_VALUE && mNumDisneyPages != Integer.MAX_VALUE && mNumWidgetPages != Integer.MAX_VALUE) {
            for (int i = mNumAppsPages + mNumDisneyPages; i < mNumAppsPages + mNumDisneyPages + mNumWidgetPages; i++) {
                syncWidgetPageItems(i, false);
            }
        }
    }
}
