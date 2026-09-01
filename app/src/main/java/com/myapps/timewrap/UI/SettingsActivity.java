package com.myapps.timewrap.UI;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

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
    private boolean isPremium = false;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        enableEdgeToEdge();
        setContentView(R.layout.activity_settings);
        applyWindowInsets();

        // ✅ Check if user is premium
        isPremium = PremiumManager.isPremium(this);
        Log.d("SettingsActivity", "User is premium: " + isPremium);

        template = findViewById(R.id.my_template);

        // ✅ Only load native ad if user is NOT premium
        if (!isPremium) {
            Log.d("SettingsActivity", "Free user - loading native ad");
            template.setVisibility(View.GONE);
            loadNative();
        } else {
            Log.d("SettingsActivity", "Premium user - hiding native ad");
            template.setVisibility(View.GONE);
        }

        this.ivBack = findViewById(R.id.iv_back);
        this.txtVersion = findViewById(R.id.txt_version);
        this.rlShare = findViewById(R.id.rl_share);
        this.rlRateApp = findViewById(R.id.rl_rateUs);
        this.rlPrivacy = findViewById(R.id.rl_privacy);

        this.rlShare.setOnClickListener(view -> {
            Intent intent = new Intent("android.intent.action.SEND");
            intent.setType("text/plain");
            intent.putExtra("android.intent.extra.SUBJECT", getResources().getString(R.string.app_name));
            intent.putExtra("android.intent.extra.TEXT", "https://play.google.com/store/apps/details?id=" + getPackageName() + System.getProperty("line.separator"));
            startActivity(Intent.createChooser(intent, "Share via"));
        });

        this.rlRateApp.setOnClickListener(view -> PlayStoreGo.onClickRateUs(SettingsActivity.this));

        this.rlPrivacy.setOnClickListener(view -> PlayStoreGo.onClickPrivacy(SettingsActivity.this));

        this.txtVersion.setText("1.0");

        this.ivBack.setOnClickListener(view -> onBackPressed());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Refresh premium status when returning to activity
        boolean currentPremium = PremiumManager.isPremium(this);
        if (currentPremium != isPremium) {
            isPremium = currentPremium;
            Log.d("SettingsActivity", "Premium status changed to: " + isPremium);
            updateAdVisibility();
        }
    }

    private void updateAdVisibility() {
        if (isPremium) {
            // Hide ad
            template.setVisibility(View.GONE);
            Log.d("SettingsActivity", "Premium user - ads hidden");
        } else {
            // Show ad - only load if not already loaded
            if (template.getVisibility() == View.GONE) {
                loadNative();
            }
            Log.d("SettingsActivity", "Free user - ads shown");
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void loadNative() {
        // ✅ Don't load ad if premium
        if (isPremium) {
            template.setVisibility(View.GONE);
            return;
        }

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

    private void enableEdgeToEdge() {
        // For Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

            // Optional: Make status bar and navigation bar transparent
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

            // Set light/dark status bar icons based on your theme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ViewCompat.getWindowInsetsController(getWindow().getDecorView())
                        .setAppearanceLightStatusBars(false); // false for light status bar, true for dark
                ViewCompat.getWindowInsetsController(getWindow().getDecorView())
                        .setAppearanceLightNavigationBars(false);
            }
        } else {
            // For older Android versions
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            );
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        }
    }

    /**
     * Apply window insets to handle system bars
     */
    private void applyWindowInsets() {
        // For the root view of your layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
            // Get insets for system bars
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            // Apply padding to your root layout to avoid overlapping with system bars
            // If you want your content to go under system bars, remove this
            view.setPadding(0, statusBarHeight, 0, navigationBarHeight);

            return insets;
        });
    }
}