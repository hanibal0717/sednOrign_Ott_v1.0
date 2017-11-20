package com.inucreative.sednlauncher.HdmiCec;

import android.os.Handler;
import android.os.Message;

import java.io.IOException;

/**
 * TV On/Off 제어
 * Created by apple on 2017. 3. 16..
 */

public class TvControl {

    public void tvPowerOn(int delay) {
        mHandlerTvOn.sendEmptyMessageDelayed(0, delay);
    }

    public void tvPowerOff(int delay) {
        mHandlerTvOff.sendEmptyMessageDelayed(0, delay);
    }

    /**
     * TV ON
     */
    Handler mHandlerTvOn = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "echo e > /sys/class/amhdmitx/amhdmitx0/cec"});
                p.getErrorStream().close();
                p.getInputStream().close();
                p.getOutputStream().close();
                p.waitFor();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * TV OFF
     */
    Handler mHandlerTvOff = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "echo d > /sys/class/amhdmitx/amhdmitx0/cec"});
                p.getErrorStream().close();
                p.getInputStream().close();
                p.getOutputStream().close();
                p.waitFor();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };
}
