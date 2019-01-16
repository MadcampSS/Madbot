package com.example.voicerecognition.epor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Timer;
import java.util.TimerTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.example.voicerecognition.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class EporConnection implements RejectedExecutionHandler {

    private static final String TAG = "EporConnection";

    // Command variables
    private int CMDNOUN = 0x0000;
    private int CMDVERB = 0x0000;
    private boolean CMDVALID = false;

    private boolean dancing = false;

    // Controller variables
    private byte[] CMDbuffer = new byte[10];
    int RGBon_off_flag=0;

    public static byte MOTOR_SPEED = 0;
    public static byte SEND_TXDATA = 1;
    public static byte SERVO_ANGLE = 3;
    public static byte ANALOG_WRITE = 4;
    public static byte DIGITAL_WRITE = 5;
    public static byte RGB_WRITE = 6;
    public static byte LCD_WRITE = 7;

    private int ServoHead = 90;
    private int ServoArm1 = 90;
    private int ServoArm2 = 90;

    private int count = 0;

    TimerTask dance = new TimerTask() {
        @Override
        public void run() {
            switch(count) {
                case 0:
                    ServoHead = 170;
                    ServoArm1 = 170;
                    ServoArm2 = 170;
                    setMotorSpeed(170,-170);
                    setRGBLed(255, 0, 0);

                case 1:
                    ServoHead = 10;
                    ServoArm1 = 10;
                    ServoArm2 = 10;
                    setMotorSpeed(-170,170);
                    setRGBLed(0, 255, 0);

                case 2:
                    ServoHead = 90;
                    ServoArm1 = 170;
                    ServoArm2 = 10;
                    setMotorSpeed(-170, -170);
                    setRGBLed(0, 0, 255);

                case 3:
                    ServoHead = 90;
                    ServoArm1 = 10;
                    ServoArm2 = 170;
                    setMotorSpeed(170, 170);
                    setRGBLed(255, 255, 255);

            }
            setServoAngle(ServoHead, ServoArm1, ServoArm2);
            count++;
            count %= 4;
        }
    };
    Timer timer;


    private static final String RECENT_DEVICE = "recent_device";

    private BluetoothAdapter mBluetooth;
    private ConnectedTask mConnectedTask;
    private ExecutorService mExec;
    private ReentrantLock mLock = new ReentrantLock();

    private Context mContext;

    public ConnectedTask getConnectedTask() {
        return mConnectedTask;
    }

    public EporConnection(Context context) {
        // Robot settings
        mExec = Executors.newCachedThreadPool();
        ((ThreadPoolExecutor) mExec).setRejectedExecutionHandler(this);
        mBluetooth = BluetoothAdapter.getDefaultAdapter();
        mContext = context;
    }

    public void sendCommand(String command) {
        CMDNOUN = 0;
        CMDVERB = 0;
        CMDVALID = false;

        // NOUN
        if(command.contains("팔")) {
            CMDNOUN |= 0x01;
            CMDVALID = true;
        }
        if(command.contains("머리")) {
            CMDNOUN |= 0x02;
            CMDVALID = true;
        }
        if(command.contains("불")) {
            CMDNOUN |= 0x04;
            CMDVALID = true;
        }
        // VERB
        if(command.contains("정지")) {
            CMDVERB |= 0x0001;
            CMDVALID = true;
        }
        if(command.contains("왼") || command.contains("좌회전")) {
            CMDVERB |= 0x0002;
            CMDVALID = true;
        }
        if(command.contains("오른") || command.contains("우회전")) {
            CMDVERB |= 0x0004;
            CMDVALID = true;
        }
        if(command.contains("밑")) {
            CMDVERB |= 0x0008;
            CMDVALID = true;
        }
        if(command.contains("뒤") || command.contains("후진")) {
            CMDVERB |= 0x0010;
            CMDVALID = true;
        }
        if(command.contains("앞") || command.contains("전진")) {
            CMDVERB |= 0x0020;
            CMDVALID = true;
        }

        // Translating

        if ((CMDVERB & 0x0001) != 0) {
            Log.d("CMDVERB", "Stop");
            setMotorSpeed(0,0);
            setServoAngle(ServoHead, ServoArm1, ServoArm2);
            return;
        }

        if (CMDVALID) {
            if (CMDNOUN == 0x01) {
                if ((CMDVERB & 0x0002) != 0) {
                    if ((CMDVERB & 0x0008) != 0) {
                        Log.d("CMDVERB", "Left Arm Bottom");
                        ServoArm2 = 90;
                    }
                    if ((CMDVERB & 0x0010) != 0) {
                        Log.d("CMDVERB", "Left Arm Back");
                        ServoArm2 = 170;
                    }
                    if ((CMDVERB & 0x0020) != 0) {
                        Log.d("CMDVERB", "Left Arm Front");
                        ServoArm2 = 10;
                    }
                }
                else if ((CMDVERB & 0x0004) != 0) {
                    if ((CMDVERB & 0x0008) != 0) {
                        Log.d("CMDVERB", "Right Arm Bottom");
                        ServoArm1 = 90;
                    }
                    if ((CMDVERB & 0x0010) != 0) {
                        Log.d("CMDVERB", "Right Arm Back");
                        ServoArm1 = 10;
                    }
                    if ((CMDVERB & 0x0020) != 0) {
                        Log.d("CMDVERB", "Right Arm Front");
                        ServoArm1 = 170;
                    }
                }
                else {
                    if ((CMDVERB & 0x0008) != 0) {
                        Log.d("CMDVERB", "Both Arm Bottom");
                        ServoArm1 = 90;
                        ServoArm2 = 90;
                    }
                    if ((CMDVERB & 0x0010) != 0) {
                        Log.d("CMDVERB", "Both Arm Back");
                        ServoArm1 = 10;
                        ServoArm2 = 170;
                    }
                    if ((CMDVERB & 0x0020) != 0) {
                        Log.d("CMDVERB", "Both Arm Front");
                        ServoArm1 = 170;
                        ServoArm2 = 10;
                    }
                }
                setServoAngle(ServoHead, ServoArm1, ServoArm2);
            }
            else if (CMDNOUN == 0x02) {
                if ((CMDVERB & 0x0002) != 0) {
                    Log.d("CMDVERB", "Head Left");
                    ServoHead = 170;
                }
                if ((CMDVERB & 0x0004) != 0) {
                    Log.d("CMDVERB", "Head Right");
                    ServoHead = 10;
                }
                if ((CMDVERB & 0x0020) != 0) {
                    Log.d("CMDVERB", "Head Front");
                    ServoHead = 90;
                }
                setServoAngle(ServoHead, ServoArm1, ServoArm2);
            }
            else if (CMDNOUN == 0x04) {

            }
            else {
                if (CMDVERB == 0x0002) {
                    Log.d("CMDVERB", "Left");
                    setMotorSpeed(170,-170);
                }
                if (CMDVERB == 0x0004) {
                    Log.d("CMDVERB", "Right");
                    setMotorSpeed(-170,170);
                }
                if (CMDVERB == 0x0010) {
                    Log.d("CMDVERB", "Backward");
                    setMotorSpeed(-170, -170);
                }
                if (CMDVERB == 0x0020) {
                    Log.d("CMDVERB", "Forward");
                    setMotorSpeed(170, 170);
                }
                if (CMDVERB == 0x0012) {
                    Log.d("CMDVERB", "Backward Left");
                    setMotorSpeed(-250, -170);
                }
                if (CMDVERB == 0x0014) {
                    Log.d("CMDVERB", "Backward Right");
                    setMotorSpeed(-170, -250);
                }
                if (CMDVERB == 0x0022) {
                    Log.d("CMDVERB", "Forward Left");
                    setMotorSpeed(250,170);
                }
                if (CMDVERB == 0x0024) {
                    Log.d("CMDVERB", "Forward Right");
                    setMotorSpeed(170,250);
                }
            }
        }
    }

    public void textToCommand(String text) {

        String command = text;

        CMDNOUN = 0;
        CMDVERB = 0;
        CMDVALID = false;

        // VALIDATION
        if(command.contains("로봇"))
            CMDVALID = true;

        // NOUN
        if(command.contains("팔"))
            CMDNOUN |= 0x01;
        if(command.contains("머리") || command.contains("고개"))
            CMDNOUN |= 0x02;
        if(command.contains("불"))
            CMDNOUN |= 0x04;

        // VERB
        if(command.contains("멈춰") || command.contains("정지") || command.contains("스톱"))
            CMDVERB |= 0x0001;
        if(command.contains("왼") || command.contains("좌측") || command.contains("좌회전"))
            CMDVERB |= 0x0002;
        if(command.contains("오른") || command.contains("우측") || command.contains("우회전"))
            CMDVERB |= 0x0004;
        if(command.contains("아래") || command.contains("밑"))
            CMDVERB |= 0x0008;
        if(command.contains("뒤") || command.contains("후방") || command.contains("후진"))
            CMDVERB |= 0x0010;
        if(command.contains("앞") || command.contains("전방") || command.contains("전진"))
            CMDVERB |= 0x0020;
        if(command.contains("춤"))
            CMDVERB |= 0x0040;

        // Translating

        if ((CMDVERB & 0x0001) != 0) {
            Log.d("CMDVERB", "Stop");
            setMotorSpeed(0,0);
            setServoAngle(ServoHead, ServoArm1, ServoArm2);
            if(dancing) {
                timer.cancel();
                timer = null;
                dancing = false;
            }

            return;
        }

        if (CMDVALID) {
            if (CMDNOUN == 0x01) {
                if ((CMDVERB & 0x0002) != 0) {
                    if ((CMDVERB & 0x0008) != 0) {
                        Log.d("CMDVERB", "Left Arm Bottom");
                        ServoArm2 = 90;
                    }
                    if ((CMDVERB & 0x0010) != 0) {
                        Log.d("CMDVERB", "Left Arm Back");
                        ServoArm2 = 170;
                    }
                    if ((CMDVERB & 0x0020) != 0) {
                        Log.d("CMDVERB", "Left Arm Front");
                        ServoArm2 = 10;
                    }
                }
                else if ((CMDVERB & 0x0004) != 0) {
                    if ((CMDVERB & 0x0008) != 0) {
                        Log.d("CMDVERB", "Right Arm Bottom");
                        ServoArm1 = 90;
                    }
                    if ((CMDVERB & 0x0010) != 0) {
                        Log.d("CMDVERB", "Right Arm Back");
                        ServoArm1 = 10;
                    }
                    if ((CMDVERB & 0x0020) != 0) {
                        Log.d("CMDVERB", "Right Arm Front");
                        ServoArm1 = 170;
                    }
                }
                else {
                    if ((CMDVERB & 0x0008) != 0) {
                        Log.d("CMDVERB", "Both Arm Bottom");
                        ServoArm1 = 90;
                        ServoArm2 = 90;
                    }
                    if ((CMDVERB & 0x0010) != 0) {
                        Log.d("CMDVERB", "Both Arm Back");
                        ServoArm1 = 10;
                        ServoArm2 = 170;
                    }
                    if ((CMDVERB & 0x0020) != 0) {
                        Log.d("CMDVERB", "Both Arm Front");
                        ServoArm1 = 170;
                        ServoArm2 = 10;
                    }
                }
                setServoAngle(ServoHead, ServoArm1, ServoArm2);
            }
            else if (CMDNOUN == 0x02) {
                if ((CMDVERB & 0x0002) != 0) {
                    Log.d("CMDVERB", "Head Left");
                    ServoHead = 170;
                }
                if ((CMDVERB & 0x0004) != 0) {
                    Log.d("CMDVERB", "Head Right");
                    ServoHead = 10;
                }
                if ((CMDVERB & 0x0020) != 0) {
                    Log.d("CMDVERB", "Head Front");
                    ServoHead = 90;
                }
                setServoAngle(ServoHead, ServoArm1, ServoArm2);
            }
            else if (CMDNOUN == 0x04) {

            }
            else {
                if (CMDVERB == 0x0002) {
                    Log.d("CMDVERB", "Left");
                    setMotorSpeed(170,-170);
                }
                if (CMDVERB == 0x0004) {
                    Log.d("CMDVERB", "Right");
                    setMotorSpeed(-170,170);
                }
                if (CMDVERB == 0x0010) {
                    Log.d("CMDVERB", "Backward");
                    setMotorSpeed(-170, -170);
                }
                if (CMDVERB == 0x0020) {
                    Log.d("CMDVERB", "Forward");
                    setMotorSpeed(170, 170);
                }
                if (CMDVERB == 0x0012) {
                    Log.d("CMDVERB", "Backward Left");
                    setMotorSpeed(-250, -170);
                }
                if (CMDVERB == 0x0014) {
                    Log.d("CMDVERB", "Backward Right");
                    setMotorSpeed(-170, -250);
                }
                if (CMDVERB == 0x0022) {
                    Log.d("CMDVERB", "Forward Left");
                    setMotorSpeed(250,170);
                }
                if (CMDVERB == 0x0024) {
                    Log.d("CMDVERB", "Forward Right");
                    setMotorSpeed(170,250);
                }
            }

            if(CMDVERB == 0x0040) {
                Log.d("DEBUG", "Dancing?!");
                dancing = true;
                timer = new Timer();
                timer.schedule(dance, 0, 1000);

            }
        }
    }

    public void onDeviceSelected(BluetoothDevice device) {
        mLock.lock();
        try {
            if (mConnectedTask != null) {
                mConnectedTask.cancel();
                mConnectedTask = null;
            }
        } finally {
            mLock.unlock();
        }
        ((Activity) mContext).runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mContext, "연결중", Toast.LENGTH_SHORT).show();
            }
        });
        ConnectTask task = new ConnectTask(device,
                Constants.SERIAL_PORT_PROFILE);
        Cancelable canceller = new CancellingTask(mExec, task, 10,
                TimeUnit.SECONDS);
        mExec.execute(canceller);
    }
    public void saveAsDefault(BluetoothDevice device) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        String addr = device.getAddress();
        editor.putString(RECENT_DEVICE, addr);
        editor.apply();
    }

    void SendXBOTcmd(byte cmd, byte d0, byte d1, byte d2, byte d3, byte d4)
    {
        CMDbuffer[0] = (byte)'X';
        CMDbuffer[1] = (byte)'R';
        CMDbuffer[2] = cmd;
        CMDbuffer[3] = d0;
        CMDbuffer[4] = d1;
        CMDbuffer[5] = d2;
        CMDbuffer[6] = d3;
        CMDbuffer[7] = d4;
        CMDbuffer[8] = (byte)'S';
    }

    void setMotorSpeed(int speed1, int speed2)
    {

        if(mConnectedTask != null){
            SendXBOTcmd(MOTOR_SPEED, (byte)((speed1 & 0xFF00) >> 8), (byte)(speed1 & 0xFF),(byte)((speed2 & 0xFF00) >> 8), (byte)(speed2 & 0xFF), (byte)0);
            mConnectedTask.SendData(CMDbuffer, 0, 9);
        }
    }

    void setRGBLed(int rColor, int gColor, int bColor)
    {

        if(mConnectedTask != null)
        {
            //	sleep(300);
            SendXBOTcmd(RGB_WRITE, (byte)(255),(byte)(rColor),(byte)(gColor), (byte)(bColor), (byte)0);
            mConnectedTask.SendData(CMDbuffer, 0, 9);
        }
    }
    void setServoAngle(int angle1, int angle2, int angle3)
    {
        if(mConnectedTask != null){

            SendXBOTcmd(SERVO_ANGLE, (byte)(angle1), (byte)(angle2),(byte)(angle3), (byte)0, (byte)0);
            mConnectedTask.SendData(CMDbuffer, 0, 9);
        }
    }

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
        // Nothing
    }

    public void destroy() {
        mExec.shutdownNow();
    }

    private final class ConnectedTask implements Cancelable {
        private final AtomicBoolean mmClosed = new AtomicBoolean();
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final BluetoothSocket mmSocket;
        public ConnectedTask(BluetoothSocket socket) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(Constants.TAG, "sockets not created", e);
            }
            mmSocket = socket;
            mmInStream = in;
            mmOutStream = out;
        }
        public void cancel() {
            if (mmClosed.getAndSet(true)) {
                return;
            }
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(Constants.TAG, "close failed", e);
            }
        }
        public void SendData(byte[] data, int offset, int count){
            if (mmSocket != null && mmOutStream != null) {
                try {

                    //���� ����
                    byte[] bytes_len = ShortToBytes((short)count);
                    mmOutStream.write(bytes_len);
                    mmOutStream.flush();

                    mmOutStream.write(data, offset, count);
                    mmOutStream.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        public void SendData(byte[] data){
            if (mmSocket != null && mmOutStream != null) {
                try {

                    //���� ����
                    byte[] bytes_len = ShortToBytes((short)data.length);
                    mmOutStream.write(bytes_len);
                    mmOutStream.flush();

                    mmOutStream.write(data);
                    mmOutStream.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        short[] BytesToShorts(byte[] bytes) {
            int b_len = bytes.length;
            int s_len = b_len / 2;
            short[] shorts = new short[s_len];
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    .get(shorts);
            return shorts;
        }
        byte[] ShortsToBytes(short[] shorts) {
            int s_len = shorts.length;
            int b_len = s_len * 2;
            byte[] bytes = new byte[b_len];
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    .put(shorts);
            return bytes;
        }

        byte[] ShortToBytes(short val) {
            return ShortsToBytes(new short[] { val });
        }
        public void run() {
            InputStream in = mmInStream;
            byte[] buffer = new byte[Constants.BUFFER_SIZE];
            int count;
            while (!mmClosed.get()) {
                try {
                    count = in.read(buffer);
                    received(buffer, 0, count);
                } catch (IOException e) {
                    connectionLost(e);
                    cancel();
                    break;
                }
            }
        }
        /*
         * public void write(byte[] buffer) { try { mmOutStream.write(buffer); }
         * catch (IOException e) { Log.e(Constants.TAG, "write failed", e); } }
         */
        void connectionLost(IOException e) {
        }
        void received(byte[] buffer, int offset, int count) {
            String str = new String(buffer, offset, count);
            ((MainActivity) mContext).serialPort_DataReceived(buffer, count);
        }
    }
    private final class ConnectTask implements Cancelable {
        private final AtomicBoolean mmClosed = new AtomicBoolean();
        private final BluetoothSocket mmSocket;
        public ConnectTask(BluetoothDevice device, UUID uuid) {
            BluetoothSocket socket = null;
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(Constants.TAG, "create failed", e);
            }
            mmSocket = socket;
        }
        public void cancel() {
            if (mmClosed.getAndSet(true)) {
                return;
            }
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(Constants.TAG, "close failed", e);
            }
        }
        public void run() {
            if (mBluetooth.isDiscovering()) {
                mBluetooth.cancelDiscovery();
            }
            try {
                mmSocket.connect();
                connected(mmSocket);
            } catch (IOException e) {
                connectionFailed(e);
                cancel();
            }
        }
        void connected(BluetoothSocket socket) {
            mLock.lock();
            try {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(mContext, "매드봇이 연결되었습니다", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.i(TAG, "Madbot connected");
                final ConnectedTask task = new ConnectedTask(socket);
                Cancelable canceller = new CancellingTask(mExec, task);
                mExec.execute(canceller);
                mConnectedTask = task;
            } finally {
                mLock.unlock();
            }
        }
        void connectionFailed(IOException e) {
            // dumpMessage(e.getLocalizedMessage());
        }
    }
}



//    private BluetoothDevice loadDefault() {
//        SharedPreferences prefs = PreferenceManager
//                .getDefaultSharedPreferences(mContext);
//        String addr = prefs.getString(RECENT_DEVICE, null);
//        if (addr == null) {
//            return null;
//        }
//        BluetoothDevice device = mBluetooth.getRemoteDevice(addr);
//        return device;
//    }