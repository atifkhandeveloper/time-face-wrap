package com.myapps.timewrap.UI;

import static android.content.ContentValues.TAG;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.myapps.timewrap.R;
import com.myapps.timewrap.Utils.C1197util;
import com.myapps.timewrap.splashAds.FirstPageMainActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.FileChannel;

public class WaterfallShareActivity extends AppCompatActivity {

    private static String MEDIA_FOLDER = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + "TIME WARP WATERFALL" + File.separator);
    Uri fileURI = null;
    String isFrom = "";
    boolean isSave = false;
    ImageView ivBack;
    ImageView ivSave;
    ImageView ivShare;
    ImageView iv_image;
    ImageView previewViewImageView;
    VideoView videoView = null;
    private AdView adView;
    private FrameLayout adContainerView;
    private InterstitialAd interstitialAd;
    private boolean adIsLoading;



    public static void moveFile(File file, File file2) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        FileOutputStream fileOutputStream = new FileOutputStream(file2);
        byte[] bArr = new byte[1024];
        while (true) {
            int read = fileInputStream.read(bArr);
            if (read <= 0) {
                break;
            }
            fileOutputStream.write(bArr, 0, read);
        }
        fileInputStream.close();
        fileOutputStream.close();
        if (!file.exists()) {
            return;
        }
        if (file.delete()) {
            PrintStream printStream = System.out;
            printStream.println("file Deleted :" + file.getPath());
            return;
        }
        PrintStream printStream2 = System.out;
        printStream2.println("file not Deleted :" + file.getPath());
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView((int) R.layout.activity_waterfall_share);


        adContainerView = findViewById(R.id.ad_view_container);

        loadBanner();
        loadAd();


        this.videoView = (VideoView) findViewById(R.id.videoView);
        this.previewViewImageView = (ImageView) findViewById(R.id.previewView_ImageView);
        this.ivBack = (ImageView) findViewById(R.id.iv_back);
        this.ivSave = (ImageView) findViewById(R.id.iv_save);
        this.ivShare = (ImageView) findViewById(R.id.iv_share);
        this.iv_image = (ImageView) findViewById(R.id.iv_image);
        Bundle extras = getIntent().getExtras();
        if (!(extras == null || extras.getString("from") == null)) {
            this.isFrom = extras.getString("from");
        }
        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            MediaController mediaController = new MediaController(this);
            mediaController.setMediaPlayer(this.videoView);
            mediaController.setAnchorView(this.videoView);
            this.videoView.setMediaController(mediaController);
            this.videoView.setVideoPath(C1197util.waterVideo.getAbsolutePath());
            this.videoView.start();
        } else {
            MediaController mediaController2 = new MediaController(this);
            mediaController2.setMediaPlayer(this.videoView);
            mediaController2.setAnchorView(this.videoView);
            this.videoView.setMediaController(mediaController2);
            this.videoView.setVideoPath(WaterFallActivity.bitmapToVideoEncoder.getOutputFile().getPath());
            this.videoView.start();
        }
        this.ivBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onBackPressed();
            }
        });
        this.iv_image.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startActivity(new Intent(WaterfallShareActivity.this, CreationActivity.class).addFlags(67108864));
                finish();
            }
        });
        this.ivShare.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                WaterfallShareActivity.this.ivSHAREfall(view);
            }
        });
        this.ivSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showInterstitial();

            }
        });
        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            this.isSave = true;
            this.ivSave.setVisibility(8);
            this.iv_image.setVisibility(8);
        }
    }

    public void ivSHAREfall(View view) {
        Uri uri;
        boolean z = this.isSave;
        if (!z) {
            if (!z) {
                addVideoToGalleryOnlyShare(WaterFallActivity.bitmapToVideoEncoder.getOutputFile());
            } else {
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
                File outputFile = WaterFallActivity.bitmapToVideoEncoder.getOutputFile();
                File file = new File(MEDIA_FOLDER + "water_fall_" + System.currentTimeMillis() + ".mp4");
                if (Build.VERSION.SDK_INT <= 28) {
                    try {
                        moveFile(outputFile, file);
                        this.fileURI = Uri.fromFile(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    this.fileURI = saveVideoMediaStore(outputFile);
                }
                if (this.fileURI != null) {
                    this.isSave = true;
                    MediaScannerConnection.scanFile(getApplicationContext(), new String[]{this.fileURI.getPath()}, new String[]{"video/mp4"}, (MediaScannerConnection.OnScanCompletedListener) null);
                }
            }
        }
        if (this.isSave) {
            if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
                Context applicationContext = getApplicationContext();
                uri = FileProvider.getUriForFile(applicationContext, getPackageName() + ".provider", C1197util.waterVideo);
            } else {
                uri = this.fileURI;
            }
            Intent intent = new Intent("android.intent.action.SEND");
            intent.putExtra("android.intent.extra.STREAM", uri);
            intent.setType("video/*");
            intent.setFlags(268435457);
            startActivity(Intent.createChooser(intent, "Share using"));
        }
    }

    public void ivSAVEfall(View view) {
        if (!this.isSave) {
            addVideoToGallery(WaterFallActivity.bitmapToVideoEncoder.getOutputFile());
            return;
        }
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
        File outputFile = WaterFallActivity.bitmapToVideoEncoder.getOutputFile();
        File file = new File(MEDIA_FOLDER + "water_fall_" + System.currentTimeMillis() + ".mp4");
        if (Build.VERSION.SDK_INT <= 28) {
            try {
                moveFile(outputFile, file);
                this.fileURI = Uri.fromFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            this.fileURI = saveVideoMediaStore(outputFile);
        }
        if (this.fileURI != null) {
            this.isSave = true;
            MediaScannerConnection.scanFile(getApplicationContext(), new String[]{this.fileURI.getPath()}, new String[]{"video/mp4"}, (MediaScannerConnection.OnScanCompletedListener) null);
        }
    }

    public static boolean copyFileToOther(String str, String str2) throws Throwable {
        FileChannel fileChannel;
        Log.d("SaveVideo", "copyFileToOther from--" + str);
        Log.d("SaveVideo", "copyFileToOther to--" + str2);
        File file = new File(str);
        File file2 = new File(str2);
        try {
            if (!file2.getParentFile().exists()) {
                file2.getParentFile().mkdirs();
            }
            if (!file2.exists()) {
                file2.createNewFile();
            }
            FileChannel fileChannel2 = null;
            try {
                FileChannel channel = new FileInputStream(file).getChannel();
                try {
                    fileChannel2 = new FileOutputStream(file2).getChannel();
                    fileChannel2.transferFrom(channel, 0, channel.size());
                    if (channel != null) {
                        channel.close();
                    }
                    if (fileChannel2 == null) {
                        return true;
                    }
                    fileChannel2.close();
                    return true;
                } catch (Throwable th) {
                    th = th;
                    FileChannel fileChannel3 = channel;
                    fileChannel = fileChannel2;
                    fileChannel2 = fileChannel3;
                    if (fileChannel2 != null) {
                        fileChannel2.close();
                    }
                    if (fileChannel != null) {
                        fileChannel.close();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                //th = th2;
                fileChannel = null;
                if (fileChannel2 != null) {
                }
                if (fileChannel != null) {
                }
                throw th2;
            }
        } catch (Exception e) {
            Log.d("SaveVideo", "exce--" + e.getMessage());
            return false;
        }
    }

    private void addVideoToGalleryOnlyShare(File file) {
        try {
            getContentResolver();
            String absolutePath = file.getAbsolutePath();
            String substring = absolutePath.substring(absolutePath.lastIndexOf("/") + 1);
            File file2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + getString(R.string.app_name) + File.separator + "WaterFallVideos");
            if (!file2.exists()) {
                file2.mkdir();
                file2.mkdirs();
            }
            File file3 = new File(file2, substring);
            C1197util.waterVideo = file3;
            if (copyFileToOther(file.getAbsolutePath(), file3.getAbsolutePath())) {
                this.isSave = true;
            }
        } catch (Exception unused) {
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void addVideoToGallery(File file) {
        try {
            getContentResolver();
            String absolutePath = file.getAbsolutePath();
            String substring = absolutePath.substring(absolutePath.lastIndexOf("/") + 1);
            File file2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + getString(R.string.app_name) + File.separator + "WaterFallVideos");
            if (!file2.exists()) {
                file2.mkdir();
                file2.mkdirs();
            }
            File file3 = new File(file2, substring);
            C1197util.waterVideo = file3;
            if (copyFileToOther(file.getAbsolutePath(), file3.getAbsolutePath())) {
                this.isSave = true;
                Toast.makeText(this, "Save Successfully", 0).show();
                startActivity(new Intent(this, CreationActivity.class).addFlags(67108864));
            }
        } catch (Exception unused) {
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private Uri saveVideoMediaStore(File file) {
        try {
            ContentResolver contentResolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put("title", file.getName());
            contentValues.put("_display_name", "water_fall_" + System.currentTimeMillis() + ".mp4");
            contentValues.put("mime_type", "video/mp4");
            contentValues.put("relative_path", Environment.DIRECTORY_DCIM + File.separator + "Camera");
            Uri insert = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                OutputStream openOutputStream = getContentResolver().openOutputStream(insert);
                byte[] bArr = new byte[4096];
                while (true) {
                    int read = fileInputStream.read(bArr);
                    if (read == -1) {
                        break;
                    }
                    openOutputStream.write(bArr, 0, read);
                }
                openOutputStream.flush();
                fileInputStream.close();
                openOutputStream.close();
            } catch (Exception e) {
                Log.e("TAG", "exception while writing video: ", e);
            }
            return insert;
        } catch (Exception unused) {
            return null;
        }
    }

    public void playVideo() {
        this.videoView.setVideoPath(WaterFallActivity.bitmapToVideoEncoder.getOutputFile().getPath());
        this.videoView.start();
    }

    public void onBackPressed() {
        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            finish();
            return;
        }
        finish();
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

    public void loadAd() {
        // Request a new ad if one isn't already loaded.
        if (adIsLoading || interstitialAd != null) {
            return;
        }
        adIsLoading = true;
        InterstitialAd.load(
                this,
                getResources().getString(R.string.interstial),
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        Log.d(TAG, "Ad was loaded.");
                        WaterfallShareActivity.this.interstitialAd = interstitialAd;
                        adIsLoading = false;
                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        // Called when fullscreen content is dismissed.
                                        Log.d(TAG, "The ad was dismissed.");
                                        // Make sure to set your reference to null so you don't
                                        // show it a second time.
                                        WaterfallShareActivity.this.interstitialAd = null;
                                        WaterfallShareActivity.this.ivSAVEfall(null);
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                                        // Called when fullscreen content failed to show.
                                        Log.d(TAG, "The ad failed to show.");
                                        // Make sure to set your reference to null so you don't
                                        // show it a second time.
                                        WaterfallShareActivity.this.interstitialAd = null;
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        // Called when fullscreen content is shown.
                                        Log.d(TAG, "The ad was shown.");
                                    }

                                    @Override
                                    public void onAdImpression() {
                                        // Called when an impression is recorded for an ad.
                                        Log.d(TAG, "The ad recorded an impression.");
                                    }

                                    @Override
                                    public void onAdClicked() {
                                        // Called when ad is clicked.
                                        Log.d(TAG, "The ad was clicked.");
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, loadAdError.getMessage());
                        interstitialAd = null;
                        adIsLoading = false;

                        WaterfallShareActivity.this.ivSAVEfall(null);
                    }
                });
    }


    private void showInterstitial() {
        // Show the ad if it's ready. Otherwise restart the game.
        // [START show_ad]
        if (interstitialAd != null) {
            interstitialAd.show(this);
        } else {
            Log.d(TAG, "The interstitial ad is still loading.");
            // [START_EXCLUDE silent]
            WaterfallShareActivity.this.ivSAVEfall(null);
            loadAd();

            // [END_EXCLUDE]
        }
        // [END show_ad]
    }
}
