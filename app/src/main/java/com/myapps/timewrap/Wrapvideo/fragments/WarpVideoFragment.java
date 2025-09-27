package com.myapps.timewrap.Wrapvideo.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.myapps.timewrap.R;
import com.myapps.timewrap.Utils.C1197util;
import com.myapps.timewrap.Wrapvideo.OnGalleryClickListener;
import com.myapps.timewrap.Wrapvideo.WrapVideoShareActivity;
import com.myapps.timewrap.Wrapvideo.base.WarpVideoView.RecordableSurfaceView;
import com.myapps.timewrap.Wrapvideo.base.utils.RecordingStatus;
import com.myapps.timewrap.Wrapvideo.base.utils.SharedPreferencesManager;
import com.myapps.timewrap.Wrapvideo.base.utils.VideoRenderer;
import com.myapps.timewrap.Wrapvideo.base.utils.YuvToRgbConverter;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class WarpVideoFragment extends Fragment implements VideoRenderer.OnRendererReadyListener, OnGalleryClickListener, View.OnClickListener {
    private static final String TAG = "VideoFragment";
    Activity activity;
    BlockingQueue<Bitmap> bitmapArrayBlockingQueue;
    Bitmap bitmapImage = null;
    ImageView btnSwap;
    private CompositeDisposable compositeDisposable;
    YuvToRgbConverter converter;
    File currentFile = null;
    RecordingStatus currentState = null;
    boolean firstLoad;
    Range<Integer> fpsRange;
    boolean isFlipped = false;
    ImageView ivShape;
    ImageView ivSpeed;
    ImageView iv_horizontal;
    ImageView iv_line;
    ImageView iv_round;
    ImageView iv_vertical;
    ImageView iv_zig_zap;
    Uri lastRecordedFile = null;
    LinearLayout llOptionShape;
    LinearLayout llOptionSpeed;
    RelativeLayout loadingLayout;
    private Handler mBackgroundHandler;
    private Handler mBackgroundHandler2;
    private HandlerThread mBackgroundThread;
    private HandlerThread mBackgroundThread2;
    public CameraDevice mCameraDevice;
    public boolean mCameraIsOpen = false;
    public Semaphore mCameraOpenCloseLock = new Semaphore(1);
    int mCameraRotation = 0;
    protected boolean mCameraSetupInProgress = true;
    protected int mCameraToUse = 1;
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
        }
    };
    private CameraCaptureSession.StateCallback mCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            WarpVideoFragment.this.mPreviewSession = cameraCaptureSession;
            Log.e(WarpVideoFragment.TAG, "CaptureSession Configured: " + cameraCaptureSession);
            WarpVideoFragment.this.updatePreview();
        }

        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            FragmentActivity activity = WarpVideoFragment.this.getActivity();
            Log.e(WarpVideoFragment.TAG, "config failed: " + cameraCaptureSession);
            if (activity != null) {
                Toast.makeText(activity, "CaptureSession Config Failed", 0).show();
            }
        }

        public void onClosed(CameraCaptureSession cameraCaptureSession) {
            super.onClosed(cameraCaptureSession);
            Log.e(WarpVideoFragment.TAG, "onClosed: " + cameraCaptureSession);
        }
    };
    LinearLayout mCircle;
    int mDeviceRotation = 0;
    private ImageReader mImageReader;
    private boolean mIsRecording = false;
    Observable<RecordingStatus> mIsRecordingObservable;
    BehaviorSubject<RecordingStatus> mIsRecordingSubject;
    ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        public void onImageAvailable(ImageReader imageReader) {
            if (!WarpVideoFragment.this.firstLoad) {
                WarpVideoFragment.this.activity.runOnUiThread(new Runnable() {
                    public void run() {
                        WarpVideoFragment.this.loadingLayout.setVisibility(8);
                        WarpVideoFragment.this.mRecordBtn.setVisibility(0);
                    }
                });
                WarpVideoFragment.this.firstLoad = true;
            }
            Image acquireLatestImage = imageReader.acquireLatestImage();
            Log.d("myApp", "onImageAvailable " + (System.currentTimeMillis() - WarpVideoFragment.this.start));
            WarpVideoFragment.this.start = System.currentTimeMillis();
            if (acquireLatestImage != null) {
                if (WarpVideoFragment.this.bitmapImage != null) {
                    acquireLatestImage.close();
                    WarpVideoFragment.this.mVideoRenderer.setCurrentImage(WarpVideoFragment.this.bitmapImage);
                    return;
                }
                WarpVideoFragment.this.start = System.currentTimeMillis();
                Bitmap yuvToRgb = WarpVideoFragment.this.converter.yuvToRgb(acquireLatestImage);
                acquireLatestImage.close();
                try {
                    WarpVideoFragment.this.bitmapArrayBlockingQueue.offer(yuvToRgb, 0, TimeUnit.MICROSECONDS);
                } catch (InterruptedException e) {
                    Log.d("myApp", e.getMessage());
                }
                Log.d("speed", "onImageAvailable end " + (System.currentTimeMillis() - WarpVideoFragment.this.start));
            }
        }
    };
    private File mOutputFile;
    private CaptureRequest.Builder mPreviewBuilder;
    public CameraCaptureSession mPreviewSession;
    private Size mPreviewSize;
    private int mPreviewTexture;
    RelativeLayout mRecordBtn;
    RecordableSurfaceView mRecordableSurfaceView;
    LinearLayout mSquare;
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        public void onOpened(CameraDevice cameraDevice) {
            WarpVideoFragment.this.mCameraOpenCloseLock.release();
            WarpVideoFragment.this.mCameraDevice = cameraDevice;
            WarpVideoFragment.this.mCameraIsOpen = true;
            WarpVideoFragment.this.startPreview();
        }

        public void onDisconnected(CameraDevice cameraDevice) {
            WarpVideoFragment.this.mCameraOpenCloseLock.release();
            cameraDevice.close();
            WarpVideoFragment.this.mCameraDevice = null;
            WarpVideoFragment.this.mCameraIsOpen = false;
            Log.e(WarpVideoFragment.TAG, "DISCONNECTED FROM CAMERA");
        }

        public void onError(CameraDevice cameraDevice, int i) {
            WarpVideoFragment.this.mCameraOpenCloseLock.release();
            cameraDevice.close();
            WarpVideoFragment.this.mCameraDevice = null;
            WarpVideoFragment.this.mCameraIsOpen = false;
            Log.e(WarpVideoFragment.TAG, "CameraDevice.StateCallback onError() " + i);
            FragmentActivity activity = WarpVideoFragment.this.getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    };
    private SurfaceTexture mSurfaceTexture;
    private List<Surface> mSurfaces;
    public VideoRenderer mVideoRenderer;
    private boolean mdisableClick = false;
    int screenHeight;
    int screenWidth;
    ScanSettings settings;
    long start = 0;
    TextView txt_one_x;
    TextView txt_three_x;
    TextView txt_two_x;

    public interface OnViewportSizeUpdatedListener {
        void onViewportSizeUpdated(int i, int i2);
    }

    public void onClick(Video video, String str) {
    }

    public void onRendererFinished() {
    }

    private static Size getClosestSupportedSize(List<Size> list, final int i, final int i2) {
        return (Size) Collections.min(list, new Comparator<Size>() {
            private int diff(Size size) {
                return Math.abs(i - size.getWidth()) + Math.abs(i2 - size.getHeight());
            }

            public int compare(Size size, Size size2) {
                return diff(size) - diff(size2);
            }
        });
    }

    private static Size chooseOptimalSize(Size[] sizeArr, int i, int i2, int i3, int i4, Size size) {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        int width = size.getWidth();
        int height = size.getHeight();
        for (Size size2 : sizeArr) {
            if (size2.getWidth() <= i3 && size2.getHeight() <= i4 && size2.getHeight() == (size2.getWidth() * height) / width) {
                if (size2.getWidth() < i || size2.getHeight() < i2) {
                    arrayList2.add(size2);
                } else {
                    arrayList.add(size2);
                }
            }
        }
        if (arrayList.size() > 0) {
            return (Size) Collections.min(arrayList, new CompareSizesByArea());
        }
        if (arrayList2.size() > 0) {
            return (Size) Collections.max(arrayList2, new CompareSizesByArea());
        }
        Size size3 = null;
        double d = Double.MAX_VALUE;
        for (Size size4 : sizeArr) {
            if (((double) Math.abs(size4.getWidth() - i2)) < d) {
                d = (double) Math.abs(size4.getWidth() - i2);
                size3 = size4;
            }
        }
        return size3 == null ? sizeArr[0] : size3;
    }

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Bundle arguments = getArguments();
        this.compositeDisposable = new CompositeDisposable();
        if (arguments != null) {
            this.settings = (ScanSettings) arguments.getParcelable(Constants.FRAGMENT_INPUT_KEY);
        } else {
            this.settings = new ScanSettings();
        }
        this.settings.readFromPreferences(getActivity());
        FragmentActivity activity2 = getActivity();
        this.activity = activity2;
        this.mCameraToUse = SharedPreferencesManager.getInt(activity2, SharedPreferencesManager.CAMERA_ID, this.mCameraToUse);
        View inflate = layoutInflater.inflate(R.layout.fragment_warpvideo, viewGroup, false);
        this.btnSwap = (ImageView) inflate.findViewById(R.id.fabSwap);
        this.loadingLayout = (RelativeLayout) inflate.findViewById(R.id.loadingLayout);
        this.mCircle = (LinearLayout) inflate.findViewById(R.id.circle_shape);
        this.mRecordBtn = (RelativeLayout) inflate.findViewById(R.id.btn_record);
        this.mRecordableSurfaceView = (RecordableSurfaceView) inflate.findViewById(R.id.surface_view);
        this.mSquare = (LinearLayout) inflate.findViewById(R.id.square_button);
        this.iv_horizontal = (ImageView) inflate.findViewById(R.id.iv_horizontal);
        this.iv_vertical = (ImageView) inflate.findViewById(R.id.iv_vertical);
        if (this.settings.getDirection() == 1) {
            this.iv_horizontal.setImageResource(R.drawable.ic_vertical);
            this.iv_vertical.setImageResource(R.drawable.ic_horizantal_hover);
        } else if (this.settings.getDirection() == 2) {
            this.iv_horizontal.setImageResource(R.drawable.ic_vertical_hover);
            this.iv_vertical.setImageResource(R.drawable.ic_horizantal);
        }
        this.iv_horizontal.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                WarpVideoFragment.this.iv_horizontal.setImageResource(R.drawable.ic_vertical_hover);
                WarpVideoFragment.this.iv_vertical.setImageResource(R.drawable.ic_horizantal);
                SharedPreferencesManager.setInt(WarpVideoFragment.this.activity, SharedPreferencesManager.SCAN_DIRECTION, WarpVideoFragment.this.settings.getDirection());
            }
        });
        this.iv_vertical.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                WarpVideoFragment.this.iv_horizontal.setImageResource(R.drawable.ic_vertical);
                WarpVideoFragment.this.iv_vertical.setImageResource(R.drawable.ic_horizantal_hover);
                SharedPreferencesManager.setInt(WarpVideoFragment.this.activity, SharedPreferencesManager.SCAN_DIRECTION, WarpVideoFragment.this.settings.getDirection());
            }
        });
        createRxSubject();
        Point point = new Point();
        this.activity.getWindowManager().getDefaultDisplay().getSize(point);
        this.screenWidth = point.x;
        this.screenHeight = (int) ((((double) point.x) * 16.0d) / 9.0d);
        if (this.mVideoRenderer == null) {
            this.mVideoRenderer = new VideoRenderer(this.activity);
        }
        this.mRecordableSurfaceView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (WarpVideoFragment.this.currentState == RecordingStatus.Start || WarpVideoFragment.this.currentState == RecordingStatus.Restart) {
                    WarpVideoFragment.this.mIsRecordingSubject.onNext(RecordingStatus.Pause);
                } else if (WarpVideoFragment.this.currentState == RecordingStatus.Pause) {
                    WarpVideoFragment.this.mIsRecordingSubject.onNext(RecordingStatus.Restart);
                }
            }
        });
        this.mRecordBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                WarpVideoFragment.this.onClickRecord();
            }
        });
        this.btnSwap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                WarpVideoFragment.this.onSwapCamera();
            }
        });
        getImage();
        this.ivShape = (ImageView) inflate.findViewById(R.id.ivShape);
        this.ivSpeed = (ImageView) inflate.findViewById(R.id.ivSpeed);
        this.llOptionSpeed = (LinearLayout) inflate.findViewById(R.id.llOptionSpeed);
        this.llOptionShape = (LinearLayout) inflate.findViewById(R.id.llOptionShape);
        this.ivShape.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                WarpVideoFragment.this.llOptionSpeed.setVisibility(4);
                if (WarpVideoFragment.this.llOptionShape.getVisibility() == 4) {
                    WarpVideoFragment.this.llOptionShape.setVisibility(0);
                } else {
                    WarpVideoFragment.this.llOptionShape.setVisibility(4);
                }
            }
        });
        this.ivSpeed.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                WarpVideoFragment.this.llOptionShape.setVisibility(4);
                if (WarpVideoFragment.this.llOptionSpeed.getVisibility() == 4) {
                    WarpVideoFragment.this.llOptionSpeed.setVisibility(0);
                } else {
                    WarpVideoFragment.this.llOptionSpeed.setVisibility(4);
                }
            }
        });
        this.iv_zig_zap = (ImageView) inflate.findViewById(R.id.iv_zig_zap);
        this.iv_round = (ImageView) inflate.findViewById(R.id.iv_round);
        this.iv_line = (ImageView) inflate.findViewById(R.id.iv_line);
        this.txt_three_x = (TextView) inflate.findViewById(R.id.txt_three_x);
        this.txt_two_x = (TextView) inflate.findViewById(R.id.txt_two_x);
        this.txt_one_x = (TextView) inflate.findViewById(R.id.txt_one_x);
        this.iv_zig_zap.setOnClickListener(this);
        this.iv_round.setOnClickListener(this);
        this.iv_line.setOnClickListener(this);
        this.txt_three_x.setOnClickListener(this);
        this.txt_two_x.setOnClickListener(this);
        this.txt_one_x.setOnClickListener(this);
        Log.d("CheckSetting", "Shape--" + this.settings.getShape() + "----speed--" + this.settings.getSpeed());
        if (this.settings.getShape() == 1) {
            this.iv_line.setImageResource(R.drawable.ic_line_hover);
            this.iv_round.setImageResource(R.drawable.ic_curve);
            this.iv_zig_zap.setImageResource(R.drawable.ic_zigzag);
        } else if (this.settings.getShape() == 2) {
            this.iv_line.setImageResource(R.drawable.ic_line);
            this.iv_round.setImageResource(R.drawable.ic_curve_hover);
            this.iv_zig_zap.setImageResource(R.drawable.ic_zigzag);
        } else if (this.settings.getShape() == 3) {
            this.iv_line.setImageResource(R.drawable.ic_line);
            this.iv_round.setImageResource(R.drawable.ic_curve);
            this.iv_zig_zap.setImageResource(R.drawable.ic_zigzag_hover);
        }
        if (this.settings.getSpeed() == 1) {
            this.txt_one_x.setTextColor(ViewCompat.MEASURED_STATE_MASK);
            this.txt_one_x.setBackgroundResource(R.drawable.round_bg);
            this.txt_two_x.setTextColor(-1);
            this.txt_two_x.setBackgroundResource(R.drawable.round_bg_black);
            this.txt_three_x.setTextColor(-1);
            this.txt_three_x.setBackgroundResource(R.drawable.round_bg_black);
        } else if (this.settings.getSpeed() == 2) {
            this.txt_one_x.setTextColor(-1);
            this.txt_one_x.setBackgroundResource(R.drawable.round_bg_black);
            this.txt_two_x.setTextColor(ViewCompat.MEASURED_STATE_MASK);
            this.txt_two_x.setBackgroundResource(R.drawable.round_bg);
            this.txt_three_x.setTextColor(-1);
            this.txt_three_x.setBackgroundResource(R.drawable.round_bg_black);
        } else if (this.settings.getSpeed() == 3) {
            this.txt_one_x.setTextColor(ViewCompat.MEASURED_STATE_MASK);
            this.txt_one_x.setBackgroundResource(R.drawable.round_bg_black);
            this.txt_two_x.setTextColor(-1);
            this.txt_two_x.setBackgroundResource(R.drawable.round_bg_black);
            this.txt_three_x.setTextColor(ViewCompat.MEASURED_STATE_MASK);
            this.txt_three_x.setBackgroundResource(R.drawable.round_bg);
        }
        return inflate;
    }

    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.iv_line) {
            this.iv_line.setImageResource(R.drawable.ic_line_hover);
            this.iv_round.setImageResource(R.drawable.ic_curve);
            this.iv_zig_zap.setImageResource(R.drawable.ic_zigzag);
            this.settings.setShape(1);
            SharedPreferencesManager.setInt(this.activity, SharedPreferencesManager.SCAN_SHAPE, this.settings.getShape());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    WarpVideoFragment.this.llOptionShape.setVisibility(View.GONE);
                }
            }, 1000);

        } else if (id == R.id.iv_round) {
            this.iv_line.setImageResource(R.drawable.ic_line);
            this.iv_round.setImageResource(R.drawable.ic_curve_hover);
            this.iv_zig_zap.setImageResource(R.drawable.ic_zigzag);
            this.settings.setShape(2);
            SharedPreferencesManager.setInt(this.activity, SharedPreferencesManager.SCAN_SHAPE, this.settings.getShape());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    WarpVideoFragment.this.llOptionShape.setVisibility(View.GONE);
                }
            }, 1000);

        } else if (id == R.id.iv_zig_zap) {
            this.iv_line.setImageResource(R.drawable.ic_line);
            this.iv_round.setImageResource(R.drawable.ic_curve);
            this.iv_zig_zap.setImageResource(R.drawable.ic_zigzag_hover);
            this.settings.setShape(3);
            SharedPreferencesManager.setInt(this.activity, SharedPreferencesManager.SCAN_SHAPE, this.settings.getShape());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    WarpVideoFragment.this.llOptionShape.setVisibility(View.GONE);
                }
            }, 1000);

        } else if (id == R.id.txt_one_x) {
            this.txt_one_x.setTextColor(ViewCompat.MEASURED_STATE_MASK);
            this.txt_one_x.setBackgroundResource(R.drawable.round_bg);
            this.txt_two_x.setTextColor(Color.WHITE);
            this.txt_two_x.setBackgroundResource(R.drawable.round_bg_black);
            this.txt_three_x.setTextColor(Color.WHITE);
            this.txt_three_x.setBackgroundResource(R.drawable.round_bg_black);
            this.settings.setSpeed(1);
            SharedPreferencesManager.setInt(this.activity, SharedPreferencesManager.SCAN_SPEED, this.settings.getSpeed());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    WarpVideoFragment.this.llOptionSpeed.setVisibility(View.GONE);
                }
            }, 1000);

        } else if (id == R.id.txt_two_x) {
            this.txt_one_x.setTextColor(Color.WHITE);
            this.txt_one_x.setBackgroundResource(R.drawable.round_bg_black);
            this.txt_two_x.setTextColor(ViewCompat.MEASURED_STATE_MASK);
            this.txt_two_x.setBackgroundResource(R.drawable.round_bg);
            this.txt_three_x.setTextColor(Color.WHITE);
            this.txt_three_x.setBackgroundResource(R.drawable.round_bg_black);
            this.settings.setSpeed(2);
            SharedPreferencesManager.setInt(this.activity, SharedPreferencesManager.SCAN_SPEED, this.settings.getSpeed());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    WarpVideoFragment.this.llOptionSpeed.setVisibility(View.GONE);
                }
            }, 1000);

        } else if (id == R.id.txt_three_x) {
            this.txt_one_x.setTextColor(Color.WHITE);
            this.txt_one_x.setBackgroundResource(R.drawable.round_bg_black);
            this.txt_two_x.setTextColor(Color.WHITE);
            this.txt_two_x.setBackgroundResource(R.drawable.round_bg_black);
            this.txt_three_x.setTextColor(ViewCompat.MEASURED_STATE_MASK);
            this.txt_three_x.setBackgroundResource(R.drawable.round_bg);
            this.settings.setSpeed(3);
            SharedPreferencesManager.setInt(this.activity, SharedPreferencesManager.SCAN_SPEED, this.settings.getSpeed());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    WarpVideoFragment.this.llOptionSpeed.setVisibility(View.GONE);
                }
            }, 1000);
        }
    }


    private void getImage() {
        String imagePath = this.settings.getImagePath();
        if (this.settings.isBackgroundFilter() && imagePath != null && imagePath != "") {
            this.bitmapImage = null;
            try {
                Bitmap createScaledBitmap = Bitmap.createScaledBitmap(MediaStore.Images.Media.getBitmap(this.activity.getContentResolver(), Uri.parse(imagePath)), this.screenWidth, this.screenHeight, true);
                this.bitmapImage = createScaledBitmap;
                this.settings.setBackgroundImage(createScaledBitmap);
            } catch (IOException e) {
                Log.d("myerror", e.getMessage());
            }
        }
    }

    private void createRxSubject() {
        BehaviorSubject<RecordingStatus> create = BehaviorSubject.create();
        this.mIsRecordingSubject = create;
        this.mIsRecordingObservable = create;
    }

    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
    }

    private void setupRecorder() {
        if (this.mVideoRenderer == null) {
            this.mVideoRenderer = new VideoRenderer(this.activity);
        }
        this.mRecordableSurfaceView.setWidthHeight(this.screenWidth, this.screenHeight);
        this.mRecordableSurfaceView.resume();
        File videoFile = getVideoFile();
        this.mOutputFile = videoFile;
        try {
            this.mRecordableSurfaceView.initRecorder(videoFile, this.screenWidth, this.screenHeight, (MediaRecorder.OnErrorListener) null, (MediaRecorder.OnInfoListener) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setVideoRenderer(this.mVideoRenderer);
    }

    public void onClickRecord() {
        toggleRecording(false);
    }

    public void onSwapCamera() {
        if (!this.mCameraSetupInProgress) {
            swapCamera();
        }
    }

    private void toggleRecording(boolean z) {
        if (!this.mdisableClick) {
            this.mdisableClick = true;
            if (this.mIsRecording) {
                stopRecording(z);
            } else {
                startRecording(z);
            }
        }
    }

    private void startRecording(boolean z) {
        try {
            if (this.mRecordableSurfaceView.startRecording()) {
                this.mSquare.setVisibility(0);
                this.mCircle.setVisibility(4);
                this.btnSwap.setVisibility(4);
                this.mIsRecording = true;
                if (!z) {
                    this.mIsRecordingSubject.onNext(RecordingStatus.Start);
                }
            } else {
                Toast.makeText(this.activity, "Oops, device could be incompatible, please try again!", 0).show();
            }
        } finally {
            this.mdisableClick = false;
        }
    }

    private void stopRecording(boolean z) {
        try {
            this.mSquare.setVisibility(4);
            this.mCircle.setVisibility(0);
            this.btnSwap.setVisibility(0);
            this.mRecordableSurfaceView.stopRecording();
            if (!z) {
                this.mIsRecordingSubject.onNext(RecordingStatus.Stop);
            }
            C1197util.wrapVideoFile = this.mOutputFile;
            File videoFile = getVideoFile();
            this.mOutputFile = videoFile;
            this.mRecordableSurfaceView.initRecorder(videoFile, this.screenWidth, this.screenHeight, (MediaRecorder.OnErrorListener) null, (MediaRecorder.OnInfoListener) null);
            startActivity(new Intent(getContext(), WrapVideoShareActivity.class));
        } catch (IOException e) {
            Log.e(TAG, "Couldn't re-init recording", e);
        } catch (Throwable th) {
            this.mdisableClick = false;
            this.mIsRecording = false;
            throw th;
        }
        this.mdisableClick = false;
        this.mIsRecording = false;
    }

    private File getVideoFile() {
        File file;
        String str = "timewarpscan_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".mp4";
        if (isExternalStorageWritable()) {
            file = this.activity.getExternalCacheDir();
        } else {
            file = this.activity.getFilesDir();
        }
        try {
            file.mkdirs();
        } catch (Exception unused) {
        }
        File file2 = new File(file, str);
        this.currentFile = file2;
        return file2;
    }

    private boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals("mounted");
    }

    public void onResume() {
        super.onResume();
        createRxSubject();
        setupRecorder();
        startBackgroundThread();
    }

    public void onPause() {
        super.onPause();
        closeCamera();
        if (this.mIsRecording) {
            stopRecording(false);
        }
        stopBackgroundThread();
        this.mRecordableSurfaceView.pause();
        this.mRecordableSurfaceView.setRendererCallbacks((RecordableSurfaceView.RendererCallbacks) null);
        this.mVideoRenderer.onSurfaceDestroyed();
        this.mVideoRenderer = null;
        this.compositeDisposable.clear();
    }

    private void startBackgroundThread() {
        this.bitmapArrayBlockingQueue = new ArrayBlockingQueue(1);
        setupConsumer();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        this.mPreviewSize = new Size(displayMetrics.widthPixels, displayMetrics.heightPixels);
        this.mDeviceRotation = this.activity.getWindowManager().getDefaultDisplay().getRotation();
        HandlerThread handlerThread = new HandlerThread("CameraBackground");
        this.mBackgroundThread = handlerThread;
        handlerThread.start();
        this.mBackgroundHandler = new Handler(this.mBackgroundThread.getLooper());
        HandlerThread handlerThread2 = new HandlerThread("CameraBackground2");
        this.mBackgroundThread2 = handlerThread2;
        handlerThread2.start();
        this.mBackgroundHandler2 = new Handler(this.mBackgroundThread2.getLooper());
    }

    public void showToast() {
        int i = SharedPreferencesManager.getInt(this.activity, SharedPreferencesManager.SHOW_HELP_MESSAGE, 0);
        if (i < 3) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.activity, R.style.AlertDialogTheme);
            builder.setMessage("To pause or restart the scanner while recording, tap on the screen.");
            builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            builder.setPositiveButton("Don't show again", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    SharedPreferencesManager.setInt(WarpVideoFragment.this.activity, SharedPreferencesManager.SHOW_HELP_MESSAGE, 5);
                    dialogInterface.cancel();
                }
            });
            SharedPreferencesManager.setInt(this.activity, SharedPreferencesManager.SHOW_HELP_MESSAGE, i + 1);
            builder.show();
        }
    }

    private void stopBackgroundThread() {
        Log.e(TAG, "RELEASE TEXTURE");
        SurfaceTexture surfaceTexture = this.mSurfaceTexture;
        if (surfaceTexture != null) {
            surfaceTexture.release();
            this.mSurfaceTexture = null;
            this.mSurfaces.clear();
        }
    }

    public void swapCamera() {
        this.mCameraSetupInProgress = true;
        closeCamera();
        if (this.mCameraToUse == 1) {
            this.mCameraToUse = 0;
        } else {
            this.mCameraToUse = 1;
        }
        SharedPreferencesManager.setInt(this.activity, SharedPreferencesManager.CAMERA_ID, this.mCameraToUse);
        openCamera();
    }

    public void openCamera() {
        FragmentActivity activity2 = getActivity();
        if (activity2 != null && !activity2.isFinishing()) {
            if (this.mCameraDevice == null || !this.mCameraIsOpen) {
                CameraManager cameraManager = (CameraManager) activity2.getSystemService("camera");
                try {
                    if (this.mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                        String[] cameraIdList = cameraManager.getCameraIdList();
                        if (this.mCameraToUse >= cameraIdList.length) {
                            this.mCameraToUse = 0;
                        }
                        String frontFacingCameraId = getFrontFacingCameraId(cameraIdList, cameraManager);
                        CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(frontFacingCameraId);
                        this.fpsRange = getRange(cameraCharacteristics);
                        this.mCameraRotation = getJpegOrientation(cameraCharacteristics, this.mDeviceRotation);
                        cameraManager.openCamera(frontFacingCameraId, this.mStateCallback, this.mBackgroundHandler);
                        return;
                    }
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                } catch (CameraAccessException unused) {
                    Toast.makeText(activity2, "Cannot access the camera.", 0).show();
                    activity2.finish();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    new ErrorDialog().show(getFragmentManager(), "dialog");
                } catch (InterruptedException unused2) {
                    throw new RuntimeException("Interrupted while trying to lock camera opening.");
                }
            }
        }
    }

    public String getFrontFacingCameraId(String[] strArr, CameraManager cameraManager) throws CameraAccessException {
        int i = 1;
        this.isFlipped = true;
        if (this.mCameraToUse == 0) {
            this.isFlipped = false;
        } else {
            i = 0;
        }
        for (String str : strArr) {
            if (((Integer) cameraManager.getCameraCharacteristics(str).get(CameraCharacteristics.LENS_FACING)).intValue() == i) {
                return str;
            }
        }
        return strArr[0];
    }

    public void share(Uri uri) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("video/*");
        intent.putExtra("android.intent.extra.STREAM", uri);
        intent.addFlags(1);
        startActivity(Intent.createChooser(intent, "Share using"));
    }

    public void setupConsumer() {
        new Thread(new Consumer(this.bitmapArrayBlockingQueue)).start();
    }

    private int getJpegOrientation(CameraCharacteristics cameraCharacteristics, int i) {
        boolean z = false;
        if (i == -1) {
            return 0;
        }
        int intValue = ((Integer) cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        int i2 = ((i + 45) / 90) * 90;
        if (((Integer) cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue() == 0) {
            z = true;
        }
        if (z) {
            i2 = -i2;
        }
        return ((intValue + i2) + 360) % 360;
    }

    public void closeCamera() {
        try {
            this.mCameraOpenCloseLock.acquire();
            if (this.mCameraDevice != null) {
                CameraCaptureSession cameraCaptureSession = this.mPreviewSession;
                if (cameraCaptureSession != null) {
                    cameraCaptureSession.stopRepeating();
                }
                this.mCameraDevice.close();
                this.mCameraDevice = null;
                this.mCameraIsOpen = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable th) {
            this.mCameraOpenCloseLock.release();
            throw th;
        }
        this.mCameraOpenCloseLock.release();
    }

    public void startPreview() {
        CameraDevice cameraDevice = this.mCameraDevice;
        if (cameraDevice != null) {
            try {
                if (this.mVideoRenderer != null) {
                    this.mPreviewBuilder = cameraDevice.createCaptureRequest(1);
                    if (this.mSurfaces == null) {
                        this.mSurfaces = new ArrayList();
                    }
                    if (this.mPreviewTexture == -1) {
                        this.mPreviewTexture = this.mVideoRenderer.getCameraTexture();
                    }
                    SurfaceTexture surfaceTexture = new SurfaceTexture(this.mPreviewTexture);
                    this.mSurfaceTexture = surfaceTexture;
                    this.mVideoRenderer.setSurfaceTexture(surfaceTexture);
                    if (this.mSurfaces.size() != 0) {
                        for (Surface release : this.mSurfaces) {
                            release.release();
                        }
                        this.mSurfaces.clear();
                    }
                    CameraManager cameraManager = (CameraManager) this.activity.getSystemService("camera");
                    Size[] outputSizes = ((StreamConfigurationMap) cameraManager.getCameraCharacteristics(cameraManager.getCameraIdList()[this.mCameraToUse]).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(SurfaceTexture.class);
                    int i = this.screenHeight;
                    int i2 = this.screenWidth;
                    Size chooseOptimalSize = chooseOptimalSize(outputSizes, i, i2, i, i2, new Size(this.screenHeight, this.screenWidth));
                    int height = chooseOptimalSize.getHeight();
                    int width = chooseOptimalSize.getWidth();
                    if (height == 0 || width == 0) {
                        height = 1920;
                        width = 1180;
                    }
                    if (chooseOptimalSize == null) {
                        Toast.makeText(this.activity, "Opps, your camera is incompatible!", 1).show();
                        return;
                    }
                    this.mSurfaceTexture.setDefaultBufferSize(width, height);
                    this.converter = new YuvToRgbConverter(getContext());
                    ImageReader newInstance = ImageReader.newInstance(width, height, 35, 1);
                    this.mImageReader = newInstance;
                    newInstance.setOnImageAvailableListener(this.mOnImageAvailableListener, this.mBackgroundHandler2);
                    this.mVideoRenderer.setScreenSize(height, width);
                    this.mRecordableSurfaceView.getWidth();
                    this.mRecordableSurfaceView.getHeight();
                    chooseOptimalSize.getHeight();
                    chooseOptimalSize.getWidth();
                    this.mVideoRenderer.setAspectRatio(1.0f);
                    Surface surface = this.mImageReader.getSurface();
                    this.mSurfaces.add(surface);
                    Surface surface2 = new Surface(this.mSurfaceTexture);
                    this.mSurfaces.add(surface2);
                    this.mPreviewBuilder.addTarget(surface);
                    this.mPreviewBuilder.addTarget(surface2);
                    this.mCameraDevice.createCaptureSession(this.mSurfaces, this.mCaptureSessionStateCallback, this.mBackgroundHandler);
                    this.mIsRecordingSubject.subscribe(new io.reactivex.rxjava3.functions.Consumer() {
                        public final void accept(Object obj) {
                            WarpVideoFragment.this.currentState = (RecordingStatus) obj;
                        }
                    }, new io.reactivex.rxjava3.functions.Consumer<Throwable>() {
                        public void accept(Throwable th) throws Throwable {
                            Log.d("myApp", th.getMessage());
                        }
                    });
                    this.mCameraSetupInProgress = false;
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void updatePreview() {
        if (this.mCameraDevice != null) {
            try {
                this.mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, 3);
                this.mPreviewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, this.fpsRange);
                this.mPreviewBuilder.set(CaptureRequest.CONTROL_AE_LOCK, false);
                this.mPreviewSession.setRepeatingRequest(this.mPreviewBuilder.build(), this.mCaptureCallback, this.mBackgroundHandler);
                this.mSurfaceTexture.setOnFrameAvailableListener(this.mVideoRenderer);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private Range<Integer> getRange(CameraCharacteristics cameraCharacteristics) {
        Range<Integer> range = null;
        for (Range<Integer> range2 : (Range[]) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)) {
            int intValue = range2.getUpper().intValue();
            if (intValue >= 29 && (range == null || intValue < range.getUpper().intValue())) {
                range = range2;
            }
        }
        return range;
    }

    public void setRecordingObservable(Observable<RecordingStatus> observable) {
        this.mIsRecordingObservable = observable;
    }

    private Size getOptimalPreviewSize(Size[] sizeArr, int i, int i2) {
        int i3 = i2;
        double d = ((double) i) / ((double) i3);
        List<Size> asList = Arrays.asList(sizeArr);
        Collections.sort(asList, new CompareSizesByArea());
        double d2 = Double.MAX_VALUE;
        Size size = null;
        double d3 = Double.MAX_VALUE;
        for (Size size2 : asList) {
            if (Math.abs((((double) size2.getWidth()) / ((double) size2.getHeight())) - d) <= 0.001d && ((double) Math.abs(size2.getWidth() - i3)) < d3) {
                d3 = (double) Math.abs(size2.getWidth() - i3);
                size = size2;
            }
        }
        if (size == null) {
            for (Size size3 : asList) {
                if (((double) Math.abs(size3.getWidth() - i3)) < d2) {
                    size = size3;
                    d2 = (double) Math.abs(size3.getWidth() - i3);
                }
            }
        }
        return size;
    }

    public int getCurrentCameraType() {
        return this.mCameraToUse;
    }

    public void setCameraToUse(int i) {
        this.mCameraToUse = i;
    }

    public void setPreviewTexture(int i) {
        this.mPreviewTexture = i;
    }

    public SurfaceTexture getSurfaceTexture() {
        return this.mSurfaceTexture;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        this.mSurfaceTexture = surfaceTexture;
    }

    public void onRendererReady() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                WarpVideoFragment.this.openCamera();
            }
        });
    }

    public VideoRenderer getVideoRenderer() {
        return this.mVideoRenderer;
    }

    public void setVideoRenderer(VideoRenderer videoRenderer) {
        this.mVideoRenderer = videoRenderer;
        if (videoRenderer != null) {
            videoRenderer.setScanSettings(this.settings);
            this.mVideoRenderer.setVideoFragment(this);
            this.mVideoRenderer.setOnRendererReadyListener(this);
            this.mRecordableSurfaceView.setRendererCallbacks(this.mVideoRenderer);
            this.mVideoRenderer.onSurfaceChanged(this.screenWidth, this.screenHeight);
            this.mVideoRenderer.setRecordingObservable(this.mIsRecordingObservable);
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {
        CompareSizesByArea() {
        }

        public int compare(Size size, Size size2) {
            return Long.signum((((long) size.getWidth()) * ((long) size.getHeight())) - (((long) size2.getWidth()) * ((long) size2.getHeight())));
        }
    }

    public static class ErrorDialog extends DialogFragment {
        public Dialog onCreateDialog(Bundle bundle) {
            final FragmentActivity activity = getActivity();
            return new AlertDialog.Builder(activity).setMessage("This device doesn't support Camera2 API.").setPositiveButton(17039370, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    activity.finish();
                }
            }).create();
        }
    }

    class Consumer implements Runnable {
        private final BlockingQueue queue;

        Consumer(BlockingQueue blockingQueue) {
            this.queue = blockingQueue;
        }

        public void run() {
            while (true) {
                try {
                    consume(this.queue.take());
                } catch (InterruptedException unused) {
                    return;
                }
            }
        }

        public void consume(Object obj) {
            long currentTimeMillis = System.currentTimeMillis();
            Bitmap rotate = WarpVideoFragment.this.converter.rotate((Bitmap) obj, WarpVideoFragment.this.mCameraRotation);
            Log.d("flip", "rotate " + (System.currentTimeMillis() - currentTimeMillis));
            if (WarpVideoFragment.this.isFlipped) {
                rotate = WarpVideoFragment.this.converter.flip(rotate);
            }
            Log.d("flip", "flip " + (System.currentTimeMillis() - currentTimeMillis));
            if (WarpVideoFragment.this.mVideoRenderer != null) {
                WarpVideoFragment.this.mVideoRenderer.setCurrentImage(rotate);
            }
        }
    }
}
