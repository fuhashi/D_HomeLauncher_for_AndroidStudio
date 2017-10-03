/*
 * Copyright (C) 2008 The Android Open Source Project
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.Toast;
import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.R;

public class InstallShortcutReceiver extends BroadcastReceiver {
    public static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    public static final String ACTION_INSTALL_SHORTCUT_KISEKAE = "jp.co.disney.apps.managed.kisekaeapp.action.INSTALL_SHORTCUT";
    public static final String NEW_APPS_PAGE_KEY = "apps.new.page";
    public static final String NEW_APPS_LIST_KEY = "apps.new.list";

    public static final String DATA_INTENT_KEY = "intent.data";
    public static final String LAUNCH_INTENT_KEY = "intent.launch";
    public static final String NAME_KEY = "name";
    public static final String ICON_KEY = "icon";
    public static final String ICON_RESOURCE_NAME_KEY = "iconResource";
    public static final String ICON_RESOURCE_PACKAGE_NAME_KEY = "iconResourcePackage";
    // The set of shortcuts that are pending install
    public static final String APPS_PENDING_INSTALL = "apps_to_install";

    public static final int NEW_SHORTCUT_BOUNCE_DURATION = 450;
    public static final int NEW_SHORTCUT_STAGGER_DELAY = 75;

    private static final int INSTALL_SHORTCUT_SUCCESSFUL = 0;
    private static final int INSTALL_SHORTCUT_IS_DUPLICATE = -1;
    private static final int INSTALL_SHORTCUT_NO_SPACE = -2;
    private static final int INSTALL_SHORTCUT_FAILED = -3;

    public static final String EXTRA_SHORTCUT_NAME_USER = "jp.co.disney.apps.managed.kisekaeapp.intent.extra.shortcut.NAME_USER";
    public static final String EXTRA_SHORTCUT_SCREEN = "jp.co.disney.apps.managed.kisekaeapp.intent.extra.shortcut.SCREEN";
    public static final String EXTRA_SHORTCUT_CELLX = "jp.co.disney.apps.managed.kisekaeapp.intent.extra.shortcut.CELLX";
    public static final String EXTRA_SHORTCUT_CELLY = "jp.co.disney.apps.managed.kisekaeapp.intent.extra.shortcut.CELLY";
    public static final String EXTRA_SHORTCUT_OVERWRITE = "jp.co.disney.apps.managed.kisekaeapp.intent.extra.shortcut.OVERWRITE";
    public static final String EXTRA_SHORTCUT_ID = "jp.co.disney.apps.managed.kisekaeapp.intent.extra.shortcut.ID";
    public static final String EXTRA_SHORTCUT_NEXT_FLAG = "jp.co.disney.apps.managed.kisekaeapp.intent.extra.shortcut.NEXT_FLAG";

    // A mime-type representing shortcut data
    public static final String SHORTCUT_MIMETYPE = "jp.co.disney.apps.managed.kisekaeapp/shortcut";

    private static Object sLock = new Object();

    private static void addToStringSet(SharedPreferences sharedPrefs,
            SharedPreferences.Editor editor, String key, String value) {
        Set<String> strings = sharedPrefs.getStringSet(key, null);
        if (strings == null) {
            strings = new HashSet<String>(0);
        } else {
            strings = new HashSet<String>(strings);
        }
        strings.add(value);
        editor.putStringSet(key, strings);
    }

    private static void addToInstallQueue(
            SharedPreferences sharedPrefs, PendingInstallShortcutInfo info) {
        synchronized(sLock) {
            try {
                JSONStringer json = new JSONStringer()
                    .object()
                    .key(DATA_INTENT_KEY).value(info.data.toUri(0))
                    .key(LAUNCH_INTENT_KEY).value(info.launchIntent.toUri(0))
                    .key(NAME_KEY).value(info.name);
                if (info.icon != null) {
                    byte[] iconByteArray = ItemInfo.flattenBitmap(info.icon);
                    json = json.key(ICON_KEY).value(
                        Base64.encodeToString(
                            iconByteArray, 0, iconByteArray.length, Base64.DEFAULT));
                }
                if (info.iconResource != null) {
                    json = json.key(ICON_RESOURCE_NAME_KEY).value(info.iconResource.resourceName);
                    json = json.key(ICON_RESOURCE_PACKAGE_NAME_KEY)
                        .value(info.iconResource.packageName);
                }
                json = json.endObject();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                addToStringSet(sharedPrefs, editor, APPS_PENDING_INSTALL, json.toString());
                editor.commit();
            } catch (org.json.JSONException e) {
                DebugLog.instance.outputLog("InstallShortcutReceiver", "Exception when adding shortcut: " + e);
            }
        }
    }

    private static ArrayList<PendingInstallShortcutInfo> getAndClearInstallQueue(
            SharedPreferences sharedPrefs) {
        synchronized(sLock) {
            Set<String> strings = sharedPrefs.getStringSet(APPS_PENDING_INSTALL, null);
            if (strings == null) {
                return new ArrayList<PendingInstallShortcutInfo>();
            }
            ArrayList<PendingInstallShortcutInfo> infos =
                new ArrayList<PendingInstallShortcutInfo>();
            for (String json : strings) {
                try {
                    JSONObject object = (JSONObject) new JSONTokener(json).nextValue();
                    Intent data = Intent.parseUri(object.getString(DATA_INTENT_KEY), 0);
                    Intent launchIntent = Intent.parseUri(object.getString(LAUNCH_INTENT_KEY), 0);
                    String name = object.getString(NAME_KEY);
                    String iconBase64 = object.optString(ICON_KEY);
                    String iconResourceName = object.optString(ICON_RESOURCE_NAME_KEY);
                    String iconResourcePackageName =
                        object.optString(ICON_RESOURCE_PACKAGE_NAME_KEY);
                    if (iconBase64 != null && !iconBase64.isEmpty()) {
                        byte[] iconArray = Base64.decode(iconBase64, Base64.DEFAULT);
                        Bitmap b = BitmapFactory.decodeByteArray(iconArray, 0, iconArray.length);
                        data.putExtra(Intent.EXTRA_SHORTCUT_ICON, b);
                    } else if (iconResourceName != null && !iconResourceName.isEmpty()) {
                        Intent.ShortcutIconResource iconResource =
                            new Intent.ShortcutIconResource();
                        iconResource.resourceName = iconResourceName;
                        iconResource.packageName = iconResourcePackageName;
                        data.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
                    }
                    data.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
                    PendingInstallShortcutInfo info =
                        new PendingInstallShortcutInfo(data, name, launchIntent);
                    infos.add(info);
                } catch (org.json.JSONException e) {
                    DebugLog.instance.outputLog("InstallShortcutReceiver", "Exception reading shortcut to add: " + e);
                } catch (java.net.URISyntaxException e) {
                    DebugLog.instance.outputLog("InstallShortcutReceiver", "Exception reading shortcut to add: " + e);
                }
            }
            sharedPrefs.edit().putStringSet(APPS_PENDING_INSTALL, new HashSet<String>()).commit();
            return infos;
        }
    }

    // Determines whether to defer installing shortcuts immediately until
    // processAllPendingInstalls() is called.
    private static boolean mUseInstallQueue = false;

    private static class PendingInstallShortcutInfo {
        Intent data;
        Intent launchIntent;
        String name;
        Bitmap icon;
        Intent.ShortcutIconResource iconResource;

        public PendingInstallShortcutInfo(Intent rawData, String shortcutName,
                Intent shortcutIntent) {
            data = rawData;
            name = shortcutName;
            launchIntent = shortcutIntent;
        }
    }

    public void onReceive(Context context, Intent data) {
        if (!ACTION_INSTALL_SHORTCUT.equals(data.getAction()) && !ACTION_INSTALL_SHORTCUT_KISEKAE.equals(data.getAction())) {
            return;
        }

        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        if (intent == null) {
            return;
        }
        // This name is only used for comparisons and notifications, so fall back to activity name
        // if not supplied
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        if (name == null) {
            try {
                PackageManager pm = context.getPackageManager();
                ComponentName component = intent.getComponent();
                if (component == null) {
                    return;
                }
                ActivityInfo info = pm.getActivityInfo(component, 0);
                name = info.loadLabel(pm).toString();
            } catch (PackageManager.NameNotFoundException nnfe) {
                return;
            }
        }
        Bitmap icon = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
        Intent.ShortcutIconResource iconResource =
            data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);

        // Queue the item up for adding if launcher has not loaded properly yet
        boolean launcherNotLoaded = LauncherModel.getCellCountX() <= 0 ||
                LauncherModel.getCellCountY() <= 0;

        PendingInstallShortcutInfo info = new PendingInstallShortcutInfo(data, name, intent);
        info.icon = icon;
        info.iconResource = iconResource;
        if (mUseInstallQueue || launcherNotLoaded) {
            String spKey = LauncherApplication.getSharedPreferencesKey();
            SharedPreferences sp = context.getSharedPreferences(spKey, Context.MODE_PRIVATE);
            addToInstallQueue(sp, info);
        } else {
            processInstallShortcut(context, info);
        }
    }

    static void enableInstallQueue() {
        mUseInstallQueue = true;
    }
    static void disableAndFlushInstallQueue(Context context) {
        mUseInstallQueue = false;
        flushInstallQueue(context);
    }
    static void flushInstallQueue(Context context) {
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = context.getSharedPreferences(spKey, Context.MODE_PRIVATE);
        ArrayList<PendingInstallShortcutInfo> installQueue = getAndClearInstallQueue(sp);
        Iterator<PendingInstallShortcutInfo> iter = installQueue.iterator();
        while (iter.hasNext()) {
            processInstallShortcut(context, iter.next());
        }
    }

    private static void processInstallShortcut(Context context,
            PendingInstallShortcutInfo pendingInfo) {
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = context.getSharedPreferences(spKey, Context.MODE_PRIVATE);

        final Intent data = pendingInfo.data;
        final Intent intent = pendingInfo.launchIntent;
        final String name = pendingInfo.name;

        final int screen = data.getIntExtra(EXTRA_SHORTCUT_SCREEN, -1);
        final int cellX = data.getIntExtra(EXTRA_SHORTCUT_CELLX, -1);
        final int cellY = data.getIntExtra(EXTRA_SHORTCUT_CELLY, -1);
        final boolean overwrite = data.getBooleanExtra(EXTRA_SHORTCUT_OVERWRITE, false);
        final long shortcutId = data.getLongExtra(EXTRA_SHORTCUT_ID, -1L);

        final boolean nextFlg = data.getBooleanExtra(EXTRA_SHORTCUT_NEXT_FLAG, false);

        // Lock on the app so that we don't try and get the items while apps are being added
        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        final int[] result = {INSTALL_SHORTCUT_SUCCESSFUL};
        boolean found = false;
        boolean oldIgnoreDbChange;
        synchronized (app) {

            oldIgnoreDbChange = app.getIgnoreDbChange();
            app.setIgnoreDbChange(nextFlg);

            // Flush the LauncherModel worker thread, so that if we just did another
            // processInstallShortcut, we give it time for its shortcut to get added to the
            // database (getItemsInLocalCoordinates reads the database)
            app.getModel().flushWorkerThread();

            final ArrayList<ItemInfo> items = LauncherModel.getItemsInLocalCoordinates(context);
            final boolean exists = LauncherModel.shortcutExists(context, name, intent);

            final int[] outScreenCount = new int[1];
            final int[] outDefaultScreen = new int[1];
            LauncherModel.getScreenInfoFromDatabase(context, outScreenCount, outDefaultScreen);

            final int screenCount = outScreenCount[0];
            if (screen >= 0 && cellX >= 0 && cellY >= 0) {
                // 作成位置指定

                final int xCount = LauncherModel.getCellCountX();
                final int yCount = LauncherModel.getCellCountY();
                if (screen < screenCount && cellX < xCount && cellY < yCount) {

                    ItemInfo oldItem = null;
                    if (overwrite) {
                        // 既存のショートカットを変更

                        // 既存ショートカット抽出
                        for(ItemInfo item : items) {
                            if (item.screen == screen && item.cellX == cellX && item.cellY == cellY) {
                                oldItem = item;
                                break;
                            }
                        }

                        // 同一性チェック
                        if (oldItem != null) {
                            if (oldItem.id != shortcutId) {
                                oldItem = null;
                            }
                        }
                    }

                    found = installShortcut(context, items, screen, cellX, cellY, data, intent, sp, oldItem, result);
                } else {
                    // 指定位置不正
                    result[0] = INSTALL_SHORTCUT_FAILED;
                }

            } else {
                // デフォルトホーム画面を起点に左右を探査
                final int start = outDefaultScreen[0];
                for (int i = 0; i < (2 * screenCount) + 1 && !found; ++i) {
                    int si = start + (int) ((i / 2f) + 0.5f) * ((i % 2 == 1) ? 1 : -1);
                    if (0 <= si && si < screenCount) {
                        found = installShortcut(context, items, si, data, exists, intent, sp, result);
                    }
                }
            }
        }

        // We only report error messages (duplicate shortcut or out of space) as the add-animation
        // will provide feedback otherwise
        if (!found) {

            if (!nextFlg && oldIgnoreDbChange) {
                synchronized (app) {
                    app.resetLoader();
                }
            }

            if (result[0] == INSTALL_SHORTCUT_NO_SPACE) {
                Toast.makeText(context, context.getString(R.string.completely_out_of_space),
                        Toast.LENGTH_SHORT).show();
            } else if (result[0] == INSTALL_SHORTCUT_IS_DUPLICATE) {
                Toast.makeText(context, context.getString(R.string.shortcut_duplicate, name),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getString(R.string.shortcut_install_failed),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static boolean installShortcut(final Context context, final ArrayList<ItemInfo> items,
            final int screen, final int cellX, final int cellY,
            final Intent data, final Intent intent, final SharedPreferences sharedPrefs, ItemInfo oldItem, int[] result) {

        if (oldItem == null) {
            if (!isEmptyCell(context, items, screen, cellX, cellY, Workspace.ICON_SPAN, Workspace.ICON_SPAN)) {
                result[0] = INSTALL_SHORTCUT_NO_SPACE;
                return false;
            }
        }

        if (!installShortcut_main(context, data, intent, screen,
                cellX, cellY, sharedPrefs, oldItem)) {
            result[0] = INSTALL_SHORTCUT_FAILED;
            return false;
        }

        return true;
    }

    private static boolean installShortcut(final Context context, final ArrayList<ItemInfo> items,
            final int screen, final Intent data, final boolean shortcutExists, final Intent intent,
            final SharedPreferences sharedPrefs, int[] result) {

        final int[] tmpCoordinates = new int[2];
        if (!findEmptyCell(context, items, tmpCoordinates, screen)) {
            result[0] = INSTALL_SHORTCUT_NO_SPACE;
            return false;
        }

        // By default, we allow for duplicate entries (located in
        // different places)
        boolean duplicate = data.getBooleanExtra(Launcher.EXTRA_SHORTCUT_DUPLICATE, true);
        if (!duplicate && shortcutExists) {
             result[0] = INSTALL_SHORTCUT_IS_DUPLICATE;
             return false;
        }

        if (!installShortcut_main(context, data, intent, screen,
                tmpCoordinates[0], tmpCoordinates[1], sharedPrefs, null)) {
            result[0] = INSTALL_SHORTCUT_FAILED;
            return false;
        }

        return true;
    }

    private static boolean installShortcut_main(final Context context, final Intent data,
            final Intent intent, final int screen, final int cellX, final int cellY,
            final SharedPreferences sharedPrefs, ItemInfo oldItem) {

        if (intent.getAction() == null) {
            intent.setAction(Intent.ACTION_VIEW);
        } else if (intent.getAction().equals(Intent.ACTION_MAIN) &&
                intent.getCategories() != null &&
                intent.getCategories().contains(Intent.CATEGORY_LAUNCHER)) {
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        }

        new Thread("setNewAppsThread") {
            public void run() {
                synchronized (sLock) {
                    // If the new app is going to fall into the same page as before,
                    // then just continue adding to the current page
                    final int newAppsScreen = sharedPrefs.getInt(
                            NEW_APPS_PAGE_KEY, screen);
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    if (newAppsScreen == -1 || newAppsScreen == screen) {
                        addToStringSet(sharedPrefs,
                            editor, NEW_APPS_LIST_KEY,
                            intent.toUri(0) + "|" + screen + "|" + cellX + "|" + cellY);
                    }
                    editor.putInt(NEW_APPS_PAGE_KEY, screen);
                    editor.commit();
                }
            }
        }.start();

        // Update the Launcher db
        if (oldItem != null) {
            LauncherModel.deleteItemFromDatabase(context, oldItem);
        }
        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        ShortcutInfo info = app.getModel().addShortcut(context, data,
                LauncherSettings.Favorites.CONTAINER_DESKTOP, screen, cellX, cellY, true);
        if (info == null) {
            return false;
        }

        return true;
    }

    private static boolean findEmptyCell(Context context, ArrayList<ItemInfo> items, int[] xy,
            int screen) {
        final int xCount = LauncherModel.getCellCountX();
        final int yCount = LauncherModel.getCellCountY();
        boolean[][] occupied = createOccupiedArray(items, xCount, yCount, screen);

        return CellLayout.findVacantCell(xy, Workspace.ICON_SPAN, Workspace.ICON_SPAN, xCount, yCount, occupied);
    }

    private static boolean isEmptyCell(Context context, ArrayList<ItemInfo> items,
            int prmScreen, int prmCellX, int prmCellY, int prmSpanX, int prmSpanY) {

        final int xCount = LauncherModel.getCellCountX();
        final int yCount = LauncherModel.getCellCountY();

        boolean[][] occupied = createOccupiedArray(items, xCount, yCount, prmScreen);

        boolean isEmpty = false;
        if (!occupied[prmCellX][prmCellY] && prmCellX + prmSpanX <= xCount && prmCellY + prmSpanY <= yCount) {
            isEmpty = true;
            for (int x = prmCellX; x < prmCellX + prmSpanX; x++) {
                for (int y = prmCellY; y < prmCellY + prmSpanY; y++) {
                    if (occupied[x][y]) {
                        isEmpty = false;
                        break;
                    }
                }
            }
        }

        return isEmpty;
    }

    private static boolean[][] createOccupiedArray(ArrayList<ItemInfo> items, int xCount, int yCount, int screen) {

        final boolean[][] occupied = new boolean[xCount][yCount];

        ItemInfo item = null;
        int cellX, cellY, spanX, spanY;
        for (int i = 0; i < items.size(); ++i) {
            item = items.get(i);
            if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                if (item.screen == screen) {
                    cellX = item.cellX;
                    cellY = item.cellY;
                    spanX = item.spanX;
                    spanY = item.spanY;
                    for (int x = cellX; 0 <= x && x < cellX + spanX && x < xCount; x++) {
                        for (int y = cellY; 0 <= y && y < cellY + spanY && y < yCount; y++) {
                            occupied[x][y] = true;
                        }
                    }
                }
            }
        }
        return occupied;
    }
}
