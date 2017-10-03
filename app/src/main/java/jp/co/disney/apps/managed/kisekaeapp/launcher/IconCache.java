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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.JsonReader;
import android.util.Log;
import jp.co.disney.apps.managed.kisekaeapp.R;

/**
 * Cache of application icons.  Icons can be made from any thread.
 */
public class IconCache {
    @SuppressWarnings("unused")
    private static final String TAG = "Launcher.IconCache";

    private static final int INITIAL_ICON_CACHE_CAPACITY = 50;

    private static class CacheEntry {
        public Bitmap icon;
        public String title;
    }

    private final Bitmap mDefaultIcon;
    private final LauncherApplication mContext;
    private final PackageManager mPackageManager;
    private final HashMap<ComponentName, CacheEntry> mCache =
            new HashMap<ComponentName, CacheEntry>(INITIAL_ICON_CACHE_CAPACITY);
    private int mIconDpi;

    // バッジ背景
    private final Drawable mBadgeBg;

    private final HashMap<String, String> mKisekaeMap = new HashMap<String, String>();
    private HashMap<String, KisekaeInfo> mCurrentKisekaeMap;

    private HashMap<String, BadgeInfo> mBadgeInfoMap;

    public HashMap<String, KisekaeInfo> getCurrentKisekaeMap() {
        return mCurrentKisekaeMap;
    }

    public HashMap<String, String> getKisekaeMap() {
        return mKisekaeMap;
    }

    public void setCurrentKisekaeMap(HashMap<String, KisekaeInfo> currentKisekaeMap) {
        mCurrentKisekaeMap = currentKisekaeMap;
    }

    public HashMap<String, BadgeInfo> getBadgeInfoMap() {
        return mBadgeInfoMap;
    }

    public void setBadgeInfoMap(HashMap<String, BadgeInfo> badgeInfoMap) {
        mBadgeInfoMap = badgeInfoMap;
    }

