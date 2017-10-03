package jp.co.disney.apps.managed.kisekaeapp.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONObject;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsFileName;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperator;
import jp.co.disney.apps.managed.kisekaeapp.system.file.FileUtils;
import jp.co.disney.apps.managed.kisekaeapp.system.graphics.BitmapUtils;

class ThemeUtils {

    private static final String TAG = "ThemeUtils";

    static final String JSON_KEY_NUM_WP = "num_wp";
    static final String JSON_KEY_MAIN_WP = "main_wp";
    static final String JSON_KEY_NUM_PAGE = "num_page";
    static final String JSON_KEY_DEFAULT_PAGE = "default_page";
    static final String JSON_KEY_TEXT_COLOR = "text_color";
    static final String JSON_KEY_PAGE_WPS = "page_wps";

    static final String JSON_KEY_PAGE_PRIORITY_FOR_ADDING = "page_priority_for_adding";

    static final String JSON_KEY_WIDGET_POS = "widget_pos";
    static final String JSON_KEY_WIDGET_POS_INAPP = "widget_pos_inapp";

    static final String JSON_KEY_SHORTCUT_POS = "shortcut_pos";

    static final String JSON_KEY_ICON_NAME = "icon_name";
    static final String JSON_KEY_PACKAGE_NAME = "package_name";
    static final String JSON_KEY_CLASS_NAME = "class_name";
    static final String JSON_KEY_PAGE_NO = "page_no";
    static final String JSON_KEY_CELL_X = "cell_x";
    static final String JSON_KEY_CELL_Y = "cell_y";
    static final String JSON_KEY_SPAN_X = "span_x";
    static final String JSON_KEY_SPAN_Y = "span_y";

    static final String WIDGET_CLASS_NAME_BATTERY = "jp.co.disney.apps.managed.kisekaeapp.widgetbattery.WidgetBattery";

    static final String WIDGET_PACKAGE_NAME_CALENDAR = "jp.co.disney.apps.managed.calendarapp";
    static final String WIDGET_PACKAGE_NAME_WEATHER = "jp.co.disney.apps.managed.weatherapp";
    static final String WIDGET_PACKAGE_NAME_KISEKAEALARM = "jp.co.disney.apps.dm.kisekaealarm";

    static final String THEME_ID_DEFAULT = "double_001";

    static final String KISEKAE_ICON_NAME_ALARM = "ic_alarm";
    static final String KISEKAE_ICON_NAME_BROWSER = "ic_browser";
    static final String KISEKAE_ICON_NAME_CALC = "ic_calc";
    static final String KISEKAE_ICON_NAME_CALENDAR = "ic_calendar";
    static final String KISEKAE_ICON_NAME_CAMERA = "ic_camera";
    static final String KISEKAE_ICON_NAME_CONTACTS = "ic_contacts";
    static final String KISEKAE_ICON_NAME_EMAIL = "ic_email";
    static final String KISEKAE_ICON_NAME_GALLERY = "ic_gallery";
    static final String KISEKAE_ICON_NAME_MESSAGE = "ic_message";
    static final String KISEKAE_ICON_NAME_MUSIC = "ic_music";
    static final String KISEKAE_ICON_NAME_PHONE = "ic_phone";
    static final String KISEKAE_ICON_NAME_PLAYSTORE = "ic_playstore";
    static final String KISEKAE_ICON_NAME_SETTINGS = "ic_settings";

    static final String PACKAGE_NAME_KISEKAE = "jp.co.disney.apps.managed.kisekaeapp";
    static final String CLASS_NAME_KISEKAE = "jp.co.disney.apps.managed.kisekaeapp.SplashActivity";
    static final String PACKAGE_NAME_DISNEY_MARKET = "jp.co.disney.apps.base.disneymarketapp";
    static final String CLASS_NAME_DISNEY_MARKET= "jp.co.disney.apps.base.disneymarketapp.actBase";
    static final String PACKAGE_NAME_CHROME = "com.android.chrome";
    static final String CLASS_NAME_CHROME = "com.google.android.apps.chrome.Main";

    static final int MAX_NUM_THEME_BG = 5;

    static String getThemeRootDirectory(Context ctx, String themeId, boolean inAppTheme, int contentsType) {

        final String dir;

        if (inAppTheme) {
            dir = "themes" + File.separator + themeId + File.separator;
        } else {
            dir = ContentsOperator.op.getDirectoryPath(ctx, themeId, contentsType) + File.separator;
        }

        return dir;
    }

    static String getThemeWallpaperPath(Context ctx, String dir, int index, boolean inAppTheme) {

        String path = null;

        if (inAppTheme) {
            path = dir + "wp_" + (index + 1) + ".jpg";
        } else {
            if (index >= 0 && index < MAX_NUM_THEME_BG) {
                final ContentsFileName[] wpNames = { ContentsFileName.wp1, ContentsFileName.wp2, ContentsFileName.wp3,
                        ContentsFileName.wp4, ContentsFileName.wp5 };

                ContentsFileName wpName = wpNames[index];
                path = ContentsOperator.op.getCurrentThemeContentsImagePath(ctx, dir, wpName);
            }
        }

        return path;
    }

