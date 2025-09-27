package com.myapps.timewrap.Wrapvideo.base.WarpVideoView;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.File;
import java.io.IOException;
import java.lang.Thread;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RecordableSurfaceView extends SurfaceView {
    public static int RENDERMODE_CONTINUOUSLY = 1;
    public static int RENDERMODE_WHEN_DIRTY = 0;
    private static final String TAG = "RecordableSurfaceView";
    private ARRenderThread mARRenderThread;
    public int mDesiredHeight = 0;
    public int mDesiredWidth = 0;
    public AtomicBoolean mHasGLContext = new AtomicBoolean(false);
    public int mHeight = 0;
    public AtomicBoolean mIsRecording = new AtomicBoolean(false);
    private MediaRecorder mMediaRecorder;
    public boolean mPaused = false;
    public AtomicInteger mRenderMode = new AtomicInteger(RENDERMODE_CONTINUOUSLY);
    public AtomicBoolean mRenderRequested = new AtomicBoolean(false);
    public WeakReference<RendererCallbacks> mRendererCallbacksWeakReference;
    public AtomicBoolean mSizeChange = new AtomicBoolean(false);
    public Surface mSurface;
    public int mWidth = 0;

    public interface RendererCallbacks {
        void onContextCreated();

        void onDrawFrame();

        void onPreDrawFrame();

        void onSurfaceChanged(int i, int i2);

        void onSurfaceCreated();

        void onSurfaceDestroyed();
    }

    public RecordableSurfaceView(Context context) {
        super(context);
    }

    public RecordableSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public RecordableSurfaceView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public RecordableSurfaceView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public void doSetup() {
        if (!this.mHasGLContext.get()) {
            Surface surface = this.mSurface;
            if (surface == null || !surface.isValid()) {
                this.mSurface = setPersistentInputSurface();
            }
            this.mARRenderThread = new ARRenderThread();
        }
        getHolder().addCallback(this.mARRenderThread);
        if (getHolder().getSurface().isValid()) {
            this.mARRenderThread.surfaceCreated((SurfaceHolder) null);
        }
        this.mPaused = true;
    }

    private Surface setPersistentInputSurface() {
        return MediaCodec.createPersistentInputSurface();
    }

    public void pause() {
        this.mPaused = true;
    }

    public void resume() {
        doSetup();
        this.mPaused = false;
    }

    public void stop() {
        this.mPaused = true;
    }

    public int getRenderMode() {
        return this.mRenderMode.get();
    }

    public void setRenderMode(int i) {
        this.mRenderMode.set(i);
    }

    public void requestRender() {
        this.mRenderRequested.set(true);
    }

    public void initRecorder(File file, int i, int i2, MediaRecorder.OnErrorListener onErrorListener, MediaRecorder.OnInfoListener onInfoListener) throws IOException {
        initRecorder(file, i, i2, i, i2, 0, onErrorListener, onInfoListener);
    }

    public void initRecorder(File file, int i, int i2, int i3, MediaRecorder.OnErrorListener onErrorListener, MediaRecorder.OnInfoListener onInfoListener) throws IOException {
        initRecorder(file, i, i2, i, i2, i3, onErrorListener, onInfoListener);
    }

    public void initRecorder(File file, int i, int i2, int i3, int i4, int i5, MediaRecorder.OnErrorListener onErrorListener, MediaRecorder.OnInfoListener onInfoListener) throws IOException {
        MediaRecorder mediaRecorder = new MediaRecorder();
        mediaRecorder.setOnInfoListener(onInfoListener);
        mediaRecorder.setOnErrorListener(onErrorListener);
        mediaRecorder.setAudioSource(1);
        mediaRecorder.setVideoSource(2);
        Surface surface = this.mSurface;
        if (surface == null || !surface.isValid()) {
            setPersistentInputSurface();
        }
        mediaRecorder.setInputSurface(this.mSurface);
        mediaRecorder.setOutputFormat(2);
        mediaRecorder.setAudioEncoder(3);
        mediaRecorder.setVideoEncoder(2);
        mediaRecorder.setVideoEncodingBitRate(12000000);
        mediaRecorder.setVideoFrameRate(30);
        if (i3 > i4) {
            if (i3 > 1920 || i4 > 1080) {
                float f = ((float) i4) / ((float) i3);
                if (f > 0.5625f) {
                    i3 = (int) Math.floor((double) (1080.0f / f));
                    i4 = 1080;
                } else {
                    i4 = (int) Math.floor((double) (f * 1920.0f));
                    i3 = 1920;
                }
            }
        } else if (i3 > 1080 || ((float) i4) > 1920.0f) {
            float f2 = ((float) i4) / ((float) i3);
            if (f2 > 1.7777778f) {
                i3 = (int) Math.floor((double) (1920.0f / f2));
                i4 = 1920;
            } else {
                i4 = (int) Math.floor((double) (f2 * 1080.0f));
                i3 = 1080;
            }
        }
        this.mDesiredHeight = i4;
        this.mDesiredWidth = i3;
        mediaRecorder.setVideoSize(i3, i4);
        mediaRecorder.setOrientationHint(i5);
        mediaRecorder.setOutputFile(file.getPath());
        mediaRecorder.prepare();
        this.mMediaRecorder = mediaRecorder;
        getHolder().setFixedSize(this.mDesiredWidth, this.mDesiredHeight);
    }

    public boolean startRecording() {
        try {
            this.mMediaRecorder.start();
            this.mIsRecording.set(true);
            return true;
        } catch (IllegalStateException unused) {
            this.mIsRecording.set(false);
            this.mMediaRecorder.reset();
            this.mMediaRecorder.release();
            return false;
        }
    }

    public boolean stopRecording() throws IllegalStateException {
        if (this.mIsRecording.get()) {
            boolean z = false;
            try {
                MediaRecorder mediaRecorder = this.mMediaRecorder;
                if (mediaRecorder == null) {
                    mediaRecorder.release();
                } else {
                    mediaRecorder.stop();
                    this.mIsRecording.set(false);
                    z = true;
                }
            } catch (RuntimeException unused) {
            } catch (Throwable th) {
                this.mMediaRecorder.release();
                throw th;
            }
            this.mMediaRecorder.release();
            return z;
        }
        throw new IllegalStateException("Cannot stop. Is not recording.");
    }

    public RendererCallbacks getRendererCallbacks() {
        WeakReference<RendererCallbacks> weakReference = this.mRendererCallbacksWeakReference;
        if (weakReference != null) {
            return (RendererCallbacks) weakReference.get();
        }
        return null;
    }

    public void setRendererCallbacks(RendererCallbacks rendererCallbacks) {
        this.mRendererCallbacksWeakReference = new WeakReference<>(rendererCallbacks);
    }

    public void queueEvent(Runnable runnable) {
        ARRenderThread aRRenderThread = this.mARRenderThread;
        if (aRRenderThread != null) {
            aRRenderThread.mRunnableQueue.add(runnable);
        }
    }

    public void setWidthHeight(int i, int i2) {
        if (this.mWidth != i) {
            this.mWidth = i;
            this.mSizeChange.set(true);
        }
        if (this.mHeight != i2) {
            this.mHeight = i2;
            this.mSizeChange.set(true);
        }
    }

    private class ARRenderThread extends Thread implements SurfaceHolder.Callback2 {
        int[] config = {12324, 8, 12323, 8, 12322, 8, 12321, 8, 12352, 4, 12610, 1, 12325, 16, 12344};
        EGLContext mEGLContext;
        EGLDisplay mEGLDisplay;
        EGLSurface mEGLSurface;
        EGLSurface mEGLSurfaceMedia;
        private AtomicBoolean mLoop = new AtomicBoolean(false);
        LinkedList<Runnable> mRunnableQueue = new LinkedList<>();

        public void surfaceRedrawNeeded(SurfaceHolder surfaceHolder) {
        }

        ARRenderThread() {
            if (Build.VERSION.SDK_INT >= 26) {
                this.config[10] = 12610;
            }
        }

        public EGLConfig chooseEglConfig(EGLDisplay eGLDisplay) {
            EGLConfig[] eGLConfigArr = new EGLConfig[1];
            EGLDisplay eGLDisplay2 = eGLDisplay;
            EGL14.eglChooseConfig(eGLDisplay2, this.config, 0, eGLConfigArr, 0, 1, new int[]{0}, 0);
            return eGLConfigArr[0];
        }

        public void run() {
            boolean z;
            EGLSurface eGLSurface;
            if (!RecordableSurfaceView.this.mHasGLContext.get()) {
                EGLDisplay eglGetDisplay = EGL14.eglGetDisplay(0);
                this.mEGLDisplay = eglGetDisplay;
                int[] iArr = new int[2];
                EGL14.eglInitialize(eglGetDisplay, iArr, 0, iArr, 1);
                EGLConfig chooseEglConfig = chooseEglConfig(this.mEGLDisplay);
                this.mEGLContext = EGL14.eglCreateContext(this.mEGLDisplay, chooseEglConfig, EGL14.EGL_NO_CONTEXT, new int[]{12440, 2, 12344}, 0);
                int[] iArr2 = {12344};
                EGLSurface eglCreateWindowSurface = EGL14.eglCreateWindowSurface(this.mEGLDisplay, chooseEglConfig, RecordableSurfaceView.this, iArr2, 0);
                this.mEGLSurface = eglCreateWindowSurface;
                EGL14.eglMakeCurrent(this.mEGLDisplay, eglCreateWindowSurface, eglCreateWindowSurface, this.mEGLContext);
                if (!(RecordableSurfaceView.this.mRendererCallbacksWeakReference == null || RecordableSurfaceView.this.mRendererCallbacksWeakReference.get() == null)) {
                    ((RendererCallbacks) RecordableSurfaceView.this.mRendererCallbacksWeakReference.get()).onSurfaceCreated();
                }
                this.mEGLSurfaceMedia = EGL14.eglCreateWindowSurface(this.mEGLDisplay, chooseEglConfig, RecordableSurfaceView.this.mSurface, iArr2, 0);
                GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
                RecordableSurfaceView.this.mHasGLContext.set(true);
                if (!(RecordableSurfaceView.this.mRendererCallbacksWeakReference == null || RecordableSurfaceView.this.mRendererCallbacksWeakReference.get() == null)) {
                    ((RendererCallbacks) RecordableSurfaceView.this.mRendererCallbacksWeakReference.get()).onContextCreated();
                }
                this.mLoop.set(true);
                while (this.mLoop.get()) {
                    if (!RecordableSurfaceView.this.mPaused) {
                        if (RecordableSurfaceView.this.mRenderMode.get() == RecordableSurfaceView.RENDERMODE_WHEN_DIRTY) {
                            if (RecordableSurfaceView.this.mRenderRequested.get()) {
                                RecordableSurfaceView.this.mRenderRequested.set(false);
                            } else {
                                z = false;
                                if (RecordableSurfaceView.this.mSizeChange.get()) {
                                    GLES20.glViewport(0, 0, RecordableSurfaceView.this.mWidth, RecordableSurfaceView.this.mHeight);
                                    if (!(RecordableSurfaceView.this.mRendererCallbacksWeakReference == null || RecordableSurfaceView.this.mRendererCallbacksWeakReference.get() == null)) {
                                        ((RendererCallbacks) RecordableSurfaceView.this.mRendererCallbacksWeakReference.get()).onSurfaceChanged(RecordableSurfaceView.this.mWidth, RecordableSurfaceView.this.mHeight);
                                    }
                                    RecordableSurfaceView.this.mSizeChange.set(false);
                                }
                                if (!(!z || (eGLSurface = this.mEGLSurface) == null || eGLSurface == EGL14.EGL_NO_SURFACE)) {
                                    if (!(RecordableSurfaceView.this.mRendererCallbacksWeakReference == null || RecordableSurfaceView.this.mRendererCallbacksWeakReference.get() == null)) {
                                        ((RendererCallbacks) RecordableSurfaceView.this.mRendererCallbacksWeakReference.get()).onPreDrawFrame();
                                    }
                                    if (!(RecordableSurfaceView.this.mRendererCallbacksWeakReference == null || RecordableSurfaceView.this.mRendererCallbacksWeakReference.get() == null)) {
                                        ((RendererCallbacks) RecordableSurfaceView.this.mRendererCallbacksWeakReference.get()).onDrawFrame();
                                    }
                                    EGL14.eglSwapBuffers(this.mEGLDisplay, this.mEGLSurface);
                                    if (RecordableSurfaceView.this.mIsRecording.get()) {
                                        EGLDisplay eGLDisplay = this.mEGLDisplay;
                                        EGLSurface eGLSurface2 = this.mEGLSurfaceMedia;
                                        EGL14.eglMakeCurrent(eGLDisplay, eGLSurface2, eGLSurface2, this.mEGLContext);
                                        if (!(RecordableSurfaceView.this.mRendererCallbacksWeakReference == null || RecordableSurfaceView.this.mRendererCallbacksWeakReference.get() == null)) {
                                            GLES20.glViewport(0, 0, RecordableSurfaceView.this.mDesiredWidth, RecordableSurfaceView.this.mDesiredHeight);
                                            ((RendererCallbacks) RecordableSurfaceView.this.mRendererCallbacksWeakReference.get()).onDrawFrame();
                                            GLES20.glViewport(0, 0, RecordableSurfaceView.this.mWidth, RecordableSurfaceView.this.mHeight);
                                        }
                                        EGL14.eglSwapBuffers(this.mEGLDisplay, this.mEGLSurfaceMedia);
                                        EGLDisplay eGLDisplay2 = this.mEGLDisplay;
                                        EGLSurface eGLSurface3 = this.mEGLSurface;
                                        EGL14.eglMakeCurrent(eGLDisplay2, eGLSurface3, eGLSurface3, this.mEGLContext);
                                    }
                                }
                                while (this.mRunnableQueue.size() > 0) {
                                    this.mRunnableQueue.remove().run();
                                }
                            }
                        }
                        z = true;
                        if (RecordableSurfaceView.this.mSizeChange.get()) {
                        }
                        ((RendererCallbacks) RecordableSurfaceView.this.mRendererCallbacksWeakReference.get()).onPreDrawFrame();
                        ((RendererCallbacks) RecordableSurfaceView.this.mRendererCallbacksWeakReference.get()).onDrawFrame();
                        EGL14.eglSwapBuffers(this.mEGLDisplay, this.mEGLSurface);
                        if (RecordableSurfaceView.this.mIsRecording.get()) {
                        }
                        while (this.mRunnableQueue.size() > 0) {
                        }
                    }
                    try {
                        Thread.sleep(16);
                    } catch (InterruptedException unused) {
                        if (!(RecordableSurfaceView.this.mRendererCallbacksWeakReference == null || RecordableSurfaceView.this.mRendererCallbacksWeakReference.get() == null)) {
                            ((RendererCallbacks) RecordableSurfaceView.this.mRendererCallbacksWeakReference.get()).onSurfaceDestroyed();
                        }
                        EGLDisplay eGLDisplay3 = this.mEGLDisplay;
                        if (eGLDisplay3 != null) {
                            EGL14.eglMakeCurrent(eGLDisplay3, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                            EGLSurface eGLSurface4 = this.mEGLSurface;
                            if (eGLSurface4 != null) {
                                EGL14.eglDestroySurface(this.mEGLDisplay, eGLSurface4);
                            }
                            EGLSurface eGLSurface5 = this.mEGLSurfaceMedia;
                            if (eGLSurface5 != null) {
                                EGL14.eglDestroySurface(this.mEGLDisplay, eGLSurface5);
                            }
                            EGL14.eglDestroyContext(this.mEGLDisplay, this.mEGLContext);
                            RecordableSurfaceView.this.mHasGLContext.set(false);
                            EGL14.eglReleaseThread();
                            EGL14.eglTerminate(this.mEGLDisplay);
                            RecordableSurfaceView.this.mSurface.release();
                            return;
                        }
                        return;
                    }
                }
            }
        }

        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            if (!isAlive() && !isInterrupted() && getState() != State.TERMINATED) {
                start();
            }
        }

        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            if (RecordableSurfaceView.this.mWidth != i2) {
                RecordableSurfaceView.this.mWidth = i2;
                RecordableSurfaceView.this.mSizeChange.set(true);
            }
            if (RecordableSurfaceView.this.mHeight != i3) {
                RecordableSurfaceView.this.mHeight = i3;
                RecordableSurfaceView.this.mSizeChange.set(true);
            }
        }

        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            this.mLoop.set(false);
            interrupt();
            RecordableSurfaceView.this.getHolder().removeCallback(this);
        }
    }
}
