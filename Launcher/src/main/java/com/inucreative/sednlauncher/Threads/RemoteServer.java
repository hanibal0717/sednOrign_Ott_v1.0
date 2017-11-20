package com.inucreative.sednlauncher.Threads;

import android.app.Instrumentation;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

import com.inucreative.sednlauncher.Activity.MainActivity;
import com.inucreative.sednlauncher.Activity.PlayerActivity;
import com.inucreative.sednlauncher.SednApplication;
import com.inucreative.sednlauncher.Util.LogUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by Jskim on 2016-06-23.
 */
public class RemoteServer {
    private final String TAG = "RemoteServer";


    static final int REMOCON_PORT = 7078;

    Context mContext;
    SednApplication mApp;

    Instrumentation mInstrumentation;

    InetAddress mClientAddr;

    private List<TCPConnection> tcpClientList;

    // 블루투스 지원
    private BluetoothAdapter btAdapter;
    private final BroadcastReceiver btReceiver;
    private List<BTConnection> btClientList;

    public RemoteServer(Context context) {
        mContext = context;
        mClientAddr = null;
        mApp = (SednApplication)((MainActivity)context).getApplication();

        mInstrumentation = new Instrumentation();

        // 블루투스
        btClientList = Collections.synchronizedList(new ArrayList<>());
        btReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    LogUtil.d("bluetooth state changed - " + state);
                    if(state == BluetoothAdapter.STATE_ON) {
                        LogUtil.d("bluetooth enabled");
                        initBluetooth();
                    }

                } else if(BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                    LogUtil.d("bluetooth pairing requested");
                    try {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        int pin=intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0);
                        byte[] pinBytes;
                        pinBytes = (""+pin).getBytes("UTF-8");
                        device.setPin(pinBytes);
                        device.setPairingConfirmation(true);
                        LogUtil.d("bluetooth paired");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    LogUtil.d("BT DISCONNECTED : " + device.getName());
                    for(int i = 0; i < btClientList.size(); i++) {
                        if(btClientList.get(i).getAddress().equals(device.getAddress())) {
                            LogUtil.d("Found at : " + i);
                            btClientList.get(i).close();
                            btClientList.remove(i);
                            break;
                        }
                    }
                }

            }
        };
        mContext.registerReceiver(btReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        mContext.registerReceiver(btReceiver, new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST));
        mContext.registerReceiver(btReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if(!btAdapter.isEnabled()) {
            LogUtil.d("Enabling Bluetooth.......");
            btAdapter.enable();
        } else {
            initBluetooth();
        }

        // TCP
        tcpClientList = Collections.synchronizedList(new ArrayList<>());
        TCPAcceptThread tcpAcceptThread = new TCPAcceptThread();
        tcpAcceptThread.start();
    }

    private void initBluetooth() {
        setSTBNameForBT();

        // 다른 장비들에게 보여지도록 설정 (팝업 없이 with root 권한)
        Method method;
        try {
            method = btAdapter.getClass().getMethod("setScanMode", int.class, int.class);
            method.invoke(btAdapter,BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,0);  // always
        }
        catch (Exception e){
            e.printStackTrace();
        }

        BTAcceptThread btAcceptThread = new BTAcceptThread();
        btAcceptThread.start();
    }

    class BTConnection {
        public BluetoothSocket mSocket;
        public InputStream mInStream;
        public OutputStream mOutStream;
        public BTInputThread mInputThread;

        public BTConnection(BluetoothSocket socket) {
            mSocket = socket;
            try {
                mInStream = socket.getInputStream();
                mOutStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mInputThread = new BTInputThread(mInStream);
            mInputThread.start();
        }

        public String getAddress() {
            return mSocket.getRemoteDevice().getAddress();
        }

        public void close() {
            try {
                mInStream.close();
                mOutStream.close();
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mInStream = null;
            mOutStream = null;
            mSocket = null;
            mInputThread = null;
        }
    }

    private class BTInputThread extends Thread {
        InputStream mInputStream;

        public BTInputThread(InputStream stream) {
            mInputStream = stream;
        }

        @Override
        public void run() {
            byte[] buf = new byte[1024];

            while(true) {
                try {
                    mInputStream.read(buf);
                    LogUtil.d("BT message - " + buf);
                    processRemoteCmd(buf);
                } catch (IOException e) {
                    e.printStackTrace();
                    LogUtil.d("BTInputThread finished");
                    break;
                }
            }
        }
    }
    private class BTAcceptThread extends Thread {
        private final BluetoothServerSocket mBTServerSocket;

        public BTAcceptThread() {
            LogUtil.d("BTAcceptThread()");
            BluetoothServerSocket tmp = null;
            try {
                tmp = btAdapter.listenUsingRfcommWithServiceRecord("SEDN STB", UUID.fromString(SednApplication.BLUETOOTH_UUID));
                LogUtil.d("bluetooth server socket created");
            } catch (Exception e) {
                e.printStackTrace();
            }
            mBTServerSocket = tmp;
        }

        @Override
        public void run() {
            if(mBTServerSocket == null) {
                LogUtil.d("Bluetooth Init failed");
                return;
            }

            while(true) {
                BluetoothSocket newSocket = null;
                try {
                    newSocket = mBTServerSocket.accept();
                    LogUtil.d("new bluetooth socket connected " + newSocket);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if(newSocket != null) {
                    // 같은 device가 있으면 제거한다.
                    for(int i = 0; i < btClientList.size(); i++) {
                        if(btClientList.get(i).getAddress().equals(newSocket.getRemoteDevice().getAddress())) {
                            btClientList.get(i).close();
                            btClientList.remove(i);
                        }
                    }
                    btClientList.add(new BTConnection(newSocket));

                    // todo: 현재 볼륨값을 전송한다.
                    sendCurrentVolumeToClient();

                    LogUtil.d("added to the bluetooth client list num - " + btClientList.size());
                }
            }
        }
    }

    private void processRemoteCmd(byte[] buf) {
        try {
            ByteArrayInputStream baos = new ByteArrayInputStream(buf);
            ObjectInputStream oos = new ObjectInputStream(baos);
            String cmd = (String) oos.readObject();
            LogUtil.d(cmd);

            if (cmd.equals("SednKeyCmd")) {
                Integer keyCode = (Integer) oos.readObject();

                // ghlee: 리모컨앱의 STOP 버튼을 누르면 GREEN KeyCode가 발생됨 (실제 리모컨도 동일)
                if(keyCode.equals(KeyEvent.KEYCODE_PROG_GREEN)) {
                    keyCode = KeyEvent.KEYCODE_MEDIA_STOP;
                }
                // ghlee: 리모컨앱의 뒤로감기 버튼을 누르면 RED KeyCode가 발생됨 (실제 리모컨은 REWIND 발생됨) - 2017.11.13
                else if(keyCode.equals(KeyEvent.KEYCODE_PROG_RED)) {
                    keyCode = KeyEvent.KEYCODE_MEDIA_REWIND;
                }

                // ghlee: Player 표시상태가 아니면 Mute 버튼 무시한다. 2017.11.14
                if(keyCode.equals(KeyEvent.KEYCODE_VOLUME_MUTE)) {
                    if(!(mApp.mCurrentContext instanceof PlayerActivity)) {
                        Log.d(TAG, "Mute - not PlayerActivity");
                        return;
                    }
                }

                mInstrumentation.sendKeyDownUpSync(keyCode);


                LogUtil.d(cmd + " : " + keyCode);
            } else if (cmd.equals("SednInputStr")) {
                final String inputStr = (String) oos.readObject();
                final EditText focusedEditText = ((MainActivity) mContext).focusedEditText;
                LogUtil.d(cmd + " : " + inputStr + ", " + focusedEditText);
                if (focusedEditText != null) {

                    focusedEditText.post(new Runnable() {
                        @Override
                        public void run() {
                            focusedEditText.setText(inputStr);
                            focusedEditText.setSelection(inputStr.length());
                            focusedEditText.invalidate();
                        }
                    });
                }
            } else if (cmd.equals("SednVolume")) {

                // Player 표시중에만 보륨조절 가능하도록 수정
                if(mApp.mCurrentContext instanceof PlayerActivity) {
                    // 리모컨에서는 0~100으로 전달됨
                    Integer volume = (Integer) oos.readObject();
                    LogUtil.d(cmd + " : " + volume);
                    ((SednApplication) (((MainActivity) mContext).getApplication())).setVolume(volume);
                    mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_FOCUS); // volume update를 KEYCODE_FOCUS 에 매핑한다.
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class TCPConnection {
        public Socket mSocket;
        public InputStream mInStream;
        public OutputStream mOutStream;
        public TCPInputThread mInputThread;

        public TCPConnection(Socket socket) {
            mSocket = socket;
            try {
                LogUtil.d("making connection");
                mInStream = socket.getInputStream();
                LogUtil.d("inputstream");
                mOutStream = socket.getOutputStream();
                LogUtil.d("outputstream");
            } catch (IOException e) {
                e.printStackTrace();
            }

            mInputThread = new TCPInputThread(mInStream);
            mInputThread.start();
        }

        public void close() {
            try {
                mInStream.close();
                mOutStream.close();
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mInStream = null;
            mOutStream = null;
            mSocket = null;
            mInputThread = null;
        }
    }

    private class TCPInputThread extends Thread {
        InputStream mInputStream;

        public TCPInputThread(InputStream stream) {
            mInputStream = stream;
        }

        @Override
        public void run() {
            byte[] buf = new byte[1024];

            while(true) {
                try {
                    int numRead = mInputStream.read(buf);
                    LogUtil.d("TCP message - " + numRead);
                    if(numRead == -1) {
                        LogUtil.d("TCP connection lost");
                        for(int i = 0; i < tcpClientList.size(); i++) {
                            if(tcpClientList.get(i).mInStream.equals(mInputStream)) {
                                tcpClientList.get(i).close();
                                tcpClientList.remove(i);
                                LogUtil.d("removing from TCP list - " + i);
                            }
                        }
                        break;
                    }
                    processRemoteCmd(buf);
                } catch (IOException e) {
                    e.printStackTrace();
                    LogUtil.d("TCPInputThread finished");
                    break;
                }
            }
        }
    }

    private class TCPAcceptThread extends Thread {
        ServerSocket mServerSocket;

        public TCPAcceptThread() {
            try {
                mServerSocket = new ServerSocket(REMOCON_PORT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                LogUtil.d("TCP Server start");

                while(true) {
                    Socket newSocket = mServerSocket.accept();
                    if(newSocket != null) {
                        tcpClientList.add(new TCPConnection(newSocket));

                        sendCurrentVolumeToClient();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setSTBNameForBT() {
        btAdapter.setName("SEDN STB - *" + mApp.myMAC.substring(11, mApp.myMAC.length()));
    }

    public void sendEditTextToClient(String text) {
        sendToClient("SednText", text);
    }

    public void sendVolumeToClient(String vol) {
        sendToClient("SednVolume", vol);
    }

    private void sendToClient(String cmd, String msg) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            final ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(cmd);
            oos.writeObject(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        final byte[] buf = baos.toByteArray();

        // 물려있는 모든 리모컨에 전달한다.
        for(int i = 0; i < btClientList.size(); i++) {
            try {
                btClientList.get(i).mOutStream.write(buf);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for(int i = 0; i < tcpClientList.size(); i++) {
            try {
                tcpClientList.get(i).mOutStream.write(buf);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendCurrentVolumeToClient() {
        AudioManager audio = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int volume = 100 * currentVolume / 15;
        sendVolumeToClient( String.valueOf(volume));
    }
}
