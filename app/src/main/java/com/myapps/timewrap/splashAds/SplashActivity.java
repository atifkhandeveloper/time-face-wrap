package com.myapps.timewrap.splashAds;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

import com.myapps.timewrap.R;
import com.myapps.timewrap.ads.MyApplication;
import com.myapps.timewrap.UI.PremiumManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "Splash";
    private static final long SPLASH_DELAY = 5000;

    private boolean isPremium = false;
    private boolean adsEnabled = true;
    private RemoteConfigManager remoteConfigManager;
    private boolean adFlowStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableEdgeToEdge();
        setContentView(R.layout.activity_splash);
        applyWindowInsets();

        // ✅ Get instance (do NOT read config values yet)
        remoteConfigManager = RemoteConfigManager.getInstance();

        // ✅ Fetch config FIRST, then read values inside the callback
        remoteConfigManager.fetchRemoteConfigSync(() -> {

            // 🔍 DEBUG LOG — after config is ready
            Log.d(TAG, "=== Remote Config READY ===");
            Log.d(TAG, "Ads Enabled       = " + remoteConfigManager.isAdsEnabled());
            Log.d(TAG, "Banner Enabled    = " + remoteConfigManager.isBannerEnabled());
            Log.d(TAG, "Interstitial En.  = " + remoteConfigManager.isInterstitialEnabled());
            Log.d(TAG, "Native Enabled    = " + remoteConfigManager.isNativeEnabled());
            Log.d(TAG, "App Open Enabled  = " + remoteConfigManager.isAppOpenEnabled());
            Log.d(TAG, "Premium Enabled   = " + remoteConfigManager.isPremiumEnabled());
            Log.d(TAG, "App Open Ad ID    = [" + remoteConfigManager.getAppOpenAdId() + "]");
            Log.d(TAG, "Banner Ad ID      = [" + remoteConfigManager.getBannerAdId() + "]");
            Log.d(TAG, "Interstitial ID   = [" + remoteConfigManager.getInterstitialAdId() + "]");
            Log.d(TAG, "Native Ad ID      = [" + remoteConfigManager.getNativeAdId() + "]");
            Log.d(TAG, "===========================");

            // ✅ Now safely read values
            isPremium = PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled();
            adsEnabled = remoteConfigManager.isAdsEnabled();
            boolean appOpenEnabled = remoteConfigManager.isAppOpenEnabled();

            Log.d(TAG, "Premium: " + isPremium
                    + ", Ads: " + adsEnabled
                    + ", AppOpen: " + appOpenEnabled);

            // ✅ Start splash delay timer only AFTER config is ready
            new Handler().postDelayed(() -> {

                // Guard against double-execution from onResume
                if (adFlowStarted) {
                    Log.d(TAG, "Ad flow already started, skipping duplicate.");
                    return;
                }
                adFlowStarted = true;

                if (isPremium || !adsEnabled || !appOpenEnabled) {
                    Log.d(TAG, "Skipping ads - Premium: " + isPremium
                            + ", Ads Enabled: " + adsEnabled
                            + ", App Open Enabled: " + appOpenEnabled);
                    goToNextActivity();
                } else {
                    Log.d(TAG, "Free user - showing app open ad");
                    MyApplication app = (MyApplication) getApplication();
                    app.showAdAfterSplash(SplashActivity.this, this::goToNextActivity);
                }
            }, SPLASH_DELAY);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ✅ Only refresh if config was already fetched at least once
        if (remoteConfigManager == null || !remoteConfigManager.isConfigReady()) {
            return;
        }

        remoteConfigManager.refresh();

        // ✅ Wait for refresh to complete before reading values
        remoteConfigManager.fetchRemoteConfigSync(() -> {

            boolean currentPremium = PremiumManager.isPremium(this)
                    && remoteConfigManager.isPremiumEnabled();
            boolean currentAdsEnabled = remoteConfigManager.isAdsEnabled();
            boolean currentAppOpenEnabled = remoteConfigManager.isAppOpenEnabled();

            Log.d(TAG, "onResume config — Premium: " + currentPremium
                    + ", Ads: " + currentAdsEnabled
                    + ", AppOpen: " + currentAppOpenEnabled);

            boolean statusChanged = (currentPremium != isPremium)
                    || (currentAdsEnabled != adsEnabled);

            if (statusChanged) {
                isPremium = currentPremium;
                adsEnabled = currentAdsEnabled;
                Log.d(TAG, "Status changed - Premium: " + isPremium
                        + ", Ads Enabled: " + adsEnabled
                        + ", App Open Enabled: " + currentAppOpenEnabled);
            }
        });
    }

    private void goToNextActivity() {
        startActivity(new Intent(SplashActivity.this, PrivacyTermsActivity.class));
        finish();
    }

    private void enableEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // ✅ Null-safe: getWindowInsetsController may return null before view is attached
                WindowInsetsControllerCompat controller =
                        ViewCompat.getWindowInsetsController(getWindow().getDecorView());

                if (controller != null) {
                    controller.setAppearanceLightStatusBars(false);
                    controller.setAppearanceLightNavigationBars(false);
                } else {
                    Log.w(TAG, "WindowInsetsController is null — skipping appearance flags");
                }
            }
        } else {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            );
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            view.setPadding(0, statusBarHeight, 0, navigationBarHeight);
            return insets;
        });
    }
}