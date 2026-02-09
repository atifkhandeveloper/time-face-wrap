package com.myapps.timewrap.Utils;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;
import androidx.core.view.MotionEventCompat;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class BitmapToVideoEncoder {
    private static final int BIT_RATE = 16000000;
    private static final int FRAME_RATE = 25;
    private static final int I_FRAME_INTERVAL = 1;
    private static final String MIME_TYPE = "video/avc";
    private static final String TAG = "BitmapToVideoEncoder";
    private static int mHeight;
    private static int mWidth;
    private boolean mAbort = false;
    private IBitmapToVideoEncoderCallback mCallback;
    private Queue<Bitmap> mEncodeQueue = new ConcurrentLinkedQueue();
    private Object mFrameSync = new Object();
    private int mGenerateIndex = 0;
    private CountDownLatch mNewFrameLatch;
    private boolean mNoMoreFrames = false;
    private File mOutputFile;
    private int mTrackIndex;
    private MediaCodec mediaCodec;
    private MediaMuxer mediaMuxer;

    public interface IBitmapToVideoEncoderCallback {
        void onEncodingComplete(File file);
    }

    private static boolean isRecognizedFormat(int i) {
        if (i == 39 || i == 2130706688) {
            return true;
        }
        switch (i) {
            case 19:
            case 20:
            case 21:
                return true;
            default:
                return false;
        }
    }

    public BitmapToVideoEncoder(IBitmapToVideoEncoderCallback iBitmapToVideoEncoderCallback) {
        this.mCallback = iBitmapToVideoEncoderCallback;
    }

    private static MediaCodecInfo selectCodec(String str) {
        int codecCount = MediaCodecList.getCodecCount();
        for (int i = 0; i < codecCount; i++) {
            MediaCodecInfo codecInfoAt = MediaCodecList.getCodecInfoAt(i);
            if (codecInfoAt.isEncoder()) {
                for (String equalsIgnoreCase : codecInfoAt.getSupportedTypes()) {
                    if (equalsIgnoreCase.equalsIgnoreCase(str)) {
                        return codecInfoAt;
                    }
                }
                continue;
            }
        }
        return null;
    }

    private static int selectColorFormat(MediaCodecInfo mediaCodecInfo, String str) {
        for (int i : mediaCodecInfo.getCapabilitiesForType(str).colorFormats) {
            if (isRecognizedFormat(i)) {
                return i;
            }
        }
        return 0;
    }

    public boolean isEncodingStarted() {
        return this.mediaCodec != null && this.mediaMuxer != null && !this.mNoMoreFrames && !this.mAbort;
    }

    public int getActiveBitmaps() {
        return this.mEncodeQueue.size();
    }

    public void startEncoding(int i, int i2, File file) {
        mWidth = i;
        mHeight = i2;
        this.mOutputFile = file;
        try {
            String canonicalPath = file.getCanonicalPath();
            MediaCodecInfo selectCodec = selectCodec(MIME_TYPE);
            if (selectCodec == null) {
                Log.e(TAG, "Unable to find an appropriate codec for video/avc");
                return;
            }
            Log.d(TAG, "found codec: " + selectCodec.getName());
            try {
                this.mediaCodec = MediaCodec.createByCodecName(selectCodec.getName());
                MediaFormat createVideoFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
                createVideoFormat.setInteger("bitrate", BIT_RATE);
                createVideoFormat.setInteger("frame-rate", 25);
                createVideoFormat.setInteger("color-format", 21);
                createVideoFormat.setInteger("i-frame-interval", 1);
                this.mediaCodec.configure(createVideoFormat, (Surface) null, (MediaCrypto) null, 1);
                this.mediaCodec.start();
                try {
                    this.mediaMuxer = new MediaMuxer(canonicalPath, 0);
                    Log.d(TAG, "Initialization complete. Starting encoder...");
                    Completable.fromAction(new Action() {
                        public final void run() {
                            BitmapToVideoEncoder.this.lambda$startEncoding$0$BitmapToVideoEncoder();
                        }
                    }).subscribeOn(Schedulers.io()).observeOn(Schedulers.newThread()).subscribe();
                } catch (IOException e) {
                    Log.e(TAG, "MediaMuxer creation failed. " + e.getMessage());
                }
            } catch (IOException e2) {
                Log.e(TAG, "Unable to create MediaCodec " + e2.getMessage());
            }
        } catch (IOException unused) {
            Log.e(TAG, "Unable to get path for " + file);
        }
    }

    public void stopEncoding() {
        if (this.mediaCodec == null || this.mediaMuxer == null) {
            Log.d(TAG, "Failed to stop encoding since it never started");
            return;
        }
        Log.d(TAG, "Stopping encoding");
        this.mNoMoreFrames = true;
        synchronized (this.mFrameSync) {
            CountDownLatch countDownLatch = this.mNewFrameLatch;
            if (countDownLatch != null && countDownLatch.getCount() > 0) {
                this.mNewFrameLatch.countDown();
            }
        }
    }

    public void abortEncoding() {
        if (this.mediaCodec == null || this.mediaMuxer == null) {
            Log.d(TAG, "Failed to abort encoding since it never started");
            return;
        }
        Log.d(TAG, "Aborting encoding");
        this.mNoMoreFrames = true;
        this.mAbort = true;
        this.mEncodeQueue = new ConcurrentLinkedQueue();
        synchronized (this.mFrameSync) {
            CountDownLatch countDownLatch = this.mNewFrameLatch;
            if (countDownLatch != null && countDownLatch.getCount() > 0) {
                this.mNewFrameLatch.countDown();
            }
        }
    }

    public void queueFrame(Bitmap bitmap) {
        if (this.mediaCodec == null || this.mediaMuxer == null) {
            Log.d(TAG, "Failed to queue frame. Encoding not started");
            return;
        }
        Log.d(TAG, "Queueing frame");
        this.mEncodeQueue.add(bitmap);
        synchronized (this.mFrameSync) {
            CountDownLatch countDownLatch = this.mNewFrameLatch;
            if (countDownLatch != null && countDownLatch.getCount() > 0) {
                this.mNewFrameLatch.countDown();
            }
        }
    }

    public void lambda$startEncoding$0$BitmapToVideoEncoder() {
        CountDownLatch countDownLatch;
        Log.d(TAG, "Encoder started");
        while (true) {
            if (!this.mNoMoreFrames || this.mEncodeQueue.size() != 0) {
                Bitmap poll = this.mEncodeQueue.poll();
                if (poll == null) {
                    synchronized (this.mFrameSync) {
                        countDownLatch = new CountDownLatch(1);
                        this.mNewFrameLatch = countDownLatch;
                    }
                    try {
                        countDownLatch.await();
                    } catch (InterruptedException unused) {
                    }
                    poll = this.mEncodeQueue.poll();
                }
                if (poll != null) {
                    byte[] nv21 = getNV21(poll.getWidth(), poll.getHeight(), poll);
                    int dequeueInputBuffer = this.mediaCodec.dequeueInputBuffer(500000);
                    long computePresentationTime = computePresentationTime((long) this.mGenerateIndex, 25);
                    if (dequeueInputBuffer >= 0) {
                        ByteBuffer inputBuffer = this.mediaCodec.getInputBuffer(dequeueInputBuffer);
                        inputBuffer.clear();
                        inputBuffer.put(nv21);
                        this.mediaCodec.queueInputBuffer(dequeueInputBuffer, 0, nv21.length, computePresentationTime, 0);
                        this.mGenerateIndex++;
                    }
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int dequeueOutputBuffer = this.mediaCodec.dequeueOutputBuffer(bufferInfo, 500000);
                    if (dequeueOutputBuffer == -1) {
                        Log.e(TAG, "No output from encoder available");
                    } else if (dequeueOutputBuffer == -2) {
                        this.mTrackIndex = this.mediaMuxer.addTrack(this.mediaCodec.getOutputFormat());
                        this.mediaMuxer.start();
                    } else if (dequeueOutputBuffer < 0) {
                        Log.e(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + dequeueOutputBuffer);
                    } else if (bufferInfo.size != 0) {
                        ByteBuffer outputBuffer = this.mediaCodec.getOutputBuffer(dequeueOutputBuffer);
                        if (outputBuffer == null) {
                            Log.e(TAG, "encoderOutputBuffer " + dequeueOutputBuffer + " was null");
                        } else {
                            outputBuffer.position(bufferInfo.offset);
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                            this.mediaMuxer.writeSampleData(this.mTrackIndex, outputBuffer, bufferInfo);
                            this.mediaCodec.releaseOutputBuffer(dequeueOutputBuffer, false);
                        }
                    }
                }
            } else {
                release();
                if (this.mAbort) {
                    this.mOutputFile.delete();
                    return;
                } else {
                    this.mCallback.onEncodingComplete(this.mOutputFile);
                    return;
                }
            }
        }
        /*while (true) {
        }*/
    }

    private void release() {
        MediaCodec mediaCodec2 = this.mediaCodec;
        if (mediaCodec2 != null) {
            mediaCodec2.stop();
            this.mediaCodec.release();
            this.mediaCodec = null;
            Log.d(TAG, "RELEASE CODEC");
        }
        MediaMuxer mediaMuxer2 = this.mediaMuxer;
        if (mediaMuxer2 != null) {
            mediaMuxer2.stop();
            this.mediaMuxer.release();
            this.mediaMuxer = null;
            Log.d(TAG, "RELEASE MUXER");
        }
    }

    private byte[] getNV21(int i, int i2, Bitmap bitmap) {
        int i3 = i * i2;
        int[] iArr = new int[i3];
        bitmap.getPixels(iArr, 0, i, 0, 0, i, i2);
        byte[] bArr = new byte[((i3 * 3) / 2)];
        encodeYUV420SP(bArr, iArr, i, i2);
        bitmap.recycle();
        return bArr;
    }

    private void encodeYUV420SP(byte[] bArr, int[] iArr, int i, int i2) {
        int i3 = i;
        int i4 = i2;
        int i5 = i3 * i4;
        int i6 = 0;
        int i7 = 0;
        for (int i8 = 0; i8 < i4; i8++) {
            int i9 = 0;
            while (i9 < i3) {
                int i10 = iArr[i7];
                int i11 = (iArr[i7] & 16711680) >> 16;
                int i12 = (iArr[i7] & MotionEventCompat.ACTION_POINTER_INDEX_MASK) >> 8;
                int i13 = 255;
                int i14 = (iArr[i7] & 255) >> 0;
                int i15 = (((((i11 * 66) + (i12 * 129)) + (i14 * 25)) + 128) >> 8) + 16;
                int i16 = (((((i11 * -38) - (i12 * 74)) + (i14 * 112)) + 128) >> 8) + 128;
                int i17 = (((((i11 * 112) - (i12 * 94)) - (i14 * 18)) + 128) >> 8) + 128;
                int i18 = i6 + 1;
                if (i15 < 0) {
                    i15 = 0;
                } else if (i15 > 255) {
                    i15 = 255;
                }
                bArr[i6] = (byte) i15;
                if (i8 % 2 == 0 && i7 % 2 == 0) {
                    int i19 = i5 + 1;
                    if (i16 < 0) {
                        i16 = 0;
                    } else if (i16 > 255) {
                        i16 = 255;
                    }
                    bArr[i5] = (byte) i16;
                    i5 = i19 + 1;
                    if (i17 < 0) {
                        i13 = 0;
                    } else if (i17 <= 255) {
                        i13 = i17;
                    }
                    bArr[i19] = (byte) i13;
                }
                i7++;
                i9++;
                i6 = i18;
            }
        }
    }

    private long computePresentationTime(long j, int i) {
        return ((j * 1000000) / ((long) i)) + 132;
    }

    public File getOutputFile() {
        return this.mOutputFile;
    }
}