    static String[] getThemeWallpaperPaths(Context ctx, String dir, int numPage, int[] pageWps, boolean inAppTheme) {

        final String[] wpPaths = new String[Workspace.MAX_SCREEN_COUNT];

        for (int i = 0; i < numPage; i++) {
            int wpIndex = pageWps[i];
            wpPaths[i] = getThemeWallpaperPath(ctx, dir, wpIndex, inAppTheme);
        }

        return wpPaths;
    }

    static JSONObject loadThemeInfoJson(Context ctx, String rootDir, boolean inAppTheme) {

        if (inAppTheme) {
            return FileUtils.loadJSONInAssets(ctx, rootDir + ContentsFileName.settingsJson.getFileName());
        } else {
            return FileUtils.loadJSON(rootDir + ContentsFileName.settingsJson.getFileName());
        }
    }

    static Bitmap loadThemeBackground(Context ctx, String filePath) {

        final DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
        final int screenWidth = displayMetrics.widthPixels;

        return loadThemeBackground(ctx, filePath, screenWidth);
    }

    static Bitmap loadThemeBackground(Context ctx, String filePath, int reqWidth) {

        if (filePath == null || filePath.equals("")) return null;

        boolean inAppTheme = !filePath.startsWith(File.separator);
        if (inAppTheme) {
            return loadThemeBackgroundInAssets(ctx, filePath, reqWidth);
        } else {
            return loadThemeBackgroundDL(ctx, filePath, reqWidth);
        }
    }

    static Bitmap loadKisekaeIcon(Context ctx, String iconFilePath) {

        if (iconFilePath == null || iconFilePath.equals("")) return null;

        boolean inAppTheme = !iconFilePath.startsWith(File.separator);
        if (inAppTheme) {
            return loadKisekaeIconInAssets(ctx, iconFilePath);
        } else {
            return loadKisekaeIconDL(ctx, iconFilePath);
        }
    }

