package com.sonuauti.facespoof;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private final int PERMISSIONS_REQUEST_CODE = 444;
    String TAG = "spoof";
    TextView toptext;
    private int CameraIndex = 1;
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat mRgba, mGray;
    private WindowManager windowManager;

    private ClassifyManager.ResultCallback resultCallback = new ClassifyManager.ResultCallback() {
        @Override
        public void onResult(final boolean isFake) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isFake) {
                        toptext.setVisibility(View.VISIBLE);
                        toptext.setText("FACE IS REAL");
                        toptext.setBackground(getResources().getDrawable(R.drawable.rount_bg));
                    } else {
                        toptext.setVisibility(View.VISIBLE);
                        toptext.setText("FACE IS FAKE");
                        toptext.setBackground(getResources().getDrawable(R.drawable.round_bg_fake));
                    }
                }
            });
        }
    };

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    toggleCameraView(true);
                    ClassifyManager.getInstance().loadModel(getApplicationContext(), "spoof.xml");
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        toptext = findViewById(R.id.toptext);
        //toptext.setVisibility(View.INVISIBLE);

        CameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_preview);
        mOpenCvCameraView.setCameraIndex(CameraIndex);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraPermissionGranted();
        //mOpenCvCameraView.setMaxFrameSize(720,1400);

        mOpenCvCameraView.setCvCameraViewListener(this);
        //toptext.setVisibility(View.INVISIBLE);

    }

    private void toggleCameraView(boolean isView) {
        if (isView) {
            if (mOpenCvCameraView != null) {
                mOpenCvCameraView.setVisibility(View.VISIBLE);
                mOpenCvCameraView.enableView();
            }
        } else {
            if (mOpenCvCameraView != null)
                mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadOpenCV();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission required!", Toast.LENGTH_LONG);
                    finish();
                }
        }
    }

    private void loadOpenCV() {
        if (!OpenCVLoader.initDebug(false)) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgbImg = inputFrame.rgba();
        Mat grayImg = inputFrame.gray();

        // Flip image to get mirror effect
        int orientation = getScreenOrientation();
        //Log.d(TAG, "Orientation " + orientation);

        switch (orientation) { // RGB image
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                if (CameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT)
                    Core.flip(rgbImg, rgbImg, 0); // Flip along x-axis
                else
                    Core.flip(rgbImg, rgbImg, -1); // Flip along both axis
                break;
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                if (CameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT)
                    Core.flip(rgbImg, rgbImg, 1); // Flip along y-axis
                break;
        }

        switch (orientation) { // Grayscale image
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                Core.transpose(grayImg, grayImg); // Rotate image
                if (CameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT)
                    Core.flip(grayImg, grayImg, -1); // Flip along both axis
                else
                    Core.flip(grayImg, grayImg, 1); // Flip along y-axis
                break;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                Core.transpose(grayImg, grayImg); // Rotate image
                if (CameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK)
                    Core.flip(grayImg, grayImg, 0); // Flip along x-axis
                break;
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                if (CameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT)
                    Core.flip(grayImg, grayImg, 1); // Flip along y-axis
                break;
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                Core.flip(grayImg, grayImg, 0); // Flip along x-axis
                if (CameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK)
                    Core.flip(grayImg, grayImg, 1); // Flip along y-axis
                break;
        }


        if (grayImg != null) {
            if (grayImg.rows() > 0 && grayImg.cols() > 0) {
                if (ClassifyManager.getInstance().detectFaces(grayImg)) {
                    //Log.d(TAG,"Face  detected");
                    double[] vector = ClassifyManager.getInstance().getFeatureVector(grayImg);
                    ClassifyManager.getInstance().classifiyInput(vector, resultCallback);
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toptext.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }
        }

        mGray = grayImg;
        return rgbImg;
    }

    private void setDefaults() {
        windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
    }

    public int getScreenOrientation() {
        int rotation = windowManager.getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && height > width || (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && width > height) {
            //Log.d(TAG, "rotaion" + rotation);
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    Log.e(TAG, "Unknown screen orientation. Defaulting to portrait");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        } else { // If the device's natural orientation is landscape or if the device is square:
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    Log.e(TAG, "Unknown screen orientation. Defaulting to landscape");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setDefaults();

        if (Build.VERSION.SDK_INT >= 23) {
            // Request permission if needed
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            } else {
                loadOpenCV();
            }
        } else {
            loadOpenCV();
        }

    }

}
