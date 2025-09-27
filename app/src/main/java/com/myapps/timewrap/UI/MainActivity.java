package com.myapps.timewrap.UI;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.myapps.timewrap.R;
import com.myapps.timewrap.splashAds.FirstPageMainActivity;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;


public class MainActivity extends AppCompatActivity {
    boolean doubleBackToExitPressedOnce = false;
    Dialog extdialog;
    ImageView ivMyWork;
    ImageView ivSettings;
    ImageView ivWaterfallVideo;
    ImageView ivWrapImage;
    ImageView ivWrapVideo;
    private static final String INTERSTITIAL_ID = "Interstitial_Android";


    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView((int) R.layout.activity_main);


        PermissionAllow.GetPermission(this);






        initView();
        loadAd();

    }


    public void initView() {

        this.ivMyWork = (ImageView) findViewById(R.id.iv_my_work);
        this.ivWrapImage = (ImageView) findViewById(R.id.iv_wrap_image);
        this.ivWrapVideo = (ImageView) findViewById(R.id.iv_wrap_video);
        this.ivWaterfallVideo = (ImageView) findViewById(R.id.iv_waterfall_video);
        this.ivSettings = (ImageView) findViewById(R.id.iv_settings);
        this.ivWrapImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showAd();
            }
        });
        this.ivWrapVideo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CreationActivity.class);
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

    public void loadAd() {
        UnityAds.load(INTERSTITIAL_ID, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                Log.d("UnityAds", "Ad loaded: " + placementId);
                showAd(); // You can show right away or later
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                Log.e("UnityAds", "Failed to load: " + placementId + " - " + error + " - " + message);
            }
        });
    }

    public void showAd() {
        UnityAds.show(this, INTERSTITIAL_ID, new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowStart(String placementId) {
                Log.d("UnityAds", "Ad started: " + placementId);
            }

            @Override
            public void onUnityAdsShowClick(String placementId) {
                Log.d("UnityAds", "Ad clicked: " + placementId);
            }

            @Override
            public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {
                Log.d("UnityAds", "Ad completed: " + placementId + " - " + state);
                Intent intent = new Intent(MainActivity.this, WrapImageActivity.class);
                startActivity(intent);

            }

            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                Log.e("UnityAds", "Ad failed: " + placementId + " - " + error + " - " + message);
                Intent intent = new Intent(MainActivity.this, WrapImageActivity.class);
                startActivity(intent);

            }
        });
    }

}