    private static Bitmap loadKisekaeIconInAssets(Context ctx, String iconFilePath) {

        Bitmap icon = null;

//        final DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
//        final int screenWidth = displayMetrics.widthPixels;

//        final int iconSize = (int) Math.floor(screenWidth * 144 / (float)1080);
//        final int iconSize = (int) Math.floor(48 * displayMetrics.density);
        final int iconSize = ctx.getResources().getDimensionPixelSize(R.dimen.app_icon_size);

        final Matrix matrix = new Matrix();

        final AssetManager assets = ctx.getAssets();

        InputStream in = null;
        try {
            in = assets.open(iconFilePath);

            Bitmap bmpSrc = BitmapUtils.decodeToBitmap(in);
            if (bmpSrc != null) {
                icon = BitmapUtils.resizeBitmap2(bmpSrc, iconSize, iconSize, matrix, true);
            }

        } catch (IOException e) {

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

        return icon;
    }

    private static Bitmap loadKisekaeIconDL(Context ctx, String iconFilePath) {

        Bitmap icon = null;

//        final DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
//        final int screenWidth = displayMetrics.widthPixels;

//        final int iconSize = (int) Math.floor(screenWidth * 144 / (float)1080);
//        final int iconSize = (int) Math.floor(48 * displayMetrics.density);
        final int iconSize = ctx.getResources().getDimensionPixelSize(R.dimen.app_icon_size);

        final Matrix matrix = new Matrix();

        FileInputStream in = null;
        try {

            File iconFile = new File(iconFilePath);

            in = new FileInputStream(iconFile);

            Bitmap bmpSrc = BitmapUtils.decodeToBitmap(in);
            if (bmpSrc != null) {
                icon = BitmapUtils.resizeBitmap2(bmpSrc, iconSize, iconSize, matrix, true);
            }

        } catch (IOException e) {

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

        return icon;
    }

    private static Bitmap loadThemeBackgroundInAssets(Context ctx, String bgFilePath, int reqWidth) {

        Bitmap bg = null;

        final Matrix matrix = new Matrix();

        final AssetManager assets = ctx.getAssets();

        InputStream in = null;
        try {
            in = assets.open(bgFilePath);

//            Bitmap bmpSrc = BitmapUtils.decodeToBitmap(in);

            int[] srcSize = new int[2];
            BitmapUtils.getImageSize(srcSize, in);

            in.close();

            int reqHeight = srcSize[1] * reqWidth / srcSize[0];
            int inSampleSize = BitmapUtils.calculateInSampleSize(srcSize[0], srcSize[1], reqWidth, reqHeight);

            in = assets.open(bgFilePath);

            Bitmap bmpSrc = BitmapUtils.decodeToBitmap(in, inSampleSize);
            if (bmpSrc != null) {
                bg = BitmapUtils.resizeBitmap(bmpSrc, reqWidth, reqHeight, matrix, true);
            }

        } catch (IOException e) {

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

        return bg;
    }

    private static Bitmap loadThemeBackgroundDL(Context ctx, String bgFilePath, int reqWidth) {

        Bitmap bg = null;

        final Matrix matrix = new Matrix();

        FileInputStream in = null;
        try {

            File bgFile = new File(bgFilePath);

            in = new FileInputStream(bgFile);

//            Bitmap bmpSrc = BitmapUtils.decodeToBitmap(in);

            int[] srcSize = new int[2];
            BitmapUtils.getImageSize(srcSize, in);

            in.close();

            int reqHeight = srcSize[1] * reqWidth / srcSize[0];
            int inSampleSize = BitmapUtils.calculateInSampleSize(srcSize[0], srcSize[1], reqWidth, reqHeight);

            in = new FileInputStream(bgFile);

            Bitmap bmpSrc = BitmapUtils.decodeToBitmap(in, inSampleSize);
            if (bmpSrc != null) {
                bg = BitmapUtils.resizeBitmap(bmpSrc, reqWidth, reqHeight, matrix, true);
            }

        } catch (IOException e) {

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return bg;
    }

    public static boolean isInAppWidget(String className, int[] span) {

        if (WIDGET_CLASS_NAME_BATTERY.equals(className)) {
            if (span != null) {
                span[0] = 6;
                span[1] = 4;
            }
            return true;
        }
        return false;
    }

    public static boolean isSpecialWidget(String widgetProviderClassName) {

        if (widgetProviderClassName.equals(WIDGET_CLASS_NAME_BATTERY)) {
            return true;
        }

        return false;
    }

    public static boolean checkAppInstalled(Context c, String packageName) {
        boolean ret = false;

        try {
            c.getPackageManager().getApplicationInfo(packageName,
                    PackageManager.GET_META_DATA);
            ret = true;
        } catch (NameNotFoundException ex) {
            ret = false;
        }

        return ret;
    }

    public static boolean checkDisneyCalendarappThere(Context c) {
        boolean ret = false;

        try {
              ApplicationInfo ai = c.getPackageManager().getApplicationInfo(WIDGET_PACKAGE_NAME_CALENDAR,  PackageManager.GET_META_DATA);
              ret = true;
            } catch (NameNotFoundException ex) {
                ret = false;
            }

           return ret;
    }

    public static boolean checkDisneyWeatherappThere(Context c) {
        boolean ret = false;

        try {
              ApplicationInfo ai = c.getPackageManager().getApplicationInfo(WIDGET_PACKAGE_NAME_WEATHER,  PackageManager.GET_META_DATA);
              ret = true;
            } catch (NameNotFoundException ex) {
                ret = false;
            }

           return ret;
    }

    public static String getAssetIdFromPackageName(String packageName) {

        String assetId = null;

        if (WIDGET_PACKAGE_NAME_CALENDAR.equals(packageName)) {
            assetId = "010200004";
        } else if (WIDGET_PACKAGE_NAME_WEATHER.equals(packageName)) {
            assetId = "010200033";
        } else if (WIDGET_PACKAGE_NAME_KISEKAEALARM.equals(packageName)) {
            assetId = "012000464";
        }

        return assetId;
    }

    public static boolean isDisneyLargeWidget(String className) {

        final String[] largeWidgets = {
                "jp.co.disney.apps.dm.dmd008alarm.receiver.ClockAppWidgetProvider",
                "jp.co.disney.apps.dm.dmd008pedometer.widget.PedometerWidget5_3",
                "jp.co.disney.apps.dm.dmd008weather.widget.WeatherWidget5_3"
        };

        final int len = largeWidgets.length;
        for (int i = 0; i < len; i++) {
            if (largeWidgets[i].equals(className)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isDisneyWindow5xWidget(String className) {

        final String[] largeWidgets = {
                "jp.co.disney.apps.dm.dmd008musicplayer.widget.QcircleWidget5x3",
                "jp.co.disney.apps.dm.dmd008alarm.receiver.ClockAppWidgetProvider",
                "jp.co.disney.apps.dm.dmd008pedometer.widget.PedometerWidget5_3",
                "jp.co.disney.apps.dm.dmd008weather.widget.WeatherWidget5_3"
        };

        final int len = largeWidgets.length;
        for (int i = 0; i < len; i++) {
            if (largeWidgets[i].equals(className)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isDisneyWindowWidget(String className) {

        final String[] largeWidgets = {
                "jp.co.disney.apps.dm.dmd008musicplayer.widget.QcircleWidget4x2",
                "jp.co.disney.apps.dm.dmd008alarm.receiver.ClockAppMiniWidgetProvider",
                "jp.co.disney.apps.dm.dmd008pedometer.widget.PedometerWidget4_2",
                "jp.co.disney.apps.dm.dmd008weather.widget.WeatherWidget4_2"
        };

        final int len = largeWidgets.length;
        for (int i = 0; i < len; i++) {
            if (largeWidgets[i].equals(className)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isDisneyLargeWeatherWidget(String className) {

        final String[] largeWidgets = {
                "jp.co.disney.apps.managed.weatherapp.WeatherWidgetProviderDaily1x5",
                "jp.co.disney.apps.managed.weatherapp.WeatherWidgetProviderWeekly1x5"
        };

        final int len = largeWidgets.length;
        for (int i = 0; i < len; i++) {
            if (largeWidgets[i].equals(className)) {
                return true;
            }
        }

        return false;
    }
}
