
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.adobe.mobile.Analytics;
import com.adobe.mobile.Config;
import com.adobe.mobile.CustomAndroidSystemObject;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.TextKeyListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Advanceable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.SplashActivity;
import jp.co.disney.apps.managed.kisekaeapp.TrackingHelper;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadSkinTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperator;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.iconpicker.IconPickerActivity;
import jp.co.disney.apps.managed.kisekaeapp.launcher.DropTarget.DragObject;
import jp.co.disney.apps.managed.kisekaeapp.launcher.DummyHomeItemView.CustomAdapter;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;
import jp.co.disney.apps.managed.kisekaeapp.system.graphics.BitmapUtils;
import jp.co.disney.apps.managed.kisekaeapp.system.view.PopupLayerView;
import jp.co.disney.apps.managed.kisekaeapp.system.view.PopupView;
import jp.co.disney.apps.managed.kisekaeapp.system.view.animation.FirstFrameAnimatorHelper;
import jp.co.disney.apps.managed.kisekaeapp.system.view.animation.LauncherViewPropertyAnimator;
import jp.co.disney.apps.managed.kisekaeapp.system.view.animation.ViewAnimUtils;
import jp.co.disney.apps.managed.kisekaeapp.system.view.pagedview.PageIndicatorView;
import jp.co.disney.apps.managed.kisekaeapp.system.view.pagedview.SmoothPagedView;
import jp.co.disney.apps.managed.kisekaeapp.widgetbattery.WidgetPickerActivity;

/**
 * Default launcher application.
 */
public final class Launcher extends Activity
        implements View.OnClickListener, OnLongClickListener, KisekaeLauncherModel.Callbacks,
                   View.OnTouchListener, DownloadSkinTaskCallback
                   ,ActivityCompat.OnRequestPermissionsResultCallback
                   {
    static final String TAG = "Launcher";
    static final boolean LOGD = false;

    static final boolean PROFILE_STARTUP = false;
    static final boolean DEBUG_WIDGETS = false;
    static final boolean DEBUG_STRICT_MODE = false;
    static final boolean DEBUG_RESUME_TIME = false;

    private static final int REQUEST_CREATE_SHORTCUT = 1;
    private static final int REQUEST_CREATE_APPWIDGET = 5;
    private static final int REQUEST_PICK_APPLICATION = 6;
    private static final int REQUEST_PICK_SHORTCUT = 7;
    private static final int REQUEST_PICK_APPWIDGET = 9;
    private static final int REQUEST_PICK_WALLPAPER = 10;

    private static final int REQUEST_BIND_APPWIDGET = 11;

    private static final int REQUEST_PICK_APPWIDGET_ICS = 101;
    private static final int REQUEST_CREATE_APPWIDGET_ICS = 102;

    static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";

    private static final String PREFERENCES = "launcher.preferences";
    // To turn on these properties, type
    // adb shell setprop log.tag.PROPERTY_NAME [VERBOSE | SUPPRESS]
    static final String FORCE_ENABLE_ROTATION_PROPERTY = "launcher_force_rotate";
    static final String DUMP_STATE_PROPERTY = "launcher_dump_state";

    // The Intent extra that defines whether to ignore the launch animation
    static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION =
            "jp.co.disney.apps.managed.kisekaeapp.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";

    // Type: int
    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    // Type: int
    private static final String RUNTIME_STATE = "launcher.state";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CONTAINER = "launcher.add_container";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SCREEN = "launcher.add_screen";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CELL_X = "launcher.add_cell_x";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CELL_Y = "launcher.add_cell_y";
    // Type: boolean
    private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME = "launcher.rename_folder";
    // Type: long
    private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME_ID = "launcher.rename_folder_id";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SPAN_X = "launcher.add_span_x";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SPAN_Y = "launcher.add_span_y";
    // Type: parcelable
    private static final String RUNTIME_STATE_PENDING_ADD_WIDGET_INFO = "launcher.add_widget_info";
    // Type: parcelable
    private static final String RUNTIME_STATE_PENDING_ADD_WIDGET_ID = "launcher.add_widget_id";

    /** The different states that Launcher can be in. */
    private enum State { NONE, WORKSPACE, APPS_CUSTOMIZE, APPS_CUSTOMIZE_SPRING_LOADED, PAGE_EDIT };
    private State mState = State.WORKSPACE;
    private AnimatorSet mStateAnimation;

    static final int APPWIDGET_HOST_ID = 1024;
    private static final int EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT = 300;
    private static final int EXIT_SPRINGLOADED_MODE_LONG_TIMEOUT = 600;

    // How long to wait before the new-shortcut animation automatically pans the workspace
    private static int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 10;

    private final BroadcastReceiver mCloseSystemDialogsReceiver
            = new CloseSystemDialogsIntentReceiver();
    private final ContentObserver mWidgetObserver = new AppWidgetResetObserver();

    private LayoutInflater mInflater;

    private Workspace mWorkspace;
    private View mLauncherView;
    private DragLayer mDragLayer;
    private DragController mDragController;

    private PopupLayerView mPopupLayerView;
    private HomeMenuView mHomeMenuView;
    private PopupView mHomeMenuPopup;

    private AppWidgetManager mAppWidgetManager;
    private LauncherAppWidgetHost mAppWidgetHost;

    private ItemInfo mPendingAddInfo = new ItemInfo();
    private AppWidgetProviderInfo mPendingAddWidgetInfo;
    private int mPendingAddWidgetId = -1;

    private int[] mTmpAddItemCellCoordinates = new int[2];

    private FolderInfo mFolderInfo;

    private Hotseat mHotseat;
    private View mAllAppsButton;

    private SearchDropTargetBar mSearchDropTargetBar;
    private AppsCustomizeTabHost mAppsCustomizeTabHost;
    private AppsCustomizePagedView mAppsCustomizeContent;
    private boolean mAutoAdvanceRunning = false;

    private PageEditScreen mPageEditScreen;

    private Bundle mSavedState;
    // We set the state in both onCreate and then onNewIntent in some cases, which causes both
    // scroll issues (because the workspace may not have been measured yet) and extra work.
    // Instead, just save the state that we need to restore Launcher to, and commit it in onResume.
    private State mOnResumeState = State.NONE;

    private SpannableStringBuilder mDefaultKeySsb = null;

    private boolean mWorkspaceLoading = true;

    private boolean mPaused = true;
    private boolean mRestoring;
    private boolean mWaitingForResult;
    private boolean mOnResumeNeedsLoad;

    private ArrayList<Runnable> mOnResumeCallbacks = new ArrayList<Runnable>();

    // Keep track of whether the user has left launcher
    private static boolean sPausedFromUserAction = false;

    private Bundle mSavedInstanceState;

    private KisekaeLauncherModel mModel;
    private IconCache mIconCache;
    private boolean mUserPresent = true;
    private boolean mVisible = false;
    private boolean mAttached = false;

    private static LocaleConfiguration sLocaleConfiguration = null;

    private static HashMap<Long, FolderInfo> sFolders = new HashMap<Long, FolderInfo>();

    // Related to the auto-advancing of widgets
    private final int ADVANCE_MSG = 1;
    private final int mAdvanceInterval = 20000;
    private final int mAdvanceStagger = 250;
    private long mAutoAdvanceSentTime;
    private long mAutoAdvanceTimeLeft = -1;
    private HashMap<View, AppWidgetProviderInfo> mWidgetsToAdvance =
        new HashMap<View, AppWidgetProviderInfo>();

    private final ArrayList<Integer> mSynchronouslyBoundPages = new ArrayList<Integer>();

    static final ArrayList<String> sDumpLogs = new ArrayList<String>();

    // We only want to get the SharedPreferences once since it does an FS stat each time we get
    // it from the context.
    private SharedPreferences mSharedPrefs;

    // Holds the page that we need to animate to, and the icon views that we need to animate up
    // when we scroll to that page on resume.
    private int mNewShortcutAnimatePage = -1;
    private ArrayList<View> mNewShortcutAnimateViews = new ArrayList<View>();
    private ImageView mFolderIconImageView;
    private Bitmap mFolderIconBitmap;
    private Canvas mFolderIconCanvas;
    private Rect mRectForFolderAnimation = new Rect();

    private BubbleTextView mWaitingForResume;

    private HideFromAccessibilityHelper mHideFromAccessibilityHelper;

    private Runnable mBuildLayersRunnable = new Runnable() {
        public void run() {
            if (mWorkspace != null) {
                mWorkspace.buildPageHardwareLayers();
            }
        }
    };

    private boolean mOpenWidgetPicker = false;

    private int mDefaultPage = 0;
    private boolean mIsThemeMode = false;
    private String mThemeId;
    private boolean mInAppTheme = true;
    private String mIconThemeId;
    private boolean mIconInAppTheme = true;
    private boolean mPageLoop = false;

    private boolean mHasThemeResLoaded = false;

    private int mWidgetHostViewCount = 0;

    private WallpaperChangeDetector mWpChangeDetector;

    private static ArrayList<PendingAddArguments> sPendingAddList
            = new ArrayList<PendingAddArguments>();

    private static class PendingAddArguments {
        int requestCode;
        Intent intent;
        long container;
        int screen;
        int cellX;
        int cellY;
    }

    private static boolean isPropertyEnabled(String propertyName) {
        return Log.isLoggable(propertyName, Log.VERBOSE);
    }

    boolean isThemeMode() {
        return mIsThemeMode;
    }

    String getThemeId() {
        return mThemeId;
    }

    boolean inAppTheme() {
        return mInAppTheme;
    }

    String getIconThemeId() {
        return mIconThemeId;
    }

    boolean iconInAppTheme() {
        return mIconInAppTheme;
    }

    boolean pageLoop() {
        return mPageLoop;
    }

    IconCache getIconCache() {
        return mIconCache;
    }

    String getWpPath(int index) {
        return mWorkspace.getPanelWallpapers().getPath(index);
    }

    int getDefaultPage() {
        return mDefaultPage;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onRestart_");
//        // APP Measurement
//        TrackingHelper.resume();
        if (Build.VERSION.SDK_INT >= 23) {//OSバージョン→OnCreate中のPermissionチェック中にHome押して再度戻ってきたときに、ここに来ること有。ただ、それ以外にPermissionがOFFの状態でここに来ることはないから、PermissionCheckでエラーになる時のTrackingHelper.resume();は不要。
            //パーミッションチェック
            for (int i = 0; i < Permissions.length; i++) {//PermissionCheckがエラーの時は、ユーザーが非許可にした時で、その時は必ずOnCreateを通るので、PermissionCheckでエラーになる時のTrackingHelper.resume();は不要。

                if (PermissionChecker.checkSelfPermission(Launcher.this
                        , Permissions[i])
                != PermissionChecker.PERMISSION_GRANTED) {
                    jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "Launcher_onRestart_checkSelfPermission_" + Permissions[i] + "_!= PERMISSION_GRANTED");
                    handler.removeCallbacks(runnnableForPermission);
                    PermissionCheckNG = true;
//					onCreateView();//チェックに行った時、背景がグレーになる事がある対応
                    // パーミッションをリクエストする
                    int MY_PERMISSION_REQUEST = 0;
                    if (Permissions[i].equals(Permissions[0])) {
                         MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_READ_PHONE_STATE;
                     } else if (Permissions[i].equals(Permissions[1])) {
                        MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_CALL_PHONE;
                    }
//     				else if (Permissions[i].equals(Permissions[2])) {//Pending中20160419
//						MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_GET_ACCOUNTS;
//					}

                    //パーミッションリクエスト
                    final int index = i;
                    final int PERMISSION_REQUEST = MY_PERMISSION_REQUEST;
                    runnnableForPermission = new Runnable() {

                        @Override public void run() {
                            ActivityCompat.requestPermissions(Launcher.this,
                                    new String[] { Permissions[index] },
                                    PERMISSION_REQUEST);
                        }
                    };
                    handler.post(runnnableForPermission);
                    break;
                } else {
                    jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "Launcher_onRestart_checkSelfPermission_" + Permissions[i] + "_== PERMISSION_GRANTED");
                }
            }

            if (!PermissionCheckNG) {
//                TrackingHelper.resume();
            	doAppMeasurement(true);
            }

        } else {//OSバージョン
//            TrackingHelper.resume();
        	doAppMeasurement(true);
        }//OSバージョン
    }

    private void doAppMeasurement(boolean isRestart){

    	SharedPreferences pref = getSharedPreferences("sc_setting", MODE_PRIVATE);
    	// 自動計測値の生成と整形
    	CustomAndroidSystemObject caso = new CustomAndroidSystemObject((Activity) this, pref);

    	// ContextData 変数用のHashmap を用意
    	HashMap cdata = new HashMap<String, Object>();
    	cdata.put("packageName",caso.getPackageName());
    	cdata.put("appVer",caso.getAppVer());
    	cdata.put("osName",caso.getOSNAME());
    	cdata.put("modelName",caso.getModelName());
    	cdata.put("osVer",caso.getOsVer());
    	cdata.put("iccImei",caso.getIccImei());
    	if(isRestart){
        	cdata.put("customEvent","resume");
    	}else{
    		cdata.put("customEvent","shutdown");
    	}

		SharedPreferences preferences = getSharedPreferences(SplashActivity.AUTH_RESULT_PREF, Context.MODE_PRIVATE);
    	String carrierId = preferences.getString(SplashActivity.AUTH_RESULT_CARRIER, "");

    	if(!carrierId.equals("")){
        	String market ="";
        	if(carrierId.equals(SplashActivity.AUTH_CARRIER_OND)){
        		market = "PinkMarket";
        	}else if(carrierId.equals(SplashActivity.AUTH_CARRIER_ONS)){
        		market = "BlackMarket";
        	}else if(carrierId.equals(SplashActivity.AUTH_CARRIER_AU)){
        		market = "DisneyPass";
        	}else if(carrierId.equals(SplashActivity.AUTH_CARRIER_CONPAS)){
        		market = "SBMMarket";
        	}
    		cdata.put("market", market);
    	}

    	if(isRestart){
        	Analytics.trackAction("resume()",cdata);
    	}else{
    		Analytics.trackAction("shutdown()",cdata);
    	}

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onRequestPermissionsResult__");
        switch (requestCode) {
        case MY_PERMISSIONS_REQUEST_READ_PHONE_STATE:
            if (grantResults.length > 0 && grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
                // パーミッションが必要な処理
                if (PermissionChecker.checkSelfPermission(
                        this, Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) {
                    jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "READ_PHONE_STATE!= PackageManager.PERMISSION_GRANTED２");
                    Launcher.this.finish();
                }else{
                }

            }
            break;
        case MY_PERMISSIONS_REQUEST_CALL_PHONE:
            if (grantResults.length > 0 && grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
                // パーミッションが必要な処理
                if (PermissionChecker.checkSelfPermission(
                        this, Manifest.permission.CALL_PHONE)
                        != PackageManager.PERMISSION_GRANTED) {
                    jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "CALL_PHONE_STATE!= PackageManager.PERMISSION_GRANTED２");
                    Launcher.this.finish();
                }else{
                }

            }
            break;
//    	case MY_PERMISSIONS_REQUEST_GET_ACCOUNTS://Pending中20160419
//            if (grantResults.length > 0 && grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
//                // パーミッションが必要な処理
//                if (PermissionChecker.checkSelfPermission(
//                		this, Manifest.permission.GET_ACCOUNTS)
//                        != PackageManager.PERMISSION_GRANTED) {
//	            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "GET_ACCOUNTS!= PackageManager.PERMISSION_GRANTED２");
//	            	Launcher.this.finish();
//                }else{
//	            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "GET_ACCOUNTS== PackageManager.PERMISSION_GRANTED２");
//                }
//
//            }
//            break;
            }
    }

//	private final String[] Permissions = {Manifest.permission.READ_PHONE_STATE,Manifest.permission.CALL_PHONE,Manifest.permission.GET_ACCOUNTS};//Pending中20160419
    private final String[] Permissions = {Manifest.permission.READ_PHONE_STATE,Manifest.permission.CALL_PHONE};

    private boolean PermissionCheckNG = false;//requestPermissionsで許可した瞬間にonCreateを通るので
    private final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 102;
    private final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 103;
//	private final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS = 104;//Pending中20160419

    private final Handler handler = new Handler();
    private Runnable runnnableForPermission;

    private boolean TrackingHelperstartDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onCreate()_");
     // SDK がapplication context にアクセスできるよう設定する
        Config.setContext(this.getApplicationContext());

       if (Build.VERSION.SDK_INT >= 23) {//OSバージョン
               TrackingHelperstartDone = false;
            //パーミッションチェック
            for (int i = 0; i < Permissions.length; i++) {

                if (PermissionChecker.checkSelfPermission(this
                        , Permissions[i])
                != PermissionChecker.PERMISSION_GRANTED) {
                    jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onCreate_checkSelfPermission_" + Permissions[i] + "_!= PERMISSION_GRANTED");
                    PermissionCheckNG = true;
                    onCreateView();//チェックに行っても落ちない対応
                    // パーミッションをリクエストする
                    int MY_PERMISSION_REQUEST = 0;
                    if (Permissions[i].equals(Permissions[0])) {
//						MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE;
//					}
                    //      				else if(Permissions[i].equals(Permissions[1]) ) {
                         MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_READ_PHONE_STATE;
                     } else if (Permissions[i].equals(Permissions[1])) {
                        MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_CALL_PHONE;
                    }
//     				else if (Permissions[i].equals(Permissions[2])) {//Pending中20160419
//						MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_GET_ACCOUNTS;
//					}

                    //パーミッションリクエスト
                    final int index = i;
                    final int PERMISSION_REQUEST = MY_PERMISSION_REQUEST;
                    runnnableForPermission = new Runnable() {

                        @Override public void run() {
                            ActivityCompat.requestPermissions(Launcher.this,
                                    new String[] { Permissions[index] },
                                    PERMISSION_REQUEST);
                        }
                    };
                    handler.post(runnnableForPermission);
                    break;

//					ActivityCompat.requestPermissions(this,
//							new String[] { Permissions[i] },
//							MY_PERMISSION_REQUEST);
//					break;

                } else {
                    jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onCreate_checkSelfPermission_" + Permissions[i] + "_== PERMISSION_GRANTED");
                }

            }//for (int i = 0; i < Permissions.length; i++) {
            if (!PermissionCheckNG) {//Permisshionチェック群が問題なかった時のみ、実行。
                onCreateView();
                onCreateAfterPermissionGranted();
            }
        } else {//OSバージョン
            onCreateView();
            onCreateAfterPermissionGranted();
        }
       super.onCreate(savedInstanceState);//tes
       mSavedState = savedInstanceState;//tes

    //→OS6.0対応の為、onCreateView()とonCreateAfterPermissionGranted()に分割して移動
//        if (DEBUG_STRICT_MODE) {
//            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//                    .detectDiskReads()
//                    .detectDiskWrites()
//                    .detectNetwork()   // or .detectAll() for all detectable problems
//                    .penaltyLog()
//                    .build());
//            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                    .detectLeakedSqlLiteObjects()
//                    .detectLeakedClosableObjects()
//                    .penaltyLog()
//                    .penaltyDeath()
//                    .build());
//        }
//
//        super.onCreate(savedInstanceState);
//
//        // APP Measurement
//        TrackingHelper.start(this, "", SPPUtility.isDebugFlag);
//
//        LauncherApplication app = ((LauncherApplication)getApplication());
//        mSharedPrefs = getSharedPreferences(LauncherApplication.getSharedPreferencesKey(),
//                Context.MODE_PRIVATE);
//        mModel = app.setLauncher(this);
//        mIconCache = app.getIconCache();
//        mDragController = new DragController(this);
//        mInflater = getLayoutInflater();
//
//        mWpChangeDetector = new WallpaperChangeDetector();
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//            mHideFromAccessibilityHelper = new HideFromAccessibilityHelper();
//        }
//
//        mWidgetHostViewCount = 0;
//
//        mAppWidgetManager = AppWidgetManager.getInstance(this);
//        mAppWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
//        mAppWidgetHost.startListening();
//
//        // If we are getting an onCreate, we can actually preempt onResume and unset mPaused here,
//        // this also ensures that any synchronous binding below doesn't re-trigger another
//        // LauncherModel load.
//        mPaused = false;
//
//        if (PROFILE_STARTUP) {
//            android.os.Debug.startMethodTracing(
//                    Environment.getExternalStorageDirectory() + "/launcher");
//        }
//
//        boolean initFlg = mSharedPrefs.getBoolean(LauncherModel.SP_KEY_INIT_FLG, false);
//        if (!initFlg) {
//            // プリファレンスを初期化
//            // (旧着せ替えアプリからのアップグレード時、ここで旧着せ替えの設定値が消えるはず)
//            SharedPreferences.Editor editor = mSharedPrefs.edit();
//            editor.clear();
//            editor.putBoolean(LauncherModel.SP_KEY_INIT_FLG, true);
//            editor.commit();
//        }
//
//        checkForLocaleChange();
//
////        //TODO GLの初期化だけ追加　15/6/8 togawa
////        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
////        config.useAccelerometer = false;
////        config.useCompass = false;
////        config.disableAudio= true;
////        initializeForView(listener, config);
////        //TODO GLの初期化だけ追加　15/6/8 togawa
//
//        setContentView(R.layout.launcher);
//        setupViews();
//
//        registerContentObservers();
//
//        lockAllApps();
//
//        mSavedState = savedInstanceState;
//
//        String themeId = null;
//        boolean inAppTheme = false; // 現状アプリ内テーマはカタログから適用されないのでfalse固定
//        int contentsType = 0;
//        if (mSavedState == null) {
//            // Activityインスタンス初回生成時はonCreateでIntentを処理する。
//            // (初回生成時以外でもIntent処理を行うととActivityが消えていた場合にも再度行われてしまう。)
//            Intent intent = getIntent();
//            if (intent != null) {
//                // カタログからのIntentデータ
//                themeId = intent.getStringExtra(ContentsOperator.INTENT_ACTION_EXTRA_ASSET_ID);
//                contentsType = intent.getIntExtra(ContentsOperator.INTENT_ACTION_EXTRA_CONTENTS_TYPE, 0);
//            }
//        } else {
//            // Activityインスタンスが消えている場合は前の状態を復元
//            restoreState(mSavedState);
//        }
//
//        // Update customization drawer _after_ restoring the states
//        if (mAppsCustomizeContent != null) {
//            mAppsCustomizeContent.onPackagesUpdated(
//                LauncherModel.getSortedWidgetsAndShortcuts(this));
//        }
//
//        if (PROFILE_STARTUP) {
//            android.os.Debug.stopMethodTracing();
//        }
//
//        if (!mRestoring) {
//            if (sPausedFromUserAction) {
//                // If the user leaves launcher, then we should just load items asynchronously when
//                // they return.
//                if (themeId != null && ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_THEME) {
//                    mModel.startLoader(true, -1, themeId, inAppTheme);
//                } else {
//                    mModel.startLoader(true, -1, null, inAppTheme);
//                }
//
//            } else {
//                if (themeId != null && ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_THEME) {
//                    mModel.startLoader(true, mWorkspace.getCurrentPage(), themeId, inAppTheme);
//                } else {
//                    mModel.startLoader(true, mWorkspace.getCurrentPage(), null, inAppTheme);
//                }
//            }
//        }
//
//        if (!mModel.isAllAppsLoaded()) {
//            ViewGroup appsCustomizeContentParent = (ViewGroup) mAppsCustomizeContent.getParent();
//            mInflater.inflate(R.layout.apps_customize_progressbar, appsCustomizeContentParent);
//        }
//
//        // For handling default keys
//        mDefaultKeySsb = new SpannableStringBuilder();
//        Selection.setSelection(mDefaultKeySsb, 0);
//
//        IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
//        registerReceiver(mCloseSystemDialogsReceiver, filter);
//
//        if (mRestoring && themeId != null && ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_THEME) {
//            applyTheme(themeId, false);
//        }
//
//        if (themeId != null) {
//
//            if (ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T) {
//                mModel.applyKisekaeIcons(this, themeId, false);
//            } else if (ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_WIDGET
//                    || ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T) {
//                mAddThemeWdiget = true;
//            } else if (ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T
//                    || ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_WP) {
//                mApplyThemeWallpaper = true;
//            }
//        }
//
//        Utilities.setLegacyOverflow(getWindow(), true);
    }

    private void onCreateView() {
      if (DEBUG_STRICT_MODE) {
      StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
              .detectDiskReads()
              .detectDiskWrites()
              .detectNetwork()   // or .detectAll() for all detectable problems
              .penaltyLog()
              .build());
      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
              .detectLeakedSqlLiteObjects()
              .detectLeakedClosableObjects()
              .penaltyLog()
              .penaltyDeath()
              .build());
  }

