package com.myapps.timewrap.ads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.ads.MobileAds;
import com.myapps.timewrap.Utils.SpUtil;



import java.util.ArrayList;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDex;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private static final String ONESIGNAL_APP_ID = "";  //add your one signal key

    public static SharedPreferences sharedPreferencesInApp;
    public static SharedPreferences.Editor editorInApp;

    private AppOpenAdManager appOpenAdManager;
    private Activity currentActivity;


    public static Context context1;




    public static String Type = "admob";   //admob | fb | max | mix


    public static int click = 4;
    public static int backclick = 4;
    public static int click1 = 0;

    public static String MoreApps = "";
    public static String PrivacyPolicy = "";

    public static String strMAX = "max"; // strMAX & Type value is same


    @Override
    public void onCreate() {
        super.onCreate();
        this.registerActivityLifecycleCallbacks(this);

        new Thread(
                () -> {
                    // Initialize the Google Mobile Ads SDK on a background thread.
                    MobileAds.initialize(this, initializationStatus -> {});
                })
                .start();


        sharedPreferencesInApp = getSharedPreferences("my", MODE_PRIVATE);
        editorInApp = sharedPreferencesInApp.edit();


        context1 = getApplicationContext();


        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        appOpenAdManager = new AppOpenAdManager();



        /*app code*/
        SpUtil.getInstance().init(context1);
    }

    @Override
    public void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected void onMoveToForeground() {
        // Show the ad (if available) when the app moves to foreground.
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }

    public void showAdIfAvailable(
            @NonNull Activity activity,
            @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
        appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener);
    }

    public interface OnShowAdCompleteListener {
        void onShowAdComplete();
    }

    private class AppOpenAdManager {

        private static final String LOG_TAG = "AppOpenAdManager";

        private boolean isLoadingAd = false;
        private boolean isShowingAd = false;
        private long loadTime = 0;


        public AppOpenAdManager() {
        }







        private void showAdIfAvailable(
                @NonNull final Activity activity,
                @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
            if (isShowingAd) {
                Log.d(LOG_TAG, "The app open ad is already showing.");
                return;
            }


            Log.d(LOG_TAG, "Will show ad.");

        }
    }

    public static void setuser_balance(Integer user_balance) {
        editorInApp.putInt("user_balance", user_balance).commit();
    }
    public static Integer getuser_balance() {
        return sharedPreferencesInApp.getInt("user_balance", 0);
    }

    public static void setuser_onetime(Integer user_onetime) {
        editorInApp.putInt("user_onetime", user_onetime).commit();
    }
    public static Integer getuser_onetime() {
        return sharedPreferencesInApp.getInt("user_onetime", 0);
    }


}