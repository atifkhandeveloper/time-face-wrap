package com.myapps.timewrap.Wrapvideo.base.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import androidx.core.internal.view.SupportMenu;
import androidx.work.Data;
import com.myapps.timewrap.R;
import com.myapps.timewrap.Wrapvideo.base.WarpVideoView.HorizontalSlicer;
import com.myapps.timewrap.Wrapvideo.base.WarpVideoView.Slicer;
import com.myapps.timewrap.Wrapvideo.filters.FilterFactory;
import com.myapps.timewrap.Wrapvideo.filters.IImageFilter;
import com.myapps.timewrap.Wrapvideo.fragments.ScanSettings;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

public class GlOverlayFilter extends GlFilter implements Disposable {
    private static final String FRAGMENT_SHADER = "precision mediump float;\nvarying vec2 vTextureCoord;\nuniform lowp sampler2D sTexture;\nuniform lowp sampler2D oTexture;\nvoid main() {\n   lowp vec4 textureColor = texture2D(sTexture, vTextureCoord);\n   lowp vec4 textureColor2 = texture2D(oTexture, vTextureCoord);\n   \n   gl_FragColor = mix(textureColor, textureColor2, textureColor2.a);\n}\n";
    private Bitmap bitmap = null;
    IImageFilter filter;
    FilterFactory filterManager;
    private Bitmap finalBitmap = null;
    boolean imageSaved = false;
    protected Size inputResolution = null;
    boolean isScrollring = false;
    Matrix lastTransformationMatrix;
    ReentrantLock lock = new ReentrantLock();
    Context mContext;
    private int mPreviewHeight;
    private int mPreviewWidth;
    private int mScreenHeight;
    private int mScreenWidth;
    Path path = new Path();
    Bitmap result;
    boolean saveImageSetting = false;
    ScanSettings settings;
    Slicer slicer;
    CompositeDisposable subsriptions = new CompositeDisposable();
    private int[] textures = new int[1];

    public boolean isDisposed() {
        return false;
    }

    public GlOverlayFilter(Context context) {
        super("attribute highp vec4 aPosition;\nattribute highp vec4 aTextureCoord;\nvarying highp vec2 vTextureCoord;\nvoid main() {\ngl_Position = aPosition;\nvTextureCoord = aTextureCoord.xy;\n}\n", FRAGMENT_SHADER);
        this.mContext = context;
        this.bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }

    public static void releaseBitmap(Bitmap bitmap2) {
        if (bitmap2 != null && !bitmap2.isRecycled()) {
            bitmap2.recycle();
        }
    }

    public void setBitmap(Bitmap bitmap2) {
        overlay(bitmap2);
    }

    public void setRecordingObservable(Observable<RecordingStatus> observable) {
        if (observable != null) {
            this.subsriptions.add(observable.subscribe(new Consumer() {
                public final void accept(Object obj) {
                    RecordingStatus recordingStatus = (RecordingStatus) obj;
                    if (recordingStatus == RecordingStatus.Start) {
                        GlOverlayFilter.this.applySettings();
                        GlOverlayFilter.this.isScrollring = true;
                    } else if (recordingStatus == RecordingStatus.Pause) {
                        GlOverlayFilter.this.isScrollring = false;
                    } else if (recordingStatus == RecordingStatus.Restart) {
                        GlOverlayFilter.this.isScrollring = true;
                    } else {
                        GlOverlayFilter.this.isScrollring = false;
                        GlOverlayFilter.this.result = null;
                        GlOverlayFilter.this.reset();
                    }
                }
            }, new Consumer<Throwable>() {
                public void accept(Throwable th) throws Throwable {
                }
            }));
        }
    }

    public void setScanSettings(ScanSettings scanSettings) {
        this.settings = scanSettings;
        createFilter();
    }

    public void setResolution(Size size) {
        this.inputResolution = size;
    }

    public void setFrameSize(int i, int i2) {
        super.setFrameSize(i, i2);
        this.mScreenWidth = i;
        this.mScreenHeight = i2;
        releaseBitmap(this.bitmap);
        this.bitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
        this.slicer = new HorizontalSlicer(this.mScreenWidth, this.mScreenHeight, 1, SupportMenu.CATEGORY_MASK);
    }

    public void setScreenSize(int i, int i2) {
        setResolution(new Size(i, i2));
        createBitmap();
    }

    private void createBitmap() {
        this.mPreviewHeight = this.inputResolution.getHeight();
        this.mPreviewWidth = this.inputResolution.getWidth();
        updateTransformationIfNeeded();
        this.finalBitmap = Bitmap.createBitmap(this.mPreviewWidth, this.mPreviewHeight, Bitmap.Config.ARGB_8888);
        this.imageSaved = false;
    }

    public void setup() {
        super.setup();
        GLES20.glGenTextures(1, this.textures, 0);
        GLES20.glBindTexture(3553, this.textures[0]);
        GLES20.glTexParameteri(3553, 10241, 9729);
        GLES20.glTexParameteri(3553, Data.MAX_DATA_BYTES, 9729);
        GLES20.glTexParameteri(3553, 10242, 33071);
        GLES20.glTexParameteri(3553, 10243, 33071);
    }

