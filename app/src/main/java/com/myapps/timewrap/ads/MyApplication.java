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

import com.facebook.ads.AudienceNetworkAds;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.myapps.timewrap.UI.PremiumManager;
import com.myapps.timewrap.splashAds.RemoteConfigManager;

import java.util.Date;

public class MyApplication extends Application
        implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private Activity currentActivity;
    private AppOpenAdManager appOpenAdManager;
    private boolean isAppInBackground = true;
    private RemoteConfigManager remoteConfigManager;

    // Firebase Analytics
    private FirebaseAnalytics mFirebaseAnalytics;

    // Flag: Remote Config ready (volatile because accessed from multiple threads)
    private volatile boolean remoteConfigReady = false;

    @Override
    public void onCreate() {

        AudienceNetworkAds.initialize(this);
        try {
            super.onCreate();
        } catch (Exception e) {
            Log.e("MyApplication", "Error in super.onCreate: " + e.getMessage());
        }

        try {
            MultiDex.install(this);
        } catch (Exception e) {
            Log.e("MyApplication", "MultiDex install failed: " + e.getMessage());
        }

        try {
            FirebaseApp.initializeApp(this);
        } catch (Exception e) {
            Log.e("MyApplication", "Firebase initialization failed: " + e.getMessage());
        }

        // Initialize Firebase Analytics
        try {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            Log.d("MyApplication", "Firebase Analytics initialized");
        } catch (Exception e) {
            Log.e("MyApplication", "Firebase Analytics initialization failed: " + e.getMessage());
        }

        // Initialize MobileAds with completion callback
        try {
            MobileAds.initialize(this, initializationStatus ->
                    Log.d("MyApplication", "MobileAds initialized: "
                            + initializationStatus.getAdapterStatusMap()));
        } catch (Exception e) {
            Log.e("MyApplication", "MobileAds initialization failed: " + e.getMessage());
        }

        // Initialize Remote Config
        try {
            remoteConfigManager = RemoteConfigManager.getInstance();
            remoteConfigManager.logAllConfigsDebug();
        } catch (Exception e) {
            Log.e("MyApplication", "Remote Config initialization failed: " + e.getMessage());
        }

        try {
            registerActivityLifecycleCallbacks(this);
            ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        } catch (Exception e) {
            Log.e("MyApplication", "Lifecycle registration failed: " + e.getMessage());
        }

        try {
            appOpenAdManager = new AppOpenAdManager(this);
        } catch (Exception e) {
            Log.e("MyApplication", "AppOpenAdManager initialization failed: " + e.getMessage());
        }

        // CRITICAL: Wait for Remote Config BEFORE any ads load.
        // Once ready, preload the App Open ad immediately (no Activity dependency).
        try {
            remoteConfigManager.waitForConfig(() -> {
                remoteConfigReady = true;
                Log.d("MyApplication", "Remote Config ready - real ad IDs available");
                Log.d("MyApplication", "App Open Ad ID: " + remoteConfigManager.getAppOpenAdId());
                Log.d("MyApplication", "Banner Ad ID: " + remoteConfigManager.getBannerAdId());
                Log.d("MyApplication", "Interstitial Ad ID: " + remoteConfigManager.getInterstitialAdId());
                Log.d("MyApplication", "Native Ad ID: " + remoteConfigManager.getNativeAdId());

                // Preload the App Open ad immediately, regardless of currentActivity
                if (appOpenAdManager != null) {
                    boolean isPremium = currentActivity != null
                            && PremiumManager.isPremium(currentActivity)
                            && remoteConfigManager.isPremiumEnabled();
                    boolean adsEnabled = remoteConfigManager.isAdsEnabled();
                    boolean appOpenEnabled = remoteConfigManager.isAppOpenEnabled();

                    if (!isPremium && adsEnabled && appOpenEnabled) {
                        Log.d("MyApplication", "Preloading App Open Ad after Remote Config ready");
                        appOpenAdManager.setAdUnitId(remoteConfigManager.getAppOpenAdId());
                        appOpenAdManager.preload();
                    }
                }
            }, 5000); // 5 second timeout
        } catch (Exception e) {
            Log.e("MyApplication", "Remote Config wait failed: " + e.getMessage());
            remoteConfigReady = true;
        }
    }

    // ================= SPLASH =================

    public void showAdAfterSplash(Activity activity, Runnable onFinish) {
        try {
            boolean isPremium = PremiumManager.isPremium(activity) && remoteConfigManager.isPremiumEnabled();
            boolean adsEnabled = remoteConfigManager.isAdsEnabled();
            boolean appOpenEnabled = remoteConfigManager.isAppOpenEnabled();

            if (isPremium || !adsEnabled || !appOpenEnabled) {
                Log.d("MyApplication", "Skipping splash ad - Premium: " + isPremium
                        + ", Ads Enabled: " + adsEnabled
                        + ", App Open Enabled: " + appOpenEnabled);
                onFinish.run();
                return;
            }

            // If config isn't ready yet, wait for it, then show
            if (!remoteConfigReady) {
                Log.d("MyApplication", "Remote Config not ready - waiting before showing splash ad");
                remoteConfigManager.waitForConfig(() -> {
                    remoteConfigReady = true;
                    String appOpenAdId = remoteConfigManager.getAppOpenAdId();
                    Log.d("MyApplication", "App Open Ad ID (after wait): " + appOpenAdId);
                    appOpenAdManager.setAdUnitId(appOpenAdId);
                    appOpenAdManager.showOrWait(activity, onFinish);
                }, 5000);
                return;
            }

            String appOpenAdId = remoteConfigManager.getAppOpenAdId();
            appOpenAdManager.setAdUnitId(appOpenAdId);
            Log.d("MyApplication", "App Open Ad ID from Remote Config: " + appOpenAdId);

            appOpenAdManager.showOrWait(activity, onFinish);

        } catch (Exception e) {
            Log.e("MyApplication", "Error showing splash ad: " + e.getMessage());
            onFinish.run();
        }
    }

    // ================= FOREGROUND DETECTION =================

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        try {
            if (currentActivity != null && isAppInBackground) {
                if (!remoteConfigReady) {
                    Log.d("MyApplication", "Remote Config not ready - skipping foreground ad");
                    isAppInBackground = false;
                    return;
                }

                boolean isPremium = PremiumManager.isPremium(currentActivity)
                        && remoteConfigManager.isPremiumEnabled();
                boolean adsEnabled = remoteConfigManager.isAdsEnabled();
                boolean appOpenEnabled = remoteConfigManager.isAppOpenEnabled();

                if (isPremium || !adsEnabled || !appOpenEnabled) {
                    Log.d("MyApplication", "Skipping foreground ad - Premium: " + isPremium
                            + ", Ads Enabled: " + adsEnabled
                            + ", App Open Enabled: " + appOpenEnabled);
                    isAppInBackground = false;
                    return;
                }

                String appOpenAdId = remoteConfigManager.getAppOpenAdId();
                appOpenAdManager.setAdUnitId(appOpenAdId);

                // For foreground resume, no callback is needed
                appOpenAdManager.showAdIfAvailable(currentActivity, () -> {});
            }
            isAppInBackground = false;
        } catch (Exception e) {
            Log.e("MyApplication", "Error in onStart: " + e.getMessage());
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        isAppInBackground = true;
    }

    // ================= ACTIVITY TRACKING =================

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        try {
            currentActivity = activity;

            if (!remoteConfigReady) {
                Log.d("MyApplication", "Remote Config not ready - skipping ad load on activity resume");
                return;
            }

            boolean isPremium = PremiumManager.isPremium(activity)
                    && remoteConfigManager.isPremiumEnabled();
            boolean adsEnabled = remoteConfigManager.isAdsEnabled();
            boolean appOpenEnabled = remoteConfigManager.isAppOpenEnabled();

            if (!isPremium && adsEnabled && appOpenEnabled) {
                String appOpenAdId = remoteConfigManager.getAppOpenAdId();
                appOpenAdManager.setAdUnitId(appOpenAdId);
                // Only preload if no ad is already loaded or loading
                if (!appOpenAdManager.isAdValid() && !appOpenAdManager.isLoading()) {
                    appOpenAdManager.preload();
                }
            } else {
                Log.d("MyApplication", "Skipping ad load - Premium: " + isPremium
                        + ", Ads Enabled: " + adsEnabled
                        + ", App Open Enabled: " + appOpenEnabled);
            }
        } catch (Exception e) {
            Log.e("MyApplication", "Error in onActivityResumed: " + e.getMessage());
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity a, Bundle b) {
        try {
            currentActivity = a;
        } catch (Exception e) {
            Log.e("MyApplication", "Error in onActivityCreated: " + e.getMessage());
        }
    }

    @Override public void onActivityStarted(@NonNull Activity a) {}
    @Override public void onActivityPaused(@NonNull Activity a) {}
    @Override public void onActivityStopped(@NonNull Activity a) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity a, Bundle b) {}
    @Override public void onActivityDestroyed(@NonNull Activity a) {}

    // ================= FIREBASE ANALYTICS HELPERS =================

    private void logAdImpression(String adFormat, String adUnitId) {
        try {
            if (mFirebaseAnalytics == null) return;
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.AD_PLATFORM, "admob");
            bundle.putString(FirebaseAnalytics.Param.AD_SOURCE, "admob");
            bundle.putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat);
            bundle.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, bundle);
            Log.d("MyApplication", "Logged ad_impression event - Format: " + adFormat);
        } catch (Exception e) {
            Log.e("MyApplication", "Error logging ad_impression: " + e.getMessage());
        }
    }

    private void logAdClick(String adFormat, String adUnitId) {
        try {
            if (mFirebaseAnalytics == null) return;
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.AD_PLATFORM, "admob");
            bundle.putString(FirebaseAnalytics.Param.AD_SOURCE, "admob");
            bundle.putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat);
            bundle.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
            // mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_CLICK, bundle);
            Log.d("MyApplication", "Logged ad_click event - Format: " + adFormat);
        } catch (Exception e) {
            Log.e("MyApplication", "Error logging ad_click: " + e.getMessage());
        }
    }

    private void sendRevenueToFirebase(double value, String currency, String adFormat, String adUnitId) {
        try {
            if (mFirebaseAnalytics == null) return;
            if (value > 0) {
                Bundle bundle = new Bundle();
                bundle.putDouble(FirebaseAnalytics.Param.VALUE, value);
                bundle.putString(FirebaseAnalytics.Param.CURRENCY, currency);
                bundle.putString(FirebaseAnalytics.Param.AD_PLATFORM, "admob");
                bundle.putString(FirebaseAnalytics.Param.AD_SOURCE, "admob");
                bundle.putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat);
                bundle.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, bundle);
                Log.d("MyApplication", "Revenue sent to Firebase: " + value + " "
                        + currency + " (" + adFormat + ")");
            }
        } catch (Exception e) {
            Log.e("MyApplication", "Error sending revenue to Firebase: " + e.getMessage());
        }
    }

    // ================= AD MANAGER =================

    private class AppOpenAdManager {

        private final Context context;
        private String adUnitId;
        private AppOpenAd appOpenAd;
        private boolean isLoading = false;
        private boolean isShowing = false;
        private long loadTime = 0;
        private RemoteConfigManager remoteConfigManager;

        // Pending splash request (used when ad is still loading)
        private Activity pendingShowActivity;
        private Runnable pendingShowFinish;

        AppOpenAdManager(Context ctx) {
            context = ctx.getApplicationContext();
            adUnitId = null;
            remoteConfigManager = RemoteConfigManager.getInstance();
        }

        void setAdUnitId(String adUnitId) {
            if (adUnitId != null && !adUnitId.isEmpty()) {
                this.adUnitId = adUnitId;
            }
        }

        boolean isLoading() {
            return isLoading;
        }

        boolean isAdValid() {
            return appOpenAd != null
                    && (new Date().getTime() - loadTime) < 4 * 60 * 60 * 1000;
        }

        /**
         * Preload the ad without any Activity dependency.
         * Safe to call as soon as the ad unit ID is known.
         */
        void preload() {
            try {
                if (!remoteConfigReady) {
                    Log.d(TAG, "Remote Config not ready - skipping preload");
                    return;
                }

                if (adUnitId == null || adUnitId.isEmpty()) {
                    Log.e(TAG, "preload: adUnitId is null/empty - aborting");
                    return;
                }

                if (isLoading || isAdValid()) {
                    Log.d(TAG, "preload: ad already loading or valid - skipping");
                    return;
                }

                boolean isPremium = currentActivity != null
                        && PremiumManager.isPremium(currentActivity)
                        && remoteConfigManager.isPremiumEnabled();
                boolean adsEnabled = remoteConfigManager.isAdsEnabled();
                boolean appOpenEnabled = remoteConfigManager.isAppOpenEnabled();

                if (isPremium || !adsEnabled || !appOpenEnabled) {
                    Log.d(TAG, "preload: skipping - Premium: " + isPremium
                            + ", Ads Enabled: " + adsEnabled
                            + ", App Open Enabled: " + appOpenEnabled);
                    return;
                }

                isLoading = true;
                Log.d(TAG, "Preloading App Open Ad: " + adUnitId);

                AppOpenAd.load(
                        context,
                        adUnitId,
                        new AdRequest.Builder().build(),
                        new AppOpenAd.AppOpenAdLoadCallback() {
                            @Override
                            public void onAdLoaded(@NonNull AppOpenAd ad) {
                                appOpenAd = ad;
                                loadTime = new Date().getTime();
                                isLoading = false;
                                Log.d(TAG, "App Open Ad preloaded successfully");

                                ad.setOnPaidEventListener(adValue -> {
                                    double revenue = adValue.getValueMicros() / 1_000_000.0;
                                    String currency = adValue.getCurrencyCode();
                                    Log.d(TAG, "App Open paid event - Revenue: " + revenue + " " + currency);
                                    sendRevenueToFirebase(revenue, currency, "app_open", adUnitId);
                                });

                                // If a splash request is waiting, show the ad now
                                if (pendingShowActivity != null && pendingShowFinish != null) {
                                    Activity act = pendingShowActivity;
                                    Runnable finish = pendingShowFinish;
                                    pendingShowActivity = null;
                                    pendingShowFinish = null;
                                    showAdIfAvailable(act, finish);
                                }
                            }

                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                                isLoading = false;
                                Log.e(TAG, "App Open preload failed: " + error.getMessage());

                                // Notify any waiting splash that the ad failed
                                if (pendingShowFinish != null) {
                                    Runnable finish = pendingShowFinish;
                                    pendingShowActivity = null;
                                    pendingShowFinish = null;
                                    finish.run();
                                }
                            }
                        }
                );
            } catch (Exception e) {
                isLoading = false;
                Log.e(TAG, "Error in preload: " + e.getMessage());

                if (pendingShowFinish != null) {
                    Runnable finish = pendingShowFinish;
                    pendingShowActivity = null;
                    pendingShowFinish = null;
                    finish.run();
                }
            }
        }

        /**
         * Show the ad if it is loaded.
         * If it is still loading, register a callback and show it when ready.
         * If it is not loading at all, start a preload and register the callback.
         */
        void showOrWait(Activity activity, Runnable onFinish) {
            // Already loaded -> show immediately
            if (isAdValid()) {
                Log.d(TAG, "showOrWait: ad already valid - showing now");
                showAdIfAvailable(activity, onFinish);
                return;
            }

            // Still loading -> wait for it
            if (isLoading) {
                Log.d(TAG, "showOrWait: ad still loading - waiting");
                pendingShowActivity = activity;
                pendingShowFinish = onFinish;
                return;
            }

            // Not loading -> start a preload and wait
            Log.d(TAG, "showOrWait: ad not loaded - starting preload");
            pendingShowActivity = activity;
            pendingShowFinish = onFinish;
            preload();
        }

        /**
         * Standard preload method (kept for compatibility with activity resume).
         */
        void loadAd(Context ctx) {
            preload();
        }

        void showAdIfAvailable(Activity activity, Runnable onFinish) {
            try {
                if (!remoteConfigReady) {
                    Log.d(TAG, "Remote Config not ready - skipping show");
                    if (onFinish != null) onFinish.run();
                    return;
                }

                if (isShowing) {
                    Log.d(TAG, "Ad already showing");
                    if (onFinish != null) onFinish.run();
                    return;
                }

                boolean isPremium = PremiumManager.isPremium(activity)
                        && remoteConfigManager.isPremiumEnabled();
                boolean adsEnabled = remoteConfigManager.isAdsEnabled();
                boolean appOpenEnabled = remoteConfigManager.isAppOpenEnabled();

                if (isPremium || !adsEnabled || !appOpenEnabled) {
                    Log.d(TAG, "Skipping app open ad - Premium: " + isPremium
                            + ", Ads Enabled: " + adsEnabled
                            + ", App Open Enabled: " + appOpenEnabled);
                    if (onFinish != null) onFinish.run();
                    return;
                }

                if (!isAdValid()) {
                    Log.d(TAG, "Ad not valid, loading new ad");
                    preload();
                    if (onFinish != null) onFinish.run();
                    return;
                }

                Log.d(TAG, "Showing App Open Ad");
                appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        appOpenAd = null;
                        isShowing = false;
                        Log.d(TAG, "App Open Ad dismissed");
                        if (onFinish != null) onFinish.run();

                        boolean isPremiumReload = PremiumManager.isPremium(context)
                                && remoteConfigManager.isPremiumEnabled();
                        boolean adsEnabledReload = remoteConfigManager.isAdsEnabled();
                        boolean appOpenEnabledReload = remoteConfigManager.isAppOpenEnabled();

                        if (!isPremiumReload && adsEnabledReload && appOpenEnabledReload) {
                            preload();
                        }
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        appOpenAd = null;
                        isShowing = false;
                        Log.e(TAG, "Ad failed to show: " + adError.getMessage());
                        if (onFinish != null) onFinish.run();

                        boolean isPremiumReload = PremiumManager.isPremium(context)
                                && remoteConfigManager.isPremiumEnabled();
                        boolean adsEnabledReload = remoteConfigManager.isAdsEnabled();
                        boolean appOpenEnabledReload = remoteConfigManager.isAppOpenEnabled();

                        if (!isPremiumReload && adsEnabledReload && appOpenEnabledReload) {
                            preload();
                        }
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        isShowing = true;
                        Log.d(TAG, "App Open Ad showed");
                        logAdImpression("app_open", adUnitId);
                    }

                    @Override
                    public void onAdClicked() {
                        Log.d(TAG, "App Open Ad clicked");
                        logAdClick("app_open", adUnitId);
                    }

                    @Override
                    public void onAdImpression() {
                        Log.d(TAG, "App Open Ad impression");
                    }
                });

                appOpenAd.show(activity);
            } catch (Exception e) {
                Log.e(TAG, "Error showing ad: " + e.getMessage());
                if (onFinish != null) onFinish.run();
            }
        }
    }
}