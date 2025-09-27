package com.myapps.timewrap.Wrapvideo.base.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {
    private static final String APP_SETTINGS = "com.time.freeze.settings";
    public static final String CAMERA_ID = "CAMERA_ID";
    public static final String SCAN_COLOR = "SCAN_COLOR";
    public static final String SCAN_DIRECTION = "SCAN_DIRECTION";
    public static final String SCAN_FILTER_FULLIMAGE = "SCAN_FILTER_FULLIMAGE";
    public static final String SCAN_FILTER_IMAGE = "SCAN_FILTER_IMAGE";
    public static final String SCAN_FILTER_NAME = "SCAN_FILTER_NAME";
    public static final String SCAN_SAVE_IMAGE = "SAVE_IMAGE";
    public static final String SCAN_SHAPE = "SCAN_SHAPE";
    public static final String SCAN_SPEED = "SCAN_SPEED";
    public static final String SHOW_HELP_MESSAGE = "HELP_MESSAGE";

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(APP_SETTINGS, 0);
    }

    public static String getString(Context context, String str, String str2) {
        return getSharedPreferences(context).getString(str, (String) null);
    }

    public static void setString(Context context, String str, String str2) {
        SharedPreferences.Editor edit = getSharedPreferences(context).edit();
        edit.putString(str, str2);
        edit.apply();
    }

    public static int getInt(Context context, String str, int i) {
        return getSharedPreferences(context).getInt(str, i);
    }

    public static boolean getBoolean(Context context, String str) {
        return getSharedPreferences(context).getBoolean(str, false);
    }

    public static void setBoolean(Context context, String str, boolean z) {
        SharedPreferences.Editor edit = getSharedPreferences(context).edit();
        edit.putBoolean(str, z);
        edit.apply();
    }

    public static void setInt(Context context, String str, int i) {
        SharedPreferences.Editor edit = getSharedPreferences(context).edit();
        edit.putInt(str, i);
        edit.apply();
    }
}