//  super.onCreate(savedInstanceState);

//    // APP Measurement
//    TrackingHelper.start(this, "", SPPUtility.isDebugFlag);//→onCreateAfterPermissionGranted();へ

  LauncherApplication app = ((LauncherApplication)getApplication());
  mSharedPrefs = getSharedPreferences(LauncherApplication.getSharedPreferencesKey(),
          Context.MODE_PRIVATE);
  mModel = app.setLauncher(this);
  mIconCache = app.getIconCache();
  mDragController = new DragController(this);
  mInflater = getLayoutInflater();

  mWpChangeDetector = new WallpaperChangeDetector();

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      mHideFromAccessibilityHelper = new HideFromAccessibilityHelper();
  }

  mWidgetHostViewCount = 0;

  mAppWidgetManager = AppWidgetManager.getInstance(this);
  mAppWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
  mAppWidgetHost.startListening();

  // If we are getting an onCreate, we can actually preempt onResume and unset mPaused here,
  // this also ensures that any synchronous binding below doesn't re-trigger another
  // LauncherModel load.
  mPaused = false;

  if (PROFILE_STARTUP) {
      android.os.Debug.startMethodTracing(
              Environment.getExternalStorageDirectory() + "/launcher");
  }

  boolean initFlg = mSharedPrefs.getBoolean(LauncherModel.SP_KEY_INIT_FLG, false);
  if (!initFlg) {
      // プリファレンスを初期化
      // (旧着せ替えアプリからのアップグレード時、ここで旧着せ替えの設定値が消えるはず)
      SharedPreferences.Editor editor = mSharedPrefs.edit();
      editor.clear();
      editor.putBoolean(LauncherModel.SP_KEY_INIT_FLG, true);
      editor.commit();
  }

  checkForLocaleChange();

//  //TODO GLの初期化だけ追加　15/6/8 togawa
//  AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
//  config.useAccelerometer = false;
//  config.useCompass = false;
//  config.disableAudio= true;
//  initializeForView(listener, config);
//  //TODO GLの初期化だけ追加　15/6/8 togawa

  setContentView(R.layout.launcher);
  setupViews();

  registerContentObservers();

  lockAllApps();

