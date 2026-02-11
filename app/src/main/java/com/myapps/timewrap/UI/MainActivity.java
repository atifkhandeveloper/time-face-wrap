package com.myapps.timewrap.UI;

import static android.content.ContentValues.TAG;

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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.interstitial.*;
import com.google.android.gms.ads.nativead.NativeAd;
import com.myapps.timewrap.R;

public class MainActivity extends AppCompatActivity {

    ImageView ivMyWork, ivSettings, ivWaterfallVideo, ivWrapImage;
    TemplateView template;

    private InterstitialAd interstitialAd;
    private boolean adIsLoading = false;
    private Intent nextIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionAllow.GetPermission(this);

        MobileAds.initialize(this);

        initView();
        loadAd();

        template = findViewById(R.id.my_template);
        template.setVisibility(View.GONE);
        loadNative();
    }

    private void initView() {

        ivMyWork = findViewById(R.id.iv_wrap_video);
        ivWrapImage = findViewById(R.id.iv_wrap_image);
        ivWaterfallVideo = findViewById(R.id.iv_waterfall_video);
        ivSettings = findViewById(R.id.settings);

        ivWrapImage.setOnClickListener(v -> {
            nextIntent = new Intent(this, WrapImageActivity.class);
            showInterstitial();
        });

        ivWaterfallVideo.setOnClickListener(v -> {
            nextIntent = new Intent(this, WaterFallActivity.class);
            showInterstitial();
        });

        ivMyWork.setOnClickListener(v -> {
            nextIntent = new Intent(this, CreationActivity.class);
            showInterstitial();
        });

        ivSettings.setOnClickListener(v -> {
            nextIntent = new Intent(this, SettingsActivity.class);
            showInterstitial();
        });
    }

    // ================= INTERSTITIAL =====================

    private void loadAd() {

        if (adIsLoading || interstitialAd != null) return;

        adIsLoading = true;

        InterstitialAd.load(
                this,
                getString(R.string.interstial),
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {

                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        adIsLoading = false;

                        ad.setFullScreenContentCallback(new FullScreenContentCallback() {

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                interstitialAd = null;

                                if (nextIntent != null) {
                                    startActivity(nextIntent);
                                    nextIntent = null;
                                }
                                loadAd();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                interstitialAd = null;
                                openNext();
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        interstitialAd = null;
                        adIsLoading = false;
                        openNext();
                    }
                }
        );
    }

    private void showInterstitial() {
        if (interstitialAd != null) {
            interstitialAd.show(this);
        } else {
            openNext();
            loadAd();
        }
    }

    private void openNext() {
        if (nextIntent != null) {
            startActivity(nextIntent);
            nextIntent = null;
        }
    }

    // ================= NATIVE =====================

    private void loadNative() {

        if (!isInternetAvailable()) return;

        AdLoader adLoader = new AdLoader.Builder(this, getString(R.string.native_ad))
                .forNativeAd(nativeAd -> {

                    NativeTemplateStyle style = new NativeTemplateStyle.Builder().build();
                    template.setStyles(style);
                    template.setNativeAd(nativeAd);
                    template.setVisibility(View.VISIBLE);
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        template.setVisibility(View.GONE);
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

    @Override
    protected void onResume() {
        super.onResume();
        if (interstitialAd == null) loadAd();
    }
}
