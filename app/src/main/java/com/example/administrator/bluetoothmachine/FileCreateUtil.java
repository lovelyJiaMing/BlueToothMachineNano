package com.example.administrator.bluetoothmachine;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by gaoyx on 2016/4/5.
 */
public class FileCreateUtil {

    public static String makeDir(String folder, boolean isUseSdcard, Context context) {
        File appCacheDir = null;
        String externalStorageState;
        try {
            externalStorageState = Environment.getExternalStorageState();
        } catch (NullPointerException e) {
            externalStorageState = "";
        } catch (IncompatibleClassChangeError e) {
            externalStorageState = "";
        }
        if (isUseSdcard && Environment.MEDIA_MOUNTED.equals(externalStorageState)) {
            appCacheDir = getExternalCacheDir(context, folder);
        }
        if (appCacheDir == null) {
            appCacheDir = new File(context.getFilesDir(), folder);
            if (!appCacheDir.exists()) {
                appCacheDir.mkdirs();
            }
        }
        return appCacheDir.getAbsolutePath() + "/";
    }

    private static File getExternalCacheDir(Context context, String folder) {
        File appCacheDir = new File(Environment.getExternalStorageDirectory(), folder);
        if (!appCacheDir.isDirectory()) {
            if (appCacheDir.exists()) {
                appCacheDir.delete();
            }
            appCacheDir.mkdirs();
        }
        return appCacheDir;
    }
}
