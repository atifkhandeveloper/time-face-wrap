package com.myapps.timewrap.splashAds;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.myapps.timewrap.R;

import androidx.annotation.NonNull;

public class RemoteConfigManager {
    private static final String TAG = "RemoteConfigManager";
    private static RemoteConfigManager instance;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private boolean isInitialized = false;
    private boolean isFetching = false;
    private OnConfigReadyListener configReadyListener;

    // ==================== TESTING FLAG ====================
    // Set to TRUE to force Google's official test ad IDs.
    // Set to FALSE before releasing to production.
    private static final boolean USE_TEST_ADS = false;

    // Google's official test ad unit IDs
    private static final String TEST_BANNER_AD_ID        = "ca-app-pub-3940256099942544/9214589741";
    private static final String TEST_INTERSTITIAL_AD_ID  = "ca-app-pub-3940256099942544/1033173712";
    private static final String TEST_NATIVE_AD_ID        = "ca-app-pub-3940256099942544/2247696110";
    private static final String TEST_APP_OPEN_AD_ID      = "ca-app-pub-3940256099942544/9257395921";
    // ======================================================

    // Remote Config Keys
    public static final String KEY_ENABLE_ADS = "enable_ads";
    public static final String KEY_ENABLE_BANNER = "enable_banner";
    public static final String KEY_ENABLE_INTERSTITIAL = "enable_interstitial";
    public static final String KEY_ENABLE_NATIVE = "enable_native";
    public static final String KEY_ENABLE_APP_OPEN = "enable_app_open";
    public static final String KEY_BANNER_AD_ID = "banner_ad_id";
    public static final String KEY_INTERSTITIAL_AD_ID = "interstitial_ad_id";
    public static final String KEY_NATIVE_AD_ID = "native_ad_id";
    public static final String KEY_APP_OPEN_AD_ID = "app_open_ad_id";
    public static final String KEY_ENABLE_PREMIUM = "enable_premium";
    public static final String KEY_ADS_SHOW_DELAY = "ads_show_delay";
    public static final String KEY_INTERSTITIAL_INTERVAL = "interstitial_interval";
    public static final String KEY_SPLASH_DELAY = "splash_delay";
    public static final String KEY_MAX_AD_COUNT = "max_ad_count";
    public static final String KEY_AD_RESET_INTERVAL = "ad_reset_interval";
    public static final String KEY_NATIVE_AD_TIMEOUT = "native_ad_timeout";

    // Safe defaults - ads disabled until fetched
    private static final boolean DEFAULT_ENABLE_ADS = false;
    private static final boolean DEFAULT_ENABLE_BANNER = false;
    private static final boolean DEFAULT_ENABLE_INTERSTITIAL = false;
    private static final boolean DEFAULT_ENABLE_NATIVE = false;
    private static final boolean DEFAULT_ENABLE_APP_OPEN = false;
    private static final String DEFAULT_BANNER_AD_ID = "";
    private static final String DEFAULT_INTERSTITIAL_AD_ID = "";
    private static final String DEFAULT_NATIVE_AD_ID = "";
    private static final String DEFAULT_APP_OPEN_AD_ID = "";
    private static final boolean DEFAULT_ENABLE_PREMIUM = false;
    private static final long DEFAULT_ADS_SHOW_DELAY = 0;
    private static final long DEFAULT_INTERSTITIAL_INTERVAL = 30;
    private static final long DEFAULT_SPLASH_DELAY = 5000;
    private static final long DEFAULT_MAX_AD_COUNT = 3;
    private static final long DEFAULT_AD_RESET_INTERVAL = 86400;
    private static final long DEFAULT_NATIVE_AD_TIMEOUT = 10000;

    public interface OnConfigReadyListener {
        void onConfigReady();
    }

    private RemoteConfigManager() {
        initializeRemoteConfig();
    }

    public static synchronized RemoteConfigManager getInstance() {
        if (instance == null) {
            instance = new RemoteConfigManager();
        }
        return instance;
    }

    private void initializeRemoteConfig() {
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(USE_TEST_ADS ? 0 : 3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);

        if (USE_TEST_ADS) {
            Log.w(TAG, "⚠️ TEST ADS MODE IS ENABLED — do not release this build!");
        }
    }