    public void initKisekaeIconMap() {

        InputStream in = null;
        JsonReader reader = null;
        try {
            in = mContext.getAssets().open("ic_map.json");
            reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
            reader.beginObject();
            while (reader.hasNext()) {
                String pkgName = reader.nextName();
                String icResName = reader.nextString();
                mKisekaeMap.put(pkgName, icResName);
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

    public IconCache(LauncherApplication context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        mContext = context;
        mPackageManager = context.getPackageManager();
        mIconDpi = activityManager.getLauncherLargeIconDensity();
        // need to set mIconDpi before getting default icon
        mDefaultIcon = makeDefaultIcon();

        mBadgeBg = ContextCompat.getDrawable(context, R.drawable.updatebatchbase);

        initKisekaeIconMap();
    }

    public Drawable getBadgeBackground() {
        return mBadgeBg;
    }

    public Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(), android.R.mipmap.sym_def_app_icon);
    }

    public Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable d;
        try {
//            Bitmap bmp = BitmapUtils.decodeToBitmap(resources, iconId);
//            d = new BitmapDrawable(resources, bmp);
//            d = ResourcesCompat.getDrawableForDensity(resources, iconId, mIconDpi, mContext.getTheme());
            d = resources.getDrawableForDensity(iconId, mIconDpi);
        } catch (Resources.NotFoundException e) {
            d = null;
        }

        return (d != null) ? d : getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(String packageName, int iconId) {

        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    public Drawable getFullResIcon(ResolveInfo info) {
        return getFullResIcon(info.activityInfo);
    }

    public boolean isKisekaeIcon(ActivityInfo info) {
        String kisekaeIconName = mKisekaeMap.get(info.packageName + "/" + info.name);
        return (kisekaeIconName != null);
    }

    public Drawable getFullResIcon(ActivityInfo info) {

        KisekaeInfo kisekaeInfo = null;
        if (mCurrentKisekaeMap != null) {
            kisekaeInfo = mCurrentKisekaeMap.get(info.packageName + "/" + info.name);
        }

        if (kisekaeInfo != null) {
            String rootDir = ThemeUtils.getThemeRootDirectory(mContext,
                    kisekaeInfo.getThemeId(), kisekaeInfo.isInAppTheme(), kisekaeInfo.getContentsType());

            Bitmap iconBmp = ThemeUtils.loadKisekaeIcon(mContext, rootDir + kisekaeInfo.getIconName() + ".png");
            if (iconBmp != null) {
                return new BitmapDrawable(mContext.getResources(), iconBmp);
            } else {
                Log.e(TAG, "cannot load the icon. [" + rootDir + kisekaeInfo.getIconName() + ".png]");
            }
        }

        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(
                    info.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }
        if (resources != null) {
            int iconId = info.getIconResource();
            if (iconId != 0) {
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    private Bitmap makeDefaultIcon() {
        Drawable d = getFullResDefaultActivityIcon();
        Bitmap b = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1),
                Math.max(d.getIntrinsicHeight(), 1),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(c);
        c.setBitmap(null);
        return b;
    }

    /**
     * Remove any records for the supplied ComponentName.
     */
    public void remove(ComponentName componentName) {
        synchronized (mCache) {
            mCache.remove(componentName);
        }
    }

    /**
     * Empty out the cache.
     */
    public void flush() {
        synchronized (mCache) {
            mCache.clear();
        }
    }

    /**
     * Fill in "application" with the icon and label for "info."
     */
    public void getTitleAndIcon(ApplicationInfo application, ResolveInfo info,
            HashMap<Object, CharSequence> labelCache) {
        synchronized (mCache) {
            CacheEntry entry = cacheLocked(application.componentName, info, labelCache);

            application.title = entry.title;
            application.iconBitmap = entry.icon;
        }
    }

    public Bitmap getIcon(Intent intent) {
        synchronized (mCache) {
            final ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, 0);
            ComponentName component = intent.getComponent();

            if (resolveInfo == null || component == null) {
                return mDefaultIcon;
            }

            CacheEntry entry = cacheLocked(component, resolveInfo, null);
            return entry.icon;
        }
    }

    public Bitmap getIcon(ComponentName component, ResolveInfo resolveInfo,
            HashMap<Object, CharSequence> labelCache) {
        synchronized (mCache) {
            if (resolveInfo == null || component == null) {
                return null;
            }

            CacheEntry entry = cacheLocked(component, resolveInfo, labelCache);
            return entry.icon;
        }
    }

    public boolean isDefaultIcon(Bitmap icon) {
        return mDefaultIcon == icon;
    }

    private CacheEntry cacheLocked(ComponentName componentName, ResolveInfo info,
            HashMap<Object, CharSequence> labelCache) {
        CacheEntry entry = mCache.get(componentName);
        if (entry == null) {
            entry = new CacheEntry();

            mCache.put(componentName, entry);

            entry.title = ApplicationInfoBase.getApplicationTitle(mPackageManager,
                    componentName, info, labelCache);

            entry.icon = Utilities.createIconBitmap(
                    getFullResIcon(info), mContext);
        }
        return entry;
    }

    public void updateCache() {

        if (mCurrentKisekaeMap == null) return;

        PackageManager pm = mContext.getPackageManager();

        synchronized (mCache) {
            Iterator<Entry<ComponentName, CacheEntry>> itr = mCache.entrySet().iterator();
            while (itr.hasNext()) {
                Entry<ComponentName, CacheEntry> pair = (Entry<ComponentName, CacheEntry>) itr.next();

                ComponentName componentName = pair.getKey();
                CacheEntry entry = pair.getValue();

                try {
                    ActivityInfo activityInfo = pm.getActivityInfo(componentName, PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);

                    // 着せ替え対象のアイコンだけ更新する
                    if (isKisekaeIcon(activityInfo)) {
                        if (entry.icon != null) {
                            entry.icon.recycle();
                        }
                        entry.icon = Utilities.createIconBitmap(getFullResIcon(activityInfo), mContext);
                    }

                } catch (NameNotFoundException e) {
                }
            }
        }
    }
}
