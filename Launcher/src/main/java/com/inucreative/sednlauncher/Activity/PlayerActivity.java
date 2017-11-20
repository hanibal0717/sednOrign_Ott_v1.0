package com.inucreative.sednlauncher.Activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import com.inucreative.sednlauncher.CustomView.VideoControllerView;
import com.inucreative.sednlauncher.DataType.STBLocation;
import com.inucreative.sednlauncher.Player.SednMediaPlayer;
import com.inucreative.sednlauncher.R;
import com.inucreative.sednlauncher.RssModel.Feed;
import com.inucreative.sednlauncher.RssModel.FeedItem;
import com.inucreative.sednlauncher.SednApplication;
import com.inucreative.sednlauncher.Threads.LocalDBManager;
import com.inucreative.sednlauncher.Util.LogUtil;
import com.inucreative.sednlauncher.Util.Utils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import retrofit2.http.GET;

public class PlayerActivity extends AppCompatActivity implements SurfaceHolder.Callback,
                                                                    MediaPlayer.OnPreparedListener,
                                                                    MediaPlayer.OnCompletionListener,
                                                                    MediaPlayer.OnErrorListener,
                                                                    SednMediaPlayer.OnFFListener,
                                                                    VideoControllerView.MediaPlayerControl {
    SednApplication mApp;

    Handler toastHandler = new Handler();

    SurfaceView videoSurface;
    SednMediaPlayer player;
    VideoControllerView controller;
    AudioManager audio;
    int maxVolume;
    boolean isMute;
    boolean isControllerSet;
    boolean mIsPrepared;

    BroadcastReceiver stopReciever;

    private static final int[] FF_SPEED = new int[] {2, 4, 8, 16, 32};
    private int mFFLevel; // 0: normal play, 5 : 32배속 FF, -5 : 32배속 FB

    public static final int PLAY_STB_LIVE = 1; // 종료버튼 누르기 전까지는 계속 재생
    public static final int PLAY_STB_VOD = 2;  // 영상 한개가 끝나면 플레이어 종료
    public static final int PLAY_SCHEDULE_LIVE = 3; // 지정된 시간동안 플레이어 구동
    public static final int PLAY_SCHEDULE_VOD = 4; // 지정된 시간동안 플레이어 구동. (VOD는 반복재생)
    public int mPlayerMode;

    ArrayList<String> mVODList;
    int curVODIndex;
    int curVODpos;
    int mTemplateType=0;

    TextView tvBGDate;
    TextView tvBGTime;
    TextClock tcClock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 네비게이션바 숨기기
        Utils.hideSystemUI(getWindow().getDecorView());

        mApp = (SednApplication)getApplication();

        mTemplateType = getIntent().getIntExtra("TEMPLATE", 0);

//        if(1 == mTemplateType) {
//            setContentView(R.layout.activity_player_type1);
//            initUI4Template();
//        }
//        else {
            setContentView(R.layout.activity_player);
//        }

        videoSurface = (SurfaceView) findViewById(R.id.videoSurface);
        SurfaceHolder videoHolder = videoSurface.getHolder();
        videoHolder.addCallback(this);

        player = new SednMediaPlayer();
        controller = new VideoControllerView(this);
        controller.setTitle(getIntent().getStringExtra("TITLE"));
        controller.setCaption(getIntent().getStringExtra("CAPTION"), getIntent().getIntExtra("CAPTION_SIZE", 1), getIntent().getIntExtra("CAPTION_SPEED", 1), getIntent().getIntExtra("CAPTION_TEXT_COLOR", Color.WHITE), getIntent().getIntExtra("CAPTION_BG_COLOR", Color.TRANSPARENT));
        isControllerSet = false;
        mFFLevel = 0;
        mIsPrepared = false;

        stopReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopPlayer(SednApplication.PLAYER_DONE);
            }
        };

        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / maxVolume;
        mApp.setVolume(volume);

        isMute = false;

        mPlayerMode = getIntent().getIntExtra("PLAY_MODE", 0);

        if(mPlayerMode == 0) finish();

        mVODList = new ArrayList<>();
        curVODIndex = 0;
        curVODpos = 0;

        if(mPlayerMode == PLAY_STB_VOD || mPlayerMode == PLAY_STB_LIVE || mPlayerMode == PLAY_SCHEDULE_LIVE) {
            mVODList.add(getIntent().getStringExtra("URI"));
        } else if(mPlayerMode == PLAY_SCHEDULE_VOD) {
            int elapsed_sec = (int)((Long)getIntent().getLongExtra("ELAPSED", 0) / 1000);   // sec 단위로 변환
            ArrayList<String> vod_path_list = getIntent().getStringArrayListExtra("VOD_PATH_LIST");
            ArrayList<String> vod_id_list = getIntent().getStringArrayListExtra("VOD_ID_LIST");
            ArrayList<Integer> vod_duration_list = getIntent().getIntegerArrayListExtra("VOD_DURATION_LIST");

            LogUtil.d("schedul vod - " + elapsed_sec);
            for(int duration: vod_duration_list) {
                LogUtil.d("duration " + duration);
            }

            if(elapsed_sec > 0) {   // VOD 따라잡기
                int cur_sec = 0;
                int cur_vod_index = 0;
                while (cur_sec + vod_duration_list.get(cur_vod_index) <= elapsed_sec) {
                    cur_sec += vod_duration_list.get(cur_vod_index);
                    cur_vod_index++;
                    if(cur_vod_index == vod_duration_list.size())
                        cur_vod_index = 0;
                }
                curVODIndex = cur_vod_index;
                curVODpos = elapsed_sec - cur_sec;
                LogUtil.d("vod catchup " + curVODIndex + ", " + curVODpos);
            }

            for(int i=0; i<vod_path_list.size(); i++) {

                String vodId = vod_id_list.get(i);

                // 로컬에 다운로드 받은 파일이 있다면 그 파일을 재생한다.
                if(mApp.mMainActivity.mLocalDBManager.isDownloaded(vodId)) {
                    String path = LocalDBManager.getDownloadedVOD(vodId).getPath();
                    mVODList.add(path);
                }
                else {
                    String path = vod_path_list.get(i);
                    mVODList.add(path);
                }
            }
        }

        try {
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(this, Uri.parse(mVODList.get(curVODIndex)));
            player.setOnPreparedListener(this);
            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
            player.setOnFFRestartListner(this);

            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        } catch (Exception e) {
            e.printStackTrace();
            stopPlayer(SednApplication.PLAYER_ERROR);
        }

        if(mPlayerMode == PLAY_SCHEDULE_VOD || mPlayerMode == PLAY_SCHEDULE_LIVE) {
            long duration = getIntent().getLongExtra("DURATION", 0);
            Runnable mRunnable = new Runnable() {
                @Override
                public void run() {
                    stopPlayer(SednApplication.PLAYER_DONE);

                    // todo: HDMI-CEC를 지원할 경우에만 아래 메세지 표시되어야함.
                    // TV 전원끄기
                    if(mApp.mMainActivity.mHdmiCecManager.isCecEnabled()) {
                        showToast(R.string.str_player_schedule_end_tvoff);
                        SednApplication.mTvControl.tvPowerOff(3000);
                    }
                }
            };

            Handler mHandler = new Handler();
            mHandler.postDelayed(mRunnable, duration);
        }

        if(mPlayerMode == PLAY_STB_VOD || mPlayerMode == PLAY_STB_LIVE)
            mApp.setStatus(mApp.STATUS_VOD);
        if(mPlayerMode == PLAY_SCHEDULE_LIVE || mPlayerMode == PLAY_SCHEDULE_VOD)
            mApp.setStatus(mApp.STATUS_BROADCAST);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //vvPlayer.start();
        mApp.mCurrentContext = this;

        IntentFilter stateFilter = new IntentFilter(SednApplication.ACTION_SEDN_STOP_PLAYER);
        registerReceiver(stopReciever, stateFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(stopReciever);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //controller.show();
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        LogUtil.d("player onKeyDown : " + keyCode);

        if(keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if(mPlayerMode == PLAY_STB_LIVE || mPlayerMode == PLAY_STB_VOD) {
                stopPlayer(SednApplication.PLAYER_DONE);
            } else {
                showToast(R.string.str_player_schedule_cannot_exit);
            }
            return true;
        }



        if(keyCode == KeyEvent.KEYCODE_FOCUS) {
            int volume = mApp.getVolume();
            LogUtil.d("setVolume : " + volume);
            setSednVolume(volume);
            controller.show(VideoControllerView.SHOW_VOLUME);
            return true;
        } else if(keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            toggleMute();
            controller.show(VideoControllerView.SHOW_VOLUME);
            return true;
        }
        // 2017.3.31 ghlee 추가
        else if(KeyEvent.KEYCODE_VOLUME_UP == keyCode) {
            int volume = mApp.getVolume() + 5;

            if(100 < volume )
                volume = 100;

            mApp.setVolume(volume);
            LogUtil.d("setVolume : " + volume);
            setSednVolume(volume);
            controller.show(VideoControllerView.SHOW_VOLUME);
            return true;
        }
        // 2017.3.31 ghlee 추가
        else if(KeyEvent.KEYCODE_VOLUME_DOWN == keyCode) {
            int volume = mApp.getVolume() - 5;

            if(0 > volume)
                volume = 0;

            mApp.setVolume(volume);
            LogUtil.d("setVolume : " + volume);
            setSednVolume(volume);
            controller.show(VideoControllerView.SHOW_VOLUME);
            return true;
        }


        // 확인키에 대해서는 모드 관계없이 지원
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            controller.show(VideoControllerView.SHOW_DEFAULT);
            return true;
        }

        // 영상 컨트롤 키는 STB VOD 모드에서만 가능
        if(mPlayerMode == PLAY_STB_VOD || PLAY_STB_LIVE == mPlayerMode) {
            if(keyCode == KeyEvent.KEYCODE_MEDIA_STOP || keyCode == KeyEvent.KEYCODE_PROG_GREEN) {
                stopPlayer(SednApplication.PLAYER_DONE);
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_PROG_YELLOW) {
                if(isPlaying() || isFastPlaying())
                    player.pause();
                else
                    player.start();
                mFFLevel = 0;
                controller.show(VideoControllerView.SHOW_DEFAULT);
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {   // 기본 리모컨 지원
                player.pause();
                mFFLevel = 0;
                controller.show(VideoControllerView.SHOW_DEFAULT);
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD || keyCode == KeyEvent.KEYCODE_PROG_BLUE) {
                if(!isFastPlaying()) mFFLevel = 0;
                if(mFFLevel < FF_SPEED.length) {
                    mFFLevel++;
                    setFFLevel(mFFLevel);
                }
                controller.show(VideoControllerView.SHOW_SPEED);
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || keyCode == KeyEvent.KEYCODE_PROG_RED) {
                if(!isFastPlaying()) mFFLevel = 0;
                if( (-mFFLevel) < FF_SPEED.length) {
                    mFFLevel--;
                    setFFLevel(mFFLevel);
                }
                controller.show(VideoControllerView.SHOW_SPEED);
            }
            else if(KeyEvent.KEYCODE_DPAD_RIGHT == keyCode) {
                if(isPlaying()) {
                    int curPos = player.getCurrentPosition();
                    int newPos = curPos + 10000; // 10초후
                    int dur = getDuration();
                    if(newPos > dur)
                        player.seekTo(dur);
                    else
                        player.seekTo(newPos);

                    controller.show(VideoControllerView.SHOW_DEFAULT);
                }
            }
            else if(KeyEvent.KEYCODE_DPAD_LEFT == keyCode) {
                if(isPlaying()) {
                    int curPos = player.getCurrentPosition();
                    if(curPos > 10000) // 10초전
                        player.seekTo(curPos - 10000);
                    else
                        player.seekTo(0);

                    controller.show(VideoControllerView.SHOW_DEFAULT);
                }
            }
            else if(KeyEvent.KEYCODE_DPAD_UP == keyCode) {
                if(isPlaying()) {
                    int curPos = player.getCurrentPosition();
                    int dur = getDuration();
                    int newPos = curPos + 300000; // 5분후
                    if(newPos > dur)
                        player.seekTo(dur);
                    else
                        player.seekTo(newPos);

                    controller.show(VideoControllerView.SHOW_DEFAULT);
                }
            }
            else if(KeyEvent.KEYCODE_DPAD_DOWN == keyCode) {
                if(isPlaying()) {
                    int curPos = player.getCurrentPosition();
                    if(curPos > 300000) // 5분전
                        player.seekTo(curPos - 300000);
                    else
                        player.seekTo(0);

                    controller.show(VideoControllerView.SHOW_DEFAULT);
                }
            }
            else {
                showToast(R.string.str_player_key_not_supported);
            }
        } else {
            showToast(R.string.str_player_key_not_supported);
        }
        return true;    // 기본적으로 처리된 키는 consume한다.
    }

    void showToast(int strID) {
        toastHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PlayerActivity.this, getResources().getString(strID), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleMute() {
        if(isMute) {
            isMute = false;
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, mApp.getVolume() * maxVolume / 100, 0);
        } else {
            isMute = true;
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        }
        controller.setMuteIcon(isMute);
    }

    private void setSednVolume(int volume) {
        isMute = false;
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume * maxVolume / 100, 0);
        controller.setMuteIcon(false);
        controller.setVolumeIcon(volume);
    }

    private void setFFLevel(int ffLevel) {
        LogUtil.d("setFFLevel : " + ffLevel);
        if(ffLevel == 0) {
            player.start();
        } else if(ffLevel > 0) {
            player.fastForward(FF_SPEED[ffLevel-1]);
        } else if(ffLevel < 0) {
            player.fastForward(-FF_SPEED[-ffLevel-1]);
        }
    }

    public int getFFSpeed() {
        return player.getFFSpeed();
    }

    public void stopPlayer(int status) {
        LogUtil.d("stoping player");
        mApp.setStatus(mApp.STATUS_ON);
        player.stop();
        player.reset();

        Intent intent = getIntent();
        intent.putExtra("returnVal", status);
        setResult(RESULT_OK, intent);

        LogUtil.d("Finishing player activity");
        finish();
    }

    // Implement SurfaceHolder.Callback
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(mApp.getStatus() != mApp.STATUS_ON) {
            player.setDisplay(holder);
            player.prepareAsync();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
    // End SurfaceHolder.Callback

    // Implement MediaPlayer.OnPreparedListener
    @Override
    public void onPrepared(MediaPlayer mp) {
        LogUtil.d("Player onPrepared");
        if(!isControllerSet) {
            controller.setMediaPlayer(this);
            controller.setAnchorView((FrameLayout) findViewById(R.id.videoSurfaceContainer));
            isControllerSet = true;
        }
        setPrepared(true);
        player.start();
        mFFLevel = 0;

        setSednVolume(mApp.getVolume());
        controller.show(VideoControllerView.SHOW_DEFAULT);

        if(curVODpos > 0) {
            player.seekTo(curVODpos * 1000);
            curVODpos = 0;
        }
    }
    // End MediaPlayer.OnPreparedListener

    @Override
    public void onCompletion(MediaPlayer mp) {
        LogUtil.d("Player onCompletion");

        mFFLevel = 0;
        if(mPlayerMode == PLAY_STB_VOD) {
            finish();
            mApp.setStatus(mApp.STATUS_ON);
        } else if(mPlayerMode == PLAY_SCHEDULE_VOD) {
            // stop되지 않았을 경우에만 다음 VOD 재생
            if(mApp.getStatus() == mApp.STATUS_BROADCAST) {
                try {
                    curVODIndex++;
                    if (curVODIndex == mVODList.size()) curVODIndex = 0;

                    setPrepared(false);
                    player.stop();
                    player.reset();
                    player.setDataSource(this, Uri.parse(mVODList.get(curVODIndex)));
                    player.prepareAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else if(mPlayerMode == PLAY_STB_LIVE) {
            // LIVE 시청중 complete된다면 에러 처리
            stopPlayer(SednApplication.PLAYER_ERROR);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        LogUtil.d("Player onError!! " + what + ", " + extra);
        mFFLevel = 0;
        stopPlayer(SednApplication.PLAYER_ERROR);
        return false;
    }

    @Override
    public void onFFBeginningReached() {
        LogUtil.d("onFFBeginningReached");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                controller.show(VideoControllerView.SHOW_DEFAULT);
            }
        });
    }

    @Override
    public void onFFEndingReached() {
        LogUtil.d("onFFEndingReached");
        mFFLevel = 0;
        stopPlayer(SednApplication.PLAYER_DONE);
        finish();
        mApp.setStatus(mApp.STATUS_ON);
    }
    // Implement VideoMediaController.MediaPlayerControl
    @Override
    public int getCurrentPosition() {
        return player.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return player.getDuration();
    }

    @Override
    public boolean isPlaying() {
        return player.isPlaying();
    }

    @Override
    public boolean isFastPlaying() {
        return player.isFastPlaying();
    }

    @Override
    public void pause() {
        mFFLevel = 0;
        player.pause();
    }

    @Override
    public void seekTo(int i) {
        player.seekTo(i);
    }

    @Override
    public void start() {
        player.start();
    }

    public void setPrepared(boolean isPrepared) {
        mIsPrepared = isPrepared;
    }
    public boolean isPrepared() {
        return mIsPrepared;
    }
// End VideoMediaController.MediaPlayerControl


    /**
     * 템플릿별 UI 초기화
     */
    private void initUI4Template() {

        if(1 == mTemplateType) {
            retrofit_News();
            Timer mWeatherTimer = new Timer();
            //mWeatherTimer.schedule(mWeatherUpdateTask, 0, 30 * 60 * 1000); // 30분
            mWeatherTimer.schedule(mWeatherUpdateTask, 0, 1 * 60 * 1000); // 30분

            tvBGDate = (TextView) findViewById(R.id.tvBGDate);
            tvBGTime = (TextView) findViewById(R.id.tvBGTime);
            tcClock = (TextClock) findViewById(R.id.tcClock);
            tcClock.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    //LogUtil.d("beforeTextChanged");
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    //LogUtil.d("onTextChanged");
                }

                @Override
                public void afterTextChanged(Editable s) {
                    //LogUtil.d("afterTextChanged " + s);
                    // TextClock은 12h/24h 이슈가 있어, 단순히 trigger용으로만 사용한다.
                    Date curDateTime = new Date();
                    tvBGDate.setText(Utils.sdf_YYYYMMDD_dot.format(curDateTime));
                    tvBGTime.setText(Utils.sdf_HHMM.format(curDateTime));
                }
            });
        }
    }


    public interface RssNewsService {
        //@GET("morningand.xml")
        @GET("news?cf=all&hl=ko&pz=1&ned=kr&output=rss")
        Call<Feed> getNewsList();
    }


    /**
     * 뉴스 가져와서 UI에 표시
     */
    public void retrofit_News() {
        final TextView tvNews = (TextView) findViewById(R.id.tvNews);
        tvNews.setText("");

        final HorizontalScrollView sv = (HorizontalScrollView)findViewById(R.id.scrollview);
        sv.scrollTo(0, 0);
        sv.setAlpha(1.0f);

        // 화면 폭만큼 추가로 스크롤하기 위해서
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        final int screenWidth = metrics.widthPixels;

        findViewById(R.id.viewBefore).getLayoutParams().width = screenWidth;
        findViewById(R.id.viewAfter).getLayoutParams().width = screenWidth;

        Retrofit retrofit = new Retrofit.Builder()
                //.baseUrl("http://fs.jtbc.joins.com/RSS/")
                .baseUrl("http://news.google.com/")
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build();

        RssNewsService service = retrofit.create(RssNewsService.class);
        final Call<Feed> news = service.getNewsList();

        news.enqueue(new Callback<Feed>() {
            @Override
            public void onResponse(Call<Feed> call, Response<Feed> response) {

                if(response.isSuccessful()) {
                    Feed feed = response.body();
                    List<FeedItem> newsList = feed.getmChannel().getFeedItems();

                    String sTitles = "<html>&nbsp; &nbsp; &nbsp; &nbsp;";
                    for (FeedItem item : newsList) {
                        sTitles += item.title;
                        sTitles += "&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;";
                    }

                    sTitles += "</html>";
                    tvNews.setText(Html.fromHtml(sTitles));

                    sv.measure(0,0);
                    int svWidth = sv.getMeasuredWidth();

                    final int nDuration = svWidth*10;

                    ObjectAnimator aniScroll = ObjectAnimator.ofInt(sv, "scrollX", svWidth);
                    aniScroll.setInterpolator(new LinearInterpolator());
                    aniScroll.setDuration(nDuration);

                    ObjectAnimator aniFadeout = ObjectAnimator.ofFloat(sv, "alpha", 0.0f);
                    aniFadeout.setInterpolator(new LinearInterpolator());
                    aniFadeout.setDuration(2000);

                    AnimatorSet animset = new AnimatorSet();
                    animset.playSequentially(aniScroll, aniFadeout);
                    animset.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    retrofit_News();
                                }
                            }, 0);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });

                    animset.start();

                }
            }

            @Override
            public void onFailure(Call<Feed> call, Throwable t) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        retrofit_News();
                    }
                }, 3000);
            }

        });
    }


    /**
     * 날씨정보 가져오기
     */
    TimerTask mWeatherUpdateTask = new TimerTask() {
        @Override
        public void run() {

            if(mApp.mSTBLocation == null) {

                // 내 좌표 가져오기
                try {

                    URL url = new URL("http://ipinfo.io/json");

                    HttpURLConnection http = (HttpURLConnection) url.openConnection();
                    http.setConnectTimeout(10000);
                    http.setRequestMethod("GET");

                    int resCode = http.getResponseCode();
                    LogUtil.d("ipinfo result code - " + resCode);

                    if (resCode == HttpURLConnection.HTTP_OK) {
                        InputStreamReader tmp = new InputStreamReader(http.getInputStream());
                        BufferedReader reader = new BufferedReader(tmp);
                        StringBuilder builder = new StringBuilder();
                        String str;
                        while ((str = reader.readLine()) != null) {
                            builder.append(str);
                        }

                        tmp.close();
                        reader.close();

                        JSONObject obj = new JSONObject(builder.toString());

                        String loc = obj.getString("loc");
                        String []arrLoc;
                        arrLoc = loc.split(",");

                        mApp.mSTBLocation = new STBLocation(Double.valueOf(arrLoc[0]), Double.valueOf(arrLoc[1]));
                        LogUtil.d("my location : " + mApp.mSTBLocation.getLatitude() + ", " + mApp.mSTBLocation.getLogitude());
                    }

                    http.disconnect();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if(mApp.mSTBLocation == null) return;

            // 좌표로 현재 날씨 가져오기
            try {
                Uri uri = new Uri.Builder()
                        .scheme("http")
                        .authority("api.openweathermap.org")
                        .path("data/2.5/weather")
                        .appendQueryParameter("lat", String.valueOf(mApp.mSTBLocation.getLatitude()))
                        .appendQueryParameter("lon", String.valueOf(mApp.mSTBLocation.getLogitude()))
                        .appendQueryParameter("APPID", SednApplication.OPENWEATHER_APPID)
                        .build();
                URL url = new URL(uri.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setRequestMethod("GET");

                int resCode = conn.getResponseCode();
                LogUtil.d("openweathermap result code - " + resCode);
                if (resCode == HttpURLConnection.HTTP_OK) {
                    String response = new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine();
                    JSONObject obj = new JSONObject(response);

                    String icon = ((JSONObject)(obj.getJSONArray("weather").get(0))).getString("icon");
                    int mWeatherImage = mApp.mMainActivity.mWeatherIconMap.get(icon);

                    double temp_val = obj.getJSONObject("main").getDouble("temp") - 273.15; // Kelvin 온도로 내려옴
                    double round_val = Math.round(temp_val*10d) / 10d;


                    String mTemperature = String.valueOf(round_val) + getResources().getString(R.string.celsius_mark);
                    LogUtil.d("my weather : " + icon + ", " + temp_val);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ImageView ivWeather = (ImageView)findViewById(R.id.ivBGWeather);
                            ivWeather.setImageResource(mWeatherImage);

                            TextView tvTemperature = (TextView) findViewById(R.id.tvBGTemperature);
                            tvTemperature.setText(mTemperature);
                        }
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Player Mode 반환
     * @return
     */
    public int getPlayerMode() {
        return mPlayerMode;
    }
}
