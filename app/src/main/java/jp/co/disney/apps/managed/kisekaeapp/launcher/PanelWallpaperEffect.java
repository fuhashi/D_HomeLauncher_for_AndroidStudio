package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import jp.co.disney.apps.managed.kisekaeapp.system.graphics.FastBitmapDrawable;

public class PanelWallpaperEffect {

    static void draw(Canvas canvas, PanelWallpapers panelWps, int index,
            int panelCount, int offset, int offsetDelta,
            int top, int width, boolean just, boolean canLoop, int overScrollX, int maxScrollX,
            float scale, float cellMarginTop, int padTop, int clh, int offset_old) {

        if (canLoop) {

            if (overScrollX < 0) {
                // ループ時に最初のページの前に最後のページの壁紙を描画
                final int left_over = -overScrollX - offsetDelta;
                drawWp(canvas, panelWps, panelCount - 1, panelCount,
                        left_over, top, width, scale, padTop, clh, cellMarginTop);

                final int left = -overScrollX;
                drawWp(canvas, panelWps, 0, panelCount,
                        left, top, width, scale, padTop, clh, cellMarginTop);

            } else if (overScrollX > maxScrollX) {
                // ループ時に最後のページの先に最初のページの壁紙を描画
                final int left = offset - overScrollX + maxScrollX;
                drawWp(canvas, panelWps, panelCount - 1, panelCount,
                        left, top, width, scale, padTop, clh, cellMarginTop);

                final int left_over = offset - overScrollX + maxScrollX + offsetDelta;
                drawWp(canvas, panelWps, 0, panelCount,
                        left_over, top, width, scale, padTop, clh, cellMarginTop);

            } else {
                drawWps(canvas, panelWps, index, top, width, scale, padTop, clh, cellMarginTop,
                        just, offset, offset_old, offsetDelta, panelCount);
            }
        } else {
            drawWps(canvas, panelWps, index, top, width, scale, padTop, clh, cellMarginTop,
                    just, offset, offset_old, offsetDelta, panelCount);
        }
    }

    private static void drawWps(Canvas canvas, PanelWallpapers panelWps, int index,
            int top, int width, float scale, int padTop, int clh, float cellMarginTop,
            boolean just, int offset, int offset_old, int offsetDelta, int panelCount) {

        boolean requireScaled = (Float.compare(scale, 1.0f) != 0);

        if (!just || requireScaled) {
            drawWp(canvas, panelWps, index - 1, panelCount,
                    offset_old, top, width, scale, padTop, clh, cellMarginTop);
        }

        drawWp(canvas, panelWps, index, panelCount,
                offset, top, width, scale, padTop, clh, cellMarginTop);

        if (requireScaled) {
            drawScaledWp(canvas, panelWps, index + 1, panelCount,
                     offset + offsetDelta, top, width, scale, padTop, clh, cellMarginTop);
        }
    }

    private static void drawWp(Canvas canvas, PanelWallpapers panelWps, int index, int panelCount,
            int left, int top, int width, float scale, int padTop, int clh, float cellMarginTop) {

        if (index >= panelCount || index >= panelWps.getMaxCount() || index < 0) return;

        final FastBitmapDrawable wp = panelWps.getWp(index);
        if (wp == null) return;

        drawWp(canvas, wp, left, top, width, scale, padTop, clh, cellMarginTop);
    }

    private static void drawScaledWp(Canvas canvas, PanelWallpapers panelWps, int index, int panelCount,
            int left, int top, int width, float scale, int padTop, int clh, float cellMarginTop) {

        if (index >= panelCount || index >= panelWps.getMaxCount() || index < 0) return;

        final FastBitmapDrawable wp = panelWps.getWp(index);
        if (wp == null) return;

        drawScaledWp(canvas, wp, left, width, scale, padTop, clh, cellMarginTop);
    }

    private static void drawWp(Canvas canvas, FastBitmapDrawable wp,
            int left, int top, int width, float scale, int padTop, int clh, float cellMarginTop) {

        final Bitmap wpBmp = wp.getBitmap();
        if (wpBmp == null) return;

        final int bw = wpBmp.getWidth();
        final int bh = wpBmp.getHeight();

        if (Float.compare(scale, 1.0f) == 0) {
            int h = (int) Math.floor((bh * width) / (float) bw);
            wp.setBounds(left, top, left + width, top + h);
            wp.draw(canvas);
        } else {
            drawScaledWp(canvas, wp, left, width, scale, padTop, clh, cellMarginTop, bw, bh);
        }
    }

