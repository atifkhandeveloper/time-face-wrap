package com.myapps.timewrap.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.myapps.timewrap.R;
import com.myapps.timewrap.Utils.PlayStoreGo;

public class SettingsActivity extends AppCompatActivity {
    ImageView ivBack;
    RelativeLayout rlPrivacy;
    RelativeLayout rlRateApp;
    RelativeLayout rlShare;
    TextView txtVersion;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView((int) R.layout.activity_settings);




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

}
