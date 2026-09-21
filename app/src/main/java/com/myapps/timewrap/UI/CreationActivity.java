package com.myapps.timewrap.UI;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.myapps.timewrap.R;
import com.myapps.timewrap.Utils.C1197util;
import com.myapps.timewrap.Wrapvideo.OnGalleryClickListener;
import com.myapps.timewrap.Wrapvideo.fragments.Video;
import com.myapps.timewrap.splashAds.RemoteConfigManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class CreationActivity extends AppCompatActivity implements OnGalleryClickListener {

    private static final String LOG_TAG = "CreationActivity";

    CreationAdapter adapter;
    ImageView ivBack;
    RecyclerView rvGallery;
    TextView txtNoRecording;
    TextView txtWaterfallVideo;
    TextView txtWrapImage;
    List<Video> videoList = new ArrayList();
    List<Video> waterfallVideo = new ArrayList();
    List<Video> wrapImageList = new ArrayList();

    private AdView adView;
    private FrameLayout adContainerView;
    private boolean isPremium = false;
    private boolean adsEnabled = true;
    private RemoteConfigManager remoteConfigManager;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // 🔴 GUARANTEED-VISIBLE LOG
        Log.e("CHECK", "=== CreationActivity onCreate STARTED ===");

        enableEdgeToEdge();
        setContentView(R.layout.activity_creation);
        applyWindowInsets();
        PermissionAllow.GetPermission(this);

        // ✅ Bind views first (no config dependency)
        adContainerView = findViewById(R.id.ad_view_container);
        this.rvGallery = findViewById(R.id.rvGallery);
        this.txtWaterfallVideo = findViewById(R.id.txt_waterfall_video);
        this.txtWrapImage = findViewById(R.id.txt_wrap_image);
        this.txtNoRecording = findViewById(R.id.txtNoRecording);
        ImageView imageView = findViewById(R.id.iv_back);
        this.ivBack = imageView;

        // Setup listeners
        imageView.setOnClickListener(view -> onBackPressed());

        this.txtWrapImage.setOnClickListener(view -> {
            txtWrapImage.setBackgroundResource(R.drawable.dark_view_bg);
            txtWrapImage.setTextColor(-1);
            txtWaterfallVideo.setBackgroundResource(0);
            txtWaterfallVideo.setTextColor(ViewCompat.MEASURED_STATE_MASK);
            getWrapImage();
        });

        this.txtWaterfallVideo.setOnClickListener(view -> {
            txtWaterfallVideo.setBackgroundResource(R.drawable.dark_view_bg);
            txtWaterfallVideo.setTextColor(-1);
            txtWrapImage.setBackgroundResource(0);
            txtWrapImage.setTextColor(ViewCompat.MEASURED_STATE_MASK);
            List<Video> waterfallVideos = getWaterfallVideos();
            waterfallVideo = waterfallVideos;
            if (waterfallVideos == null || waterfallVideos.size() <= 0) {
                txtNoRecording.setVisibility(View.VISIBLE);
                rvGallery.setVisibility(View.GONE);
                return;
            }
            rvGallery.setVisibility(View.VISIBLE);
            txtNoRecording.setVisibility(View.GONE);
            CreationAdapter creationAdapter = new CreationAdapter(waterfallVideo, this, 0,
                    getContentResolver(), this, C1197util.waterfallVideo);
            adapter = creationAdapter;
            rvGallery.setAdapter(creationAdapter);
        });

        this.rvGallery.setLayoutManager(new GridLayoutManager(this, 2));
        getWrapImage();

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
            Log.e(LOG_TAG, "===========================");

            // ✅ Now safely read values
            isPremium = PremiumManager.isPremium(this) && remoteConfigManager.isPremiumEnabled();
            adsEnabled = remoteConfigManager.isAdsEnabled();

            Log.e(LOG_TAG, "User is premium: " + isPremium);
            Log.e(LOG_TAG, "Ads enabled: " + adsEnabled);

            // ✅ Only load banner if NOT premium AND ads enabled AND banner enabled
            if (!isPremium && adsEnabled && remoteConfigManager.isBannerEnabled()) {
                Log.e(LOG_TAG, "Free user with ads enabled - loading banner");
                loadBanner();
            } else {
                Log.e(LOG_TAG, "Premium user or ads disabled - hiding banner");
                hideBanner();
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
            boolean currentBannerEnabled = remoteConfigManager.isBannerEnabled();

            boolean statusChanged = (currentPremium != isPremium)
                    || (currentAdsEnabled != adsEnabled);

            if (statusChanged) {
                isPremium = currentPremium;
                adsEnabled = currentAdsEnabled;
                Log.e(LOG_TAG, "Status changed - Premium: " + isPremium
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

    private void updateAdVisibility() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isBannerEnabled()) {
            hideBanner();
            Log.e(LOG_TAG, "Ads hidden - Premium: " + isPremium
                    + ", Ads Enabled: " + adsEnabled);
        } else {
            if (adContainerView.getChildCount() == 0) {
                loadBanner();
            }
            adContainerView.setVisibility(View.VISIBLE);
            Log.e(LOG_TAG, "Showing banner ad");
        }
    }

    private void hideBanner() {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
            adContainerView.setVisibility(View.GONE);
        }
    }

    public void onClick(Video video, String str) {
        if (str.equalsIgnoreCase(C1197util.waterfallVideo)) {
            C1197util.waterVideo = new File(video.getRealPath());
            Intent intent = new Intent(this, WaterfallShareActivity.class);
            intent.putExtra("from", C1197util.MyWork);
            startActivity(intent);
        } else if (str.equalsIgnoreCase(C1197util.wrapImage)) {
            C1197util.wrapImagePath = video.getRealPath();
            Intent intent2 = new Intent(this, WrapImageShareActivity.class);
            intent2.putExtra("from", C1197util.MyWork);
            startActivity(intent2);
        }
    }

    public void share(Uri uri) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("video/*");
        intent.putExtra("android.intent.extra.STREAM", uri);
        intent.addFlags(1);
        startActivity(Intent.createChooser(intent, "Share using"));
    }

    public void getWrapImage() {
        try {
            this.wrapImageList = new ArrayList();
            Log.e("Files", "Path: /storage/emulated/0/DCIM/"
                    + getResources().getString(R.string.app_name)
                    + File.separator + "WarpImage");
            File[] listFiles = new File("/storage/emulated/0/DCIM/"
                    + getResources().getString(R.string.app_name)
                    + File.separator + "WarpImage").listFiles();
            if (listFiles != null) {
                Log.e("Files", "Size: " + listFiles.length);
                for (int i = 0; i < listFiles.length; i++) {
                    Log.e("Files", "FileName:" + listFiles[i].getName());
                    this.wrapImageList.add(new Video(listFiles[i].getName(),
                            listFiles[i].getPath(), listFiles[i].getAbsolutePath()));
                }
                List<Video> list = this.wrapImageList;
                if (list != null && list.size() > 0) {
                    this.rvGallery.setVisibility(View.VISIBLE);
                    this.txtNoRecording.setVisibility(View.GONE);
                    CreationAdapter creationAdapter = new CreationAdapter(
                            this.wrapImageList, this, 0, getContentResolver(),
                            this, C1197util.wrapImage);
                    this.adapter = creationAdapter;
                    this.rvGallery.setAdapter(creationAdapter);
                    return;
                }
            }
            this.txtNoRecording.setVisibility(View.VISIBLE);
            this.rvGallery.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Video> getVids() {
        ArrayList arrayList = new ArrayList();
        File[] listFiles = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM) + File.separator
                + getString(R.string.app_name) + File.separator + "WrapVideo").listFiles();
        if (listFiles != null) {
            for (int i = 0; i < listFiles.length; i++) {
                if (!listFiles[i].isDirectory() && listFiles[i].getName().endsWith(".mp4")) {
                    arrayList.add(new Video(ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, 0),
                            listFiles[i].getName(),
                            listFiles[i].getAbsolutePath(),
                            listFiles[i].getAbsolutePath()));
                }
            }
        }
        return arrayList;
    }

    public List<Video> getWaterfallVideos() {
        ArrayList arrayList = new ArrayList();
        File[] listFiles = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM) + File.separator
                + getString(R.string.app_name) + File.separator + "WaterFallVideos").listFiles();
        if (listFiles != null) {
            for (int i = 0; i < listFiles.length; i++) {
                if (!listFiles[i].isDirectory() && listFiles[i].getName().endsWith(".mp4")) {
                    arrayList.add(new Video(ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, 0),
                            listFiles[i].getName(),
                            listFiles[i].getAbsolutePath(),
                            listFiles[i].getAbsolutePath()));
                }
            }
        }
        return arrayList;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private class GetGalleryData extends AsyncTask<String, Void, String> {
        List<Video> vids;

        public void onPreExecute() {}

        public void onProgressUpdate(Void... voidArr) {}

        private GetGalleryData() {}

        public String doInBackground(String... strArr) {
            this.vids = CreationActivity.this.getVids();
            return null;
        }

        public void onPostExecute(String str) {
            CreationActivity.this.videoList.clear();
            CreationActivity.this.videoList.addAll(this.vids);
            if (CreationActivity.this.videoList.size() == 0) {
                CreationActivity.this.txtNoRecording.setVisibility(View.VISIBLE);
                CreationActivity.this.rvGallery.setVisibility(View.GONE);
                return;
            }
            CreationActivity.this.rvGallery.setVisibility(View.VISIBLE);
            CreationActivity.this.txtNoRecording.setVisibility(View.GONE);
            CreationActivity creationActivity = CreationActivity.this;
            List<Video> list = creationActivity.videoList;
            CreationActivity creationActivity2 = CreationActivity.this;
            creationActivity.adapter = new CreationAdapter(list, creationActivity2, 0,
                    creationActivity2.getContentResolver(),
                    CreationActivity.this, C1197util.wrapVideo);
            CreationActivity.this.rvGallery.setAdapter(CreationActivity.this.adapter);
        }
    }

    private void loadBanner() {
        if (isPremium || !adsEnabled || !remoteConfigManager.isBannerEnabled()) {
            Log.e(LOG_TAG, "Skipping banner - conditions not met");
            return;
        }

        String bannerAdId = remoteConfigManager.getBannerAdId();

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
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        Log.e(LOG_TAG, "✅ Banner ad load initiated");
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
}