package com.example.voicerecognition;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.voicerecognition.realtime.CameraSource;
import com.example.voicerecognition.realtime.CameraSourcePreview;
import com.example.voicerecognition.realtime.FaceDetectionProcessor;
import com.example.voicerecognition.realtime.GraphicOverlay;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
                cameraSource.setMachineLearningFrameProcessor(new FaceDetectionProcessor(this));
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

    private void notifyPhotoAdded(String photoPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(photoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    public void takeScreenshot() {
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        try {
            // image naming and path  to include sd card  appending name you choose for file
            final String mPath = Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera/" + now + ".jpg";

            preview.setDrawingCacheEnabled(true);
            final Bitmap bitmap = Bitmap.createBitmap(preview.getDrawingCache());
            preview.setDrawingCacheEnabled(false);

            final File imageFile = new File(mPath);

            if(ActivityCompat.checkSelfPermission(PreviewActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                FileOutputStream outputStream = new FileOutputStream(imageFile);
                int quality = 100;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                outputStream.flush();
                outputStream.close();

                Toast.makeText(PreviewActivity.this, "Captured @ " +  mPath, Toast.LENGTH_SHORT).show();
                notifyPhotoAdded(mPath);
            } else {
                Dexter.withActivity(PreviewActivity.this)
                        .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .withListener(new MultiplePermissionsListener() {
                            @Override
                            public void onPermissionsChecked(MultiplePermissionsReport report) {
                                if (report.areAllPermissionsGranted()) {
                                    FileOutputStream outputStream = null;
                                    try {
                                        outputStream = new FileOutputStream(imageFile);
                                        int quality = 100;
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                                        outputStream.flush();
                                        outputStream.close();

                                        Toast.makeText(PreviewActivity.this, "Captured @ " +  mPath, Toast.LENGTH_SHORT).show();
                                        notifyPhotoAdded(mPath);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                } else if (report.isAnyPermissionPermanentlyDenied()) {
                                    Toast.makeText(PreviewActivity.this, "PERMISSION DENIED", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                                token.continuePermissionRequest();
                            }
                        }).check();
            }
        } catch (Throwable e) {
            // Several error may come out with file handling or DOM
            e.printStackTrace();
        }
    }
}
