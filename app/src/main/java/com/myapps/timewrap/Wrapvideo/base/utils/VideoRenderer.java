package com.myapps.timewrap.Wrapvideo.base.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.util.SparseIntArray;
import androidx.work.Data;
import com.myapps.timewrap.Wrapvideo.base.WarpVideoView.RecordableSurfaceView;
import com.myapps.timewrap.Wrapvideo.fragments.ScanSettings;
import com.myapps.timewrap.Wrapvideo.fragments.WarpVideoFragment;
import io.reactivex.rxjava3.core.Observable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

public class VideoRenderer implements RecordableSurfaceView.RendererCallbacks, SurfaceTexture.OnFrameAvailableListener {
    private static final int MAX_TEXTURES = 16;
    private static final SparseIntArray ORIENTATIONS;
    private static final String TAG = "VideoRenderer";
    private static short[] drawOrder = {0, 1, 2, 1, 3, 2};
    private static float[] squareCoords = {-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};
    private static float squareSize = 1.0f;
    static int xpos;
    static int ypos;
    private String DEFAULT_FRAGMENT_SHADER = "vid.frag.glsl";
    private String DEFAULT_VERTEX_SHADER = "vid.vert.glsl";
    Bitmap bm2;
    private ShortBuffer drawListBuffer;
    Bitmap f212bm;
    private String fragmentShaderCode;
    private GlOverlayFilter glFilter;
    private float mAspectRatio = 1.0f;
    protected int mCameraShaderProgram;
    private float[] mCameraTransformMatrix = new float[16];
    private WeakReference<Context> mContextWeakReference;
    private String mFragmentShaderPath;
    private boolean mFrameAvailableRegistered = false;
    int mNeedsRefreshCount = 0;
    private OnRendererReadyListener mOnRendererReadyListener;
    private float[] mOrthoMatrix = new float[16];
    Observable<RecordingStatus> mRecordingObservable;
    private int mScreenHeight;
    private int mScreenWidth;
    protected int mSurfaceHeight;
    protected SurfaceTexture mSurfaceTexture;
    protected int mSurfaceWidth;
    private ArrayList<Texture> mTextureArray = new ArrayList<>();
    private int[] mTextureConsts = {33985, 33986, 33987, 33988, 33989, 33990, 33991, 33992, 33993, 33994, 33995, 33996, 33997, 33998, 33999, 34000};
    private int[] mTexturesIds = new int[16];
    private String mVertexShaderPath;
    private int mViewportHeight;
    private int mViewportWidth;
    private WarpVideoFragment mWarpVideoFragment;
    private int positionHandle;
    ScanSettings settings;
    long start = 0;
    private FloatBuffer textureBuffer;
    private int textureCoordinateHandle;
    private float[] textureCoords = {0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f};
    private FloatBuffer vertexBuffer;
    private String vertexShaderCode;

    public interface OnRendererReadyListener {
        void onRendererFinished();

        void onRendererReady();
    }

    private void onPreSetupGLComponents() {
    }

    public void onContextCreated() {
    }

    static {
        SparseIntArray sparseIntArray = new SparseIntArray();
        ORIENTATIONS = sparseIntArray;
        sparseIntArray.append(0, 90);
        sparseIntArray.append(1, 0);
        sparseIntArray.append(2, 270);
        sparseIntArray.append(3, 180);
    }

    public VideoRenderer(Context context) {
        init(context, this.DEFAULT_FRAGMENT_SHADER, this.DEFAULT_VERTEX_SHADER);
    }

    public VideoRenderer(Context context, String str, String str2) {
        init(context, str, str2);
    }

    public void setScreenSize(int i, int i2) {
        this.mScreenHeight = i2;
        this.mScreenWidth = i;
        this.glFilter.setScreenSize(i, i2);
    }

    public void reset() {
        GlOverlayFilter glOverlayFilter = this.glFilter;
        if (glOverlayFilter != null) {
            glOverlayFilter.reset();
        }
    }

    private void init(Context context, String str, String str2) {
        this.mContextWeakReference = new WeakReference<>(context);
        this.mFragmentShaderPath = str;
        this.mVertexShaderPath = str2;
        loadFromShadersFromAssets(str, str2);
    }

    private void loadFromShadersFromAssets(String str, String str2) {
        try {
            this.fragmentShaderCode = ShaderUtils.getStringFromFileInAssets((Context) this.mContextWeakReference.get(), str);
            this.vertexShaderCode = ShaderUtils.getStringFromFileInAssets((Context) this.mContextWeakReference.get(), str2);
        } catch (IOException e) {
            Log.e(TAG, "loadFromShadersFromAssets() failed. Check paths to assets.\n" + e.getMessage());
        }
    }

