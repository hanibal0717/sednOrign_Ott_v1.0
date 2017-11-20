package com.inucreative.sednlauncher;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * 볼륨변화됐을때 감지하기 위해서 추가
 * Created by apple on 2017. 11. 14..
 */

public class SettingsContentObserver extends ContentObserver {
    final String TAG = "Observer";
    Context context;
    Handler mHandler;

    public SettingsContentObserver(Context c, Handler handler) {
        super(handler);
        context=c;
        mHandler = handler;
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

        int volume = 100 * currentVolume / 15;
        Log.d(TAG, "volume:" + volume);

        Message msg = new Message();
        msg.what = 0;
        msg.arg1 = volume;

        mHandler.sendMessage(msg);
    }
}
