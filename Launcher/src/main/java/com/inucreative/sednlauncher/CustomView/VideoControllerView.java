package com.inucreative.sednlauncher.CustomView;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.support.percent.PercentRelativeLayout;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.BaseInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.android.internal.view.animation.NativeInterpolatorFactory;
import com.android.internal.view.animation.NativeInterpolatorFactoryHelper;
import com.inucreative.sednlauncher.R;
import com.inucreative.sednlauncher.Util.LogUtil;
import com.inucreative.sednlauncher.Util.Utils;

import java.io.InvalidObjectException;
import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

/**
 * Created by Jskim on 2016-07-20.
 */
public class VideoControllerView extends FrameLayout {
    private static final String TAG = "VideoControllerView";

    private MediaPlayerControl  mPlayer;
    private Context             mContext;
    private ViewGroup           mAnchor;
    private View                mRoot;
    private ProgressBar         pbProgress;
    private TextView            tvEndTime, tvCurrentTime;
    private String              mTitle;
    private String              mCaption;
    private int                mCaptionSize;
    private int                mCaptionSpeed;
    private int                mCaptionTextColor;
    private int                mCaptionBGColor;
    private TextView            tvTitle;
    private TextView            tvPlaySpeed;
    private ImageView           ivVolumeMute;
    private ImageView           ivPlayStateButton;

    private RelativeLayout      layoutPlayControl;
    private RelativeLayout      layoutVolumeControl;

    private static final int    sDefaultTimeout = 5000;
    private static final int    FADE_OUT = 1;
    private static final int    SHOW_PROGRESS = 2;

    public static final int     SHOW_DEFAULT = 1;       // 배속 정보를 제외한 모든 정보
    public static final int     SHOW_SPEED = 2;           // 배속 정보 포함, 볼륨컨트롤 제외한 모든 정보
    public static final int     SHOW_VOLUME = 3;        // 볼륨 컨트롤 only

    StringBuilder               mFormatBuilder;
    Formatter                   mFormatter;

    private View                vVolumeLevel;
    private Handler             mHandler = new MessageHandler(this);

    private int[] captionSize = {0, 30, 45, 60};    // 1-based 1:작게, 2:보통, 3:크게
    private int[] captionDuration = {0, 0, 15000, 9000, 21000};    // 1-based, 1:고정, 2:보통, 3:빠르게, 4:천천히
    private int[] captionLimit = {0, 66, 45, 33};
    //private int[] captionLimit = {0, 33, 23, 17};

    public VideoControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRoot = null;
        mContext = context;