    public void initGLComponents() {
        onPreSetupGLComponents();
        setupVertexBuffer();
        setupTextures();
        setupCameraTexture();
        setupShaders();
        onSetupComplete();
        createFilter();
        this.glFilter.setup();
    }

    private void createFilter() {
        if (this.glFilter == null) {
            GlOverlayFilter glOverlayFilter = new GlOverlayFilter((Context) this.mContextWeakReference.get());
            this.glFilter = glOverlayFilter;
            glOverlayFilter.setRecordingObservable(this.mRecordingObservable);
        }
    }

    public void deinitGL() {
        deinitGLComponents();
    }

    public void deinitGLComponents() {
        GLES20.glDeleteTextures(16, this.mTexturesIds, 0);
        GLES20.glDeleteProgram(this.mCameraShaderProgram);
    }

    public void setAspectRatio(float f) {
        this.mAspectRatio = f;
    }

    public void setupVertexBuffer() {
        ByteBuffer allocateDirect = ByteBuffer.allocateDirect(drawOrder.length * 2);
        allocateDirect.order(ByteOrder.nativeOrder());
        ShortBuffer asShortBuffer = allocateDirect.asShortBuffer();
        this.drawListBuffer = asShortBuffer;
        asShortBuffer.put(drawOrder);
        this.drawListBuffer.position(0);
        ByteBuffer allocateDirect2 = ByteBuffer.allocateDirect(squareCoords.length * 4);
        allocateDirect2.order(ByteOrder.nativeOrder());
        FloatBuffer asFloatBuffer = allocateDirect2.asFloatBuffer();
        this.vertexBuffer = asFloatBuffer;
        asFloatBuffer.put(squareCoords);
        this.vertexBuffer.position(0);
    }

    public void setupTextures() {
        ByteBuffer allocateDirect = ByteBuffer.allocateDirect(this.textureCoords.length * 4);
        allocateDirect.order(ByteOrder.nativeOrder());
        FloatBuffer asFloatBuffer = allocateDirect.asFloatBuffer();
        this.textureBuffer = asFloatBuffer;
        asFloatBuffer.put(this.textureCoords);
        this.textureBuffer.position(0);
        GLES20.glGenTextures(16, this.mTexturesIds, 0);
        checkGlError("Texture generate");
    }

    public void setupCameraTexture() {
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(36197, this.mTexturesIds[0]);
        checkGlError("Texture bind");
    }

    public int getCameraTexture() {
        int[] iArr = this.mTexturesIds;
        if (iArr == null || iArr.length <= 0) {
            return -1;
        }
        return iArr[0];
    }

    public void setupShaders() {
        int glCreateShader = GLES20.glCreateShader(35633);
        GLES20.glShaderSource(glCreateShader, this.vertexShaderCode);
        GLES20.glCompileShader(glCreateShader);
        checkGlError("Vertex shader compile");
        Log.d(TAG, "vertexShader info log:\n " + GLES20.glGetShaderInfoLog(glCreateShader));
        int glCreateShader2 = GLES20.glCreateShader(35632);
        GLES20.glShaderSource(glCreateShader2, this.fragmentShaderCode);
        GLES20.glCompileShader(glCreateShader2);
        checkGlError("Pixel shader compile");
        Log.d(TAG, "fragmentShader info log:\n " + GLES20.glGetShaderInfoLog(glCreateShader2));
        int glCreateProgram = GLES20.glCreateProgram();
        this.mCameraShaderProgram = glCreateProgram;
        GLES20.glAttachShader(glCreateProgram, glCreateShader);
        GLES20.glAttachShader(this.mCameraShaderProgram, glCreateShader2);
        GLES20.glLinkProgram(this.mCameraShaderProgram);
        checkGlError("Shader program compile");
        int[] iArr = new int[1];
        GLES20.glGetProgramiv(this.mCameraShaderProgram, 35714, iArr, 0);
        if (iArr[0] != 1) {
            String glGetProgramInfoLog = GLES20.glGetProgramInfoLog(this.mCameraShaderProgram);
            Log.e("SurfaceTest", "Error while linking program:\n" + glGetProgramInfoLog);
        }
    }

    public void onSetupComplete() {
        OnRendererReadyListener onRendererReadyListener = this.mOnRendererReadyListener;
        if (onRendererReadyListener != null) {
            onRendererReadyListener.onRendererReady();
        }
    }

    public void shutdown() {
        this.mOnRendererReadyListener.onRendererFinished();
    }

