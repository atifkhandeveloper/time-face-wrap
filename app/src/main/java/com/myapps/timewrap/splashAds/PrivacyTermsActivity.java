package com.myapps.timewrap.splashAds;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.myapps.timewrap.R;
import com.myapps.timewrap.UI.PremiumManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class PrivacyTermsActivity extends AppCompatActivity {

    private static final String TAG = "PrivacyTerms";

    Button accept_button;
    CheckBox first_check, second_check;
    Activity activity;
    private AdView adView;
    private FrameLayout adContainerView;
    private boolean isPremium = false;
    private boolean adsEnabled = true;
    private RemoteConfigManager remoteConfigManager;

    // ✅ Firebase Analytics
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔴 GUARANTEED-VISIBLE LOG — confirms this class is running
        Log.e("CHECK", "=== PrivacyTermsActivity onCreate STARTED ===");

        enableEdgeToEdge();
        setContentView(R.layout.activity_privacy_terms);
        activity = PrivacyTermsActivity.this;

        // ✅ Initialize Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Log.e(TAG, "Firebase Analytics initialized");

        // ✅ Bind views first (no config dependency)
        adContainerView = findViewById(R.id.ad_view_container);
        first_check = findViewById(R.id.first_check);
        second_check = findViewById(R.id.second_check);
        accept_button = findViewById(R.id.accept_button);

        accept_button.setOnClickListener(v -> {
            if (!first_check.isChecked() || !second_check.isChecked()) {
                Toast.makeText(getApplicationContext(),
                        "Check above options to continue", Toast.LENGTH_SHORT).show();
                return;
            } else {
                startActivity(new Intent(activity, FirstPageMainActivity.class));
            }
        });

        // ✅ Remote Config
        remoteConfigManager = RemoteConfigManager.getInstance();

        // ✅ Fetch config FIRST, then read values inside callback
        remoteConfigManager.fetchRemoteConfigSync(() -> {

            // 🔴 GUARANTEED-VISIBLE LOG
            Log.e(TAG, "=== Remote Config READY ===");
            Log.e(TAG, "Ads Enabled       = " + remoteConfigManager.isAdsEnabled());
            Log.e(TAG, "Banner Enabled    = " + remoteConfigManager.isBannerEnabled());
            Log.e(TAG, "Interstitial En.  = " + remoteConfigManager.isInterstitialEnabled());
            Log.e(TAG, "Native Enabled    = " + remoteConfigManager.isNativeEnabled());
            Log.e(TAG, "App Open Enabled  = " + remoteConfigManager.isAppOpenEnabled());
            Log.e(TAG, "Premium Enabled   = " + remoteConfigManager.isPremiumEnabled());
            Log.e(TAG, "Banner Ad ID      = [" + remoteConfigManager.getBannerAdId() + "]");
            Log.e(TAG, "Interstitial ID   = [" + remoteConfigManager.getInterstitialAdId() + "]");
            Log.e(TAG, "Native Ad ID      = [" + remoteConfigManager.getNativeAdId() + "]");
            Log.e(TAG, "App Open Ad ID    = [" + remoteConfigManager.getAppOpenAdId() + "]");
            Log.e(TAG, "===========================");

            // ✅ Now safely read values
            isPremium = PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled();
            adsEnabled = remoteConfigManager.isAdsEnabled();

            Log.e(TAG, "User is premium: " + isPremium);
            Log.e(TAG, "Ads enabled: " + adsEnabled);

            // ✅ Decide whether to show banner
            if (isPremium || !adsEnabled || !remoteConfigManager.isBannerEnabled()) {
                Log.e(TAG, "Premium user or ads disabled - hiding ads");
                hideAds();
            } else {
                Log.e(TAG, "Free user - showing ads");
                loadBanner();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            accept_button.setText("Get Started");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (remoteConfigManager == null || !remoteConfigManager.isConfigReady()) {
            return;
        }

        remoteConfigManager.refresh();

        remoteConfigManager.fetchRemoteConfigSync(() -> {

            boolean currentPremium = PremiumManager.isPremium(this)
                    && remoteConfigManager.isPremiumEnabled();
            boolean currentAdsEnabled = remoteConfigManager.isAdsEnabled();
            boolean currentBannerEnabled = remoteConfigManager.isBannerEnabled();

            boolean statusChanged = (currentPremium != isPremium)
                    || (currentAdsEnabled != adsEnabled);

            if (statusChanged) {
                isPremium = currentPremium;
                adsEnabled = currentAdsEnabled;
                Log.e(TAG, "Status changed - Premium: " + isPremium
                        + ", Ads Enabled: " + adsEnabled
                        + ", Banner Enabled: " + currentBannerEnabled);
                updateAdVisibility();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    // ---------------- ADS ----------------

    private void updateAdVisibility() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isBannerEnabled()) {
            hideAds();
            Log.e(TAG, "Ads hidden - Premium: " + isPremium + ", Ads Enabled: " + adsEnabled);
        } else {
            if (adContainerView.getChildCount() == 0) {
                loadBanner();
            }
            adContainerView.setVisibility(View.VISIBLE);
            Log.e(TAG, "Showing ads");
        }
    }

    private void loadBanner() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isBannerEnabled()) {
            Log.e(TAG, "Skipping banner load - conditions not met");
            return;
        }

        final String bannerAdId = remoteConfigManager.getBannerAdId();

        // 🔴 GUARANTEED-VISIBLE LOG — right before ad request
        Log.e(TAG, "Banner Ad ID = [" + bannerAdId + "]");

        if (bannerAdId == null || bannerAdId.isEmpty()) {
            Log.e(TAG, "❌ Banner Ad ID is EMPTY — Remote Config not ready");
            adContainerView.setVisibility(View.GONE);
            return;
        }

        adView = new AdView(this);
        adView.setAdUnitId(bannerAdId);
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360));

        adContainerView.removeAllViews();
        adContainerView.addView(adView);

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.e(TAG, "✅ Banner ad loaded");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                Log.e(TAG, "❌ Banner ad failed: " + adError.getMessage()
                        + " | code=" + adError.getCode()
                        + " | domain=" + adError.getDomain());
            }

            @Override
            public void onAdOpened() {
                Log.e(TAG, "Banner ad opened");
            }

            @Override
            public void onAdClicked() {
                Log.e(TAG, "Banner ad clicked");
                logAdClick("banner", bannerAdId);
            }

            @Override
            public void onAdImpression() {
                Log.e(TAG, "📊 Banner ad impression recorded");
                logAdImpression("banner", bannerAdId);
            }
        });

        adView.setOnPaidEventListener(adValue -> {
            double revenue = adValue.getValueMicros() / 1_000_000.0;
            String currency = adValue.getCurrencyCode();
            Log.e(TAG, "💰 Banner paid event - Revenue: " + revenue + " " + currency);
            sendRevenueToFirebase(revenue, currency, "banner", bannerAdId);
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        adContainerView.setVisibility(View.VISIBLE);
        Log.e(TAG, "✅ Banner ad load initiated");
    }

    private void hideAds() {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
            adContainerView.setVisibility(View.GONE);
            Log.e(TAG, "Banner ads hidden");
        }
    }

    // ---------------- UI HELPERS ----------------

    private void enableEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                WindowInsetsControllerCompat controller =
                        ViewCompat.getWindowInsetsController(getWindow().getDecorView());
                if (controller != null) {
                    controller.setAppearanceLightStatusBars(false);
                    controller.setAppearanceLightNavigationBars(false);
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

    // ================= FIREBASE ANALYTICS METHODS =====================

    private void logAdImpression(String adFormat, String adUnitId) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.AD_PLATFORM, "admob");
            bundle.putString(FirebaseAnalytics.Param.AD_SOURCE, "admob");
            bundle.putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat);
            bundle.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, bundle);
            Log.e(TAG, "📊 Logged ad_impression - Format: " + adFormat);
        } catch (Exception e) {
            Log.e(TAG, "Error logging ad_impression: " + e.getMessage());
        }
    }

    private void logAdClick(String adFormat, String adUnitId) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.AD_PLATFORM, "admob");
            bundle.putString(FirebaseAnalytics.Param.AD_SOURCE, "admob");
            bundle.putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat);
            bundle.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
//            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_CLICK, bundle);
            Log.e(TAG, "📊 Logged ad_click - Format: " + adFormat);
        } catch (Exception e) {
            Log.e(TAG, "Error logging ad_click: " + e.getMessage());
        }
    }

    private void sendRevenueToFirebase(double value, String currency, String adFormat, String adUnitId) {
        try {
            if (value > 0) {
                Bundle bundle = new Bundle();
                bundle.putDouble(FirebaseAnalytics.Param.VALUE, value);
                bundle.putString(FirebaseAnalytics.Param.CURRENCY, currency);
                bundle.putString(FirebaseAnalytics.Param.AD_PLATFORM, "admob");
                bundle.putString(FirebaseAnalytics.Param.AD_SOURCE, "admob");
                bundle.putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat);
                bundle.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, bundle);
                Log.e(TAG, "💰 Revenue sent to Firebase: " + value + " " + currency + " (" + adFormat + ")");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending revenue: " + e.getMessage());
        }
    }
}