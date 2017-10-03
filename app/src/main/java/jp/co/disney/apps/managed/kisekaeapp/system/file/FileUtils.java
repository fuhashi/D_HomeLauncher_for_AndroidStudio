package jp.co.disney.apps.managed.kisekaeapp.system.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class FileUtils {

    private static final String TAG = "FileUtils";

    public static String readFromFile(String filePath) {

        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        String contents = null;

        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(filePath));
            int size = in.available();
            byte[] buffer = new byte[size];
            in.read(buffer);
            contents = new String(buffer, "UTF-8");

        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

        return contents;
    }

    public static String readFromFileInAssets(Context ctx, String filePath) {

        final AssetManager assetManager = ctx.getAssets();

        String contents = null;

        InputStream in = null;
        try {
            in = new BufferedInputStream(assetManager.open(filePath));
            int size = in.available();
            byte[] buffer = new byte[size];
            in.read(buffer);
            contents = new String(buffer, "UTF-8");

        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

        return contents;
    }

    public static JSONObject loadJSON(String jsonFilePath) {

        String json = FileUtils.readFromFile(jsonFilePath);
        if (json == null) return null;

        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(json);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return jsonObj;
    }

    public static JSONObject loadJSONInAssets(Context ctx, String jsonFilePath) {

        String json = FileUtils.readFromFileInAssets(ctx, jsonFilePath);
        if (json == null) return null;

        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(json);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return jsonObj;
    }

    public static void copyAssetsFileOrDir(Context ctx, String srcPath, File dstDir)
            throws IOException {

        final AssetManager assetManager = ctx.getAssets();

        String[] assets = assetManager.list(srcPath);
        if (assets.length == 0) {
            // ファイル
            copyAssetsFile(ctx, srcPath, dstDir);
        } else {
            // ディレクトリ
            File dstSubDir = new File(dstDir.getAbsolutePath() + File.separator + srcPath);
            if (!dstSubDir.exists()) dstSubDir.mkdir();

            for (String a : assets) {
                copyAssetsFileOrDir(ctx, srcPath + File.separator + a, dstDir);
            }
        }
    }

    private static void copyAssetsFile(Context ctx, String srcFilePath, File dstDir)
            throws FileNotFoundException, IOException {

        final AssetManager assetManager = ctx.getAssets();

        InputStream in = null;
        FileOutputStream out = null;
        try {

            in = assetManager.open(srcFilePath);
            out = new FileOutputStream(dstDir.getAbsolutePath() + File.separator + srcFilePath);
            copyFile(in, out);

        } finally {

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void copyFileOrDir(File src, File dstDir)
            throws FileNotFoundException, IOException {

        if (src.isDirectory()) {

            File dstSubDir = new File(dstDir.getAbsolutePath() + File.separator + src.getName());
            if (!dstSubDir.exists()) dstSubDir.mkdir();

            File[] files = src.listFiles();
            for (File f : files) {
                copyFileOrDir(f, dstSubDir);
            }

        } else {
            copyFile(src, dstDir);
        }
    }

    private static void copyFile(File srcFile, File dstDir)
            throws FileNotFoundException, IOException {

        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(srcFile);
            out = new FileOutputStream(dstDir.getAbsolutePath() + File.separator + srcFile.getName());
            copyFile(in, out);

        } finally {

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static void copyFile(InputStream src, OutputStream dst)
            throws IOException {

        BufferedInputStream in = new BufferedInputStream(src);
        BufferedOutputStream out = new BufferedOutputStream(dst);

        final int bufLen = 1024;
        byte[] buf = new byte[bufLen];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.flush();
    }
}
