package com.myapps.timewrap.splashAds;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.myapps.timewrap.R;
import com.myapps.timewrap.ads.MyApplication;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    String var;

    private static final String INTERSTITIAL_ID = "Interstitial_Android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        loadAd();


        

        new Handler().postDelayed(new Runnable() {
            public void run() {
                OpenAppAds();
            }
        }, 5000);


    }


    public void OpenAppAds() {
        try {

                goNext();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goNext() {
        loadOpenApp();
    }

    private void loadOpenApp() {
        if (MyApplication.getuser_onetime() == 0) {

            showAd();

        }else {
            Intent i = new Intent(SplashActivity.this, FirstPageMainActivity.class);
            startActivity(i);
        }
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
                Intent i = new Intent(SplashActivity.this, PrivacyTermsActivity.class);
                startActivity(i);
            }

            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                Log.e("UnityAds", "Ad failed: " + placementId + " - " + error + " - " + message);
                Intent i = new Intent(SplashActivity.this, PrivacyTermsActivity.class);
                startActivity(i);
            }
        });
    }
}
