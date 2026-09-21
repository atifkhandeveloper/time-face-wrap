package com.myapps.timewrap.UI;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.FileChannel;

public class WaterfallShareActivity extends AppCompatActivity {

    private static final String LOG_TAG = "WaterfallShare";

    private static String MEDIA_FOLDER = (Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM) + File.separator + "TIME WARP WATERFALL" + File.separator);
    Uri fileURI = null;
    String isFrom = "";
    boolean isSave = false;
    ImageView ivBack;
    ImageView ivSave;
    ImageView ivShare;
    ImageView iv_image;
    ImageView previewViewImageView;
    VideoView videoView = null;
    private AdView adView;
    private FrameLayout adContainerView;
    private InterstitialAd interstitialAd;
    private boolean adIsLoading;
    private boolean isPremium = false;
    private boolean adsEnabled = true;
    private RemoteConfigManager remoteConfigManager;

    // ✅ Firebase Analytics
    private FirebaseAnalytics mFirebaseAnalytics;

    public static void moveFile(File file, File file2) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        FileOutputStream fileOutputStream = new FileOutputStream(file2);
        byte[] bArr = new byte[1024];
        while (true) {
            int read = fileInputStream.read(bArr);
            if (read <= 0) {
                break;
            }
            fileOutputStream.write(bArr, 0, read);
        }
        fileInputStream.close();
        fileOutputStream.close();
        if (!file.exists()) {
            return;
        }
        if (file.delete()) {
            Log.e("WaterfallShare", "file Deleted :" + file.getPath());
            return;
        }
        Log.e("WaterfallShare", "file not Deleted :" + file.getPath());
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // 🔴 GUARANTEED-VISIBLE LOG
        Log.e("CHECK", "=== WaterfallShareActivity onCreate STARTED ===");

        enableEdgeToEdge();
        setContentView(R.layout.activity_waterfall_share);
        applyWindowInsets();

        // ✅ Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Log.e(LOG_TAG, "Firebase Analytics initialized");

        // ✅ Bind views first (no config dependency)
        adContainerView = findViewById(R.id.ad_view_container);
        this.videoView = findViewById(R.id.videoView);
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

        // ✅ CRASH FIX: Guard against null bitmapToVideoEncoder
        if (!this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            if (WaterFallActivity.bitmapToVideoEncoder == null) {
                Log.e(LOG_TAG, "bitmapToVideoEncoder is null — cannot play video. Finishing.");
                Toast.makeText(this, "Video not available. Please try again.",
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            File outputFile = WaterFallActivity.bitmapToVideoEncoder.getOutputFile();
            if (outputFile == null || !outputFile.exists()) {
                Log.e(LOG_TAG, "Output file is missing — finishing.");
                Toast.makeText(this, "Video file not found. Please try again.",
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        // ✅ Setup video playback
        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            MediaController mediaController = new MediaController(this);
            mediaController.setMediaPlayer(this.videoView);
            mediaController.setAnchorView(this.videoView);
            this.videoView.setMediaController(mediaController);
            this.videoView.setVideoPath(C1197util.waterVideo.getAbsolutePath());
            this.videoView.start();
        } else {
            MediaController mediaController2 = new MediaController(this);
            mediaController2.setMediaPlayer(this.videoView);
            mediaController2.setAnchorView(this.videoView);
            this.videoView.setMediaController(mediaController2);
            this.videoView.setVideoPath(
                    WaterFallActivity.bitmapToVideoEncoder.getOutputFile().getPath());
            this.videoView.start();
        }

        // ✅ Setup click listeners (no config dependency)
        this.ivBack.setOnClickListener(view -> onBackPressed());

        this.iv_image.setOnClickListener(view -> {
            startActivity(new Intent(WaterfallShareActivity.this, CreationActivity.class)
                    .addFlags(67108864));
            finish();
        });

        this.ivShare.setOnClickListener(view -> ivSHAREfall(view));

        this.ivSave.setOnClickListener(view -> {
            // isPremium / adsEnabled will be populated after config fetch
            if (isPremium || !adsEnabled || !remoteConfigManager.isInterstitialEnabled()) {
                Log.e(LOG_TAG, "Skipping ad - Premium: " + isPremium
                        + ", Ads Enabled: " + adsEnabled);
                ivSAVEfall(null);
            } else {
                showInterstitial();
            }
        });

        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            this.isSave = true;
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

    public void ivSHAREfall(View view) {
        Uri uri;
        boolean z = this.isSave;
        if (!z) {
            if (!z) {
                addVideoToGalleryOnlyShare(WaterFallActivity.bitmapToVideoEncoder.getOutputFile());
            } else {
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
                File outputFile = WaterFallActivity.bitmapToVideoEncoder.getOutputFile();
                File file = new File(MEDIA_FOLDER + "water_fall_"
                        + System.currentTimeMillis() + ".mp4");
                if (Build.VERSION.SDK_INT <= 28) {
                    try {
                        moveFile(outputFile, file);
                        this.fileURI = Uri.fromFile(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    this.fileURI = saveVideoMediaStore(outputFile);
                }
                if (this.fileURI != null) {
                    this.isSave = true;
                    MediaScannerConnection.scanFile(getApplicationContext(),
                            new String[]{this.fileURI.getPath()},
                            new String[]{"video/mp4"}, null);
                }
            }
        }
        if (this.isSave) {
            if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
                Context applicationContext = getApplicationContext();
                uri = FileProvider.getUriForFile(applicationContext,
                        getPackageName() + ".provider", C1197util.waterVideo);
            } else {
                uri = this.fileURI;
            }
            Intent intent = new Intent("android.intent.action.SEND");
            intent.putExtra("android.intent.extra.STREAM", uri);
            intent.setType("video/*");
            intent.setFlags(268435457);
            startActivity(Intent.createChooser(intent, "Share using"));
        }
    }

    public void ivSAVEfall(View view) {
        if (!this.isSave) {
            addVideoToGallery(WaterFallActivity.bitmapToVideoEncoder.getOutputFile());
            return;
        }
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
        File outputFile = WaterFallActivity.bitmapToVideoEncoder.getOutputFile();
        File file = new File(MEDIA_FOLDER + "water_fall_" + System.currentTimeMillis() + ".mp4");
        if (Build.VERSION.SDK_INT <= 28) {
            try {
                moveFile(outputFile, file);
                this.fileURI = Uri.fromFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            this.fileURI = saveVideoMediaStore(outputFile);
        }
        if (this.fileURI != null) {
            this.isSave = true;
            MediaScannerConnection.scanFile(getApplicationContext(),
                    new String[]{this.fileURI.getPath()},
                    new String[]{"video/mp4"}, null);
        }
    }

    public static boolean copyFileToOther(String str, String str2) throws Throwable {
        FileChannel fileChannel;
        Log.e("SaveVideo", "copyFileToOther from--" + str);
        Log.e("SaveVideo", "copyFileToOther to--" + str2);
        File file = new File(str);
        File file2 = new File(str2);
        try {
            if (!file2.getParentFile().exists()) {
                file2.getParentFile().mkdirs();
            }
            if (!file2.exists()) {
                file2.createNewFile();
            }
            FileChannel fileChannel2 = null;
            try {
                FileChannel channel = new FileInputStream(file).getChannel();
                try {
                    fileChannel2 = new FileOutputStream(file2).getChannel();
                    fileChannel2.transferFrom(channel, 0, channel.size());
                    if (channel != null) {
                        channel.close();
                    }
                    if (fileChannel2 == null) {
                        return true;
                    }
                    fileChannel2.close();
                    return true;
                } catch (Throwable th) {
                    th = th;
                    FileChannel fileChannel3 = channel;
                    fileChannel = fileChannel2;
                    fileChannel2 = fileChannel3;
                    if (fileChannel2 != null) {
                        fileChannel2.close();
                    }
                    if (fileChannel != null) {
                        fileChannel.close();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                fileChannel = null;
                if (fileChannel2 != null) {
                }
                if (fileChannel != null) {
                }
                throw th2;
            }
        } catch (Exception e) {
            Log.e("SaveVideo", "exce--" + e.getMessage());
            return false;
        }
    }

    private void addVideoToGalleryOnlyShare(File file) {
        try {
            getContentResolver();
            String absolutePath = file.getAbsolutePath();
            String substring = absolutePath.substring(absolutePath.lastIndexOf("/") + 1);
            File file2 = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM) + File.separator
                    + getString(R.string.app_name) + File.separator + "WaterFallVideos");
            if (!file2.exists()) {
                file2.mkdir();
                file2.mkdirs();
            }
            File file3 = new File(file2, substring);
            C1197util.waterVideo = file3;
            if (copyFileToOther(file.getAbsolutePath(), file3.getAbsolutePath())) {
                this.isSave = true;
            }
        } catch (Exception unused) {
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void addVideoToGallery(File file) {
        try {
            getContentResolver();
            String absolutePath = file.getAbsolutePath();
            String substring = absolutePath.substring(absolutePath.lastIndexOf("/") + 1);
            File file2 = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM) + File.separator
                    + getString(R.string.app_name) + File.separator + "WaterFallVideos");
            if (!file2.exists()) {
                file2.mkdir();
                file2.mkdirs();
            }
            File file3 = new File(file2, substring);
            C1197util.waterVideo = file3;
            if (copyFileToOther(file.getAbsolutePath(), file3.getAbsolutePath())) {
                this.isSave = true;
                Toast.makeText(this, "Save Successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, CreationActivity.class).addFlags(67108864));
            }
        } catch (Exception unused) {
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private Uri saveVideoMediaStore(File file) {
        try {
            ContentResolver contentResolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put("title", file.getName());
            contentValues.put("_display_name",
                    "water_fall_" + System.currentTimeMillis() + ".mp4");
            contentValues.put("mime_type", "video/mp4");
            contentValues.put("relative_path",
                    Environment.DIRECTORY_DCIM + File.separator + "Camera");
            Uri insert = contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                OutputStream openOutputStream = getContentResolver().openOutputStream(insert);
                byte[] bArr = new byte[4096];
                while (true) {
                    int read = fileInputStream.read(bArr);
                    if (read == -1) {
                        break;
                    }
                    openOutputStream.write(bArr, 0, read);
                }
                openOutputStream.flush();
                fileInputStream.close();
                openOutputStream.close();
            } catch (Exception e) {
                Log.e("TAG", "exception while writing video: ", e);
            }
            return insert;
        } catch (Exception unused) {
            return null;
        }
    }

    public void playVideo() {
        this.videoView.setVideoPath(
                WaterFallActivity.bitmapToVideoEncoder.getOutputFile().getPath());
        this.videoView.start();
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
                        WaterfallShareActivity.this.interstitialAd = interstitialAd;
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
                                        WaterfallShareActivity.this.interstitialAd = null;
                                        WaterfallShareActivity.this.ivSAVEfall(null);
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        Log.e(LOG_TAG, "Interstitial failed to show: "
                                                + adError.getMessage());
                                        WaterfallShareActivity.this.interstitialAd = null;
                                        WaterfallShareActivity.this.ivSAVEfall(null);
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
                        WaterfallShareActivity.this.ivSAVEfall(null);
                    }
                });
    }

    private void showInterstitial() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isInterstitialEnabled()) {
            Log.e(LOG_TAG, "Skipping interstitial - Premium: " + isPremium
                    + ", Ads Enabled: " + adsEnabled);
            ivSAVEfall(null);
            return;
        }

        if (interstitialAd != null) {
            interstitialAd.show(this);
        } else {
            Log.e(LOG_TAG, "Interstitial still loading — calling ivSAVEfall directly");
            WaterfallShareActivity.this.ivSAVEfall(null);
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