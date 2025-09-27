package com.myapps.timewrap.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.io.File;

public class C1197util {
    public static String MyWork = "MY_WORK";
    public static Bitmap bitmap = null;
    public static File waterVideo = null;
    public static String waterfallVideo = "WATERFALL_VIDEO";
    public static String wrapImage = "WRAP_IMAGE";
    public static String wrapImagePath = null;
    public static String wrapVideo = "WRAP_VIDEO";
    public static File wrapVideoFile;

    public static boolean isOnline(Context context) {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }
}
