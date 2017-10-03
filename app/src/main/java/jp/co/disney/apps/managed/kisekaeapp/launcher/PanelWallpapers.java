package jp.co.disney.apps.managed.kisekaeapp.launcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import jp.co.disney.apps.managed.kisekaeapp.system.graphics.FastBitmapDrawable;

/**
 * パネル壁紙クラス
 *
 * パネル間で同じ壁紙を使用している場合、同じ画像オブジェクトを使用する。
 */
public class PanelWallpapers {
    private static final String TAG = "PanelWallpapers";

    private final HashMap<String, FastBitmapDrawable> mWpMap = new HashMap<String, FastBitmapDrawable>();

    private final String[] mWpPaths;
    private final int mMaxWpCount;

    private final FastBitmapDrawable[] mWps;

    private final Object mLock = new Object();

    PanelWallpapers(String[] wpPaths) {
        mWpPaths = wpPaths;
        mMaxWpCount = mWpPaths.length;
        mWps = new FastBitmapDrawable[mMaxWpCount];
    }

    FastBitmapDrawable getWp(int index) {
        return mWps[index];
    }

    String getPath(int index) {
        return mWpPaths[index];
    }

    int getMaxCount() {
        return mMaxWpCount;
    }

    void load(Context ctx) {
        synchronized (mLock) {
            for (int i = 0; i < mMaxWpCount; i++) {
                load(ctx, i);
            }
        }
    }

    void update(Context ctx, String[] newWpPaths, boolean forceReload) {
        synchronized (mLock) {
            for (int i = 0; i < mMaxWpCount; i++) {
                mWps[i] = null;
                mWpPaths[i] = newWpPaths[i];
            }

            // 使用しない壁紙画像を解放
            ArrayList<String> delList = new ArrayList<String>();
            Set<String> keySet = mWpMap.keySet();
            Iterator<String> itr = keySet.iterator();
            while (itr.hasNext()) {
                String wpPath = itr.next();

                if (forceReload) {
                    delList.add(wpPath);
                } else {

                    boolean contains = false;
                    for (int i = 0; i < mMaxWpCount; i++) {
                        if (wpPath.equals(newWpPaths[i])) {
                            contains = true;
                            break;
                        }
                    }
                    if (!contains) {
                        delList.add(wpPath);
                    }
                }
            }
            for (String wpPath : delList) {
                recycleWpBmp(wpPath);
            }

            // 壁紙画像を読込
            load(ctx);
        }
    }

    void unload() {
        synchronized (mLock) {
            for (int i = 0; i < mMaxWpCount; i++) {
                mWps[i] = null;
            }

            for (FastBitmapDrawable d : mWpMap.values()) {
                d.getBitmap().recycle();
                d.setBitmap(null);
            }

            mWpMap.clear();
        }
    }

    private void load(Context ctx, int index) {
        final String wpPath = mWpPaths[index];
        if (wpPath == null) return;

        if (mWpMap.containsKey(wpPath)) {
            mWps[index] = mWpMap.get(wpPath);
        } else {
            Bitmap wpBmp = ThemeUtils.loadThemeBackground(ctx, wpPath);
            if (wpBmp == null) {
                Log.e(TAG, "cannnot load the wallpaper. [" + wpPath + "]");
                return;
            }
            FastBitmapDrawable wpDrawable = new FastBitmapDrawable(wpBmp);
            mWpMap.put(wpPath, wpDrawable);
            mWps[index] = wpDrawable;
        }
    }

    private void recycleWpBmp(String wpPath) {
        FastBitmapDrawable wpDrawable = mWpMap.get(wpPath);
        mWpMap.remove(wpPath);
        wpDrawable.getBitmap().recycle();
        wpDrawable.setBitmap(null);
    }
}
