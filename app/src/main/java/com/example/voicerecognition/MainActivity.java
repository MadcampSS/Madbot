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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    private static final int REQUEST_SELECT_DEVICE = 222;

    private SpeechRecognize speech;
    private EporConnection eporConnection;
    private ChatBot chatBot;

    private static final boolean isDebugging = true;

    // Layout Views
    private ListView listView;
    private EditText editText;
    private Button sendButton;

    public static ArrayAdapter<String> chatArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatBot = new ChatBot(this);
        eporConnection = new EporConnection(this);

        speech = new SpeechRecognize(this, eporConnection);

        listView = (ListView) findViewById(R.id.listView);
        editText = (EditText) findViewById(R.id.editText);
        sendButton = (Button) findViewById(R.id.sendButton);

        setupChat();

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

    private void setupChat() {

        // Initialize the array adapter for the conversation thread
        chatArrayAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.message);

        listView.setAdapter(chatArrayAdapter);

        // Initialize the compose field with a listener for the return key
        editText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String message = editText.getText().toString();
                if(!message.equals("")) {
                    sendMessage(message);
                }
            }
        });

    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    private void sendMessage(String message) {
        chatBot.setQuery(message).requestTask();
        eporConnection.textToCommand(message);
        chatArrayAdapter.add("나:  " + message);
        editText.setText("");
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
