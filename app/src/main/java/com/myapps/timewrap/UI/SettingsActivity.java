package com.myapps.timewrap.UI;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.myapps.timewrap.R;
import com.myapps.timewrap.Utils.PlayStoreGo;
import com.myapps.timewrap.splashAds.RemoteConfigManager;

public class SettingsActivity extends AppCompatActivity {
    ImageView ivBack;
    RelativeLayout rlPrivacy;
    RelativeLayout rlRateApp;
    RelativeLayout rlShare;
    TextView txtVersion;
    TemplateView template;
    private boolean isPremium = false;
    private boolean adsEnabled = true;
    private RemoteConfigManager remoteConfigManager;
    private NativeAd currentNativeAd = null;
    private FirebaseAnalytics mFirebaseAnalytics;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        enableEdgeToEdge();
        setContentView(R.layout.activity_settings);
        applyWindowInsets();

        // ✅ Initialize Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // ✅ Initialize Remote Config
        remoteConfigManager = RemoteConfigManager.getInstance();
        remoteConfigManager.logAllConfigsDebug();

        // ✅ Check if user is premium
        isPremium = PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled();
        Log.d("SettingsActivity", "User is premium: " + isPremium);

        // ✅ Check if ads are enabled
        adsEnabled = remoteConfigManager.isAdsEnabled();
        Log.d("SettingsActivity", "Ads enabled: " + adsEnabled);

        template = findViewById(R.id.my_template);

        // ✅ Only load native ad if conditions met
        if (!isPremium && adsEnabled && remoteConfigManager.isNativeEnabled()) {
            Log.d("SettingsActivity", "Free user with ads enabled - loading native ad");
            template.setVisibility(View.GONE);
            loadNative();
        } else {
            Log.d("SettingsActivity", "Premium user or ads disabled - hiding native ad");
            template.setVisibility(View.GONE);
        }

        this.ivBack = findViewById(R.id.iv_back);
        this.txtVersion = findViewById(R.id.txt_version);
        this.rlShare = findViewById(R.id.rl_share);
        this.rlRateApp = findViewById(R.id.rl_rateUs);
        this.rlPrivacy = findViewById(R.id.rl_privacy);

        this.rlShare.setOnClickListener(view -> {
            Intent intent = new Intent("android.intent.action.SEND");
            intent.setType("text/plain");
            intent.putExtra("android.intent.extra.SUBJECT", getResources().getString(R.string.app_name));
            intent.putExtra("android.intent.extra.TEXT", "https://play.google.com/store/apps/details?id=" + getPackageName() + System.getProperty("line.separator"));
            startActivity(Intent.createChooser(intent, "Share via"));
        });

        this.rlRateApp.setOnClickListener(view -> PlayStoreGo.onClickRateUs(SettingsActivity.this));

        this.rlPrivacy.setOnClickListener(view -> PlayStoreGo.onClickPrivacy(SettingsActivity.this));

        this.txtVersion.setText("1.0");

