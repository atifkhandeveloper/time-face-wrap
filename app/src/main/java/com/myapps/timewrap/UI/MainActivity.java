package com.myapps.timewrap.UI;

import static android.content.ContentValues.TAG;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.interstitial.*;
import com.google.android.gms.ads.nativead.NativeAd;
import com.myapps.timewrap.R;
import com.myapps.timewrap.splashAds.AppThankYouActivity;
import com.myapps.timewrap.splashAds.PrivacyTermsActivity;
import com.myapps.timewrap.splashAds.SplashActivity;

public class MainActivity extends AppCompatActivity {

    ImageView ivMyWork, ivSettings, ivWaterfallVideo, ivWrapImage;
    TemplateView template;

    private InterstitialAd interstitialAd;
    private boolean adIsLoading = false;
    private Intent nextIntent;
    private boolean isPremium = false;

    // Ad Capping Variables
    private static final String PREF_NAME = "AdPrefs";
    private static final String KEY_AD_COUNT = "ad_count";
    private static final String KEY_LAST_RESET_TIME = "last_reset_time";
    private static final int MAX_AD_COUNT = 3; // Show ad every 3 clicks
    private static final long RESET_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours in milliseconds
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableEdgeToEdge();
        setContentView(R.layout.activity_main);
        applyWindowInsets();
        PermissionAllow.GetPermission(this);

        // ✅ Check if user is premium
        isPremium = PremiumManager.isPremium(this);
        Log.d("MainActivity", "User is premium: " + isPremium);

        MobileAds.initialize(this);
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        initView();

        template = findViewById(R.id.my_template);

        // ✅ Only load ads if user is NOT premium
        if (!isPremium) {
            Log.d("MainActivity", "Free user - loading ads");
            template.setVisibility(View.GONE);
            loadAd();
            loadNative();
        } else {
            Log.d("MainActivity", "Premium user - hiding ads");
            template.setVisibility(View.GONE);
            // No ads loaded
        }
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

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Refresh premium status
        boolean currentPremium = PremiumManager.isPremium(this);
        if (currentPremium != isPremium) {
            isPremium = currentPremium;
            Log.d("MainActivity", "Premium status changed to: " + isPremium);
            updateAdVisibility();
        }

        // Only load ad if not premium and ad is null
        if (!isPremium && interstitialAd == null) {
            loadAd();
        }
    }

    private void updateAdVisibility() {
        if (isPremium) {
            // Hide ads
            template.setVisibility(View.GONE);
            if (interstitialAd != null) {
                interstitialAd = null;
            }
            adIsLoading = false;
            Log.d("MainActivity", "Premium user - ads hidden");
        } else {
            // Show ads
            if (template.getVisibility() == View.GONE) {
                loadNative();
            }
            if (interstitialAd == null && !adIsLoading) {
                loadAd();
            }
            Log.d("MainActivity", "Free user - ads shown");
        }
    }

    // ================= AD CAPPING METHODS =====================

    private boolean shouldShowAd() {
        // ✅ Premium users never show ads
        if (isPremium) {
            return false;
        }

        // Reset counter if 24 hours have passed
        long lastResetTime = sharedPreferences.getLong(KEY_LAST_RESET_TIME, 0);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastResetTime > RESET_INTERVAL) {
            // Reset the counter
            sharedPreferences.edit()
                    .putInt(KEY_AD_COUNT, 0)
                    .putLong(KEY_LAST_RESET_TIME, currentTime)
                    .apply();
            return true;
        }

        // Check current ad count
        int adCount = sharedPreferences.getInt(KEY_AD_COUNT, 0);
        return adCount < MAX_AD_COUNT;
    }

    private void incrementAdCount() {
        int adCount = sharedPreferences.getInt(KEY_AD_COUNT, 0);
        sharedPreferences.edit()
                .putInt(KEY_AD_COUNT, adCount + 1)
                .apply();
    }

    // ================= INTERSTITIAL =====================

    private void loadAd() {
        // ✅ Don't load ad if premium
        if (isPremium) {
            return;
        }

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
                                // ✅ Only reload if not premium
                                if (!isPremium) {
                                    loadAd();
                                }
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
        // ✅ Premium users skip ads entirely
        if (isPremium) {
            Log.d("MainActivity", "Premium user - skipping ad");
            openNext();
            return;
        }

        // Check if ad should be shown based on capping
        if (shouldShowAd()) {
            if (interstitialAd != null) {
                interstitialAd.show(this);
                incrementAdCount(); // Increment counter after showing ad
                loadAd(); // Preload next ad
            } else {
                openNext();
                loadAd();
            }
        } else {
            // Don't show ad, directly open the activity
            openNext();
            // Still load ad in background for future use
            if (interstitialAd == null) {
                loadAd();
            }
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
        // ✅ Don't load native ad if premium
        if (isPremium) {
            template.setVisibility(View.GONE);
            return;
        }

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

    @Override
    public void onBackPressed() {
        Intent i = new Intent(MainActivity.this, AppThankYouActivity.class);
        startActivity(i);
        finish();
    }
}