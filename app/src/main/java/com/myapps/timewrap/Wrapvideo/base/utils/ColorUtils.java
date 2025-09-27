package com.myapps.timewrap.Wrapvideo.base.utils;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import androidx.core.content.ContextCompat;
import com.myapps.timewrap.R;

public class ColorUtils {
    public static final int getColor(Context context, int i) {
        if (Build.VERSION.SDK_INT >= 23) {
            return ContextCompat.getColor(context, i);
        }
        return context.getResources().getColor(i);
    }

    public static int[] colorChoice(Context context) {
        String[] stringArray = context.getResources().getStringArray(R.array.default_color_choice_values);
        if (stringArray == null || stringArray.length <= 0) {
            return null;
        }
        int[] iArr = new int[stringArray.length];
        for (int i = 0; i < stringArray.length; i++) {
            iArr[i] = Color.parseColor(stringArray[i]);
        }
        return iArr;
    }
}