//  mSavedState = savedInstanceState;

  String themeId = null;
  boolean inAppTheme = false; // 現状アプリ内テーマはカタログから適用されないのでfalse固定
  int contentsType = 0;
  if (mSavedState == null) {
      // Activityインスタンス初回生成時はonCreateでIntentを処理する。
      // (初回生成時以外でもIntent処理を行うととActivityが消えていた場合にも再度行われてしまう。)
      Intent intent = getIntent();
      if (intent != null) {
          // カタログからのIntentデータ
          themeId = intent.getStringExtra(ContentsOperator.INTENT_ACTION_EXTRA_ASSET_ID);
          contentsType = intent.getIntExtra(ContentsOperator.INTENT_ACTION_EXTRA_CONTENTS_TYPE, 0);
      }

      // OSバージョンアップ時に現在のきせかえ定義を更新する
      Context context = getApplicationContext();
      SharedPreferences sp = context.getSharedPreferences(LauncherApplication.getSharedPreferencesKey(),
              Context.MODE_PRIVATE);
      if(sp.getBoolean(LauncherModel.SP_KEY_KISEKAE_INIT_FLG, false)) { // 初回起動時は対象外
          // OSバージョンアップ判定
          int oldVersion = sp.getInt(LauncherModel.SP_KEY_OS_VERSION, -1);
          if (Build.VERSION.SDK_INT != oldVersion) { // 一応ダウングレードも考慮して
              // 更新
              KisekaeLauncherModel.refreshCurrentKisekaeMap(context, mIconCache);
              // 現在のOSバージョンを保存
              Editor editor = sp.edit();
              editor.putInt(LauncherModel.SP_KEY_OS_VERSION, Build.VERSION.SDK_INT);
              editor.apply();
          }
      } else {
          // 初回起動時は現在のOSバージョンを保存するのみ
          Editor editor = sp.edit();
          editor.putInt(LauncherModel.SP_KEY_OS_VERSION, Build.VERSION.SDK_INT);
          editor.apply();
      }

  } else {
      // Activityインスタンスが消えている場合は前の状態を復元
      restoreState(mSavedState);
  }

  // Update customization drawer _after_ restoring the states
  if (mAppsCustomizeContent != null) {
      mAppsCustomizeContent.onPackagesUpdated(
          LauncherModel.getSortedWidgetsAndShortcuts(this));
  }

  if (PROFILE_STARTUP) {
      android.os.Debug.stopMethodTracing();
  }

  if (!mRestoring) {

      if (sPausedFromUserAction) {
          // If the user leaves launcher, then we should just load items asynchronously when
          // they return.
          if (themeId != null && ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_THEME) {
              mModel.startLoader(true, -1, themeId, inAppTheme);
          } else {
              mModel.startLoader(true, -1, null, inAppTheme);
          }

      } else {
          if (themeId != null && ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_THEME) {
              mModel.startLoader(true, mWorkspace.getCurrentPage(), themeId, inAppTheme);
          } else {
              mModel.startLoader(true, mWorkspace.getCurrentPage(), null, inAppTheme);
          }
      }
  }

  if (!mModel.isAllAppsLoaded()) {
      ViewGroup appsCustomizeContentParent = (ViewGroup) mAppsCustomizeContent.getParent();
      mInflater.inflate(R.layout.apps_customize_progressbar, appsCustomizeContentParent);
  }

  // For handling default keys
  mDefaultKeySsb = new SpannableStringBuilder();
  Selection.setSelection(mDefaultKeySsb, 0);

  IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
  registerReceiver(mCloseSystemDialogsReceiver, filter);

  if (mRestoring && themeId != null && ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_THEME) {
      applyTheme(themeId, false);
  }

  if (themeId != null) {

      if (ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T) {
          mModel.applyKisekaeIcons(this, themeId, false);
      } else if (ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_WIDGET
              || ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T) {
          mAddThemeWdiget = true;
      } else if (ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T
              || ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_WP) {
          mApplyThemeWallpaper = true;
      }
  }

  Utilities.setLegacyOverflow(getWindow(), true);
    }

    private void onCreateAfterPermissionGranted() {
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onCreateAfterPermissionGranted()_");
        // APP Measurement
        TrackingHelperstartDone = true;
//        TrackingHelper.start(this, "", SPPUtility.isDebugFlag);
    }


    private boolean mAddThemeWdiget = false;
    private boolean mApplyThemeWallpaper = false;
    private boolean mApplyTheme = false;
    private String mThemeIdForApplying = null;
    private boolean mInAppThemeForApplying = false;

    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        sPausedFromUserAction = true;
    }

    private void checkForLocaleChange() {
        if (sLocaleConfiguration == null) {
            new AsyncTask<Void, Void, LocaleConfiguration>() {
                @Override
                protected LocaleConfiguration doInBackground(Void... unused) {
                    LocaleConfiguration localeConfiguration = new LocaleConfiguration();
                    readConfiguration(Launcher.this, localeConfiguration);
                    return localeConfiguration;
                }

                @Override
                protected void onPostExecute(LocaleConfiguration result) {
                    sLocaleConfiguration = result;
                    checkForLocaleChange();  // recursive, but now with a locale configuration
                }
            }.execute();
            return;
        }

        final Configuration configuration = getResources().getConfiguration();

        final String previousLocale = sLocaleConfiguration.locale;
        final String locale = configuration.locale.toString();

        final int previousMcc = sLocaleConfiguration.mcc;
        final int mcc = configuration.mcc;

        final int previousMnc = sLocaleConfiguration.mnc;
        final int mnc = configuration.mnc;

        boolean localeChanged = !locale.equals(previousLocale) || mcc != previousMcc || mnc != previousMnc;

        if (localeChanged) {
            sLocaleConfiguration.locale = locale;
            sLocaleConfiguration.mcc = mcc;
            sLocaleConfiguration.mnc = mnc;

            mIconCache.flush();

            final LocaleConfiguration localeConfiguration = sLocaleConfiguration;
            new Thread("WriteLocaleConfiguration") {
                @Override
                public void run() {
                    writeConfiguration(Launcher.this, localeConfiguration);
                }
            }.start();
        }
    }

    private static class LocaleConfiguration {
        public String locale;
        public int mcc = -1;
        public int mnc = -1;
    }

    private static void readConfiguration(Context context, LocaleConfiguration configuration) {
        DataInputStream in = null;
        try {
            in = new DataInputStream(context.openFileInput(PREFERENCES));
            configuration.locale = in.readUTF();
            configuration.mcc = in.readInt();
            configuration.mnc = in.readInt();
        } catch (FileNotFoundException e) {
            // Ignore
        } catch (IOException e) {
            // Ignore
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private static void writeConfiguration(Context context, LocaleConfiguration configuration) {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(context.openFileOutput(PREFERENCES, MODE_PRIVATE));
            out.writeUTF(configuration.locale);
            out.writeInt(configuration.mcc);
            out.writeInt(configuration.mnc);
            out.flush();
        } catch (FileNotFoundException e) {
            // Ignore
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            context.getFileStreamPath(PREFERENCES).delete();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public DragLayer getDragLayer() {
        return mDragLayer;
    }

    boolean isDraggingEnabled() {
        // We prevent dragging when we are loading the workspace as it is possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        return !mModel.isLoadingWorkspace();
    }

    /**
     * Returns whether we should delay spring loaded mode -- for shortcuts and widgets that have
     * a configuration step, this allows the proper animations to run after other transitions.
     */
    private boolean completeAdd(PendingAddArguments args) {
        boolean result = false;
        switch (args.requestCode) {
            case REQUEST_PICK_APPLICATION:
                completeAddApplication(args.intent, args.container, args.screen, args.cellX,
                        args.cellY);
                break;
            case REQUEST_PICK_SHORTCUT:
                processShortcut(args.intent);
                break;
            case REQUEST_CREATE_SHORTCUT:
                completeAddShortcut(args.intent, args.container, args.screen, args.cellX,
                        args.cellY);
                result = true;
                break;
            case REQUEST_CREATE_APPWIDGET:
                int appWidgetId = args.intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                completeAddAppWidget(appWidgetId, args.container, args.screen, null, null);
                result = true;
                break;
            case REQUEST_PICK_WALLPAPER:
                // We just wanted the activity result here so we can clear mWaitingForResult
                break;
        }
        // Before adding this resetAddInfo(), after a shortcut was added to a workspace screen,
        // if you turned the screen off and then back while in All Apps, Launcher would not
        // return to the workspace. Clearing mAddInfo.container here fixes this issue
        resetAddInfo();
        return result;
    }

    private void disableThemeWallpaperIfNecessary() {

        if (!mIsThemeMode) return;

        // 壁紙が変更されていればテーマモードをOFFにする
        if (mWpChangeDetector.isReady()
                && mWpChangeDetector.hasWallpaperChanged(this, R.drawable.default_wallpaper)) {
            mIsThemeMode = false;
            KisekaeLauncherModel.disableThemeInDatabase(this);
            synchronized (KisekaeLauncherModel.sBgLock) {
                KisekaeLauncherModel.sThemeMode = 0;
            }
            updateWallpaperVisibility(true);
            mWorkspace.disableThemeMode();
            mWorkspace.invalidate();

            mWpChangeDetector.turnOff();
        }
    }

    @Override
    protected void onActivityResult(
            final int requestCode, final int resultCode, final Intent data) {

        mWaitingForResult = false;

        DebugLog.instance.outputLog(TAG, "Request Code: " + String.valueOf(requestCode));
        DebugLog.instance.outputLog(TAG, "Result Code: " + String.valueOf(resultCode));

        if (requestCode == REQUEST_PICK_WALLPAPER) {
            disableThemeWallpaperIfNecessary();
            return;
        }

        if (requestCode == REQUEST_PICK_APPWIDGET_ICS
                || requestCode == REQUEST_CREATE_APPWIDGET_ICS) {

            if (requestCode == REQUEST_PICK_APPWIDGET_ICS) {
                mOpenWidgetPicker = false;
            }

            if (resultCode == RESULT_OK ) {

                if (requestCode == REQUEST_PICK_APPWIDGET_ICS) {

                    if (mFromDummy) {
                        if (mPendingDummyView != null) {
                            DummyInfo dummyInfo = (DummyInfo) mPendingDummyView.getTag();
                            LauncherModel.deleteItemFromDatabase(this, dummyInfo);
                            ((HomeGridLayout) mPendingDummyView.getParent().getParent()).removeViewInLayout(mPendingDummyView);
                            mPendingDummyView = null;

                            Bundle extras = data.getExtras();
                            int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                            AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
                            int[] span = getSpanForWidget(Launcher.this, appWidgetInfo);
                            int[] minSpan = getMinSpanForWidget(Launcher.this, appWidgetInfo);
                            dummyInfo.spanX = span[0];
                            dummyInfo.spanY = span[1];
                            dummyInfo.minSpanX = minSpan[0];
                            dummyInfo.minSpanY = minSpan[1];

                            mPendingAddInfo.cellX = dummyInfo.cellX;
                            mPendingAddInfo.cellY = dummyInfo.cellY;
                            mPendingAddInfo.spanX = dummyInfo.spanX;
                            mPendingAddInfo.spanY = dummyInfo.spanY;
                            mPendingAddInfo.minSpanX = dummyInfo.minSpanX;
                            mPendingAddInfo.minSpanY = dummyInfo.minSpanY;
                        }
                        mFromDummy = false;
                    }

                    configureWidget(data);
                } else {
                    showWorkspace(true);
                    int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                    completeAddAppWidget(appWidgetId, LauncherSettings.Favorites.CONTAINER_DESKTOP,
                            mWorkspace.getCurrentPage(), null, null);
                }

            } else if (resultCode == RESULT_CANCELED) {

                if (requestCode == REQUEST_PICK_APPWIDGET_ICS) {
                    mFromDummy = false;
                    mPendingDummyView = null;
                }

                if (data != null) {
                    int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                    if (appWidgetId != -1) {
                        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                    }
                }
            }

            return;
        }

        int pendingAddWidgetId = mPendingAddWidgetId;
        mPendingAddWidgetId = -1;

        if (requestCode == REQUEST_BIND_APPWIDGET) {
            int appWidgetId = data != null ?
            data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (appWidgetId < 0) {
                // Intentでは取得できない端末が存在
                appWidgetId = pendingAddWidgetId;
            }

            if (resultCode == RESULT_CANCELED) {

                mFromDummy = false;
                mPendingDummyView = null;

                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId);

                addPendingAppWidget();
            } else if (resultCode == RESULT_OK) {

                if (mFromDummy) {
                    if (mPendingDummyView != null) {
                        DummyInfo dummyInfo = (DummyInfo) mPendingDummyView.getTag();
                        LauncherModel.deleteItemFromDatabase(this, dummyInfo);
                        ((HomeGridLayout) mPendingDummyView.getParent().getParent()).removeViewInLayout(mPendingDummyView);
                        mPendingDummyView = null;
                        mPendingAddInfo.cellX = dummyInfo.cellX;
                        mPendingAddInfo.cellY = dummyInfo.cellY;
                        mPendingAddInfo.spanX = dummyInfo.spanX;
                        mPendingAddInfo.spanY = dummyInfo.spanY;
                    }
                    mFromDummy = false;
                }

                addAppWidgetImpl(appWidgetId, mPendingAddInfo, null, mPendingAddWidgetInfo);
            }
            return;
        }
        boolean delayExitSpringLoadedMode = false;
        boolean isWidgetDrop = (requestCode == REQUEST_PICK_APPWIDGET ||
                requestCode == REQUEST_CREATE_APPWIDGET);

        // We have special handling for widgets
        if (isWidgetDrop) {
            final int appWidgetId;
            int widgetId = data != null ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                    : -1;
            if (widgetId < 0) {
                appWidgetId = pendingAddWidgetId;
            } else {
                appWidgetId = widgetId;
            }

            if (appWidgetId < 0) {
                Log.e(TAG, "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not returned from the \\" +
                        "widget configuration activity.");
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId);
            } else {
                completeTwoStageWidgetDrop(resultCode, appWidgetId);
            }
            return;
        }

        // The pattern used here is that a user PICKs a specific application,
        // which, depending on the target, might need to CREATE the actual target.

        // For example, the user would PICK_SHORTCUT for "Music playlist", and we
        // launch over to the Music app to actually CREATE_SHORTCUT.
        if (resultCode == RESULT_OK && mPendingAddInfo.container != ItemInfo.NO_ID) {
            final PendingAddArguments args = new PendingAddArguments();
            args.requestCode = requestCode;
            args.intent = data;
            args.container = mPendingAddInfo.container;
            args.screen = mPendingAddInfo.screen;
            args.cellX = mPendingAddInfo.cellX;
            args.cellY = mPendingAddInfo.cellY;
            if (isWorkspaceLocked()) {
                sPendingAddList.add(args);
            } else {
                delayExitSpringLoadedMode = completeAdd(args);
            }
        }
        mDragLayer.clearAnimatedView();
        // Exit spring loaded mode if necessary after cancelling the configuration of a widget
        exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED), delayExitSpringLoadedMode,
                null);
    }

    private void completeTwoStageWidgetDrop(final int resultCode, final int appWidgetId) {
        CellLayout cellLayout =
                (CellLayout) mWorkspace.getChildAt(mPendingAddInfo.screen);
        Runnable onCompleteRunnable = null;
        int animationType = 0;

        AppWidgetHostView boundWidget = null;
        if (resultCode == RESULT_OK) {
            animationType = Workspace.COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION;
            final AppWidgetHostView layout = mAppWidgetHost.createView(this, appWidgetId,
                    mPendingAddWidgetInfo);
            boundWidget = layout;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    completeAddAppWidget(appWidgetId, mPendingAddInfo.container,
                            mPendingAddInfo.screen, layout, null);
                    exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED), false,
                            null);
                }
            };
        } else if (resultCode == RESULT_CANCELED) {
            mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            animationType = Workspace.CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED), false,
                            null);
                }
            };
        }
        if (mDragLayer.getAnimatedView() != null) {
            mWorkspace.animateWidgetDrop(mPendingAddInfo, cellLayout,
                    (DragView) mDragLayer.getAnimatedView(), onCompleteRunnable,
                    animationType, boundWidget, true);
        } else {
            // The animated view may be null in the case of a rotation during widget configuration
            onCompleteRunnable.run();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirstFrameAnimatorHelper.setIsVisible(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirstFrameAnimatorHelper.setIsVisible(true);
    }

    @Override
    protected void onResume() {
         jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onResume()_");
        long startTime = 0;
        if (DEBUG_RESUME_TIME) {
            startTime = System.currentTimeMillis();
        }
        super.onResume();
        Config.collectLifecycleData();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                disableThemeWallpaperIfNecessary();
            }
        }, 100);

        // Restore the previous launcher state
        if (mOnResumeState == State.WORKSPACE) {
            if (mIsThemeMode) {
                updateWallpaperVisibility(false);
            }
            showWorkspace(false);
        } else if (mOnResumeState == State.APPS_CUSTOMIZE) {
            updateWallpaperVisibility(false);
            showAllApps(false);
        } else if (mOnResumeState == State.PAGE_EDIT) {
            updateWallpaperVisibility(false);
            showPageEdit(false);
        }
        mOnResumeState = State.NONE;

        mWorkspace.updateDummyItems();

        // Process any items that were added while Launcher was away
        InstallShortcutReceiver.flushInstallQueue(this);

        mPaused = false;
        sPausedFromUserAction = false;
        if (mRestoring || mOnResumeNeedsLoad) {
            mWorkspaceLoading = true;
            mModel.startLoader(true, -1);
            mRestoring = false;
            mOnResumeNeedsLoad = false;
        }

        if (mOnResumeCallbacks.size() > 0) {
            // We might have postponed some bind calls until onResume (see waitUntilResume) --
            // execute them here
            long startTimeCallbacks = 0;
            if (DEBUG_RESUME_TIME) {
                startTimeCallbacks = System.currentTimeMillis();
            }

            if (mAppsCustomizeContent != null) {
                mAppsCustomizeContent.setBulkBind(true);
            }
            for (int i = 0; i < mOnResumeCallbacks.size(); i++) {
                mOnResumeCallbacks.get(i).run();
            }
            if (mAppsCustomizeContent != null) {
                mAppsCustomizeContent.setBulkBind(false);
            }
            mOnResumeCallbacks.clear();
            if (DEBUG_RESUME_TIME) {
                DebugLog.instance.outputLog(TAG, "Time spent processing callbacks in onResume: " +
                    (System.currentTimeMillis() - startTimeCallbacks));
            }
        }

        // Reset the pressed state of icons that were locked in the press state while activities
        // were launching
        if (mWaitingForResume != null) {
            // Resets the previous workspace icon press state
            mWaitingForResume.setStayPressed(false);
        }
        if (mAppsCustomizeContent != null) {
            // Resets the previous all apps icon press state
            mAppsCustomizeContent.resetDrawableState();
        }

        if (DEBUG_RESUME_TIME) {
            DebugLog.instance.outputLog(TAG, "Time spent in onResume: " + (System.currentTimeMillis() - startTime));
        }

        if (Build.VERSION.SDK_INT >= 23) {//OSバージョン//Oncreateでパーミッションダイアログ表示中にHome押したとき用
            //パーミッションチェック
                PermissionCheckNG = false;
            for (int i = 0; i < Permissions.length; i++) {

                if (PermissionChecker.checkSelfPermission(this
                        , Permissions[i])
                != PermissionChecker.PERMISSION_GRANTED) {
                    jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onResume_checkSelfPermission_" + Permissions[i] + "_!= PERMISSION_GRANTED");
                    PermissionCheckNG = true;
//					onCreateView();//チェックに行っても落ちない対応
                    // パーミッションをリクエストする
                    int MY_PERMISSION_REQUEST = 0;
                    if (Permissions[i].equals(Permissions[0])) {
                        MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_READ_PHONE_STATE;
                    } else if (Permissions[i].equals(Permissions[1])) {
                        MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_CALL_PHONE;
                    }
//      				else if (Permissions[i].equals(Permissions[2])) {//Pending中20160419
//						MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_GET_ACCOUNTS;
//					}

                    //パーミッションリクエスト
                    final int index = i;
                    final int PERMISSION_REQUEST = MY_PERMISSION_REQUEST;
                    runnnableForPermission = new Runnable() {

                        @Override public void run() {
                            ActivityCompat.requestPermissions(Launcher.this,
                                    new String[] { Permissions[index] },
                                    PERMISSION_REQUEST);
                        }
                    };
                    handler.post(runnnableForPermission);
                    break;

//					ActivityCompat.requestPermissions(this,
//							new String[] { Permissions[i] },
//							MY_PERMISSION_REQUEST);
//					break;

                } else {
                    jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onResume_checkSelfPermission_" + Permissions[i] + "_== PERMISSION_GRANTED");
//					if(!TrackingHelperstartDone)onCreateAfterPermissionGranted();
                }

//				if (!PermissionCheckNG) {
//					if(!TrackingHelperstartDone)onCreateAfterPermissionGranted();
//				}
            }//for (int i = 0; i < Permissions.length; i++) {
            if (!PermissionCheckNG) {
                if(!TrackingHelperstartDone)onCreateAfterPermissionGranted();
            }

//    		}//if (PermissionCheckNG) {//Oncreateで実行中

        } else {//OSバージョン
        }
    }

    @Override
    protected void onPause() {

        if (mWidgetPickerDialog != null) {
            mWidgetPickerDialog.dismiss();
            mWidgetPickerDialog = null;
        }

        if (mSppFailedAlertDialog != null) {
            mSppFailedAlertDialog.dismiss();
            mSppFailedAlertDialog = null;
        }

        // NOTE: We want all transitions from launcher to act as if the wallpaper were enabled
        // to be consistent.  So re-enable the flag here, and we will re-disable it as necessary
        // when Launcher resumes and we are still in AllApps.
        updateWallpaperVisibility(true);

        super.onPause();
        Config.pauseCollectingLifecycleData();

        mPaused = true;
        mDragController.cancelDrag();
        mDragController.resetLastGestureUpTime();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        // Flag the loader to stop early before switching
        mModel.stopLoader();
        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.surrender();
        }
        return Boolean.TRUE;
    }

    // We can't hide the IME if it was forced open.  So don't bother
    /*
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            final InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            inputManager.hideSoftInputFromWindow(lp.token, 0, new android.os.ResultReceiver(new
                        android.os.Handler()) {
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            Log.d(TAG, "ResultReceiver got resultCode=" + resultCode);
                        }
                    });
            Log.d(TAG, "called hideSoftInputFromWindow from onWindowFocusChanged");
        }
    }
    */

    private boolean acceptFilter() {
        final InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        return !inputManager.isFullscreenMode();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        final int uniChar = event.getUnicodeChar();
        final boolean handled = super.onKeyDown(keyCode, event);
        final boolean isKeyNotWhitespace = uniChar > 0 && !Character.isWhitespace(uniChar);
        if (!handled && acceptFilter() && isKeyNotWhitespace) {
            boolean gotKey = TextKeyListener.getInstance().onKeyDown(mWorkspace, mDefaultKeySsb,
                    keyCode, event);
            if (gotKey && mDefaultKeySsb != null && mDefaultKeySsb.length() > 0) {
                // something usable has been typed - start a search
                // the typed text will be retrieved and cleared by
                // showSearchDialog()
                // If there are multiple keystrokes before the search dialog takes focus,
                // onSearchRequested() will be called for every keystroke,
                // but it is idempotent, so it's fine.
                return onSearchRequested();
            }
        }

        // Eat the long press event so the keyboard doesn't come up.
        if (keyCode == KeyEvent.KEYCODE_MENU && event.isLongPress()) {
            return true;
        }

        return handled;
    }


    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MENU ) {
            if (!isAllAppsVisible() && !isPageEditVisible()) {
                showHomeMenu();
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    private void clearTypedText() {
        mDefaultKeySsb.clear();
        mDefaultKeySsb.clearSpans();
        Selection.setSelection(mDefaultKeySsb, 0);
    }

    /**
     * Given the integer (ordinal) value of a State enum instance, convert it to a variable of type
     * State
     */
    private static State intToState(int stateOrdinal) {
        State state = State.WORKSPACE;
        final State[] stateValues = State.values();
        for (int i = 0; i < stateValues.length; i++) {
            if (stateValues[i].ordinal() == stateOrdinal) {
                state = stateValues[i];
                break;
            }
        }
        return state;
    }

    /**
     * Restores the previous state, if it exists.
     *
     * @param savedState The previous state.
     */
    private void restoreState(Bundle savedState) {

        State state = intToState(savedState.getInt(RUNTIME_STATE, State.WORKSPACE.ordinal()));
        if (state == State.APPS_CUSTOMIZE) {
            mOnResumeState = State.APPS_CUSTOMIZE;
        }

        int currentScreen = savedState.getInt(RUNTIME_STATE_CURRENT_SCREEN, -1);
        if (currentScreen > -1) {
            mWorkspace.setCurrentPage(currentScreen);
        }

        final long pendingAddContainer = savedState.getLong(RUNTIME_STATE_PENDING_ADD_CONTAINER, -1);
        final int pendingAddScreen = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SCREEN, -1);

        if (pendingAddContainer != ItemInfo.NO_ID && pendingAddScreen > -1) {
            mPendingAddInfo.container = pendingAddContainer;
            mPendingAddInfo.screen = pendingAddScreen;
            mPendingAddInfo.cellX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_X);
            mPendingAddInfo.cellY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_Y);
            mPendingAddInfo.spanX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_X);
            mPendingAddInfo.spanY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y);
            mPendingAddWidgetInfo = savedState.getParcelable(RUNTIME_STATE_PENDING_ADD_WIDGET_INFO);
            mPendingAddWidgetId = savedState.getInt(RUNTIME_STATE_PENDING_ADD_WIDGET_ID);
            mWaitingForResult = true;
            mRestoring = true;
        }

        boolean renameFolder = savedState.getBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, false);
        if (renameFolder) {
            long id = savedState.getLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID);
            mFolderInfo = mModel.getFolderById(this, sFolders, id);
            mRestoring = true;
        }

        // Restore the AppsCustomize tab
        if (mAppsCustomizeTabHost != null) {
            String curTab = savedState.getString("apps_customize_currentTab");
            if (curTab != null) {
                mAppsCustomizeTabHost.setContentTypeImmediate(
                        mAppsCustomizeTabHost.getContentTypeForTabTag(curTab));
                mAppsCustomizeContent.loadAssociatedPages(
                        mAppsCustomizeContent.getCurrentPage());
            }

            int currentIndex = savedState.getInt("apps_customize_currentIndex");
            mAppsCustomizeContent.restorePageForIndex(currentIndex);
        }

        mWpChangeDetector.restoreState(savedState);
    }

    /**
     * Finds all the views we need and configure them properly.
     */
    private void setupViews() {
        final DragController dragController = mDragController;

        mLauncherView = findViewById(R.id.launcher);
        mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
        mWorkspace = (Workspace) mDragLayer.findViewById(R.id.workspace);

        mLauncherView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // Setup the drag layer
        mDragLayer.setup(this, dragController);

        // Setup the hotseat
        mHotseat = (Hotseat) findViewById(R.id.hotseat);
        if (mHotseat != null) {
            mHotseat.setup(this);
        }

        // Setup the workspace
        mWorkspace.setHapticFeedbackEnabled(false);
        mWorkspace.setOnLongClickListener(this);
        mWorkspace.setup(dragController);
        PageIndicatorView pageIndicator = (PageIndicatorView) findViewById(R.id.page_indicator);
        FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) pageIndicator.getLayoutParams();
        // ページインディケータの高さを調整
        flp.setMargins(0, 0, 0, mHotseat.getContentHeight()
                + pageIndicator.getContentHeight() + (int) (LauncherApplication.getScreenDensity() * 8));
        mWorkspace.setPageIndicator(pageIndicator);
        pageIndicator.updateNumPages(1);
        pageIndicator.updateCurrentPage(mWorkspace.getCurrentPage());
        dragController.addDragListener(mWorkspace);

        // Get the search/delete bar
        mSearchDropTargetBar = (SearchDropTargetBar) mDragLayer.findViewById(R.id.qsb_bar);

        // Setup AppsCustomize
        mAppsCustomizeTabHost = (AppsCustomizeTabHost) findViewById(R.id.apps_customize_pane);
        mAppsCustomizeTabHost.setup(this);
        mAppsCustomizeContent = (AppsCustomizePagedView)
                mAppsCustomizeTabHost.findViewById(R.id.apps_customize_pane_content);
        mAppsCustomizeContent.setup(this, dragController);

        mPageEditScreen = (PageEditScreen) findViewById(R.id.page_edit_screen);
        mPageEditScreen.setup(this, mDragController);

        // Setup the drag controller (drop targets have to be added in reverse order in priority)
        dragController.setDragScoller(mWorkspace);
        dragController.setScrollView(mDragLayer);
        dragController.setMoveTarget(mWorkspace);
        dragController.addDropTarget(mWorkspace);
        dragController.addDropTarget(mPageEditScreen);
        if (mSearchDropTargetBar != null) {
            mSearchDropTargetBar.setup(this, dragController);
        }

        mPopupLayerView = (PopupLayerView) findViewById(R.id.popup_layer);
        mHomeMenuView = (HomeMenuView) LayoutInflater.from(this).inflate(
                R.layout.home_menu, mPopupLayerView, false);
        mHomeMenuView.setOnMenuItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tag = (String) v.getTag();

                if (HomeMenuView.KEY_BUTTON_KISEKAE.equals(tag)) {

                    Intent intent = new Intent(Launcher.this, SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

//                    startActivity(intent);
                    startActivitySafely(intent);

                } else if (HomeMenuView.KEY_BUTTON_WALLPAPER.equals(tag)) {
                    startWallpaper();
                } else if (HomeMenuView.KEY_BUTTON_WIDGET.equals(tag)) {

                    // ウィジェットショートカット
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        // Android 4.0では「ピッカーを表示する
                        startAppWidgetPick(false, null);
                    } else {
                        showAllApps(true);
                        mAppsCustomizeTabHost.selectWidgetsTab();
                    }

                } else if (HomeSubMenuView.TAG_SETTINGS_SYSTEM.equals(tag)) {
                    showSystemSettingActivity();
                } else if (HomeSubMenuView.TAG_SETTINGS_PANEL.equals(tag)) {
                    showPageEdit(true);
                }
            }
        });

        mHomeMenuView.setOnHistoryItemClickListener(new HomeMenuView.OnHistoryItemSelectListener() {
            @Override
            public void onItemSelect(String themeId) {

                if (mThemeId != null && mThemeId.equals(themeId)) {
                    if (!mIsThemeMode) {
                        // 個別壁紙に戻す
                        mModel.enableThemeMode(Launcher.this, mThemeId, false, mWorkspace.getPageCount());
                        return;
                    }
                }

                mHistoryThemeId = themeId;
                ContentsOperator.op.callSetSkinTask(Launcher.this, Long.parseLong(themeId));
            }
        });

        mHomeMenuView.setOnKisekaeIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Launcher.this, SplashActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivitySafely(intent);
            }
        });

        if (mHomeMenuPopup == null) {
            mHomeMenuPopup = mHomeMenuView.makePopup(mPopupLayerView);
        }
    }

    private String mHistoryThemeId;

    @Override
    public void onFailedDownloadSkin(int reason, long assetId) {
        Toast.makeText(this, R.string.err_msg_load_theme, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFinishedDownloadSkin(ContentsDataDto settedCto, long delAssetIdForHistory, long delAssetIdForMypage) {

        if (mHistoryThemeId != null) {
            final String themeId = mHistoryThemeId;
            applyTheme(themeId, false);
        }
    }

    private void createDummyItem(DummyInfo dummyInfo) {

        DummyHomeItemView dummy = new DummyHomeItemView(this);
        dummy.applyFromDummyInfo(dummyInfo);
        dummy.setOnClickListener(mDummyOnClickListener);

        dummy.updateState();

        LauncherModel.addItemToDatabase(this, dummyInfo,
                LauncherSettings.Favorites.CONTAINER_DESKTOP, dummyInfo.screen, dummyInfo.cellX, dummyInfo.cellY, false);

        mWorkspace.addInScreen(dummy, dummyInfo.container, dummyInfo.screen,
                dummyInfo.cellX, dummyInfo.cellY, dummyInfo.spanX, dummyInfo.spanY);
    }

    private final View.OnClickListener mDummyOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {

            DummyInfo info = (DummyInfo) v.getTag();
            if (info == null) return;

            int oldDummyState = info.dummyState;
            ((DummyHomeItemView) v).updateState();
            if (oldDummyState != info.dummyState) return;

            if (info.dummyState == DummyInfo.DUMMY_TYPE_DL) {
                // ディズニーマーケットに遷移

                String assetId = ThemeUtils.getAssetIdFromPackageName(info.widgetPackageName);
                if (assetId == null) return;

                try {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("dmarket://moveappinfo/?assetID=" + assetId));
                    startActivity(intent);

                } catch (ActivityNotFoundException ex) {

                    if(SPPUtility.isDocomoDevice(getApplicationContext())){//docomo端末なら

                        if(info.widgetPackageName.equals(ThemeUtils.WIDGET_PACKAGE_NAME_CALENDAR)){
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://ss10apps.disney.co.jp/app/?material_id=3"));
                            startActivitySafely(intent);
                            return;
                        }else if(info.widgetPackageName.equals(ThemeUtils.WIDGET_PACKAGE_NAME_WEATHER)){
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://ss10apps.disney.co.jp/app/?material_id=10"));
                            startActivitySafely(intent);
                            return;
                        }

                    }else{//docomo以外なら
                        DebugLog.instance.outputLog("DKWid", "catch＿try {_else_docomo_" );
                        if(!SPPUtility.isAppInstalled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")){
                            DebugLog.instance.outputLog("DKWid", "catch＿try {_else_docomo_if(!SPPUtility.isAppInstalled(getApplicationContext(), jp.co.disney.apps.base.disneymarketapp)){" );

                            if(SPPUtility.getCarrierID(getApplicationContext()).equals(SplashActivity.AUTH_CARRIER_AU)){
                                //auマーケットが入っていたら
                                if(SPPUtility.isAppInstalled(getApplicationContext(), "com.kddi.market") && SPPUtility.isAppEnabled(getApplicationContext(), "com.kddi.market")){
                                    //
                                    Toast.makeText(getApplicationContext(), "Disney passをインストールして下さい。", Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(Intent.ACTION_VIEW,
                                            Uri.parse("auonemkt://details?id=8588000000001"));
                                    startActivitySafely(intent);
//                                    finish();
                                    return;

                                }
                            }

                            //ダウンロード開始用Activity
                            Intent intent = new Intent();
                            intent.setClassName(getPackageName(),"jp.co.disney.apps.managed.kisekaeapp.spp.BaseAppDownloadActivity");
                            startActivitySafely(intent);
//                            finish();
                            return;
                            //ここで終了------------------------------------------------------------------------------------
                        }else if(!SPPUtility.isAppEnabled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")){
                            DebugLog.instance.outputLog("DKWid", "catch＿try {_else_docomo_eles_if(!SPPUtility.isAppEnabled(getApplicationContext(), jp.co.disney.apps.base.disneymarketapp)){" );
                            displaySppFailedAlertDialog(SplashActivity.SPP_FAILED_REASON_BASE_APP_INVALID);
                            return;
                        }
                        else{//tes
                            DebugLog.instance.outputLog("DKWid", "catch＿try {_else_docomo_eles_if(!SPPUtility.isAppInstalled(getApplicationContext(), jp.co.disney.apps.base.disneymarketapp)){" );
                        }
                    }

                }

            } else if (info.dummyState == DummyInfo.DUMMY_TYPE_PICKER) {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    startAppWidgetPick(true, v);
                } else {

                    if (info.inAppWidget) {
                        // アプリ内ウィジェット配置
                        addAppWidgetFromDummy((DummyHomeItemView) v, null, null, false);

                    } else {

                        if (mWidgetPickerDialog != null) return;

                        final ArrayList<AppWidgetProviderInfo> providerList =
                                LauncherModel.findWidgetByPackageName(Launcher.this, info.widgetPackageName);
                        if (providerList.size() == 0) return;

                        final ArrayList<AppWidgetProviderInfo> filteredProviderList = new  ArrayList<AppWidgetProviderInfo>();
                        for (AppWidgetProviderInfo providerInfo : providerList) {
                            int[] spanXY = getSpanForWidget(Launcher.this, providerInfo);
                            int[] minSpanXY = getMinSpanForWidget(Launcher.this, providerInfo);
                            int minSpanX = Math.min(spanXY[0], minSpanXY[0]);
                            int minSpanY = Math.min(spanXY[1], minSpanXY[1]);
                            if (minSpanX <= LauncherModel.getCellCountX() &&
                                minSpanY <= LauncherModel.getCellCountY()) {
                                filteredProviderList.add(providerInfo);
                            }
                        }

                        final int count = filteredProviderList.size();
                        final String[] items = new String[count];

                        final PackageManager pm = getPackageManager();

                        for (int i = 0; i < count; i++) {

                            AppWidgetProviderInfo providerInfo = filteredProviderList.get(i);

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                                items[i] = providerInfo.label;
                            } else {
                                items[i] = providerInfo.loadLabel(pm);
                            }
                        }

                        Drawable icon = null;
                        try {
                            icon = Launcher.this.getPackageManager().getApplicationIcon(filteredProviderList.get(0).provider.getPackageName());
                        } catch (NameNotFoundException e) {
                        }

                        ArrayList<DummyHomeItemView.Item> itemList = new ArrayList<DummyHomeItemView.Item>();
                        for (int i = 0; i < count; i++) {
                            DummyHomeItemView.Item item = new DummyHomeItemView.Item();
                            item.setImagaData(icon);
                            item.setStringItem(items[i]);
                            itemList.add(item);
                        }

                        CustomAdapter adapter = new CustomAdapter(Launcher.this, 0, itemList);

                        ListView listView = new ListView(Launcher.this);
                        listView.setAdapter(adapter);
                        listView.setOnItemClickListener(new OnItemClickListener() {
                            public void onItemClick(AdapterView<?> items,
                                    View view, int position, long id) {

                                if (mWidgetPickerDialog != null) {
                                    mWidgetPickerDialog.dismiss();
                                    mWidgetPickerDialog = null;
                                }

                                AppWidgetProviderInfo providerInfo = filteredProviderList.get(position);

                                int[] span = getSpanForWidget(Launcher.this, providerInfo);
                                int[] minSpan = getMinSpanForWidget(Launcher.this, providerInfo);

                                DummyInfo dummyInfo = (DummyInfo) v.getTag();
                                dummyInfo.spanX = span[0];
                                dummyInfo.spanY = span[1];
                                dummyInfo.minSpanX = minSpan[0];
                                dummyInfo.minSpanY = minSpan[1];

                                addAppWidgetFromDummy((DummyHomeItemView) v, null,
                                        providerInfo.provider.getClassName(), false);
                            }
                        });

                        AlertDialog.Builder listDlg = new AlertDialog.Builder(new ContextThemeWrapper(Launcher.this, R.style.MyDialogTheme));
                        TextView title =  new TextView(Launcher.this);
                        title.setText(R.string.title_widget_picker);
                        title.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
                        title.setTextSize(22);
                        title.setBackgroundColor(Color.argb(0, 0,0, 0));
                        title.setTextColor(Color.WHITE);
                        DisplayMetrics metrics = getResources().getDisplayMetrics();
                        title.setPadding((int) (metrics.density * 16), (int) (metrics.density * 18), (int) (metrics.density * 16), (int) (metrics.density * 14));
                        listDlg.setCustomTitle(title);

                        listDlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                if (mWidgetPickerDialog != null) {
                                    mWidgetPickerDialog.dismiss();
                                    mWidgetPickerDialog = null;
                                }
                            }
                        });

                        mWidgetPickerDialog = listDlg.setView(listView).create();
                        mWidgetPickerDialog.show();
                        int dividerId = mWidgetPickerDialog.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
                        View divider = mWidgetPickerDialog.findViewById(dividerId);
                        divider.setBackgroundColor(Color.argb(0, 0, 0, 0));
                    }
                }
            }
        }
    };

    //エラーダイアログ表示
    private void displaySppFailedAlertDialog(int reason){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (reason) {
        default:
        case SplashActivity.SPP_FAILED_REASON_AUTH_ERROR:
            builder.setMessage(R.string.invalid_app_fig2);
            break;
        case SplashActivity.SPP_FAILED_REASON_NETWORK_ERROR:
            builder.setMessage(R.string.invalid_app_fig1);
            break;
        case SplashActivity.SPP_FAILED_REASON_BASE_APP_INVALID:
            builder.setMessage(R.string.invalid_app_fig3);
            break;
        }

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mSppFailedAlertDialog != null) {
                    mSppFailedAlertDialog.dismiss();
                    mSppFailedAlertDialog = null;
                }
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mSppFailedAlertDialog != null) {
                    mSppFailedAlertDialog.dismiss();
                    mSppFailedAlertDialog = null;
                }
            }
        });

        // ダイアログの作成と描画
        mSppFailedAlertDialog = builder.show();
        TextView messageText = (TextView) mSppFailedAlertDialog.findViewById(android.R.id.message);
        messageText.setGravity(Gravity.CENTER);
    }

    private AlertDialog mSppFailedAlertDialog = null;

    private Dialog mWidgetPickerDialog = null;

    @Override
    public void initThemeShortcutIcon(int defaultPage) {

        PackageManager pm = getPackageManager();

        // 着せ替えアプリ
        createShortcutForTheme(this, pm,
                ThemeUtils.PACKAGE_NAME_KISEKAE,
                ThemeUtils.CLASS_NAME_KISEKAE,
                LauncherSettings.Favorites.CONTAINER_DESKTOP, defaultPage, 0, 8);

        // ディズニーマーケット
        createShortcutForTheme(this, pm,
                ThemeUtils.PACKAGE_NAME_DISNEY_MARKET,
                ThemeUtils.CLASS_NAME_DISNEY_MARKET,
                LauncherSettings.Favorites.CONTAINER_DESKTOP, defaultPage, 6, 8);

        String phonePackageName = null;
        String phoneClassName = null;
        String mailPackageName = null;
        String mailClassName = null;
        String browserPackageName = null;
        String browserClassName = null;
        String cameraPackageName = null;
        String cameraClassName = null;

        boolean chromeInstalled = (mModel.findResolveInfo(this,
                ThemeUtils.PACKAGE_NAME_CHROME, ThemeUtils.CLASS_NAME_CHROME) != null);

        HashMap<String, KisekaeInfo> currentKisekaeMap = mIconCache.getCurrentKisekaeMap();
        if (currentKisekaeMap == null) return;

        Iterator<Entry<String, KisekaeInfo>> itr = currentKisekaeMap.entrySet().iterator();
        while(itr.hasNext()) {

            Entry<String, KisekaeInfo> pair = itr.next();
            String iconName = pair.getValue().getIconName();

            if (ThemeUtils.KISEKAE_ICON_NAME_PHONE.equals(iconName)) {
                // 電話
                String[] splited = pair.getKey().split("/");
                phonePackageName = splited[0];
                phoneClassName = splited[1];

            } else if (ThemeUtils.KISEKAE_ICON_NAME_EMAIL.equals(iconName)) {
                // メール
                String[] splited = pair.getKey().split("/");
                mailPackageName = splited[0];
                mailClassName = splited[1];

            } else if (ThemeUtils.KISEKAE_ICON_NAME_CAMERA.equals(iconName)) {
                // カメラ
                String[] splited = pair.getKey().split("/");
                cameraPackageName = splited[0];
                cameraClassName = splited[1];

            } else if (!chromeInstalled && ThemeUtils.KISEKAE_ICON_NAME_BROWSER.equals(iconName)) {
                // ブラウザ
                String[] splited = pair.getKey().split("/");
                browserPackageName = splited[0];
                browserClassName = splited[1];
            }
        }

        if (phonePackageName != null && phoneClassName != null) {
            createShortcutForTheme(this, pm,
                    phonePackageName,
                    phoneClassName,
                    LauncherSettings.Favorites.CONTAINER_HOTSEAT, -1, 0, 0);
        }

        if (mailPackageName != null && mailClassName != null) {
            createShortcutForTheme(this, pm,
                    mailPackageName,
                    mailClassName,
                    LauncherSettings.Favorites.CONTAINER_HOTSEAT, -1, 2, 0);
        }

        if (chromeInstalled) {
            createShortcutForTheme(this, pm,
                    ThemeUtils.PACKAGE_NAME_CHROME,
                    ThemeUtils.CLASS_NAME_CHROME,
                    LauncherSettings.Favorites.CONTAINER_HOTSEAT, -1, 6, 0);
        } else {
            if (browserPackageName != null && browserClassName != null) {
                createShortcutForTheme(this, pm,
                        browserPackageName,
                        browserClassName,
                        LauncherSettings.Favorites.CONTAINER_HOTSEAT, -1, 6, 0);
            }
        }

        if (cameraPackageName != null && cameraClassName != null) {
            createShortcutForTheme(this, pm,
                    cameraPackageName,
                    cameraClassName,
                    LauncherSettings.Favorites.CONTAINER_HOTSEAT, -1, 8, 0);
        }
    }

    private boolean createKisekaeShortcut(Context context, PackageManager pm,
            String iconName, int screen, int cellX, int cellY) {

        if (iconName == null) return false;

        HashMap<String, KisekaeInfo> currentKisekaeMap = mIconCache.getCurrentKisekaeMap();
        if (currentKisekaeMap == null) return false;

        boolean created = false;
        Iterator<Entry<String, KisekaeInfo>> itr = currentKisekaeMap.entrySet().iterator();
        while(itr.hasNext()) {

            Entry<String, KisekaeInfo> pair = itr.next();
            String mappedIconName = pair.getValue().getIconName();

            if (iconName.equals(mappedIconName)) {

                String[] splited = pair.getKey().split("/");
                String packageName = splited[0];
                String className = splited[1];

                if (createShortcutForTheme(context, pm,
                        packageName,
                        className,
                        LauncherSettings.Favorites.CONTAINER_DESKTOP, screen, cellX, cellY)) {
                    created = true;
                    break;
                }
            }
        }

        return created;
    }

    private boolean createShortcutForTheme(Context context, PackageManager pm,
            String packageName, String className,
            int container, int screen, int cellX, int cellY) {

        if (packageName == null || className == null) return false;

        ResolveInfo rInfo = mModel.findResolveInfo(context, packageName, className);
        if (rInfo == null) {
            return false;
        }

        ApplicationInfo appInfo = new ApplicationInfo(pm, rInfo, mIconCache, null);
        ShortcutInfo shortcutInfo = new ShortcutInfo(appInfo);

        final CellLayout target = getCellLayout(container, screen);
        mWorkspace.addApplicationShortcut(shortcutInfo, target, container, screen, cellX, cellY,
                isWorkspaceLocked(), cellX + 1, cellY + 1);

        return true;
    }

    /**
     * Creates a view representing a shortcut.
     *
     * @param info The data structure describing the shortcut.
     *
     * @return A View inflated from R.layout.application.
     */
    View createShortcut(ShortcutInfo info) {
        return createShortcut(R.layout.application,
                (ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentPage()), info);
    }

    /**
     * Creates a view representing a shortcut inflated from the specified resource.
     *
     * @param layoutResId The id of the XML layout used to create the shortcut.
     * @param parent The group the shortcut belongs to.
     * @param info The data structure describing the shortcut.
     *
     * @return A View inflated from layoutResId.
     */
    View createShortcut(int layoutResId, ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) mInflater.inflate(layoutResId, parent, false);

        CellLayout cellLayout = (CellLayout) parent;
        favorite.applyFromShortcutInfo(info, mIconCache, cellLayout.getCellWidth() * 2, cellLayout.getIconSize(),
                cellLayout.getCellPaddingTop(), cellLayout.getTextPadding(), LauncherApplication.getScreenDensity());

        favorite.setOnClickListener(this);
        return favorite;
    }

    /**
     * Add an application shortcut to the workspace.
     *
     * @param data The intent describing the application.
     * @param cellInfo The position on screen where to create the shortcut.
     */
    void completeAddApplication(Intent data, long container, int screen, int cellX, int cellY) {
        final int[] cellXY = mTmpAddItemCellCoordinates;
        final CellLayout layout = getCellLayout(container, screen);

        // First we check if we already know the exact location where we want to add this item.
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
        } else if (!layout.findCellForSpan(cellXY, Workspace.ICON_SPAN, Workspace.ICON_SPAN)) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        final ShortcutInfo info = mModel.getShortcutInfo(getPackageManager(), data, this);

        if (info != null) {
            // Necessary flags are added when the activity is launched via
            // LauncherApps
            info.setActivity(data);
            info.container = ItemInfo.NO_ID;
            mWorkspace.addApplicationShortcut(info, layout, container, screen, cellXY[0], cellXY[1],
                    isWorkspaceLocked(), cellX, cellY);
        } else {
            Log.e(TAG, "Couldn't find ActivityInfo for selected application: " + data);
        }
    }

    /**
     * Add a shortcut to the workspace.
     *
     * @param data The intent describing the shortcut.
     * @param cellInfo The position on screen where to create the shortcut.
     */
    private void completeAddShortcut(Intent data, long container, int screen, int cellX,
            int cellY) {
        int[] cellXY = mTmpAddItemCellCoordinates;
        int[] touchXY = mPendingAddInfo.dropPos;
        CellLayout layout = getCellLayout(container, screen);

        boolean foundCellSpan = false;

        ShortcutInfo info = mModel.infoFromShortcutIntent(this, data, null);
        if (info == null) {
            return;
        }
        final View view = createShortcut(info);

        // First we check if we already know the exact location where we want to add this item.
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
            foundCellSpan = true;

            // If appropriate, either create a folder or add to an existing folder
            if (mWorkspace.createUserFolderIfNecessary(view, container, layout, cellXY, 0,
                    true, null,null)) {
                return;
            }
            DragObject dragObject = new DragObject();
            dragObject.dragInfo = info;
            if (mWorkspace.addToExistingFolderIfNecessary(view, layout, cellXY, 0, dragObject,
                    true)) {
                return;
            }
        } else if (touchXY != null) {
            // when dragging and dropping, just find the closest free spot
            int[] result = layout.findNearestVacantArea(touchXY[0], touchXY[1], Workspace.ICON_SPAN, Workspace.ICON_SPAN, cellXY);
            foundCellSpan = (result != null);
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, Workspace.ICON_SPAN, Workspace.ICON_SPAN);
        }

        if (!foundCellSpan) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        LauncherModel.addItemToDatabase(this, info, container, screen, cellXY[0], cellXY[1], false);

        if (!mRestoring) {
            mWorkspace.addInScreen(view, container, screen, cellXY[0], cellXY[1], Workspace.ICON_SPAN, Workspace.ICON_SPAN,
                    isWorkspaceLocked());
        }
    }

    static int[] getSpanForWidget(Context context, ComponentName component, int minWidth,
            int minHeight) {

        int[] retSpan = new int[2];

        String widgetClassName = component.getClassName();

        if (ThemeUtils.isInAppWidget(widgetClassName, retSpan)) {
            return retSpan;
        } else {
            // We want to account for the extra amount of padding that we are adding to the widget
            // to ensure that it gets the full amount of space that it has requested
            Rect padding = AppWidgetHostView.getDefaultPaddingForWidget(context, component, null);
            int requiredWidth = minWidth + padding.left + padding.right;
            int requiredHeight = minHeight + padding.top + padding.bottom;

            retSpan = CellLayout.rectToCell(context.getResources(), requiredWidth, requiredHeight, null);

            if (ThemeUtils.isDisneyLargeWidget(widgetClassName)) {
                int dw = context.getResources().getDisplayMetrics().widthPixels;
                if (dw >= 1080 && retSpan[0] >= 9) {
                    retSpan[0] = 8;
                    retSpan[1] = 7;
                }
            }
//            else if (ThemeUtils.isDisneyLargeWeatherWidget(widgetClassName)) {
//
//                int dw = context.getResources().getDisplayMetrics().widthPixels;
//                if (dw >= 1080 && retSpan[0] >= 9) {
//                    retSpan[0] = 8;
//                    retSpan[1] = 2;
//                }
//            }

            return retSpan;
        }
    }

    static int[] getSpanForWidget(Context context, AppWidgetProviderInfo info) {
        return getSpanForWidget(context, info.provider, info.minWidth, info.minHeight);
    }

    static int[] getMinSpanForWidget(Context context, AppWidgetProviderInfo info) {
        return getSpanForWidget(context, info.provider, info.minResizeWidth, info.minResizeHeight);
    }

    static int[] getSpanForWidget(Context context, PendingAddWidgetInfo info) {
        return getSpanForWidget(context, info.componentName, info.info.minWidth,
                info.info.minHeight);
    }

    static int[] getMinSpanForWidget(Context context, PendingAddWidgetInfo info) {
        return getSpanForWidget(context, info.componentName, info.info.minResizeWidth,
                info.info.minResizeHeight);
    }

    /**
     * Add a widget to the workspace.
     *
     * @param appWidgetId The app widget id
     * @param cellInfo The position on screen where to create the widget.
     */
    private void completeAddAppWidget(final int appWidgetId, long container, int screen,
            AppWidgetHostView hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo == null) {
            appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        }

        // Calculate the grid spans needed to fit this widget
        CellLayout layout = getCellLayout(container, screen);

        int[] minSpanXY = getMinSpanForWidget(this, appWidgetInfo);
        int[] spanXY = getSpanForWidget(this, appWidgetInfo);

        // Try finding open space on Launcher screen
        // We have saved the position to which the widget was dragged-- this really only matters
        // if we are placing widgets on a "spring-loaded" screen
        int[] cellXY = mTmpAddItemCellCoordinates;
        int[] touchXY = mPendingAddInfo.dropPos;
        int[] finalSpan = new int[2];
        boolean foundCellSpan = false;
        if (mPendingAddInfo.cellX >= 0 && mPendingAddInfo.cellY >= 0) {

            cellXY[0] = mPendingAddInfo.cellX;
            cellXY[1] = mPendingAddInfo.cellY;
            spanXY[0] = mPendingAddInfo.spanX;
            spanXY[1] = mPendingAddInfo.spanY;


            boolean reArrange = false;

            if (cellXY[0] + spanXY[0] > 8 || cellXY[1] + spanXY[1] > 10) {
                reArrange = true;
            } else {
                for (int x = cellXY[0]; x < cellXY[0] + spanXY[0]; x++) {
                    for (int y = cellXY[1]; y < cellXY[1] + spanXY[1]; y++) {
                        if (layout.isOccupied(x, y)) {
                            reArrange = true;
                            break;
                        }
                    }
                }
            }

            if (reArrange) {
                // 指定された位置とサイズをもとに空きスペースを探す
                // (ガイドからのウィジェット貼り付け用)
                int pixelX = (int) Math.floor(layout.getCellWidth() * cellXY[0]);
                int pixelY = (int) Math.floor(layout.getCellHeight() * cellXY[1]);
                int[] result = new int[2];
                int[] resultSpan = new int[2];
                int ret[] = layout.findNearestVacantArea(pixelX, pixelY, minSpanXY[0], minSpanXY[1],
                        spanXY[0], spanXY[1], result, resultSpan);
                if (ret[0] != -1 && ret[1] != -1) {
                     cellXY[0] = result[0];
                     cellXY[1] = result[1];
                     spanXY[0] = resultSpan[0];
                     spanXY[1] = resultSpan[1];
                    foundCellSpan = true;
                }
            } else {
                foundCellSpan = true;
            }

        } else if (touchXY != null) {
            // when dragging and dropping, just find the closest free spot
            int[] result = layout.findNearestVacantArea(
                    touchXY[0], touchXY[1], minSpanXY[0], minSpanXY[1], spanXY[0],
                    spanXY[1], cellXY, finalSpan);
            spanXY[0] = finalSpan[0];
            spanXY[1] = finalSpan[1];
            foundCellSpan = (result != null);
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, minSpanXY[0], minSpanXY[1]);
            spanXY[0] = minSpanXY[0];
            spanXY[1] = minSpanXY[1];
        }

        if (!foundCellSpan) {
            if (appWidgetId != -1) {
                // Deleting an app widget ID is a void call but writes to disk before returning
                // to the caller...
                new Thread("deleteAppWidgetId") {
                    public void run() {
                        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                    }
                }.start();
            }
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        // Build Launcher-specific widget info and save to database
        LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(appWidgetId,
                appWidgetInfo.provider);
        launcherInfo.spanX = spanXY[0];
        launcherInfo.spanY = spanXY[1];
        launcherInfo.minSpanX = mPendingAddInfo.minSpanX;
        launcherInfo.minSpanY = mPendingAddInfo.minSpanY;

        LauncherModel.addItemToDatabase(this, launcherInfo,
                container, screen, cellXY[0], cellXY[1], false);

        if (!mRestoring) {
            if (hostView == null) {
                // Perform actual inflation because we're live
                launcherInfo.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
                launcherInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
            } else {
                // The AppWidgetHostView has already been inflated and instantiated
                launcherInfo.hostView = hostView;
            }

            launcherInfo.hostView.setTag(launcherInfo);
            launcherInfo.hostView.setVisibility(View.VISIBLE);

            mWorkspace.addInScreen(launcherInfo.hostView, container, screen, cellXY[0], cellXY[1],
                    launcherInfo.spanX, launcherInfo.spanY, isWorkspaceLocked());

            addWidgetToAutoAdvanceIfNeeded(launcherInfo.hostView, appWidgetInfo);
        }
        resetAddInfo();

        mWorkspace.invalidate();

        addPendingAppWidget();
    }

    private void addPendingAppWidget() {
        if (mPendingWidgetInfoList != null && mPendingWidgetInfoList.size() > 0) {
            DummyInfo widgetInfo = mPendingWidgetInfoList.get(0);
            mPendingWidgetInfoList.remove(0);
            if (!addAppWidgetFromDummy(null, widgetInfo, null, false)) {
                createDummyItem(widgetInfo);
            }
        }
    }

    private ArrayList<DummyInfo> mPendingWidgetInfoList;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mUserPresent = false;
                mDragLayer.clearAllResizeFrames();
                mDragLayer.closeContextMenu();
                mDragLayer.closeNameEditView();

                mHomeMenuView.hideMenu(false);

                updateRunning();

                if (mPageEditScreen != null && isPageEditVisible()) {
                    hidePageEdit(false, null);
                }

                // Reset AllApps to its initial state only if we are not in the middle of
                // processing a multi-step drop
                if (mAppsCustomizeTabHost != null && mPendingAddInfo.container == ItemInfo.NO_ID) {
                    mAppsCustomizeTabHost.reset();
                    showWorkspace(false);
                }

            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                mUserPresent = true;
                updateRunning();
            }
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Listen for broadcasts related to user-presence
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mReceiver, filter);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            FirstFrameAnimatorHelper.initializePreDrawListener(getWindow().getDecorView());
        } else {
            FirstFrameAnimatorHelper.initializeDrawListener(getWindow().getDecorView());
        }
        mAttached = true;
        mVisible = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;

        if (mAttached) {
            unregisterReceiver(mReceiver);
            mAttached = false;
        }
        updateRunning();
    }

    @SuppressLint("NewApi")
    public void onWindowVisibilityChanged(int visibility) {
        mVisible = visibility == View.VISIBLE;
        updateRunning();
        // The following code used to be in onResume, but it turns out onResume is called when
        // you're in All Apps and click home to go to the workspace. onWindowVisibilityChanged
        // is a more appropriate event to handle
        if (mVisible) {
            mAppsCustomizeTabHost.onWindowVisible();
            if (!mWorkspaceLoading) {
                final ViewTreeObserver observer = mWorkspace.getViewTreeObserver();
                // We want to let Launcher draw itself at least once before we force it to build
                // layers on all the workspace pages, so that transitioning to Launcher from other
                // apps is nice and speedy.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {

                    observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        private boolean mStarted = false;
                        public boolean onPreDraw() {
                            if (mStarted) return true;
                            mStarted = true;
                            // We delay the layer building a bit in order to give
                            // other message processing a time to run.  In particular
                            // this avoids a delay in hiding the IME if it was
                            // currently shown, because doing that may involve
                            // some communication back with the app.
                            mWorkspace.postDelayed(mBuildLayersRunnable, 500);
                            final ViewTreeObserver.OnPreDrawListener listener = this;
                            mWorkspace.post(new Runnable() {
                                    public void run() {
                                        if (mWorkspace != null &&
                                                mWorkspace.getViewTreeObserver() != null) {
                                            mWorkspace.getViewTreeObserver().
                                                    removeOnPreDrawListener(listener);
                                        }
                                    }
                                });
                            return true;
                        }
                    });

                } else {

                    observer.addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
                        private boolean mStarted = false;
                        public void onDraw() {
                            if (mStarted) return;
                            mStarted = true;
                            // We delay the layer building a bit in order to give
                            // other message processing a time to run.  In particular
                            // this avoids a delay in hiding the IME if it was
                            // currently shown, because doing that may involve
                            // some communication back with the app.
                            mWorkspace.postDelayed(mBuildLayersRunnable, 500);
                            final ViewTreeObserver.OnDrawListener listener = this;
                            mWorkspace.post(new Runnable() {
                                    public void run() {
                                        if (mWorkspace != null &&
                                                mWorkspace.getViewTreeObserver() != null) {
                                            mWorkspace.getViewTreeObserver().
                                                    removeOnDrawListener(listener);
                                        }
                                    }
                                });
                            return;
                        }
                    });
                }
            }
            clearTypedText();
        }
    }

    private void sendAdvanceMessage(long delay) {
        mHandler.removeMessages(ADVANCE_MSG);
        Message msg = mHandler.obtainMessage(ADVANCE_MSG);
        mHandler.sendMessageDelayed(msg, delay);
        mAutoAdvanceSentTime = System.currentTimeMillis();
    }

    private void updateRunning() {
        boolean autoAdvanceRunning = mVisible && mUserPresent && !mWidgetsToAdvance.isEmpty();
        if (autoAdvanceRunning != mAutoAdvanceRunning) {
            mAutoAdvanceRunning = autoAdvanceRunning;
            if (autoAdvanceRunning) {
                long delay = mAutoAdvanceTimeLeft == -1 ? mAdvanceInterval : mAutoAdvanceTimeLeft;
                sendAdvanceMessage(delay);
            } else {
                if (!mWidgetsToAdvance.isEmpty()) {
                    mAutoAdvanceTimeLeft = Math.max(0, mAdvanceInterval -
                            (System.currentTimeMillis() - mAutoAdvanceSentTime));
                }
                mHandler.removeMessages(ADVANCE_MSG);
                mHandler.removeMessages(0); // Remove messages sent using postDelayed()
            }
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ADVANCE_MSG) {
                int i = 0;
                for (View key: mWidgetsToAdvance.keySet()) {
                    final View v = key.findViewById(mWidgetsToAdvance.get(key).autoAdvanceViewId);
                    final int delay = mAdvanceStagger * i;
                    if (v instanceof Advanceable) {
                       postDelayed(new Runnable() {
                           public void run() {
                               ((Advanceable) v).advance();
                           }
                       }, delay);
                    }
                    i++;
                }
                sendAdvanceMessage(mAdvanceInterval);
            }
        }
    };

    void addWidgetToAutoAdvanceIfNeeded(View hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo == null || appWidgetInfo.autoAdvanceViewId == -1) return;
        View v = hostView.findViewById(appWidgetInfo.autoAdvanceViewId);
        if (v instanceof Advanceable) {
            mWidgetsToAdvance.put(hostView, appWidgetInfo);
            ((Advanceable) v).fyiWillBeAdvancedByHostKThx();
            updateRunning();
        }
    }

    void removeWidgetToAutoAdvance(View hostView) {
        if (mWidgetsToAdvance.containsKey(hostView)) {
            mWidgetsToAdvance.remove(hostView);
            updateRunning();
        }
    }

    public void removeAppWidget(LauncherAppWidgetInfo launcherInfo) {
        removeWidgetToAutoAdvance(launcherInfo.hostView);
        launcherInfo.hostView = null;
    }

    void showOutOfSpaceMessage(boolean isHotseatLayout) {
        int strId = (isHotseatLayout ? R.string.hotseat_out_of_space : R.string.out_of_space);
        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    void closeSystemDialogs() {
        getWindow().closeAllPanels();

        // Whatever we were doing is hereby canceled.
        mWaitingForResult = false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        long startTime = 0;
        if (DEBUG_RESUME_TIME) {
            startTime = System.currentTimeMillis();
        }
        super.onNewIntent(intent);

        // ドロワーからのウィジェット貼り付け時にホームボタンを押された時の対策
        if (Float.compare(mWorkspace.getBgScale(), 1.0f) < 0) {
            return;
        }

        // Close the menu
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            // also will cancel mWaitingForResult.
            closeSystemDialogs();

            final boolean alreadyOnHome =
                    ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                        != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

            Runnable processIntent = new Runnable() {
                public void run() {
                    if (mWorkspace == null) {
                        // Can be cases where mWorkspace is null, this prevents a NPE
                        return;
                    }
                    Folder openFolder = mWorkspace.getOpenFolder();
                    // In all these cases, only animate if we're already on home
                    mWorkspace.exitDragLayerViews();
                    if (alreadyOnHome && mState == State.WORKSPACE && !mWorkspace.isTouchActive() &&
                            openFolder == null
                            && mDragLayer.getContextMenuView() == null
                            && mDragLayer.getNameEditView() == null
                            && !mHomeMenuPopup.isShow()) {
                        mWorkspace.moveToDefaultScreen(true);
                    }

                    if (alreadyOnHome) {
                        mHomeMenuView.hideMenu(true);
                    } else {
                        mHomeMenuView.hideMenu(false);
                    }

                    closeFolder();
                    exitSpringLoadedDragMode();

                    if (isPageEditVisible()) {

                        if (alreadyOnHome) {
                            hidePageEdit(true, new Runnable() {
                                @Override
                                public void run() {
                                    applyPageEdit();
                                }
                            });

                        } else {
                            hidePageEdit(false, null);
                        }

                    } else {
                        // If we are already on home, then just animate back to the workspace,
                        // otherwise, just wait until onResume to set the state back to Workspace
                        if (alreadyOnHome) {
                            showWorkspace(true);
                        } else {
                            mOnResumeState = State.WORKSPACE;
                        }
                    }

                    final View v = getWindow().peekDecorView();
                    if (v != null && v.getWindowToken() != null) {
                        InputMethodManager imm = (InputMethodManager)getSystemService(
                                INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }

                    // Reset AllApps to its initial state
                    if (!alreadyOnHome && mAppsCustomizeTabHost != null) {
                        mAppsCustomizeTabHost.reset();
                    }
                }
            };

            if (alreadyOnHome && !mWorkspace.hasWindowFocus()) {
                // Delay processing of the intent to allow the status bar animation to finish
                // first in order to avoid janky animations.
                mWorkspace.postDelayed(processIntent, 350);
            } else {
                // Process the intent immediately.
                processIntent.run();
            }
        }

        String themeId = intent.getStringExtra(ContentsOperator.INTENT_ACTION_EXTRA_ASSET_ID);
        int contentsType = intent.getIntExtra(ContentsOperator.INTENT_ACTION_EXTRA_CONTENTS_TYPE, 0);
        if (themeId != null) {
            ContentsTypeValue contentsTypeValue = ContentsTypeValue.getEnum(contentsType);
            if (contentsTypeValue == ContentsTypeValue.CONTENTS_TYPE_THEME) {

                if (mHasThemeResLoaded) {
                    applyTheme(themeId, false);
                } else {
                    mApplyTheme = true;
                    mThemeIdForApplying = themeId;
                    mInAppThemeForApplying = false;
                }

            } else if (contentsTypeValue == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T) {
                mModel.applyKisekaeIcons(this, themeId, false);
            } else if (ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_WIDGET
                    || ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T) {

                if (mHasThemeResLoaded) {
                    addThemeTandokuWidget();
                } else {
                    mAddThemeWdiget = true;
                }
            } else if (ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T
                    || ContentsTypeValue.getEnum(contentsType) == ContentsTypeValue.CONTENTS_TYPE_WP) {

                if (mHasThemeResLoaded) {

                    PanelWallpapers panelWps = mWorkspace.getPanelWallpapers();
                    if (panelWps == null) {
                        panelWps = new PanelWallpapers(new String[Workspace.MAX_SCREEN_COUNT]);
                        mWorkspace.setThemeBackgrounds(panelWps);
                    }

                    mModel.applyThemeWallpaper(this, ContentsOperator.op.getIndividualWpPath(this),
                            panelWps, mWorkspace.getPageCount());
                } else {
                    mApplyThemeWallpaper = true;
                }
            }
        }

        if (DEBUG_RESUME_TIME) {
            DebugLog.instance.outputLog(TAG, "Time spent in onNewIntent: " + (System.currentTimeMillis() - startTime));
        }
    }

    private void addThemeTandokuWidget() {

        AppWidgetProviderInfo providerInfo =
                LauncherModel.findWidgetByProviderClassName(this, ThemeUtils.WIDGET_CLASS_NAME_BATTERY);
        if (providerInfo == null) return;

        int screen = getCurrentWorkspaceScreen();
        CellLayout layout = getCellLayout(LauncherSettings.Favorites.CONTAINER_DESKTOP, screen);

        int[] cellXY = { 0, 0 };
        int[] minSpanXY = getMinSpanForWidget(this, providerInfo);
        int[] spanXY = getSpanForWidget(this, providerInfo);

        boolean foundCellSpan = layout.findCellForSpan(cellXY, minSpanXY[0], minSpanXY[1]);
        if (!foundCellSpan) {
            // 他のページも空きスペースがないか探す
            final int pageCount = mWorkspace.getPageCount();
            for (int i = 0; i < pageCount; i++) {
                if (i == screen) continue;
                layout = getCellLayout(LauncherSettings.Favorites.CONTAINER_DESKTOP, i);
                foundCellSpan = layout.findCellForSpan(cellXY, minSpanXY[0], minSpanXY[1]);
                if (foundCellSpan) {
                    screen = i;
                    break;
                }
            }

            if (!foundCellSpan) {
                showOutOfSpaceMessage(isHotseatLayout(layout));
                return;
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            // ダミーを追加

            DummyInfo dummyInfo = new DummyInfo();
            dummyInfo.screen = screen;
            dummyInfo.cellX = cellXY[0];
            dummyInfo.cellY = cellXY[1];
            dummyInfo.spanX = spanXY[0];
            dummyInfo.spanY = spanXY[1];
            dummyInfo.minSpanX = spanXY[0];
            dummyInfo.minSpanY = spanXY[1];
            dummyInfo.inAppWidget = true;
            dummyInfo.inAppWidgetClassName = ThemeUtils.WIDGET_CLASS_NAME_BATTERY;

            createDummyItem(dummyInfo);

        } else {

            if (!addAppWidget(providerInfo, screen, cellXY, spanXY, minSpanXY, false)) {
                DummyInfo dummyInfo = new DummyInfo();
                dummyInfo.screen = screen;
                dummyInfo.cellX = cellXY[0];
                dummyInfo.cellY = cellXY[1];
                dummyInfo.spanX = spanXY[0];
                dummyInfo.spanY = spanXY[1];
                dummyInfo.minSpanX = spanXY[0];
                dummyInfo.minSpanY = spanXY[1];
                dummyInfo.inAppWidget = true;
                dummyInfo.inAppWidgetClassName = ThemeUtils.WIDGET_CLASS_NAME_BATTERY;

                createDummyItem(dummyInfo);
            }
        }
    }


    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        for (int page: mSynchronouslyBoundPages) {
            mWorkspace.restoreInstanceStateForChild(page);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, mWorkspace.getNextPage());
        super.onSaveInstanceState(outState);

        outState.putInt(RUNTIME_STATE, mState.ordinal());
        // We close any open folder since it will not be re-opened, and we need to make sure
        // this state is reflected.
        closeFolder();

        if (mPendingAddInfo.container != ItemInfo.NO_ID && mPendingAddInfo.screen > -1 &&
                mWaitingForResult) {
            outState.putLong(RUNTIME_STATE_PENDING_ADD_CONTAINER, mPendingAddInfo.container);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SCREEN, mPendingAddInfo.screen);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_X, mPendingAddInfo.cellX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_Y, mPendingAddInfo.cellY);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_X, mPendingAddInfo.spanX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y, mPendingAddInfo.spanY);
            outState.putParcelable(RUNTIME_STATE_PENDING_ADD_WIDGET_INFO, mPendingAddWidgetInfo);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_WIDGET_ID, mPendingAddWidgetId);
        }

        if (mFolderInfo != null && mWaitingForResult) {
            outState.putBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, true);
            outState.putLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID, mFolderInfo.id);
        }

        // Save the current AppsCustomize tab
        if (mAppsCustomizeTabHost != null) {
            String currentTabTag = mAppsCustomizeTabHost.getCurrentTabTag();
            if (currentTabTag != null) {
                outState.putString("apps_customize_currentTab", currentTabTag);
            }
            int currentIndex = mAppsCustomizeContent.getSaveInstanceStateIndex();
            outState.putInt("apps_customize_currentIndex", currentIndex);
        }

        mWpChangeDetector.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onDestroy()_");

        // APP Measurement
        //TrackingHelper.shutdown();
        doAppMeasurement(false);

        super.onDestroy();

        // Remove all pending runnables
        mHandler.removeMessages(ADVANCE_MSG);
        mHandler.removeMessages(0);
        mWorkspace.removeCallbacks(mBuildLayersRunnable);

        // Stop callbacks from LauncherModel
        LauncherApplication app = ((LauncherApplication) getApplication());
        mModel.stopLoader();
        app.setLauncher(null);

        try {
            mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }
        mAppWidgetHost = null;

        mWidgetsToAdvance.clear();

        TextKeyListener.getInstance().release();

        // Disconnect any of the callbacks and drawables associated with ItemInfos on the workspace
        // to prevent leaking Launcher activities on orientation change.
        if (mModel != null) {
            mModel.unbindItemInfosAndClearQueuedBindRunnables();
        }

        getContentResolver().unregisterContentObserver(mWidgetObserver);
        unregisterReceiver(mCloseSystemDialogsReceiver);

        mDragLayer.clearAllResizeFrames();
        mDragLayer.closeContextMenu();
        ((ViewGroup) mWorkspace.getParent()).removeAllViews();
        mWorkspace.removeAllViews();

        PanelWallpapers panelWps = mWorkspace.getPanelWallpapers();
        if (panelWps != null) {
            panelWps.unload();
        }
        mWorkspace = null;
        mDragController = null;

        handler.removeCallbacks(runnnableForPermission);//pemissionチェック対応

        ViewAnimUtils.onDestroyActivity();
    }

    public DragController getDragController() {
        return mDragController;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (requestCode >= 0) mWaitingForResult = true;
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isWorkspaceLocked()) {
            return false;
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mAppsCustomizeTabHost.isTransitioning()) {
            return false;
        }
