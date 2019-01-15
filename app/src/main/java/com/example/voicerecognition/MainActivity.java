package com.example.voicerecognition;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.text.StringPrepParseException;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_READ_EXTERNAL_STORAGE = 111;
    private static final int REQUEST_SELECT_DEVICE = 222;

    private ArrayList<String> imgPaths;
    private FaceDetectionActivity faceDetectionActivity;
    private GridView gridView;
    private SpeechRecognize speech;
    private EporConnection eporConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridView = (GridView) findViewById(R.id.gridView);
        faceDetectionActivity = new FaceDetectionActivity();

        eporConnection = new EporConnection(this);

        speech = new SpeechRecognize(this, eporConnection);

        checkPermission(this);

        findViewById(R.id.floatingButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (eporConnection.getConnectedTask() != null) {
                    speech.startListen();
                } else {
                    Toast.makeText(MainActivity.this, "매드봇을 연결해주세요", Toast.LENGTH_SHORT).show();
                }
                // startActivityForResult(recognizerIntent, SpeechRecognize.RESULT_SPEECH);
            }
        });

        findViewById(R.id.previewButton).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, PreviewActivity.class);
                startActivity(i);
            }
        });

        findViewById(R.id.connectButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent selectDevice = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(selectDevice, REQUEST_SELECT_DEVICE);
            }
        });

    }

    private void initGridView() {
        imgPaths = getImagesPath(this);

        gridView.setAdapter(new ImageAdapter(this, imgPaths));

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                String selectedImagePath = imgPaths.get(position);

                Intent intent = new Intent(MainActivity.this, FaceDetectionActivity.class);
                intent.putExtra("imgPath", selectedImagePath);
                startActivity(intent);
            }
        });
    }

    private void checkPermission(Activity activity) {

        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_READ_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            initGridView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    initGridView();


                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    protected ArrayList<String> getImagesPath(Activity activity) {
        Uri uri;
        ArrayList<String> imageList = new ArrayList<String>();
        Cursor cursor;
        int column_index_data;

        String PathOfImage = null;
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = { MediaStore.MediaColumns.DATA };

        cursor = activity.getContentResolver().query(uri, projection, null,
                null, null);

        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

        while (cursor.moveToNext()) {
            PathOfImage = cursor.getString(column_index_data);
            imageList.add(PathOfImage);
        }
        return imageList;
    }


    private void consumeRequestDeviceSelect(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        BluetoothDevice device = data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        eporConnection.saveAsDefault(device);
        eporConnection.onDeviceSelected(device);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_SELECT_DEVICE : {
                consumeRequestDeviceSelect(resultCode, data);
                break;
            }
        }
    }
}
