package com.myapps.timewrap.UI;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.nativead.NativeAd;
import com.myapps.timewrap.R;



public class MainActivity extends AppCompatActivity {
    boolean doubleBackToExitPressedOnce = false;
    Dialog extdialog;
    ImageView ivMyWork;
    ImageView ivSettings;
    ImageView ivWaterfallVideo;
    ImageView ivWrapImage;
    TemplateView template;
    ImageView ivWrapVideo;


    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView((int) R.layout.activity_main);


        PermissionAllow.GetPermission(this);
        initView();
        template = findViewById(R.id.my_template);
        template.setVisibility(View.GONE);
        loadNative();
    }

    public void initView() {
        this.ivMyWork = (ImageView) findViewById(R.id.iv_wrap_video);
        this.ivWrapImage = (ImageView) findViewById(R.id.iv_wrap_image);
//        this.ivWrapVideo = (ImageView) findViewById(R.id.iv_wrap_video);
        this.ivWaterfallVideo = (ImageView) findViewById(R.id.iv_waterfall_video);
        this.ivSettings = (ImageView) findViewById(R.id.settings);
        this.ivWrapImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, WrapImageActivity.class);
                startActivity(intent);
            }
        });

        this.ivWaterfallVideo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, WaterFallActivity.class);
                startActivity(intent);
            }
        });
        this.ivMyWork.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CreationActivity.class);
                startActivity(intent);
            }
        });
        this.ivSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

    }

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (i == 100) {
            hasAllPermissionsSatisfied(strArr, iArr);
        }
    }

    private boolean hasAllPermissionsSatisfied(String[] strArr, int[] iArr) {
        boolean z = true;
        for (int i : iArr) {
            z = z && i == 0;
        }
        return z;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void loadNative() {
        // Check internet before loading ad
        if (!isInternetAvailable()) {
            Log.d("Ads", "No internet available. Skipping native ad.");
            template.setVisibility(View.GONE);
            return;
        }

        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading ad...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Timeout after 10 seconds if ad not loaded
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    template.setVisibility(View.GONE);
                    Log.d("Ads", "Ad load timeout after 10 seconds.");
                }
            }
        };
        handler.postDelayed(timeoutRunnable, 10000); // 10 seconds

        // Initialize and load native ad
        MobileAds.initialize(this, initializationStatus -> {
            AdLoader adLoader = new AdLoader.Builder(this, getResources().getString(R.string.native_ad))
                    .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                        @Override
                        public void onNativeAdLoaded(NativeAd nativeAd) {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }

                            NativeTemplateStyle styles = new NativeTemplateStyle.Builder().build();
                            template.setStyles(styles);
                            template.setNativeAd(nativeAd);
                            template.setVisibility(View.VISIBLE);

                            Log.d("Ads", "Native ad loaded successfully.");
                            handler.removeCallbacks(timeoutRunnable);
                        }
                    })
                    .withAdListener(new AdListener() {
                        @Override
                        public void onAdFailedToLoad(LoadAdError adError) {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            template.setVisibility(View.GONE);
                            Log.e("Ads", "Failed to load native ad: " + adError.getMessage());
                            handler.removeCallbacks(timeoutRunnable);
                        }
                    })
                    .build();

            adLoader.loadAd(new AdRequest.Builder().build());
        });
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }



}
