package com.inucreative.sednlauncher.Threads;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.inucreative.sednlauncher.Activity.MainActivity;
import com.inucreative.sednlauncher.SednApplication;
import com.inucreative.sednlauncher.Util.LogUtil;
import com.inucreative.sednlauncher.Util.Utils;

/**
 * Created by Jskim on 2016-08-01.
 */
public class ConnectivityListener {
    private final Context mContext;
    private final SednApplication mApp;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;

    public ConnectivityListener(Context context)
    {
        mContext = context;
        mApp = (SednApplication) (((MainActivity) mContext).getApplication());

        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");

        mReceiver = new BroadcastReceiver()
        {
            public void onReceive(Context context, Intent intent)
            {
                mApp.myIP = Utils.getIPAddress(true);

                if(mApp.initializedForNetwork) {
                    updateConnectivityStatus();
                } else {
                    ((MainActivity)mContext).initializeForNetework();
                }
                LogUtil.d("Connectivity Received : " + intent.getAction());
            }
        };
    }

    public void updateConnectivityStatus() {
        ((MainActivity)mContext).redrawNetworkSetup();
    }

    public void start() {
        mContext.registerReceiver(this.mReceiver, this.mFilter);
    }

    public void stop() {
        mContext.unregisterReceiver(this.mReceiver);
    }
}