//        boolean allAppsVisible = (mAppsCustomizeTabHost.getVisibility() == View.VISIBLE);
//        menu.setGroupVisible(MENU_GROUP_WALLPAPER, !allAppsVisible);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//        case MENU_WALLPAPER_SETTINGS:
//            startWallpaper();
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null, true);
        // Use a custom animation for launching search
        return true;
    }

    public boolean isWorkspaceLocked() {
        return mWorkspaceLoading || mWaitingForResult;
    }

    private void resetAddInfo() {
        mPendingAddInfo.container = ItemInfo.NO_ID;
        mPendingAddInfo.screen = -1;
        mPendingAddInfo.cellX = mPendingAddInfo.cellY = -1;
        mPendingAddInfo.spanX = mPendingAddInfo.spanY = -1;
        mPendingAddInfo.minSpanX = mPendingAddInfo.minSpanY = -1;
        mPendingAddInfo.dropPos = null;
    }

    void addAppWidgetImpl(final int appWidgetId, ItemInfo info, AppWidgetHostView boundWidget,
            AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo.configure != null) {
            mPendingAddWidgetInfo = appWidgetInfo;
            mPendingAddWidgetId = appWidgetId;

            // Launch over to configure widget, if needed
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                intent.setComponent(appWidgetInfo.configure);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                startActivityForResultSafely(intent, REQUEST_CREATE_APPWIDGET);

            } else {
                startAppWidgetConfigureActivitySafely(appWidgetId);
            }

        } else {
            // Otherwise just add it
            completeAddAppWidget(appWidgetId, info.container, info.screen, boundWidget,
                    appWidgetInfo);
            // Exit spring loaded mode if necessary after adding the widget
            exitSpringLoadedDragModeDelayed(true, false, null);
        }
    }

    /**
     * Process a shortcut drop.
     *
     * @param componentName The name of the component
     * @param screen The screen where it should be added
     * @param cell The cell it should be added to, optional
     * @param position The location on the screen where it was dropped, optional
     */
    void processShortcutFromDrop(ComponentName componentName, long container, int screen,
            int[] cell, int[] loc) {
        resetAddInfo();
        mPendingAddInfo.container = container;
        mPendingAddInfo.screen = screen;
        mPendingAddInfo.dropPos = loc;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }

        Intent createShortcutIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        createShortcutIntent.setComponent(componentName);
        processShortcut(createShortcutIntent);
    }

    /**
     * Process a widget drop.
     *
     * @param info The PendingAppWidgetInfo of the widget being added.
     * @param screen The screen where it should be added
     * @param cell The cell it should be added to, optional
     * @param position The location on the screen where it was dropped, optional
     */
    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    void addAppWidgetFromDrop(PendingAddWidgetInfo info, long container, int screen,
            int[] cell, int[] span, int[] loc) {
        resetAddInfo();
        mPendingAddInfo.container = info.container = container;
        mPendingAddInfo.screen = info.screen = screen;
        mPendingAddInfo.dropPos = loc;
        mPendingAddInfo.minSpanX = info.minSpanX;
        mPendingAddInfo.minSpanY = info.minSpanY;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }
        if (span != null) {
            mPendingAddInfo.spanX = span[0];
            mPendingAddInfo.spanY = span[1];
        }

        AppWidgetHostView hostView = info.boundWidget;
        int appWidgetId;
        if (hostView != null) {
            appWidgetId = hostView.getAppWidgetId();
            addAppWidgetImpl(appWidgetId, info, hostView, info.info);
        } else {
            // In this case, we either need to start an activity to get permission to bind
            // the widget, or we need to start an activity to configure the widget, or both.
            appWidgetId = getAppWidgetHost().allocateAppWidgetId();

            boolean success;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                success = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.componentName);
                if (success) {
                    mAppWidgetManager.updateAppWidgetOptions(appWidgetId, info.bindOptions);
                }
            } else {
                success = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.componentName, info.bindOptions);
            }

            if (success) {
                addAppWidgetImpl(appWidgetId, info, null, info.info);
            } else {
                mPendingAddWidgetInfo = info.info;
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                mPendingAddWidgetId = appWidgetId; // Intentでは渡せない端末が存在
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.componentName);
                // TODO: we need to make sure that this accounts for the options bundle.
                // intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
                startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
            }
        }
    }

    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    boolean addAppWidgetFromDummy(DummyHomeItemView dummyView, DummyInfo dummyInfo, String providerClassName, boolean notShowDialog) {

        if (dummyView != null) {
            dummyInfo = (DummyInfo) dummyView.getTag();
        }

        AppWidgetProviderInfo providerInfo;
        if (dummyInfo.inAppWidget) {
            providerInfo = LauncherModel.findWidgetByProviderClassName(
                    Launcher.this, dummyInfo.inAppWidgetClassName);
        } else {
            providerInfo = LauncherModel.findWidgetByProviderClassName(
                    Launcher.this, providerClassName);
        }
        if (providerInfo == null) {
            addPendingAppWidget();
            return false;
        }

        PendingAddWidgetInfo info = new PendingAddWidgetInfo(providerInfo, null, null);

        int[] cell = { dummyInfo.cellX, dummyInfo.cellY };
        int[] span = { dummyInfo.spanX, dummyInfo.spanY };
        int[] minSpan = { dummyInfo.minSpanX, dummyInfo.minSpanY };

        resetAddInfo();
        mPendingAddInfo.container = info.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
        mPendingAddInfo.screen = info.screen = dummyInfo.screen;
        mPendingAddInfo.dropPos = cell;
//        mPendingAddInfo.minSpanX = info.minSpanX;
//        mPendingAddInfo.minSpanY = info.minSpanY;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }
        if (span != null) {
            mPendingAddInfo.spanX = span[0];
            mPendingAddInfo.spanY = span[1];
        }
        mPendingAddInfo.minSpanX = minSpan[0];
        mPendingAddInfo.minSpanY = minSpan[1];

        AppWidgetHostView hostView = info.boundWidget;
        int appWidgetId;
        if (hostView != null) {
            appWidgetId = hostView.getAppWidgetId();
            addAppWidgetImpl(appWidgetId, info, hostView, info.info);

            return true;

        } else {
            // In this case, we either need to start an activity to get permission to bind
            // the widget, or we need to start an activity to configure the widget, or both.
            appWidgetId = getAppWidgetHost().allocateAppWidgetId();

            boolean success;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                success = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.componentName);
                if (success) {
                    mAppWidgetManager.updateAppWidgetOptions(appWidgetId, info.bindOptions);
                }
            } else {
                success = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.componentName, info.bindOptions);
            }

            if (success) {

                if (dummyView != null) {
                    LauncherModel.deleteItemFromDatabase(Launcher.this, dummyInfo);
                    ((HomeGridLayout) dummyView.getParent().getParent()).removeViewInLayout(dummyView);
                }

                addAppWidgetImpl(appWidgetId, info, null, info.info);

                return true;

            } else {

                if (notShowDialog) {
                    addPendingAppWidget();
                    return false;
                }

                if (dummyView != null) {
                    mFromDummy = true;
                    mPendingDummyView = dummyView;
                }

                mPendingAddWidgetInfo = info.info;
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                mPendingAddWidgetId = appWidgetId; // Intentでは渡せない端末が存在
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.componentName);
                // TODO: we need to make sure that this accounts for the options bundle.
                // intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
                startActivityForResult(intent, REQUEST_BIND_APPWIDGET);

                return true;
            }
        }
    }

    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    boolean addAppWidget(AppWidgetProviderInfo providerInfo,
            int screen, int[] cellXY, int[] spanXY, int[] minSpanXY, boolean notShowDialog) {

        PendingAddWidgetInfo info = new PendingAddWidgetInfo(providerInfo, null, null);

        resetAddInfo();
        mPendingAddInfo.container = info.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
        mPendingAddInfo.screen = info.screen = screen;
        mPendingAddInfo.dropPos = cellXY;
        mPendingAddInfo.minSpanX = info.minSpanX;
        mPendingAddInfo.minSpanY = info.minSpanY;

        mPendingAddInfo.cellX = cellXY[0];
        mPendingAddInfo.cellY = cellXY[1];
        mPendingAddInfo.spanX = spanXY[0];
        mPendingAddInfo.spanY = spanXY[1];

        AppWidgetHostView hostView = info.boundWidget;
        int appWidgetId;
        if (hostView != null) {
            appWidgetId = hostView.getAppWidgetId();
            addAppWidgetImpl(appWidgetId, info, hostView, info.info);

            return true;

        } else {
            // In this case, we either need to start an activity to get permission to bind
            // the widget, or we need to start an activity to configure the widget, or both.
            appWidgetId = getAppWidgetHost().allocateAppWidgetId();

            boolean success;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                success = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.componentName);
                if (success) {
                    mAppWidgetManager.updateAppWidgetOptions(appWidgetId, info.bindOptions);
                }
            } else {
                success = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.componentName, info.bindOptions);
            }

            if (success) {
                addAppWidgetImpl(appWidgetId, info, null, info.info);
                return true;

            } else {

                if (notShowDialog) {
                    return false;
                }

                mPendingAddWidgetInfo = info.info;
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.componentName);
                // TODO: we need to make sure that this accounts for the options bundle.
                // intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
                startActivityForResult(intent, REQUEST_BIND_APPWIDGET);

                return true;
            }
        }
    }

    void processShortcut(Intent intent) {
        // Handle case where user selected "Applications"
        String applicationName = getResources().getString(R.string.group_applications);
        String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        if (applicationName != null && applicationName.equals(shortcutName)) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
            pickIntent.putExtra(Intent.EXTRA_TITLE, getText(R.string.title_select_application));
            startActivityForResultSafely(pickIntent, REQUEST_PICK_APPLICATION);
        } else {
            startActivityForResultSafely(intent, REQUEST_CREATE_SHORTCUT);
        }
    }

    void processWallpaper(Intent intent) {
        startActivityForResult(intent, REQUEST_PICK_WALLPAPER);
    }

    FolderIcon addFolder(CellLayout layout, long container, final int screen, int cellX,
            int cellY) {
        final FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = getText(R.string.folder_name);

        // Update the model
        LauncherModel.addItemToDatabase(Launcher.this, folderInfo, container, screen, cellX, cellY,
                false);
        sFolders.put(folderInfo.id, folderInfo);

        // Create the view
        FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this,
                layout, folderInfo, mIconCache, layout.getFolderIconSize(),
                layout.getFolderCellPaddingTop(),
                layout.getScale(),
                layout.getCellWidth() * Workspace.ICON_SPAN,
                layout.getCellPaddingTop());
        mWorkspace.addInScreen(newFolder, container, screen, cellX, cellY,
                Workspace.ICON_SPAN, Workspace.ICON_SPAN, isWorkspaceLocked());
        return newFolder;
    }

    void removeFolder(FolderInfo folder) {
        sFolders.remove(folder.id);
    }

    private void showHomeMenu() {
        showWorkspace(true);

        closeFolder();
        mDragLayer.closeContextMenu();
        mDragLayer.closeNameEditView();

        mHomeMenuPopup.show();
    }

    private void startWallpaper() {
        final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        Intent chooser = Intent.createChooser(pickWallpaper,
                getText(R.string.chooser_wallpaper));
        startActivityForResult(chooser, REQUEST_PICK_WALLPAPER);
    }

    /**
     * Registers various content observers. The current implementation registers
     * only a favorites observer to keep track of the favorites applications.
     */
    private void registerContentObservers() {
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(LauncherProvider.CONTENT_APPWIDGET_RESET_URI,
                true, mWidgetObserver);
    }

    private boolean mPressedBackKeyLong = false;

    @Override
    public boolean onKeyLongPress (int keyCode, KeyEvent event) {

        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            mPressedBackKeyLong = true;
        }

        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (mHomeMenuPopup.isShow()) {
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_MOVE_HOME:
            case KeyEvent.KEYCODE_MOVE_END:
                return true;
            }
        }

        if (mDragLayer.getContextMenuView() != null) {
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_MOVE_HOME:
            case KeyEvent.KEYCODE_MOVE_END:
                return true;
            }
        }

        NameEditView nameEditView = mDragLayer.getNameEditView();
        if (nameEditView != null) {
            if (nameEditView.editTextIsFocused()) {

                if (nameEditView.isFocusedTextStart()) {
                    switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        return true;
                    }
                }
                // else if にはしない (長さ0の場合を考慮)
                if (nameEditView.isFocusedTextEnd()) {
                    switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        return true;
                    }
                }

            } else {
                // 編集中でない場合
                switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_PAGE_UP:
                case KeyEvent.KEYCODE_PAGE_DOWN:
                case KeyEvent.KEYCODE_MOVE_HOME:
                case KeyEvent.KEYCODE_MOVE_END:
                    return true;
                }
            }
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (isPropertyEnabled(DUMP_STATE_PROPERTY)) {
                        dumpState();
                        return true;
                    }
                    break;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if (mPressedBackKeyLong) {
                        mPressedBackKeyLong = false;
                        return true;
                    }
            }
        }

        return super.dispatchKeyEvent(event);
    }

    private void applyPageEdit() {

        // ループ設定更新
        if (mPageLoop != mPageEditScreen.pageLoop()) {
            mPageLoop = mPageEditScreen.pageLoop();
            mWorkspace.setLooping(mPageLoop);
            mModel.updatePageLoopSetting(Launcher.this, mPageLoop);
        }

        boolean defaultPageChangeFlg = false;
        final int newDefaultPage = mPageEditScreen.getDefaultPageIndex();
        if (newDefaultPage != mDefaultPage) {
            mDefaultPage = newDefaultPage;
            defaultPageChangeFlg = true;
        }

        final int currentPageCount = mWorkspace.getPageCount();
        final int newPageCount = mPageEditScreen.getNumPages();

        final String[] editedWpPaths = mPageEditScreen.getEditedWpPaths();
        String[] editedWpPathsCopy;

        // DB状態更新

        if (mIsThemeMode) {
            editedWpPathsCopy = Arrays.copyOf(editedWpPaths, editedWpPaths.length);

            boolean wpPathChangeFlg = false;
            PanelWallpapers panelWps = mWorkspace.getPanelWallpapers();
            for (int i = 0; i < editedWpPaths.length; i++) {
                if (panelWps.getPath(i) != editedWpPaths[i]) {
                    wpPathChangeFlg = true;
                    break;
                }
            }

            if (defaultPageChangeFlg || newPageCount != currentPageCount
                    || wpPathChangeFlg) {
                mModel.updatePageInfo(Launcher.this, newDefaultPage, newPageCount, editedWpPathsCopy);
            }
        } else {
            if (defaultPageChangeFlg || newPageCount != currentPageCount) {
                mModel.updatePageInfo(Launcher.this, newDefaultPage, newPageCount);
            }
        }

        final int[] pageMap = mPageEditScreen.getPageMap();
        final int[] pageMapCopy = Arrays.copyOf(pageMap, pageMap.length);
        ArrayList<int[]> screeNoMap = new ArrayList<int[]>();
        for (int i = 0; i < currentPageCount; i++) {
            for (int j = 0; j < newPageCount; j++) {
                if (i == pageMapCopy[j]) {
                     screeNoMap.add(new int[] { i, j });
                     break;
                }
            }
        }
        boolean pageOrderChangeFlg = false;
        for (int[] item : screeNoMap) {
            if (item[0] != item[1]) {
                pageOrderChangeFlg = true;
                break;
            }
        }
        if (pageOrderChangeFlg) {
            KisekaeLauncherModel.updateScreenNos(Launcher.this, screeNoMap);
        }

        boolean hasChange = false;
        if (currentPageCount == newPageCount) {
            for (int i = 0; i < pageMapCopy.length; i++) {
                if (i != pageMapCopy[i]) {
                    hasChange = true;
                    break;
                }
            }
        } else {
            hasChange = true;
        }

        if (!hasChange) {
            mWorkspace.setDefaultPage(mDefaultPage);
            return;
        }

        if (mIsThemeMode) {

            LauncherModel.runOnWorkerThread(new Runnable() {
                @Override
                public void run() {

                    final PanelWallpapers panelWps = mWorkspace.getPanelWallpapers();
                    panelWps.update(Launcher.this, editedWpPaths, false);

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updatePageOrder(currentPageCount, newPageCount, pageMapCopy);
                            mWorkspace.setDefaultPage(mDefaultPage);
                            mWorkspace.invalidate();
                        }
                    });
                }
            });

        } else {
            updatePageOrder(currentPageCount, newPageCount, pageMapCopy);
            mWorkspace.setDefaultPage(mDefaultPage);
            mWorkspace.invalidate();
        }
    }

    @Override
    public void onBackPressed() {
        if (isAllAppsVisible()) {
            showWorkspace(true);

        } else if (isPageEditVisible()) {

            hidePageEdit(true, new Runnable() {
                @Override
                public void run() {
                    applyPageEdit();
                }
            });

        } else if (mWorkspace.getOpenFolder() != null) {
            Folder openFolder = mWorkspace.getOpenFolder();
            if (openFolder.isEditingName()) {
                openFolder.dismissEditingName();
            } else {
                closeFolder();
            }
        } else {
            mWorkspace.exitDragLayerViews();

            // Back button is a no-op here, but give at least some feedback for the button press
            mWorkspace.showOutlinesTemporarily();
        }
    }

    private void updatePageOrder(int currentPageCount, int newPageCount, int[] pageMap) {

        View[] vs = new View[currentPageCount];
        for (int i = 0; i < currentPageCount; i++) {
            vs[i] = mWorkspace.getChildAt(i);
        }

        for (int i = 0; i < currentPageCount; i++) {
            boolean holds = false;
            for (int j = 0; j < newPageCount; j++) {
                if (i == pageMap[j]) {
                    holds = true;
                    break;
                }
            }
            if (!holds) {
                mWorkspace.removePageAllItems((CellLayout) vs[i]);
            }
        }

        mWorkspace.removeAllViews();

        for (int i = 0; i < newPageCount; i++) {
            int index = pageMap[i];
            if (index >= 0) {
                mWorkspace.addPage(vs[index]);
            } else {
                mWorkspace.addPage();
            }
        }

        if (mWorkspace.getCurrentPage() >= newPageCount) {
            mWorkspace.setCurrentPage(newPageCount - 1);
        }
    }

    /**
     * Re-listen when widgets are reset.
     */
    private void onAppWidgetReset() {
        if (mAppWidgetHost != null) {
            mAppWidgetHost.startListening();
        }
    }

    /**
     * Launches the intent referred by the clicked shortcut.
     *
     * @param v The view representing the clicked shortcut.
     */
    public void onClick(View v) {

        // Make sure that rogue clicks don't get through while allapps is launching, or after the
        // view has detached (it's possible for this to happen if the view is removed mid touch).
        if (v.getWindowToken() == null) {
            return;
        }

        if (!mWorkspace.isFinishedSwitchingState()) {
            return;
        }

        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            // Open shortcut
            final Intent intent = ((ShortcutInfo) tag).intent;
            int[] pos = new int[2];
            v.getLocationOnScreen(pos);
            intent.setSourceBounds(new Rect(pos[0], pos[1],
                    pos[0] + v.getWidth(), pos[1] + v.getHeight()));

            boolean success = startActivitySafely(v, intent, tag);

            if (success && v instanceof BubbleTextView) {
                mWaitingForResume = (BubbleTextView) v;
                mWaitingForResume.setStayPressed(true);
            }
        } else if (tag instanceof FolderInfo) {
            if (v instanceof FolderIcon) {
                FolderIcon fi = (FolderIcon) v;
                handleFolderClick(fi);
            }
        } else if (v == mAllAppsButton) {
            if (isAllAppsVisible()) {
                showWorkspace(true);
            } else {
                onClickAllAppsButton(v);
            }
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        // this is an intercepted event being forwarded from mWorkspace;
        // clicking anywhere on the workspace causes the customization drawer to slide down
        showWorkspace(true);
        return false;
    }

    /**
     * Event handler for the "grid" button that appears on the home screen, which
     * enters all apps mode.
     *
     * @param v The view that was clicked.
     */
    public void onClickAllAppsButton(View v) {
        showAllApps(true);
    }

    public void onTouchDownAllAppsButton(View v) {
        // Provide the same haptic feedback that the system offers for virtual keys.
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    void startApplicationDetailsActivity(ComponentName componentName) {
        String packageName = componentName.getPackageName();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(intent);
    }

    void startApplicationUninstallActivity(ApplicationInfo appInfo) {
        if ((appInfo.flags & ApplicationInfo.DOWNLOADED_FLAG) == 0) {
            // System applications cannot be installed. For now, show a toast explaining that.
            // We may give them the option of disabling apps this way.
            int messageId = R.string.uninstall_system_app_text;
            Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
        } else {
            String packageName = appInfo.componentName.getPackageName();
            String className = appInfo.componentName.getClassName();
            Intent intent = new Intent(
                    Intent.ACTION_DELETE, Uri.fromParts("package", packageName, className));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
        }
    }

    @SuppressLint("NewApi")
    boolean startActivity(View v, Intent intent, Object tag) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                startActivity(intent);
            } else {
                // Only launch using the new animation if the shortcut has not opted out (this is a
                // private contract between launcher and may be ignored in the future).
                boolean useLaunchAnimation = (v != null) &&
                        !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);

                if (useLaunchAnimation) {
                    ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0,
                            v.getMeasuredWidth(), v.getMeasuredHeight());

                    // Could be launching some bookkeeping activity
                    startActivity(intent, opts.toBundle());

                } else {
                    startActivity(intent);
                }
            }
            return true;
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity. "
                    + "tag="+ tag + " intent=" + intent, e);
        }
        return false;
    }

    boolean startActivitySafely(View v, Intent intent, Object tag) {
        boolean success = false;
        try {
            success = startActivity(v, intent, tag);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch. tag=" + tag + " intent=" + intent, e);
        }
        return success;
    }

    boolean startActivitySafely(Intent intent) {
        boolean success = false;
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch. intent=" + intent, e);
        }
        return success;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    void startAppWidgetConfigureActivitySafely(int appWidgetId) {
        try {
            mAppWidgetHost.startAppWidgetConfigureActivityForResult(this, appWidgetId, 0,
                    REQUEST_CREATE_APPWIDGET, null);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    void startActivityForResultSafely(Intent intent, int requestCode) {
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity.", e);
        }
    }

    private void handleFolderClick(FolderIcon folderIcon) {
        final FolderInfo info = folderIcon.getFolderInfo();
        Folder openFolder = mWorkspace.getFolderForTag(info);

        // If the folder info reports that the associated folder is open, then verify that
        // it is actually opened. There have been a few instances where this gets out of sync.
        if (info.opened && openFolder == null) {
            DebugLog.instance.outputLog(TAG, "Folder info marked as open, but associated folder is not open. Screen: "
                    + info.screen + " (" + info.cellX + ", " + info.cellY + ")");
            info.opened = false;
        }

        if (!info.opened && !folderIcon.getFolder().isDestroyed()) {
            // Close any open folder
            closeFolder();
            // Open the requested folder
            openFolder(folderIcon);
        } else {
            // Find the open folder...
            int folderScreen;
            if (openFolder != null) {
                folderScreen = mWorkspace.getPageForView(openFolder);
                // .. and close it
                closeFolder(openFolder);
                if (folderScreen != mWorkspace.getCurrentPage()) {
                    // Close any folder open on the current screen
                    closeFolder();
                    // Pull the folder onto this screen
                    openFolder(folderIcon);
                }
            }
        }
    }

    /**
     * This method draws the FolderIcon to an ImageView and then adds and positions that ImageView
     * in the DragLayer in the exact absolute location of the original FolderIcon.
     */
    private void copyFolderIconToImage(FolderIcon fi) {
        final int width = fi.getMeasuredWidth();
        final int height = fi.getMeasuredHeight();

        // Lazy load ImageView, Bitmap and Canvas
        if (mFolderIconImageView == null) {
            mFolderIconImageView = new ImageView(this);
        }
        if (mFolderIconBitmap == null || mFolderIconBitmap.getWidth() != width ||
                mFolderIconBitmap.getHeight() != height) {
            mFolderIconBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mFolderIconCanvas = new Canvas(mFolderIconBitmap);
        }

        DragLayer.LayoutParams lp;
        if (mFolderIconImageView.getLayoutParams() instanceof DragLayer.LayoutParams) {
            lp = (DragLayer.LayoutParams) mFolderIconImageView.getLayoutParams();
        } else {
            lp = new DragLayer.LayoutParams(width, height);
        }

        // The layout from which the folder is being opened may be scaled, adjust the starting
        // view size by this scale factor.
        float scale = mDragLayer.getDescendantRectRelativeToSelf(fi, mRectForFolderAnimation);
        lp.customPosition = true;
        lp.x = mRectForFolderAnimation.left;
        lp.y = mRectForFolderAnimation.top;
        lp.width = (int) (scale * width);
        lp.height = (int) (scale * height);

        mFolderIconCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        fi.draw(mFolderIconCanvas);
        mFolderIconImageView.setImageBitmap(mFolderIconBitmap);
        if (fi.getFolder() != null) {
            mFolderIconImageView.setPivotX(fi.getFolder().getPivotXForIconAnimation());
            mFolderIconImageView.setPivotY(fi.getFolder().getPivotYForIconAnimation());
        }
        // Just in case this image view is still in the drag layer from a previous animation,
        // we remove it and re-add it.
        if (mDragLayer.indexOfChild(mFolderIconImageView) != -1) {
            mDragLayer.removeView(mFolderIconImageView);
        }
        mDragLayer.addView(mFolderIconImageView, lp);
        if (fi.getFolder() != null) {
            fi.getFolder().bringToFront();
        }
    }

    private void growAndFadeOutFolderIcon(FolderIcon fi) {
        if (fi == null) return;
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.5f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.5f);

        FolderInfo info = (FolderInfo) fi.getTag();
        if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            CellLayout cl = (CellLayout) fi.getParent().getParent();
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) fi.getLayoutParams();
            cl.setFolderLeaveBehindCell(lp.cellX, lp.cellY);
        }

        // Push an ImageView copy of the FolderIcon into the DragLayer and hide the original
        copyFolderIconToImage(fi);
        fi.setVisibility(View.INVISIBLE);

        ObjectAnimator oa = ViewAnimUtils.ofPropertyValuesHolder(mFolderIconImageView, alpha,
                scaleX, scaleY);
        oa.setDuration(getResources().getInteger(R.integer.config_folderAnimDuration));
        oa.start();
    }

    private void shrinkAndFadeInFolderIcon(final FolderIcon fi) {
        if (fi == null) return;
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1.0f);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f);

        final CellLayout cl = (CellLayout) fi.getParent().getParent();

        // We remove and re-draw the FolderIcon in-case it has changed
        mDragLayer.removeView(mFolderIconImageView);
        copyFolderIconToImage(fi);
        ObjectAnimator oa = ViewAnimUtils.ofPropertyValuesHolder(mFolderIconImageView, alpha,
                scaleX, scaleY);
        oa.setDuration(getResources().getInteger(R.integer.config_folderAnimDuration));
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (cl != null) {
                    cl.clearFolderLeaveBehind();
                    // Remove the ImageView copy of the FolderIcon and make the original visible.
                    mDragLayer.removeView(mFolderIconImageView);
                    fi.setVisibility(View.VISIBLE);
                }
            }
        });
        oa.start();
    }

    /**
     * Opens the user folder described by the specified tag. The opening of the folder
     * is animated relative to the specified View. If the View is null, no animation
     * is played.
     *
     * @param folderInfo The FolderInfo describing the folder to open.
     */
    public void openFolder(FolderIcon folderIcon) {
        Folder folder = folderIcon.getFolder();
        FolderInfo info = folder.mInfo;

        info.opened = true;

        // Just verify that the folder hasn't already been added to the DragLayer.
        // There was a one-off crash where the folder had a parent already.
        if (folder.getParent() == null) {
            mDragLayer.addView(folder);
            mDragController.addDropTarget((DropTarget) folder);
        } else {
            Log.w(TAG, "Opening folder (" + folder + ") which already has a parent (" +
                    folder.getParent() + ").");
        }
        folder.animateOpen();
        growAndFadeOutFolderIcon(folderIcon);

        // Notify the accessibility manager that this folder "window" has appeared and occluded
        // the workspace items
        folder.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        getDragLayer().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    }

    public void closeFolder() {
        Folder folder = mWorkspace.getOpenFolder();
        if (folder != null) {
            if (folder.isEditingName()) {
                folder.dismissEditingName();
            }
            closeFolder(folder);
        }
    }

    void closeFolder(Folder folder) {
        folder.getInfo().opened = false;

        ViewGroup parent = (ViewGroup) folder.getParent().getParent();
        if (parent != null) {
            FolderIcon fi = (FolderIcon) mWorkspace.getViewForTag(folder.mInfo);
            shrinkAndFadeInFolderIcon(fi);
        }
        folder.animateClosed();

        // Notify the accessibility manager that this folder "window" has disappeard and no
        // longer occludeds the workspace items
        getDragLayer().sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    public boolean onLongClick(View v) {
        if (!isDraggingEnabled()) return false;
        if (isWorkspaceLocked()) return false;
        if (mState != State.WORKSPACE) return false;

        if (!(v instanceof CellLayout)) {
            v = (View) v.getParent().getParent();
        }

        resetAddInfo();
        CellLayout.CellInfo longClickCellInfo = (CellLayout.CellInfo) v.getTag();
        // This happens when long clicking an item with the dpad/trackball
        if (longClickCellInfo == null) {
            return true;
        }

        // The hotseat touch handling does not go through Workspace, and we always allow long press
        // on hotseat items.
        final View itemUnderLongClick = longClickCellInfo.cell;
        boolean allowLongPress = isHotseatLayout(v) || mWorkspace.allowLongPress();
        if (allowLongPress && !mDragController.isDragging()) {
            if (itemUnderLongClick == null) {
                // User long pressed on empty space
                mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);

                // 画面のタッチ状態を取り消し
                long downTime = SystemClock.uptimeMillis();
                long eventTime = downTime + 100;
                MotionEvent motionEvent = MotionEvent.obtain(
                    downTime,
                    eventTime,
                    MotionEvent.ACTION_UP,
                    0.0f,
                    0.0f,
                    0
                );
                mWorkspace.dispatchTouchEvent(motionEvent);

                showHomeMenu();
            } else {
                if (!(itemUnderLongClick instanceof Folder)) {
                    // User long pressed on an item
                    mWorkspace.startDrag(longClickCellInfo);
                }
            }
        }
        return true;
    }

    boolean isHotseatLayout(View layout) {
        return mHotseat != null && layout != null &&
                (layout instanceof CellLayout) && (layout == mHotseat.getLayout());
    }
    Hotseat getHotseat() {
        return mHotseat;
    }
    SearchDropTargetBar getSearchBar() {
        return mSearchDropTargetBar;
    }

    /**
     * Returns the CellLayout of the specified container at the specified screen.
     */
    CellLayout getCellLayout(long container, int screen) {
        if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            if (mHotseat != null) {
                return mHotseat.getLayout();
            } else {
                return null;
            }
        } else {
            return (CellLayout) mWorkspace.getChildAt(screen);
        }
    }

    Workspace getWorkspace() {
        return mWorkspace;
    }

    // Now a part of LauncherModel.Callbacks. Used to reorder loading steps.
    @Override
    public boolean isAllAppsVisible() {
        return (mState == State.APPS_CUSTOMIZE) || (mOnResumeState == State.APPS_CUSTOMIZE);
    }

    public boolean isPageEditVisible() {
        return (mState == State.PAGE_EDIT) || (mOnResumeState == State.PAGE_EDIT);
    }

    @Override
    public boolean isAllAppsButtonRank(int rank) {
        return mHotseat.isAllAppsButtonRank(rank);
    }

    void disableWallpaperIfInAllApps() {
        // Only disable it if we are in all apps
        if (isAllAppsVisible()) {
            if (mAppsCustomizeTabHost != null &&
                    !mAppsCustomizeTabHost.isTransitioning()) {
                updateWallpaperVisibility(false);
            }
        }
    }

    void updateWallpaperVisibility(boolean visible) {
        int wpflags = visible ? WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER : 0;
        int curflags = getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
        if (wpflags != curflags) {
            getWindow().setFlags(wpflags, WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        }
    }

    private void dispatchOnLauncherTransitionPrepare(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionPrepare(this, animated, toWorkspace);
        }
    }

    private void dispatchOnLauncherTransitionStart(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionStart(this, animated, toWorkspace);
        }

        // Update the workspace transition step as well
        dispatchOnLauncherTransitionStep(v, 0f);
    }

    private void dispatchOnLauncherTransitionStep(View v, float t) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionStep(this, t);
        }
    }

    private void dispatchOnLauncherTransitionEnd(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionEnd(this, animated, toWorkspace);
        }

        // Update the workspace transition step as well
        dispatchOnLauncherTransitionStep(v, 1f);
    }

    /**
     * Things to test when changing the following seven functions.
     *   - Home from workspace
     *          - from center screen
     *          - from other screens
     *   - Home from all apps
     *          - from center screen
     *          - from other screens
     *   - Back from all apps
     *          - from center screen
     *          - from other screens
     *   - Launch app from workspace and quit
     *          - with back
     *          - with home
     *   - Launch app from all apps and quit
     *          - with back
     *          - with home
     *   - Go to a screen that's not the default, then all
     *     apps, and launch and app, and go back
     *          - with back
     *          -with home
     *   - On workspace, long press power and go back
     *          - with back
     *          - with home
     *   - On all apps, long press power and go back
     *          - with back
     *          - with home
     *   - On workspace, power off
     *   - On all apps, power off
     *   - Launch an app and turn off the screen while in that app
     *          - Go back with home key
     *          - Go back with back key  TODO: make this not go to workspace
     *          - From all apps
     *          - From workspace
     *   - Enter and exit car mode (becuase it causes an extra configuration changed)
     *          - From all apps
     *          - From the center workspace
     *          - From another workspace
     */

    /**
     * Zoom the camera out from the workspace to reveal 'toView'.
     * Assumes that the view to show is anchored at either the very top or very bottom
     * of the screen.
     */
    private void showAppsCustomizeHelper(final boolean animated, final boolean springLoaded) {

        cancelStateAnimation();

        final View toView = mAppsCustomizeTabHost;

        Animator workspaceAnim;
        if (springLoaded) {
            workspaceAnim = mWorkspace.getChangeStateAnimation(Workspace.State.NORMAL, animated);
        } else {
            workspaceAnim = mWorkspace.getChangeStateAnimation(Workspace.State.SMALL, animated);
        }

        if (animated) {

            final Resources res = getResources();
            final int duration = res.getInteger(R.integer.config_appsCustomizeShowAnimTime);
            final int fadeDuration = res.getInteger(R.integer.config_appsCustomizeFadeInTime);
            final int startDelay = res.getInteger(R.integer.config_workspaceAppsCustomizeAnimationStagger);

            final LauncherViewPropertyAnimator transAnim = new LauncherViewPropertyAnimator(toView);
            transAnim.translationY(0).
                setDuration(duration).
                setInterpolator(new DecelerateInterpolator());

            toView.setScaleX(1f);
            toView.setScaleY(1f);
            toView.setVisibility(View.VISIBLE);
            toView.setAlpha(1f);

            showViewFromWorkspaceWithAnim(toView, springLoaded, transAnim,
                    fadeDuration, startDelay, workspaceAnim, false);

        } else {
            showViewFromWorkspace(toView, springLoaded, false);
        }
    }

    private void showPageEditCustomizeHelper(final boolean animated, final boolean springLoaded) {

        cancelStateAnimation();

        final View toView = mPageEditScreen;

        Animator workspaceAnim;
        if (springLoaded) {
            workspaceAnim = mWorkspace.getChangeStateAnimation(Workspace.State.NORMAL, animated);
        } else {
            workspaceAnim = mWorkspace.getChangeStateAnimation(Workspace.State.SMALL, animated);
        }

        if (animated) {

            final Resources res = getResources();
            final int duration = res.getInteger(R.integer.config_appsCustomizeShowAnimTime);
            final int fadeDuration = res.getInteger(R.integer.config_appsCustomizeFadeInTime);
            final int startDelay = res.getInteger(R.integer.config_workspaceAppsCustomizeAnimationStagger);

            final LauncherViewPropertyAnimator transAnim = new LauncherViewPropertyAnimator(toView);
            transAnim.translationY(0).
                setDuration(duration).
                setInterpolator(new DecelerateInterpolator());

            toView.setScaleX(1f);
            toView.setScaleY(1f);
            toView.setVisibility(View.VISIBLE);
            toView.setAlpha(1f);

            showViewFromWorkspaceWithAnim(toView, springLoaded, transAnim,
                    fadeDuration, startDelay, workspaceAnim, false);

        } else {
            showViewFromWorkspace(toView, springLoaded, false);
        }
    }

    private void hidePageEditCustomizeHelper(State toState, final boolean animated,
            final boolean springLoaded, final Runnable onCompleteRunnable) {

        cancelStateAnimation();

        final View fromView = mPageEditScreen;

        final Resources res = getResources();

        Animator workspaceAnim = null;
        if (toState == State.WORKSPACE) {
            int stagger = res.getInteger(R.integer.config_appsCustomizeWorkspaceAnimationStagger);
            workspaceAnim = mWorkspace.getChangeStateAnimation(Workspace.State.NORMAL, animated, stagger);
        } else if (toState == State.APPS_CUSTOMIZE_SPRING_LOADED) {
            workspaceAnim = mWorkspace.getChangeStateAnimation(Workspace.State.SPRING_LOADED, animated);
        }

        if (mIsThemeMode) {
            updateWallpaperVisibility(false);
        } else {
            updateWallpaperVisibility(true);
        }

        showHotseat(animated);
        if (animated) {

            final int duration = res.getInteger(R.integer.config_appsCustomizeHideAnimTime);
            final int fadeOutDuration = res.getInteger(R.integer.config_appsCustomizeFadeOutTime);

            final LauncherViewPropertyAnimator transAnim = new LauncherViewPropertyAnimator(fromView);
            transAnim.
                translationY(fromView.getHeight()).
                setDuration(duration).
                setInterpolator(new AccelerateInterpolator());

            hideViewAndShowWorkspaceWithAnim(fromView, transAnim, fadeOutDuration,
                    workspaceAnim, onCompleteRunnable, true);
        } else {
            hideViewAndShowWorkspace(fromView);
        }
    }

    /**
     * Zoom the camera back into the workspace, hiding 'fromView'.
     * This is the opposite of showAppsCustomizeHelper.
     * @param animated If true, the transition will be animated.
     */
    private void hideAppsCustomizeHelper(State toState, final boolean animated,
            final boolean springLoaded, final Runnable onCompleteRunnable) {

        cancelStateAnimation();

        final View fromView = mAppsCustomizeTabHost;

        final Resources res = getResources();

        Animator workspaceAnim = null;
        if (toState == State.WORKSPACE) {
            int stagger = res.getInteger(R.integer.config_appsCustomizeWorkspaceAnimationStagger);
            workspaceAnim = mWorkspace.getChangeStateAnimation(Workspace.State.NORMAL, animated, stagger);
        } else if (toState == State.APPS_CUSTOMIZE_SPRING_LOADED) {
            workspaceAnim = mWorkspace.getChangeStateAnimation(Workspace.State.SPRING_LOADED, animated);
        }

        if (mIsThemeMode) {
            updateWallpaperVisibility(false);
        } else {
            updateWallpaperVisibility(true);
        }

        showHotseat(animated);
        if (animated) {

            final int duration = res.getInteger(R.integer.config_appsCustomizeHideAnimTime);
            final int fadeOutDuration = res.getInteger(R.integer.config_appsCustomizeFadeOutTime);

            final LauncherViewPropertyAnimator transAnim = new LauncherViewPropertyAnimator(fromView);
            transAnim.
                translationY(fromView.getHeight()).
                setDuration(duration).
                setInterpolator(new AccelerateInterpolator());

            hideViewAndShowWorkspaceWithAnim(fromView, transAnim, fadeOutDuration,
                    workspaceAnim, onCompleteRunnable, false);

        } else {
            hideViewAndShowWorkspace(fromView);
        }
    }

    private void cancelStateAnimation() {
        if (mStateAnimation != null) {
            mStateAnimation.setDuration(0);
            mStateAnimation.cancel();
            mStateAnimation = null;
        }
    }

    private void showViewFromWorkspaceWithAnim(final View toView, final boolean springLoaded,
            Animator anim, int fadeDuration, int startDelay, Animator workspaceAnim, final boolean wallpaperVisible) {

        final View fromView = mWorkspace;

        final ValueAnimator valAnim = ValueAnimator.ofFloat(0f, 1f);
        valAnim.setDuration(fadeDuration);
        valAnim.setInterpolator(new DecelerateInterpolator(1.5f));
        valAnim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (animation == null) {
                    throw new RuntimeException("animation is null");
                }
                float t = (Float) animation.getAnimatedValue();
                dispatchOnLauncherTransitionStep(fromView, t);
                dispatchOnLauncherTransitionStep(toView, t);
            }
        });

        // toView should appear right at the end of the workspace shrink
        // animation
        mStateAnimation = ViewAnimUtils.createAnimatorSet();
        mStateAnimation.play(anim).after(startDelay);
        mStateAnimation.play(valAnim).after(startDelay);

        mStateAnimation.addListener(new AnimatorListenerAdapter() {
            boolean animationCancelled = false;

            @Override
            public void onAnimationStart(Animator animation) {
                if (!mIsThemeMode) {
                    updateWallpaperVisibility(true);
                }
                // Prepare the position
                toView.setTranslationX(0.0f);
                toView.setTranslationY(toView.getHeight());
                toView.setVisibility(View.VISIBLE);
                toView.bringToFront();
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                dispatchOnLauncherTransitionEnd(fromView, true, false);
                dispatchOnLauncherTransitionEnd(toView, true, false);

                if (mWorkspace != null && !springLoaded && !LauncherApplication.isScreenLarge()) {
                    // Hide the workspace scrollbar
                    mWorkspace.hideScrollingIndicator(true);
                }
                if (!animationCancelled && !wallpaperVisible) {
                    updateWallpaperVisibility(false);
                }

                mWorkspaceAnim = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animationCancelled = true;
            }
        });

        if (workspaceAnim != null) {
            mStateAnimation.play(workspaceAnim);
            mWorkspaceAnim = workspaceAnim;
        }

        boolean delayAnim = false;

        dispatchOnLauncherTransitionPrepare(fromView, true, false);
        dispatchOnLauncherTransitionPrepare(toView, true, false);

        // If any of the objects being animated haven't been measured/laid out
        // yet, delay the animation until we get a layout pass
        if ((((LauncherTransitionable) toView).getContent().getMeasuredWidth() == 0) ||
                (mWorkspace.getMeasuredWidth() == 0) ||
                (toView.getMeasuredWidth() == 0)) {
            delayAnim = true;
        }

        final AnimatorSet stateAnimation = mStateAnimation;
        final Runnable startAnimRunnable = new Runnable() {
            public void run() {
                // Check that mStateAnimation hasn't changed while
                // we waited for a layout/draw pass
                if (mStateAnimation != stateAnimation)
                    return;
                dispatchOnLauncherTransitionStart(fromView, true, false);
                dispatchOnLauncherTransitionStart(toView, true, false);
                ViewAnimUtils.startAnimationAfterNextDraw(mStateAnimation, toView);
            }
        };
        if (delayAnim) {
            final ViewTreeObserver observer = toView.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                    @SuppressLint("NewApi")
                    @SuppressWarnings("deprecation")
                    public void onGlobalLayout() {
                        startAnimRunnable.run();
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            toView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            toView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                });
        } else {
            startAnimRunnable.run();
        }
    }

    private void showViewFromWorkspace(View toView, boolean springLoaded, final boolean wallpaperVisible) {

        final View fromView = mWorkspace;

        toView.setTranslationX(0.0f);
        toView.setTranslationY(0.0f);
        toView.setScaleX(1.0f);
        toView.setScaleY(1.0f);
        toView.setAlpha(1f);
        toView.setVisibility(View.VISIBLE);
        toView.bringToFront();

        if (!springLoaded && !LauncherApplication.isScreenLarge()) {
            // Hide the workspace scrollbar
            mWorkspace.hideScrollingIndicator(true);
        }

        dispatchOnLauncherTransitionPrepare(fromView, false, false);
        dispatchOnLauncherTransitionStart(fromView, false, false);
        dispatchOnLauncherTransitionEnd(fromView, false, false);
        dispatchOnLauncherTransitionPrepare(toView, false, false);
        dispatchOnLauncherTransitionStart(toView, false, false);
        dispatchOnLauncherTransitionEnd(toView, false, false);
        if (!wallpaperVisible) {
            updateWallpaperVisibility(false);
        }
    }

    private void hideViewAndShowWorkspaceWithAnim(final View fromView, Animator anim, int fadeOutDuration,
            Animator workspaceAnim, final Runnable onCompleteRunnable, final boolean wallpaperVisible) {

        final View toView = mWorkspace;

        final ValueAnimator valAnim = ValueAnimator.ofFloat(0f, 1f);
        valAnim.setDuration(fadeOutDuration);
        valAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        valAnim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = (Float) animation.getAnimatedValue();
                dispatchOnLauncherTransitionStep(fromView, t);
                dispatchOnLauncherTransitionStep(toView, t);
            }
        });

        mStateAnimation = ViewAnimUtils.createAnimatorSet();

        dispatchOnLauncherTransitionPrepare(fromView, true, true);
        dispatchOnLauncherTransitionPrepare(toView, true, true);
        mAppsCustomizeContent.pauseScrolling();

        mStateAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // 念のため
                if (!wallpaperVisible) {
                    if (mIsThemeMode) {
                        updateWallpaperVisibility(false);
                    } else {
                        updateWallpaperVisibility(true);
                    }
                }
                fromView.setVisibility(View.GONE);
                dispatchOnLauncherTransitionEnd(fromView, true, true);
                dispatchOnLauncherTransitionEnd(toView, true, true);
                if (mWorkspace != null) {
                    mWorkspace.hideScrollingIndicator(false);
                }
                if (onCompleteRunnable != null) {
                    mHandler.postDelayed(onCompleteRunnable, 100);
//                    onCompleteRunnable.run();
                }
                mAppsCustomizeContent.updateCurrentPageScroll();
                mAppsCustomizeContent.resumeScrolling();

                mWorkspaceAnim = null;
            }
        });

        mStateAnimation.playTogether(anim, valAnim);
        if (workspaceAnim != null) {
            mStateAnimation.play(workspaceAnim);
            mWorkspaceAnim = workspaceAnim;
        }
        dispatchOnLauncherTransitionStart(fromView, true, true);
        dispatchOnLauncherTransitionStart(toView, true, true);
        ViewAnimUtils.startAnimationAfterNextDraw(mStateAnimation, toView);
    }

    private Animator mWorkspaceAnim = null;

    private void hideViewAndShowWorkspace(View fromView) {

        final View toView = mWorkspace;

        fromView.setVisibility(View.GONE);
        dispatchOnLauncherTransitionPrepare(fromView, false, true);
        dispatchOnLauncherTransitionStart(fromView, false, true);
        dispatchOnLauncherTransitionEnd(fromView, false, true);
        dispatchOnLauncherTransitionPrepare(toView, false, true);
        dispatchOnLauncherTransitionStart(toView, false, true);
        dispatchOnLauncherTransitionEnd(toView, false, true);
        mWorkspace.hideScrollingIndicator(false);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            mAppsCustomizeTabHost.onTrimMemory();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!hasFocus) {
            // When another window occludes launcher (like the notification shade, or recents),
            // ensure that we enable the wallpaper flag so that transitions are done correctly.
            updateWallpaperVisibility(true);
        } else {
            // When launcher has focus again, disable the wallpaper if we are in AllApps
            mWorkspace.postDelayed(new Runnable() {
                @Override
                public void run() {
                    disableWallpaperIfInAllApps();
                    if (mIsThemeMode) {
                        updateWallpaperVisibility(false);
                    }
                }
            }, 500);
        }
    }

    void showWorkspace(boolean animated) {
        showWorkspace(animated, null);
    }

    void showWorkspace(boolean animated, Runnable onCompleteRunnable) {
        if (mState != State.WORKSPACE) {
//            boolean wasInSpringLoadedMode = (mState == State.APPS_CUSTOMIZE_SPRING_LOADED);
            mWorkspace.setVisibility(View.VISIBLE);
            hideAppsCustomizeHelper(State.WORKSPACE, animated, false, onCompleteRunnable);

            // Set focus to the AppsCustomize button
            if (mAllAppsButton != null) {
                mAllAppsButton.requestFocus();
            }
        }

        mWorkspace.flashScrollingIndicator(animated);

        // Change the state *after* we've called all the transition code
        mState = State.WORKSPACE;

        // Resume the auto-advance of widgets
        mUserPresent = true;
        updateRunning();

        // Send an accessibility event to announce the context change
        getWindow().getDecorView()
                .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void hidePageEdit(boolean animated, Runnable onCompleteRunnable) {

        if (mState != State.WORKSPACE) {
//            boolean wasInSpringLoadedMode = (mState == State.APPS_CUSTOMIZE_SPRING_LOADED);
            mWorkspace.setVisibility(View.VISIBLE);
            hidePageEditCustomizeHelper(State.WORKSPACE, animated, false, onCompleteRunnable);

            // Set focus to the AppsCustomize button
            if (mAllAppsButton != null) {
                mAllAppsButton.requestFocus();
            }
        }

        mWorkspace.flashScrollingIndicator(animated);

        mDragController.addDropTarget(mWorkspace);
        mSearchDropTargetBar.enableDeleteDropTarget(mDragController);

        // Change the state *after* we've called all the transition code
        mState = State.WORKSPACE;

        // Resume the auto-advance of widgets
        mUserPresent = true;
        updateRunning();

        // Send an accessibility event to announce the context change
        getWindow().getDecorView()
                .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void showPageEdit(boolean animated) {
        if (mState != State.WORKSPACE) return;

        // TODO:テーマ適用中は画面を開かない(判定方法を変更する)
        if (mIsThemeMode && mWorkspace.getPanelWallpapers() == null) return;

        // 現在のWorkspaceのキャプチャ画像をPageEditScreenに渡す
        mPageEditScreen.init(generateWorkspaceCaptureImages());

        showPageEditCustomizeHelper(animated, false);
//        mAppsCustomizeTabHost.requestFocus();

        mSearchDropTargetBar.disableDeleteDropTarget(mDragController);
        mDragController.removeDropTarget(mWorkspace);

        // Change the state *after* we've called all the transition code
        mState = State.PAGE_EDIT;

        // Pause the auto-advance of widgets until we are out of AllApps
        mUserPresent = false;
        updateRunning();
        closeFolder();

        // Send an accessibility event to announce the context change
        getWindow().getDecorView()
                .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private Bitmap[] generateWorkspaceCaptureImages() {

        final int pageCount = mWorkspace.getPageCount();

        Bitmap[] pageBmps = new Bitmap[pageCount];

        mWorkspace.setDrawingCacheEnabled(true);

        int scrollX = mWorkspace.getScrollX();

        int w = mWorkspace.getMeasuredWidth();
        int ps = mWorkspace.getPageSpacing();

        // システム壁紙の時に画面が一瞬スクロールするのを防ぐ
        if (!mIsThemeMode) {
            mWorkspace.setWillNotDraw(true);
        }

        for (int i = 0; i < pageCount; i++) {

            mWorkspace.setScrollX(i * (w + ps));

            mWorkspace.buildDrawingCache();

            int cw = mWorkspace.getDrawingCache().getWidth();
            int ch = mWorkspace.getDrawingCache().getHeight();
            int rw = cw / 3;
            int rh = ch / 3;

            pageBmps[i] = BitmapUtils.resizeBitmap(mWorkspace.getDrawingCache(), rw, rh, new Matrix(), false);
        }

        mWorkspace.setDrawingCacheEnabled(false);

        if (!mIsThemeMode) {
            mWorkspace.setWillNotDraw(false);
        }
        mWorkspace.setScrollX(scrollX);

        return pageBmps;
    }

    void showAllApps(boolean animated) {
        if (mState != State.WORKSPACE) return;

        // WorkspaceAnimのDelayが終わるまでは待つ。
        // WorkspaceAnimが開始されないと、
        // 後のStateAnimのキャンセルでWorkspaceAnimのキャンセル処理が実行されない
        if (mWorkspaceAnim != null) {
            if (!mWorkspaceAnim.isRunning()) {
                return;
            }
        }

        showAppsCustomizeHelper(animated, false);
        mAppsCustomizeTabHost.requestFocus();

        // Change the state *after* we've called all the transition code
        mState = State.APPS_CUSTOMIZE;

        // Pause the auto-advance of widgets until we are out of AllApps
        mUserPresent = false;
        updateRunning();
        closeFolder();

        // Send an accessibility event to announce the context change
        getWindow().getDecorView()
                .sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void enterSpringLoadedDragMode() {
        if (isAllAppsVisible()) {
            hideAppsCustomizeHelper(State.APPS_CUSTOMIZE_SPRING_LOADED, true, true, null);
            mState = State.APPS_CUSTOMIZE_SPRING_LOADED;
        }
    }

    void exitSpringLoadedDragModeDelayed(final boolean successfulDrop, boolean extendedDelay,
            final Runnable onCompleteRunnable) {
        if (mState != State.APPS_CUSTOMIZE_SPRING_LOADED) return;

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (successfulDrop) {
                    // Before we show workspace, hide all apps again because
                    // exitSpringLoadedDragMode made it visible. This is a bit hacky; we should
                    // clean up our state transition functions
                    mAppsCustomizeTabHost.setVisibility(View.GONE);
                    showWorkspace(true, onCompleteRunnable);
                } else {
                    exitSpringLoadedDragMode();
                }
            }
        }, (extendedDelay ?
                EXIT_SPRINGLOADED_MODE_LONG_TIMEOUT :
                EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT));
    }

    void exitSpringLoadedDragMode() {
        if (mState == State.APPS_CUSTOMIZE_SPRING_LOADED) {
            final boolean animated = true;
            final boolean springLoaded = true;
            showAppsCustomizeHelper(animated, springLoaded);
            mState = State.APPS_CUSTOMIZE;
        }
        // Otherwise, we are not in spring loaded mode, so don't do anything.
    }

    void lockAllApps() {
        // TODO
    }

    void unlockAllApps() {
        // TODO
    }

    /**
     * Shows the hotseat area.
     */
    void showHotseat(boolean animated) {
        if (!LauncherApplication.isScreenLarge()) {
            if (animated) {
                if (mHotseat.getAlpha() != 1f) {
                    int duration = 0;
                    if (mSearchDropTargetBar != null) {
                        duration = mSearchDropTargetBar.getTransitionInDuration();
                    }
                    mHotseat.animate().alpha(1f).setDuration(duration);
                }
            } else {
                mHotseat.setAlpha(1f);
            }
        }
    }

    /**
     * Hides the hotseat area.
     */
    void hideHotseat(boolean animated) {
        if (!LauncherApplication.isScreenLarge()) {
            if (animated) {
                if (mHotseat.getAlpha() != 0f) {
                    int duration = 0;
                    if (mSearchDropTargetBar != null) {
                        duration = mSearchDropTargetBar.getTransitionOutDuration();
                    }
                    mHotseat.animate().alpha(0f).setDuration(duration);
                }
            } else {
                mHotseat.setAlpha(0f);
            }
        }
    }

    /**
     * Add an item from all apps or customize onto the given workspace screen.
     * If layout is null, add to the current screen.
     */
    void addExternalItemToScreen(ItemInfo itemInfo, final CellLayout layout) {
        if (!mWorkspace.addExternalItemToScreen(itemInfo, layout)) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        final boolean result = super.dispatchPopulateAccessibilityEvent(event);
        final List<CharSequence> text = event.getText();
        text.clear();
        // Populate event with a fake title based on the current state.
        if (mState == State.APPS_CUSTOMIZE) {
            text.add(getString(R.string.all_apps_button_label));
        } else {
            text.add(getString(R.string.all_apps_home_button_label));
        }
        return result;
    }

    /**
     * Receives notifications when system dialogs are to be closed.
     */
    private class CloseSystemDialogsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            closeSystemDialogs();
        }
    }

    /**
     * Receives notifications whenever the appwidgets are reset.
     */
    private class AppWidgetResetObserver extends ContentObserver {
        public AppWidgetResetObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            onAppWidgetReset();
        }
    }

    /**
     * If the activity is currently paused, signal that we need to run the passed Runnable
     * in onResume.
     *
     * This needs to be called from incoming places where resources might have been loaded
     * while we are paused.  That is becaues the Configuration might be wrong
     * when we're not running, and if it comes back to what it was when we
     * were paused, we are not restarted.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return true if we are currently paused.  The caller might be able to
     * skip some work in that case since we will come back again.
     */
    private boolean waitUntilResume(Runnable run, boolean deletePreviousRunnables) {
        if (mPaused) {
            Log.i(TAG, "Deferring update until onResume");
            if (deletePreviousRunnables) {
                while (mOnResumeCallbacks.remove(run)) {
                }
            }
            mOnResumeCallbacks.add(run);
            return true;
        } else {
            return false;
        }
    }

    private boolean waitUntilResume(Runnable run) {
        return waitUntilResume(run, false);
    }

    /**
     * If the activity is currently paused, signal that we need to re-run the loader
     * in onResume.
     *
     * This needs to be called from incoming places where resources might have been loaded
     * while we are paused.  That is becaues the Configuration might be wrong
     * when we're not running, and if it comes back to what it was when we
     * were paused, we are not restarted.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return true if we are currently paused.  The caller might be able to
     * skip some work in that case since we will come back again.
     */
    public boolean setLoadOnResume() {
        if (mPaused) {
            Log.i(TAG, "setLoadOnResume");
            mOnResumeNeedsLoad = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public int getCurrentWorkspaceScreen() {
        if (mWorkspace != null) {
            return mWorkspace.getCurrentPage();
        } else {
            return 0;
        }
    }

    @Override
    public void initWorkspacePages(int numPages, int defaultPage) {

        mDefaultPage = defaultPage;

        mWorkspace.initPages(numPages, defaultPage);
    }

    @Override
    public void setThemeWallpapers(String[] wpPaths, PanelWallpapers panelWps) {

        mIsThemeMode = true;

        mWorkspace.setThemeBackgrounds(panelWps);

        turnOnWallPaperChangeDetector();

        mWorkspace.invalidate();
        updateWallpaperVisibility(false);
    }

    @Override
    public void applyThemeWallpaper() {

        mIsThemeMode = true;

        turnOnWallPaperChangeDetector();

        mWorkspace.invalidate();
        updateWallpaperVisibility(false);
    }

    @Override
    public void createThemeShortcuts(ArrayList<ThemeShortcutDef> themeShortcutDefs) {

        if (themeShortcutDefs == null) return;

        PackageManager pm = getPackageManager();

        for (ThemeShortcutDef shortcutDef : themeShortcutDefs) {
            try {
                if (!mWorkspace.isOccupied(shortcutDef.screen, shortcutDef.cellX, shortcutDef.cellY)) {

                    if (shortcutDef.iconName != null) {
                        createKisekaeShortcut(this, pm,
                                shortcutDef.iconName,
                                shortcutDef.screen,
                                shortcutDef.cellX,
                                shortcutDef.cellY);
                    } else {
                        createShortcutForTheme(this, pm,
                                shortcutDef.packageName,
                                shortcutDef.className,
                                LauncherSettings.Favorites.CONTAINER_DESKTOP,
                                shortcutDef.screen,
                                shortcutDef.cellX,
                                shortcutDef.cellY);
                    }
                }
            } catch (RuntimeException e) {
                // 位置指定値不正
            }
        }
    }

    @Override
    public void setThemeWidgets(ArrayList<DummyInfo> widgetInfos) {

        if (widgetInfos == null) return;

        final int infoCount = widgetInfos.size();
        if (infoCount == 0) return;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            // Android 4.0ではガイドを配置
            for (DummyInfo info : widgetInfos) {
                createDummyItem(info);
            }
        } else {
            mPendingWidgetInfoList = new ArrayList<DummyInfo>();
            if (infoCount  > 1) {
                for (int i = 1; i < infoCount; i++) {
                    mPendingWidgetInfoList.add(widgetInfos.get(i));
                }
            }
            final DummyInfo widgetInfo = widgetInfos.get(0);
            if (!addAppWidgetFromDummy(null, widgetInfo, null, false)) {
                createDummyItem(widgetInfo);
            }
        }
    }

    @Override
    public void setThemeResources(String themeId, boolean isThemeMode, boolean inAppTheme, String[] wpPaths, PanelWallpapers panelWps,
            Bitmap drawerBg, int pageCount, int defaultPage, Bitmap drawerIconBmp, String iconThemeId, boolean iconInAppTheme,
            Boolean pageLoop, String drawerTextColor, boolean applied, ArrayList<DummyInfo> widgetInfos, ArrayList<ThemeShortcutDef> themeShortcutDefs) {

        mHasThemeResLoaded = true;

        mIsThemeMode = isThemeMode;
        mThemeId = themeId;
        mInAppTheme = inAppTheme;
        mIconThemeId = iconThemeId;
        mIconInAppTheme = iconInAppTheme;
        if (pageLoop != null) {
            mPageLoop = pageLoop;
            mWorkspace.setLooping(pageLoop);
        }
        mDefaultPage = defaultPage;

//        if (applied) {
//            // ページを削除する前に行っておく必要がある
//            mWorkspace.removeAllItems();
//        }

        // 前テーマの壁紙画像を解放
        PanelWallpapers oldPanelWps = mWorkspace.getPanelWallpapers();
        if (oldPanelWps != null) {
            mWorkspace.setPanelWallpapers(null);
            oldPanelWps.unload();
        }

        // ページの増減を実行
        final int currentPageCount = mWorkspace.getPageCount();
        if (pageCount > currentPageCount) {
            int len = pageCount - currentPageCount;
            for (int i = 0; i < len; i++) {
                mWorkspace.addPage();
            }
        }
//        else if (pageCount < currentPageCount) {
//            for (int i = currentPageCount - 1; i >= pageCount; i--) {
//                mWorkspace.removePage(i);
//            }
//        }

        if (applied) {
            // ページが追加されてから行う必要がある

//            // 初期アイコン配置
//            initThemeShortcutIcon();

            // ショートカット配置
            createThemeShortcuts(themeShortcutDefs);

            // テーマウィジェット配置
            setThemeWidgets(widgetInfos);
        }

        mWorkspace.setDefaultPage(defaultPage);

        if (applied) {

            final int defaultPageTmp = defaultPage;
            // この段階ではまだページ追加に伴う状態の変更がされていない可能性があるので、
            // Handlerで実行
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWorkspace.setCurrentPage(defaultPageTmp);
                }
            });
        }

        if (isThemeMode) {

            // 壁紙が変更されていればテーマモードをOFFにする
            if (mWpChangeDetector.isReady()
                    && mWpChangeDetector.hasWallpaperChanged(this, R.drawable.default_wallpaper)) {

                mIsThemeMode = false;
                KisekaeLauncherModel.disableThemeInDatabase(this);
                synchronized (KisekaeLauncherModel.sBgLock) {
                    KisekaeLauncherModel.sThemeMode = 0;
                }
                updateWallpaperVisibility(true);
                mWorkspace.disableThemeMode();
                mWorkspace.invalidate();

                panelWps.unload();

                mWpChangeDetector.turnOff();

            } else {

                mWorkspace.setThemeBackgrounds(panelWps);
                turnOnWallPaperChangeDetector();

                updateWallpaperVisibility(false);
            }

        } else {
             updateWallpaperVisibility(true);
        }

        mHotseat.setDrawerIcon(drawerIconBmp);

        mAppsCustomizeTabHost.setThemeBackground(drawerBg);
        mAppsCustomizeContent.setThemeTextColor(drawerTextColor);
        mAppsCustomizeContent.updateApplicationInfoIcons();
        mAppsCustomizeContent.updatePageIcons();

        mWorkspace.updateShortcutIcons();

        if (mAddThemeWdiget) {
            mAddThemeWdiget = false;
            addThemeTandokuWidget();
        }
        if (mApplyTheme) {
            mApplyTheme = false;
            applyTheme(mThemeIdForApplying, mInAppThemeForApplying);
        }
        if (mApplyThemeWallpaper) {
            mApplyThemeWallpaper = false;

            PanelWallpapers panelWps2 = mWorkspace.getPanelWallpapers();
            if (panelWps2 == null) {
                panelWps2 = new PanelWallpapers(new String[Workspace.MAX_SCREEN_COUNT]);
                mWorkspace.setThemeBackgrounds(panelWps2);
            }

            mModel.applyThemeWallpaper(this, ContentsOperator.op.getIndividualWpPath(this),
                    panelWps2, mWorkspace.getPageCount());
        }

        mWorkspace.invalidate();
    }

    private void turnOnWallPaperChangeDetector() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mWpChangeDetector.turnOn(Launcher.this);
            }
        }, 100);
    }

    @Override
    public void setKisekaeIcons() {
        mAppsCustomizeContent.updateApplicationInfoIcons();
        mAppsCustomizeContent.updatePageIcons();
        mWorkspace.updateShortcutIcons();
        mWorkspace.invalidate();
        mHotseat.invalidate();
        mAppsCustomizeContent.invalidate();
    }

    @Override