    private static void drawScaledWp(Canvas canvas, FastBitmapDrawable wp,
            int left, int width, float scale, int padTop, int clh, float cellMarginTop) {

        final Bitmap wpBmp = wp.getBitmap();
        if (wpBmp == null) return;

        final int bw = wpBmp.getWidth();
        final int bh = wpBmp.getHeight();

        drawScaledWp(canvas, wp, left, width, scale, padTop, clh, cellMarginTop, bw, bh);
    }

    private static void drawScaledWp(Canvas canvas, FastBitmapDrawable wp,
            int left, int width, float scale, int padTop, int clh, float cellMarginTop, int bw, int bh) {
        float dspbw = width * scale;
        float dsph = (bh * dspbw) / bw;
        int top2 = padTop + (int) Math.floor(clh * (1 - scale) / 2 - cellMarginTop * scale);
        wp.setBounds(left + (int) Math.floor((width - dspbw) / 2), top2,
                left + (int) Math.floor((width + dspbw) / 2), top2 + (int) Math.floor(dsph));
        wp.draw(canvas);
    }

    static void drawWithFade(Canvas canvas, PanelWallpapers panelWps, int index,
            int panelCount, int deltaX, int offset, int offsetDelta,
            int top, int dw, boolean just, boolean canLoop, int overScrollX, int maxScrollX) {

        if (canLoop) {

            if (overScrollX < 0) {
                final int alpha = (int) Math.ceil((255 * (1.0f + (overScrollX) / (float) offsetDelta)));
                drawWpWithFade(canvas, panelWps, panelCount - 1, panelCount, deltaX, top, dw);
                drawWpWithFade(canvas, panelWps, 0, panelCount, deltaX, top, dw, alpha);

            } else if (overScrollX > maxScrollX) {
                final int alpha = (int) (255 * ((overScrollX - maxScrollX) / (float) offsetDelta));
                drawWpWithFade(canvas, panelWps, panelCount - 1, panelCount, deltaX, top, dw);
                drawWpWithFade(canvas, panelWps, 0, panelCount, deltaX, top, dw, alpha);

            } else {
                drawWpsWidthFade(canvas, panelWps, index, panelCount, deltaX, top, dw, offset, offsetDelta, just);
            }

        } else {
            drawWpsWidthFade(canvas, panelWps, index, panelCount, deltaX, top, dw, offset, offsetDelta, just);
        }
    }

    private static void drawWpsWidthFade(Canvas canvas, PanelWallpapers panelWps, int index, int panelCount,
            int deltaX, int top, int width, int offset, int offsetDelta, boolean just) {

        final int alpha = (int) Math.ceil((255 * (1.0f - ((offset - deltaX) / (float) offsetDelta))));

        if (!just) {
            drawWpWithFade(canvas, panelWps, index - 1, panelCount, deltaX, top, width);
        }

        drawWpWithFade(canvas, panelWps, index, panelCount, deltaX, top, width, alpha);
    }

    private static void drawWpWithFade(Canvas canvas, PanelWallpapers panelWps, int index, int panelCount,
            int deltaX, int top, int width) {

        if (index >= panelCount || index >= panelWps.getMaxCount() || index < 0) return;

        final FastBitmapDrawable wp = panelWps.getWp(index);
        if (wp == null) return;

        drawWpWithFade(canvas, wp, deltaX, top, width);
    }

    private static void drawWpWithFade(Canvas canvas, PanelWallpapers panelWps, int index, int panelCount,
            int deltaX, int top, int width, int alpha) {

        if (index >= panelCount || index >= panelWps.getMaxCount() || index < 0) return;

        final FastBitmapDrawable wp = panelWps.getWp(index);
        if (wp == null) return;

        drawWpWithFade(canvas, wp, deltaX, top, width, alpha);
    }

    private static void drawWpWithFade(Canvas canvas, FastBitmapDrawable wp,
            int deltaX, int top, int width) {

        final Bitmap wpBmp = wp.getBitmap();
        if (wpBmp == null) return;

        int h = (wpBmp.getHeight() * width) / wpBmp.getWidth();
        wp.setAlpha(255);
        wp.setBounds(deltaX, top, deltaX + width, top + h);
        wp.draw(canvas);
    }

    private static void drawWpWithFade(Canvas canvas, FastBitmapDrawable wp,
            int deltaX, int top, int width, int alpha) {

        final Bitmap wpBmp = wp.getBitmap();
        if (wpBmp == null) return;

        int h = (wpBmp.getHeight() * width) / wpBmp.getWidth();
        wp.setAlpha(alpha);
        wp.setBounds(deltaX, top, deltaX + width, top + h);
        wp.draw(canvas);
        wp.setAlpha(255);
    }
}
