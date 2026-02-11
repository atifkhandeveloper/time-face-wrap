package com.myapps.timewrap.UI;

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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
import com.myapps.timewrap.Utils.PlayStoreGo;

public class SettingsActivity extends AppCompatActivity {
    ImageView ivBack;
    RelativeLayout rlPrivacy;
    RelativeLayout rlRateApp;
    RelativeLayout rlShare;
    TextView txtVersion;
    TemplateView template;


    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView((int) R.layout.activity_settings);

        template = findViewById(R.id.my_template);
        template.setVisibility(View.GONE);
        loadNative();




        this.ivBack = (ImageView) findViewById(R.id.iv_back);
        this.txtVersion = (TextView) findViewById(R.id.txt_version);
        this.rlShare = (RelativeLayout) findViewById(R.id.rl_share);
        this.rlRateApp = (RelativeLayout) findViewById(R.id.rl_rateUs);
        this.rlPrivacy = (RelativeLayout) findViewById(R.id.rl_privacy);
        this.rlShare.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent("android.intent.action.SEND");
                intent.setType("text/plain");
                intent.putExtra("android.intent.extra.SUBJECT", SettingsActivity.this.getResources().getString(R.string.app_name));
                intent.putExtra("android.intent.extra.TEXT", "https://play.google.com/store/apps/details?id=" + SettingsActivity.this.getPackageName() + System.getProperty("line.separator"));
                SettingsActivity.this.startActivity(Intent.createChooser(intent, "Share via"));
            }
        });
        this.rlRateApp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                PlayStoreGo.onClickRateUs(SettingsActivity.this);
            }
        });
        this.rlPrivacy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                PlayStoreGo.onClickPrivacy(SettingsActivity.this);
            }
        });
        this.txtVersion.setText("1.0");
        this.ivBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SettingsActivity.this.onBackPressed();
            }
        });
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