//    public void setKisekaeIconResources(String iconThemeId, boolean iconInAppTheme, Bitmap drawerIconBmp) {
      public void setKisekaeIconResources(String iconThemeId, boolean iconInAppTheme) {

        mIconThemeId = iconThemeId;
        mIconInAppTheme = iconInAppTheme;

//        mHotseat.setDrawerIcon(drawerIconBmp);
        mAppsCustomizeContent.updateApplicationInfoIcons();
        mAppsCustomizeContent.updatePageIcons();
        mWorkspace.updateShortcutIcons();
        mWorkspace.invalidate();
        mHotseat.invalidate();
        mAppsCustomizeContent.invalidate();
    }

    /**
     * Refreshes the shortcuts shown on the workspace.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void startBinding() {
        // If we're starting binding all over again, clear any bind calls we'd postponed in
        // the past (see waitUntilResume) -- we don't need them since we're starting binding
        // from scratch again
        mOnResumeCallbacks.clear();

        final Workspace workspace = mWorkspace;
        mNewShortcutAnimatePage = -1;
        mNewShortcutAnimateViews.clear();
        mWorkspace.clearDropTargets();
        int count = workspace.getChildCount();
        for (int i = 0; i < count; i++) {
            // Use removeAllViewsInLayout() to avoid an extra requestLayout() and invalidate().
            final CellLayout layoutParent = (CellLayout) workspace.getChildAt(i);
            layoutParent.removeAllViewsInLayout();
        }
        mWidgetsToAdvance.clear();
        if (mHotseat != null) {
            mHotseat.resetLayout();
        }
    }

    /**
     * Bind the items start-end from the list.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindItems(final ArrayList<ItemInfo> shortcuts, final int start, final int end) {
        if (waitUntilResume(new Runnable() {
                public void run() {
                    bindItems(shortcuts, start, end);
                }
            })) {
            return;
        }

        // Get the list of added shortcuts and intersect them with the set of shortcuts here
        Set<String> newApps = new HashSet<String>();
        newApps = mSharedPrefs.getStringSet(InstallShortcutReceiver.NEW_APPS_LIST_KEY, newApps);

        Workspace workspace = mWorkspace;
        for (int i = start; i < end; i++) {
            final ItemInfo item = shortcuts.get(i);

            // Short circuit if we are loading dock items for a configuration which has no dock
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
                    mHotseat == null) {
                continue;
            }

            switch (item.itemType) {
                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    ShortcutInfo info = (ShortcutInfo) item;
                    String uri = info.intent.toUri(0).toString() + "|" + info.screen + "|" + info.cellX + "|" + info.cellY;
                    View shortcut = createShortcut(info);
                    workspace.addInScreen(shortcut, item.container, item.screen, item.cellX,
                            item.cellY, Workspace.ICON_SPAN, Workspace.ICON_SPAN, false);
                    boolean animateIconUp = false;
                    synchronized (newApps) {
                        if (newApps.contains(uri)) {
                            animateIconUp = newApps.remove(uri);
                        }
                    }
                    if (animateIconUp) {
                        // Prepare the view to be animated up
                        shortcut.setAlpha(0f);
                        shortcut.setScaleX(0f);
                        shortcut.setScaleY(0f);
                        mNewShortcutAnimatePage = item.screen;
                        if (!mNewShortcutAnimateViews.contains(shortcut)) {
                            mNewShortcutAnimateViews.add(shortcut);
                        }
                    }
                    break;
                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                    CellLayout layout = (CellLayout) workspace.getChildAt(workspace.getCurrentPage());
                FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon,
                        this, layout, (FolderInfo) item, mIconCache,
                        layout.getFolderIconSize(),
                        layout.getFolderCellPaddingTop(),
                        layout.getScale(),
                        layout.getCellWidth() * Workspace.ICON_SPAN,
                        layout.getCellPaddingTop());
                workspace.addInScreen(newFolder, item.container, item.screen,
                        item.cellX, item.cellY, Workspace.ICON_SPAN, Workspace.ICON_SPAN, false);
                    break;
            }
        }

        workspace.requestLayout();
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindFolders(final HashMap<Long, FolderInfo> folders) {
        if (waitUntilResume(new Runnable() {
                public void run() {
                    bindFolders(folders);
                }
            })) {
            return;
        }
        sFolders.clear();
        sFolders.putAll(folders);
    }

    @Override
    public void bindDummyItem(final DummyInfo dummyInfo) {
        if (waitUntilResume(new Runnable() {
            public void run() {
                bindDummyItem(dummyInfo);
                }
            })) {
            return;
        }

        DummyHomeItemView dummy = new DummyHomeItemView(this);
        dummy.applyFromDummyInfo(dummyInfo);
        dummy.setOnClickListener(mDummyOnClickListener);

        dummy.updateState();

        mWorkspace.addInScreen(dummy, dummyInfo.container, dummyInfo.screen,
                dummyInfo.cellX, dummyInfo.cellY, dummyInfo.spanX, dummyInfo.spanY);

        mWorkspace.requestLayout();
    }

    /**
     * Add the views for a widget to the workspace.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppWidget(final LauncherAppWidgetInfo item) {
        if (waitUntilResume(new Runnable() {
                public void run() {
                    bindAppWidget(item);
                }
            })) {
            return;
        }

        final long start = DEBUG_WIDGETS ? SystemClock.uptimeMillis() : 0;
        if (DEBUG_WIDGETS) {
            DebugLog.instance.outputLog(TAG, "bindAppWidget: " + item);
        }
        final Workspace workspace = mWorkspace;

        final int appWidgetId = item.appWidgetId;
        final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo == null) {
            // ここにきている時点で通常はありえないが念のため。
            return;
        }

        if (DEBUG_WIDGETS) {
            DebugLog.instance.outputLog(TAG, "bindAppWidget: id=" + item.appWidgetId + " belongs to component " + appWidgetInfo.provider);
        }

        item.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
        // WidgetのHostViewごとにIDを割り振る
        // 通常はありえない想定だが、HostViewが重なって描画されるときに、
        // IDが同じだと下記の例外が発生する場合がある。
        // java.lang.IllegalArgumentException:
        // Wrong state class, expecting View State but received class android.appwidget.AppWidgetHostView$ParcelableSparseArray instead.
        // This usually happens when two views of different type have the same id in the same hierarchy.
        // This view's id is NO_ID. Make sure other views do not use the same id.
        item.hostView.setId(mWidgetHostViewCount);
        mWidgetHostViewCount++;

        item.hostView.setTag(item);
        item.onBindAppWidget(this);

        workspace.addInScreen(item.hostView, item.container, item.screen, item.cellX,
                item.cellY, item.spanX, item.spanY, false);
        addWidgetToAutoAdvanceIfNeeded(item.hostView, appWidgetInfo);

        workspace.requestLayout();

        if (DEBUG_WIDGETS) {
            DebugLog.instance.outputLog(TAG, "bound widget id="+item.appWidgetId+" in "
                    + (SystemClock.uptimeMillis()-start) + "ms");
        }
    }

    public void onPageBoundSynchronously(int page) {
        mSynchronouslyBoundPages.add(page);
    }

    /**
     * Callback saying that there aren't any more items to bind.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void finishBindingItems() {
        if (waitUntilResume(new Runnable() {
                public void run() {
                    finishBindingItems();
                }
            })) {
            return;
        }
        if (mSavedState != null) {
            if (!mWorkspace.hasFocus()) {
                mWorkspace.getChildAt(mWorkspace.getCurrentPage()).requestFocus();
            }
            mSavedState = null;
        }

        mWorkspace.restoreInstanceStateForRemainingPages();

        // If we received the result of any pending adds while the loader was running (e.g. the
        // widget configuration forced an orientation change), process them now.
        for (int i = 0; i < sPendingAddList.size(); i++) {
            completeAdd(sPendingAddList.get(i));
        }
        sPendingAddList.clear();

        // Animate up any icons as necessary
        if (mVisible || mWorkspaceLoading) {
            Runnable newAppsRunnable = new Runnable() {
                @Override
                public void run() {
                    runNewAppsAnimation(false);
                }
            };

            boolean willSnapPage = mNewShortcutAnimatePage > -1 &&
                    mNewShortcutAnimatePage != mWorkspace.getCurrentPage();
            if (canRunNewAppsAnimation()) {
                // If the user has not interacted recently, then either snap to the new page to show
                // the new-apps animation or just run them if they are to appear on the current page
                if (willSnapPage) {
                    mWorkspace.snapToPage(mNewShortcutAnimatePage, newAppsRunnable);
                } else {
                    runNewAppsAnimation(false);
                }
            } else {
                // If the user has interacted recently, then just add the items in place if they
                // are on another page (or just normally if they are added to the current page)
                runNewAppsAnimation(willSnapPage);
            }
        }

        mWorkspaceLoading = false;
    }

    private boolean canRunNewAppsAnimation() {
        long diff = System.currentTimeMillis() - mDragController.getLastGestureUpTime();
        return diff > (NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Runs a new animation that scales up icons that were added while Launcher was in the
     * background.
     *
     * @param immediate whether to run the animation or show the results immediately
     */
    private void runNewAppsAnimation(boolean immediate) {
        AnimatorSet anim = ViewAnimUtils.createAnimatorSet();
        Collection<Animator> bounceAnims = new ArrayList<Animator>();

        // Order these new views spatially so that they animate in order
        Collections.sort(mNewShortcutAnimateViews, new Comparator<View>() {
            @Override
            public int compare(View a, View b) {
                CellLayout.LayoutParams alp = (CellLayout.LayoutParams) a.getLayoutParams();
                CellLayout.LayoutParams blp = (CellLayout.LayoutParams) b.getLayoutParams();
                int cellCountX = LauncherModel.getCellCountX();
                return (alp.cellY * cellCountX + alp.cellX) - (blp.cellY * cellCountX + blp.cellX);
            }
        });

        // Animate each of the views in place (or show them immediately if requested)
        if (immediate) {
            for (View v : mNewShortcutAnimateViews) {
                v.setAlpha(1f);
                v.setScaleX(1f);
                v.setScaleY(1f);
            }
        } else {
            for (int i = 0; i < mNewShortcutAnimateViews.size(); ++i) {
                View v = mNewShortcutAnimateViews.get(i);
                ValueAnimator bounceAnim = ViewAnimUtils.ofPropertyValuesHolder(v,
                        PropertyValuesHolder.ofFloat("alpha", 1f),
                        PropertyValuesHolder.ofFloat("scaleX", 1f),
                        PropertyValuesHolder.ofFloat("scaleY", 1f));
                bounceAnim.setDuration(InstallShortcutReceiver.NEW_SHORTCUT_BOUNCE_DURATION);
                bounceAnim.setStartDelay(i * InstallShortcutReceiver.NEW_SHORTCUT_STAGGER_DELAY);
                bounceAnim.setInterpolator(new SmoothPagedView.OvershootInterpolator());
                bounceAnims.add(bounceAnim);
            }
            anim.playTogether(bounceAnims);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mWorkspace != null) {
                        mWorkspace.postDelayed(mBuildLayersRunnable, 500);
                    }
                }
            });
            anim.start();
        }

        // Clean up
        mNewShortcutAnimatePage = -1;
        mNewShortcutAnimateViews.clear();
        new Thread("clearNewAppsThread") {
            public void run() {
                mSharedPrefs.edit()
                            .putInt(InstallShortcutReceiver.NEW_APPS_PAGE_KEY, -1)
                            .putStringSet(InstallShortcutReceiver.NEW_APPS_LIST_KEY, null)
                            .commit();
            }
        }.start();
    }

    /**
     * Add the icons for all apps.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAllApplications(final ArrayList<ApplicationInfo> apps) {
        Runnable setAllAppsRunnable = new Runnable() {
            public void run() {
                if (mAppsCustomizeContent != null) {
                    mAppsCustomizeContent.setApps(apps);
                }
            }
        };

        // Remove the progress bar entirely; we could also make it GONE
        // but better to remove it since we know it's not going to be used
        View progressBar = mAppsCustomizeTabHost.
            findViewById(R.id.apps_customize_progress_bar);
        if (progressBar != null) {
            ((ViewGroup)progressBar.getParent()).removeView(progressBar);

            // We just post the call to setApps so the user sees the progress bar
            // disappear-- otherwise, it just looks like the progress bar froze
            // which doesn't look great
            mAppsCustomizeTabHost.post(setAllAppsRunnable);
        } else {
            // If we did not initialize the spinner in onCreate, then we can directly set the
            // list of applications without waiting for any progress bars views to be hidden.
            setAllAppsRunnable.run();
        }
    }

    /**
     * A package was installed.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsAdded(final ArrayList<ApplicationInfo> apps) {
        if (waitUntilResume(new Runnable() {
                public void run() {
                    bindAppsAdded(apps);
                }
            })) {
            return;
        }


        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.addApps(apps);
        }
    }

    /**
     * A package was updated.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsUpdated(final ArrayList<ApplicationInfo> apps) {
        if (waitUntilResume(new Runnable() {
                public void run() {
                    bindAppsUpdated(apps);
                }
            })) {
            return;
        }

        if (mWorkspace != null) {
            mWorkspace.updateShortcuts(apps);
        }

        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.updateApps(apps);
        }
    }

    /**
     * A package was uninstalled.  We take both the super set of packageNames
     * in addition to specific applications to remove, the reason being that
     * this can be called when a package is updated as well.  In that scenario,
     * we only remove specific components from the workspace, where as
     * package-removal should clear all items by package name.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindComponentsRemoved(final ArrayList<String> packageNames,
                                      final ArrayList<ApplicationInfo> appInfos,
                                      final boolean matchPackageNamesOnly) {
        if (waitUntilResume(new Runnable() {
            public void run() {
                bindComponentsRemoved(packageNames, appInfos, matchPackageNamesOnly);
            }
        })) {
            return;
        }

        if (matchPackageNamesOnly) {
            mWorkspace.removeItemsByPackageName(packageNames);
        } else {
            mWorkspace.removeItemsByApplicationInfo(appInfos);
        }

        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.removeApps(appInfos);
        }

        // Notify the drag controller
        mDragController.onAppsRemoved(appInfos, this);
    }

    /**
     * A number of packages were updated.
     */

    private ArrayList<Object> mWidgetsAndShortcuts;
    private Runnable mBindPackagesUpdatedRunnable = new Runnable() {
            public void run() {
                bindPackagesUpdated(mWidgetsAndShortcuts);
                mWidgetsAndShortcuts = null;
            }
        };

    public void bindPackagesUpdated(final ArrayList<Object> widgetsAndShortcuts) {
        if (waitUntilResume(mBindPackagesUpdatedRunnable, true)) {
            mWidgetsAndShortcuts = widgetsAndShortcuts;
            return;
        }

        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.onPackagesUpdated(widgetsAndShortcuts);
        }
    }

    public void updateBadgeInfo() {
        mWorkspace.updateBadgeInfo();
        mAppsCustomizeContent.updatePageIcons();
    }

    private boolean mFromDummy = false;
    private View mPendingDummyView;

    void startAppWidgetPick(boolean fromDummy, View dummyView) {

        if (mOpenWidgetPicker) return;

        mOpenWidgetPicker = true;

        if (fromDummy) {
            mFromDummy = true;
            mPendingDummyView = dummyView;
        }

        int appWidgetId = mAppWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        startActivityForResultSafely(pickIntent, REQUEST_PICK_APPWIDGET_ICS);
    }

    private void configureWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResultSafely(intent, REQUEST_CREATE_APPWIDGET_ICS);
        } else {
            showWorkspace(true);
            completeAddAppWidget(appWidgetId, LauncherSettings.Favorites.CONTAINER_DESKTOP,
                    mWorkspace.getCurrentPage(), null, appWidgetInfo);
        }
    }

    void startShortcutKisekaeActivity(ShortcutInfo info) {
        Intent i = new Intent(getApplicationContext(), IconPickerActivity.class);
        i.putExtra(Intent.EXTRA_SHORTCUT_NAME, info.title);
        i.putExtra(Intent.EXTRA_SHORTCUT_INTENT, info.intent);
        if (info.shortcutName != null) {
            i.putExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_NAME_USER, info.shortcutName);
        }
        i.putExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_SCREEN, info.screen);
        i.putExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_CELLX, info.cellX);
        i.putExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_CELLY, info.cellY);
        i.putExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_ID, info.id);

        startActivitySafely(i);
    }

    void startBatteryWidgetSkinChangeActivity(int appWidgetId) {
        Intent i = new Intent(getApplicationContext(), WidgetPickerActivity.class);
        i.putExtra("AppWidgetId", appWidgetId);
        startActivitySafely(i);
    }

    private void showSystemSettingActivity() {
        startActivitySafely(new Intent(Settings.ACTION_SETTINGS));
    }

    private void applyTheme(String themeId, boolean inAppTheme) {

        // 現在のリソースを解放
        PanelWallpapers panelWps = mWorkspace.getPanelWallpapers();
        if (panelWps != null) {
            mWorkspace.setPanelWallpapers(null);
            panelWps.unload();
        }
        mAppsCustomizeTabHost.recycleBg();

        mModel.applyTheme(this, themeId, inAppTheme, mWorkspace.getPageCount(), mDefaultPage);
    }

    /**
     * Prints out out state for debugging.
     */
    public void dumpState() {
        DebugLog.instance.outputLog(TAG, "BEGIN launcher2 dump state for launcher " + this);
        DebugLog.instance.outputLog(TAG, "mSavedState=" + mSavedState);
        DebugLog.instance.outputLog(TAG, "mWorkspaceLoading=" + mWorkspaceLoading);
        DebugLog.instance.outputLog(TAG, "mRestoring=" + mRestoring);
        DebugLog.instance.outputLog(TAG, "mWaitingForResult=" + mWaitingForResult);
        DebugLog.instance.outputLog(TAG, "mSavedInstanceState=" + mSavedInstanceState);
        DebugLog.instance.outputLog(TAG, "sFolders.size=" + sFolders.size());
        mModel.dumpState();

        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.dumpState();
        }
        DebugLog.instance.outputLog(TAG, "END launcher2 dump state");
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.println(" ");
        writer.println("Debug logs: ");
        for (int i = 0; i < sDumpLogs.size(); i++) {
            writer.println("  " + sDumpLogs.get(i));
        }
    }

    public static void dumpDebugLogsToConsole() {
        DebugLog.instance.outputLog(TAG, "");
        DebugLog.instance.outputLog(TAG, "*********************");
        DebugLog.instance.outputLog(TAG, "Launcher debug logs: ");
        for (int i = 0; i < sDumpLogs.size(); i++) {
            DebugLog.instance.outputLog(TAG, "  " + sDumpLogs.get(i));
        }
        DebugLog.instance.outputLog(TAG, "*********************");
        DebugLog.instance.outputLog(TAG, "");
    }
}

interface LauncherTransitionable {
    View getContent();
    void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace);
    void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace);
    void onLauncherTransitionStep(Launcher l, float t);
    void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace);
    boolean isMainView();
}
