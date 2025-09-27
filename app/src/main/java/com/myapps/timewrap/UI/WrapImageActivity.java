package com.myapps.timewrap.UI;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.YuvImage;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Size;
import android.view.Display;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.PointerIconCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;
import com.myapps.timewrap.R;
import com.myapps.timewrap.Utils.C1197util;
import com.myapps.timewrap.Utils.OnSwipeTouchListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.ExecutionException;

public class WrapImageActivity extends AppCompatActivity implements CameraXConfig.Provider {
    private static final String MEDIA_FOLDER = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + File.separator + "TIME WARP SCAN" + File.separator);
    private static final String TIME_WARP_SCAN_PREFS = "TIME_WARP_SCAN_PREFS";
    Timer GIFTimer = null;
    public int REQUEST_CODE_PERMISSIONS = PointerIconCompat.TYPE_CONTEXT_MENU;
    ConstraintLayout beforeCaptureUI = null;
    ProcessCameraProvider cameraProvider;
    public ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    CameraSelector cameraSelector;
    boolean capture = false;
    CAPTURE_MODE captureMode = CAPTURE_MODE.PHOTO;
    ConstraintLayout captureUI = null;
    int facing = 0;
    Uri fileURI = null;
    Button firebaseButton = null;
    int frameRate = 2;
    ImageAnalysis imageAnalysis;
    ImageView imageView = null;
    boolean isSwitching = false;
    int lineCount = 0;
    int lineResolution = 50;
    Camera mCamera;
    Preview preview;
    public PreviewView previewView;
    ImageView previewViewImageView = null;
    int resolutionX = 480;
    int resolutionY = 640;
    Bitmap resultBitmap = null;
    ConstraintLayout resultUI = null;
    SharedPreferences sharedPref = null;
    Bitmap subBitmap = null;
    ConstraintLayout tutorialUI = null;
    public WARP_DIRECTION warpDirection = WARP_DIRECTION.DOWN;

    enum CAPTURE_MODE {
        PHOTO,
        GIF
    }

    enum WARP_DIRECTION {
        DOWN,
        RIGHT,
        LEFT
    }

    private void hideResultUI() {
    }

    public void getAdvertise() {
    }

    public static Bitmap MirrorBitmap(Bitmap bitmap, int i, int i2) {
        Matrix matrix = new Matrix();
        matrix.preScale((float) i, (float) i2);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().setFlags(1024, 1024);
        setContentView((int) R.layout.activity_wrap_image);


        PermissionAllow.GetPermission(this);


        File file = new File(MEDIA_FOLDER);
        if (!file.exists()) {
            file.mkdirs();
            sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", Uri.fromFile(file)));
        }


        PreviewView previewView2 = (PreviewView) findViewById(R.id.previewView);
        this.previewView = previewView2;
        previewView2.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        this.previewViewImageView = (ImageView) findViewById(R.id.previewView_ImageView);
        this.cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        ImageAnalysis build = new ImageAnalysis.Builder().setTargetResolution(new Size(480, 640)).setBackpressureStrategy(0).build();
        this.imageAnalysis = build;
        build.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageCapture());
        this.cameraProviderFuture.addListener(new Runnable() {
            public final void run() {
                try {
                    WrapImageActivity wrapImageActivity = WrapImageActivity.this;
                    wrapImageActivity.cameraProvider = (ProcessCameraProvider) wrapImageActivity.cameraProviderFuture.get();
                    WrapImageActivity wrapImageActivity2 = WrapImageActivity.this;
                    wrapImageActivity2.bindPreview(wrapImageActivity2.cameraProvider);
                    /*if (WrapImageActivity.this.allPermissionsGranted()) {
                        WrapImageActivity wrapImageActivity2 = WrapImageActivity.this;
                        wrapImageActivity2.bindPreview(wrapImageActivity2.cameraProvider);
                        return;
                    }
                    WrapImageActivity wrapImageActivity3 = WrapImageActivity.this;
                    ActivityCompat.requestPermissions(wrapImageActivity3, wrapImageActivity3.REQUIRED_PERMISSIONS, WrapImageActivity.this.REQUEST_CODE_PERMISSIONS);*/
                } catch (InterruptedException | ExecutionException unused) {
                }
            }
        }, ContextCompat.getMainExecutor(this));
        ((Button) findViewById(R.id.FIREBASE_TEST_BUTTON1)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {
                    WrapImageActivity.this.hideTutorialUI();
                    WrapImageActivity wrapImageActivity = WrapImageActivity.this;
                    wrapImageActivity.startCapture(wrapImageActivity.mCamera, WARP_DIRECTION.RIGHT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        ((Button) findViewById(R.id.FIREBASE_TEST_BUTTON2)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {
                    WrapImageActivity.this.hideTutorialUI();
                    WrapImageActivity wrapImageActivity = WrapImageActivity.this;
                    wrapImageActivity.startCapture(wrapImageActivity.mCamera, WARP_DIRECTION.DOWN);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        this.beforeCaptureUI = (ConstraintLayout) findViewById(R.id.before_capture_UI);
        this.captureUI = (ConstraintLayout) findViewById(R.id.capture_UI);
        this.resultUI = (ConstraintLayout) findViewById(R.id.result_UI);
        this.tutorialUI = (ConstraintLayout) findViewById(R.id.tutorial_UI);
        ImageView imageView2 = (ImageView) findViewById(R.id.result_imageView);
        this.imageView = imageView2;
        imageView2.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeLeft() throws InterruptedException {
            }

            public void onSwipeTop() {
            }

            public void onSwipeRight() throws InterruptedException {
                if (WrapImageActivity.this.beforeCaptureUI.getVisibility() == 0) {
                    WrapImageActivity.this.hideTutorialUI();
                    WrapImageActivity wrapImageActivity = WrapImageActivity.this;
                    wrapImageActivity.startCapture(wrapImageActivity.mCamera, WARP_DIRECTION.RIGHT);
                }
            }

            public void onSwipeBottom() throws InterruptedException {
                if (WrapImageActivity.this.beforeCaptureUI.getVisibility() == 0) {
                    WrapImageActivity.this.hideTutorialUI();
                    WrapImageActivity wrapImageActivity = WrapImageActivity.this;
                    wrapImageActivity.startCapture(wrapImageActivity.mCamera, WARP_DIRECTION.DOWN);
                }
            }
        });
        ((Button) findViewById(R.id.resultCancel_Button)).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                WrapImageActivity.this.resumeToBeforeCaptureUI();
                WrapImageActivity.this.getAdvertise();
            }
        });
        ((ImageView) findViewById(R.id.switchCamera_ImageView)).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                WrapImageActivity.this.isSwitching = true;
                if (WrapImageActivity.this.facing == 0) {
                    WrapImageActivity.this.setFacing(1);
                } else {
                    WrapImageActivity.this.setFacing(0);
                }
                WrapImageActivity wrapImageActivity = WrapImageActivity.this;
                wrapImageActivity.bindPreview(wrapImageActivity.cameraProvider);
                WrapImageActivity.this.isSwitching = false;
            }
        });
        showTutorialUI();
        showTutorial();
        this.resolutionY = 640;
        this.resolutionX = 480;
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
        if (i == this.REQUEST_CODE_PERMISSIONS) {
            bindPreview(this.cameraProvider);
            /*if (allPermissionsGranted()) {
                bindPreview(this.cameraProvider);
                return;
            }
            Toast.makeText(this, "Permissions not granted by the user.", 0).show();
            finish();*/
        }
    }

    public void setFacing(int i) {
        this.facing = i;
    }

    public Bitmap overlay(Bitmap bitmap, Bitmap bitmap2, int i, WARP_DIRECTION warp_direction) {
        new Matrix().preScale(1.0f, -1.0f);
        Bitmap createBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(createBitmap);
        Paint paint = null;
        canvas.drawBitmap(bitmap, new Matrix(), paint);
        if (warp_direction == WARP_DIRECTION.DOWN) {
            canvas.drawBitmap(bitmap2, 0.0f, (float) i, paint);
        }
        if (warp_direction == WARP_DIRECTION.RIGHT) {
            canvas.drawBitmap(bitmap2, (float) i, 0.0f, paint);
        }
        return createBitmap;
    }

    public Bitmap overlay(Bitmap bitmap, Bitmap bitmap2) {
        new Matrix().preScale(1.0f, -1.0f);
        Bitmap createBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(createBitmap);
        new Paint().setColor(-1);
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

    public void drawScanEffect(List<Bitmap> list, WARP_DIRECTION warp_direction) {
        for (int i = 0; i < list.size(); i++) {
            Canvas canvas = new Canvas(list.get(i));
            Paint paint = new Paint();
            paint.setStrokeWidth(10.0f);
            paint.setColor(ContextCompat.getColor(this, R.color.colorAccent));
            if (warp_direction == WARP_DIRECTION.DOWN) {
                canvas.drawLine(0.0f, (float) (this.lineResolution * i), (float) list.get(0).getWidth(), (float) (this.lineResolution * i), paint);
            } else if (warp_direction == WARP_DIRECTION.RIGHT) {
                float f = (float) (this.lineResolution * i);
                canvas.drawLine(f, 0.0f, f, (float) list.get(0).getHeight(), paint);
            }
            canvas.drawBitmap(list.get(i), 0.0f, 0.0f, (Paint) null);
        }
    }

    public void drawScanEffect(Bitmap bitmap, WARP_DIRECTION warp_direction, int i) {
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setStrokeWidth(10.0f);
        paint.setColor(ContextCompat.getColor(this, R.color.colorAccent));
        if (warp_direction == WARP_DIRECTION.DOWN) {
            float f = (float) (i + 5);
            canvas.drawLine(0.0f, f, (float) bitmap.getWidth(), f, paint);
        } else if (warp_direction == WARP_DIRECTION.RIGHT) {
            float f2 = (float) (i + 5);
            canvas.drawLine(f2, 0.0f, f2, (float) bitmap.getHeight(), paint);
        }
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
    }

    public void drawWaterMark(Bitmap bitmap, int i) {
        Canvas canvas = new Canvas(bitmap);
        if (i == 1) {
            canvas.scale(-1.0f, 1.0f, (float) (canvas.getWidth() / 2), (float) (canvas.getHeight() / 2));
        }
        Paint paint = new Paint();
        paint.setColor(-1);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        paint.setTextSize(15.0f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, 1));
        canvas.drawText("made with", paint.getTextSize(), paint.getTextSize() * 2.0f, paint);
        canvas.drawText("TIME WARP SCAN", paint.getTextSize(), (paint.getTextSize() * 3.0f) + 0.0f, paint);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
    }

    public void initializeImageView() {
        Bitmap createBitmap = Bitmap.createBitmap(this.resolutionX, this.resolutionY, Bitmap.Config.ARGB_8888);
        this.resultBitmap = createBitmap;
        createBitmap.eraseColor(0);
    }

    private Uri saveBitmapInGalary(Bitmap bitmap) {
        String file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
        File file2 = new File(file + "/" + getResources().getString(R.string.app_name) + File.separator + "WarpImage");
        file2.mkdirs();
        int nextInt = new Random().nextInt(10000);
        File file3 = new File(file2, "Image-" + nextInt + ".jpg");
        if (file3.exists()) {
            file3.delete();
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file3);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Uri.fromFile(file3);
    }

    private void showBeforeCaptureUI() {
        this.beforeCaptureUI.setVisibility(0);
    }

    private void hideBeforeCaptureUI() {
        this.beforeCaptureUI.setVisibility(4);
    }

    private void showCaptureUI() {
        this.captureUI.setVisibility(0);
    }

    private void hideCaptureUI() {
        this.captureUI.setVisibility(4);
    }

    private void showTutorialUI() {
        this.tutorialUI.setVisibility(0);
    }

    public void hideTutorialUI() {
        this.tutorialUI.setVisibility(4);
    }

    public void shareFile(String str, Uri uri) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.SEND");
        if (this.captureMode == CAPTURE_MODE.PHOTO) {
            intent.setType("image/jpeg");
        }
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
        if (this.captureMode == CAPTURE_MODE.PHOTO) {
            intent.setType("image/jpeg");
        }
        startActivity(Intent.createChooser(intent, "Send Image using "));
    }

    private boolean checkAppInstall(String str) {
        try {
            getPackageManager().getPackageInfo(str, 1);
            return true;
        } catch (PackageManager.NameNotFoundException unused) {
            return false;
        }
    }

    public int getRandomColor() {
        Random random = new Random();
        return Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    public Drawable getTintedDrawable(Resources resources, int i) {
        Drawable drawable = resources.getDrawable(i);
        drawable.setColorFilter(getRandomColor(), PorterDuff.Mode.MULTIPLY);
        return drawable;
    }

    public void resumeToBeforeCaptureUI() {
        Timer timer = this.GIFTimer;
        if (timer != null) {
            timer.cancel();
        }
        this.lineCount = 0;
        this.resultBitmap = null;
        initializeImageView();
        hideResultUI();
        hideCaptureUI();
        showBeforeCaptureUI();
        showTutorialUI();
        this.capture = false;
    }

    public void startCapture(Camera camera, WARP_DIRECTION warp_direction) throws InterruptedException {
        this.warpDirection = warp_direction;
        this.lineResolution = 2;
        this.lineCount = 0;
        this.resultBitmap = null;
        initializeImageView();
        hideResultUI();
        hideBeforeCaptureUI();
        showCaptureUI();
        this.capture = true;
    }

    private void showTutorial() {
        TextView textView = (TextView) findViewById(R.id.tutorial_TextView);
        final ImageView imageView2 = (ImageView) findViewById(R.id.tutorialHandDown_ImageView);
        final ImageView imageView3 = (ImageView) findViewById(R.id.tutorialHandRight_ImageView);
        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getSize(point);
        int i = point.x;
        int i2 = point.y;
        final ObjectAnimator ofFloat = ObjectAnimator.ofFloat(imageView3, "translationX", new float[]{(float) (i / 3)});
        ofFloat.setDuration(1000);
        ofFloat.setRepeatMode(1);
        ofFloat.start();
        final ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(imageView2, "translationY", new float[]{(float) (i2 / 3)});
        ofFloat2.setDuration(1000);
        ofFloat2.setRepeatMode(1);
        ofFloat.addListener(new Animator.AnimatorListener() {
            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
                imageView2.setVisibility(4);
            }

            public void onAnimationEnd(Animator animator) {
                if (WrapImageActivity.this.tutorialUI.getVisibility() == 0) {
                    imageView2.setVisibility(0);
                    ofFloat2.start();
                }
            }
        });
        ofFloat2.addListener(new Animator.AnimatorListener() {
            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
                imageView3.setVisibility(4);
            }

            public void onAnimationEnd(Animator animator) {
                if (WrapImageActivity.this.tutorialUI.getVisibility() == 0) {
                    imageView3.setVisibility(0);
                    ofFloat.start();
                }
            }
        });
    }

    public void showProVersionAlertDialog() {
        getPackageName();
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
        builder.setMessage("");
        builder.setTitle("Are you tired of those annoying and stupid ads and watermarks? ");
        builder.setCancelable(false);
        builder.setNegativeButton("Im Okay with this version.", new DialogInterface.OnClickListener() {
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
        this.preview = new Preview.Builder().setTargetResolution(new Size(480, 640)).build();
        this.cameraSelector = new CameraSelector.Builder().requireLensFacing(this.facing).build();
        ImageAnalysis build = new ImageAnalysis.Builder().setTargetResolution(new Size(480, 640)).setBackpressureStrategy(0).build();
        this.imageAnalysis = build;
        build.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageCapture());
        this.preview.setSurfaceProvider(this.previewView.getSurfaceProvider());
        this.mCamera = processCameraProvider.bindToLifecycle((LifecycleOwner) this, this.cameraSelector, this.preview);
        processCameraProvider.bindToLifecycle((LifecycleOwner) this, this.cameraSelector, this.imageAnalysis, this.preview);
    }

    public void stopCapture() {
        if ((this.lineCount == this.resolutionY && this.warpDirection == WARP_DIRECTION.DOWN) || (this.lineCount == this.resolutionX && this.warpDirection == WARP_DIRECTION.RIGHT)) {
            C1197util.bitmap = this.resultBitmap;
            startActivity(new Intent(this, WrapImageShareActivity.class));
            finish();
            resumeToBeforeCaptureUI();
        } else {
            hideCaptureUI();
        }
        this.capture = false;
    }

    public void onBackPressed() {
        if (this.beforeCaptureUI.getVisibility() == 0) {
            finish();
        } else if (this.captureUI.getVisibility() == 0) {
            resumeToBeforeCaptureUI();
        } else if (this.resultUI.getVisibility() == 0) {
            resumeToBeforeCaptureUI();
        }
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
            if (WrapImageActivity.this.previewView.getPreviewStreamState().getValue() == PreviewView.StreamState.STREAMING && WrapImageActivity.this.previewView.getChildAt(0).getClass() == TextureView.class) {
                bitmap = ((TextureView) WrapImageActivity.this.previewView.getChildAt(0)).getBitmap(WrapImageActivity.this.resolutionX, WrapImageActivity.this.resolutionY);
            } else if (imageProxy.getFormat() == 35) {
                WrapImageActivity wrapImageActivity = WrapImageActivity.this;
                bitmap = wrapImageActivity.rotateBitmap(wrapImageActivity.toBitmap(imageProxy.getImage()), 90);
                if (WrapImageActivity.this.facing == 0) {
                    bitmap = WrapImageActivity.MirrorBitmap(bitmap, 1, -1);
                }
            } else {
                bitmap = null;
            }
            if (bitmap == null) {
                imageProxy.close();
                return;
            }
            if ((WrapImageActivity.this.lineCount >= WrapImageActivity.this.resolutionY || WrapImageActivity.this.warpDirection != WARP_DIRECTION.DOWN) && !((WrapImageActivity.this.lineCount < WrapImageActivity.this.resolutionX && WrapImageActivity.this.warpDirection == WARP_DIRECTION.RIGHT && WrapImageActivity.this.facing == 0) || (WrapImageActivity.this.lineCount < WrapImageActivity.this.resolutionX && WrapImageActivity.this.warpDirection == WARP_DIRECTION.RIGHT && WrapImageActivity.this.facing == 1))) {
                if (WrapImageActivity.this.capture) {
                    WrapImageActivity.this.stopCapture();
                }
            } else if (WrapImageActivity.this.capture) {
                long currentTimeMillis = System.currentTimeMillis();
                if (WrapImageActivity.this.resultBitmap == null) {
                    WrapImageActivity.this.initializeImageView();
                }
                if (WrapImageActivity.this.warpDirection == WARP_DIRECTION.DOWN) {
                    WrapImageActivity wrapImageActivity2 = WrapImageActivity.this;
                    wrapImageActivity2.subBitmap = Bitmap.createBitmap(bitmap, 0, wrapImageActivity2.lineCount, WrapImageActivity.this.resolutionX, WrapImageActivity.this.lineResolution);
                } else if (WrapImageActivity.this.warpDirection == WARP_DIRECTION.RIGHT) {
                    WrapImageActivity wrapImageActivity3 = WrapImageActivity.this;
                    wrapImageActivity3.subBitmap = Bitmap.createBitmap(bitmap, wrapImageActivity3.lineCount, 0, WrapImageActivity.this.lineResolution, WrapImageActivity.this.resolutionY);
                }
                WrapImageActivity wrapImageActivity4 = WrapImageActivity.this;
                wrapImageActivity4.resultBitmap = wrapImageActivity4.overlay(wrapImageActivity4.resultBitmap, WrapImageActivity.this.subBitmap, WrapImageActivity.this.lineCount, WrapImageActivity.this.warpDirection);
                WrapImageActivity.this.imageView.setImageBitmap(WrapImageActivity.this.resultBitmap);
                WrapImageActivity wrapImageActivity5 = WrapImageActivity.this;
                wrapImageActivity5.drawScanEffect(bitmap, wrapImageActivity5.warpDirection, WrapImageActivity.this.lineCount);
                WrapImageActivity.this.lineCount += WrapImageActivity.this.lineResolution;
                long currentTimeMillis2 = currentTimeMillis - System.currentTimeMillis();
                if (currentTimeMillis2 < ((long) WrapImageActivity.this.frameRate)) {
                    try {
                        Thread.sleep(((long) WrapImageActivity.this.frameRate) - currentTimeMillis2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            WrapImageActivity.this.previewViewImageView.setImageBitmap(bitmap);
            imageProxy.close();
        }
    }
}
