package com.myapps.timewrap.UI;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.myapps.timewrap.R;
import com.myapps.timewrap.Utils.C1197util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

public class WrapImageShareActivity extends AppCompatActivity {
    Uri fileURI = null;
    String isFrom = "";
    boolean isSaved = false;
    ImageView ivBack;
    ImageView ivSave;
    ImageView ivShare;
    ImageView iv_image;
    ImageView previewViewImageView;
    Bitmap resultBitmap;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView((int) R.layout.activity_wrapimage_share);





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
            ((RequestBuilder) ((RequestBuilder) Glide.with((FragmentActivity) this).load(C1197util.wrapImagePath).placeholder((int) R.drawable.icon)).error((int) R.drawable.icon)).into(this.previewViewImageView);
        } else {
            Bitmap bitmap = C1197util.bitmap;
            this.resultBitmap = bitmap;
            this.previewViewImageView.setImageBitmap(bitmap);
        }
        this.iv_image.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                WrapImageShareActivity.this.startActivity(new Intent(WrapImageShareActivity.this, CreationActivity.class).addFlags(67108864));
                WrapImageShareActivity.this.finish();
            }
        });
        this.ivShare.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Uri uri;
                if (!WrapImageShareActivity.this.isSaved) {
                    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
                    WrapImageShareActivity wrapImageShareActivity = WrapImageShareActivity.this;
                    wrapImageShareActivity.fileURI = wrapImageShareActivity.saveBitmapInGalary(wrapImageShareActivity.resultBitmap);
                    Log.e("TAG", "onCreate: " + WrapImageShareActivity.this.fileURI.getPath());
                    if (WrapImageShareActivity.this.fileURI != null) {
                        WrapImageShareActivity.this.isSaved = true;
                        MediaScannerConnection.scanFile(WrapImageShareActivity.this.getApplicationContext(), new String[]{WrapImageShareActivity.this.fileURI.getPath()}, new String[]{"image/jpeg"}, (MediaScannerConnection.OnScanCompletedListener) null);
                    }
                }
                if (WrapImageShareActivity.this.isSaved) {
                    if (WrapImageShareActivity.this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
                        Context applicationContext = WrapImageShareActivity.this.getApplicationContext();
                        uri = FileProvider.getUriForFile(applicationContext, WrapImageShareActivity.this.getPackageName() + ".provider", new File(C1197util.wrapImagePath));
                    } else {
                        uri = WrapImageShareActivity.this.fileURI;
                    }
                    Intent intent = new Intent("android.intent.action.SEND");
                    intent.putExtra("android.intent.extra.STREAM", uri);
                    intent.setType("image/jpeg");
                    intent.setFlags(268435457);
                    WrapImageShareActivity.this.startActivity(Intent.createChooser(intent, "Share Image using"));
                }
            }
        });
        this.ivSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!WrapImageShareActivity.this.isSaved) {
                    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
                    WrapImageShareActivity wrapImageShareActivity = WrapImageShareActivity.this;
                    wrapImageShareActivity.fileURI = wrapImageShareActivity.saveBitmapInGalary(wrapImageShareActivity.resultBitmap);
                    Log.e("TAG", "onCreate: " + WrapImageShareActivity.this.fileURI.getPath());
                    if (WrapImageShareActivity.this.fileURI != null) {
                        WrapImageShareActivity.this.isSaved = true;
                        MediaScannerConnection.scanFile(WrapImageShareActivity.this.getApplicationContext(), new String[]{WrapImageShareActivity.this.fileURI.getPath()}, new String[]{"image/jpeg"}, (MediaScannerConnection.OnScanCompletedListener) null);
                    }
                    WrapImageShareActivity.this.startActivity(new Intent(WrapImageShareActivity.this, CreationActivity.class).addFlags(67108864));
                }
            }
        });
        this.ivBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                WrapImageShareActivity.this.onBackPressed();
            }
        });
        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            this.isSaved = true;
            this.ivSave.setVisibility(8);
            this.iv_image.setVisibility(8);
        }
    }

    
    public Uri saveBitmapInGalary(Bitmap bitmap) {
        String file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
        File file2 = new File(file + "/" + getResources().getString(R.string.app_name) + File.separator + "WarpImage");
        file2.mkdirs();
        int nextInt = new Random().nextInt(10000);
        File file3 = new File(file2, "Image-" + nextInt + ".jpg");
        if (file3.exists()) {
            file3.delete();
        }
        try {
            C1197util.wrapImagePath = file3.getAbsolutePath();
            FileOutputStream fileOutputStream = new FileOutputStream(file3);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "Save Successfully", 0).show();
        return Uri.fromFile(file3);
    }

    public void onBackPressed() {
        if (this.isFrom.equalsIgnoreCase(C1197util.MyWork)) {
            finish();
            return;
        }
        finish();
    }
}
