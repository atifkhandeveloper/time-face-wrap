package com.myapps.timewrap.ads;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDex;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.firebase.FirebaseApp;
import com.myapps.timewrap.R;

import java.util.Date;

public class MyApplication extends Application
        implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private Activity currentActivity;
    private AppOpenAdManager appOpenAdManager;
    private boolean isAppInBackground = true;

    @Override
    public void onCreate() {
        super.onCreate();

        MultiDex.install(this);
        FirebaseApp.initializeApp(this);
        MobileAds.initialize(this);

        registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        appOpenAdManager = new AppOpenAdManager(this);
    }

    // ================= SPLASH =================

    public void showAdAfterSplash(Activity activity, Runnable onFinish) {
        appOpenAdManager.showAdIfAvailable(activity, onFinish);
    }

    // ================= FOREGROUND DETECTION =================

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (currentActivity != null && isAppInBackground) {
            appOpenAdManager.showAdIfAvailable(currentActivity, () -> {});
        }
        isAppInBackground = false;
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        isAppInBackground = true;
    }

    // ================= ACTIVITY TRACKING =================

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
        appOpenAdManager.loadAd(activity);
    }

    @Override public void onActivityCreated(@NonNull Activity a, Bundle b) { currentActivity = a; }
    @Override public void onActivityStarted(@NonNull Activity a) {}
    @Override public void onActivityPaused(@NonNull Activity a) {}
    @Override public void onActivityStopped(@NonNull Activity a) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity a, Bundle b) {}
    @Override public void onActivityDestroyed(@NonNull Activity a) {}

    // ================= AD MANAGER =================

    private static class AppOpenAdManager {

        private final Context context;
        private final String adUnitId;

        private AppOpenAd appOpenAd;
        private boolean isLoading = false;
        private boolean isShowing = false;
        private long loadTime = 0;

        AppOpenAdManager(Context ctx) {
            context = ctx.getApplicationContext();
            adUnitId = context.getString(R.string.appopen);
        }

        boolean isAdValid() {
            return appOpenAd != null &&
                    (new Date().getTime() - loadTime) < 4 * 60 * 60 * 1000;
        }

        void loadAd(Context ctx) {
            if (isLoading || isAdValid()) return;

            isLoading = true;

            AppOpenAd.load(
                    ctx,
                    adUnitId,
                    new AdRequest.Builder().build(),
                    new AppOpenAd.AppOpenAdLoadCallback() {

                        @Override
                        public void onAdLoaded(@NonNull AppOpenAd ad) {
                            appOpenAd = ad;
                            loadTime = new Date().getTime();
                            isLoading = false;
                            Log.d(TAG, "App Open Ad Loaded");
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError error) {
                            isLoading = false;
                            Log.e(TAG, "Ad load failed: " + error.getMessage());
                        }
                    }
            );
        }

        void showAdIfAvailable(Activity activity, Runnable onFinish) {

            if (isShowing) return;

            if (!isAdValid()) {
                loadAd(activity);
                onFinish.run();
                return;
            }

            appOpenAd.setFullScreenContentCallback(
                    new FullScreenContentCallback() {

                        @Override
                        public void onAdDismissedFullScreenContent() {
                            appOpenAd = null;
                            isShowing = false;
                            onFinish.run();
                            loadAd(context);
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            appOpenAd = null;
                            isShowing = false;
                            onFinish.run();
                            loadAd(context);
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            isShowing = true;
                        }
                    });

            appOpenAd.show(activity);
        }
    }
}
