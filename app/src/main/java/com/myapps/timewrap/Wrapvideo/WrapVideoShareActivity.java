package com.myapps.timewrap.Wrapvideo;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.FileProvider;
import com.myapps.timewrap.R;
import com.myapps.timewrap.UI.CreationActivity;
import com.myapps.timewrap.UI.MainActivity;
import com.myapps.timewrap.Utils.C1197util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class WrapVideoShareActivity extends AppCompatActivity {
    String isFrom = "";
    boolean isSave = false;
    ImageView ivBack;
    ImageView ivSave;
    ImageView ivShare;
    ImageView iv_image;
    Uri lastRecordedFile = null;
    ImageView previewViewImageView;
    VideoView videoView = null;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView((int) R.layout.activity_wrap_video_view);


        this.videoView = (VideoView) findViewById(R.id.vidView);
        this.previewViewImageView = (ImageView) findViewById(R.id.previewView_ImageView);
        this.ivBack = (ImageView) findViewById(R.id.iv_back);
        this.ivSave = (ImageView) findViewById(R.id.iv_save);
        this.ivShare = (ImageView) findViewById(R.id.iv_share);
        this.iv_image = (ImageView) findViewById(R.id.iv_image);
        Bundle extras = getIntent().getExtras();
        if (!(extras == null || extras.getString("from") == null)) {
            this.isFrom = extras.getString("from");
            Log.d("nmnmnmnm", "-----" + this.isFrom);
        }
        MediaController mediaController = new MediaController(this);
        mediaController.setMediaPlayer(this.videoView);
        mediaController.setAnchorView(this.videoView);
        this.videoView.setMediaController(mediaController);
        this.videoView.setVideoPath(C1197util.wrapVideoFile.getAbsolutePath());
        this.videoView.start();
        this.ivBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onBackPressed();
            }
        });
        this.iv_image.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startActivity(new Intent(WrapVideoShareActivity.this, CreationActivity.class).addFlags(67108864));
                finish();
            }
        });
        this.ivShare.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                WrapVideoShareActivity.this.ivSHAREit(view);
            }
        });
        this.ivSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                WrapVideoShareActivity.this.ivSAVEit(view);
            }
        });
        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            this.isSave = true;
            this.ivSave.setVisibility(8);
            this.iv_image.setVisibility(8);
        }
    }


    public void ivSHAREit(View view) {
        Uri uri;
        if (!this.isSave) {
            Log.e("TAG", "onCreate wrapVideoFile : " + C1197util.wrapVideoFile);
            addVideoToGalleryOnlyshare(C1197util.wrapVideoFile);
        }
        if (this.isSave) {
            if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
                Log.d("jijijj", "shaRE VIDEO-----" + C1197util.wrapVideoFile);
                Log.d("jijijj", "shaRE uRI-----" + Uri.parse(String.valueOf(C1197util.wrapVideoFile)));
                Context applicationContext = getApplicationContext();
                uri = FileProvider.getUriForFile(applicationContext, getPackageName() + ".provider", C1197util.wrapVideoFile);
            } else {
                uri = this.lastRecordedFile;
            }
            Intent intent = new Intent("android.intent.action.SEND");
            intent.putExtra("android.intent.extra.STREAM", uri);
            intent.setType("video/*");
            intent.setFlags(268435457);
            startActivity(Intent.createChooser(intent, "Share using"));
        }
    }

    public void ivSAVEit(View view) {
        if (!this.isSave) {
            Log.e("TAG", "onCreate wrapVideoFile : " + C1197util.wrapVideoFile);
            addVideoToGallery(C1197util.wrapVideoFile);
        }
    }

    private void addVideoToGalleryOnlyshare(File file) {
        try {
            getContentResolver();
            String absolutePath = file.getAbsolutePath();
            String substring = absolutePath.substring(absolutePath.lastIndexOf("/") + 1);
            new ContentValues();
            File file2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + getString(R.string.app_name) + File.separator + "WrapVideo");
            if (!file2.exists()) {
                file2.mkdir();
                file2.mkdirs();
            }
            File file3 = new File(file2, substring);
            C1197util.wrapVideoFile = file3;
            Log.d("jijijj", "addVideoToGallery-----" + C1197util.wrapVideoFile);
            if (copyFileToOther(file.getAbsolutePath(), file3.getAbsolutePath())) {
                this.isSave = true;
            }
        } catch (Exception e) {
            Log.d("SaveVideo", "e2--" + e.getMessage());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void addVideoToGallery(File file) {
        try {
            getContentResolver();
            String absolutePath = file.getAbsolutePath();
            String substring = absolutePath.substring(absolutePath.lastIndexOf("/") + 1);
            new ContentValues();
            File file2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + getString(R.string.app_name) + File.separator + "WrapVideo");
            if (!file2.exists()) {
                file2.mkdir();
                file2.mkdirs();
            }
            File file3 = new File(file2, substring);
            C1197util.wrapVideoFile = file3;
            Log.d("jijijj", "addVideoToGallery-----" + C1197util.wrapVideoFile);
            if (copyFileToOther(file.getAbsolutePath(), file3.getAbsolutePath())) {
                this.isSave = true;
                Toast.makeText(this, "Save Successfully", 0).show();
                startActivity(new Intent(this, CreationActivity.class).addFlags(67108864));
            }
        } catch (Exception e) {
            Log.d("SaveVideo", "e2--" + e.getMessage());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
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

    public void onResume() {
        super.onResume();
    }

    public void onBackPressed() {
        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            finish();
            return;
        }
        finish();
    }
}