    public void onDraw() {
        try {
            Bitmap bitmap2 = this.bitmap;
            if (bitmap2 != null && !bitmap2.isRecycled()) {
                this.bitmap.eraseColor(Color.argb(0, 0, 0, 0));
                Canvas canvas = new Canvas(this.bitmap);
                canvas.scale(1.0f, -1.0f, (float) (canvas.getWidth() / 2), (float) (canvas.getHeight() / 2));
                canvas.concat(this.lastTransformationMatrix);
                drawCanvas(canvas);
                int handle = getHandle("oTexture");
                GLES20.glActiveTexture(33987);
                GLES20.glBindTexture(3553, this.textures[0]);
                GLUtils.texImage2D(3553, 0, 6408, this.bitmap, 0);
                GLES20.glUniform1i(handle, 3);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void overlay(Bitmap bitmap2) {
        if (this.isScrollring && bitmap2 != null) {
            System.currentTimeMillis();
            this.lock.lock();
            try {
                if (!this.slicer.isScanDone()) {
                    this.slicer.drawSlice(bitmap2, this.finalBitmap, this.filter);
                    this.result = this.finalBitmap;
                    if (!this.filter.isBackgorundFilter()) {
                        bitmap2.recycle();
                    }
                } else if (this.saveImageSetting) {
                    saveImage(this.result);
                }
            } finally {
                this.lock.unlock();
            }
        }
    }

    public void drawCanvas(Canvas canvas) {
        if (this.result != null) {
            System.currentTimeMillis();
            this.lock.lock();
            try {
                canvas.drawBitmap(this.result, 0.0f, 0.0f, (Paint) null);
                if (!this.slicer.isScanDone()) {
                    this.slicer.drawScanner(canvas);
                }
            } finally {
                this.lock.unlock();
            }
        }
    }

    public void reset() {
        this.bitmap.eraseColor(0);
        createBitmap();
    }

    public void applySettings() {
        int i;
        if (this.settings == null) {
            this.settings = new ScanSettings();
        }
        int i2 = this.mPreviewWidth;
        if (i2 != 0 && (i = this.mPreviewHeight) != 0) {
            this.slicer = Slicer.createSlicer(this.settings, i2, i);
            this.filter.setDirection(this.settings.getDirection());
            this.saveImageSetting = this.settings.isSaveImage();
        }
    }

    private void createFilter() {
        if (this.settings == null) {
            this.settings = new ScanSettings();
        }
        FilterFactory filterFactory = new FilterFactory();
        this.filterManager = filterFactory;
        this.filter = filterFactory.createFilter(this.mContext, this.settings.getFilter());
    }

    private void updateTransformationIfNeeded() {
        Matrix matrix = new Matrix();
        this.lastTransformationMatrix = matrix;
        matrix.setScale((((float) this.mScreenWidth) * 1.0f) / ((float) this.mPreviewWidth), (((float) this.mScreenHeight) * 1.0f) / ((float) this.mPreviewHeight));
    }

    public void dispose() {
        CompositeDisposable compositeDisposable = this.subsriptions;
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
        releaseBitmap(this.bitmap);
        releaseBitmap(this.finalBitmap);
        releaseBitmap(this.result);
    }

    private void saveImage(Bitmap bitmap2) {
        if (bitmap2 != null) {
            try {
                if (!bitmap2.isRecycled() && !this.imageSaved && !this.filter.isBackgorundFilter() && !this.filter.isFakeFilter()) {
                    this.imageSaved = true;
                    Bitmap copy = bitmap2.copy(bitmap2.getConfig(), true);
                    new SaveImageTask().execute(new Bitmap[]{copy});
                }
            } catch (Exception unused) {
            }
        }
    }

    private class SaveImageTask extends AsyncTask<Bitmap, Void, Void> {
        private SaveImageTask() {
        }

        public Void doInBackground(Bitmap... bitmapArr) {
            try {
                if (bitmapArr.length > 0 && bitmapArr[0] == null) {
                    return null;
                }
                ContentResolver contentResolver = GlOverlayFilter.this.mContext.getContentResolver();
                String str = "TF_" + new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date()) + ".jpg";
                ContentValues contentValues = new ContentValues();
                contentValues.put("_display_name", str);
                contentValues.put("mime_type", "image/jpg");
                if (Build.VERSION.SDK_INT >= 29) {
                    contentValues.put("relative_path", Environment.DIRECTORY_DCIM + File.separator + GlOverlayFilter.this.mContext.getString(R.string.app_name) + File.separator + "WarpImage");
                } else {
                    File externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                    if (!externalStoragePublicDirectory.exists()) {
                        externalStoragePublicDirectory.mkdirs();
                    }
                    contentValues.put("_data", new File(externalStoragePublicDirectory, str).getAbsolutePath());
                }
                Uri insert = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (insert != null) {
                    OutputStream openOutputStream = contentResolver.openOutputStream(insert);
                    bitmapArr[0].compress(Bitmap.CompressFormat.JPEG, 100, openOutputStream);
                    openOutputStream.flush();
                    openOutputStream.close();
                }
                return null;
            } catch (IOException e) {
                Log.d("myApp", "Image saved!" + e.getMessage());
                e.printStackTrace();
                return null;
            } catch (Exception e2) {
                Log.d("myApp", "Image saved!" + e2.getMessage());
                e2.printStackTrace();
                return null;
            }
        }
    }
}
