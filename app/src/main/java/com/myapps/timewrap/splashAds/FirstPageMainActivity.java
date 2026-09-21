package com.myapps.timewrap.splashAds;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.myapps.timewrap.R;
import com.myapps.timewrap.UI.MainActivity;
import com.myapps.timewrap.UI.PermissionAllow;
import com.myapps.timewrap.UI.PremiumActivity;
import com.myapps.timewrap.UI.PremiumManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class FirstPageMainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "FirstPageMain";

    private AdView adView;
    private FrameLayout adContainerView;
    private InterstitialAd interstitialAd;
    private boolean adIsLoading;
    private boolean isPremium = false;
    private boolean adsEnabled = true;
    private RemoteConfigManager remoteConfigManager;
    private long lastInterstitialShowTime = 0;
    private boolean isFirstInterstitial = true;

    // ✅ Firebase Analytics
    private FirebaseAnalytics mFirebaseAnalytics;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔴 GUARANTEED-VISIBLE LOG
        Log.e("CHECK", "=== FirstPageMainActivity onCreate STARTED ===");

        enableEdgeToEdge();
        setContentView(R.layout.activity_first_page_main);
        applyWindowInsets();

        // ✅ Initialize Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Log.e(LOG_TAG, "Firebase Analytics initialized");

        // ✅ Bind views first (no config dependency)
        adContainerView = findViewById(R.id.ad_view_container);

        // ✅ Status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.primarymain));
        }

        PermissionAllow.GetPermission(this);

        ((LinearLayout) findViewById(R.id.btnstart)).setOnClickListener(view -> {
            if (isPremium) {
                Log.e(LOG_TAG, "Premium user - going directly to MainActivity");
                startActivity(new Intent(FirstPageMainActivity.this, MainActivity.class));
                finish();
            } else {
                showInterstitial();
            }
        });

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
            Log.e(LOG_TAG, "Banner Ad ID      = [" + remoteConfigManager.getBannerAdId() + "]");
            Log.e(LOG_TAG, "Interstitial ID   = [" + remoteConfigManager.getInterstitialAdId() + "]");
            Log.e(LOG_TAG, "Native Ad ID      = [" + remoteConfigManager.getNativeAdId() + "]");
            Log.e(LOG_TAG, "App Open Ad ID    = [" + remoteConfigManager.getAppOpenAdId() + "]");
            Log.e(LOG_TAG, "Ads Show Delay    = " + remoteConfigManager.getAdsShowDelay());
            Log.e(LOG_TAG, "Interstitial Int. = " + remoteConfigManager.getInterstitialInterval());
            Log.e(LOG_TAG, "===========================");

            // ✅ Now safely read values
            isPremium = PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled();
            adsEnabled = remoteConfigManager.isAdsEnabled();

            Log.e(LOG_TAG, "User is premium: " + isPremium);
            Log.e(LOG_TAG, "Ads enabled: " + adsEnabled);

            // ✅ If premium, go directly to MainActivity
            if (isPremium) {
                Log.e(LOG_TAG, "Premium user - going directly to MainActivity");
                startActivity(new Intent(FirstPageMainActivity.this, MainActivity.class));
                finish();
                return;
            }

            // ✅ Only load ads if not premium AND ads are enabled
            if (!isPremium && adsEnabled) {
                Log.e(LOG_TAG, "Free user with ads enabled - loading ads");

                long delayMillis = remoteConfigManager.getAdsShowDelay() * 1000;
                if (delayMillis > 0) {
                    Log.e(LOG_TAG, "Delaying ad load by " + delayMillis + "ms");
                    new Handler().postDelayed(() -> {
                        loadBanner();
                        loadInterstitialAd();
                    }, delayMillis);
                } else {
                    loadBanner();
                    loadInterstitialAd();
                }
            } else {
                Log.e(LOG_TAG, "Premium user or ads disabled - hiding ads");
                hideAds();
            }
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

                if (isPremium) {
                    Log.e(LOG_TAG, "User became premium - redirecting to MainActivity");
                    startActivity(new Intent(FirstPageMainActivity.this, MainActivity.class));
                    finish();
                    return;
                }
                updateAdVisibility();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        if (interstitialAd != null) {
            interstitialAd = null;
        }
        super.onDestroy();
    }

    private void updateAdVisibility() {
        if (isPremium || !adsEnabled) {
            hideAds();
        } else {
            if (adContainerView.getChildCount() == 0 && remoteConfigManager.isBannerEnabled()) {
                loadBanner();
            }
            if (interstitialAd == null && !adIsLoading && remoteConfigManager.isInterstitialEnabled()) {
                loadInterstitialAd();
            }
            adContainerView.setVisibility(View.VISIBLE);
        }
    }

    private void hideAds() {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
            adContainerView.setVisibility(View.GONE);
        }
        if (interstitialAd != null) {
            interstitialAd = null;
        }
        adIsLoading = false;
    }

    @Override
    public void onBackPressed() {
        ExitDialog();
    }

    private void ExitDialog() {
        final Dialog dialog = new Dialog(FirstPageMainActivity.this, R.style.DialogTheme);
        dialog.setContentView(R.layout.popup_exit_dialog);
        dialog.setCancelable(false);

        RelativeLayout no = dialog.findViewById(R.id.no);
        RelativeLayout rate = dialog.findViewById(R.id.rate);
        RelativeLayout yes = dialog.findViewById(R.id.yes);

        no.setOnClickListener(v -> dialog.dismiss());

        rate.setOnClickListener(v -> {
            final String rateapp = getPackageName();
            Intent intent1 = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + rateapp));
            startActivity(intent1);
        });

        yes.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(getApplicationContext(), AppThankYouActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        dialog.show();
    }

    // ================= BANNER AD =====================

    private void loadBanner() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isBannerEnabled()) {
            Log.e(LOG_TAG, "Skipping banner - conditions not met");
            return;
        }

        final String bannerAdId = remoteConfigManager.getBannerAdId();

        // 🔴 GUARANTEED-VISIBLE LOG — right before ad request
        Log.e(LOG_TAG, "Banner Ad ID = [" + bannerAdId + "]");

        if (bannerAdId == null || bannerAdId.isEmpty()) {
            Log.e(LOG_TAG, "❌ Banner Ad ID is EMPTY — skipping load");
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
                Log.e(LOG_TAG, "✅ Banner ad loaded");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                Log.e(LOG_TAG, "❌ Banner ad failed: " + adError.getMessage()
                        + " | code=" + adError.getCode()
                        + " | domain=" + adError.getDomain());
            }

            @Override
            public void onAdOpened() {
                Log.e(LOG_TAG, "Banner ad opened");
            }

            @Override
            public void onAdClicked() {
                Log.e(LOG_TAG, "Banner ad clicked");
                logAdClick("banner", bannerAdId);
            }

            @Override
            public void onAdImpression() {
                Log.e(LOG_TAG, "📊 Banner ad impression recorded");
                logAdImpression("banner", bannerAdId);
            }
        });

        adView.setOnPaidEventListener(adValue -> {
            double revenue = adValue.getValueMicros() / 1_000_000.0;
            String currency = adValue.getCurrencyCode();
            Log.e(LOG_TAG, "💰 Banner paid event - Revenue: " + revenue + " " + currency);
            sendRevenueToFirebase(revenue, currency, "banner", bannerAdId);
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        Log.e(LOG_TAG, "✅ Banner ad load initiated");
    }

    // ================= INTERSTITIAL AD =====================

    public void loadInterstitialAd() {
        if (adIsLoading || interstitialAd != null || isPremium
                || !adsEnabled || !remoteConfigManager.isInterstitialEnabled()) {
            Log.e(LOG_TAG, "Skipping interstitial load - conditions not met");
            return;
        }

        long interval = remoteConfigManager.getInterstitialInterval() * 1000;
        long timeSinceLast = System.currentTimeMillis() - lastInterstitialShowTime;
        if (!isFirstInterstitial && timeSinceLast < interval) {
            Log.e(LOG_TAG, "Interstitial interval not met. Waiting: "
                    + (interval - timeSinceLast) + "ms");
            return;
        }

        adIsLoading = true;

        final String interstitialAdId = remoteConfigManager.getInterstitialAdId();

        // 🔴 GUARANTEED-VISIBLE LOG — right before ad request
        Log.e(LOG_TAG, "Interstitial Ad ID = [" + interstitialAdId + "]");

        if (interstitialAdId == null || interstitialAdId.isEmpty()) {
            Log.e(LOG_TAG, "❌ Interstitial Ad ID is EMPTY — skipping load");
            adIsLoading = false;
            return;
        }

        InterstitialAd.load(
                this,
                interstitialAdId,
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        Log.e(LOG_TAG, "✅ Interstitial ad loaded");
                        interstitialAd = ad;
                        adIsLoading = false;

                        interstitialAd.setOnPaidEventListener(adValue -> {
                            double revenue = adValue.getValueMicros() / 1_000_000.0;
                            String currency = adValue.getCurrencyCode();
                            Log.e(LOG_TAG, "💰 Interstitial paid event - Revenue: "
                                    + revenue + " " + currency);
                            sendRevenueToFirebase(revenue, currency,
                                    "interstitial", interstitialAdId);
                        });

                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        Log.e(LOG_TAG, "Interstitial dismissed");
                                        interstitialAd = null;
                                        lastInterstitialShowTime = System.currentTimeMillis();
                                        isFirstInterstitial = false;

                                        if (PremiumManager.isPremium(FirstPageMainActivity.this)
                                                && remoteConfigManager.isPremiumEnabled()) {
                                            isPremium = true;
                                            updateAdVisibility();
                                            startActivity(new Intent(FirstPageMainActivity.this,
                                                    MainActivity.class));
                                            finish();
                                        } else {
                                            startActivity(new Intent(FirstPageMainActivity.this,
                                                    PremiumActivity.class));
                                        }
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        Log.e(LOG_TAG, "Interstitial failed to show: "
                                                + adError.getMessage());
                                        interstitialAd = null;

                                        if (PremiumManager.isPremium(FirstPageMainActivity.this)
                                                && remoteConfigManager.isPremiumEnabled()) {
                                            startActivity(new Intent(FirstPageMainActivity.this,
                                                    MainActivity.class));
                                            finish();
                                        } else {
                                            startActivity(new Intent(FirstPageMainActivity.this,
                                                    PremiumActivity.class));
                                        }
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        Log.e(LOG_TAG, "📊 Interstitial shown");
                                        logAdImpression("interstitial", interstitialAdId);
                                    }

                                    @Override
                                    public void onAdImpression() {
                                        Log.e(LOG_TAG, "Interstitial impression recorded");
                                    }

                                    @Override
                                    public void onAdClicked() {
                                        Log.e(LOG_TAG, "Interstitial clicked");
                                        logAdClick("interstitial", interstitialAdId);
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(LOG_TAG, "❌ Interstitial failed: " + loadAdError.getMessage()
                                + " | code=" + loadAdError.getCode()
                                + " | domain=" + loadAdError.getDomain());
                        interstitialAd = null;
                        adIsLoading = false;

                        if (PremiumManager.isPremium(FirstPageMainActivity.this)
                                && remoteConfigManager.isPremiumEnabled()) {
                            startActivity(new Intent(FirstPageMainActivity.this,
                                    MainActivity.class));
                            finish();
                        } else {
                            startActivity(new Intent(FirstPageMainActivity.this,
                                    MainActivity.class));
                        }
                    }
                });
    }

    private void showInterstitial() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isInterstitialEnabled()) {
            Log.e(LOG_TAG, "Skipping interstitial - Premium: " + isPremium
                    + ", Ads Enabled: " + adsEnabled);
            if (PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled()) {
                startActivity(new Intent(FirstPageMainActivity.this, MainActivity.class));
                finish();
            } else {
                startActivity(new Intent(FirstPageMainActivity.this, PremiumActivity.class));
            }
            return;
        }

        if (interstitialAd != null) {
            interstitialAd.show(this);
        } else {
            Log.e(LOG_TAG, "Interstitial still loading — navigating directly");
            if (PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled()) {
                startActivity(new Intent(FirstPageMainActivity.this, MainActivity.class));
                finish();
            } else {
                startActivity(new Intent(FirstPageMainActivity.this, PremiumActivity.class));
            }
            loadInterstitialAd();
        }
    }

    private void enableEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                androidx.core.view.WindowInsetsControllerCompat controller =
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

    private void sendRevenueToFirebase(double value, String currency,
                                       String adFormat, String adUnitId) {
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
                Log.e(LOG_TAG, "💰 Revenue sent to Firebase: " + value + " "
                        + currency + " (" + adFormat + ")");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error sending revenue: " + e.getMessage());
        }
    }
}