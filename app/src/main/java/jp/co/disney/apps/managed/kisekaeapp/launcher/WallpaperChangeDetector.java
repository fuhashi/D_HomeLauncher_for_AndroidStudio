package jp.co.disney.apps.managed.kisekaeapp.launcher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;

/**
 * システム壁紙変更検出クラス
 */
public class WallpaperChangeDetector {

    private byte[] mWpHash = null;
    private int[] mWpSize = new int[2];

    private boolean mIsReady = false;

    public boolean isReady() {
        return mIsReady;
    }

    public void turnOn(Context ctx) {

        if (Build.VERSION.SDK_INT < 17) {
            setCurrentWallPaperHashAndSize(ctx);
        }

        mIsReady = true;
    }

    public void turnOff() {

        mIsReady = false;

        if (Build.VERSION.SDK_INT < 17) {
            clearWallpaperHashAndSize();
        }
    }

    public boolean hasWallpaperChanged(Context ctx, int originalWallpaperResId) {

        boolean wallpaperChanged = false;

        WallpaperManager wm = (WallpaperManager) ctx.getSystemService(Context.WALLPAPER_SERVICE);
        if (wm.getWallpaperInfo() != null) {
            // ライブ壁紙に変更された
            wallpaperChanged = true;
        } else {

            if (Build.VERSION.SDK_INT >= 17) {
                wallpaperChanged = !wm.hasResourceWallpaper(originalWallpaperResId);
            } else {

                if (mWpHash == null) return false;

                wm.suggestDesiredDimensions(mWpSize[0], mWpSize[1]);
                BitmapDrawable wpBmpDrawable = (BitmapDrawable) wm.getDrawable();
                if (wpBmpDrawable == null) return false;
                Bitmap wpBmp = wpBmpDrawable.getBitmap();
                if (wpBmp == null) return false;

                final int w = wpBmp.getWidth();
                final int h = wpBmp.getHeight();

                if (mWpSize[0] == w && mWpSize[1] == h) {

                    int[] nowWpPixels = new int[w * h];
                    wpBmp.getPixels(nowWpPixels, 0, w, 0, 0, w, h);
                    byte[] nowWpHash = createHash(nowWpPixels);

                    for (int i = 0; i < nowWpHash.length; i++) {
                        if (nowWpHash[i] != mWpHash[i]) {
                            wallpaperChanged = true;
                            break;
                        }
                    }

                } else {
                    // サイズ違い
                    wallpaperChanged = true;
                }
            }
        }

        return wallpaperChanged;
    }

    void onSaveInstanceState(Bundle outState) {
        if (Build.VERSION.SDK_INT < 17) {
            outState.putByteArray("default_wallpaper_hash", mWpHash);
            outState.putIntArray("default_wallpaper_size", mWpSize);
        }
        outState.putBoolean("wp_detector_ready", mIsReady);
    }

    void restoreState(Bundle savedState) {
        if (Build.VERSION.SDK_INT < 17) {
            mWpHash = savedState.getByteArray("default_wallpaper_hash");
            mWpSize = savedState.getIntArray("default_wallpaper_size");
        }
        mIsReady = savedState.getBoolean("wp_detector_ready");
    }

    private void setCurrentWallPaperHashAndSize(Context ctx) {
        WallpaperManager wm = (WallpaperManager) ctx.getSystemService(Context.WALLPAPER_SERVICE);
        Bitmap wallpaper = ((BitmapDrawable) wm.getDrawable()).getBitmap();
        if (wallpaper != null) {

            int w = wallpaper.getWidth();
            int h = wallpaper.getHeight();

            int[] wallpaperPixels = new int[w * h];
            wallpaper.getPixels(wallpaperPixels, 0, w, 0, 0, w, h);
            mWpHash = createHash(wallpaperPixels);
            mWpSize[0] = w;
            mWpSize[1] = h;
        }
    }

    private void clearWallpaperHashAndSize() {
        mWpHash = null;
        mWpSize[0] = 0;
        mWpSize[1] = 1;
    }

    private static byte[] createHash(int[] a) {
        return createHash(intArrayToByteArray(a));
    }

    private static byte[] createHash(byte[] hashThis) {
        try {
            byte[] hash = new byte[20];
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            hash = md.digest(hashThis);
            return hash;
        } catch (NoSuchAlgorithmException ex) {
        }
        return null;
    }

    private static byte[] intArrayToByteArray(int[] a) {

        final int len = a.length;

        byte[] ret = new byte[len * 4];
        for (int i = 0; i < len; i++) {
            ret[i * 4 + 3] = (byte) (a[i] & 0xFF);
            ret[i * 4 + 2] = (byte) ((a[i] >> 8) & 0xFF);
            ret[i * 4 + 1] = (byte) ((a[i] >> 16) & 0xFF);
            ret[i * 4] = (byte) ((a[i] >> 24) & 0xFF);
        }
        return ret;
    }
}
