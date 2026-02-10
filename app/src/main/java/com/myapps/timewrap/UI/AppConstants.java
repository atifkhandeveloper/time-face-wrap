package com.myapps.timewrap.UI;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.Objects;

public class AppConstants {
    public final static boolean WILL_TEST_MEDIATION = false;
    public final static boolean IS_TEST_AD = false;
    public static boolean WILL_DISPLAY_BANNER_ADS = true;
    public static boolean WILL_DISPLAY_INTERSTITIAL_AD_TO_USER = true;
    public static int pageCounter = 0;
    public final static int INTERSTITIAL_AD_PAGES = 10; // per page
    public final static String PRE_NAME = "Time Warp"; // per page

    //Update your app to pro version and enjoy novel reading without Ads. Dark Mode supported


    public static boolean isConnectedToAnyNetwork(Context context) {
        NetworkInfo info = getNetworkInfo(context);
        return (info != null && info.isConnected());
    }

    private static NetworkInfo getNetworkInfo(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return Objects.requireNonNull(connectivityManager).getActiveNetworkInfo();
    }

}
