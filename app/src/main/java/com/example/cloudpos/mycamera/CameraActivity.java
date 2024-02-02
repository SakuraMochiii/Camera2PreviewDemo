package com.example.cloudpos.mycamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class CameraActivity extends AppCompatActivity {
    private static String TAG = "CameraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        String cameraId = getIntent().getExtras().getString("cameraId");
        //Toast.makeText(CameraActivity.this, cameraId, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "getIntent().getExtras().getString(cameraId) = " + cameraId);
        iCreateManager(cameraId);
    }

    CameraManager mCameraManager;
    SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;

    public void iCreateManager(final String mCameraId) {
        mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        mSurfaceView = findViewById(R.id.surfaceview);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated =  " + mCameraId);
                initCameraAndPreview(mCameraId);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    Handler mHandler;
    ImageReader mImageReader;


    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        public ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }


    private File mFile;

    private Handler mBackgroundHandler;

    /**
     * This is called when the captured image is ready to be saved
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
        }

    };

    private void initCameraAndPreview(String mCameraId) {
        Log.d(TAG, "init camera and preview");
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        try {
            //mCameraId = "" + CameraCharacteristics.LENS_FACING_FRONT;

            mImageReader = ImageReader.newInstance(mSurfaceView.getWidth(), mSurfaceView.getHeight(),
                    ImageFormat.JPEG,/*maxImages*/7);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Toast.makeText(this, "please check camera permission!", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
                return;
            }
            Log.d(TAG, "open camera id = " + mCameraId+ "  mCameraManager = "+mCameraManager);

            mCameraManager.openCamera(mCameraId, deviceStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "open camera failed." + e.getMessage());
        }
    }

    CameraDevice mCameraDevice;
    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG, "deviceStateCallback:camera was opend.");
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            try {
                createCameraCaptureSession();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    /**
     * Camera state: preview
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: waiting capture
     */
    private static final int STATE_WAITING_CAPTURE = 2;
    /**
     * current state
     */
    private int mState = STATE_PREVIEW;

    private CaptureRequest.Builder mPreviewBuilder;

    private void createCameraCaptureSession() throws CameraAccessException {
        Log.d(TAG, "createCameraCaptureSession");

        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewBuilder.addTarget(mSurfaceHolder.getSurface());
        mState = STATE_PREVIEW;
        mCameraDevice.createCaptureSession(
                Arrays.asList(mSurfaceHolder.getSurface(), mImageReader.getSurface()),
                mSessionPreviewStateCallback, mHandler);
    }

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;
    private CameraCaptureSession.StateCallback mSessionPreviewStateCallback = new
            CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession captureSession) {
                    Log.d(TAG, "mSessionPreviewStateCallback onConfigured");
                    mCaptureSession = captureSession;
                    try {
                        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        Log.e(TAG, "set preview builder failed." + e.getMessage());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            };

    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
                    //Log.d(TAG, "mSessionCaptureCallback, onCaptureCompleted");//mSessionCaptureCallback 会循环不断回调
                    mCaptureSession = session;
                    checkState(result);
                }

                @Override
                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request,
                                                CaptureResult partialResult) {
                    Log.d(TAG, "mSessionCaptureCallback,  onCaptureProgressed");
                    mCaptureSession = session;
                    checkState(partialResult);
                }

                private void checkState(CaptureResult result) {
                    switch (mState) {
                        case STATE_PREVIEW:
                            // NOTHING
                            break;
                        case STATE_WAITING_CAPTURE:
                            int afState = result.get(CaptureResult.CONTROL_AF_STATE);

                            if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                                    || CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState
                                    || CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED == afState) {
                                //do something like save picture
                            }
                            break;
                    }
                }

            };

    /**
     * Monitor Back button:
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Log.e("onKeyDown", "back button onKeyDown()");
            closeCamera();
            finish();
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }

    }


    private void closeCamera() {
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }


    public void onCapture(View view) {
        try {
            Log.d(TAG, "take picture");
            mState = STATE_WAITING_CAPTURE;
            mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), mSessionCaptureCallback, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

}