    /**
     * Fetch Remote Config and notify listener when done.
     * If already fetched, runs listener immediately.
     */
    public void fetchRemoteConfigSync(final OnConfigReadyListener listener) {
        if (isInitialized) {
            if (listener != null) {
                listener.onConfigReady();
            }
            return;
        }

        if (isFetching) {
            configReadyListener = listener;
            return;
        }

        isFetching = true;
        configReadyListener = listener;

        Log.d(TAG, "Fetching Remote Config...");

        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        isFetching = false;
                        isInitialized = true;

                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            Log.d(TAG, "Remote config fetched and activated: " + updated);
                        } else {
                            Log.e(TAG, "Remote config fetch failed", task.getException());
                        }

                        logAllConfigs();

                        if (configReadyListener != null) {
                            configReadyListener.onConfigReady();
                            configReadyListener = null;
                        }
                    }
                });
    }

    /**
     * Wait for config to be ready, with timeout fallback.
     */
    public void waitForConfig(final OnConfigReadyListener listener, long timeoutMs) {
        if (isInitialized) {
            listener.onConfigReady();
            return;
        }

        if (!isFetching) {
            fetchRemoteConfigSync(listener);
            return;
        }

        configReadyListener = listener;

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isInitialized && configReadyListener != null) {
                    Log.w(TAG, "Remote Config fetch timeout - using defaults");
                    isInitialized = true;
                    configReadyListener.onConfigReady();
                    configReadyListener = null;
                }
            }
        }, timeoutMs);
    }

    public void waitForConfig(final OnConfigReadyListener listener) {
        waitForConfig(listener, 5000);
    }

    public boolean isConfigReady() {
        return isInitialized;
    }

    public boolean areAdIdsAvailable() {
        if (!isInitialized) return false;
        return !getAppOpenAdId().isEmpty()
                || !getBannerAdId().isEmpty()
                || !getInterstitialAdId().isEmpty()
                || !getNativeAdId().isEmpty();
    }

    private void logAllConfigs() {
        Log.d(TAG, "=== Remote Config Values " + (USE_TEST_ADS ? "(TEST MODE)" : "") + " ===");
        Log.d(TAG, "Enable Ads: " + isAdsEnabled());
        Log.d(TAG, "Enable Banner: " + isBannerEnabled());
        Log.d(TAG, "Enable Interstitial: " + isInterstitialEnabled());
        Log.d(TAG, "Enable Native: " + isNativeEnabled());
        Log.d(TAG, "Enable App Open: " + isAppOpenEnabled());
        Log.d(TAG, "Banner Ad ID: " + getBannerAdId());
        Log.d(TAG, "Interstitial Ad ID: " + getInterstitialAdId());
        Log.d(TAG, "Native Ad ID: " + getNativeAdId());
        Log.d(TAG, "App Open Ad ID: " + getAppOpenAdId());
        Log.d(TAG, "Enable Premium: " + isPremiumEnabled());
        Log.d(TAG, "Ads Show Delay: " + getAdsShowDelay());
        Log.d(TAG, "Interstitial Interval: " + getInterstitialInterval());
        Log.d(TAG, "Splash Delay: " + getSplashDelay());
        Log.d(TAG, "Max Ad Count: " + getMaxAdCount());
        Log.d(TAG, "Ad Reset Interval: " + getAdResetInterval() + " seconds");
        Log.d(TAG, "Native Ad Timeout: " + getNativeAdTimeout() + " ms");
        Log.d(TAG, "===========================");
    }

    // ==================== GETTER METHODS ====================

    public boolean isAdsEnabled() {
        if (USE_TEST_ADS) return true;
        if (!isInitialized) return DEFAULT_ENABLE_ADS;
        return mFirebaseRemoteConfig.getBoolean(KEY_ENABLE_ADS);
    }

    public boolean isBannerEnabled() {
        if (USE_TEST_ADS) return true;
        if (!isInitialized) return DEFAULT_ENABLE_BANNER;
        return mFirebaseRemoteConfig.getBoolean(KEY_ENABLE_BANNER);
    }

    public boolean isInterstitialEnabled() {
        if (USE_TEST_ADS) return true;
        if (!isInitialized) return DEFAULT_ENABLE_INTERSTITIAL;
        return mFirebaseRemoteConfig.getBoolean(KEY_ENABLE_INTERSTITIAL);
    }

    public boolean isNativeEnabled() {
        if (USE_TEST_ADS) return true;
        if (!isInitialized) return DEFAULT_ENABLE_NATIVE;
        return mFirebaseRemoteConfig.getBoolean(KEY_ENABLE_NATIVE);
    }

    public boolean isAppOpenEnabled() {
        if (USE_TEST_ADS) return true;
        if (!isInitialized) return DEFAULT_ENABLE_APP_OPEN;
        return mFirebaseRemoteConfig.getBoolean(KEY_ENABLE_APP_OPEN);
    }

    public String getBannerAdId() {
        if (USE_TEST_ADS) return TEST_BANNER_AD_ID;
        if (!isInitialized) return DEFAULT_BANNER_AD_ID;
        String adId = mFirebaseRemoteConfig.getString(KEY_BANNER_AD_ID);
        return (adId != null && !adId.isEmpty()) ? adId : DEFAULT_BANNER_AD_ID;
    }

    public String getInterstitialAdId() {
        if (USE_TEST_ADS) return TEST_INTERSTITIAL_AD_ID;
        if (!isInitialized) return DEFAULT_INTERSTITIAL_AD_ID;
        String adId = mFirebaseRemoteConfig.getString(KEY_INTERSTITIAL_AD_ID);
        return (adId != null && !adId.isEmpty()) ? adId : DEFAULT_INTERSTITIAL_AD_ID;
    }

    public String getNativeAdId() {
        if (USE_TEST_ADS) return TEST_NATIVE_AD_ID;
        if (!isInitialized) return DEFAULT_NATIVE_AD_ID;
        String adId = mFirebaseRemoteConfig.getString(KEY_NATIVE_AD_ID);
        return (adId != null && !adId.isEmpty()) ? adId : DEFAULT_NATIVE_AD_ID;
    }

    public String getAppOpenAdId() {
        if (USE_TEST_ADS) return TEST_APP_OPEN_AD_ID;
        if (!isInitialized) return DEFAULT_APP_OPEN_AD_ID;
        String adId = mFirebaseRemoteConfig.getString(KEY_APP_OPEN_AD_ID);
        return (adId != null && !adId.isEmpty()) ? adId : DEFAULT_APP_OPEN_AD_ID;
    }

    public boolean isPremiumEnabled() {
        if (USE_TEST_ADS) return true;
        if (!isInitialized) return DEFAULT_ENABLE_PREMIUM;
        return mFirebaseRemoteConfig.getBoolean(KEY_ENABLE_PREMIUM);
    }

    public long getAdsShowDelay() {
        if (!isInitialized) return DEFAULT_ADS_SHOW_DELAY;
        return mFirebaseRemoteConfig.getLong(KEY_ADS_SHOW_DELAY);
    }

    public long getInterstitialInterval() {
        if (USE_TEST_ADS) return 0;
        if (!isInitialized) return DEFAULT_INTERSTITIAL_INTERVAL;
        return mFirebaseRemoteConfig.getLong(KEY_INTERSTITIAL_INTERVAL);
    }

    public long getSplashDelay() {
        if (!isInitialized) return DEFAULT_SPLASH_DELAY;
        return mFirebaseRemoteConfig.getLong(KEY_SPLASH_DELAY);
    }

    public long getMaxAdCount() {
        if (!isInitialized) return DEFAULT_MAX_AD_COUNT;
        return mFirebaseRemoteConfig.getLong(KEY_MAX_AD_COUNT);
    }

    public long getAdResetInterval() {
        if (!isInitialized) return DEFAULT_AD_RESET_INTERVAL;
        return mFirebaseRemoteConfig.getLong(KEY_AD_RESET_INTERVAL);
    }

    public long getNativeAdTimeout() {
        if (!isInitialized) return DEFAULT_NATIVE_AD_TIMEOUT;
        return mFirebaseRemoteConfig.getLong(KEY_NATIVE_AD_TIMEOUT);
    }

    public void refresh() {
        isInitialized = false;
        fetchRemoteConfigSync(null);
    }

    // ==================== DEBUG METHODS ====================

    public void logAllConfigsDebug() {
        logAllConfigs();
    }
}