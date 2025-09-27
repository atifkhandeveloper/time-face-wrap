package com.myapps.timewrap.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SpUtil {
    private static volatile SpUtil mInstance;
    private boolean f176a;
    private String f177b;
    private Context mContext;
    private SharedPreferences mPref;

    private SpUtil() {
    }

    public static SpUtil getInstance() {
        if (mInstance == null) {
            synchronized (SpUtil.class) {
                if (mInstance == null) {
                    mInstance = new SpUtil();
                }
            }
        }
        return mInstance;
    }

    public void init(Context context) {
        if (this.mContext == null) {
            this.mContext = context;
        }
        if (this.mPref == null) {
            this.mPref = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        }
    }

    public void putString(String str, String str2) {
        SharedPreferences.Editor edit = this.mPref.edit();
        edit.putString(str, str2);
        edit.apply();
    }

    public void putLong(String str, long j) {
        SharedPreferences.Editor edit = this.mPref.edit();
        edit.putLong(str, j);
        edit.apply();
    }

    public void putInt(String str, int i) {
        SharedPreferences.Editor edit = this.mPref.edit();
        edit.putInt(str, i);
        edit.apply();
    }

    public void putBoolean(String str, boolean z) {
        SharedPreferences.Editor edit = this.mPref.edit();
        edit.putBoolean(str, z);
        edit.apply();
    }

    public boolean getBoolean(String str) {
        return this.mPref.getBoolean(str, false);
    }

    public boolean getBoolean(String str, boolean z) {
        return this.mPref.getBoolean(str, z);
    }

    public String getString(String str) {
        return this.mPref.getString(str, "");
    }

    public String getString(String str, String str2) {
        return this.mPref.getString(str, str2);
    }

    public long getLong(String str) {
        return this.mPref.getLong(str, 0);
    }

    public long getLong(String str, int i) {
        return this.mPref.getLong(str, (long) i);
    }

    public int getInt(String str) {
        return this.mPref.getInt(str, 0);
    }

    public long getInt(String str, int i) {
        return (long) this.mPref.getInt(str, i);
    }

    public boolean contains(String str) {
        return this.mPref.contains(str);
    }

    public void remove(String str) {
        SharedPreferences.Editor edit = this.mPref.edit();
        edit.remove(str);
        edit.apply();
    }

    public void clear() {
        SharedPreferences.Editor edit = this.mPref.edit();
        edit.clear();
        edit.apply();
    }
}