        Log.i(TAG, TAG);
    }

    public VideoControllerView(Context context, boolean useFastForward) {
        super(context);
        mContext = context;

        Log.i(TAG, TAG);
    }

    public VideoControllerView(Context context) {
        this(context, true);

        Log.i(TAG, TAG);
    }

    @Override
    public void onFinishInflate() {
        if (mRoot != null)
            initControllerView(mRoot);
    }

    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setCaption(String caption, int size, int speed, int text_color, int bg_color) {
        mCaption = caption;
        mCaptionSize = size;
        mCaptionSpeed = speed;
        mCaptionTextColor = text_color;
        mCaptionBGColor = bg_color;
        LogUtil.d("setCaption : " + mCaptionSize + ", " + mCaptionSpeed + ", " + mCaptionTextColor + ", " + mCaptionBGColor);
    }
    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView, or your Activity's main view.
     * @param view The view to which to anchor the controller when it is visible.
     */
    public void setAnchorView(ViewGroup view) {
        mAnchor = view;

        removeAllViews();
        makeControllerView();
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classes can override this to create their own.
     * @return The controller view.
     * @hide This doesn't work as advertised
     */
    protected void makeControllerView() {
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(R.layout.media_controller, null);
        initControllerView(mRoot);
        addView(mRoot);
        mAnchor.addView(this, frameParams);
        //addView(mRoot, frameParams);
    }

    private void initControllerView(View v) {
        tvTitle = (TextView) v.findViewById(R.id.tvTitle);
        tvTitle.setText(mTitle);

        tvPlaySpeed = (TextView) v.findViewById(R.id.tvPlaySpeed);

        pbProgress = (ProgressBar) v.findViewById(R.id.mediacontroller_progress);
        if (pbProgress != null) {
            if (pbProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) pbProgress;
                BitmapDrawable thumb = (BitmapDrawable)getResources().getDrawable(R.drawable.spot);
                seeker.setThumb(Utils.scaleImage(getResources(), thumb, 0.66f));
            }
            pbProgress.setMax(1000);
            pbProgress.setPadding(15, 0, 15, 0);
        }
        tvEndTime = (TextView) v.findViewById(R.id.time);
        tvCurrentTime = (TextView) v.findViewById(R.id.time_current);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        vVolumeLevel = v.findViewById(R.id.volumeLevel);
        ivVolumeMute = (ImageView) v.findViewById(R.id.ivVolumeMute);

        ivPlayStateButton = (ImageView) v.findViewById(R.id.ivPlayStateButton);

        // layout
        layoutPlayControl = (RelativeLayout) v.findViewById(R.id.layoutPlayControl);
        layoutVolumeControl = (RelativeLayout) v.findViewById(R.id.layoutVolumeControl);

        // 초기상태 invisible
        layoutPlayControl.setVisibility(INVISIBLE);
        tvPlaySpeed.setVisibility(INVISIBLE);
        layoutVolumeControl.setVisibility(INVISIBLE);
        ivPlayStateButton.setVisibility(INVISIBLE);

        // 자막
        if(mCaption != null && !mCaption.isEmpty()) {
            TextView tvCaption = (TextView) v.findViewById(R.id.tvCaption);
            TextView tvCaptionShadow = (TextView) v.findViewById(R.id.tvCaptionShadow);

            // todo: 글자크기별로 줄바꿈 문자를 삽입한다
//            StringBuffer sb = new StringBuffer(mCaption);
//            if(sb.length() > captionLimit[mCaptionSize]) {
//                sb.insert(captionLimit[mCaptionSize], "\n");
//            }
//
//            if(sb.length() > captionLimit[mCaptionSize] * 2 + 1) {
//                sb.insert(captionLimit[mCaptionSize]*2 + 1, "\n");
//            }
//
//            if(sb.length() > captionLimit[mCaptionSize] * 3 + 2) {
//                sb.insert(captionLimit[mCaptionSize]*3 + 2, "\n");
//            }
//            String sCaption = sb.toString().substring(0, sb.length()-1);

            tvCaption.setText(mCaption);
            tvCaption.setMaxLines(3);
            tvCaption.setTextSize(TypedValue.COMPLEX_UNIT_PX, captionSize[mCaptionSize]);
            tvCaption.setTextColor(mCaptionTextColor);
            tvCaption.setBackgroundColor(mCaptionBGColor);

            tvCaptionShadow.setText(mCaption);
            tvCaptionShadow.setMaxLines(3);
            tvCaptionShadow.setTextSize(TypedValue.COMPLEX_UNIT_PX, captionSize[mCaptionSize]);
            tvCaptionShadow.setTextColor(mCaptionTextColor);
            tvCaptionShadow.setBackgroundColor(mCaptionBGColor);

            Animation captionAnim = AnimationUtils.loadAnimation(mContext, R.anim.caption_marquee);
            Animation captionShadowAnim = AnimationUtils.loadAnimation(mContext, R.anim.caption_shadow_marquee);
            captionAnim.setDuration(captionDuration[mCaptionSpeed]);
            captionShadowAnim.setDuration(captionDuration[mCaptionSpeed]);
            captionAnim.setInterpolator(new ConstantInterpolator());
            captionShadowAnim.setInterpolator(new ConstantInterpolator());

            tvCaption.startAnimation(captionAnim);
            tvCaptionShadow.startAnimation(captionShadowAnim);
        }
    }

    public class ConstantInterpolator implements Interpolator {
        public ConstantInterpolator() {
        }
        public float getInterpolation(float input) {
            return input;
        }
    }

    private void setPlayStateIcon() {
        if(mPlayer.isPlaying())
            ivPlayStateButton.setImageResource(R.drawable.player);
        else {
            if(mPlayer.getFFSpeed() < 0)
                ivPlayStateButton.setImageResource(R.drawable.rewind);
            else if(mPlayer.getFFSpeed() > 0)
                ivPlayStateButton.setImageResource(R.drawable.unwind);
            else
                ivPlayStateButton.setImageResource(R.drawable.stop);
        }
    }

    private String getSpeedString() {
        StringBuilder sb = new StringBuilder();
        int ffSpeed = mPlayer.getFFSpeed();

        if(ffSpeed != 0) {
            if (ffSpeed < 0) sb.append("-");
            sb.append("X");
            sb.append(String.valueOf(Math.abs(ffSpeed)));
            sb.append(getResources().getString(R.string.str_player_speed_text));
        }
        return sb.toString();
    }
    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    public void show(int show_type) {show(sDefaultTimeout, show_type); }

    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     * @param timeout The timeout in milliseconds. Use 0 to show
     * the controller until hide() is called.
     */
    public void show(int timeout, int show_type) {
        if (mAnchor != null) {
            switch(show_type) {
                case SHOW_DEFAULT:
                    layoutPlayControl.setVisibility(VISIBLE);
                    mHandler.removeMessages(SHOW_PROGRESS);
                    mHandler.sendEmptyMessage(SHOW_PROGRESS);
                    layoutVolumeControl.setVisibility(VISIBLE);
                    setPlayStateIcon();
                    ivPlayStateButton.setVisibility(VISIBLE);
                    tvPlaySpeed.setVisibility(INVISIBLE);
                    break;
                case SHOW_SPEED:
                    layoutPlayControl.setVisibility(VISIBLE);
                    mHandler.removeMessages(SHOW_PROGRESS);
                    mHandler.sendEmptyMessage(SHOW_PROGRESS);
                    tvPlaySpeed.setText(getSpeedString());
                    tvPlaySpeed.setVisibility(VISIBLE);
                    setPlayStateIcon();
                    ivPlayStateButton.setVisibility(VISIBLE);
                    break;
                case SHOW_VOLUME:
                    layoutVolumeControl.setVisibility(VISIBLE);
                    break;
            }
        }

        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            // FF 중일때는 fade_out하지 않는다.
            if(mPlayer != null && !mPlayer.isFastPlaying())
                mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    /**
     * Remove the controller from the screen.
     */
    public void hide() {
        if (mAnchor == null) {
            return;
        }

        layoutVolumeControl.setVisibility(INVISIBLE);

        if(mPlayer.getFFSpeed() == 0) {
            tvPlaySpeed.setVisibility(INVISIBLE);
            layoutPlayControl.setVisibility(INVISIBLE);
        }

        if(mPlayer.isPlaying())
            ivPlayStateButton.setVisibility(INVISIBLE);

        mHandler.removeMessages(SHOW_PROGRESS);
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() throws Exception {
        if (mPlayer == null) {
            return 0;
        }

        try {
            if (mPlayer.isPrepared()) {
                int position = mPlayer.getCurrentPosition();
                int duration = mPlayer.getDuration();
                if (pbProgress != null) {
                    if (duration > 0) {
                        // use long to avoid overflow
                        long pos = 1000L * position / duration;
                        pbProgress.setProgress((int) pos);
                    }
                }

                if (tvEndTime != null)
                    tvEndTime.setText(stringForTime(duration));
                if (tvCurrentTime != null)
                    tvCurrentTime.setText(stringForTime(position));

                return position;
            }
        } catch (Exception e) {
            throw e;
        }
        return -1;
    }

    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            show(sDefaultTimeout);
        }
    };

    private void doPauseResume() {
        if (mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
    }

    public void setMuteIcon(boolean isMute) {
        if(isMute)
            ivVolumeMute.setImageResource(R.drawable.icon_volume_mute);
        else
            ivVolumeMute.setImageResource(R.drawable.icon_volume);
    }

    // level : 0 ~ 100
    public void setVolumeIcon(int level) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)vVolumeLevel.getLayoutParams();
        params.height = 270 * level / 100;
        vVolumeLevel.setLayoutParams(params);
    }

    @Override
    public void setEnabled(boolean enabled) {

        if (pbProgress != null) {
            pbProgress.setEnabled(enabled);
        }
        super.setEnabled(enabled);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(VideoControllerView.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(VideoControllerView.class.getName());
    }

    public interface MediaPlayerControl {
        void    start();
        void    pause();
        int     getDuration();
        int     getCurrentPosition();
        void    seekTo(int pos);
        boolean isPlaying();
        boolean isFastPlaying();
        boolean isPrepared();
        int     getFFSpeed();
    }

    private static class MessageHandler extends Handler {
        private final WeakReference<VideoControllerView> mView;

        MessageHandler(VideoControllerView view) {
            mView = new WeakReference<VideoControllerView>(view);
        }
        @Override
        public void handleMessage(Message msg) {
            VideoControllerView view = mView.get();
            if (view == null || view.mPlayer == null) {
                return;
            }

            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    LogUtil.d("FADE_OUT");
                    view.hide();
                    break;
                case SHOW_PROGRESS:
                    try {
                        pos = view.setProgress();
                        LogUtil.d("SHOW_PROGRESS " + view.mPlayer.isPlaying() + ", " + view.mPlayer.isFastPlaying() + ", " + pos);
                        if (view.mPlayer.isPlaying() || view.mPlayer.isFastPlaying()) {
                            msg = obtainMessage(SHOW_PROGRESS);
                            sendMessageDelayed(msg, 1000 - (pos % 1000));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

}
