package com.myapps.timewrap.ads.admob;

import android.content.Context;
import android.content.SharedPreferences;

public class AdMobSharedPref {

    public static final String CLICK = "CLICK";
    public static final String PREF_NAME = "PREF_PROFILE";

    public static int getInteger(Context context, String str, int i) {
        return getPreferences(context).getInt(str, i);
    }

    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, 0);
    }

}
