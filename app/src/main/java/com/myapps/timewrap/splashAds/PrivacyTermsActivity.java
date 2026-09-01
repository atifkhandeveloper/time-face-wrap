package com.myapps.timewrap.splashAds;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.myapps.timewrap.R;
import com.myapps.timewrap.UI.PremiumManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class PrivacyTermsActivity extends AppCompatActivity {

    Button accept_button;
    CheckBox first_check, second_check;
    Activity activity;
    private AdView adView;
    private FrameLayout adContainerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableEdgeToEdge();
        setContentView(R.layout.activity_privacy_terms);
        applyWindowInsets();
        activity = PrivacyTermsActivity.this;

        adContainerView = findViewById(R.id.ad_view_container);

        // ✅ Check if user is premium BEFORE loading banner
        if (PremiumManager.isPremium(this)) {
            // Premium user - hide ads
            Log.d("PrivacyTerms", "Premium user - hiding ads");
            hideAds();
        } else {
            // Free user - show ads
            Log.d("PrivacyTerms", "Free user - showing ads");
            loadBanner();
        }

        first_check = findViewById(R.id.first_check);
        second_check = findViewById(R.id.second_check);
        accept_button = findViewById(R.id.accept_button);
        accept_button.setOnClickListener(v -> {
            if (!first_check.isChecked() || !second_check.isChecked()) {
                Toast.makeText(getApplicationContext(), "Check above options to continue", Toast.LENGTH_SHORT).show();
                return;
            } else {
                startActivity(new Intent(activity, FirstPageMainActivity.class));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            accept_button.setText("Get Started");
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    // ---------------- ADS ----------------

    private void loadBanner() {
        // Create a new ad view
        adView = new AdView(this);
        adView.setAdUnitId(getResources().getString(R.string.banner));
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360));

        // Replace ad container with new ad view
        adContainerView.removeAllViews();
        adContainerView.addView(adView);

        // Load ad
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void hideAds() {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
            adContainerView.setVisibility(View.GONE);
        }
    }

    // ---------------- UI HELPERS ----------------

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