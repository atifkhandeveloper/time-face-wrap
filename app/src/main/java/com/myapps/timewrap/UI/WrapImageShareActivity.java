package com.myapps.timewrap.UI;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
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
import com.myapps.timewrap.Utils.C1197util;
import com.myapps.timewrap.splashAds.RemoteConfigManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

public class WrapImageShareActivity extends AppCompatActivity {

    private static final String LOG_TAG = "WrapImageShare";

    Uri fileURI = null;
    String isFrom = "";
    boolean isSaved = false;
    ImageView ivBack;
    ImageView ivSave;
    ImageView ivShare;
    ImageView iv_image;
    ImageView previewViewImageView;
    Bitmap resultBitmap;
    private AdView adView;
    private FrameLayout adContainerView;
    private InterstitialAd interstitialAd;
    private boolean adIsLoading;
    private boolean isPremium = false;
    private boolean adsEnabled = true;
    private RemoteConfigManager remoteConfigManager;

    // ✅ Firebase Analytics
    private FirebaseAnalytics mFirebaseAnalytics;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // 🔴 GUARANTEED-VISIBLE LOG
        Log.e("CHECK", "=== WrapImageShareActivity onCreate STARTED ===");

        enableEdgeToEdge();
        setContentView(R.layout.activity_wrapimage_share);
        applyWindowInsets();

        // ✅ Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Log.e(LOG_TAG, "Firebase Analytics initialized");

        // ✅ Bind views first (no config dependency)
        adContainerView = findViewById(R.id.ad_view_container);
        this.previewViewImageView = findViewById(R.id.previewView_ImageView);
        this.ivBack = findViewById(R.id.iv_back);
        this.ivSave = findViewById(R.id.iv_save);
        this.ivShare = findViewById(R.id.iv_share);
        this.iv_image = findViewById(R.id.iv_image);

        // ✅ Read intent extras
        Bundle extras = getIntent().getExtras();
        if (!(extras == null || extras.getString("from") == null)) {
            this.isFrom = extras.getString("from");
        }

        // ✅ Load image (no config dependency)
        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            Glide.with(this)
                    .load(C1197util.wrapImagePath)
                    .placeholder(R.drawable.icon)
                    .error(R.drawable.icon)
                    .into(this.previewViewImageView);
        } else {
            Bitmap bitmap = C1197util.bitmap;
            this.resultBitmap = bitmap;
            this.previewViewImageView.setImageBitmap(bitmap);
        }

        // ✅ Setup click listeners (no config dependency)
        this.iv_image.setOnClickListener(view -> {
            startActivity(new Intent(WrapImageShareActivity.this, CreationActivity.class)
                    .addFlags(67108864));
            finish();
        });

        this.ivShare.setOnClickListener(view -> {
            Uri uri;
            if (!isSaved) {
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
                fileURI = saveBitmapInGalary(resultBitmap);
                Log.e("TAG", "onCreate: " + fileURI.getPath());
                if (fileURI != null) {
                    isSaved = true;
                    MediaScannerConnection.scanFile(getApplicationContext(),
                            new String[]{fileURI.getPath()},
                            new String[]{"image/jpeg"}, null);
                }
            }
            if (isSaved) {
                if (isFrom.equalsIgnoreCase(C1197util.MyWork)) {
                    Context applicationContext = getApplicationContext();
                    uri = FileProvider.getUriForFile(applicationContext,
                            getPackageName() + ".provider",
                            new File(C1197util.wrapImagePath));
                } else {
                    uri = fileURI;
                }
                Intent intent = new Intent("android.intent.action.SEND");
                intent.putExtra("android.intent.extra.STREAM", uri);
                intent.setType("image/jpeg");
                intent.setFlags(268435457);
                startActivity(Intent.createChooser(intent, "Share Image using"));
            }
        });

        this.ivSave.setOnClickListener(view -> {
            if (isPremium || !adsEnabled || !remoteConfigManager.isInterstitialEnabled()) {
                Log.e(LOG_TAG, "Skipping ad - Premium: " + isPremium
                        + ", Ads Enabled: " + adsEnabled);
                saveImage();
            } else {
                showInterstitial();
            }
        });

        this.ivBack.setOnClickListener(view -> onBackPressed());

        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            this.isSaved = true;
            this.ivSave.setVisibility(View.GONE);
            this.iv_image.setVisibility(View.GONE);
        }

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
            Log.e(LOG_TAG, "===========================");

            // ✅ Now safely read values
            isPremium = PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled();
            adsEnabled = remoteConfigManager.isAdsEnabled();

            Log.e(LOG_TAG, "User is premium: " + isPremium);
            Log.e(LOG_TAG, "Ads enabled: " + adsEnabled);

            // ✅ Only load ads if NOT premium AND ads are enabled
            if (!isPremium && adsEnabled) {
                Log.e(LOG_TAG, "Free user with ads enabled - loading ads");
                loadBanner();
                loadInterstitialAd();
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
            if (interstitialAd == null && !adIsLoading
                    && remoteConfigManager.isInterstitialEnabled()) {
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

    // ✅ Save image without ad
    private void saveImage() {
        if (!isSaved) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
            fileURI = saveBitmapInGalary(resultBitmap);
            Log.e("TAG", "onCreate: " + fileURI.getPath());
            if (fileURI != null) {
                isSaved = true;
                MediaScannerConnection.scanFile(getApplicationContext(),
                        new String[]{fileURI.getPath()},
                        new String[]{"image/jpeg"}, null);
            }
            startActivity(new Intent(WrapImageShareActivity.this, CreationActivity.class)
                    .addFlags(67108864));
        }
    }

    public Uri saveBitmapInGalary(Bitmap bitmap) {
        String file = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM).toString();
        File file2 = new File(file + "/" + getResources().getString(R.string.app_name)
                + File.separator + "WarpImage");
        file2.mkdirs();
        int nextInt = new Random().nextInt(10000);
        File file3 = new File(file2, "Image-" + nextInt + ".jpg");
        if (file3.exists()) {
            file3.delete();
        }
        try {
            C1197util.wrapImagePath = file3.getAbsolutePath();
            FileOutputStream fileOutputStream = new FileOutputStream(file3);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "Save Successfully", Toast.LENGTH_SHORT).show();
        return Uri.fromFile(file3);
    }

    public void onBackPressed() {
        finish();
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
        if (isPremium || !adsEnabled || !remoteConfigManager.isInterstitialEnabled()) {
            Log.e(LOG_TAG, "Skipping interstitial - conditions not met");
            return;
        }

        if (adIsLoading || interstitialAd != null) {
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
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        Log.e(LOG_TAG, "✅ Interstitial ad loaded");
                        WrapImageShareActivity.this.interstitialAd = interstitialAd;
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
                                        WrapImageShareActivity.this.interstitialAd = null;
                                        saveImage();
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        Log.e(LOG_TAG, "Interstitial failed to show: "
                                                + adError.getMessage());
                                        WrapImageShareActivity.this.interstitialAd = null;
                                        saveImage();
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
                        saveImage();
                    }
                });
    }

    private void showInterstitial() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isInterstitialEnabled()) {
            Log.e(LOG_TAG, "Skipping interstitial - Premium: " + isPremium
                    + ", Ads Enabled: " + adsEnabled);
            saveImage();
            return;
        }

        if (interstitialAd != null) {
            interstitialAd.show(this);
        } else {
            Log.e(LOG_TAG, "Interstitial still loading — calling saveImage directly");
            saveImage();
            loadInterstitialAd();
        }
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