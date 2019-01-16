package com.example.voicerecognition;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voicerecognition.chatbot.SpeechRecognize;
import com.example.voicerecognition.chatbot.ChatBot;
import com.example.voicerecognition.epor.DeviceListActivity;
import com.example.voicerecognition.epor.EporConnection;

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
    private ImageView sight;

    public static ArrayAdapter<String> chatArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        eporConnection = new EporConnection(this);
        chatBot = new ChatBot(this, eporConnection);


        speech = new SpeechRecognize(this, eporConnection);

        listView = (ListView) findViewById(R.id.listView);
        editText = (EditText) findViewById(R.id.editText);
        sendButton = (Button) findViewById(R.id.sendButton);

        header[0] = (byte) 'R';
        header[1] = (byte) 'X';
        header[2] = (byte) '=';
        // edit1 = (EditText) findViewById(R.id.editText1);
        // edit2 = (EditText) findViewById(R.id.editText2);
        sight = (ImageView) findViewById(R.id.eporeye);

        setupChat();

        final MediaPlayer carBeep = MediaPlayer.create(this, R.raw.carbeep);

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
                carBeep.start();
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
//        eporConnection.textToCommand(message);
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

    // BCam//
    int MaxBuffer = 5000;
    byte[] buff = new byte[MaxBuffer];
    int MaxFIFO = 100000;
    byte[] m_pBuf = new byte[MaxFIFO];
    int frameCount = 0;
    int m_receiveMode = 0;
    int addr = 0;
    int ImageSize = 0;
    byte[] header = new byte[3];
    byte[] adcValue = new byte[6];
    byte[] RxValue = new byte[3];
    // byte DValue = 0x00;



    public int ByteIndexOf(byte[] searched, byte[] find, int start, int end) {
        // Do standard error checking here.
        Boolean matched = false;
        for (int index = start; index <= end - find.length; ++index) {
            // Assume the values matched.
            matched = true;
            // Search in the values to be found.
            for (int subIndex = 0; subIndex < find.length; ++subIndex) {
                // Check the value in the searched array vs the value
                // in the find array.
                if (find[subIndex] != searched[index + subIndex]) {
                    // The values did not match.
                    matched = false;
                    // Break out of the loop.
                    break;
                }
            }
            // If the values matched, return the index.
            if (matched) {
                // Return the index.
                return index;
            }
        }
        // None of the values matched, return -1.
        return -1;
    }

    byte[] BlockCopy(byte[] src, int start_ind, int length) {
        byte[] res = new byte[length];
        for (int i = 0; i < length; i++) {
            res[i] = src[i + start_ind];
        }
        return res;
    }

    final int CIRCLED_QUEUE_SIZE = 100000;
    byte[] _CQ_Array = new byte[CIRCLED_QUEUE_SIZE];
    int _s_array_ind = -1;
    int _e_array_ind = 0;
    void CQ_AddBytes(byte[] bytes, int count) {
        if (_s_array_ind < 0)
            _s_array_ind = 0;
        for (int i = 0; i < count; i++) {
            _CQ_Array[_e_array_ind] = bytes[i];
            _e_array_ind++;
            if (_e_array_ind >= CIRCLED_QUEUE_SIZE)
                _e_array_ind = 0;
        }
    }

    int CQ_GetLength() {
        if (_s_array_ind < 0 || _e_array_ind < 0)
            return 0;
        else if (_e_array_ind >= _s_array_ind)
            return (_e_array_ind - _s_array_ind);
        else {
            int len1 = CIRCLED_QUEUE_SIZE - _s_array_ind;
            int len2 = _e_array_ind;
            return len1 + len2;
        }
    }

    byte[] CQ_GetData(int length) {
        byte[] res = new byte[length];
        for (int i = 0; i < length; i++) {
            res[i] = _CQ_Array[_s_array_ind];
            _s_array_ind++;
            if (_s_array_ind >= CIRCLED_QUEUE_SIZE)
                _s_array_ind = 0;
        }
        return res;
    }

    byte[] CQ_GetData(int start_ind, int length) {
        byte[] res = new byte[length - start_ind];
        for (int i = 0; i < length; i++) {
            if (i >= start_ind)
                res[i - start_ind] = _CQ_Array[_s_array_ind];
            _s_array_ind++;
            if (_s_array_ind >= CIRCLED_QUEUE_SIZE)
                _s_array_ind = 0;
        }
        return res;
    }

    void CQ_RemoveData(int length) {
        for (int i = 0; i < length; i++) {
            _s_array_ind++;
            if (_s_array_ind >= CIRCLED_QUEUE_SIZE)
                _s_array_ind = 0;
        }
    }

    void CQ_ClearData() {
        _s_array_ind = -1;
        _e_array_ind = 0;
    }

    byte[] _FIFO_Array = new byte[CIRCLED_QUEUE_SIZE];
    int _s_fifo_ind = -1;
    int _e_fifo_ind = 0;
    void FIFO_AddBytes(byte[] bytes, int count) {
        if (_s_fifo_ind < 0)
            _s_fifo_ind = 0;
        for (int i = 0; i < count; i++) {
            _FIFO_Array[_e_fifo_ind] = bytes[i];
            _e_fifo_ind++;
            if (_e_fifo_ind >= CIRCLED_QUEUE_SIZE)
                _e_fifo_ind = 0;
        }
    }

    int FIFO_GetLength() {
        if (_s_fifo_ind < 0 || _e_fifo_ind < 0)
            return 0;
        else if (_e_fifo_ind >= _s_fifo_ind)
            return (_e_fifo_ind - _s_fifo_ind);
        else {
            int len1 = CIRCLED_QUEUE_SIZE - _s_fifo_ind;
            int len2 = _e_fifo_ind;
            return len1 + len2;
        }
    }

    byte[] FIFO_GetData(int start_ind, int length) {
        byte[] res = new byte[length - start_ind];
        for (int i = 0; i < length; i++) {
            if (i >= start_ind)
                res[i - start_ind] = _FIFO_Array[_s_fifo_ind];
            _s_fifo_ind++;
            if (_s_fifo_ind >= CIRCLED_QUEUE_SIZE)
                _s_fifo_ind = 0;
        }
        return res;
    }

    byte[] FIFO_GetData(int length) {
        byte[] res = new byte[length];
        for (int i = 0; i < length; i++) {
            res[i] = _FIFO_Array[_s_fifo_ind];
            _s_fifo_ind++;
            if (_s_fifo_ind >= CIRCLED_QUEUE_SIZE)
                _s_fifo_ind = 0;
        }
        return res;
    }

    void FIFO_RemoveData(int length) {
        for (int i = 0; i < length; i++) {
            _s_fifo_ind++;
            if (_s_fifo_ind >= CIRCLED_QUEUE_SIZE)
                _s_fifo_ind = 0;
        }
    }

    void FIFO_ClearData() {
        _s_fifo_ind = -1;
        _e_fifo_ind = 0;
    }

    public void serialPort_DataReceived(byte[] recv_buff, int recv_count) {
        try {
            CQ_AddBytes(recv_buff, recv_count);
            int fSize = 0;
            if (m_receiveMode == 0) {
                if (CQ_GetLength() < 50)
                    return;
                fSize = CQ_GetLength();
                buff = CQ_GetData(fSize);
                int index = ByteIndexOf(buff, header, 0, fSize);
                if (index != -1) {
                    ImageSize = 0;
                    ImageSize = (int) (buff[index + 3] & 0x00ff) << 8;
                    ImageSize = ImageSize | (buff[index + 4] & 0xff);
                    addr = 0;
                    int imageBase = index + 5;
                    byte[] temp_buff = BlockCopy(buff, imageBase, fSize
                            - imageBase);
                    FIFO_AddBytes(temp_buff, fSize - imageBase);
                    addr += (fSize - imageBase);
                    m_receiveMode = 1;
                }
            } else if (m_receiveMode == 1) {
                if (addr < ImageSize) {
                    fSize = CQ_GetLength();
                    byte[] temp_buff = CQ_GetData(fSize);
                    FIFO_AddBytes(temp_buff, fSize);
                    addr += fSize;
                }
                if (addr >= ImageSize) {
                    m_pBuf = FIFO_GetData(FIFO_GetLength());
                    frameCount++;
                    m_receiveMode = 0;
                    for (int i = 0; i < 5; i++)
                        adcValue[i] = m_pBuf[i];
                    adcValue[5] = m_pBuf[ImageSize - 5];
                    RxValue[0] = m_pBuf[ImageSize - 4];
                    RxValue[1] = m_pBuf[ImageSize - 3];
                    RxValue[2] = m_pBuf[ImageSize - 2];
                    String data_str = String
                            .format("ADC data [ %d %d %d %d %d %d ] \r\nRX serial data [ %d %d %d]",
                                    (int) adcValue[0], (int) adcValue[1],
                                    (int) adcValue[2], (int) adcValue[3],
                                    (int) adcValue[4], (int) adcValue[5],
                                    RxValue[0], RxValue[1], RxValue[2]);
                    // LogInfo(data_str);
                    ShowJpegData();
                }
            }
        } catch (final Exception ex) {
            LogInfo(ex.toString());
        }
    }


    private void LogInfo(final String log) {
        Runnable dumpTask = new Runnable() {
            public void run() {
                //edit2.setText(log);
                Log.d("DEBUG", log);
            }
        };
        runOnUiThread(dumpTask);
    }
    private void ShowJpegData() {
        try {
            byte[] PictureData = new byte[ImageSize - 10];
            PictureData = BlockCopy(m_pBuf, 5, ImageSize - 10);
            final Bitmap bmp = BitmapFactory.decodeByteArray(PictureData, 0,
                    PictureData.length);
            Runnable dumpTask = new Runnable() {
                public void run() {
                    // Drawable old = image1.getDrawable();
                    sight.setImageBitmap(bmp);
                    // old = null;
                }
            };
            runOnUiThread(dumpTask);
        } catch (Exception ex) {
            LogInfo(ex.toString());
        }
    }

}
