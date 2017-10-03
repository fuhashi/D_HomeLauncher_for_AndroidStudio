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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetHost;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import jp.co.disney.apps.managed.kisekaeapp.DebugLog;

public class LauncherProvider extends ContentProvider {
    private static final String TAG = "LauncherProvider";

    private static final String DATABASE_NAME = "disney_kisekae.db";

    private static final int DATABASE_VERSION = 2;

    static final String AUTHORITY = "jp.co.disney.apps.managed.kisekaeapp.settings";

    static final String TABLE_FAVORITES = "favorites";
    static final String TABLE_FAVORITES_HEAD = "favorites_head";
    static final String TABLE_KISEKAE_MAP = "kisekae_map";
    static final String TABLE_BADGE_INFO = "badge_info";

    static final String PARAMETER_NOTIFY = "notify";
    static final String DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED =
            "DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED";

    /**
     * {@link Uri} triggered at any registered {@link android.database.ContentObserver} when
     * {@link AppWidgetHost#deleteHost()} is called during database creation.
     * Use this to recall {@link AppWidgetHost#startListening()} if needed.
     */
    static final Uri CONTENT_APPWIDGET_RESET_URI =
            Uri.parse("content://" + AUTHORITY + "/appWidgetReset");

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        ((LauncherApplication) getContext()).setLauncherProvider(this);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        } else {
            return "vnd.android.cursor.item/" + args.table;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);

        return result;
    }

    private static long dbInsertAndCheck(DatabaseHelper helper,
            SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (table.equals(LauncherProvider.TABLE_FAVORITES)) {
            if (!values.containsKey(LauncherSettings.Favorites._ID)) {
                throw new RuntimeException("Error: attempting to add item without specifying an id");
            }
        }
        return db.insert(table, nullColumnHack, values);
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final long rowId = dbInsertAndCheck(mOpenHelper, db, args.table, null, initialValues);
        if (rowId <= 0) return null;

        uri = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri);

        return uri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                if (dbInsertAndCheck(mOpenHelper, db, args.table, null, values[i]) < 0) {
                    return 0;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        sendNotify(uri);
        return values.length;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        // Overrideでトランザクションを実装
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentProviderResult[] result = super.applyBatch(operations);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);
        if (count > 0) sendNotify(uri);

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0) sendNotify(uri);

        return count;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }

    public long generateNewId() {
        return mOpenHelper.generateNewId();
    }

    @SuppressLint("NewApi")
    public void deleteDatabase() {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final File dbFile = new File(db.getPath());
        mOpenHelper.close();
        if (dbFile.exists()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                deleteDatabase(dbFile);
            } else {
                SQLiteDatabase.deleteDatabase(dbFile);
            }
        }
        mOpenHelper = new DatabaseHelper(getContext());
    }

    /**
     * API15以下用に自前で定義
     * @param file
     * @return
     */
    private static boolean deleteDatabase(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }

        boolean deleted = false;
        deleted |= file.delete();
        deleted |= new File(file.getPath() + "-journal").delete();
        deleted |= new File(file.getPath() + "-shm").delete();
        deleted |= new File(file.getPath() + "-wal").delete();

        File dir = file.getParentFile();
        if (dir != null) {
            final String prefix = file.getName() + "-mj";
            File[] files = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File candidate) {
                    return candidate.getName().startsWith(prefix);
                }
            });
            if (files != null) {
                for (File masterJournal : files) {
                    deleted |= masterJournal.delete();
                }
            }
        }
        return deleted;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private final Context mContext;
        private final AppWidgetHost mAppWidgetHost;
        private long mMaxId = -1;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
            mAppWidgetHost = new AppWidgetHost(context, Launcher.APPWIDGET_HOST_ID);

            if (mMaxId == -1) {
                mMaxId = initializeMaxId(getWritableDatabase());
            }
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            DebugLog.instance.outputLog(TAG, "creating new launcher database");

            mMaxId = 1;

            db.execSQL("CREATE TABLE favorites (" +
                    "_id INTEGER PRIMARY KEY," +
                    "title TEXT," +
                    "intent TEXT," +
                    "container INTEGER," +
                    "screen INTEGER," +
                    "cellX INTEGER," +
                    "cellY INTEGER," +
                    "spanX INTEGER," +
                    "spanY INTEGER," +
                    "itemType INTEGER," +
                    "appWidgetId INTEGER NOT NULL DEFAULT -1," +
                    "isShortcut INTEGER," +
                    "iconType INTEGER," +
                    "iconPackage TEXT," +
                    "iconResource TEXT," +
                    "icon BLOB," +
                    "uri TEXT," +
                    "displayMode INTEGER," +
                    "shortcutName TEXT," +
                    "dummy_type INTEGER," +
                    "dummy_name TEXT" +
                    ");");

            db.execSQL("CREATE TABLE favorites_head (" +
                    "_id INTEGER PRIMARY KEY," +
                    "num_pages INTEGER," +
                    "default_page INTEGER," +
                    "theme_mode INTEGER," +
                    "theme_id TEXT," +
                    "in_app_theme INTEGER," +
                    "icon_theme_id TEXT," +
                    "icon_in_app_theme INTEGER," +
                    "page_loop," +
                    "wp_1 TEXT," +
                    "wp_2 TEXT," +
                    "wp_3 TEXT," +
                    "wp_4 TEXT," +
                    "wp_5 TEXT," +
                    "wp_6 TEXT," +
                    "wp_7 TEXT," +
                    "wp_8 TEXT," +
                    "wp_9 TEXT" +
                    ");");

            db.execSQL("CREATE TABLE kisekae_map (" +
                    "_id INTEGER PRIMARY KEY," +
                    "app_name TEXT," +
                    "theme_id TEXT," +
                    "in_app_theme INTEGER," +
                    "contents_type INTEGER," +
                    "ic_name TEXT" +
                    ");");

            db.execSQL("CREATE TABLE badge_info (" +
                    "app_name TEXT PRIMARY KEY," +
                    "badge_count INTEGER," +
                    "enable INTEGER" +
                    ");");

            insertFavorites_HeadDefaultData(db);

            // DBが新たに作成されたので、以前のWidgetは全て停止させる
            if (mAppWidgetHost != null) {
                mAppWidgetHost.deleteHost();
                sendAppWidgetResetNotify();
            }

            // Set a shared pref so that we know we need to load the default workspace later
            setFlagToLoadDefaultWorkspaceLater();
        }

        private void insertFavorites_HeadDefaultData(SQLiteDatabase db) {

            final String initialThemeId = ThemeUtils.THEME_ID_DEFAULT;
            final String themeRootDir = ThemeUtils.getThemeRootDirectory(mContext, initialThemeId, true, -1);
            final String themeWpPath = ThemeUtils.getThemeWallpaperPath(mContext, themeRootDir, 0, true);

            ContentValues values = new ContentValues();
            values.put(LauncherSettings.Favorites_Head._ID, 0);
            values.put(LauncherSettings.Favorites_Head.NUM_PAGES, 3);
            values.put(LauncherSettings.Favorites_Head.DEFAULT_PAGE, 1);
            values.put(LauncherSettings.Favorites_Head.THEME_MODE, 1);
            values.put(LauncherSettings.Favorites_Head.THEME_ID, initialThemeId);
            values.put(LauncherSettings.Favorites_Head.IN_APP_THEME, 1);
            values.put(LauncherSettings.Favorites_Head.ICON_IN_APP_THEME, 1);
            values.put(LauncherSettings.Favorites_Head.PAGE_LOOP, 0);
            values.put(LauncherSettings.Favorites_Head.WP_1, themeWpPath);
            values.put(LauncherSettings.Favorites_Head.WP_2, themeWpPath);
            values.put(LauncherSettings.Favorites_Head.WP_3, themeWpPath);

            db.insert(TABLE_FAVORITES_HEAD, null, values);
        }

        /**
         * Send notification that we've deleted the {@link AppWidgetHost},
         * probably as part of the initial database creation. The receiver may
         * want to re-call {@link AppWidgetHost#startListening()} to ensure
         * callbacks are correctly set.
         */
        private void sendAppWidgetResetNotify() {
            final ContentResolver resolver = mContext.getContentResolver();
            resolver.notifyChange(CONTENT_APPWIDGET_RESET_URI, null);
        }

        private void setFlagToLoadDefaultWorkspaceLater() {
            String spKey = LauncherApplication.getSharedPreferencesKey();
            SharedPreferences sp = mContext.getSharedPreferences(spKey, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED, true);
            editor.putBoolean(LauncherModel.SP_KEY_KISEKAE_INIT_FLG, false);
            editor.commit();
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            DebugLog.instance.outputLog(TAG, "onUpgrade triggered");

            int version = oldVersion;

            if (version < 2) {
                db.execSQL("CREATE TABLE badge_info (" +
                        "app_name TEXT PRIMARY KEY," +
                        "badge_count INTEGER," +
                        "enable INTEGER" +
                        ");");
                version = 2;
            }

            if (version != DATABASE_VERSION) {
                Log.w(TAG, "Destroying all old data.");
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
                onCreate(db);
            }
        }

        // Generates a new ID to use for an object in your database. This method should be only
        // called from the main UI thread. As an exception, we do call it when we call the
        // constructor from the worker thread; however, this doesn't extend until after the
        // constructor is called, and we only pass a reference to LauncherProvider to LauncherApp
        // after that point
        public long generateNewId() {
            if (mMaxId < 0) {
                throw new RuntimeException("Error: max id was not initialized");
            }
            mMaxId += 1;
            return mMaxId;
        }

        private long initializeMaxId(SQLiteDatabase db) {
            Cursor c = db.rawQuery("SELECT MAX(_id) FROM favorites", null);

            // get the result
            final int maxIdIndex = 0;
            long id = -1;
            if (c != null && c.moveToNext()) {
                id = c.getLong(maxIdIndex);
            }
            if (c != null) {
                c.close();
            }

            if (id == -1) {
                throw new RuntimeException("Error: could not query max id");
            }

            return id;
        }
    }

    /**
     * Build a query string that will match any row where the column matches
     * anything in the values list.
     */
    static String buildOrWhereString(String column, int[] values) {
        StringBuilder selectWhere = new StringBuilder();
        for (int i = values.length - 1; i >= 0; i--) {
            selectWhere.append(column).append("=").append(values[i]);
            if (i > 0) {
                selectWhere.append(" OR ");
            }
        }
        return selectWhere.toString();
    }

    static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }
}
