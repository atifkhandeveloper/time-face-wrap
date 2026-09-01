package com.myapps.timewrap.UI;

import static android.content.ContentValues.TAG;

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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.myapps.timewrap.R;
import com.myapps.timewrap.Utils.C1197util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

public class WrapImageShareActivity extends AppCompatActivity {
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

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        enableEdgeToEdge();
        setContentView(R.layout.activity_wrapimage_share);
        applyWindowInsets();

        // ✅ Check if user is premium
        isPremium = PremiumManager.isPremium(this);
        Log.d("WrapImageShare", "User is premium: " + isPremium);

        adContainerView = findViewById(R.id.ad_view_container);

        // ✅ Only load ads if user is NOT premium
        if (!isPremium) {
            Log.d("WrapImageShare", "Free user - loading ads");
            loadBanner();
            loadAd();
        } else {
            Log.d("WrapImageShare", "Premium user - hiding ads");
            hideAds();
        }

        this.previewViewImageView = findViewById(R.id.previewView_ImageView);
        this.ivBack = findViewById(R.id.iv_back);
        this.ivSave = findViewById(R.id.iv_save);
        this.ivShare = findViewById(R.id.iv_share);
        this.iv_image = findViewById(R.id.iv_image);

        Bundle extras = getIntent().getExtras();
        if (!(extras == null || extras.getString("from") == null)) {
            this.isFrom = extras.getString("from");
        }

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

        this.iv_image.setOnClickListener(view -> {
            startActivity(new Intent(WrapImageShareActivity.this, CreationActivity.class).addFlags(67108864));
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
                    MediaScannerConnection.scanFile(getApplicationContext(), new String[]{fileURI.getPath()}, new String[]{"image/jpeg"}, null);
                }
            }
            if (isSaved) {
                if (isFrom.equalsIgnoreCase(C1197util.MyWork)) {
                    Context applicationContext = getApplicationContext();
                    uri = FileProvider.getUriForFile(applicationContext, getPackageName() + ".provider", new File(C1197util.wrapImagePath));
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
            // ✅ Premium users skip ads
            if (isPremium) {
                Log.d("WrapImageShare", "Premium user - saving directly without ad");
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Refresh premium status
        boolean currentPremium = PremiumManager.isPremium(this);
        if (currentPremium != isPremium) {
            isPremium = currentPremium;
            Log.d("WrapImageShare", "Premium status changed to: " + isPremium);
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

    // ✅ New method to save image without ad
    private void saveImage() {
        if (!isSaved) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
            fileURI = saveBitmapInGalary(resultBitmap);
            Log.e("TAG", "onCreate: " + fileURI.getPath());
            if (fileURI != null) {
                isSaved = true;
                MediaScannerConnection.scanFile(getApplicationContext(), new String[]{fileURI.getPath()}, new String[]{"image/jpeg"}, null);
            }
            startActivity(new Intent(WrapImageShareActivity.this, CreationActivity.class).addFlags(67108864));
        }
    }

    public Uri saveBitmapInGalary(Bitmap bitmap) {
        String file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
        File file2 = new File(file + "/" + getResources().getString(R.string.app_name) + File.separator + "WarpImage");
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
                        WrapImageShareActivity.this.interstitialAd = interstitialAd;
                        adIsLoading = false;
                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        Log.d(TAG, "The ad was dismissed.");
                                        WrapImageShareActivity.this.interstitialAd = null;
                                        saveImage();
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        Log.d(TAG, "The ad failed to show.");
                                        WrapImageShareActivity.this.interstitialAd = null;
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
                        saveImage();
                    }
                });
    }

    private void showInterstitial() {
        // ✅ Premium users skip ads
        if (isPremium) {
            Log.d("WrapImageShare", "Premium user - skipping interstitial");
            saveImage();
            return;
        }

        if (interstitialAd != null) {
            interstitialAd.show(this);
        } else {
            Log.d(TAG, "The interstitial ad is still loading.");
            saveImage();
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