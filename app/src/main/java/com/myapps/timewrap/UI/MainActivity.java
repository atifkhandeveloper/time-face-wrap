package com.myapps.timewrap.UI;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.facebook.ads.Ad;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.InterstitialAdListener;
import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.interstitial.*;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewException;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.myapps.timewrap.R;
import com.myapps.timewrap.splashAds.AppThankYouActivity;
import com.myapps.timewrap.splashAds.PrivacyTermsActivity;
import com.myapps.timewrap.splashAds.RemoteConfigManager;
import com.myapps.timewrap.splashAds.SplashActivity;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";

    ImageView ivMyWork, ivSettings, ivWaterfallVideo, ivWrapImage;
    TemplateView template;

    private InterstitialAd interstitialAd;
    private boolean adIsLoading = false;
    private Intent nextIntent;
    private boolean isPremium = false;
    private boolean adsEnabled = true;
    private RemoteConfigManager remoteConfigManager;

    // Ad Capping Variables
    private static final String PREF_NAME = "AdPrefs";
    private static final String KEY_AD_COUNT = "ad_count";
    private static final String KEY_LAST_RESET_TIME = "last_reset_time";
    private SharedPreferences sharedPreferences;

    // ✅ Meta (Facebook) Interstitial Ad
    private com.facebook.ads.InterstitialAd metaInterstitialAd;
    private boolean isMetaAdShowing = false;
    private String META_PLACEMENT_ID = "1573747854530400_1573749944530191";
    private String metaBannerAdId = "1573747854530400_1573749931196859";

    // Default values (will be overridden by Remote Config)
    private int maxAdCount = 3;
    private long resetInterval = 24 * 60 * 60 * 1000; // 24 hours

    // ✅ Firebase Analytics instance
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔴 GUARANTEED-VISIBLE LOG
        Log.e("CHECK", "=== MainActivity onCreate STARTED ===");

        enableEdgeToEdge();
        setContentView(R.layout.activity_main);
        applyWindowInsets();
        PermissionAllow.GetPermission(this);
        rateusdialog();

        // ✅ Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Log.e(LOG_TAG, "Firebase Analytics initialized");

        // ✅ SharedPreferences + view binding first (no config dependency)
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        initView();
        template = findViewById(R.id.my_template);

        // ✅ Initialize Google Mobile Ads
        MobileAds.initialize(this);

        // ✅ Remote Config — fetch FIRST, then read values
        remoteConfigManager = RemoteConfigManager.getInstance();

        remoteConfigManager.fetchRemoteConfigSync(() -> {

            // 🔴 GUARANTEED-VISIBLE LOG
            Log.e(LOG_TAG, "=== Remote Config READY ===");
            Log.e(LOG_TAG, "Ads Enabled       = " + remoteConfigManager.isAdsEnabled());
            Log.e(LOG_TAG, "Banner Enabled    = " + remoteConfigManager.isBannerEnabled());
            Log.e(LOG_TAG, "Interstitial En.  = " + remoteConfigManager.isInterstitialEnabled());
            Log.e(LOG_TAG, "Native Enabled    = " + remoteConfigManager.isNativeEnabled());
            Log.e(LOG_TAG, "App Open Enabled  = " + remoteConfigManager.isAppOpenEnabled());
            Log.e(LOG_TAG, "Premium Enabled   = " + remoteConfigManager.isPremiumEnabled());
            Log.e(LOG_TAG, "Interstitial ID   = [" + remoteConfigManager.getInterstitialAdId() + "]");
            Log.e(LOG_TAG, "Native Ad ID      = [" + remoteConfigManager.getNativeAdId() + "]");
            Log.e(LOG_TAG, "Banner Ad ID      = [" + remoteConfigManager.getBannerAdId() + "]");
            Log.e(LOG_TAG, "App Open Ad ID    = [" + remoteConfigManager.getAppOpenAdId() + "]");
            Log.e(LOG_TAG, "===========================");

            // ✅ Now safely read values
            isPremium = PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled();
            adsEnabled = remoteConfigManager.isAdsEnabled();
            maxAdCount = (int) remoteConfigManager.getMaxAdCount();
            resetInterval = remoteConfigManager.getAdResetInterval();

            Log.e(LOG_TAG, "User is premium: " + isPremium);
            Log.e(LOG_TAG, "Ads enabled: " + adsEnabled);
            Log.e(LOG_TAG, "Max Ad Count: " + maxAdCount
                    + ", Reset Interval: " + resetInterval);

            // ✅ Load Meta ad (its own network — always safe to attempt)
            loadmetaad();

            // ✅ Only load ads if NOT premium AND ads enabled
            if (!isPremium && adsEnabled) {
                Log.e(LOG_TAG, "Free user with ads enabled - loading ads");
                template.setVisibility(View.GONE);
                loadGoogleInterstitialAd();
                loadNativeAd();
            } else {
                Log.e(LOG_TAG, "Premium user or ads disabled - hiding ads");
                template.setVisibility(View.GONE);
            }
        });
    }

    private void initView() {
        ivMyWork = findViewById(R.id.iv_wrap_video);
        ivWrapImage = findViewById(R.id.iv_wrap_image);
        ivWaterfallVideo = findViewById(R.id.iv_waterfall_video);
        ivSettings = findViewById(R.id.settings);

        ivWrapImage.setOnClickListener(v -> {
            nextIntent = new Intent(this, WrapImageActivity.class);
            showGoogleInterstitial();
        });

        ivWaterfallVideo.setOnClickListener(v -> {
            nextIntent = new Intent(this, WaterFallActivity.class);
            showGoogleInterstitial();
        });

        ivMyWork.setOnClickListener(v -> {
            nextIntent = new Intent(this, CreationActivity.class);
            showGoogleInterstitial();
        });

        ivSettings.setOnClickListener(v -> {
            showmetaad();
        });
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

            boolean statusChanged = (currentPremium != isPremium)
                    || (currentAdsEnabled != adsEnabled);

            if (statusChanged) {
                isPremium = currentPremium;
                adsEnabled = currentAdsEnabled;
                Log.e(LOG_TAG, "Status changed - Premium: " + isPremium
                        + ", Ads Enabled: " + adsEnabled);
                updateAdVisibility();
            }

            if (!isPremium && adsEnabled && interstitialAd == null) {
                loadGoogleInterstitialAd();
            }

            maxAdCount = (int) remoteConfigManager.getMaxAdCount();
            resetInterval = remoteConfigManager.getAdResetInterval();
        });
    }

    @Override
    protected void onDestroy() {
        if (template != null) {
            // template.setNativeAd(null);
        }
        if (interstitialAd != null) {
            interstitialAd = null;
        }
        if (metaInterstitialAd != null) {
            metaInterstitialAd.destroy();
            metaInterstitialAd = null;
        }
        super.onDestroy();
    }

    private void updateAdVisibility() {
        if (isPremium || !adsEnabled) {
            template.setVisibility(View.GONE);
            if (interstitialAd != null) {
                interstitialAd = null;
            }
            if (metaInterstitialAd != null) {
                metaInterstitialAd.destroy();
                metaInterstitialAd = null;
            }
            adIsLoading = false;
            Log.e(LOG_TAG, "Ads hidden - Premium: " + isPremium
                    + ", Ads Enabled: " + adsEnabled);
        } else {
            if (template.getVisibility() == View.GONE && remoteConfigManager.isNativeEnabled()) {
                loadNativeAd();
            }
            if (interstitialAd == null && !adIsLoading && remoteConfigManager.isInterstitialEnabled()) {
                loadGoogleInterstitialAd();
            }
            Log.e(LOG_TAG, "Free user - showing ads");
        }
    }

    // ================= AD CAPPING METHODS =====================

    private boolean shouldShowAd() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isInterstitialEnabled()) {
            return false;
        }

        long lastResetTime = sharedPreferences.getLong(KEY_LAST_RESET_TIME, 0);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastResetTime > resetInterval) {
            sharedPreferences.edit()
                    .putInt(KEY_AD_COUNT, 0)
                    .putLong(KEY_LAST_RESET_TIME, currentTime)
                    .apply();
            return true;
        }

        int adCount = sharedPreferences.getInt(KEY_AD_COUNT, 0);
        Log.e(LOG_TAG, "Ad count: " + adCount + "/" + maxAdCount);
        return adCount < maxAdCount;
    }

    private void incrementAdCount() {
        int adCount = sharedPreferences.getInt(KEY_AD_COUNT, 0);
        sharedPreferences.edit()
                .putInt(KEY_AD_COUNT, adCount + 1)
                .apply();
        Log.e(LOG_TAG, "Ad count incremented to: " + (adCount + 1));
    }

    // ================= GOOGLE INTERSTITIAL =====================

    private void loadGoogleInterstitialAd() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isInterstitialEnabled()) {
            Log.e(LOG_TAG, "Skipping Google interstitial - conditions not met");
            return;
        }

        if (adIsLoading || interstitialAd != null) return;

        adIsLoading = true;

        String interstitialAdId = remoteConfigManager.getInterstitialAdId();

        // 🔴 GUARANTEED-VISIBLE LOG — right before ad request
        Log.e(LOG_TAG, "Interstitial Ad ID = [" + interstitialAdId + "]");

        if (interstitialAdId == null || interstitialAdId.isEmpty()) {
            Log.e(LOG_TAG, "❌ Interstitial Ad ID is EMPTY — skipping load");
            adIsLoading = false;
            return;
        }

        try {
            InterstitialAd.load(
                    this,
                    interstitialAdId,
                    new AdRequest.Builder().build(),
                    new InterstitialAdLoadCallback() {

                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd ad) {
                            interstitialAd = ad;
                            adIsLoading = false;
                            Log.e(LOG_TAG, "✅ Google Interstitial ad loaded");

                            ad.setOnPaidEventListener(adValue -> {
                                double revenue = adValue.getValueMicros() / 1_000_000.0;
                                String currency = adValue.getCurrencyCode();
                                Log.e(LOG_TAG, "💰 Paid event - Revenue: " + revenue + " " + currency);
                                sendRevenueToFirebase(revenue, currency, "interstitial");
                            });

                            ad.setFullScreenContentCallback(new FullScreenContentCallback() {

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    Log.e(LOG_TAG, "Google Interstitial ad shown");
                                    logAdImpression("interstitial", interstitialAdId);
                                }

                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    interstitialAd = null;
                                    Log.e(LOG_TAG, "Google Interstitial ad dismissed");

                                    if (nextIntent != null) {
                                        startActivity(nextIntent);
                                        nextIntent = null;
                                    }
                                    if (!isPremium && adsEnabled
                                            && remoteConfigManager.isInterstitialEnabled()) {
                                        loadGoogleInterstitialAd();
                                    }
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(AdError adError) {
                                    interstitialAd = null;
                                    Log.e(LOG_TAG, "Google Interstitial failed to show: "
                                            + adError.getMessage());
                                    openNext();
                                }
                            });
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError error) {
                            interstitialAd = null;
                            adIsLoading = false;
                            Log.e(LOG_TAG, "❌ Google Interstitial failed: " + error.getMessage()
                                    + " | code=" + error.getCode()
                                    + " | domain=" + error.getDomain());
                            openNext();
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error loading interstitial ad: " + e.getMessage());
            adIsLoading = false;
            interstitialAd = null;
        }
    }

    private void showGoogleInterstitial() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isInterstitialEnabled()) {
            Log.e(LOG_TAG, "Skipping Google ad - Premium: " + isPremium
                    + ", Ads Enabled: " + adsEnabled);
            openNext();
            return;
        }

        if (shouldShowAd()) {
            if (interstitialAd != null) {
                Log.e(LOG_TAG, "Showing Google interstitial ad");
                try {
                    interstitialAd.show(this);
                    incrementAdCount();
                    loadGoogleInterstitialAd();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error showing ad: " + e.getMessage());
                    openNext();
                }
            } else {
                Log.e(LOG_TAG, "Google Interstitial ad not ready - opening next");
                openNext();
                loadGoogleInterstitialAd();
            }
        } else {
            Log.e(LOG_TAG, "Ad capping limit reached - skipping Google ad");
            openNext();
            if (interstitialAd == null && !adIsLoading) {
                loadGoogleInterstitialAd();
            }
        }
    }

    private void openNext() {
        if (nextIntent != null) {
            startActivity(nextIntent);
            nextIntent = null;
        }
    }

    // ================= NATIVE AD =====================

    private void loadNativeAd() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isNativeEnabled()) {
            template.setVisibility(View.GONE);
            return;
        }

        if (!isInternetAvailable()) {
            Log.e(LOG_TAG, "No internet - skipping native ad");
            template.setVisibility(View.GONE);
            return;
        }

        try {
            String nativeAdId = remoteConfigManager.getNativeAdId();

            // 🔴 GUARANTEED-VISIBLE LOG
            Log.e(LOG_TAG, "Native Ad ID = [" + nativeAdId + "]");

            if (nativeAdId == null || nativeAdId.isEmpty()) {
                Log.e(LOG_TAG, "❌ Native Ad ID is EMPTY — skipping load");
                template.setVisibility(View.GONE);
                return;
            }

            AdLoader adLoader = new AdLoader.Builder(this, nativeAdId)
                    .forNativeAd(nativeAd -> {
                        NativeTemplateStyle style = new NativeTemplateStyle.Builder().build();
                        template.setStyles(style);
                        template.setNativeAd(nativeAd);
                        template.setVisibility(View.VISIBLE);
                        Log.e(LOG_TAG, "✅ Native ad loaded successfully");

                        logAdImpression("native", nativeAdId);

                        nativeAd.setOnPaidEventListener(adValue -> {
                            double revenue = adValue.getValueMicros() / 1_000_000.0;
                            String currency = adValue.getCurrencyCode();
                            Log.e(LOG_TAG, "💰 Native paid event - Revenue: "
                                    + revenue + " " + currency);
                            sendRevenueToFirebase(revenue, currency, "native");
                        });
                    })
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(LoadAdError adError) {
                            template.setVisibility(View.GONE);
                            Log.e(LOG_TAG, "❌ Native ad failed: " + adError.getMessage()
                                    + " | code=" + adError.getCode()
                                    + " | domain=" + adError.getDomain());
                        }

                        @Override
                        public void onAdLoaded() {
                            Log.e(LOG_TAG, "Native ad loaded (AdListener)");
                        }

                        @Override
                        public void onAdClicked() {
                            Log.e(LOG_TAG, "Native ad clicked");
                            logAdClick("native", nativeAdId);
                        }
                    })
                    .build();

            adLoader.loadAd(new AdRequest.Builder().build());
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error loading native ad: " + e.getMessage());
            template.setVisibility(View.GONE);
        }
    }

    private boolean isInternetAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo net = cm.getActiveNetworkInfo();
                return net != null && net.isConnected();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error checking internet: " + e.getMessage());
        }
        return false;
    }

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

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content),
                (view, insets) -> {
                    int statusBarHeight = insets.getInsets(
                            WindowInsetsCompat.Type.statusBars()).top;
                    int navigationBarHeight = insets.getInsets(
                            WindowInsetsCompat.Type.navigationBars()).bottom;
                    view.setPadding(0, statusBarHeight, 0, navigationBarHeight);
                    return insets;
                });
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(MainActivity.this, AppThankYouActivity.class);
        startActivity(i);
        finish();
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
            Log.e(LOG_TAG, "📊 Logged ad_impression - Format: " + adFormat);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error logging ad_impression: " + e.getMessage());
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
            Log.e(LOG_TAG, "📊 Logged ad_click - Format: " + adFormat);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error logging ad_click: " + e.getMessage());
        }
    }

    public void sendRevenueToFirebase(double value, String currency, String adFormat) {
        try {
            String adUnitId = remoteConfigManager.getInterstitialAdId();

            Bundle impressionBundle = new Bundle();
            impressionBundle.putString(FirebaseAnalytics.Param.AD_PLATFORM, "admob");
            impressionBundle.putString(FirebaseAnalytics.Param.AD_SOURCE, "admob");
            impressionBundle.putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat);
            impressionBundle.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, impressionBundle);

            if (value > 0) {
                Bundle revenueBundle = new Bundle();
                revenueBundle.putDouble(FirebaseAnalytics.Param.VALUE, value);
                revenueBundle.putString(FirebaseAnalytics.Param.CURRENCY, currency);
                revenueBundle.putString(FirebaseAnalytics.Param.AD_PLATFORM, "admob");
                revenueBundle.putString(FirebaseAnalytics.Param.AD_SOURCE, "admob");
                revenueBundle.putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat);
                revenueBundle.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, revenueBundle);
                Log.e(LOG_TAG, "💰 Sent revenue to Firebase: " + value + " "
                        + currency + " (" + adFormat + ")");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error sending revenue to Firebase: " + e.getMessage());
        }
    }

    public void loadmetaad() {
        metaInterstitialAd = new com.facebook.ads.InterstitialAd(this, META_PLACEMENT_ID);

        InterstitialAdListener metaInterstitialListener = new InterstitialAdListener() {
            @Override
            public void onInterstitialDisplayed(Ad ad) {
                Log.e("MetaAd", "Interstitial displayed.");
            }

            @Override
            public void onInterstitialDismissed(Ad ad) {
                Log.e("MetaAd", "Interstitial dismissed.");
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
            }

            @Override
            public void onError(Ad ad, com.facebook.ads.AdError adError) {
                Log.e("MetaAd", "Meta ad error: " + adError.getErrorMessage());
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
            }

            @Override
            public void onAdLoaded(Ad ad) {
                Log.e("MetaAd", "Interstitial loaded and ready.");
            }

            @Override
            public void onAdClicked(Ad ad) {
                Log.e("MetaAd", "Interstitial clicked.");
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                Log.e("MetaAd", "Impression logged.");
            }
        };

        metaInterstitialAd.loadAd(
                metaInterstitialAd.buildLoadAdConfig()
                        .withAdListener(metaInterstitialListener)
                        .build()
        );
    }

    public void showmetaad() {
        if (metaInterstitialAd != null && metaInterstitialAd.isAdLoaded()) {
            metaInterstitialAd.show();
        } else {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }
    }

    public void rateusdialog() {
        ReviewManager manager = ReviewManagerFactory.create(this);

        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(this, reviewInfo);
                flow.addOnCompleteListener(flowTask -> {
                    // Flow finished
                });
            } else {
                int reviewErrorCode = ((ReviewException) task.getException()).getErrorCode();
            }
        });
    }
}