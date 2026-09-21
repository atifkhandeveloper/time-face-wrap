package com.myapps.timewrap.splashAds;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.myapps.timewrap.R;
import com.myapps.timewrap.UI.PremiumManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class AppThankYouActivity extends AppCompatActivity {

    TemplateView template;
    private boolean isPremium = false;
    private boolean adsEnabled = true;
    private RemoteConfigManager remoteConfigManager;

    // ✅ Track current native ad
    private NativeAd currentNativeAd = null;

    // ✅ Firebase Analytics
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableEdgeToEdge();
        setContentView(R.layout.activity_thankyou_app);
        applyWindowInsets();

        // ✅ Initialize Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Log.d("AppThankYou", "Firebase Analytics initialized");

        // ✅ Initialize Remote Config
        remoteConfigManager = RemoteConfigManager.getInstance();
        remoteConfigManager.logAllConfigsDebug();

        // ✅ Check if user is premium (from Remote Config and local)
        isPremium = PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled();
        Log.d("AppThankYou", "User is premium: " + isPremium);

        // ✅ Check if ads are enabled from Remote Config
        adsEnabled = remoteConfigManager.isAdsEnabled();
        Log.d("AppThankYou", "Ads enabled: " + adsEnabled);

        template = findViewById(R.id.my_template);

        // ✅ Only load native ad if NOT premium AND ads are enabled in remote config
        if (!isPremium && adsEnabled && remoteConfigManager.isNativeEnabled()) {
            Log.d("AppThankYou", "Free user - loading native ad");
            template.setVisibility(View.GONE);
            loadNative();
        } else {
            Log.d("AppThankYou", "Premium user or ads disabled - hiding native ad");
            template.setVisibility(View.GONE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorlight));
        }

        RelativeLayout Exit = findViewById(R.id.exitapp);
        RelativeLayout Rate = findViewById(R.id.rate);

        Exit.setOnClickListener(v -> finishAffinity());

        Rate.setOnClickListener(v -> {
            final String rateapp = getPackageName();
            Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + rateapp));
            startActivity(intent1);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ✅ Refresh remote config and premium status
        remoteConfigManager.refresh();

        // ✅ Update premium and ads status
        boolean currentPremium = PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled();
        boolean currentAdsEnabled = remoteConfigManager.isAdsEnabled();

        // Check if status changed
        boolean statusChanged = (currentPremium != isPremium) || (currentAdsEnabled != adsEnabled);

        if (statusChanged) {
            isPremium = currentPremium;
            adsEnabled = currentAdsEnabled;
            Log.d("AppThankYou", "Status changed - Premium: " + isPremium + ", Ads Enabled: " + adsEnabled);
            updateAdVisibility();
        }
    }

    @Override
    protected void onDestroy() {
        // ✅ SAFE cleanup — DO NOT call template.setNativeAd(null) (crashes the library)
        if (template != null) {
            template.setVisibility(View.GONE);
        }

        // ✅ Destroy the NativeAd object itself
        if (currentNativeAd != null) {
            currentNativeAd.destroy();
            currentNativeAd = null;
        }

        super.onDestroy();
    }

    private void updateAdVisibility() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isNativeEnabled()) {
            template.setVisibility(View.GONE);
            // ✅ Destroy native ad
            if (currentNativeAd != null) {
                currentNativeAd.destroy();
                currentNativeAd = null;
            }
            Log.d("AppThankYou", "Ads hidden - Premium: " + isPremium + ", Ads Enabled: " + adsEnabled);
        } else {
            // Only load if not already loaded and showing
            if (template.getVisibility() == View.GONE && currentNativeAd == null) {
                loadNative();
            }
            Log.d("AppThankYou", "Free user - showing ads");
        }
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

    // ================= NATIVE AD =====================

    private void loadNative() {
        // ✅ Check all conditions before loading
        if (!isInternetAvailable()) {
            Log.d("AppThankYou", "No internet - skipping native ad");
            template.setVisibility(View.GONE);
            return;
        }

        // ✅ Double-check premium status before loading
        if (PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled()) {
            Log.d("AppThankYou", "Premium user - skipping native ad load");
            template.setVisibility(View.GONE);
            return;
        }

        // ✅ Check if ads are enabled
        if (!adsEnabled || !remoteConfigManager.isNativeEnabled()) {
            Log.d("AppThankYou", "Ads disabled - skipping native ad load");
            template.setVisibility(View.GONE);
            return;
        }

        Log.d("AppThankYou", "Loading native ad...");

        // ✅ Get native ad ID from Remote Config
        final String nativeAdId = remoteConfigManager.getNativeAdId();
        Log.d("AppThankYou", "Native Ad ID from Remote Config: " + nativeAdId);

        if (nativeAdId == null || nativeAdId.isEmpty()) {
            Log.e("AppThankYou", "Native Ad ID is null or empty");
            template.setVisibility(View.GONE);
            return;
        }

        AdLoader adLoader = new AdLoader.Builder(this, nativeAdId)
                .forNativeAd(nativeAd -> {
                    // ✅ Destroy previous ad before storing new one
                    if (currentNativeAd != null) {
                        currentNativeAd.destroy();
                    }

                    currentNativeAd = nativeAd;

                    NativeTemplateStyle style = new NativeTemplateStyle.Builder().build();
                    template.setStyles(style);
                    template.setNativeAd(nativeAd);
                    template.setVisibility(View.VISIBLE);
                    Log.d("AppThankYou", "✅ Native ad loaded successfully");

                    // ✅ Log ad_impression event
                    logAdImpression("native", nativeAdId);

                    // ✅ Listen for paid events (real revenue data)
                    nativeAd.setOnPaidEventListener(adValue -> {
                        double revenue = adValue.getValueMicros() / 1_000_000.0;
                        String currency = adValue.getCurrencyCode();
                        Log.d("AppThankYou", "💰 Native paid event - Revenue: " + revenue + " " + currency);
                        sendRevenueToFirebase(revenue, currency, "native", nativeAdId);
                    });
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        template.setVisibility(View.GONE);
                        Log.e("AppThankYou", "❌ Native ad failed to load: " + adError.getMessage());
                    }

                    @Override
                    public void onAdLoaded() {
                        Log.d("AppThankYou", "Native ad loaded (AdListener)");
                    }

                    @Override
                    public void onAdClicked() {
                        Log.d("AppThankYou", "Native ad clicked");
                        logAdClick("native", nativeAdId);
                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo net = cm.getActiveNetworkInfo();
            return net != null && net.isConnected();
        }
        return false;
    }

    // ================= FIREBASE ANALYTICS METHODS =====================

    /**
     * Log ad impression event to Firebase Analytics
     */
    private void logAdImpression(String adFormat, String adUnitId) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.AD_PLATFORM, "admob");
            bundle.putString(FirebaseAnalytics.Param.AD_SOURCE, "admob");
            bundle.putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat);
            bundle.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, bundle);
            Log.d("AppThankYou", "📊 Logged ad_impression event - Format: " + adFormat);
        } catch (Exception e) {
            Log.e("AppThankYou", "Error logging ad_impression: " + e.getMessage());
        }
    }

    /**
     * Log ad click event to Firebase Analytics
     */
    private void logAdClick(String adFormat, String adUnitId) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.AD_PLATFORM, "admob");
            bundle.putString(FirebaseAnalytics.Param.AD_SOURCE, "admob");
            bundle.putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat);
            bundle.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
//            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_CLICK, bundle);
            Log.d("AppThankYou", "📊 Logged ad_click event - Format: " + adFormat);
        } catch (Exception e) {
            Log.e("AppThankYou", "Error logging ad_click: " + e.getMessage());
        }
    }

    /**
     * Send ad revenue to Firebase Analytics
     */
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
                Log.d("AppThankYou", "💰 Revenue sent to Firebase: " + value + " " + currency + " (" + adFormat + ")");
            }
        } catch (Exception e) {
            Log.e("AppThankYou", "Error sending revenue to Firebase: " + e.getMessage());
        }
    }
}