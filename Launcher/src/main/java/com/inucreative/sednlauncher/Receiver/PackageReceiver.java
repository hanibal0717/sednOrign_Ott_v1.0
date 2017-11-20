package com.inucreative.sednlauncher.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by apple on 2017. 5. 23..
 */

public class PackageReceiver extends BroadcastReceiver{

    private final static String OTT_PKG_NAME="com.inucreative.sednlauncher";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final String packageName = intent.getData().getSchemeSpecificPart();

        if(!OTT_PKG_NAME.equals(packageName))
            return;

//        if (Intent.ACTION_PACKAGE_INSTALL.equals(action)
//                || Intent.ACTION_PACKAGE_ADDED.equals(action)
//                || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {

        if(Intent.ACTION_PACKAGE_REPLACED.equals(action) || Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            //Toast.makeText(context, "INSTALLER-" + action + ":" + packageName, Toast.LENGTH_SHORT ).show();

            Intent i = context.getPackageManager().getLaunchIntentForPackage(OTT_PKG_NAME);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

    private void rebootSTB() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
