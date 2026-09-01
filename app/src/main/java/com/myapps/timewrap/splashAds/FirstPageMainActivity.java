package com.myapps.timewrap.splashAds;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.myapps.timewrap.R;
import com.myapps.timewrap.UI.MainActivity;
import com.myapps.timewrap.UI.PermissionAllow;
import com.myapps.timewrap.UI.PremiumActivity;
import com.myapps.timewrap.UI.PremiumManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class FirstPageMainActivity extends AppCompatActivity {

    private AdView adView;
    private FrameLayout adContainerView;
    private InterstitialAd interstitialAd;
    private boolean adIsLoading;
    private boolean isPremium = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableEdgeToEdge();
        setContentView(R.layout.activity_first_page_main);
        applyWindowInsets();

        // ✅ Check if user is premium
        isPremium = PremiumManager.isPremium(this);
        Log.d("FirstPageMain", "User is premium: " + isPremium);

        // ✅ If premium, go directly to MainActivity
        if (isPremium) {
            Log.d("FirstPageMain", "Premium user - going directly to MainActivity");
            startActivity(new Intent(FirstPageMainActivity.this, MainActivity.class));
            finish();
            return;
        }

        adContainerView = findViewById(R.id.ad_view_container);

        // ✅ Only load ads for free users
        if (!isPremium) {
            Log.d("FirstPageMain", "Free user - loading ads");
            loadBanner();
            loadAd();
        } else {
            Log.d("FirstPageMain", "Premium user - hiding ads");
            hideAds();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.primarymain));
        }

        PermissionAllow.GetPermission(this);

        ((LinearLayout) findViewById(R.id.btnstart)).setOnClickListener(view -> {
            // ✅ If premium, go directly to MainActivity
            if (isPremium) {
                Log.d("FirstPageMain", "Premium user - going directly to MainActivity");
                startActivity(new Intent(FirstPageMainActivity.this, MainActivity.class));
                finish();
            } else {
                showInterstitial();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Refresh premium status when returning to activity
        boolean currentPremium = PremiumManager.isPremium(this);
        if (currentPremium != isPremium) {
            isPremium = currentPremium;
            Log.d("FirstPageMain", "Premium status changed to: " + isPremium);

            // ✅ If user became premium, go to MainActivity
            if (isPremium) {
                Log.d("FirstPageMain", "User became premium - redirecting to MainActivity");
                startActivity(new Intent(FirstPageMainActivity.this, MainActivity.class));
                finish();
                return;
            }
            updateAdVisibility();
        }
    }

    private void updateAdVisibility() {
        if (isPremium) {
            hideAds();
        } else {
            // Only load if not already loaded
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

    @Override
    public void onBackPressed() {
        ExitDialog();
    }

    private void ExitDialog() {
        final Dialog dialog = new Dialog(FirstPageMainActivity.this, R.style.DialogTheme);
        dialog.setContentView(R.layout.popup_exit_dialog);
        dialog.setCancelable(false);

        RelativeLayout no = dialog.findViewById(R.id.no);
        RelativeLayout rate = dialog.findViewById(R.id.rate);
        RelativeLayout yes = dialog.findViewById(R.id.yes);

        no.setOnClickListener(v -> dialog.dismiss());

        rate.setOnClickListener(v -> {
            final String rateapp = getPackageName();
            Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + rateapp));
            startActivity(intent1);
        });

        yes.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(getApplicationContext(), AppThankYouActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        dialog.show();
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
        if (adIsLoading || interstitialAd != null || isPremium) {
            return;
        }
        adIsLoading = true;
        InterstitialAd.load(
                this,
                getResources().getString(R.string.interstial),
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        Log.d(TAG, "Ad was loaded.");
                        interstitialAd = ad;
                        adIsLoading = false;

                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        Log.d(TAG, "The ad was dismissed.");
                                        interstitialAd = null;

                                        // ✅ Check premium status before navigating
                                        if (PremiumManager.isPremium(FirstPageMainActivity.this)) {
                                            isPremium = true;
                                            updateAdVisibility();
                                            // ✅ Premium user goes to MainActivity
                                            startActivity(new Intent(FirstPageMainActivity.this, MainActivity.class));
                                            finish();
                                        } else {
                                            // Free user goes to PremiumActivity
                                            Intent intent = new Intent(FirstPageMainActivity.this, PremiumActivity.class);
                                            startActivity(intent);
                                        }
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        Log.d(TAG, "The ad failed to show.");
                                        interstitialAd = null;

                                        // ✅ On ad failure, check premium status
                                        if (PremiumManager.isPremium(FirstPageMainActivity.this)) {
                                            startActivity(new Intent(FirstPageMainActivity.this, MainActivity.class));
                                            finish();
                                        } else {
                                            startActivity(new Intent(FirstPageMainActivity.this, PremiumActivity.class));
                                        }
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

                        // ✅ On ad load failure, check premium status
                        if (PremiumManager.isPremium(FirstPageMainActivity.this)) {
                            startActivity(new Intent(FirstPageMainActivity.this, MainActivity.class));
                            finish();
                        } else {
                            startActivity(new Intent(FirstPageMainActivity.this, MainActivity.class));
                        }
                    }
                });
    }

    private void showInterstitial() {
        if (interstitialAd != null) {
            interstitialAd.show(this);
        } else {
            Log.d(TAG, "The interstitial ad is still loading.");
            // ✅ Check premium status before navigating
            if (PremiumManager.isPremium(this)) {
                startActivity(new Intent(FirstPageMainActivity.this, MainActivity.class));
                finish();
            } else {
                startActivity(new Intent(FirstPageMainActivity.this, PremiumActivity.class));
            }
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