        this.ivBack.setOnClickListener(view -> onBackPressed());
    }

    @Override
    protected void onResume() {
        super.onResume();

        remoteConfigManager.refresh();

        boolean currentPremium = PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled();
        boolean currentAdsEnabled = remoteConfigManager.isAdsEnabled();
        boolean currentNativeEnabled = remoteConfigManager.isNativeEnabled();

        boolean statusChanged = (currentPremium != isPremium) || (currentAdsEnabled != adsEnabled);

        if (statusChanged) {
            isPremium = currentPremium;
            adsEnabled = currentAdsEnabled;
            Log.d("SettingsActivity", "Status changed - Premium: " + isPremium +
                    ", Ads Enabled: " + adsEnabled +
                    ", Native Enabled: " + currentNativeEnabled);
            updateAdVisibility();
        }
    }

    @Override
    protected void onDestroy() {
        // ✅ SAFE cleanup — DO NOT call template.setNativeAd(null)
        // The native template library crashes when null is passed.
        if (template != null) {
            template.setVisibility(View.GONE);
        }

        // ✅ Destroy the NativeAd object itself (correct cleanup)
        if (currentNativeAd != null) {
            currentNativeAd.destroy();
            currentNativeAd = null;
        }

        super.onDestroy();
    }

    private void updateAdVisibility() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isNativeEnabled()) {
            // Hide ad
            if (template != null) {
                template.setVisibility(View.GONE);
                // ❌ REMOVED: template.setNativeAd(null) — crashes the library
            }
            // ✅ Destroy native ad
            if (currentNativeAd != null) {
                currentNativeAd.destroy();
                currentNativeAd = null;
            }
            Log.d("SettingsActivity", "Ads hidden - Premium: " + isPremium +
                    ", Ads Enabled: " + adsEnabled);
        } else {
            if (template.getVisibility() == View.GONE && currentNativeAd == null) {
                loadNative();
            }
            Log.d("SettingsActivity", "Free user - showing ads");
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void loadNative() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isNativeEnabled()) {
            if (template != null) {
                template.setVisibility(View.GONE);
            }
            Log.d("SettingsActivity", "Skipping native ad load - conditions not met");
            return;
        }

        if (!isInternetAvailable()) {
            Log.d("SettingsActivity", "No internet available. Skipping native ad.");
            if (template != null) {
                template.setVisibility(View.GONE);
            }
            return;
        }

        try {
            String nativeAdId = remoteConfigManager.getNativeAdId();
            Log.d("SettingsActivity", "Native Ad ID from Remote Config: " + nativeAdId);

            if (nativeAdId == null || nativeAdId.isEmpty()) {
                Log.e("SettingsActivity", "Native Ad ID is null or empty");
                if (template != null) {
                    template.setVisibility(View.GONE);
                }
                return;
            }

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Loading ad...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            Handler handler = new Handler(Looper.getMainLooper());
            Runnable timeoutRunnable = new Runnable() {
                @Override
                public void run() {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                        if (template != null) {
                            template.setVisibility(View.GONE);
                        }
                        Log.d("SettingsActivity", "Ad load timeout after 10 seconds.");
                    }
                }
            };
            handler.postDelayed(timeoutRunnable, 10000);

            MobileAds.initialize(this, initializationStatus -> {
                AdLoader adLoader = new AdLoader.Builder(this, nativeAdId)
                        .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                            @Override
                            public void onNativeAdLoaded(NativeAd nativeAd) {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }

                                // ✅ Destroy previous ad before storing new one
                                if (currentNativeAd != null) {
                                    currentNativeAd.destroy();
                                }

                                currentNativeAd = nativeAd;

                                NativeTemplateStyle styles = new NativeTemplateStyle.Builder().build();
                                template.setStyles(styles);
                                template.setNativeAd(nativeAd);
                                template.setVisibility(View.VISIBLE);

                                Log.d("SettingsActivity", "✅ Native ad loaded successfully.");
                                handler.removeCallbacks(timeoutRunnable);

                                // ✅ Log impression
                                logAdImpression("native", nativeAdId);

                                // ✅ Paid event listener for revenue
                                nativeAd.setOnPaidEventListener(adValue -> {
                                    double revenue = adValue.getValueMicros() / 1_000_000.0;
                                    String currency = adValue.getCurrencyCode();
                                    sendRevenueToFirebase(revenue, currency, "native", nativeAdId);
                                });
                            }
                        })
                        .withAdListener(new AdListener() {
                            @Override
                            public void onAdFailedToLoad(LoadAdError adError) {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                if (template != null) {
                                    template.setVisibility(View.GONE);
                                }
                                Log.e("SettingsActivity", "❌ Failed to load native ad: " + adError.getMessage());
                                handler.removeCallbacks(timeoutRunnable);
                            }

                            @Override
                            public void onAdLoaded() {
                                Log.d("SettingsActivity", "Native ad loaded (AdListener)");
                            }

                            @Override
                            public void onAdClicked() {
                                logAdClick("native", nativeAdId);
                            }
                        })
                        .build();

                adLoader.loadAd(new AdRequest.Builder().build());
            });
        } catch (Exception e) {
            Log.e("SettingsActivity", "Error loading native ad: " + e.getMessage());
            if (template != null) {
                template.setVisibility(View.GONE);
            }
        }
    }

    private boolean isInternetAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnected();
            }
        } catch (Exception e) {
            Log.e("SettingsActivity", "Error checking internet: " + e.getMessage());
        }
        return false;
    }

    private void enableEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ViewCompat.getWindowInsetsController(getWindow().getDecorView())
                        .setAppearanceLightStatusBars(false);
                ViewCompat.getWindowInsetsController(getWindow().getDecorView())
                        .setAppearanceLightNavigationBars(false);
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

    // ================= FIREBASE ANALYTICS =====================

    private void logAdImpression(String adFormat, String adUnitId) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.AD_PLATFORM, "admob");
            bundle.putString(FirebaseAnalytics.Param.AD_SOURCE, "admob");
            bundle.putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat);
            bundle.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, bundle);
            Log.d("SettingsActivity", "📊 Logged ad_impression - " + adFormat);
        } catch (Exception e) {
            Log.e("SettingsActivity", "Error logging ad_impression: " + e.getMessage());
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
            Log.d("SettingsActivity", "📊 Logged ad_click - " + adFormat);
        } catch (Exception e) {
            Log.e("SettingsActivity", "Error logging ad_click: " + e.getMessage());
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
                Log.d("SettingsActivity", "💰 Revenue sent: " + value + " " + currency);
            }
        } catch (Exception e) {
            Log.e("SettingsActivity", "Error sending revenue: " + e.getMessage());
        }
    }
}