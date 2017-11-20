package com.inucreative.sednlauncher.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.inucreative.sednlauncher.SednApplication.app;


/**
 * Created by GongHee on 2017. 11. 9..
 */

public class ShutdownReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if(Intent.ACTION_SHUTDOWN.equals(action)) {

            app.setStatus(app.STATUS_OFF);
        }
    }
}
