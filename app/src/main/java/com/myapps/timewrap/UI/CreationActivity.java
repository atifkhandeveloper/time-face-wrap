package com.myapps.timewrap.UI;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.myapps.timewrap.R;
import com.myapps.timewrap.Utils.C1197util;
import com.myapps.timewrap.Wrapvideo.OnGalleryClickListener;

import com.myapps.timewrap.Wrapvideo.fragments.Video;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CreationActivity extends AppCompatActivity implements OnGalleryClickListener {
    CreationAdapter adapter;
    ImageView ivBack;
    RecyclerView rvGallery;
    TextView txtNoRecording;
    TextView txtWaterfallVideo;
    TextView txtWrapImage;
    List<Video> videoList = new ArrayList();
    List<Video> waterfallVideo = new ArrayList();
    List<Video> wrapImageList = new ArrayList();

    private AdView adView;
    private FrameLayout adContainerView;
    private boolean isPremium = false;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        enableEdgeToEdge();
        setContentView(R.layout.activity_creation);
        applyWindowInsets();
        PermissionAllow.GetPermission(this);

        // ✅ Check if user is premium
        isPremium = PremiumManager.isPremium(this);
        Log.d("CreationActivity", "User is premium: " + isPremium);

        adContainerView = findViewById(R.id.ad_view_container);

        // ✅ Only load banner if user is NOT premium
        if (!isPremium) {
            Log.d("CreationActivity", "Free user - loading banner");
            loadBanner();
        } else {
            Log.d("CreationActivity", "Premium user - hiding banner");
            hideBanner();
        }

        this.rvGallery = findViewById(R.id.rvGallery);
        this.txtWaterfallVideo = findViewById(R.id.txt_waterfall_video);
        this.txtWrapImage = findViewById(R.id.txt_wrap_image);
        this.txtNoRecording = findViewById(R.id.txtNoRecording);
        ImageView imageView = findViewById(R.id.iv_back);
        this.ivBack = imageView;
        imageView.setOnClickListener(view -> onBackPressed());

        this.txtWrapImage.setOnClickListener(view -> {
            txtWrapImage.setBackgroundResource(R.drawable.dark_view_bg);
            txtWrapImage.setTextColor(-1);
            txtWaterfallVideo.setBackgroundResource(0);
            txtWaterfallVideo.setTextColor(ViewCompat.MEASURED_STATE_MASK);
            getWrapImage();
        });

        this.txtWaterfallVideo.setOnClickListener(view -> {
            txtWaterfallVideo.setBackgroundResource(R.drawable.dark_view_bg);
            txtWaterfallVideo.setTextColor(-1);
            txtWrapImage.setBackgroundResource(0);
            txtWrapImage.setTextColor(ViewCompat.MEASURED_STATE_MASK);
            List<Video> waterfallVideos = getWaterfallVideos();
            waterfallVideo = waterfallVideos;
            if (waterfallVideos == null || waterfallVideos.size() <= 0) {
                txtNoRecording.setVisibility(View.VISIBLE);
                rvGallery.setVisibility(View.GONE);
                return;
            }
            rvGallery.setVisibility(View.VISIBLE);
            txtNoRecording.setVisibility(View.GONE);
            CreationAdapter creationAdapter = new CreationAdapter(waterfallVideo, this, 0, getContentResolver(), this, C1197util.waterfallVideo);
            adapter = creationAdapter;
            rvGallery.setAdapter(creationAdapter);
        });

        this.rvGallery.setLayoutManager(new GridLayoutManager(this, 2));
        getWrapImage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ Refresh premium status
        boolean currentPremium = PremiumManager.isPremium(this);
        if (currentPremium != isPremium) {
            isPremium = currentPremium;
            Log.d("CreationActivity", "Premium status changed to: " + isPremium);
            updateAdVisibility();
        }
    }

    private void updateAdVisibility() {
        if (isPremium) {
            hideBanner();
        } else {
            if (adContainerView.getChildCount() == 0) {
                loadBanner();
            }
            adContainerView.setVisibility(View.VISIBLE);
        }
    }

    private void hideBanner() {
        if (adContainerView != null) {
            adContainerView.removeAllViews();
            adContainerView.setVisibility(View.GONE);
        }
    }

    public void onClick(Video video, String str) {
        if (str.equalsIgnoreCase(C1197util.waterfallVideo)) {
            C1197util.waterVideo = new File(video.getRealPath());
            Intent intent = new Intent(this, WaterfallShareActivity.class);
            intent.putExtra("from", C1197util.MyWork);
            startActivity(intent);
        } else if (str.equalsIgnoreCase(C1197util.wrapImage)) {
            C1197util.wrapImagePath = video.getRealPath();
            Intent intent2 = new Intent(this, WrapImageShareActivity.class);
            intent2.putExtra("from", C1197util.MyWork);
            startActivity(intent2);
        }
    }

    public void share(Uri uri) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("video/*");
        intent.putExtra("android.intent.extra.STREAM", uri);
        intent.addFlags(1);
        startActivity(Intent.createChooser(intent, "Share using"));
    }

    public void getWrapImage() {
        try {
            this.wrapImageList = new ArrayList();
            Log.d("Files", "Path: /storage/emulated/0/DCIM/" + getResources().getString(R.string.app_name) + File.separator + "WarpImage");
            File[] listFiles = new File("/storage/emulated/0/DCIM/" + getResources().getString(R.string.app_name) + File.separator + "WarpImage").listFiles();
            if (listFiles != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Size: ");
                sb.append(listFiles.length);
                Log.d("Files", sb.toString());
                for (int i = 0; i < listFiles.length; i++) {
                    Log.d("Files", "FileName:" + listFiles[i].getName());
                    this.wrapImageList.add(new Video(listFiles[i].getName(), listFiles[i].getPath(), listFiles[i].getAbsolutePath()));
                }
                List<Video> list = this.wrapImageList;
                if (list != null && list.size() > 0) {
                    this.rvGallery.setVisibility(View.VISIBLE);
                    this.txtNoRecording.setVisibility(View.GONE);
                    CreationAdapter creationAdapter = new CreationAdapter(this.wrapImageList, this, 0, getContentResolver(), this, C1197util.wrapImage);
                    this.adapter = creationAdapter;
                    this.rvGallery.setAdapter(creationAdapter);
                    return;
                }
            }
            this.txtNoRecording.setVisibility(View.VISIBLE);
            this.rvGallery.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Video> getVids() {
        ArrayList arrayList = new ArrayList();
        File[] listFiles = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + getString(R.string.app_name) + File.separator + "WrapVideo").listFiles();
        if (listFiles != null) {
            for (int i = 0; i < listFiles.length; i++) {
                if (!listFiles[i].isDirectory() && listFiles[i].getName().endsWith(".mp4")) {
                    arrayList.add(new Video(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, 0), listFiles[i].getName(), listFiles[i].getAbsolutePath(), listFiles[i].getAbsolutePath()));
                }
            }
        }
        return arrayList;
    }

    public List<Video> getWaterfallVideos() {
        ArrayList arrayList = new ArrayList();
        File[] listFiles = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + getString(R.string.app_name) + File.separator + "WaterFallVideos").listFiles();
        if (listFiles != null) {
            for (int i = 0; i < listFiles.length; i++) {
                if (!listFiles[i].isDirectory() && listFiles[i].getName().endsWith(".mp4")) {
                    arrayList.add(new Video(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, 0), listFiles[i].getName(), listFiles[i].getAbsolutePath(), listFiles[i].getAbsolutePath()));
                }
            }
        }
        return arrayList;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private class GetGalleryData extends AsyncTask<String, Void, String> {
        List<Video> vids;

        public void onPreExecute() {
        }

        public void onProgressUpdate(Void... voidArr) {
        }

        private GetGalleryData() {
        }

        public String doInBackground(String... strArr) {
            this.vids = CreationActivity.this.getVids();
            return null;
        }

        public void onPostExecute(String str) {
            CreationActivity.this.videoList.clear();
            CreationActivity.this.videoList.addAll(this.vids);
            if (CreationActivity.this.videoList.size() == 0) {
                CreationActivity.this.txtNoRecording.setVisibility(View.VISIBLE);
                CreationActivity.this.rvGallery.setVisibility(View.GONE);
                return;
            }
            CreationActivity.this.rvGallery.setVisibility(View.VISIBLE);
            CreationActivity.this.txtNoRecording.setVisibility(View.GONE);
            CreationActivity creationActivity = CreationActivity.this;
            List<Video> list = creationActivity.videoList;
            CreationActivity creationActivity2 = CreationActivity.this;
            creationActivity.adapter = new CreationAdapter(list, creationActivity2, 0, creationActivity2.getContentResolver(), CreationActivity.this, C1197util.wrapVideo);
            CreationActivity.this.rvGallery.setAdapter(CreationActivity.this.adapter);
        }
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