    public void setUniformsAndAttribs() {
        try {
            int glGetUniformLocation = GLES20.glGetUniformLocation(this.mCameraShaderProgram, "camTexture");
            int glGetUniformLocation2 = GLES20.glGetUniformLocation(this.mCameraShaderProgram, "camTextureTransform");
            int glGetUniformLocation3 = GLES20.glGetUniformLocation(this.mCameraShaderProgram, "uPMatrix");
            this.textureCoordinateHandle = GLES20.glGetAttribLocation(this.mCameraShaderProgram, "camTexCoordinate");
            int glGetAttribLocation = GLES20.glGetAttribLocation(this.mCameraShaderProgram, "position");
            this.positionHandle = glGetAttribLocation;
            GLES20.glEnableVertexAttribArray(glGetAttribLocation);
            GLES20.glVertexAttribPointer(this.positionHandle, 2, 5126, false, 8, this.vertexBuffer);
            GLES20.glActiveTexture(33984);
            GLES20.glBindTexture(36197, this.mTexturesIds[0]);
            GLES20.glUniform1i(glGetUniformLocation, 0);
            GLES20.glEnableVertexAttribArray(this.textureCoordinateHandle);
            FloatBuffer floatBuffer = this.textureBuffer;
            if (floatBuffer != null) {
                GLES20.glVertexAttribPointer(this.textureCoordinateHandle, 2, 5126, false, 8, floatBuffer);
            }
            GLES20.glUniformMatrix4fv(glGetUniformLocation2, 1, false, this.mCameraTransformMatrix, 0);
            GLES20.glUniformMatrix4fv(glGetUniformLocation3, 1, false, this.mOrthoMatrix, 0);
            GLES20.glEnable(3042);
            GLES20.glBlendFunc(1, 771);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int addTexture(int i, String str) {
        int i2 = this.mTextureConsts[this.mTextureArray.size()];
        if (this.mTextureArray.size() + 1 < 16) {
            return addTexture(i2, BitmapFactory.decodeResource(((Context) this.mContextWeakReference.get()).getResources(), i), str, true);
        }
        throw new IllegalStateException("Too many textures! Please don't use so many :(");
    }

    public int addTexture(Bitmap bitmap, String str) {
        int i = this.mTextureConsts[this.mTextureArray.size()];
        if (this.mTextureArray.size() + 1 < 16) {
            return addTexture(i, bitmap, str, true);
        }
        throw new IllegalStateException("Too many textures! Please don't use so many :(");
    }

    public int addTexture(int i, Bitmap bitmap, String str, boolean z) {
        int size = this.mTextureArray.size() + 1;
        GLES20.glActiveTexture(i);
        checkGlError("Texture generate");
        GLES20.glBindTexture(3553, this.mTexturesIds[size]);
        checkGlError("Texture bind");
        GLES20.glTexParameterf(3553, 10241, 9728.0f);
        GLES20.glTexParameterf(3553, Data.MAX_DATA_BYTES, 9728.0f);
        GLUtils.texImage2D(3553, 0, bitmap, 0);
        if (z) {
            bitmap.recycle();
        }
        Texture texture = new Texture(size, i, str);
        if (!this.mTextureArray.contains(texture)) {
            this.mTextureArray.add(texture);
            Log.d(TAG, "addedTexture() " + this.mTexturesIds[size] + " : " + texture);
        }
        return size;
    }

    public void updateTexture(int i, Bitmap bitmap) {
        GLES20.glActiveTexture(this.mTextureConsts[i - 1]);
        checkGlError("Texture generate");
        GLES20.glBindTexture(3553, this.mTexturesIds[i]);
        checkGlError("Texture bind");
        GLUtils.texSubImage2D(3553, 0, 0, 0, bitmap);
        checkGlError("Tex Sub Image");
        bitmap.recycle();
    }

    public void setExtraTextures() {
        for (int i = 0; i < this.mTextureArray.size(); i++) {
            Texture texture = this.mTextureArray.get(i);
            int glGetUniformLocation = GLES20.glGetUniformLocation(this.mCameraShaderProgram, texture.uniformName);
            GLES20.glActiveTexture(texture.texId);
            GLES20.glBindTexture(3553, this.mTexturesIds[texture.texNum]);
            GLES20.glUniform1i(glGetUniformLocation, texture.texNum);
        }
    }

    public void drawElements() {
        GLES20.glDrawElements(4, drawOrder.length, 5123, this.drawListBuffer);
        drawBitmap();
    }

    private void drawBitmap() {
        this.glFilter.draw(this.mTexturesIds[0]);
    }

    public void onDrawCleanup() {
        GLES20.glDisableVertexAttribArray(this.positionHandle);
        GLES20.glDisableVertexAttribArray(this.textureCoordinateHandle);
    }

    public void checkGlError(String str) {
        while (true) {
            int glGetError = GLES20.glGetError();
            if (glGetError != 0) {
                Log.e("SurfaceTest", str + ": glError " + GLUtils.getEGLErrorString(glGetError));
            } else {
                return;
            }
        }
    }

    public void setOnRendererReadyListener(OnRendererReadyListener onRendererReadyListener) {
        this.mOnRendererReadyListener = onRendererReadyListener;
    }

    public void setVideoFragment(WarpVideoFragment warpVideoFragment) {
        this.mWarpVideoFragment = warpVideoFragment;
        warpVideoFragment.setPreviewTexture(getCameraTexture());
    }

    public void onSurfaceCreated() {
        deinitGL();
        this.mTextureArray = new ArrayList<>();
        initGLComponents();
    }

    public void onSurfaceChanged(int i, int i2) {
        this.mSurfaceHeight = i2;
        this.mSurfaceWidth = i;
        this.mViewportHeight = i2;
        this.mViewportWidth = i;
        this.mAspectRatio = (((float) i) * 1.0f) / ((float) i2);
        createFilter();
        if (i > 0) {
            this.glFilter.setFrameSize(this.mSurfaceWidth, this.mSurfaceHeight);
        }
    }

    public void onSurfaceDestroyed() {
        deinitGL();
        GlOverlayFilter glOverlayFilter = this.glFilter;
        if (glOverlayFilter != null) {
            glOverlayFilter.dispose();
        }
    }

    public void onPreDrawFrame() {
        SurfaceTexture surfaceTexture = this.mSurfaceTexture;
        if (surfaceTexture != null) {
            if (!this.mFrameAvailableRegistered) {
                surfaceTexture.setOnFrameAvailableListener(this);
                this.mFrameAvailableRegistered = true;
            }
        } else if (this.mWarpVideoFragment.getSurfaceTexture() != null) {
            this.mSurfaceTexture = this.mWarpVideoFragment.getSurfaceTexture();
        } else {
            this.mWarpVideoFragment.setPreviewTexture(getCameraTexture());
        }
    }

    public void onDrawFrame() {
        float[] fArr = this.mOrthoMatrix;
        float f = this.mAspectRatio;
        Matrix.orthoM(fArr, 0, -f, f, -1.0f, 1.0f, -1.0f, 1.0f);
        if (this.mNeedsRefreshCount > 0) {
            for (int i = 0; i < this.mNeedsRefreshCount; i++) {
                this.mSurfaceTexture.updateTexImage();
                this.mSurfaceTexture.getTransformMatrix(this.mCameraTransformMatrix);
                this.mNeedsRefreshCount--;
            }
        }
        GLES20.glViewport(0, 0, this.mViewportWidth, this.mViewportHeight);
        GLES20.glClearColor(0.329412f, 0.329412f, 0.329412f, 0.0f);
        GLES20.glClear(16384);
        GLES20.glUseProgram(this.mCameraShaderProgram);
        setUniformsAndAttribs();
        setExtraTextures();
        drawElements();
        onDrawCleanup();
    }

    private void drawBox() {
        int i = ypos + 4;
        ypos = i;
        if (i > this.mViewportHeight) {
            ypos = 0;
        }
        GLES20.glEnable(3089);
        GLES20.glScissor(xpos, ypos, this.mViewportWidth, 12);
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(16384);
        GLES20.glDisable(3089);
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        this.mSurfaceTexture = surfaceTexture;
        surfaceTexture.setOnFrameAvailableListener(this);
    }

    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.d("myApp", "onFrameAvailable " + (System.currentTimeMillis() - this.start));
        this.start = System.currentTimeMillis();
        this.mNeedsRefreshCount = this.mNeedsRefreshCount + 1;
        if (this.mSurfaceTexture == null && this.mWarpVideoFragment.getSurfaceTexture() != null) {
            this.mSurfaceTexture = this.mWarpVideoFragment.getSurfaceTexture();
        }
    }

    public void setCurrentImage(Bitmap bitmap) {
        this.glFilter.setBitmap(bitmap);
    }

    public void setRecordingObservable(Observable<RecordingStatus> observable) {
        this.mRecordingObservable = observable;
        GlOverlayFilter glOverlayFilter = this.glFilter;
        if (glOverlayFilter != null) {
            glOverlayFilter.setRecordingObservable(observable);
            this.glFilter.setScanSettings(this.settings);
        }
    }

    public void setScanSettings(ScanSettings scanSettings) {
        this.settings = scanSettings;
    }

    private class Texture {
        public int texId;
        public int texNum;
        public String uniformName;

        private Texture(int i, int i2, String str) {
            this.texNum = i;
            this.texId = i2;
            this.uniformName = str;
        }

        public String toString() {
            return "[Texture] num: " + this.texNum + " id: " + this.texId + ", uniformName: " + this.uniformName;
        }
    }
}
