package com.inucreative.sednlauncher.Threads;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;

import com.inucreative.sednlauncher.Activity.MainActivity;
import com.inucreative.sednlauncher.Activity.PlayerActivity;
import com.inucreative.sednlauncher.Client.RetrofitClient;
import com.inucreative.sednlauncher.DataType.VODItem;
import com.inucreative.sednlauncher.R;
import com.inucreative.sednlauncher.SednApplication;
import com.inucreative.sednlauncher.Util.LogUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Jskim on 2016-07-25.
 * MQTT 서버가 가동중이 아니거나 연결이 끊어지더라도 주기적으로 재시도한다.
 */

public class SednServer implements IMqttActionListener {
    boolean mConnected;

    Context mContext;
    SednServer mServerListner;
    ServerSocket server;
    SednApplication application;
    MqttAndroidClient mClient;
    MqttConnectOptions opts;

    Handler connectHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (mClient != null) {
                LogUtil.d(mClient.toString());
                try {
                    mClient.unregisterResources();
                    mClient.connect(opts, null, mServerListner);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public SednServer(Context context) {
        mContext = context;
        mServerListner = this;
        application = (SednApplication) (((MainActivity) mContext).getApplication());

        opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setKeepAliveInterval(60);

        String port_str = "";
        if(application.gSednPushPort != null && !application.gSednPushPort.isEmpty()) {
            port_str = ":" + application.gSednPushPort;
        }

        mClient = new MqttAndroidClient(mContext, "tcp://" + application.gSednServer + port_str, "SEDN_" + application.myMAC.replace(":", ""));
        LogUtil.d("MTQQ connection : ");
        mClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                LogUtil.d("connectionLost");
                mConnected = false;

                // 재접속
                connectHandler.sendEmptyMessage(0);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String command = new String(message.getPayload());

                LogUtil.d("messageArrived:" + topic + ": " + command);
                switch (command) {
                    case "reboot":
                        Handler rebootHandler = new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                super.handleMessage(msg);
                                try {
                                    Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
                                    p.waitFor();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        rebootHandler.sendEmptyMessageDelayed(0, 5000);

                        rebootHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder alert = new AlertDialog.Builder(application.mCurrentContext);
                                alert.setMessage(mContext.getResources().getString(R.string.msg_reboot_alert));
                                alert.show();
                            }
                        });

                        break;
                    case "schedule_download":
                        //SednDBClient.getSchedule();
                        RetrofitClient.getSchedule();

                        //SednDBClient.getSTBData();  // UI 업데이트
                        RetrofitClient.getSTBData();
                        break;
                    case "firmware_update":
                        //SednDBClient.checkForUpdate(true);
                        RetrofitClient.checkForUpdate(true);
                        break;
                    case "vod_download":
                        // 우선 동기화 한 후 다운로드 시작한다.
                        //SednDBClient.getSchedule();
                        RetrofitClient.getSchedule();

                        //SednDBClient.getSTBData();  // UI 업데이트
                        RetrofitClient.getSTBData();

                        //SednDBClient.getVodSchedule_24Hour();
                        RetrofitClient.getVodSchedule_24Hour();

                        // 한시간 내에 랜덤 딜레이 적용
                        Handler downloadHandler = new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                super.handleMessage(msg);
                                LogUtil.d("Checking for auto download");
                                // 다운로드할 VOD 목록 생성
                                application.downloadVODIndex = 0;
                                application.downloadVODList.clear();
                                LogUtil.d("schedule size = " + application.todaySchedule.size());

                                for(String key : application.todaySchedule.keySet()) {
                                    Intent item = application.todaySchedule.get(key);
                                    int mode = item.getIntExtra("PLAY_MODE", 0);
                                    LogUtil.d("schedule item mode = " + mode);

                                    if(mode == PlayerActivity.PLAY_SCHEDULE_VOD) {
                                        ArrayList<String> vod_id_list = item.getStringArrayListExtra("VOD_ID_LIST");
                                        ArrayList<String> vod_path_list = item.getStringArrayListExtra("VOD_PATH_LIST");
                                        for(int i = 0; i < vod_id_list.size(); i++) {
                                            String vod_id = vod_id_list.get(i);
                                            String vod_path = vod_path_list.get(i);
                                            VODItem vodItem = new VODItem(vod_id, null, null, vod_path);
                                            LogUtil.d("vod item - " + vod_id + ", " + vod_path);
                                            if(!LocalDBManager.isDownloaded(vod_id)) {
                                                application.downloadVODList.add(vodItem);
                                                LogUtil.d("Add Download VOD - " + vod_id + ", " + vod_path);
                                            }
                                        }
                                    }
                                }
                                // 다운로드 시작
                                if(!application.downloadVODList.isEmpty()) {
                                    ((MainActivity)mContext).showToast(mContext.getResources().getString(R.string.msg_batch_down_start));
                                    new Handler().post(new Runnable() {
                                        @Override
                                        public void run() {

                                            //if(SednApplication.mUseSDCARD) {
                                                for(VODItem item : application.downloadVODList) {
                                                    ((MainActivity) mContext).requestDownload("BATCH_DOWN", item);
                                                }
                                            //}
                                            //else {
                                            //    ((MainActivity) mContext).requestDownload("BATCH_DOWN", application.downloadVODList.get(application.downloadVODIndex));
                                            //}
                                        }
                                    });
                                }
                            }
                        };
                        long random_delay = new Random().nextInt(SednApplication.VOD_DOWNLOAD_RANDOM_DELAY);
                        LogUtil.d("VOD Download will start in " + random_delay);
                        downloadHandler.sendEmptyMessageDelayed(0, random_delay);
                        break;

                    case "tv_power_on":
                        SednApplication.mTvControl.tvPowerOn(0);
                        break;

                    case "tv_power_off":
                        ((MainActivity)mContext).showToast(mContext.getResources().getString(R.string.toast_tv_off));
                        SednApplication.mTvControl.tvPowerOff(3000);
                        break;

                    case "vod_update":
                        //SednDBClient.getSTBData();  // UI 업데이트
                        RetrofitClient.getSTBData();
                        ((MainActivity)mContext).showToast(mContext.getResources().getString(R.string.toast_vod_update));
                        break;

                    case "config_update":
                        RetrofitClient.getConfiguration();
                        ((MainActivity)mContext).showToast(mContext.getResources().getString(R.string.toast_config_update));
                        break;
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                LogUtil.d("deliveryComplete");
            }
        });

        connectHandler.sendEmptyMessage(0);
    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        LogUtil.d("MQTT Connection Success!!");
        try {
            mClient.subscribe("/broadcast", 0);
            mClient.subscribe("/" + application.myMAC.replace(":", ""), 0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mConnected = true;
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        LogUtil.d("MQTT Connection Failure!");
        mConnected = false;
        connectHandler.sendEmptyMessageDelayed(0, 2000);
    }
}