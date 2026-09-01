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
import com.myapps.timewrap.UI.PremiumManager;

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
        // ✅ Check if user is premium - skip ads
        if (PremiumManager.isPremium(activity)) {
            Log.d("MyApplication", "Premium user - skipping splash ad");
            onFinish.run();
            return;
        }
        appOpenAdManager.showAdIfAvailable(activity, onFinish);
    }

    // ================= FOREGROUND DETECTION =================

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (currentActivity != null && isAppInBackground) {
            // ✅ Check if user is premium - skip ads
            if (PremiumManager.isPremium(currentActivity)) {
                Log.d("MyApplication", "Premium user - skipping foreground ad");
                return;
            }
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
        // ✅ Only load ad if user is NOT premium
        if (!PremiumManager.isPremium(activity)) {
            appOpenAdManager.loadAd(activity);
        } else {
            Log.d("MyApplication", "Premium user - not loading ads");
        }
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
            // ✅ Don't load if already loading or ad is valid
            if (isLoading || isAdValid()) return;

            // ✅ Double-check premium status before loading
            if (PremiumManager.isPremium(ctx)) {
                Log.d(TAG, "Premium user - skipping ad load");
                return;
            }

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
            // ✅ Don't show if already showing
            if (isShowing) {
                onFinish.run();
                return;
            }

            // ✅ Check premium status before showing
            if (PremiumManager.isPremium(activity)) {
                Log.d(TAG, "Premium user - skipping app open ad");
                onFinish.run();
                return;
            }

            // ✅ If ad is not valid, load a new one and skip showing
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
                            // ✅ Only reload if not premium
                            if (!PremiumManager.isPremium(context)) {
                                loadAd(context);
                            }
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            appOpenAd = null;
                            isShowing = false;
                            onFinish.run();
                            // ✅ Only reload if not premium
                            if (!PremiumManager.isPremium(context)) {
                                loadAd(context);
                            }
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            isShowing = true;
                            Log.d(TAG, "App Open Ad showed");
                        }
                    });

            appOpenAd.show(activity);
        }
    }
}