package jp.co.disney.apps.managed.kisekaeapp.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Process;
import android.os.RemoteException;
import android.util.JsonReader;
import android.util.Log;
import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsFileName;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsTypeValue;

public class KisekaeLauncherModel extends LauncherModel {

    static int sWorkspaceNumPages;
    static int sWorkspaceDefaultPage;
    static int sThemeMode;
    static String sThemeId;
    static boolean sInAppTheme;
    static boolean sPageLoop;
    static final String[] sWpPaths = new String[Workspace.MAX_SCREEN_COUNT];
    static String sIconThemeId;
    static boolean sIconInAppTheme;

    private volatile boolean mInitialLoading = false;
    private ArrayList<AbstractLoaderTask> mPendingLoaderTask = new ArrayList<AbstractLoaderTask>();

    public interface Callbacks extends LauncherModel.Callbacks {
        public void setThemeResources(String themeId, boolean isThemeMode, boolean inAppTheme, String[] wpPaths, PanelWallpapers panelWps,
                Bitmap drawerBg, int pageCount, int defaultPage, Bitmap drawerIconBmp, String iconThemeId,
                boolean iconInAppTheme, Boolean pageLoop, String drawerTextColor, boolean applied, ArrayList<DummyInfo> widgetInfos,
                ArrayList<ThemeShortcutDef> themeShortcutDefs);
//        public void setKisekaeIconResources(String iconThemeId, boolean iconInAppTheme, Bitmap drawerIconBmp);
        public void setKisekaeIconResources(String iconThemeId, boolean iconInAppTheme);
        public void setKisekaeIcons();
        public void initThemeShortcutIcon(int defaultPage);
        public void setThemeWallpapers(String[] wpPaths, PanelWallpapers panelWps);
        public void setThemeWidgets(ArrayList<DummyInfo> widgetInfos);
        public void createThemeShortcuts(ArrayList<ThemeShortcutDef> themeShortcutDefs);
        public void applyThemeWallpaper();
        public void updateBadgeInfo();
    }

    KisekaeLauncherModel(LauncherApplication app, IconCache iconCache) {
        super(app, iconCache);
    }

    @Override
    public void startLoader(boolean isLaunching, int synchronousBindPage, String themeId, boolean inAppTheme) {

        synchronized (mLock) {
            // Clear any deferred bind-runnables from the synchronized load process
            // We must do this before any loading/binding is scheduled below.
            mDeferredBindRunnables.clear();

            // Don't bother to start the thread if we know it's not going to do anything
            if (mCallbacks != null && mCallbacks.get() != null) {
                // If there is already one running, tell it to stop.
                // also, don't downgrade isLaunching if we're already running
                isLaunching = isLaunching || stopLoaderLocked();
                mLoaderTask = new KisekaeLoaderTask(mApp, isLaunching);

                if (themeId != null) {
                    ((KisekaeLoaderTask) mLoaderTask).setAppliedThemeId(themeId, inAppTheme);
                }

                if (synchronousBindPage > -1 && mAllAppsLoaded && mWorkspaceLoaded) {
                    ((LoaderTask) mLoaderTask).runBindSynchronousPage(synchronousBindPage);
                } else {

                    if (mInitialLoading) {
                        mPendingLoaderTask.add(mLoaderTask);
                    } else {
                        sWorkerThread.setPriority(Thread.NORM_PRIORITY);
                        sWorker.post(mLoaderTask);
                    }
                }
            }
        }
    }

