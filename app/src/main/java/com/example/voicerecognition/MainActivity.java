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


    private static final int REQUEST_SELECT_DEVICE = 222;

    private SpeechRecognize speech;
    private EporConnection eporConnection;

    private static final boolean isDebugging = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        eporConnection = new EporConnection(this);

        speech = new SpeechRecognize(this, eporConnection);


        findViewById(R.id.connectButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent selectDevice = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(selectDevice, REQUEST_SELECT_DEVICE);
            }
        });

        findViewById(R.id.streamButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, StreamActivity.class);
                startActivity(i);
            }
        });

        findViewById(R.id.speechButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((eporConnection.getConnectedTask() != null) || isDebugging) {
                    speech.startListen();
                } else {
                    Toast.makeText(MainActivity.this, "매드봇을 연결해주세요", Toast.LENGTH_SHORT).show();
                }
                // startActivityForResult(recognizerIntent, SpeechRecognize.RESULT_SPEECH);
            }
        });

        findViewById(R.id.galleryButton).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, GalleryActivity.class);
                startActivity(i);
            }
        });

        findViewById(R.id.previewButton).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, PreviewActivity.class);
                startActivity(i);
            }
        });

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
