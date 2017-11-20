package com.inucreative.sednlauncher.Threads;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.inucreative.sednlauncher.Activity.MainActivity;
import com.inucreative.sednlauncher.Client.RetrofitClient;
import com.inucreative.sednlauncher.SednApplication;
import com.inucreative.sednlauncher.Util.LogUtil;
import com.inucreative.sednlauncher.Util.Utils;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jskim on 2016-07-29.
 */
public class ScheduleManager {
    private Context mContext;
    private SednApplication app;
    private Intent intentB;

    public ScheduleManager(Context context) {
        mContext = context;
        app = (SednApplication)((MainActivity)context).getApplication();

        Calendar cal = Calendar.getInstance();
        int sec = cal.get(Calendar.SECOND);

        //int initialDelay = 60 - SednApplication.mPlayerReadyTimeCount - sec;    // 매 55초에 체크

        final int TV_ON_DELAY = 10;
        int initialDelay = 60 - (SednApplication.mPlayerReadyTimeCount + TV_ON_DELAY) - sec;    // 매 45초에 체크
        if(initialDelay < 0) initialDelay += 60;

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Calendar cal = Calendar.getInstance();
                Date curDateTime = cal.getTime();
                cal.add(Calendar.MINUTE, 1);
                Date curDateTimePlus1Min = cal.getTime();

                String curTimeStr = Utils.sdf_YYYYMMDDHHMMSS.format(curDateTime);
                String curTimeStrForBroadcast = Utils.sdf_YYYYMMDDHHMM.format(curDateTimePlus1Min);
                LogUtil.d("ScheduledExecutorService - " + curTimeStr + ", " + curTimeStrForBroadcast);

                // 스케줄 방송 체크
                checkBroadcast(curTimeStrForBroadcast);

                // 서버에 ping 보내기
                sendPing();

                // 보유 기간 지난 파일 삭제하기
                checkFileExpiration();
            }
        }, initialDelay, 60, TimeUnit.SECONDS);
    }

    private void checkBroadcast(String timeStr) {

        // 맵에서 일치하는 일정을 빼냄
        intentB = app.schedule.remove(timeStr);

        if (intentB != null) {
            LogUtil.d("schedule match!!");

            // 방송을 10초 후에 시작한다 (TV 켜지는 시간을 감안한다)
            handler.postDelayed(r, 10000);

            // TV를 먼저 켠다
            SednApplication.mTvControl.tvPowerOn(0);
        }
    }

    Handler handler = new Handler(Looper.getMainLooper());
    final Runnable r = new Runnable() {
        @Override
        public void run() {
            ((MainActivity)mContext).startBroadcast(intentB);
        }
    };


    private void sendPing() {

        //SednDBClient.sendPing();
        RetrofitClient.sendPing();
    }

    private void checkFileExpiration() {
        File downDir = LocalDBManager.getDownloadFolder();
        File[] fileList= downDir.listFiles();
        long curMillisec = System.currentTimeMillis();
        for(int i=0; i<fileList.length; i++) {
            long lastModified = fileList[i].lastModified();
            LogUtil.d("checkFileExpiration() down file - " + fileList[i].toString() + ", " + new Date(lastModified) + "("+lastModified+"), " + (curMillisec - lastModified));

            long retentionCut = 0L;
            switch(app.gStorageRetention) {
                case SednApplication.RETENTION_3_MONTH:
                    retentionCut = 3L * 30L * 24L * 60L * 60L * 1000L;
                    break;
                case SednApplication.RETENTION_6_MONTH:
                    retentionCut = 6L * 30L * 24L * 60L * 60L * 1000L;
                    break;
                case SednApplication.RETENTION_NONE:
                    retentionCut = Long.MAX_VALUE;
                    break;
            }

            LogUtil.d("retention " + app.gStorageRetention + ", cut " + retentionCut);
            if((curMillisec - lastModified) > retentionCut) {
                LogUtil.d("delete this file!!");
                fileList[i].delete();
            }
        }
    }
}
