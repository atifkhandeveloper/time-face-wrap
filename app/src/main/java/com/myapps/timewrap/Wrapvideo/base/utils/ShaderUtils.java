package com.myapps.timewrap.Wrapvideo.base.utils;

import android.content.Context;
import android.view.Window;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShaderUtils {
    public static void goFullscreen(Window window) {
    }

    public static String getStringFromFileInAssets(Context context, String str) throws IOException {
        return getStringFromFileInAssets(context, str, true);
    }

    public static String getStringFromFileInAssets(Context context, String str, boolean z) throws IOException {
        InputStream open = context.getAssets().open(str);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(open));
        StringBuilder sb = new StringBuilder();
        while (true) {
            String readLine = bufferedReader.readLine();
            if (readLine != null) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(readLine);
                sb2.append(z ? "\n" : "");
                sb.append(sb2.toString());
            } else {
                open.close();
                return sb.toString();
            }
        }
    }
}
