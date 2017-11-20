package com.inucreative.sednlauncher.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.inucreative.sednlauncher.R;
import com.inucreative.sednlauncher.SednApplication;
import com.inucreative.sednlauncher.Util.LogUtil;
import com.inucreative.sednlauncher.Util.Utils;

public class ScheduleReadyActivity extends AppCompatActivity {
    TextView tvCount;
    Animation countFadeout;
    Intent playInfo;

    BroadcastReceiver stopReciever;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            LogUtil.d("ScheduleReadyActivity " + msg.what);

            if(msg.what == 0) {
                startActivity(playInfo);
                finish();
            }
            else {
                tvCount.setText(String.valueOf(msg.what));
                tvCount.startAnimation(countFadeout);
                handler.sendEmptyMessageDelayed(msg.what - 1, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_ready);

        LogUtil.d("ready activity onCreate");

        // 네비게이션바 숨기기
        Utils.hideSystemUI(getWindow().getDecorView());

        playInfo = (Intent)getIntent().getExtra("PlayInfo");

        tvCount = (TextView)findViewById(R.id.tvCount);
        countFadeout = AnimationUtils.loadAnimation(this, R.anim.count_fadeout);
        countFadeout.setFillAfter(true);

        stopReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                LogUtil.d("ScheduleReadyActivity stopReciever onReceive");
                handler.removeCallbacksAndMessages(null);
                finish();
            }
        };

        if(0 < SednApplication.mPlayerReadyTimeCount)
            handler.sendEmptyMessageDelayed(SednApplication.mPlayerReadyTimeCount, 1000);
        else
            handler.sendEmptyMessage(0);

        // TV 켜기
        SednApplication.mTvControl.tvPowerOn(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtil.d("ready activity onResume");

        IntentFilter stateFilter = new IntentFilter(SednApplication.ACTION_SEDN_STOP_PLAYER);
        registerReceiver(stopReciever, stateFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtil.d("ready activity onPause");
        unregisterReceiver(stopReciever);
        handler.removeCallbacksAndMessages(null);
    }



}
