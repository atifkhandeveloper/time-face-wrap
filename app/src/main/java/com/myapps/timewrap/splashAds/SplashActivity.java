package com.myapps.timewrap.splashAds;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

import com.myapps.timewrap.R;
import com.myapps.timewrap.ads.MyApplication;
import com.myapps.timewrap.UI.PremiumManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableEdgeToEdge();
        setContentView(R.layout.activity_splash);
        applyWindowInsets();

        new Handler().postDelayed(() -> {
            // ✅ Check if user is premium
            if (PremiumManager.isPremium(this)) {
                Log.d("Splash", "Premium user - skipping ads");
                goToNextActivity();
            } else {
                Log.d("Splash", "Free user - showing ads");
                MyApplication app = (MyApplication) getApplication();
                app.showAdAfterSplash(SplashActivity.this, this::goToNextActivity);
            }
        }, SPLASH_DELAY);
    }

    private void goToNextActivity() {
        startActivity(new Intent(SplashActivity.this, PrivacyTermsActivity.class));
        finish();
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