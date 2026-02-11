package com.myapps.timewrap.UI;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.FileProvider;
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

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView((int) R.layout.activity_wrapimage_share);


        adContainerView = findViewById(R.id.ad_view_container);

        loadBanner();
        loadAd();


        this.previewViewImageView = (ImageView) findViewById(R.id.previewView_ImageView);
        this.ivBack = (ImageView) findViewById(R.id.iv_back);
        this.ivSave = (ImageView) findViewById(R.id.iv_save);
        this.ivShare = (ImageView) findViewById(R.id.iv_share);
        this.iv_image = (ImageView) findViewById(R.id.iv_image);
        Bundle extras = getIntent().getExtras();
        if (!(extras == null || extras.getString("from") == null)) {
            this.isFrom = extras.getString("from");
        }
        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            ((RequestBuilder) ((RequestBuilder) Glide.with((FragmentActivity) this).load(C1197util.wrapImagePath).placeholder((int) R.drawable.icon)).error((int) R.drawable.icon)).into(this.previewViewImageView);
        } else {
            Bitmap bitmap = C1197util.bitmap;
            this.resultBitmap = bitmap;
            this.previewViewImageView.setImageBitmap(bitmap);
        }
        this.iv_image.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                WrapImageShareActivity.this.startActivity(new Intent(WrapImageShareActivity.this, CreationActivity.class).addFlags(67108864));
                WrapImageShareActivity.this.finish();
            }
        });
        this.ivShare.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Uri uri;
                if (!WrapImageShareActivity.this.isSaved) {
                    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
                    WrapImageShareActivity wrapImageShareActivity = WrapImageShareActivity.this;
                    wrapImageShareActivity.fileURI = wrapImageShareActivity.saveBitmapInGalary(wrapImageShareActivity.resultBitmap);
                    Log.e("TAG", "onCreate: " + WrapImageShareActivity.this.fileURI.getPath());
                    if (WrapImageShareActivity.this.fileURI != null) {
                        WrapImageShareActivity.this.isSaved = true;
                        MediaScannerConnection.scanFile(WrapImageShareActivity.this.getApplicationContext(), new String[]{WrapImageShareActivity.this.fileURI.getPath()}, new String[]{"image/jpeg"}, (MediaScannerConnection.OnScanCompletedListener) null);
                    }
                }
                if (WrapImageShareActivity.this.isSaved) {
                    if (WrapImageShareActivity.this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
                        Context applicationContext = WrapImageShareActivity.this.getApplicationContext();
                        uri = FileProvider.getUriForFile(applicationContext, WrapImageShareActivity.this.getPackageName() + ".provider", new File(C1197util.wrapImagePath));
                    } else {
                        uri = WrapImageShareActivity.this.fileURI;
                    }
                    Intent intent = new Intent("android.intent.action.SEND");
                    intent.putExtra("android.intent.extra.STREAM", uri);
                    intent.setType("image/jpeg");
                    intent.setFlags(268435457);
                    WrapImageShareActivity.this.startActivity(Intent.createChooser(intent, "Share Image using"));
                }
            }
        });
        this.ivSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showInterstitial();
            }
        });
        this.ivBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                WrapImageShareActivity.this.onBackPressed();
            }
        });
        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            this.isSaved = true;
            this.ivSave.setVisibility(8);
            this.iv_image.setVisibility(8);
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
        Toast.makeText(this, "Save Successfully", 0).show();
        return Uri.fromFile(file3);
    }

    public void onBackPressed() {
        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            finish();
            return;
        }
        finish();
    }

    private void loadBanner() {
        // [START create_ad_view]
        // Create a new ad view.
        adView = new AdView(this);
        adView.setAdUnitId(getResources().getString(R.string.banner));
        // [START set_ad_size]
        // Request an anchored adaptive banner with a width of 360.
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360));
        // [END set_ad_size]

        // Replace ad container with new ad view.
        adContainerView.removeAllViews();
        adContainerView.addView(adView);
        // [END create_ad_view]

        // [START load_ad]
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        // [END load_ad]
    }

    public void loadAd() {
        // Request a new ad if one isn't already loaded.
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
                                        // Called when fullscreen content is dismissed.
                                        Log.d(TAG, "The ad was dismissed.");
                                        // Make sure to set your reference to null so you don't
                                        // show it a second time.
                                        WrapImageShareActivity.this.interstitialAd = null;
                                        if (!WrapImageShareActivity.this.isSaved) {
                                            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
                                            WrapImageShareActivity wrapImageShareActivity = WrapImageShareActivity.this;
                                            wrapImageShareActivity.fileURI = wrapImageShareActivity.saveBitmapInGalary(wrapImageShareActivity.resultBitmap);
                                            Log.e("TAG", "onCreate: " + WrapImageShareActivity.this.fileURI.getPath());
                                            if (WrapImageShareActivity.this.fileURI != null) {
                                                WrapImageShareActivity.this.isSaved = true;
                                                MediaScannerConnection.scanFile(WrapImageShareActivity.this.getApplicationContext(), new String[]{WrapImageShareActivity.this.fileURI.getPath()}, new String[]{"image/jpeg"}, (MediaScannerConnection.OnScanCompletedListener) null);
                                            }
                                            WrapImageShareActivity.this.startActivity(new Intent(WrapImageShareActivity.this, CreationActivity.class).addFlags(67108864));
                                        }
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        // Called when fullscreen content failed to show.
                                        Log.d(TAG, "The ad failed to show.");
                                        // Make sure to set your reference to null so you don't
                                        // show it a second time.
                                        WrapImageShareActivity.this.interstitialAd = null;
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        // Called when fullscreen content is shown.
                                        Log.d(TAG, "The ad was shown.");
                                    }

                                    @Override
                                    public void onAdImpression() {
                                        // Called when an impression is recorded for an ad.
                                        Log.d(TAG, "The ad recorded an impression.");
                                    }

                                    @Override
                                    public void onAdClicked() {
                                        // Called when ad is clicked.
                                        Log.d(TAG, "The ad was clicked.");
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, loadAdError.getMessage());
                        interstitialAd = null;
                        adIsLoading = false;

                        if (!WrapImageShareActivity.this.isSaved) {
                            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
                            WrapImageShareActivity wrapImageShareActivity = WrapImageShareActivity.this;
                            wrapImageShareActivity.fileURI = wrapImageShareActivity.saveBitmapInGalary(wrapImageShareActivity.resultBitmap);
                            Log.e("TAG", "onCreate: " + WrapImageShareActivity.this.fileURI.getPath());
                            if (WrapImageShareActivity.this.fileURI != null) {
                                WrapImageShareActivity.this.isSaved = true;
                                MediaScannerConnection.scanFile(WrapImageShareActivity.this.getApplicationContext(), new String[]{WrapImageShareActivity.this.fileURI.getPath()}, new String[]{"image/jpeg"}, (MediaScannerConnection.OnScanCompletedListener) null);
                            }
                            WrapImageShareActivity.this.startActivity(new Intent(WrapImageShareActivity.this, CreationActivity.class).addFlags(67108864));
                        }
                    }
                });
    }


    private void showInterstitial() {
        // Show the ad if it's ready. Otherwise restart the game.
        // [START show_ad]
        if (interstitialAd != null) {
            interstitialAd.show(this);
        } else {
            Log.d(TAG, "The interstitial ad is still loading.");
            // [START_EXCLUDE silent]
            if (!WrapImageShareActivity.this.isSaved) {
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
                WrapImageShareActivity wrapImageShareActivity = WrapImageShareActivity.this;
                wrapImageShareActivity.fileURI = wrapImageShareActivity.saveBitmapInGalary(wrapImageShareActivity.resultBitmap);
                Log.e("TAG", "onCreate: " + WrapImageShareActivity.this.fileURI.getPath());
                if (WrapImageShareActivity.this.fileURI != null) {
                    WrapImageShareActivity.this.isSaved = true;
                    MediaScannerConnection.scanFile(WrapImageShareActivity.this.getApplicationContext(), new String[]{WrapImageShareActivity.this.fileURI.getPath()}, new String[]{"image/jpeg"}, (MediaScannerConnection.OnScanCompletedListener) null);
                }
                WrapImageShareActivity.this.startActivity(new Intent(WrapImageShareActivity.this, CreationActivity.class).addFlags(67108864));
            }
            loadAd();

            // [END_EXCLUDE]
        }
        // [END show_ad]
    }
}
