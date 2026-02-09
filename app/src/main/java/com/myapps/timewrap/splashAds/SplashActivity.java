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



import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    String var;

    private static final String INTERSTITIAL_ID = "Interstitial_Android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);



        

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

            Intent i = new Intent(SplashActivity.this, PrivacyTermsActivity.class);
            startActivity(i);

        }else {
            Intent i = new Intent(SplashActivity.this, FirstPageMainActivity.class);
            startActivity(i);
        }
    }


}
