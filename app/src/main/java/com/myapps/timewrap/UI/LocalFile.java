package com.myapps.timewrap.UI;

import android.content.Context;
import android.content.SharedPreferences;

public class LocalFile {
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private final static String KEY_LAST_VISIBLE_ITEM = "key_lastVisiblePageNumber";
    private final static String KEY_BOOK_MARKS = "key_bookmarks";
    private final static String KEY_NIGHT_MODE = "key_night_mode";

    public LocalFile(Context context) {
        pref = context.getSharedPreferences(AppConstants.PRE_NAME, Context.MODE_PRIVATE); // 0 - for private mode
        editor = pref.edit();
    }



    public boolean isNightMode() {
        return pref.getBoolean(KEY_NIGHT_MODE, false);
    }

    public void setNightMode(boolean flag) {
        editor.putBoolean(KEY_NIGHT_MODE, flag);
        editor.commit();
        editor.apply();
    }

    public int getLastVisiblePage() {
        return pref.getInt(KEY_LAST_VISIBLE_ITEM, 0);
    }

    public void setLastVisiblePage(int page, boolean isFromBookMark) {
        if (isFromBookMark) {
            if (page < 1) {
                editor.putInt(KEY_LAST_VISIBLE_ITEM, 0);
            } else {
                editor.putInt(KEY_LAST_VISIBLE_ITEM, page - 1);
            }
        } else {
            editor.putInt(KEY_LAST_VISIBLE_ITEM, page);
        }

        editor.commit();
        editor.apply();
    }

    public String getBookMarks() {
        return pref.getString(KEY_BOOK_MARKS, null);
    }

    public void setBookMarks(String bookMarks) {
        editor.putString(KEY_BOOK_MARKS, bookMarks);
        editor.commit();
        editor.apply();
    }
}
