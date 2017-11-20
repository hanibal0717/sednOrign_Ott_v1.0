package com.inucreative.sednlauncher.Player;

import android.media.MediaPlayer;

import com.inucreative.sednlauncher.Util.LogUtil;

/**
 * Created by Jskim on 2016-08-07.
 */
public class SednMediaPlayer extends MediaPlayer implements MediaPlayer.OnSeekCompleteListener {
    public int mFFSpeed = 0;
    public int mFFPosition;
    Thread mFFThread = null;
    OnFFListener mFFRestartListener = null;

    private static final int FF_REFRESH_INTERVAL = 150; // msec
    public SednMediaPlayer() {
        setOnSeekCompleteListener(this);
    }

    Runnable mFFRunnable = new Runnable() {
        @Override
        public void run() {
            while(true) {
                if(mFFSpeed == 0 || !isPlayingSuper()) {
                    LogUtil.d("ff thread finish");
                    break;
                }
                mFFPosition += mFFSpeed * FF_REFRESH_INTERVAL;
                LogUtil.d("ff seek pos : " + mFFPosition);

                // 처음으로 돌아간 경우 정상 재생
                if(mFFPosition < 0) {
                    mFFSpeed = 0;
                    mFFRestartListener.onFFBeginningReached();
                    break;
                }

                // 끝까지 간 경우 종료
                if(mFFPosition > getDuration()) {
                    mFFSpeed = 0;
                    mFFRestartListener.onFFEndingReached();
                    break;
                }

                try {
                    seekTo(mFFPosition);
                } catch (Exception e) {
                    break;
                }

                try {
                    Thread.sleep(FF_REFRESH_INTERVAL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    };

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        LogUtil.d("onSeekComplete pos " + mp.getCurrentPosition());
    }

    public boolean isFastPlaying() {
        return mFFSpeed != 0;
    }

    public boolean isPlaying()
    {
        if(mFFSpeed != 0)
            return false;

        return super.isPlaying();
    }

    public boolean isPlayingSuper() {
        return super.isPlaying();
    }

    public int getFFSpeed() {
        LogUtil.d("getFFSpeed " + mFFSpeed);
        return mFFSpeed;
    }

    public void pause()
    {
        mFFSpeed = 0;
        super.pause();
    }

    public void reset()
    {
        super.reset();
    }

    public void start()
    {
        mFFSpeed = 0;
        if(!isPlaying())
            super.start();
    }

    public void stop()
    {
        mFFSpeed = 0;
        super.stop();
    }

    public void fastForward(int speed) {
        LogUtil.d("FF speed " + speed);
        // ff진입시 position set
        if(mFFSpeed == 0 ) {
            if(!isPlayingSuper())
                start();
            mFFPosition = getCurrentPosition();
            mFFSpeed = speed;
            mFFThread = new Thread(mFFRunnable);
            mFFThread.start();
        } else {
            mFFSpeed = speed;
        }
    }

    public void setOnFFRestartListner(OnFFListener listner) {
        mFFRestartListener = listner;
    }

    public interface OnFFListener {
        void onFFBeginningReached();
        void onFFEndingReached();
    }
}
