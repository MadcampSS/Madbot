package com.example.voicerecognition;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.firebase.ml.common.FirebaseMLException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PreviewActivity extends AppCompatActivity {

    private static final String TAG = "PreviewActivity";
    private static final String FACE_DETECTION = "Face Detection";
    private static final int CAMERA_FACING_BACK = 0;
    private static final int CAMERA_FACING_FRONT = 1;
    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private String selectedModel = FACE_DETECTION;

    private int cameraFacing =  CAMERA_FACING_BACK;

    private static final int PERMISSION_REQUESTS = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        preview = findViewById(R.id.firePreview);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = findViewById(R.id.fireFaceOverlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }


        FloatingActionButton facingSwitch = findViewById(R.id.facingSwitch);
        facingSwitch.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(cameraSource != null) {
                    try {
                        if (cameraFacing == CAMERA_FACING_BACK) {
                            cameraFacing = CAMERA_FACING_FRONT;
                            cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
                        } else {
                            cameraFacing = CAMERA_FACING_BACK;
                            cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
                        }
                    } catch (IllegalAccessException e) {
                        Log.e(TAG,"Error on switching camera: "+ e);
                    }
                }
                preview.stop();
                startCameraSource();
            }
        });

        if(Camera.getNumberOfCameras() == 1) {
            facingSwitch.setVisibility(View.GONE);
        }

        if(allPermissionGranted()) {
            createCameraSource(selectedModel);
        } else {
            getRuntimePermission();
        }

    }

    private void createCameraSource(String model) {
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }

        switch (model) {
            case FACE_DETECTION:
                cameraSource.setMachineLearningFrameProcessor(new FaceDetectionProcessor());
                break;
            default:
                Log.e(TAG, "Unknown model: " + model);
        }
    }

    private void startCameraSource() {
        Log.d(TAG, "START CAMERA SOURCE");
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphicOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source", e);
                cameraSource.release();
                cameraSource = null;
            }
        } else {
            Log.d(TAG, "NO CAMERA SOURCE");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    private String[] getRequiredPermission() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                    .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionGranted() {
        for (String permission : getRequiredPermission()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private void getRuntimePermission() {
        List<String> allNeededPermission = new ArrayList<>();
        for (String permission : getRequiredPermission()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermission.add(permission);
            }
        }

        if(!allNeededPermission.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermission.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }
}
