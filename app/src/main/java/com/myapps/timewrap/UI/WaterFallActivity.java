package com.myapps.timewrap.UI;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.YuvImage;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.PointerIconCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.myapps.timewrap.R;
import com.myapps.timewrap.Utils.BitmapToVideoEncoder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class WaterFallActivity extends AppCompatActivity implements CameraXConfig.Provider {
    private static final String APP_OPENED_PREF = "WATERFALL_APP_OPENED_PREF";
    private static final int FREE_SCANS = 3;
    private static final String GOOGLE_AD_COUNTER_PREF = "WATERFALL_GOOGLE_AD_COUNTER_PREF_1";
    public static String MEDIA_FOLDER = (Environment.getExternalStorageDirectory().toString() + File.separator + "TIME WARP WATERFALL" + File.separator);
    private static final String RATE_DIALOG_PREF = "WATERFALL_RATE_DIALOG_PREF";
    private static final String SYSTEM_TIME_PREF = "WATERFALL_SYSTEM_TIME_PREF";
    private static final String TIME_WARP_WATERFALL_PREFS = "TIME_WARP_WATERFALL_PREFS";
    static BitmapToVideoEncoder bitmapToVideoEncoder;
    public int REQUEST_CODE_PERMISSIONS = PointerIconCompat.TYPE_CONTEXT_MENU;
    ConstraintLayout beforeCaptureUI = null;
    ProcessCameraProvider cameraProvider;
    public ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    CameraSelector cameraSelector;
    boolean capture = false;
    ConstraintLayout captureUI = null;
    int facing = 0;
    Uri fileURI = null;
    private Integer frameRate = 25;
    ImageAnalysis imageAnalysis;
    ImageView imageView = null;
    boolean isSwitching = false;
    int lineCount = 0;
    int lineResolution = 5;
    Camera mCamera;
    Preview preview;
    public PreviewView previewView;
    ImageView previewViewImageView = null;
    int resolutionX = 480;
    int resolutionY = 640;
    Bitmap resultBitmap = null;
    ConstraintLayout resultUI = null;

    Button saveButton;
    SharedPreferences sharedPref = null;
    Bitmap subBitmap = null;
    ImageView toogleTorch_ImageView = null;
    ConstraintLayout tutorialUI = null;
    VideoView videoView = null;
    List<Bitmap> waterfallBitmapList = null;
    ImageView waterfallView = null;

    public static Bitmap MirrorBitmap(Bitmap bitmap, int i, int i2) {
        Matrix matrix = new Matrix();
        matrix.preScale((float) i, (float) i2);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

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
        if (Build.VERSION.SDK_INT > 29) {
            this.resolutionX = 720;
            this.resolutionY = 1280;
            this.lineResolution = 7;
        }
        getWindow().setFlags(1024, 1024);
        setContentView((int) R.layout.activity_waterfall);


        PermissionAllow.GetPermission(this);


        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(TIME_WARP_WATERFALL_PREFS, 0);
        this.sharedPref = sharedPreferences;
        sharedPreferences.edit().putInt(APP_OPENED_PREF, this.sharedPref.getInt(APP_OPENED_PREF, 0) + 1).apply();
        File file = new File(MEDIA_FOLDER);
        if (!file.exists()) {
            file.mkdirs();
            sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", Uri.fromFile(file)));
        }
        PreviewView previewView2 = (PreviewView) findViewById(R.id.previewView);
        this.previewView = previewView2;
        previewView2.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        this.toogleTorch_ImageView = (ImageView) findViewById(R.id.toogleTorch_ImageView);
        this.previewViewImageView = (ImageView) findViewById(R.id.previewView_ImageView);
        this.videoView = (VideoView) findViewById(R.id.videoView);
        this.waterfallView = (ImageView) findViewById(R.id.waterfall_View);
        this.waterfallBitmapList = new ArrayList();
        this.cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        ImageAnalysis build = new ImageAnalysis.Builder().setTargetResolution(new Size(this.resolutionX, this.resolutionY)).setBackpressureStrategy(0).build();
        this.imageAnalysis = build;
        build.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageCapture());
        this.cameraProviderFuture.addListener(new Runnable() {
            public final void run() {
                try {
                    WaterFallActivity waterFallActivity = WaterFallActivity.this;
                    waterFallActivity.cameraProvider = (ProcessCameraProvider) waterFallActivity.cameraProviderFuture.get();
                    WaterFallActivity waterFallActivity2 = WaterFallActivity.this;
                    waterFallActivity2.bindPreview(waterFallActivity2.cameraProvider);
                    /*if (WaterFallActivity.this.allPermissionsGranted()) {
                        WaterFallActivity waterFallActivity2 = WaterFallActivity.this;
                        waterFallActivity2.bindPreview(waterFallActivity2.cameraProvider);
                        return;
                    }
                    WaterFallActivity waterFallActivity3 = WaterFallActivity.this;
                    ActivityCompat.requestPermissions(waterFallActivity3, waterFallActivity3.REQUIRED_PERMISSIONS, WaterFallActivity.this.REQUEST_CODE_PERMISSIONS);*/
                } catch (InterruptedException | ExecutionException unused) {
                }
            }
        }, ContextCompat.getMainExecutor(this));
        this.beforeCaptureUI = (ConstraintLayout) findViewById(R.id.before_capture_UI);
        this.captureUI = (ConstraintLayout) findViewById(R.id.capture_UI);
        this.resultUI = (ConstraintLayout) findViewById(R.id.result_UI);
        this.imageView = (ImageView) findViewById(R.id.result_imageView);
        Button button = (Button) findViewById(R.id.save_Button);
        this.saveButton = button;
        button.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
                File outputFile = WaterFallActivity.bitmapToVideoEncoder.getOutputFile();
                File file = new File(WaterFallActivity.MEDIA_FOLDER + System.currentTimeMillis() + ".mp4");
                if (Build.VERSION.SDK_INT <= 28) {
                    try {
                        WaterFallActivity.moveFile(outputFile, file);
                        WaterFallActivity.this.fileURI = Uri.fromFile(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    WaterFallActivity waterFallActivity = WaterFallActivity.this;
                    waterFallActivity.fileURI = waterFallActivity.saveVideoMediaStore(outputFile);
                }
                if (WaterFallActivity.this.fileURI != null) {
                    MediaScannerConnection.scanFile(WaterFallActivity.this.getApplicationContext(), new String[]{WaterFallActivity.this.fileURI.getPath()}, new String[]{"video/mp4"}, (MediaScannerConnection.OnScanCompletedListener) null);
                }
                WaterFallActivity.this.saveButton.setClickable(true);
            }
        });
        ((ImageView) findViewById(R.id.capture_video_Button)).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                WaterFallActivity.this.videoView.setVisibility(0);
                try {
                    WaterFallActivity.this.startCapture();
                    WaterFallActivity.this.hideBeforeCaptureUI();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        ((ImageView) findViewById(R.id.stop_capture_video_Button)).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                WaterFallActivity.this.videoView.setVisibility(0);
                WaterFallActivity.this.stopCapture();
            }
        });
        ((Button) findViewById(R.id.resultCancel_Button)).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                WaterFallActivity.this.resumeToBeforeCaptureUI();
            }
        });
        ((ImageView) findViewById(R.id.switchCamera_ImageView)).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                WaterFallActivity.this.isSwitching = true;
                if (WaterFallActivity.this.facing == 0) {
                    WaterFallActivity.this.setFacing(1);
                } else {
                    WaterFallActivity.this.setFacing(0);
                }
                WaterFallActivity waterFallActivity = WaterFallActivity.this;
                waterFallActivity.bindPreview(waterFallActivity.cameraProvider);
                WaterFallActivity.this.isSwitching = false;
            }
        });
    }

    /*public boolean allPermissionsGranted() {
        for (String checkSelfPermission : this.REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, checkSelfPermission) != 0) {
                return false;
            }
        }
        return true;
    }*/

    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        if (i != this.REQUEST_CODE_PERMISSIONS) {
            return;
        }
        bindPreview(this.cameraProvider);
        /*if (allPermissionsGranted()) {
            bindPreview(this.cameraProvider);
            return;
        }
        Toast.makeText(this, "Permissions not granted by the user.", 0).show();
        finish();*/
    }

    public void setFacing(int i) {
        this.facing = i;
    }

    public Bitmap overlay(Bitmap bitmap, Bitmap bitmap2) {
        new Matrix().preScale(1.0f, -1.0f);
        Bitmap createBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(createBitmap);
        Paint paint = null;
        canvas.drawBitmap(bitmap, new Matrix(), paint);
        canvas.drawBitmap(bitmap2, 0.0f, 0.0f, paint);
        return createBitmap;
    }

    public Bitmap rotateBitmap(Bitmap bitmap, int i) {
        Matrix matrix = new Matrix();
        matrix.setRotate((float) i);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }

    public void drawWaterMark(Bitmap bitmap, Context context) {
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(-1);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        paint.setTextSize(15.0f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, 1));
        canvas.drawText("On Google Play Store:", paint.getTextSize(), paint.getTextSize() * 2.0f, paint);
        canvas.drawText(String.valueOf(context.getResources().getText(R.string.app_name)), paint.getTextSize(), (paint.getTextSize() * 3.0f) + 0.0f, paint);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
    }

    public void addWaterfallBitmap(Bitmap bitmap) {
        this.waterfallBitmapList.add(0, bitmap);
    }

    public void updateWaterfallBitmap(Bitmap bitmap) {
        Paint paint = new Paint();
        Canvas canvas = new Canvas(bitmap);
        for (int size = this.waterfallBitmapList.size() - 1; size > 0; size--) {
            canvas.drawBitmap(this.waterfallBitmapList.get(size), 0.0f, (float) ((bitmap.getHeight() / 2) + (this.lineResolution * size)), paint);
        }
        this.waterfallView.setImageBitmap(bitmap);
    }

    public void initializeImageView() {
        Bitmap createBitmap = Bitmap.createBitmap(this.resolutionX, this.resolutionY, Bitmap.Config.ARGB_8888);
        this.resultBitmap = createBitmap;
        createBitmap.eraseColor(0);
        this.imageView.setImageBitmap(this.resultBitmap);
    }

    private void showBeforeCaptureUI() {
        this.beforeCaptureUI.setVisibility(0);
    }

    public void hideBeforeCaptureUI() {
        this.beforeCaptureUI.setVisibility(4);
    }

    private void showCaptureUI() {
        this.captureUI.setVisibility(0);
    }

    private void hideCaptureUI() {
        this.captureUI.setVisibility(4);
    }

    private void showResultUI() {
        Intent intent = new Intent(this, WaterfallShareActivity.class);
        startActivity(intent);
    }

    private void hideResultUI() {
        this.resultUI.setVisibility(4);
    }

    public void shareFile(String str, Uri uri) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.SEND");
        intent.setType("video/mp4");
        intent.addFlags(1);
        intent.putExtra("android.intent.extra.STREAM", uri);
        if (checkAppInstall(str)) {
            intent.setPackage(str);
            startActivity(intent);
            return;
        }
        Toast.makeText(getApplicationContext(), "Sorry but this app is not installed. Wanna share your piece of art with another app? ", 1).show();
        startActivity(Intent.createChooser(intent, "Send Image using "));
    }

    public void shareFileWithChooser(Uri uri) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.putExtra("android.intent.extra.STREAM", uri);
        intent.addFlags(1);
        intent.setType("video/mp4");
        startActivity(Intent.createChooser(intent, "Send Image using "));
    }

    private boolean checkAppInstall(String str) {
        try {
            getPackageManager().getPackageInfo(str, 0);
            return true;
        } catch (PackageManager.NameNotFoundException unused) {
            return false;
        }
    }

    private int getRandomColor() {
        Random random = new Random();
        return Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    public void resumeToBeforeCaptureUI() {
        this.waterfallBitmapList = new ArrayList();
        this.waterfallView.setVisibility(4);
        initializeImageView();
        hideResultUI();
        hideCaptureUI();
        showBeforeCaptureUI();
        this.capture = false;
    }

    public void startCapture() throws InterruptedException {
        File file;
        bitmapToVideoEncoder = new BitmapToVideoEncoder(new BitmapToVideoEncoder.IBitmapToVideoEncoderCallback() {
            public void onEncodingComplete(File file) {
                WaterFallActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        WaterFallActivity.this.playVideo();
                    }
                });
            }
        });
        try {
            file = File.createTempFile("tempFile", ".mp4", getApplicationContext().getCacheDir());
            file.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
            bitmapToVideoEncoder.startEncoding(this.resolutionX, this.resolutionY, (File) null);
            this.lineCount = 0;
            this.resultBitmap = null;
            initializeImageView();
            hideResultUI();
            showCaptureUI();
            this.capture = true;
            file = null;
        }
        bitmapToVideoEncoder.startEncoding(this.resolutionX, this.resolutionY, file);
        this.lineCount = 0;
        this.resultBitmap = null;
        initializeImageView();
        hideResultUI();
        showCaptureUI();
        this.capture = true;
    }

    private void showRateAlertDialog() {
        final String packageName = getPackageName();
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
        builder.setMessage("Hello you awsome person! Please help us with a good rating in the Google Play Store! :)");
        builder.setCancelable(true);
        builder.setNegativeButton("Yeah for sure!", new DialogInterface.OnClickListener() {
            public final void onClick(DialogInterface dialogInterface, int i) {
                try {
                    WaterFallActivity waterFallActivity = WaterFallActivity.this;
                    waterFallActivity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=" + packageName)));
                } catch (ActivityNotFoundException unused) {
                    WaterFallActivity waterFallActivity2 = WaterFallActivity.this;
                    waterFallActivity2.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
                }
            }
        });
        builder.setPositiveButton("No thanks!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        AlertDialog create = builder.create();
        create.getWindow().setBackgroundDrawable(getDrawable(R.drawable.layout_bg));
        create.show();
    }

    public Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        ByteBuffer buffer2 = planes[1].getBuffer();
        ByteBuffer buffer3 = planes[2].getBuffer();
        int remaining = buffer.remaining();
        int remaining2 = buffer2.remaining();
        int remaining3 = buffer3.remaining();
        byte[] bArr = new byte[(remaining + remaining2 + remaining3)];
        buffer.get(bArr, 0, remaining);
        buffer3.get(bArr, remaining, remaining3);
        buffer2.get(bArr, remaining + remaining3, remaining2);
        YuvImage yuvImage = new YuvImage(bArr, 17, image.getWidth(), image.getHeight(), (int[]) null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

    public CameraXConfig getCameraXConfig() {
        return Camera2Config.defaultConfig();
    }

    public void bindPreview(ProcessCameraProvider processCameraProvider) {
        processCameraProvider.unbindAll();
        this.preview = new Preview.Builder().setTargetResolution(new Size(this.resolutionX, this.resolutionY)).build();
        this.cameraSelector = new CameraSelector.Builder().requireLensFacing(this.facing).build();
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
        Camera2Interop.Extender extender = new Camera2Interop.Extender(builder);
        CaptureRequest.Key key = CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE;
        Integer num = this.frameRate;
        extender.setCaptureRequestOption(key, new Range(num, num));
        ImageAnalysis build = builder.setTargetResolution(new Size(this.resolutionX, this.resolutionY)).setBackpressureStrategy(0).build();
        this.imageAnalysis = build;
        build.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageCapture());
        this.preview.setSurfaceProvider(this.previewView.getSurfaceProvider());
        this.mCamera = processCameraProvider.bindToLifecycle((LifecycleOwner) this, this.cameraSelector, this.preview);
        processCameraProvider.bindToLifecycle((LifecycleOwner) this, this.cameraSelector, this.imageAnalysis, this.preview);
        this.toogleTorch_ImageView.setBackgroundResource(R.drawable.ic_flash_off_black_24dp);
        if (this.mCamera.getCameraInfo().hasFlashUnit()) {
            this.toogleTorch_ImageView.setVisibility(0);
        } else {
            this.toogleTorch_ImageView.setVisibility(4);
        }
    }

    public void toggleTorch(View view) {
        if (!this.mCamera.getCameraInfo().hasFlashUnit()) {
            return;
        }
        if (this.mCamera.getCameraInfo().getTorchState().getValue().intValue() == 1) {
            this.mCamera.getCameraControl().enableTorch(false);
            this.toogleTorch_ImageView.setBackgroundResource(R.drawable.ic_flash_off_black_24dp);
            return;
        }
        this.mCamera.getCameraControl().enableTorch(true);
        this.toogleTorch_ImageView.setBackgroundResource(R.drawable.ic_flash_on_black_24dp);
    }

    public void playVideo() {
        this.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setLooping(true);
                float videoWidth = (((float) mediaPlayer.getVideoWidth()) / ((float) mediaPlayer.getVideoHeight())) / (((float) WaterFallActivity.this.videoView.getWidth()) / ((float) WaterFallActivity.this.videoView.getHeight()));
                if (videoWidth >= 1.0f) {
                    WaterFallActivity.this.videoView.setScaleX(videoWidth);
                } else {
                    WaterFallActivity.this.videoView.setScaleY(1.0f / videoWidth);
                }
            }
        });
        this.videoView.setVideoPath(bitmapToVideoEncoder.getOutputFile().getPath());
        this.videoView.start();
    }

    public Uri saveVideoMediaStore(File file) {
        try {
            ContentResolver contentResolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put("title", file.getName());
            contentValues.put("_display_name", System.currentTimeMillis() + ".mp4");
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

    private void incrementCounter() {
        int i = this.sharedPref.getInt(GOOGLE_AD_COUNTER_PREF, 0);
        SharedPreferences.Editor edit = this.sharedPref.edit();
        if (i < 3) {
            edit.putInt(GOOGLE_AD_COUNTER_PREF, i + 1);
            edit.apply();
        }
    }

    private void resetCounter() {
        SharedPreferences.Editor edit = this.sharedPref.edit();
        edit.putInt(GOOGLE_AD_COUNTER_PREF, 0);
        edit.apply();
    }

    public void stopCapture() {
        bitmapToVideoEncoder.stopEncoding();
        showResultUI();
        hideCaptureUI();
        this.capture = false;
        this.waterfallBitmapList = new ArrayList();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void onResume() {
        this.videoView.start();
        ImageView imageView2 = this.toogleTorch_ImageView;
        if (imageView2.getVisibility() == 0 && this.mCamera.getCameraInfo().hasFlashUnit() && imageView2.getBackground().getConstantState().equals(getResources().getDrawable(R.drawable.ic_flash_on_black_24dp).getConstantState())) {
            this.mCamera.getCameraControl().enableTorch(true);
        }
        super.onResume();
    }

    public void onPause() {
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    private class ImageCapture implements ImageAnalysis.Analyzer {
        /*public *//* synthetic *//* int getTargetCoordinateSystem() {
            return ImageAnalysis.Analyzer.CC.$default$getTargetCoordinateSystem(this);
        }

        public *//* synthetic *//* Size getTargetResolutionOverride() {
            return ImageAnalysis.Analyzer.CC.$default$getTargetResolutionOverride(this);
        }

        public *//* synthetic *//* void updateTransform(Matrix matrix) {
            ImageAnalysis.Analyzer.CC.$default$updateTransform(this, matrix);
        }*/

        private ImageCapture() {
        }

        public void analyze(ImageProxy imageProxy) {
            Bitmap bitmap;
            if (WaterFallActivity.this.previewView.getPreviewStreamState().getValue() == PreviewView.StreamState.STREAMING && WaterFallActivity.this.previewView.getChildAt(0).getClass() == TextureView.class) {
                bitmap = ((TextureView) WaterFallActivity.this.previewView.getChildAt(0)).getBitmap(WaterFallActivity.this.resolutionX, WaterFallActivity.this.resolutionY);
            } else if (imageProxy.getFormat() == 35) {
                WaterFallActivity waterFallActivity = WaterFallActivity.this;
                bitmap = waterFallActivity.rotateBitmap(waterFallActivity.toBitmap(imageProxy.getImage()), 90);
                if (WaterFallActivity.this.facing == 0) {
                    bitmap = WaterFallActivity.MirrorBitmap(bitmap, 1, -1);
                }
            } else {
                bitmap = null;
            }
            if (bitmap == null) {
                imageProxy.close();
                return;
            }
            if (WaterFallActivity.this.capture) {
                if (WaterFallActivity.this.resultBitmap == null) {
                    WaterFallActivity.this.initializeImageView();
                }
                WaterFallActivity.this.subBitmap = Bitmap.createBitmap(bitmap, 0, bitmap.getHeight() / 2, WaterFallActivity.this.resolutionX, WaterFallActivity.this.lineResolution);
                WaterFallActivity waterFallActivity2 = WaterFallActivity.this;
                waterFallActivity2.addWaterfallBitmap(waterFallActivity2.subBitmap);
                WaterFallActivity waterFallActivity3 = WaterFallActivity.this;
                waterFallActivity3.updateWaterfallBitmap(waterFallActivity3.resultBitmap);
                WaterFallActivity waterFallActivity4 = WaterFallActivity.this;
                WaterFallActivity.bitmapToVideoEncoder.queueFrame(waterFallActivity4.overlay(bitmap, waterFallActivity4.resultBitmap));
            }
            WaterFallActivity.this.previewViewImageView.setImageBitmap(bitmap);
            imageProxy.close();
        }
    }
}
