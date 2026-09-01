package com.myapps.timewrap.splashAds;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.myapps.timewrap.R;
import com.myapps.timewrap.UI.PremiumManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class AppThankYouActivity extends AppCompatActivity {

    TemplateView template;
    private boolean isPremium = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableEdgeToEdge();
        setContentView(R.layout.activity_thankyou_app);
        applyWindowInsets();

        // ✅ Check if user is premium
        isPremium = PremiumManager.isPremium(this);
        Log.d("AppThankYou", "User is premium: " + isPremium);

        template = findViewById(R.id.my_template);

        // ✅ Only load native ad if user is NOT premium
        if (!isPremium) {
            Log.d("AppThankYou", "Free user - loading native ad");
            template.setVisibility(View.GONE);
            loadNative();
        } else {
            Log.d("AppThankYou", "Premium user - hiding native ad");
            template.setVisibility(View.GONE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorlight));
        }

        RelativeLayout Exit = findViewById(R.id.exitapp);
        RelativeLayout Rate = findViewById(R.id.rate);

        Exit.setOnClickListener(v -> finishAffinity());

        Rate.setOnClickListener(v -> {
            final String rateapp = getPackageName();
            Intent intent1 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + rateapp));
            startActivity(intent1);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Refresh premium status when returning to activity
        boolean currentPremium = PremiumManager.isPremium(this);
        if (currentPremium != isPremium) {
            isPremium = currentPremium;
            Log.d("AppThankYou", "Premium status changed to: " + isPremium);
            updateAdVisibility();
        }
    }

    private void updateAdVisibility() {
        if (isPremium) {
            template.setVisibility(View.GONE);
            Log.d("AppThankYou", "Premium user - ads hidden");
        } else {
            // Only load if not already loaded
            if (template.getVisibility() == View.GONE) {
                loadNative();
            }
            Log.d("AppThankYou", "Free user - ads shown");
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

    private void loadNative() {
        if (!isInternetAvailable()) {
            Log.d("AppThankYou", "No internet - skipping native ad");
            template.setVisibility(View.GONE);
            return;
        }

        // ✅ Double-check premium status before loading
        if (PremiumManager.isPremium(this)) {
            Log.d("AppThankYou", "Premium user - skipping native ad load");
            template.setVisibility(View.GONE);
            return;
        }

        Log.d("AppThankYou", "Loading native ad...");

        AdLoader adLoader = new AdLoader.Builder(this, getString(R.string.native_ad))
                .forNativeAd(nativeAd -> {
                    NativeTemplateStyle style = new NativeTemplateStyle.Builder().build();
                    template.setStyles(style);
                    template.setNativeAd(nativeAd);
                    template.setVisibility(View.VISIBLE);
                    Log.d("AppThankYou", "✅ Native ad loaded");
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        template.setVisibility(View.GONE);
                        Log.e("AppThankYou", "❌ Native ad failed: " + adError.getMessage());
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
}