    void loadAndSetTheme(final Context context, final String themeId, final boolean isThemeMode, final boolean inAppTheme,
            final String[] wpPaths, final int pageCount, final int defaultPage, final String iconThemeId,
            final boolean iconInAppTheme, final Boolean pageLoop, final boolean applied, final ArrayList<DummyInfo> widgetInfos,
            final ArrayList<ThemeShortcutDef> themeShortcutDefs) {

        Runnable r = new Runnable() {
            public void run() {

                final String rootDir = ThemeUtils.getThemeRootDirectory(context, themeId, inAppTheme, ContentsTypeValue.CONTENTS_TYPE_THEME.getValue());
                if (rootDir == null || rootDir.equals("")) {
                    showToast(context, R.string.err_msg_load_theme);
                    return;
                }

                // リソース読み込み
                final PanelWallpapers panelWps = new PanelWallpapers(wpPaths);
                if (isThemeMode) {
                    panelWps.load(context);
                }

                final Bitmap drawerBg = ThemeUtils.loadThemeBackground(context, rootDir + ContentsFileName.drawerBack.getFileName());

                String jsonTextColor = null;
                final JSONObject infoJson = ThemeUtils.loadThemeInfoJson(context, rootDir, inAppTheme);
                if (infoJson != null) {
                    try {
                        jsonTextColor = infoJson.getString(ThemeUtils.JSON_KEY_TEXT_COLOR);
                    } catch (JSONException e) {
                    }
                }
                final String drawerTextColor = jsonTextColor;

                final Bitmap drawerIconBmp;
//                if (iconThemeId == null) {
                    drawerIconBmp = ThemeUtils.loadKisekaeIcon(context, rootDir + ContentsFileName.drawerIon.getFileName());
//                } else {
//                    final String iconRootDir = ThemeUtils.getThemeRootDirectory(context, iconThemeId, iconInAppTheme,
//                            ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue());
//                    if (iconRootDir == null || iconRootDir.equals("")) {
//                        showToast(context, R.string.err_msg_load_theme);
//                        return;
//                    }
//                    drawerIconBmp = ThemeUtils.loadKisekaeIcon(context, iconRootDir + ContentsFileName.drawerIon.getFileName());
//                }

                mIconCache.updateCache();

                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        // Viewにリソースを反映
                        Callbacks callback = (Callbacks) mCallbacks.get();
                        if (callback != null) {
                            callback.setThemeResources(themeId, isThemeMode, inAppTheme, wpPaths, panelWps,
                                    drawerBg, pageCount, defaultPage, drawerIconBmp, iconThemeId,
                                    iconInAppTheme, pageLoop, drawerTextColor, applied, widgetInfos, themeShortcutDefs);
                        }
                    }
                });
            }
        };
        runOnWorkerThread(r);
    }

    void applyTheme(final Context context, final String themeId, final boolean inAppTheme,
            final int currentPageCount, final int currentDefaultPage) {

        Runnable r = new Runnable() {
            public void run() {

                // DBの状態を更新
                String[][] retWpPaths = new String[1][];
                int[] retNewPageCount = new int[1];
                int[] retNewDefaultPage = new int[1];
                ArrayList<DummyInfo> retWidgetInfos = new ArrayList<DummyInfo>();
                ArrayList<ThemeShortcutDef> retThemeShortcutDefs = new ArrayList<ThemeShortcutDef>();
                if (!updateThemeState(context, themeId, inAppTheme, currentPageCount, currentDefaultPage,
                        retWpPaths, retNewPageCount, retNewDefaultPage, retWidgetInfos, retThemeShortcutDefs, false)) {
                    showToast(context, R.string.err_msg_load_theme);
                    return;
                }

                updateKisekaeIconState(context, themeId, inAppTheme, ContentsTypeValue.CONTENTS_TYPE_THEME.getValue());

                // リソース読み込み、表示に反映
                loadAndSetTheme(context, themeId, true, inAppTheme, retWpPaths[0], retNewPageCount[0], retNewDefaultPage[0],
                        null, true, null, true, retWidgetInfos, retThemeShortcutDefs);
            }
        };

        runOnWorkerThread(r);
    }

    void enableThemeMode(final Context context, final String themeId, final boolean inAppTheme, final int currentPageCount) {

        Runnable r = new Runnable() {
            public void run() {

                final String[] wpPaths = getWallpaperPathsFromDatabase(context);

                final String rootDir = ThemeUtils.getThemeRootDirectory(context, themeId, inAppTheme,
                        ContentsTypeValue.CONTENTS_TYPE_THEME.getValue());
                if (rootDir == null || rootDir.equals("")) {
                    showToast(context, R.string.err_msg_load_theme);
                    return;
                }

                final JSONObject infoJson = ThemeUtils.loadThemeInfoJson(context, rootDir, inAppTheme);
                if (infoJson == null) {
                    showToast(context, R.string.err_msg_load_theme);
                    return;
                }

                int numPage = 0;
                int[] pageWps = null;
                try {
                    numPage = infoJson.getInt(ThemeUtils.JSON_KEY_NUM_PAGE);

                    JSONArray pageWpsJsonArray = infoJson.getJSONArray(ThemeUtils.JSON_KEY_PAGE_WPS);
                    if (pageWpsJsonArray != null) {
                        int count = pageWpsJsonArray.length();
                        pageWps = new int[count];
                        for (int i = 0; i < count; i++) {
                            pageWps[i] =  pageWpsJsonArray.getInt(i);
                        }
                    }
                } catch (JSONException e) {
                }
                if (numPage <= 0 || pageWps == null || pageWps.length < numPage) {
                    showToast(context, R.string.err_msg_load_theme);
                    return;
                }
                numPage = Math.min(numPage, ThemeUtils.MAX_NUM_THEME_BG);

                for (int i = 0; i < numPage; i++) {
                    if (wpPaths[i] != null) continue;
                    wpPaths[i] = ThemeUtils.getThemeWallpaperPath(context, rootDir, pageWps[i], inAppTheme);
                }

                if (numPage < currentPageCount) {
                    for (int i = numPage; i < currentPageCount; i++) {
                        if (wpPaths[i] != null) continue;
                        int i2 = i % numPage;
                        wpPaths[i] = ThemeUtils.getThemeWallpaperPath(context, rootDir, pageWps[i2], inAppTheme);
                    }
                }

                for (int i = currentPageCount; i < Workspace.MAX_SCREEN_COUNT; i++) {
                    wpPaths[i] = null;
                }

                enableThemeInDatabase(context, wpPaths);

                synchronized (sBgLock) {
                    sThemeMode = 1;
                    final int count = sWpPaths.length;
                    for (int i = 0; i < count; i++) {
                        sWpPaths[i] = wpPaths[i];
                    }
                }

                final PanelWallpapers panelWps = new PanelWallpapers(wpPaths);
                panelWps.load(context);

                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        // Viewにリソースを反映
                        Callbacks callback = (Callbacks) mCallbacks.get();
                        if (callback != null) {
                            callback.setThemeWallpapers(wpPaths, panelWps);
                        }
                    }
                });
            }
        };

        runOnWorkerThread(r);
    }

    void applyThemeWallpaper(final Context context, final String wpPath,
            final PanelWallpapers panelWps, final int currentPageCount) {

        Runnable r = new Runnable() {
            public void run() {

                String[] newWpPaths = new String[Workspace.MAX_SCREEN_COUNT];
                for (int i = 0; i < currentPageCount; i++) {
                    newWpPaths[i] = wpPath;
                }

                // DBの状態更新
                updateThemeWallpaperInDatabase(context, newWpPaths);

                synchronized (sBgLock) {
                    sThemeMode = 1;
                    final int count = sWpPaths.length;
                    for (int i = 0; i < count; i++) {
                        sWpPaths[i] = newWpPaths[i];
                    }
                }

                panelWps.update(context, newWpPaths, true);

                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        // Viewにリソースを反映
                        Callbacks callback = (Callbacks) mCallbacks.get();
                        if (callback != null) {
                            callback.applyThemeWallpaper();
                        }
                    }
                });
            }
        };
        runOnWorkerThread(r);
    }

    void updateBadgeInfo(final Context context, final String packageName, final String className, final int badgeCount) {

        final BadgeInfo badgeInfo = new BadgeInfo();
        badgeInfo.setAppName(packageName + "/" + className);
        badgeInfo.setBadgeCount(badgeCount);
        badgeInfo.setEnable(1);

        HashMap<String, BadgeInfo> badgeInfoMap = mIconCache.getBadgeInfoMap();
        if (badgeInfoMap == null) return;

        badgeInfoMap.put(badgeInfo.getAppName(), badgeInfo);

        Runnable r = new Runnable() {
            @Override
            public void run() {

                final Uri uri = LauncherSettings.Badge_Info.CONTENT_URI;
                final ContentResolver cr = context.getContentResolver();

                ContentValues values = new ContentValues();
                values.put(LauncherSettings.Badge_Info.APP_NAME, badgeInfo.getAppName());
                values.put(LauncherSettings.Badge_Info.BADGE_COUNT, badgeInfo.getBadgeCount());
                values.put(LauncherSettings.Badge_Info.ENABLE, badgeInfo.getEnable());

                if (cr.update(uri, values,
                        LauncherSettings.Badge_Info.APP_NAME + "='" + badgeInfo.getAppName() + "'", null) == 0) {
                    cr.insert(uri, values);
                }

                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Callbacks callback = (Callbacks) mCallbacks.get();
                        if (callback != null) {
                            callback.updateBadgeInfo();
                        }
                    }
                });
            }
        };
        runOnWorkerThread(r);
    }

    void loadAndSetKisekaeIcons(final Context context, final String themeId, final boolean inAppTheme) {

        Runnable r = new Runnable() {
            public void run() {

//                final String rootDir = ThemeUtils.getThemeRootDirectory(context, themeId, inAppTheme,
//                        ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue());
//                if (rootDir == null || rootDir.equals("")) {
//                    showToast(context, R.string.err_msg_load_theme);
//                    return;
//                }
//
//                // リソース読み込み
//                final Bitmap drawerIconBmp = ThemeUtils.loadKisekaeIcon(context, rootDir + ContentsFileName.drawerIon.getFileName());

                mIconCache.updateCache();

                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        // Viewにリソースを反映
                        Callbacks callback = (Callbacks) mCallbacks.get();
                        if (callback != null) {
//                        	callback.setKisekaeIconResources(themeId, inAppTheme, drawerIconBmp);
                            callback.setKisekaeIconResources(themeId, inAppTheme);
                        }
                    }
                });
            }
        };
        runOnWorkerThread(r);
    }

    void applyKisekaeIcons(final Context context, final String themeId, final boolean inAppTheme) {

        Runnable r = new Runnable() {
            public void run() {

                updateKisekaeIconState(context, themeId, inAppTheme,
                        ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue());
                updateThemeIconStateInDatabase(context, themeId, inAppTheme);

                synchronized (sBgLock) {
                    sIconThemeId = themeId;
                    sIconInAppTheme = inAppTheme;
                }

                // リソース読み込み、表示に反映
                loadAndSetKisekaeIcons(context, themeId, inAppTheme);
            }
        };

        runOnWorkerThread(r);
    }

    boolean updateThemeState(Context context, String themeId, boolean inAppTheme,
            int currentPageCount, int currentDefaultPage,
            String[][] retWpPaths, int[] retNewPageCount, int[] retNewDefaultPage,
            ArrayList<DummyInfo> retWidgetInfos, ArrayList<ThemeShortcutDef> retThemeShortcutDefs,
            boolean first) {

        final String rootDir = ThemeUtils.getThemeRootDirectory(context, themeId, inAppTheme,
                ContentsTypeValue.CONTENTS_TYPE_THEME.getValue());
        if (rootDir == null || rootDir.equals("")) {
            return false;
        }

        final JSONObject infoJson = ThemeUtils.loadThemeInfoJson(context, rootDir, inAppTheme);
        if (infoJson == null) {
            return false;
        }

        int defPageCount = 0;
        int defDefaultPage = 0;
        int[] defPageWps = null;
        int[] defPagePriorityForAdding = null;
        ArrayList<DummyInfo> defWidgetInfos = new ArrayList<DummyInfo>();
        ArrayList<ThemeShortcutDef> defThemeShortcutDefs = new ArrayList<ThemeShortcutDef>();

        try {
            defPageCount = infoJson.getInt(ThemeUtils.JSON_KEY_NUM_PAGE);
            defDefaultPage = infoJson.getInt(ThemeUtils.JSON_KEY_DEFAULT_PAGE);

            JSONArray pageWpsJsonArray = infoJson.getJSONArray(ThemeUtils.JSON_KEY_PAGE_WPS);
            if (pageWpsJsonArray != null) {
                int count = pageWpsJsonArray.length();
                defPageWps = new int[count];
                for (int i = 0; i < count; i++) {
                    defPageWps[i] =  pageWpsJsonArray.getInt(i);
                }
            }

            try {
                JSONArray pagePriorityJsonArray = infoJson.getJSONArray(ThemeUtils.JSON_KEY_PAGE_PRIORITY_FOR_ADDING);
                if (pagePriorityJsonArray != null) {
                    int count = pagePriorityJsonArray.length();
                    defPagePriorityForAdding = new int[count];
                    for (int i = 0; i < count; i++) {
                        defPagePriorityForAdding[i] =  pagePriorityJsonArray.getInt(i);
                    }
                }
            } catch (JSONException e) {
                // なくても良い
                defPagePriorityForAdding = null;
            }

            int widgetPosCount = 0;
            JSONArray widgetPosJsonArray = null;
            try {
                widgetPosJsonArray = infoJson.getJSONArray(ThemeUtils.JSON_KEY_WIDGET_POS);
                widgetPosCount = widgetPosJsonArray.length();
            } catch (JSONException e) {
                // なくても良い
            }

            int inAppWidgetPosCount = 0;
            JSONArray inAppWidgetPosJsonArray = null;
            try {
                inAppWidgetPosJsonArray = infoJson.getJSONArray(ThemeUtils.JSON_KEY_WIDGET_POS_INAPP);
                inAppWidgetPosCount = inAppWidgetPosJsonArray.length();
            } catch (JSONException e) {
                // なくても良い
            }

            int widgetInfoCount = widgetPosCount + inAppWidgetPosCount;
            if (widgetInfoCount > 0) {

                for (int i = 0; i < widgetPosCount; i++) {

                    try {
                        JSONObject jsonObj = widgetPosJsonArray.getJSONObject(i);

                        DummyInfo widgetInfo = new DummyInfo();
                        widgetInfo.inAppWidget = false;
                        widgetInfo.widgetPackageName = jsonObj.getString(ThemeUtils.JSON_KEY_PACKAGE_NAME);
                        widgetInfo.screen = jsonObj.getInt(ThemeUtils.JSON_KEY_PAGE_NO);
                        widgetInfo.cellX = (int) (jsonObj.getDouble(ThemeUtils.JSON_KEY_CELL_X) * Workspace.ICON_SPAN);
                        widgetInfo.cellY = (int) (jsonObj.getDouble(ThemeUtils.JSON_KEY_CELL_Y) * Workspace.ICON_SPAN);
                        widgetInfo.spanX = (int) (jsonObj.getDouble(ThemeUtils.JSON_KEY_SPAN_X) * Workspace.ICON_SPAN);
                        widgetInfo.spanY = (int) (jsonObj.getDouble(ThemeUtils.JSON_KEY_SPAN_Y) * Workspace.ICON_SPAN);
                        widgetInfo.minSpanX = widgetInfo.spanX;
                        widgetInfo.minSpanY = widgetInfo.spanY;

                        defWidgetInfos.add(widgetInfo);

                    } catch (JSONException e) {
                        // 正常でないものはとばす
                        Log.e(TAG, e.getMessage(), e);
                    }
                }

                for (int i = 0; i < inAppWidgetPosCount; i++) {

                    try {
                        JSONObject jsonObj = inAppWidgetPosJsonArray.getJSONObject(i);

                        DummyInfo widgetInfo = new DummyInfo();
                        widgetInfo.inAppWidget = true;
                        widgetInfo.inAppWidgetClassName = jsonObj.getString(ThemeUtils.JSON_KEY_CLASS_NAME);
                        widgetInfo.screen = jsonObj.getInt(ThemeUtils.JSON_KEY_PAGE_NO);
                        widgetInfo.cellX = (int) (jsonObj.getDouble(ThemeUtils.JSON_KEY_CELL_X) * Workspace.ICON_SPAN);
                        widgetInfo.cellY = (int) (jsonObj.getDouble(ThemeUtils.JSON_KEY_CELL_Y) * Workspace.ICON_SPAN);
                        widgetInfo.spanX = (int) (jsonObj.getDouble(ThemeUtils.JSON_KEY_SPAN_X) * Workspace.ICON_SPAN);
                        widgetInfo.spanY = (int) (jsonObj.getDouble(ThemeUtils.JSON_KEY_SPAN_Y) * Workspace.ICON_SPAN);
                        widgetInfo.minSpanX = widgetInfo.spanX;
                        widgetInfo.minSpanY = widgetInfo.spanY;

                        defWidgetInfos.add(widgetInfo);

                    } catch (JSONException e) {
                        // 正常でないものはとばす
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            }

            int shortcutPosCount = 0;
            JSONArray shortcutPosJsonArray = null;
            try {
                shortcutPosJsonArray = infoJson.getJSONArray(ThemeUtils.JSON_KEY_SHORTCUT_POS);
                shortcutPosCount = shortcutPosJsonArray.length();
            } catch (JSONException e) {
                // なくても良い
            }

            if (shortcutPosCount > 0) {

                for (int i = 0; i < shortcutPosCount; i++) {

                    try {
                        JSONObject jsonObj = shortcutPosJsonArray.getJSONObject(i);

                        ThemeShortcutDef themeShortcutDef = new ThemeShortcutDef();
                        themeShortcutDef.iconName = jsonObj.optString(ThemeUtils.JSON_KEY_ICON_NAME, null);
                        themeShortcutDef.packageName = jsonObj.optString(ThemeUtils.JSON_KEY_PACKAGE_NAME, null);
                        themeShortcutDef.className = jsonObj.optString(ThemeUtils.JSON_KEY_CLASS_NAME, null);
                        themeShortcutDef.screen = jsonObj.getInt(ThemeUtils.JSON_KEY_PAGE_NO);
                        themeShortcutDef.cellX = (int) (jsonObj.getDouble(ThemeUtils.JSON_KEY_CELL_X) * Workspace.ICON_SPAN);
                        themeShortcutDef.cellY = (int) (jsonObj.getDouble(ThemeUtils.JSON_KEY_CELL_Y) * Workspace.ICON_SPAN);

                        defThemeShortcutDefs.add(themeShortcutDef);

                    } catch (JSONException e) {
                        // 正常でないものはとばす
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        defPageCount = Math.min(defPageCount, Workspace.MAX_SCREEN_COUNT);

        if (defPageCount <= 0 || defPageWps == null || defPageWps.length < defPageCount) {
            return false;
        }

        String[] newWpPaths = new String[Workspace.MAX_SCREEN_COUNT];
        ArrayList<DummyInfo> newWidgetInfos = new ArrayList<DummyInfo>();
        ArrayList<ThemeShortcutDef> newThemeShortcutDefs = new ArrayList<ThemeShortcutDef>();

        int newPageCount = Math.max(currentPageCount, defPageCount);
        int newDefaultPage;

        if (first) {

            for (int i = 0; i < newPageCount; i++) {
                int i2 = i % defPageCount;
                newWpPaths[i] = ThemeUtils.getThemeWallpaperPath(context, rootDir, defPageWps[i2], inAppTheme);
            }

            newDefaultPage = defDefaultPage;

            newWidgetInfos.addAll(defWidgetInfos);
            newThemeShortcutDefs.addAll(defThemeShortcutDefs);

        } else {

            // 現在のパネルの壁紙を変更

            int minPageCount = Math.min(currentPageCount, defPageCount);
            for (int i = 0; i < minPageCount; i++) {
                newWpPaths[i] = ThemeUtils.getThemeWallpaperPath(context, rootDir, defPageWps[i], inAppTheme);
            }

            if (currentPageCount < defPageCount) {
                // 現在のパネル数がテーマのパネル数よりも少ない場合
                for (int i = currentPageCount; i < defPageCount; i++) {

                    // テーマパネル数に達するまでテーマパネルを追加

                    newWpPaths[i] = ThemeUtils.getThemeWallpaperPath(context, rootDir, defPageWps[i], inAppTheme);

                    // 追加ページにウィジェットとショートカットの定義があれば適用する
                    for (DummyInfo defWidgetInfo : defWidgetInfos) {
                        if (defWidgetInfo.screen == i) {
                            newWidgetInfos.add(defWidgetInfo);
                        }
                    }
                    for (ThemeShortcutDef shortcutDef : defThemeShortcutDefs) {
                        if (shortcutDef.screen == i) {
                            newThemeShortcutDefs.add(shortcutDef);
                        }
                    }
                }
            } else {
                // あふれたパネルの壁紙を変更
                for (int i = defPageCount; i < currentPageCount; i++) {

                    int i2 = i % defPageCount;
                    newWpPaths[i] = ThemeUtils.getThemeWallpaperPath(context, rootDir, defPageWps[i2], inAppTheme);
                }
            }

            newDefaultPage = currentDefaultPage;

            // テーマの追加用パネルを追加
            if (defPagePriorityForAdding != null) {
                for (int i = 0; i < defPagePriorityForAdding.length; i++) {

                    if (newPageCount >= Workspace.MAX_SCREEN_COUNT) break;

                    int pageNo = defPagePriorityForAdding[i];

                    if (currentPageCount < defPageCount) {

                        // 追加済み
                        if (pageNo > currentPageCount - 1) continue;

                        // 同一壁紙追加済みチェック
                        boolean added = false;
                        for (int j = currentPageCount; j < defPageCount; j++) {
                            if (defPageWps[j] == defPageWps[pageNo]) {
                                // 同一壁紙追加済み

                                // ページ空判定
                                boolean isEmpty = true;
                                for (DummyInfo newWidgetInfo : newWidgetInfos) {
                                    if (newWidgetInfo.screen == j) {
                                        isEmpty = false;
                                        break;
                                    }
                                }
                                if (!isEmpty) continue;

                                for (ThemeShortcutDef newShortcutDef : newThemeShortcutDefs) {
                                    if (newShortcutDef.screen == j) {
                                        isEmpty = false;
                                        break;
                                    }
                                }
                                if (!isEmpty) continue;

                                // 追加済みページにウィジェットとショートカットを貼る
                                for (DummyInfo defWidgetInfo : defWidgetInfos) {
                                    if (defWidgetInfo.screen == pageNo) {
                                        DummyInfo defWidgetInfoCloned = new DummyInfo(defWidgetInfo);
                                        defWidgetInfoCloned.screen = j;
                                        newWidgetInfos.add(defWidgetInfoCloned);
                                    }
                                }
                                for (ThemeShortcutDef shortcutDef : defThemeShortcutDefs) {
                                    if (shortcutDef.screen == pageNo) {
                                        ThemeShortcutDef shortcutDefCloned = new ThemeShortcutDef(shortcutDef);
                                        shortcutDefCloned.screen = j;
                                        newThemeShortcutDefs.add(shortcutDefCloned);
                                    }
                                }
                                added = true;
                                break;
                            }
                        }

                        if (added) continue;
                    }

                    // 最後に追加

                    newWpPaths[newPageCount] = ThemeUtils.getThemeWallpaperPath(context, rootDir,
                            defPageWps[pageNo], inAppTheme);

                    // 追加パネルのウィジェットとショートカットの定義を追加
                    for (DummyInfo defWidgetInfo : defWidgetInfos) {
                        if (defWidgetInfo.screen == pageNo) {
                            DummyInfo defWidgetInfoCloned = new DummyInfo(defWidgetInfo);
                            defWidgetInfoCloned.screen = newPageCount;
                            newWidgetInfos.add(defWidgetInfoCloned);
                        }
                    }
                    for (ThemeShortcutDef shortcutDef : defThemeShortcutDefs) {
                        if (shortcutDef.screen == pageNo) {
                            ThemeShortcutDef shortcutDefCloned = new ThemeShortcutDef(shortcutDef);
                            shortcutDefCloned.screen = newPageCount;
                            newThemeShortcutDefs.add(shortcutDefCloned);
                        }
                    }

                    newPageCount++;
                }
            }
        }

        // DBの状態更新
        updateThemeStateInDatabase(context, themeId, inAppTheme, newWpPaths, newPageCount, newDefaultPage);

        synchronized (sBgLock) {
            sWorkspaceNumPages = newPageCount;
            sWorkspaceDefaultPage = newDefaultPage;
            sThemeMode = 1;
            sThemeId = themeId;
            sInAppTheme = inAppTheme;
            final int count = sWpPaths.length;
            for (int i = 0; i < count; i++) {
                sWpPaths[i] = newWpPaths[i];
            }
            sIconThemeId = null;
            sIconInAppTheme = true;
        }

        if (retWpPaths != null) {
            retWpPaths[0] = newWpPaths;
        }
        if (retNewPageCount != null) {
            retNewPageCount[0] = newPageCount;
        }
        if (retNewDefaultPage != null) {
            retNewDefaultPage[0] = newDefaultPage;
        }
        if (retWidgetInfos != null) {
            retWidgetInfos.addAll(newWidgetInfos);
        }
        if (retThemeShortcutDefs != null) {
            retThemeShortcutDefs.addAll(newThemeShortcutDefs);
        }

        return true;
    }

    void updateKisekaeIconState(Context context, String themeId, boolean inAppTheme, int contentsType) {
        mBgAllAppsList.updateKisekaeInfo(themeId, inAppTheme, contentsType);
        updateKisekaeMapInDatabase(context, mIconCache.getCurrentKisekaeMap());
    }

    static void updateKisekaeMapInDatabase(Context context, HashMap<String, KisekaeInfo> kisekaeMap) {

        final ContentValues[] values = new ContentValues[kisekaeMap.size()];

        int i = 0;
        Iterator<Entry<String, KisekaeInfo>> itr = kisekaeMap.entrySet().iterator();
        while (itr.hasNext()) {

            Entry<String, KisekaeInfo> pair = itr.next();

            KisekaeInfo kisekaeInfo = pair.getValue();

            ContentValues row = new ContentValues();
            row.put(LauncherSettings.Kisekae_Map._ID, i);
            row.put(LauncherSettings.Kisekae_Map.APP_NAME, kisekaeInfo.getAppName());
            row.put(LauncherSettings.Kisekae_Map.THEME_ID, kisekaeInfo.getThemeId());
            row.put(LauncherSettings.Kisekae_Map.IN_APP_THEME, kisekaeInfo.isInAppTheme() ? 1 : 0);
            row.put(LauncherSettings.Kisekae_Map.CONTENTS_TYPE, kisekaeInfo.getContentsType());
            row.put(LauncherSettings.Kisekae_Map.IC_NAME, kisekaeInfo.getIconName());

            values[i] = row;

            ++i;
        }

        updateKisekaeMap(context, values);
    }

    static void updateKisekaeMap(final Context context, final ContentValues[] values) {

        final Uri uri = LauncherSettings.Kisekae_Map.CONTENT_URI;
        final ContentResolver cr = context.getContentResolver();

        Runnable r = new Runnable() {
            public void run() {
                cr.delete(uri, null, null);
                cr.bulkInsert(uri, values);

                String spKey = LauncherApplication.getSharedPreferencesKey();
                SharedPreferences sp = context.getSharedPreferences(spKey, Context.MODE_PRIVATE);
                Editor editor = sp.edit();
                editor.putBoolean(SP_KEY_KISEKAE_INIT_FLG, true);
                editor.commit();
            }
        };
        runOnWorkerThread(r);
    }

    static void updateScreenNos(Context context, final ArrayList<int[]> screeNoMap) {

        if (screeNoMap.size() == 0) return;

        final Uri uri = LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION;
        final ContentResolver cr = context.getContentResolver();

        Runnable r = new Runnable() {
            public void run() {

                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

                final int count = screeNoMap.size();

                // 予めスクリーンNoに1000加算しておく(更新前と更新後でかぶらないように)
                for (int i = 0; i < count; i++) {
                    int[] map = screeNoMap.get(i);
                    ops.add(ContentProviderOperation.newUpdate(uri)
                            .withSelection(LauncherSettings.Favorites.SCREEN + "=? AND "
                    + LauncherSettings.Favorites.CONTAINER + "=" + LauncherSettings.Favorites.CONTAINER_DESKTOP,
                    new String[] { String.valueOf(map[0]) })
                            .withValue(LauncherSettings.Favorites.SCREEN, String.valueOf(map[0] + 1000)).build());
                }

                for (int i = 0; i < count; i++) {
                    int[] map = screeNoMap.get(i);
                    ops.add(ContentProviderOperation.newUpdate(uri)
                            .withSelection(LauncherSettings.Favorites.SCREEN + "=? AND "
                    + LauncherSettings.Favorites.CONTAINER + "=" + LauncherSettings.Favorites.CONTAINER_DESKTOP,
                    new String[] { String.valueOf(map[0] + 1000) })
                            .withValue(LauncherSettings.Favorites.SCREEN, String.valueOf(map[1])).build());
                }

                try {
                    cr.applyBatch(uri.getAuthority(), ops);

                    synchronized (sBgLock) {

                        for (int j = 0; j < count; j++) {

                            int[] map = screeNoMap.get(j);

                            for (int i = 0; i < sBgWorkspaceItems.size(); i++) {
                                ItemInfo info = sBgWorkspaceItems.get(i);
                                if (info == null || info.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) continue;
                                if (info.screen == map[0]) {
                                    info.screen += 1000;
                                }
                            }
                            for (int i = 0; i < sBgAppWidgets.size(); i++) {
                                ItemInfo info = sBgAppWidgets.get(i);
                                if (info == null || info.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) continue;
                                if (info.screen == map[0]) {
                                    info.screen += 1000;
                                }
                            }
                            for (int i = 0; i < sBgFolders.size(); i++) {
                                ItemInfo info = sBgFolders.get(i);
                                if (info == null || info.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) continue;
                                if (info.screen == map[0]) {
                                    info.screen += 1000;
                                }
                            }
                            for (int i = 0; i < sBgDummyItems.size(); i++) {
                                ItemInfo info = sBgDummyItems.get(i);
                                if (info == null || info.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) continue;
                                if (info.screen == map[0]) {
                                    info.screen += 1000;
                                }
                            }
                        }

                        for (int j = 0; j < count; j++) {

                            int[] map = screeNoMap.get(j);

                            for (int i = 0; i < sBgWorkspaceItems.size(); i++) {
                                ItemInfo info = sBgWorkspaceItems.get(i);
                                if (info == null || info.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) continue;
                                if (info.screen == map[0] + 1000) {
                                    info.screen = map[1];
                                }
                            }
                            for (int i = 0; i < sBgAppWidgets.size(); i++) {
                                ItemInfo info = sBgAppWidgets.get(i);
                                if (info == null || info.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) continue;
                                if (info.screen == map[0] + 1000) {
                                    info.screen = map[1];
                                }
                            }
                            for (int i = 0; i < sBgFolders.size(); i++) {
                                ItemInfo info = sBgFolders.get(i);
                                if (info == null || info.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) continue;
                                if (info.screen == map[0] + 1000) {
                                    info.screen = map[1];
                                }
                            }
                            for (int i = 0; i < sBgDummyItems.size(); i++) {
                                ItemInfo info = sBgDummyItems.get(i);
                                if (info == null || info.container != LauncherSettings.Favorites.CONTAINER_DESKTOP) continue;
                                if (info.screen == map[0] + 1000) {
                                    info.screen = map[1];
                                }
                            }
                        }
                    }

                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage(), e);
                } catch (OperationApplicationException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        };
        runOnWorkerThread(r);
    }

    void updatePageInfo(final Context context, final int defaultPage, final int pageCount, final String[] wpPaths) {

        Runnable r = new Runnable() {
            public void run() {
                updatePageInfoInDatabase(context, defaultPage, pageCount, wpPaths);

                synchronized (sBgLock) {
                    sWorkspaceNumPages = pageCount;
                    sWorkspaceDefaultPage = defaultPage;
                    final int count = sWpPaths.length;
                    for (int i = 0; i < count; i++) {
                        sWpPaths[i] = wpPaths[i];
                    }
                }
            }
        };

        runOnWorkerThread(r);
    }

    void updatePageInfo(final Context context, final int defaultPage, final int pageCount) {

        Runnable r = new Runnable() {
            public void run() {
                updatePageInfoInDatabase(context, defaultPage, pageCount);

                synchronized (sBgLock) {
                    sWorkspaceNumPages = pageCount;
                    sWorkspaceDefaultPage = defaultPage;
                }
            }
        };

        runOnWorkerThread(r);
    }

    void updatePageLoopSetting(final Context context, final boolean pageLoop) {

        Runnable r = new Runnable() {
            public void run() {
                updatePageLoopSettingInDatabase(context, pageLoop);

                synchronized (sBgLock) {
                    sPageLoop = pageLoop;
                }
            }
        };

        runOnWorkerThread(r);
    }

    static void updateKisekaeMapThemeInDatabase(final Context context, final String themeId, final boolean inAppTheme) {

        final Uri uri = LauncherSettings.Kisekae_Map.CONTENT_URI;
        final ContentResolver cr = context.getContentResolver();

        Runnable r = new Runnable() {
            public void run() {

                ContentValues values = new ContentValues();
                values.put(LauncherSettings.Kisekae_Map.THEME_ID, themeId);
                values.put(LauncherSettings.Kisekae_Map.IN_APP_THEME, inAppTheme ? 1 : 0);

                cr.update(uri, values, null, null);
            }
        };
        runOnWorkerThread(r);
    }

    static void updateFavorites_Head(Context context, final ContentValues values) {

        final Uri uri = LauncherSettings.Favorites_Head.CONTENT_URI;
        final ContentResolver cr = context.getContentResolver();

        Runnable r = new Runnable() {
            public void run() {
                cr.update(uri, values, null, null);
            }
        };
        runOnWorkerThread(r);
    }

    static void updateThemeWallpaperInDatabase(Context context, String[] wpPaths) {

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites_Head._ID, 0);
        values.put(LauncherSettings.Favorites_Head.THEME_MODE, 1);
        values.put(LauncherSettings.Favorites_Head.WP_1, wpPaths[0]);
        values.put(LauncherSettings.Favorites_Head.WP_2, wpPaths[1]);
        values.put(LauncherSettings.Favorites_Head.WP_3, wpPaths[2]);
        values.put(LauncherSettings.Favorites_Head.WP_4, wpPaths[3]);
        values.put(LauncherSettings.Favorites_Head.WP_5, wpPaths[4]);
        values.put(LauncherSettings.Favorites_Head.WP_6, wpPaths[5]);
        values.put(LauncherSettings.Favorites_Head.WP_7, wpPaths[6]);
        values.put(LauncherSettings.Favorites_Head.WP_8, wpPaths[7]);
        values.put(LauncherSettings.Favorites_Head.WP_9, wpPaths[8]);

        updateFavorites_Head(context, values);
    }

    static void updateThemeStateInDatabase(Context context, String themeId, boolean inAppTheme,
            String[] wpPaths, int pageCount, int defaultPage) {

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites_Head._ID, 0);
        values.put(LauncherSettings.Favorites_Head.NUM_PAGES, pageCount);
        values.put(LauncherSettings.Favorites_Head.DEFAULT_PAGE, defaultPage);
        values.put(LauncherSettings.Favorites_Head.THEME_MODE, 1);
        values.put(LauncherSettings.Favorites_Head.THEME_ID, themeId);
        values.put(LauncherSettings.Favorites_Head.IN_APP_THEME, inAppTheme ? 1 : 0);
        values.put(LauncherSettings.Favorites_Head.WP_1, wpPaths[0]);
        values.put(LauncherSettings.Favorites_Head.WP_2, wpPaths[1]);
        values.put(LauncherSettings.Favorites_Head.WP_3, wpPaths[2]);
        values.put(LauncherSettings.Favorites_Head.WP_4, wpPaths[3]);
        values.put(LauncherSettings.Favorites_Head.WP_5, wpPaths[4]);
        values.put(LauncherSettings.Favorites_Head.WP_6, wpPaths[5]);
        values.put(LauncherSettings.Favorites_Head.WP_7, wpPaths[6]);
        values.put(LauncherSettings.Favorites_Head.WP_8, wpPaths[7]);
        values.put(LauncherSettings.Favorites_Head.WP_9, wpPaths[8]);
        // 個別着せ替え初期化
        values.putNull(LauncherSettings.Favorites_Head.ICON_THEME_ID);
        values.put(LauncherSettings.Favorites_Head.ICON_IN_APP_THEME, 1);

        updateFavorites_Head(context, values);
    }

    static void updatePageInfoInDatabase(Context context, int defaultPage, int pageCount, String[] wpPaths) {

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites_Head._ID, 0);
        values.put(LauncherSettings.Favorites_Head.NUM_PAGES, pageCount);
        values.put(LauncherSettings.Favorites_Head.DEFAULT_PAGE, defaultPage);
        values.put(LauncherSettings.Favorites_Head.WP_1, wpPaths[0]);
        values.put(LauncherSettings.Favorites_Head.WP_2, wpPaths[1]);
        values.put(LauncherSettings.Favorites_Head.WP_3, wpPaths[2]);
        values.put(LauncherSettings.Favorites_Head.WP_4, wpPaths[3]);
        values.put(LauncherSettings.Favorites_Head.WP_5, wpPaths[4]);
        values.put(LauncherSettings.Favorites_Head.WP_6, wpPaths[5]);
        values.put(LauncherSettings.Favorites_Head.WP_7, wpPaths[6]);
        values.put(LauncherSettings.Favorites_Head.WP_8, wpPaths[7]);
        values.put(LauncherSettings.Favorites_Head.WP_9, wpPaths[8]);

        updateFavorites_Head(context, values);
    }

    static void updatePageInfoInDatabase(Context context, int defaultPage, int pageCount) {

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites_Head._ID, 0);
        values.put(LauncherSettings.Favorites_Head.NUM_PAGES, pageCount);
        values.put(LauncherSettings.Favorites_Head.DEFAULT_PAGE, defaultPage);

        updateFavorites_Head(context, values);
    }

    static void updateThemeIconStateInDatabase(Context context, String iconThemeId, boolean iconInAppTheme) {

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites_Head._ID, 0);
        values.put(LauncherSettings.Favorites_Head.ICON_THEME_ID, iconThemeId);
        values.put(LauncherSettings.Favorites_Head.ICON_IN_APP_THEME, iconInAppTheme ? 1 : 0);

        updateFavorites_Head(context, values);
    }

    static void updatePageLoopSettingInDatabase(Context context, boolean pageLoop) {

        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites_Head._ID, 0);
        values.put(LauncherSettings.Favorites_Head.PAGE_LOOP, pageLoop ? 1 : 0);

        updateFavorites_Head(context, values);
    }

    static void disableThemeInDatabase(Context context) {
        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites_Head._ID, 0);
        values.put(LauncherSettings.Favorites_Head.THEME_MODE, 0);
        updateFavorites_Head(context, values);
    }

    static void enableThemeInDatabase(Context context, String[] wpPaths) {
        final ContentValues values = new ContentValues();
        values.put(LauncherSettings.Favorites_Head._ID, 0);
        values.put(LauncherSettings.Favorites_Head.THEME_MODE, 1);
        values.put(LauncherSettings.Favorites_Head.WP_1, wpPaths[0]);
        values.put(LauncherSettings.Favorites_Head.WP_2, wpPaths[1]);
        values.put(LauncherSettings.Favorites_Head.WP_3, wpPaths[2]);
        values.put(LauncherSettings.Favorites_Head.WP_4, wpPaths[3]);
        values.put(LauncherSettings.Favorites_Head.WP_5, wpPaths[4]);
        values.put(LauncherSettings.Favorites_Head.WP_6, wpPaths[5]);
        values.put(LauncherSettings.Favorites_Head.WP_7, wpPaths[6]);
        values.put(LauncherSettings.Favorites_Head.WP_8, wpPaths[7]);
        values.put(LauncherSettings.Favorites_Head.WP_9, wpPaths[8]);
        updateFavorites_Head(context, values);
    }

    static void refreshCurrentKisekaeMap(Context context, IconCache iconCache) {

        String[] themeId_ret = new String[1];
        boolean[] inAppTheme_ret = new boolean[1];
        loadCurrentThemeIdForIcon(context, themeId_ret, inAppTheme_ret);
        if(themeId_ret[0] == null) return;

        HashMap<String, String> defKisekaeMap = new HashMap<String, String>();
        loadKisekaeIconMap(context, defKisekaeMap);

        ArrayList<ApplicationInfoBase> allAppsList = loadAllApps(context, iconCache);
        if (allAppsList == null) return;

        HashMap<String, KisekaeInfo> kisekaeMap = createKisekaeMap(allAppsList, defKisekaeMap,
                themeId_ret[0], inAppTheme_ret[0], ContentsTypeValue.CONTENTS_TYPE_THEME.getValue());
        updateKisekaeMapInDatabase(context, kisekaeMap);
    }

    static ArrayList<ApplicationInfoBase> loadAllApps(Context context, IconCache iconCache) {

        ArrayList<ApplicationInfoBase> allAppsList = new ArrayList<ApplicationInfoBase>();

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final PackageManager packageManager = context.getPackageManager();

        List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        if (apps == null) {
            return null;
        }
        int appCount = apps.size();
        if (appCount == 0) {
            return null;
        }

        for (int i = 0; i < appCount; i++) {
            allAppsList.add(new ApplicationInfoBase(packageManager, apps.get(i), null));
        }

        return allAppsList;
    }

    static void loadCurrentThemeIdForIcon(Context context, String[] themeId_ret, boolean[] inAppTheme_ret) {

        String themeId = null;
        boolean inAppTheme = false;

        final ContentResolver contentResolver = context.getContentResolver();
        final Cursor c_head = contentResolver.query(
                LauncherSettings.Favorites_Head.CONTENT_URI, null, null, null, null);
        try {
            final int themeIdIndex = c_head.getColumnIndexOrThrow(
                    LauncherSettings.Favorites_Head.THEME_ID);
            final int inAppThemeIndex = c_head.getColumnIndexOrThrow(
                    LauncherSettings.Favorites_Head.IN_APP_THEME);
            final int iconThemeIdIndex = c_head.getColumnIndex(
                    LauncherSettings.Favorites_Head.ICON_THEME_ID);
            final int iconInAppThemeIndex = c_head.getColumnIndex(
                    LauncherSettings.Favorites_Head.ICON_IN_APP_THEME);

            while (c_head.moveToNext()) {
                themeId = c_head.getString(iconThemeIdIndex);
                if (themeId == null || themeId.equals("")) {
                    themeId = c_head.getString(themeIdIndex);
                    inAppTheme = (c_head.getInt(inAppThemeIndex) != 0);
                } else {
                    inAppTheme = (c_head.getInt(iconInAppThemeIndex) != 0);
                }
            }
        } finally {
            c_head.close();
        }

        themeId_ret[0] = themeId;
        inAppTheme_ret[0] = inAppTheme;
    }

    static void loadKisekaeIconMap(Context context, HashMap<String, String> kisekaeMap) {

        InputStream in = null;
        JsonReader reader = null;
        try {
            in = context.getAssets().open("ic_map.json");
            reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
            reader.beginObject();
            while (reader.hasNext()) {
                String pkgName = reader.nextName();
                String icResName = reader.nextString();
                kisekaeMap.put(pkgName, icResName);
            }

        } catch (IOException e) {

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    static HashMap<String, KisekaeInfo> createKisekaeMap(ArrayList<ApplicationInfoBase> data,
            HashMap<String, String> defKisekaeMap, String themeId, boolean inAppTheme, int contentsType) {

        HashMap<String, KisekaeInfo> kisekaeMap = new HashMap<String, KisekaeInfo>();
        HashMap<String, KisekaeInfo> iconMap = new HashMap<String, KisekaeInfo>();

        for (ApplicationInfoBase info : data) {

            String appName = info.componentName.getPackageName() + "/" + info.componentName.getClassName();
            String kisekaeIconName = defKisekaeMap.get(appName);

            if (kisekaeIconName != null) {

                KisekaeInfo chofuku = iconMap.get(kisekaeIconName);
                if (chofuku != null) {

                    DebugLog.instance.outputLog(TAG, "着せ替え重複[1]: " + chofuku.getAppTitle() + "[" + chofuku.getAppName() + "], " + kisekaeIconName);
                    DebugLog.instance.outputLog(TAG, "着せ替え重複[2]: " + info.title.toString() + "[" + appName + "], " + kisekaeIconName);

                    boolean replace = false;

                    // 優先度チェック
                    if (kisekaeIconName.equals("ic_gallery")) {

                        final String[] titles = { "ギャラリー", "アルバム", "フォト" };
                        final int count = titles.length;
                        String appTitle = info.title.toString();

                        int newItem;
                        for (newItem = 0; newItem < count; newItem++) {
                            if (appTitle.equals(titles[newItem])) {
                                break;
                            }
                        }

                        int oldItem;
                        for (oldItem = 0; oldItem < count; oldItem++) {
                            if (chofuku.getAppTitle().equals(titles[oldItem])) {
                                break;
                            }
                        }

                        if (newItem < oldItem) {
                            replace = true;
                        }


                    } else if (kisekaeIconName.equals("ic_contacts")) {

                        final String[] titles = { "連絡先", "電話帳" };
                        final int count = titles.length;
                        String appTitle = info.title.toString();

                        int newItem;
                        for (newItem = 0; newItem < count; newItem++) {
                            if (appTitle.equals(titles[newItem])) {
                                break;
                            }
                        }

                        int oldItem;
                        for (oldItem = 0; oldItem < count; oldItem++) {
                            if (chofuku.getAppTitle().equals(titles[oldItem])) {
                                break;
                            }
                        }

                        if (newItem < oldItem) {
                            replace = true;
                        }

                    } else if (kisekaeIconName.equals("ic_music")) {

                        final String[] titles = { "ミュージック", "ウォークマン", "メディアプレイヤー" };
                        final int count = titles.length;
                        String appTitle = info.title.toString();

                        int newItem;
                        for (newItem = 0; newItem < count; newItem++) {
                            if (appTitle.equals(titles[newItem])) {
                                break;
                            }
                        }

                        int oldItem;
                        for (oldItem = 0; oldItem < count; oldItem++) {
                            if (chofuku.getAppTitle().equals(titles[oldItem])) {
                                break;
                            }
                        }

                        if (newItem < oldItem) {
                            replace = true;
                        }

                    } else if (kisekaeIconName.equals("ic_email")) {

                        final String[] titles = { "PCメール", "Eメール" };
                        final int count = titles.length;
                        String appTitle = info.title.toString();

                        int newItem;
                        for (newItem = 0; newItem < count; newItem++) {
                            if (appTitle.equals(titles[newItem])) {
                                break;
                            }
                        }

                        int oldItem;
                        for (oldItem = 0; oldItem < count; oldItem++) {
                            if (chofuku.getAppTitle().equals(titles[oldItem])) {
                                break;
                            }
                        }

                        if (newItem < oldItem) {
                            replace = true;
                        }
                    }

                    if (replace) {

                        kisekaeMap.remove(chofuku.getAppName());

                        KisekaeInfo kisekaeInfo = new KisekaeInfo();
                        kisekaeInfo.setAppName(appName);
                        kisekaeInfo.setThemeId(themeId);
                        kisekaeInfo.setInAppTheme(inAppTheme);
                        kisekaeInfo.setContentsType(contentsType);
                        kisekaeInfo.setIconName(kisekaeIconName);
                        kisekaeInfo.setAppTitle(info.title.toString());
                        kisekaeMap.put(appName, kisekaeInfo);

                        iconMap.put(kisekaeIconName, kisekaeInfo);
                    }

                } else {

                    KisekaeInfo kisekaeInfo = new KisekaeInfo();
                    kisekaeInfo.setAppName(appName);
                    kisekaeInfo.setThemeId(themeId);
                    kisekaeInfo.setInAppTheme(inAppTheme);
                    kisekaeInfo.setContentsType(contentsType);
                    kisekaeInfo.setIconName(kisekaeIconName);
                    kisekaeInfo.setAppTitle(info.title.toString());
                    kisekaeMap.put(appName, kisekaeInfo);

                    iconMap.put(kisekaeIconName, kisekaeInfo);
                }
            }
        }

        return kisekaeMap;
    }

    protected class KisekaeLoaderTask extends LauncherModel.LoaderTask {

        protected String mAppliedThemeId;
        protected boolean mInAppTheme;
        protected ArrayList<DummyInfo> mAppliedWidgetInfos;
        protected ArrayList<ThemeShortcutDef> mAppliedShortcutDefs;

        KisekaeLoaderTask(Context context, boolean isLaunching) {
            super(context, isLaunching);
        }

        public void setAppliedThemeId(String themeId, boolean inAppTheme) {
            mAppliedThemeId = themeId;
            mInAppTheme = inAppTheme;
        }

        @Override
        public void runMain() {

            keep_running: {
                // Elevate priority when Home launches for the first time to avoid
                // starving at boot time. Staring at a blank home is not cool.
                synchronized (mLock) {
                    Process.setThreadPriority(mIsLaunching
                            ? Process.THREAD_PRIORITY_DEFAULT : Process.THREAD_PRIORITY_BACKGROUND);
                }

                // First step. Load workspace first, this is necessary since adding of apps from
                // managed profile in all apps is deferred until onResume. See http://b/17336902.
                loadAndBindWorkspace();

                if (mStopped) {
                    break keep_running;
                }

                // Whew! Hard work done.  Slow us down, and wait until the UI thread has
                // settled down.
                synchronized (mLock) {
                    if (mIsLaunching) {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    }
                }
                waitForIdle();

                // Second step. Load all apps.
                loadAndBindAllApps();

                String spKey = LauncherApplication.getSharedPreferencesKey();
                SharedPreferences sp = mContext.getSharedPreferences(spKey, Context.MODE_PRIVATE);

                if(!sp.getBoolean(SP_KEY_KISEKAE_INIT_FLG, false)) {

                    final IconCache iconCache = getIconCache();

                    String themeId;
                    boolean inAppTheme;
                    final int workspaceDefaultPage;
                    synchronized (sBgLock) {
                        themeId = sThemeId;
                        inAppTheme = sInAppTheme;
                        workspaceDefaultPage = sWorkspaceDefaultPage;
                    }
                    mBgAllAppsList.updateKisekaeInfo(themeId, inAppTheme,
                            ContentsTypeValue.CONTENTS_TYPE_THEME.getValue());
                    updateKisekaeMapInDatabase(mContext, iconCache.getCurrentKisekaeMap());

                    iconCache.updateCache();

                    mInitialLoading = true;

                    runOnMainThread(new Runnable() {
                        @Override
                        public void run() {

                            // Viewにリソースを反映
                            Callbacks callback = (mCallbacks != null) ? (Callbacks) mCallbacks.get() : null;
                            if (callback == null) return;

                            callback.setKisekaeIcons();
                            callback.initThemeShortcutIcon(workspaceDefaultPage);
                            if (mAppliedThemeId != null) {
                                callback.createThemeShortcuts(mAppliedShortcutDefs);
                                callback.setThemeWidgets(mAppliedWidgetInfos);
                            }

                            synchronized (mLock) {
                                if (mPendingLoaderTask.size() > 0) {
                                    sWorkerThread.setPriority(Thread.NORM_PRIORITY);
                                    for (AbstractLoaderTask task : mPendingLoaderTask) {
                                        sWorker.post(task);
                                    }
                                    mPendingLoaderTask.clear();
                                }
                                mInitialLoading = false;
                            }
                        }
                    });
                }

                // Restore the default thread priority after we are done loading items
                synchronized (mLock) {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
                }
            }

            // Update the saved icons if necessary
            synchronized (sBgLock) {
                for (Object key : sBgDbIconCache.keySet()) {
                    updateSavedIcon(mContext, (ShortcutInfo) key, sBgDbIconCache.get(key));
                }
                sBgDbIconCache.clear();
            }

            // Clear out this reference, otherwise we end up holding it until all of the
            // callback runnables are done.
            mContext = null;
        }

        @Override
        protected void loadAndBindWorkspace() {
            mIsLoadingAndBindingWorkspace = true;

            if (mAppliedThemeId != null) {

                int[] retPageCount = new int[1];
                int[] retDefaultPage = new int[1];
                getScreenInfoFromDatabase(mContext, retPageCount, retDefaultPage);

                mAppliedWidgetInfos = new ArrayList<DummyInfo>();
                mAppliedShortcutDefs = new ArrayList<ThemeShortcutDef>();

                boolean first = false;

                String spKey = LauncherApplication.getSharedPreferencesKey();
                SharedPreferences sp = mContext.getSharedPreferences(spKey, Context.MODE_PRIVATE);
                if(!sp.getBoolean(SP_KEY_KISEKAE_INIT_FLG, false)) {
                    // 一度も起動されていない場合
                    first = true;
                }

                updateThemeState(mContext, mAppliedThemeId, mInAppTheme,
                        retPageCount[0], retDefaultPage[0], null, null, null,
                        mAppliedWidgetInfos, mAppliedShortcutDefs, first);

                updateKisekaeMapThemeInDatabase(mContext, mAppliedThemeId, mInAppTheme);
                HashMap<String, KisekaeInfo> currentKisekaeMap = getIconCache().getCurrentKisekaeMap();
                if (currentKisekaeMap != null) {
                    Iterator<Entry<String, KisekaeInfo>> itr = currentKisekaeMap.entrySet().iterator();
                    while (itr.hasNext()) {
                        Entry<String, KisekaeInfo> pair = itr.next();
                        KisekaeInfo info = pair.getValue();
                        info.setThemeId(mAppliedThemeId);
                        info.setInAppTheme(mInAppTheme);
                    }
                }
            }

            // Load the workspace
            if (!mWorkspaceLoaded) {
                loadWorkspace();
                synchronized (KisekaeLoaderTask.this) {
                    if (mStopped) {
                        return;
                    }
                    mWorkspaceLoaded = true;
                }
            }

            // Bind the workspace
            bindWorkspace(-1);
        }

        @Override
        protected void loadWorkspace() {

            final Context context = mContext;
            final ContentResolver contentResolver = context.getContentResolver();
            final PackageManager manager = context.getPackageManager();
            final AppWidgetManager widgets = AppWidgetManager.getInstance(context);
            final boolean isSafeMode = manager.isSafeMode();

            synchronized (sBgLock) {

                sWorkspaceNumPages = 1;
                sWorkspaceDefaultPage = 0;
                sThemeMode = 0;
                sThemeId = null;
                sInAppTheme = true;
                sPageLoop = false;
                final int count = sWpPaths.length;
                for (int i = 0; i < count; i++) {
                    sWpPaths[i] = null;
                }
                sIconThemeId = null;
                sIconInAppTheme = true;

                final Cursor c_head = contentResolver.query(
                        LauncherSettings.Favorites_Head.CONTENT_URI, null, null, null, null);
                try {
                    final int numPagesIndex = c_head.getColumnIndexOrThrow(
                            LauncherSettings.Favorites_Head.NUM_PAGES);
                    final int defaultPageIndex = c_head.getColumnIndexOrThrow(
                            LauncherSettings.Favorites_Head.DEFAULT_PAGE);
                    final int themeModeIndex = c_head.getColumnIndexOrThrow(
                            LauncherSettings.Favorites_Head.THEME_MODE);
                    final int themeIdIndex = c_head.getColumnIndexOrThrow(
                            LauncherSettings.Favorites_Head.THEME_ID);
                    final int inAppThemeIndex = c_head.getColumnIndexOrThrow(
                            LauncherSettings.Favorites_Head.IN_APP_THEME);
                    final int pageLoopIndex = c_head.getColumnIndexOrThrow(
                            LauncherSettings.Favorites_Head.PAGE_LOOP);
                    final int wp1Index = c_head.getColumnIndex(
                            LauncherSettings.Favorites_Head.WP_1);
                    final int wp2Index = c_head.getColumnIndex(
                            LauncherSettings.Favorites_Head.WP_2);
                    final int wp3Index = c_head.getColumnIndex(
                            LauncherSettings.Favorites_Head.WP_3);
                    final int wp4Index = c_head.getColumnIndex(
                            LauncherSettings.Favorites_Head.WP_4);
                    final int wp5Index = c_head.getColumnIndex(
                            LauncherSettings.Favorites_Head.WP_5);
                    final int wp6Index = c_head.getColumnIndex(
                            LauncherSettings.Favorites_Head.WP_6);
                    final int wp7Index = c_head.getColumnIndex(
                            LauncherSettings.Favorites_Head.WP_7);
                    final int wp8Index = c_head.getColumnIndex(
                            LauncherSettings.Favorites_Head.WP_8);
                    final int wp9Index = c_head.getColumnIndex(
                            LauncherSettings.Favorites_Head.WP_9);
                    final int iconThemeIdIndex = c_head.getColumnIndex(
                            LauncherSettings.Favorites_Head.ICON_THEME_ID);
                    final int iconInAppThemeIndex = c_head.getColumnIndex(
                            LauncherSettings.Favorites_Head.ICON_IN_APP_THEME);

                    while (!mStopped && c_head.moveToNext()) {
                        sWorkspaceNumPages = c_head.getInt(numPagesIndex);
                        sWorkspaceDefaultPage = c_head.getInt(defaultPageIndex);
                        sThemeMode = c_head.getInt(themeModeIndex);
                        sThemeId = c_head.getString(themeIdIndex);
                        sInAppTheme = (c_head.getInt(inAppThemeIndex) != 0);
                        sPageLoop = (c_head.getInt(pageLoopIndex) != 0);
                        sWpPaths[0] = c_head.getString(wp1Index);
                        sWpPaths[1] = c_head.getString(wp2Index);
                        sWpPaths[2] = c_head.getString(wp3Index);
                        sWpPaths[3] = c_head.getString(wp4Index);
                        sWpPaths[4] = c_head.getString(wp5Index);
                        sWpPaths[5] = c_head.getString(wp6Index);
                        sWpPaths[6] = c_head.getString(wp7Index);
                        sWpPaths[7] = c_head.getString(wp8Index);
                        sWpPaths[8] = c_head.getString(wp9Index);
                        sIconThemeId = c_head.getString(iconThemeIdIndex);
                        sIconInAppTheme = (c_head.getInt(iconInAppThemeIndex) != 0);
                    }
                } finally {
                    c_head.close();
                }

                HashMap<String, KisekaeInfo> kisekaeMap = new HashMap<String, KisekaeInfo>();

                final Cursor c_kisekae = contentResolver.query(
                        LauncherSettings.Kisekae_Map.CONTENT_URI, null, null, null, null);
                try {
                    final int appNameIndex = c_kisekae.getColumnIndexOrThrow(
                            LauncherSettings.Kisekae_Map.APP_NAME);
                    final int themeIdIndex = c_kisekae.getColumnIndexOrThrow(
                            LauncherSettings.Kisekae_Map.THEME_ID);
                    final int inAppThemeIndex = c_kisekae.getColumnIndexOrThrow(
                            LauncherSettings.Kisekae_Map.IN_APP_THEME);
                    final int contentsTypeIndex = c_kisekae.getColumnIndexOrThrow(
                            LauncherSettings.Kisekae_Map.CONTENTS_TYPE);
                    final int icNameIndex = c_kisekae.getColumnIndexOrThrow(
                            LauncherSettings.Kisekae_Map.IC_NAME);

                    while (!mStopped && c_kisekae.moveToNext()) {

                        KisekaeInfo kisekaeInfo = new KisekaeInfo();
                        kisekaeInfo.setAppName(c_kisekae.getString(appNameIndex));
                        kisekaeInfo.setThemeId(c_kisekae.getString(themeIdIndex));
                        kisekaeInfo.setInAppTheme(c_kisekae.getInt(inAppThemeIndex) != 0);
                        kisekaeInfo.setContentsType(c_kisekae.getInt(contentsTypeIndex));
                        kisekaeInfo.setIconName(c_kisekae.getString(icNameIndex));

                        kisekaeMap.put(kisekaeInfo.getAppName(), kisekaeInfo);
                    }

                } finally {
                    c_kisekae.close();
                }

                getIconCache().setCurrentKisekaeMap(kisekaeMap);


                HashMap<String, BadgeInfo> badgeInfoMap = new HashMap<String, BadgeInfo>();

                final Cursor c_badge = contentResolver.query(
                        LauncherSettings.Badge_Info.CONTENT_URI, null, null, null, null);

                try {
                    final int appNameIndex = c_badge.getColumnIndexOrThrow(
                            LauncherSettings.Badge_Info.APP_NAME);
                    final int badgeCountIndex = c_badge.getColumnIndexOrThrow(
                            LauncherSettings.Badge_Info.BADGE_COUNT);
                    final int enableIndex = c_badge.getColumnIndexOrThrow(
                            LauncherSettings.Badge_Info.ENABLE);

                    while (!mStopped && c_badge.moveToNext()) {

                        BadgeInfo badgeInfo = new BadgeInfo();
                        badgeInfo.setAppName(c_badge.getString(appNameIndex));
                        badgeInfo.setBadgeCount(c_badge.getInt(badgeCountIndex));
                        badgeInfo.setEnable(c_badge.getInt(enableIndex));

                        badgeInfoMap.put(badgeInfo.getAppName(), badgeInfo);
                    }

                } finally {
                    c_badge.close();
                }

                getIconCache().setBadgeInfoMap(badgeInfoMap);

                sBgWorkspaceItems.clear();
                sBgAppWidgets.clear();
                sBgFolders.clear();
                sBgItemsIdMap.clear();
                sBgDbIconCache.clear();
                sBgDummyItems.clear();

                final ArrayList<Long> itemsToRemove = new ArrayList<Long>();

                final Cursor c = contentResolver.query(
                        LauncherSettings.Favorites.CONTENT_URI, null, null, null, null);

                // +1 for the hotseat (it can be larger than the workspace)
                // Load workspace in reverse order to ensure that latest items are loaded first (and
                // before any earlier duplicates)
                final ItemInfo occupied[][][] =
                        new ItemInfo[Workspace.MAX_SCREEN_COUNT + 1][getCellCountX() + 1][getCellCountY() + 1];

                try {
                    final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
                    final int intentIndex = c.getColumnIndexOrThrow
                            (LauncherSettings.Favorites.INTENT);
                    final int titleIndex = c.getColumnIndexOrThrow
                            (LauncherSettings.Favorites.TITLE);
                    final int iconTypeIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.ICON_TYPE);
                    final int iconIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
                    final int iconPackageIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.ICON_PACKAGE);
                    final int iconResourceIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.ICON_RESOURCE);
                    final int containerIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.CONTAINER);
                    final int itemTypeIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.ITEM_TYPE);
                    final int appWidgetIdIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.APPWIDGET_ID);
                    final int screenIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.SCREEN);
                    final int cellXIndex = c.getColumnIndexOrThrow
                            (LauncherSettings.Favorites.CELLX);
                    final int cellYIndex = c.getColumnIndexOrThrow
                            (LauncherSettings.Favorites.CELLY);
                    final int spanXIndex = c.getColumnIndexOrThrow
                            (LauncherSettings.Favorites.SPANX);
                    final int spanYIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.SPANY);
                    final int shortcutNameIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.SHORTCUT_NAME);
                    final int dummyTypeIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.DUMMY_TYPE);
                    final int dummyNameIndex = c.getColumnIndexOrThrow(
                            LauncherSettings.Favorites.DUMMY_NAME);
                    //final int uriIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.URI);
                    //final int displayModeIndex = c.getColumnIndexOrThrow(
                    //        LauncherSettings.Favorites.DISPLAY_MODE);

                    ShortcutInfo info;
                    String intentDescription;
                    LauncherAppWidgetInfo appWidgetInfo;
                    DummyInfo dummyInfo;
                    int container;
                    long id;
                    Intent intent;

                    while (!mStopped && c.moveToNext()) {
                        try {
                            int itemType = c.getInt(itemTypeIndex);

                            switch (itemType) {
                            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                                intentDescription = c.getString(intentIndex);
                                try {
                                    intent = Intent.parseUri(intentDescription, 0);
                                } catch (URISyntaxException e) {
                                    continue;
                                }

                                if (itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                                        info = getShortcutInfo(manager, intent, context, c, iconIndex,
                                            titleIndex, mLabelCache);
                                } else {
                                    info = getShortcutInfo(c, context, iconTypeIndex,
                                            iconPackageIndex, iconResourceIndex, iconIndex,
                                            titleIndex);

                                    // App shortcuts that used to be automatically added to Launcher
                                    // didn't always have the correct intent flags set, so do that
                                    // here
                                    if (intent.getAction() != null &&
                                        intent.getCategories() != null &&
                                        intent.getAction().equals(Intent.ACTION_MAIN) &&
                                        intent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) {
                                        intent.addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                    }
                                }

                                if (info != null) {
                                    info.intent = intent;
                                    info.id = c.getLong(idIndex);
                                    container = c.getInt(containerIndex);
                                    info.container = container;
                                    info.screen = c.getInt(screenIndex);
                                    info.cellX = c.getInt(cellXIndex);
                                    info.cellY = c.getInt(cellYIndex);
                                    info.shortcutName = c.getString(shortcutNameIndex);

                                    // check & update map of what's occupied
                                    if (!checkItemPlacement(occupied, info)) {
                                        break;
                                    }

                                    switch (container) {
                                    case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                    case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                                        sBgWorkspaceItems.add(info);
                                        break;
                                    default:
                                        // Item is in a user folder
                                        FolderInfo folderInfo =
                                                findOrMakeFolder(sBgFolders, container);
                                        folderInfo.add(info);
                                        break;
                                    }
                                    sBgItemsIdMap.put(info.id, info);

                                    // now that we've loaded everthing re-save it with the
                                    // icon in case it disappears somehow.
                                    queueIconToBeChecked(sBgDbIconCache, info, c, iconIndex);
                                } else {
                                    // Failed to load the shortcut, probably because the
                                    // activity manager couldn't resolve it (maybe the app
                                    // was uninstalled), or the db row was somehow screwed up.
                                    // Delete it.
                                    id = c.getLong(idIndex);
                                    Log.e(TAG, "Error loading shortcut " + id + ", removing it");
                                    contentResolver.delete(LauncherSettings.Favorites.getContentUri(
                                                id, false), null, null);
                                }
                                break;

                            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                                id = c.getLong(idIndex);
                                FolderInfo folderInfo = findOrMakeFolder(sBgFolders, id);

                                folderInfo.title = c.getString(titleIndex);
                                folderInfo.id = id;
                                container = c.getInt(containerIndex);
                                folderInfo.container = container;
                                folderInfo.screen = c.getInt(screenIndex);
                                folderInfo.cellX = c.getInt(cellXIndex);
                                folderInfo.cellY = c.getInt(cellYIndex);

                                // check & update map of what's occupied
                                if (!checkItemPlacement(occupied, folderInfo)) {
                                    break;
                                }
                                switch (container) {
                                    case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                    case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                                        sBgWorkspaceItems.add(folderInfo);
                                        break;
                                }

                                sBgItemsIdMap.put(folderInfo.id, folderInfo);
                                sBgFolders.put(folderInfo.id, folderInfo);
                                break;

                            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                                // Read all Launcher-specific widget details
                                int appWidgetId = c.getInt(appWidgetIdIndex);
                                id = c.getLong(idIndex);

                                final AppWidgetProviderInfo provider =
                                        widgets.getAppWidgetInfo(appWidgetId);

                                if (!isSafeMode && (provider == null || provider.provider == null ||
                                        provider.provider.getPackageName() == null)) {
                                    String log = "Deleting widget that isn't installed anymore: id="
                                        + id + " appWidgetId=" + appWidgetId;
                                    Log.e(TAG, log);
                                    Launcher.sDumpLogs.add(log);
                                    itemsToRemove.add(id);
                                } else {
                                    appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId,
                                            provider.provider);
                                    appWidgetInfo.id = id;
                                    appWidgetInfo.screen = c.getInt(screenIndex);
                                    appWidgetInfo.cellX = c.getInt(cellXIndex);
                                    appWidgetInfo.cellY = c.getInt(cellYIndex);
                                    appWidgetInfo.spanX = c.getInt(spanXIndex);
                                    appWidgetInfo.spanY = c.getInt(spanYIndex);
                                    int[] minSpan = Launcher.getMinSpanForWidget(context, provider);
                                    appWidgetInfo.minSpanX = minSpan[0];
                                    appWidgetInfo.minSpanY = minSpan[1];

                                    container = c.getInt(containerIndex);
                                    if (container != LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                                        container != LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                                        Log.e(TAG, "Widget found where container != " +
                                            "CONTAINER_DESKTOP nor CONTAINER_HOTSEAT - ignoring!");
                                        continue;
                                    }
                                    appWidgetInfo.container = c.getInt(containerIndex);

                                    // check & update map of what's occupied
                                    if (!checkItemPlacement(occupied, appWidgetInfo)) {
                                        break;
                                    }
                                    sBgItemsIdMap.put(appWidgetInfo.id, appWidgetInfo);
                                    sBgAppWidgets.add(appWidgetInfo);
                                }
                                break;
                            case LauncherSettings.Favorites.ITEM_TYPE_DUMMY:

                                dummyInfo = new DummyInfo();
                                dummyInfo.id = id = c.getLong(idIndex);
                                dummyInfo.screen = c.getInt(screenIndex);
                                dummyInfo.cellX = c.getInt(cellXIndex);
                                dummyInfo.cellY = c.getInt(cellYIndex);
                                dummyInfo.spanX = c.getInt(spanXIndex);
                                dummyInfo.spanY = c.getInt(spanYIndex);
                                dummyInfo.minSpanX = dummyInfo.spanX;
                                dummyInfo.minSpanY = dummyInfo.spanY;
                                dummyInfo.inAppWidget = (c.getInt(dummyTypeIndex) != 0);
                                if (dummyInfo.inAppWidget) {
                                    dummyInfo.inAppWidgetClassName = c.getString(dummyNameIndex);
                                } else {
                                    dummyInfo.widgetPackageName = c.getString(dummyNameIndex);
                                }

                                container = c.getInt(containerIndex);
                                if (container != LauncherSettings.Favorites.CONTAINER_DESKTOP ) {
                                    Log.e(TAG, "Dummy Item found where container != " +
                                        "CONTAINER_DESKTOP - ignoring!");
                                    continue;
                                }
                                dummyInfo.container = c.getInt(containerIndex);

                                // check & update map of what's occupied
                                if (!checkItemPlacement(occupied, dummyInfo)) {
                                    break;
                                }

                                sBgItemsIdMap.put(dummyInfo.id, dummyInfo);
                                sBgDummyItems.add(dummyInfo);
                                break;
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Desktop items loading interrupted:", e);
                        }
                    }
                } finally {
                    c.close();
                }

                if (itemsToRemove.size() > 0) {
                    ContentProviderClient client = contentResolver.acquireContentProviderClient(
                                    LauncherSettings.Favorites.CONTENT_URI);
                    // Remove dead items
                    for (long id : itemsToRemove) {
                        // Don't notify content observers
                        try {
                            client.delete(LauncherSettings.Favorites.getContentUri(id, false),
                                    null, null);
                        } catch (RemoteException e) {
                            Log.w(TAG, "Could not remove id = " + id);
                        }
                    }
                }
            }
        }

        @Override
        protected void bindWorkspace(int synchronizeBindPage) {
            Runnable r;

            // Don't use these two variables in any of the callback runnables.
            // Otherwise we hold a reference to them.
            final Callbacks oldCallbacks = (Callbacks) mCallbacks.get();
            if (oldCallbacks == null) {
                // WeakReferenceの参照(Launcherクラス)が消えている

                // このランチャーは既に終了しているので、何もしない.
                Log.w(TAG, "LoaderTask running with no launcher");
                return;
            }

            final int workspaceNumPages;
            final int workspaceDefaultPage;
            final int themeMode;
            final String themeId;
            final boolean inAppTheme;
            final boolean pagLoop;
            final String[] wpPaths;
            final String iconThemeId;
            final boolean iconInAppTheme;
            synchronized (sBgLock) {
                workspaceNumPages = sWorkspaceNumPages;
                workspaceDefaultPage = sWorkspaceDefaultPage;
                themeMode = sThemeMode;
                themeId = sThemeId;
                inAppTheme = sInAppTheme;
                pagLoop = sPageLoop;
                wpPaths = sWpPaths;
                iconThemeId = sIconThemeId;
                iconInAppTheme = sIconInAppTheme;
            }

            final boolean isLoadingSynchronously = (synchronizeBindPage > -1);
//            final int currentScreen = isLoadingSynchronously ? synchronizeBindPage :
//                oldCallbacks.getCurrentWorkspaceScreen();
            final int currentScreen = isLoadingSynchronously ? synchronizeBindPage : workspaceDefaultPage;

            // はじめにカレントページの全要素の読み込みを先に行う。
            // その前に、下の方のstartBinding()を呼ぶ前に、Workspaceに存在する全要素をアンバインドする。
            unbindWorkspaceItemsOnMainThread();

            if (!isLoadingSynchronously) {
                r = new Runnable() {
                    public void run() {
                        Callbacks callbacks = (Callbacks) tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.initWorkspacePages(workspaceNumPages, workspaceDefaultPage);
                        }
                    }
                };
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
            }

            ArrayList<ItemInfo> workspaceItems = new ArrayList<ItemInfo>();
            ArrayList<LauncherAppWidgetInfo> appWidgets =
                    new ArrayList<LauncherAppWidgetInfo>();
            ArrayList<DummyInfo> dummyItems = new ArrayList<DummyInfo>();
            HashMap<Long, FolderInfo> folders = new HashMap<Long, FolderInfo>();
            HashMap<Long, ItemInfo> itemsIdMap = new HashMap<Long, ItemInfo>();
            synchronized (sBgLock) {
                workspaceItems.addAll(sBgWorkspaceItems);
                appWidgets.addAll(sBgAppWidgets);
                dummyItems.addAll(sBgDummyItems);
                folders.putAll(sBgFolders);
                itemsIdMap.putAll(sBgItemsIdMap);
            }

            ArrayList<ItemInfo> currentWorkspaceItems = new ArrayList<ItemInfo>();
            ArrayList<ItemInfo> otherWorkspaceItems = new ArrayList<ItemInfo>();
            ArrayList<LauncherAppWidgetInfo> currentAppWidgets =
                    new ArrayList<LauncherAppWidgetInfo>();
            ArrayList<LauncherAppWidgetInfo> otherAppWidgets =
                    new ArrayList<LauncherAppWidgetInfo>();
            ArrayList<DummyInfo> currentDummyItems = new ArrayList<DummyInfo>();
            ArrayList<DummyInfo> otherDummyItems = new ArrayList<DummyInfo>();
            HashMap<Long, FolderInfo> currentFolders = new HashMap<Long, FolderInfo>();
            HashMap<Long, FolderInfo> otherFolders = new HashMap<Long, FolderInfo>();

            // Separate the items that are on the current screen, and all the other remaining items
            filterCurrentWorkspaceItems(currentScreen, workspaceItems, currentWorkspaceItems,
                    otherWorkspaceItems);
            filterCurrentAppWidgets(currentScreen, appWidgets, currentAppWidgets,
                    otherAppWidgets);
            filterCurrentDummyItems(currentScreen, dummyItems, currentDummyItems,
                    otherDummyItems);
            filterCurrentFolders(currentScreen, itemsIdMap, folders, currentFolders,
                    otherFolders);
            sortWorkspaceItemsSpatially(currentWorkspaceItems, workspaceNumPages);
            sortWorkspaceItemsSpatially(otherWorkspaceItems, workspaceNumPages);

            // Tell the workspace that we're about to start binding items
            r = new Runnable() {
                public void run() {
                    Callbacks callbacks = (Callbacks) tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.startBinding();
                    }
                }
            };
            runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);

            // Load items on the current page
            bindWorkspaceItems(oldCallbacks, currentWorkspaceItems, currentAppWidgets,
                    currentDummyItems, currentFolders, null);
            if (isLoadingSynchronously) {
                r = new Runnable() {
                    public void run() {
                        Callbacks callbacks = (Callbacks) tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.onPageBoundSynchronously(currentScreen);
                        }
                    }
                };
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
            }

            // Load all the remaining pages (if we are loading synchronously, we want to defer this
            // work until after the first render)
            final ArrayList<Runnable> deferredBindRunnables = getDeferredBindRunnables();
            deferredBindRunnables.clear();
            bindWorkspaceItems(oldCallbacks, otherWorkspaceItems, otherAppWidgets, otherDummyItems,
                    otherFolders, (isLoadingSynchronously ? deferredBindRunnables : null));

            // Tell the workspace that we're done binding items
            r = new Runnable() {
                public void run() {
                    Callbacks callbacks = (Callbacks) tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.finishBindingItems();
                    }

                    mIsLoadingAndBindingWorkspace = false;
                }
            };
            if (isLoadingSynchronously) {
                deferredBindRunnables.add(r);
            } else {
                runOnMainThread(r, MAIN_THREAD_BINDING_RUNNABLE);
            }

            if (themeId != null) {
                boolean applied = (mAppliedThemeId != null);

                String spKey = LauncherApplication.getSharedPreferencesKey();
                SharedPreferences sp = mContext.getSharedPreferences(spKey, Context.MODE_PRIVATE);
                if(!sp.getBoolean(SP_KEY_KISEKAE_INIT_FLG, false)) {
                    // 着せ替え対象のアイコンがこの時点ではわからないため。
                    applied = false;
                }

                loadAndSetTheme(mContext, themeId, (themeMode == 1),
                        inAppTheme, wpPaths, workspaceNumPages, workspaceDefaultPage,
                        iconThemeId, iconInAppTheme, pagLoop, applied, mAppliedWidgetInfos, mAppliedShortcutDefs);
            }
        }
    }

    /** Sorts the set of items by hotseat, workspace (spatially from top to bottom, left to
     * right) */
    protected void sortWorkspaceItemsSpatially(ArrayList<ItemInfo> workspaceItems, final int screenCount) {
        // XXX: review this
        Collections.sort(workspaceItems, new Comparator<ItemInfo>() {
            @Override
            public int compare(ItemInfo lhs, ItemInfo rhs) {
                int cellCountX = getCellCountX();
                int cellCountY = getCellCountY();
                int screenOffset = cellCountX * cellCountY;
                int containerOffset = screenOffset * (screenCount + 1); // +1 hotseat
                long lr = (lhs.container * containerOffset + lhs.screen * screenOffset +
                        lhs.cellY * cellCountX + lhs.cellX);
                long rr = (rhs.container * containerOffset + rhs.screen * screenOffset +
                        rhs.cellY * cellCountX + rhs.cellX);
                return (int) (lr - rr);
            }
        });
    }
}
