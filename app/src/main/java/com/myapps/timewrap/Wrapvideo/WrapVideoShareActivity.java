package com.myapps.timewrap.Wrapvideo;

import static android.content.ContentValues.TAG;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.myapps.timewrap.R;
import com.myapps.timewrap.UI.CreationActivity;
import com.myapps.timewrap.UI.MainActivity;
import com.myapps.timewrap.UI.PremiumManager;
import com.myapps.timewrap.UI.WaterfallShareActivity;
import com.myapps.timewrap.Utils.C1197util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class WrapVideoShareActivity extends AppCompatActivity {
    String isFrom = "";
    boolean isSave = false;
    ImageView ivBack;
    ImageView ivSave;
    ImageView ivShare;
    ImageView iv_image;
    Uri lastRecordedFile = null;
    ImageView previewViewImageView;
    VideoView videoView = null;

    private AdView adView;
    private FrameLayout adContainerView;
    private InterstitialAd interstitialAd;
    private boolean adIsLoading;
    private boolean isPremium = false;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        enableEdgeToEdge();
        setContentView(R.layout.activity_wrap_video_view);
        applyWindowInsets();

        // ✅ Check if user is premium
        isPremium = PremiumManager.isPremium(this);
        Log.d("WrapVideoShare", "User is premium: " + isPremium);

        adContainerView = findViewById(R.id.ad_view_container);

        // ✅ Only load ads if user is NOT premium
        if (!isPremium) {
            Log.d("WrapVideoShare", "Free user - loading ads");
            loadBanner();
            loadAd();
        } else {
            Log.d("WrapVideoShare", "Premium user - hiding ads");
            hideAds();
        }

        this.videoView = findViewById(R.id.vidView);
        this.previewViewImageView = findViewById(R.id.previewView_ImageView);
        this.ivBack = findViewById(R.id.iv_back);
        this.ivSave = findViewById(R.id.iv_save);
        this.ivShare = findViewById(R.id.iv_share);
        this.iv_image = findViewById(R.id.iv_image);

        Bundle extras = getIntent().getExtras();
        if (!(extras == null || extras.getString("from") == null)) {
            this.isFrom = extras.getString("from");
            Log.d("nmnmnmnm", "-----" + this.isFrom);
        }

        MediaController mediaController = new MediaController(this);
        mediaController.setMediaPlayer(this.videoView);
        mediaController.setAnchorView(this.videoView);
        this.videoView.setMediaController(mediaController);
        this.videoView.setVideoPath(C1197util.wrapVideoFile.getAbsolutePath());
        this.videoView.start();

        this.ivBack.setOnClickListener(view -> onBackPressed());

        this.iv_image.setOnClickListener(view -> {
            startActivity(new Intent(WrapVideoShareActivity.this, CreationActivity.class).addFlags(67108864));
            finish();
        });

        this.ivShare.setOnClickListener(view -> ivSHAREit(view));

        this.ivSave.setOnClickListener(view -> {
            // ✅ Premium users skip ads
            if (isPremium) {
                Log.d("WrapVideoShare", "Premium user - saving directly without ad");
                ivSAVEit(null);
            } else {
                showInterstitial();
            }
        });

        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            this.isSave = true;
            this.ivSave.setVisibility(View.GONE);
            this.iv_image.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Refresh premium status
        boolean currentPremium = PremiumManager.isPremium(this);
        if (currentPremium != isPremium) {
            isPremium = currentPremium;
            Log.d("WrapVideoShare", "Premium status changed to: " + isPremium);
            updateAdVisibility();
        }
    }

    private void updateAdVisibility() {
        if (isPremium) {
            hideAds();
        } else {
            if (adContainerView.getChildCount() == 0) {
                loadBanner();
            }
            if (interstitialAd == null && !adIsLoading) {
                loadAd();
            }
            adContainerView.setVisibility(View.VISIBLE);
        }
    }

    private void hideAds() {
        // Hide banner
        if (adContainerView != null) {
            adContainerView.removeAllViews();
            adContainerView.setVisibility(View.GONE);
        }
        // Remove interstitial
        if (interstitialAd != null) {
            interstitialAd = null;
        }
        adIsLoading = false;
    }

    public void ivSHAREit(View view) {
        Uri uri;
        if (!this.isSave) {
            Log.e("TAG", "onCreate wrapVideoFile : " + C1197util.wrapVideoFile);
            addVideoToGalleryOnlyshare(C1197util.wrapVideoFile);
        }
        if (this.isSave) {
            if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
                Log.d("jijijj", "shaRE VIDEO-----" + C1197util.wrapVideoFile);
                Log.d("jijijj", "shaRE uRI-----" + Uri.parse(String.valueOf(C1197util.wrapVideoFile)));
                Context applicationContext = getApplicationContext();
                uri = FileProvider.getUriForFile(applicationContext, getPackageName() + ".provider", C1197util.wrapVideoFile);
            } else {
                uri = this.lastRecordedFile;
            }
            Intent intent = new Intent("android.intent.action.SEND");
            intent.putExtra("android.intent.extra.STREAM", uri);
            intent.setType("video/*");
            intent.setFlags(268435457);
            startActivity(Intent.createChooser(intent, "Share using"));
        }
    }

    public void ivSAVEit(View view) {
        if (!this.isSave) {
            Log.e("TAG", "onCreate wrapVideoFile : " + C1197util.wrapVideoFile);
            addVideoToGallery(C1197util.wrapVideoFile);
        }
    }

    private void addVideoToGalleryOnlyshare(File file) {
        try {
            getContentResolver();
            String absolutePath = file.getAbsolutePath();
            String substring = absolutePath.substring(absolutePath.lastIndexOf("/") + 1);
            new ContentValues();
            File file2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + getString(R.string.app_name) + File.separator + "WrapVideo");
            if (!file2.exists()) {
                file2.mkdir();
                file2.mkdirs();
            }
            File file3 = new File(file2, substring);
            C1197util.wrapVideoFile = file3;
            Log.d("jijijj", "addVideoToGallery-----" + C1197util.wrapVideoFile);
            if (copyFileToOther(file.getAbsolutePath(), file3.getAbsolutePath())) {
                this.isSave = true;
            }
        } catch (Exception e) {
            Log.d("SaveVideo", "e2--" + e.getMessage());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void addVideoToGallery(File file) {
        try {
            getContentResolver();
            String absolutePath = file.getAbsolutePath();
            String substring = absolutePath.substring(absolutePath.lastIndexOf("/") + 1);
            new ContentValues();
            File file2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + getString(R.string.app_name) + File.separator + "WrapVideo");
            if (!file2.exists()) {
                file2.mkdir();
                file2.mkdirs();
            }
            File file3 = new File(file2, substring);
            C1197util.wrapVideoFile = file3;
            Log.d("jijijj", "addVideoToGallery-----" + C1197util.wrapVideoFile);
            if (copyFileToOther(file.getAbsolutePath(), file3.getAbsolutePath())) {
                this.isSave = true;
                Toast.makeText(this, "Save Successfully", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, CreationActivity.class).addFlags(67108864));
            }
        } catch (Exception e) {
            Log.d("SaveVideo", "e2--" + e.getMessage());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static boolean copyFileToOther(String str, String str2) throws Throwable {
        FileChannel fileChannel;
        Log.d("SaveVideo", "copyFileToOther from--" + str);
        Log.d("SaveVideo", "copyFileToOther to--" + str2);
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
            Log.d("SaveVideo", "exce--" + e.getMessage());
            return false;
        }
    }

    public void onBackPressed() {
        finish();
    }

    private void loadBanner() {
        // ✅ Don't load banner if premium
        if (isPremium) {
            return;
        }

        adView = new AdView(this);
        adView.setAdUnitId(getResources().getString(R.string.banner));
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360));

        adContainerView.removeAllViews();
        adContainerView.addView(adView);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    public void loadAd() {
        // ✅ Don't load interstitial if premium
        if (isPremium) {
            return;
        }

        if (adIsLoading || interstitialAd != null) {
            return;
        }
        adIsLoading = true;
        InterstitialAd.load(
                this,
                getResources().getString(R.string.interstial),
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        Log.d(TAG, "Ad was loaded.");
                        WrapVideoShareActivity.this.interstitialAd = interstitialAd;
                        adIsLoading = false;
                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        Log.d(TAG, "The ad was dismissed.");
                                        WrapVideoShareActivity.this.interstitialAd = null;
                                        WrapVideoShareActivity.this.ivSAVEit(null);
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        Log.d(TAG, "The ad failed to show.");
                                        WrapVideoShareActivity.this.ivSAVEit(null);
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        Log.d(TAG, "The ad was shown.");
                                    }

                                    @Override
                                    public void onAdImpression() {
                                        Log.d(TAG, "The ad recorded an impression.");
                                    }

                                    @Override
                                    public void onAdClicked() {
                                        Log.d(TAG, "The ad was clicked.");
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, loadAdError.getMessage());
                        interstitialAd = null;
                        adIsLoading = false;
                        WrapVideoShareActivity.this.ivSAVEit(null);
                    }
                });
    }

    private void showInterstitial() {
        // ✅ Premium users skip ads
        if (isPremium) {
            Log.d("WrapVideoShare", "Premium user - skipping interstitial");
            ivSAVEit(null);
            return;
        }

        if (interstitialAd != null) {
            interstitialAd.show(this);
        } else {
            Log.d(TAG, "The interstitial ad is still loading.");
            WrapVideoShareActivity.this.ivSAVEit(null);
            loadAd();
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
}