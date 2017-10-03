package jp.co.disney.apps.managed.kisekaeapp.system.graphics;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;

final public class BitmapUtils {

    public static void getImageSize(int[] size, int resourceId, Context context) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resourceId, options);

        size[0] = options.outWidth;
        size[1] = options.outHeight;
    }

    public static void getImageSize(int[] size, InputStream in) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        decodeToBitmapAtOptions(in, options);

        size[0] = options.outWidth;
        size[1] = options.outHeight;
    }

    public static Bitmap decodeToBitmap(Context context, int resourceId) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        return decodeToBitmapAtOptions(context, resourceId, options);
    }

    public static Bitmap decodeToBitmap(Context context, int resourceId, int inSampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;
        return decodeToBitmapAtOptions(context, resourceId, options);
    }

    public static Bitmap decodeToBitmap(Resources resources, int resourceId) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        return decodeToBitmapAtOptions(resources, resourceId, options);
    }

    public static Bitmap decodeToBitmap(InputStream in) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bmp = decodeToBitmapAtOptions(in, options);
        return bmp;
    }

    public static Bitmap decodeToBitmap(InputStream in, int inSampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;
        Bitmap bmp = decodeToBitmapAtOptions(in, options);
        return bmp;
    }

    public static Bitmap resizeBitmap2(Bitmap src, int width, int height, Matrix matrix, boolean recycleSrc) {

        final int srcWidth = src.getWidth();
        final int srcHeight = src.getHeight();

        // 解像度と画像のサイズ比率
        final float widthScale = width / (float) srcWidth;
        final float heightScale = height / (float) srcHeight;

        if (Float.compare(widthScale, 1.0f) != 0 ||  Float.compare(heightScale, 1.0f) != 0) {
            // 縮小処理

            // 比率をMatrixに設定
            matrix.reset();
            if (widthScale > heightScale) {
                matrix.postScale(widthScale, widthScale);
            } else {
                matrix.postScale(heightScale, heightScale);
            }

            // 縮小Bitmapを生成
            Bitmap bmpRsz = Bitmap.createBitmap(src, 0, 0, srcWidth, srcHeight, matrix, true);

            if (recycleSrc) {
                src.recycle();
            }

            return bmpRsz;
        }

        return src;
    }

    public static Bitmap resizeBitmap(Bitmap src, int width, int height, Matrix matrix, boolean recycleSrc) {

        final int srcWidth = src.getWidth();
        final int srcHeight = src.getHeight();

        // 解像度と画像のサイズ比率
        final float widthScale = width / (float) srcWidth;
        final float heightScale = height / (float) srcHeight;

        if (widthScale < 1.0f && heightScale < 1.0f) {
            // 縮小処理

            // 比率をMatrixに設定
            matrix.reset();
            if (widthScale > heightScale) {
                matrix.postScale(widthScale, widthScale);
            } else {
                matrix.postScale(heightScale, heightScale);
            }

            // 縮小Bitmapを生成
            Bitmap bmpRsz = Bitmap.createBitmap(src, 0, 0, srcWidth, srcHeight, matrix, true);

            if (recycleSrc) {
                src.recycle();
            }

            return bmpRsz;
        }

        return src;
    }

    public static int calculateInSampleSize(int srcWidth, int srcHeight, int reqWidth, int reqHeight) {

        int inSampleSize = 1;
        if (srcWidth > reqWidth || srcHeight > reqHeight) {

            final int halfWidth = srcWidth / 2;
            final int halfHeight = srcHeight / 2;

            while ((halfWidth / inSampleSize) > reqWidth
                    && (halfHeight / inSampleSize) > reqHeight) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private static Bitmap decodeToBitmapAtOptions(InputStream in, BitmapFactory.Options options) {
        Bitmap bmp = BitmapFactory.decodeStream(in, null, options);
        return bmp;
    }

    private static Bitmap decodeToBitmapAtOptions(Context context, int resourceId, BitmapFactory.Options options) {
        return decodeToBitmapAtOptions(context.getResources(), resourceId, options);
    }

    private static Bitmap decodeToBitmapAtOptions(Resources resources, int resourceId, BitmapFactory.Options options) {

        Bitmap bmp = null;

        InputStream ins = null;
        try {
            ins = resources.openRawResource(resourceId);
            bmp = BitmapFactory.decodeStream(ins, null, options);
//          if (bmp == null) {
//              Log.e(TAG, "decodeToBitmap failed. decodeStream returned null.");
//          }
        } catch (NotFoundException nfe) {
//          Log.e(TAG, "decodeToBitmap failed. openRawResource throwed NotFoundException.", nfe);
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ioe) { }
            }
        }
        return bmp;
    }

    public static Bitmap highlightImage(Bitmap src) {
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
}
