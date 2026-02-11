package com.myapps.timewrap.UI;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.view.ViewCompat;
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

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView((int) R.layout.activity_creation);

        PermissionAllow.GetPermission(this);
        adContainerView = findViewById(R.id.ad_view_container);
        loadBanner();


        this.rvGallery = (RecyclerView) findViewById(R.id.rvGallery);
        this.txtWaterfallVideo = (TextView) findViewById(R.id.txt_waterfall_video);
        this.txtWrapImage = (TextView) findViewById(R.id.txt_wrap_image);
        this.txtNoRecording = (TextView) findViewById(R.id.txtNoRecording);
        ImageView imageView = (ImageView) findViewById(R.id.iv_back);
        this.ivBack = imageView;
        imageView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                CreationActivity.this.onBackPressed();
            }
        });
        this.txtWrapImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                CreationActivity.this.txtWrapImage.setBackgroundResource(R.drawable.dark_view_bg);
                CreationActivity.this.txtWrapImage.setTextColor(-1);
                CreationActivity.this.txtWaterfallVideo.setBackgroundResource(0);
                CreationActivity.this.txtWaterfallVideo.setTextColor(ViewCompat.MEASURED_STATE_MASK);
                CreationActivity.this.getWrapImage();
            }
        });
        this.txtWaterfallVideo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                CreationActivity.this.txtWaterfallVideo.setBackgroundResource(R.drawable.dark_view_bg);
                CreationActivity.this.txtWaterfallVideo.setTextColor(-1);
                CreationActivity.this.txtWrapImage.setBackgroundResource(0);
                CreationActivity.this.txtWrapImage.setTextColor(ViewCompat.MEASURED_STATE_MASK);
                List<Video> waterfallVideos = CreationActivity.this.getWaterfallVideos();
                CreationActivity.this.waterfallVideo = waterfallVideos;
                if (waterfallVideos == null || waterfallVideos.size() <= 0) {
                    CreationActivity.this.txtNoRecording.setVisibility(0);
                    CreationActivity.this.rvGallery.setVisibility(8);
                    return;
                }
                CreationActivity.this.rvGallery.setVisibility(0);
                CreationActivity.this.txtNoRecording.setVisibility(8);
                List<Video> list = CreationActivity.this.waterfallVideo;
                CreationActivity creationActivity = CreationActivity.this;
                CreationAdapter creationAdapter = new CreationAdapter(list, creationActivity, 0, creationActivity.getContentResolver(), CreationActivity.this, C1197util.waterfallVideo);
                CreationActivity.this.adapter = creationAdapter;
                CreationActivity.this.rvGallery.setAdapter(creationAdapter);
            }
        });
        this.rvGallery.setLayoutManager(new GridLayoutManager(this, 2));
        getWrapImage();
    }

    public void onClick(Video video, String str) {
        /*if (str.equalsIgnoreCase(C1197util.wrapVideo)) {
            if (video != null) {
                playVid(video, str);
            }
        } else*/ if (str.equalsIgnoreCase(C1197util.waterfallVideo)) {
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
            StringBuilder sb = new StringBuilder();
            sb.append("Size: ");
            sb.append(listFiles.length);
            Log.d("Files", sb.toString());
            for (int i = 0; i < listFiles.length; i++) {
                Log.d("Files", "FileName:" + listFiles[i].getName());
                this.wrapImageList.add(new Video(listFiles[i].getName(), listFiles[i].getPath(), listFiles[i].getAbsolutePath()));
            }
            List<Video> list = this.wrapImageList;
            if (list != null) {
                if (list.size() > 0) {
                    this.rvGallery.setVisibility(0);
                    this.txtNoRecording.setVisibility(8);
                    CreationAdapter creationAdapter = new CreationAdapter(this.wrapImageList, this, 0, getContentResolver(), this, C1197util.wrapImage);
                    this.adapter = creationAdapter;
                    this.rvGallery.setAdapter(creationAdapter);
                    return;
                }
            }
            this.txtNoRecording.setVisibility(0);
            this.rvGallery.setVisibility(8);
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
                CreationActivity.this.txtNoRecording.setVisibility(0);
                CreationActivity.this.rvGallery.setVisibility(8);
                return;
            }
            CreationActivity.this.rvGallery.setVisibility(0);
            CreationActivity.this.txtNoRecording.setVisibility(8);
            CreationActivity creationActivity = CreationActivity.this;
            List<Video> list = creationActivity.videoList;
            CreationActivity creationActivity2 = CreationActivity.this;
            creationActivity.adapter = new CreationAdapter(list, creationActivity2, 0, creationActivity2.getContentResolver(), CreationActivity.this, C1197util.wrapVideo);
            CreationActivity.this.rvGallery.setAdapter(CreationActivity.this.adapter);
        }
    }

    private void loadBanner() {
        // [START create_ad_view]
        // Create a new ad view.
        adView = new AdView(this);
        adView.setAdUnitId(getResources().getString(R.string.banner));
        // [START set_ad_size]
        // Request an anchored adaptive banner with a width of 360.
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360));
        // [END set_ad_size]

        // Replace ad container with new ad view.
        adContainerView.removeAllViews();
        adContainerView.addView(adView);
        // [END create_ad_view]

        // [START load_ad]
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        // [END load_ad]
    }


}
