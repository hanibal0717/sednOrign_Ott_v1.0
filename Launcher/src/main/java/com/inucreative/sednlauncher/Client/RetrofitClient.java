package com.inucreative.sednlauncher.Client;

import android.content.Context;
import android.content.Intent;

import com.inucreative.sednlauncher.Activity.MainActivity;
import com.inucreative.sednlauncher.Activity.PlayerActivity;
import com.inucreative.sednlauncher.Client.Model.ChannelListResponse;
import com.inucreative.sednlauncher.Client.Model.ConfigResponse;
import com.inucreative.sednlauncher.Client.Model.FirmwareInfoResponse;
import com.inucreative.sednlauncher.Client.Model.LiveScheduleResponse;
import com.inucreative.sednlauncher.Client.Model.PersonalBroadcastResponse;
import com.inucreative.sednlauncher.Client.Model.ScheduleResponse;
import com.inucreative.sednlauncher.Client.Model.StbDataResponse;
import com.inucreative.sednlauncher.Client.Model.StbInfoResponse;
import com.inucreative.sednlauncher.Client.Model.TodayScheduleResponse;
import com.inucreative.sednlauncher.DataType.ChannelItem;
import com.inucreative.sednlauncher.DataType.ScheduleItem;
import com.inucreative.sednlauncher.R;
import com.inucreative.sednlauncher.SednApplication;
import com.inucreative.sednlauncher.Threads.LocalDBManager;
import com.inucreative.sednlauncher.Util.LogUtil;
import com.inucreative.sednlauncher.Util.Utils;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by apple on 2017. 5. 22..
 */

public class RetrofitClient {

    private static Context mContext;
    private static SednApplication app;
    private static LocalDBManager localDB;

    public RetrofitClient (Context context) {
        mContext = context;
        app = (SednApplication)((MainActivity)context).getApplication();
        localDB = ((MainActivity)context).mLocalDBManager;
    }

    /**
     * Base URL 계산
     * @return
     */
    private static String getBaseUrl() {

        // 나중에는 이렇게 해야하지만. 임시로 SednMgr 서버에서 가져오도록 하드코딩한다
        String result = "http://" + app.gSednServer + "/ott/";
        return result;
        //return "http://182.162.172.130/php/";
    }


    private static String convertAsHTTPPath(String path) {
        String result = "";
        if(path != null && !path.isEmpty() && !path.equals(SednApplication.SEDN_UPLOAD_ROOT))
            result = path.replace(SednApplication.SEDN_UPLOAD_ROOT, "http://" + app.gSednServer + ":" + app.gSednServerPort);
        return result;
    }

    private static String convertAsStreamingPath(String path) {
        // hls 적용 후 수정해야 함.
        return convertAsHTTPPath(path);
    }

    public static int converToIntColor(String color) {
        return (int)(Long.parseLong("ff" + color.substring(1), 16)); // #ffffff 의 형태여야 함
    }

    public static int converToIntBGColor(String color) {
        if(color == null || color.isEmpty())
            return 0;

        return (int)(Long.parseLong("99" + color.substring(1), 16)); // #ffffff 의 형태여야 함
    }

    // "HH:mm:ss" 스트링을 초단위 int로 변환
    private static int convertToIntTime(String time_str) {
        String[] part = time_str.split(":");
        int hh = Integer.parseInt(part[0]);
        int mm = Integer.parseInt(part[1]);
        int ss = Integer.parseInt(part[2]);

        return hh * 60 * 60 + mm * 60 + ss;
    }

    interface Service_CheckForUpdate {
        @GET("check_for_update.php")
        Call<FirmwareInfoResponse> checkForUpdate();
    }

    interface Service_GetStbData {
        @GET("get_stb_data.php")
        Call<StbDataResponse> getStbData();
    }

    interface Service_GetSchedule {
        @GET("get_schedule.php")
        Call<List<ScheduleResponse>> getSchedule(@Query("groupID") String groupID);
    }

    interface Service_GetTodaySchedule {
        @GET("get_today_schedule.php")
        Call<List<TodayScheduleResponse>> getTodaySchedule(@Query("groupID") String groupID);
    }

    interface Service_GetLiveSchedule {
        @GET("get_live_schedule.php")
        Call<List<LiveScheduleResponse>> getLiveSchedule(@Query("ch_idx") String ch_idx);
    }

    interface Service_GetChannelList {
        @GET("get_channel_list.php")
        Call<List<ChannelListResponse>> getChannelList();
    }

    interface Service_GetConfiguration {
        @GET("get_config.php")
        Call<ConfigResponse> getConfiguration();
    }

    interface Service_InsertVodHistory {
        @GET("insert_vod_history.php")
        Call<Void> insertVodHistory(@Query("vodID") String vod_id, @Query("myID") String myId);
    }

    interface Service_GetStbInfo {
        @GET("get_stb_info.php")
        Call<StbInfoResponse> getStbInfo(@Query("mac") String mac, @Query("ip") String ip);
    }

    interface Service_UpdateStbStatus {
        @GET("update_stb_status.php")
        Call<Void> updateStbStatus(@Query("status") int status, @Query("mac") String mac, @Query("timestamp") int add_timestamp);
    }

    interface Service_SendPing {
        @GET("send_ping.php")
        Call<Void> sendPing(@Query("mac") String mac);
    }

    interface Service_GetVodSchedule24Hour {
        @GET("get_vod_schedule_24hour.php")
        Call<List<ScheduleResponse>> getVodSchedule24Hour(@Query("groupID") String groupID);
    }

    interface Service_GetPersonalBroadcastInfo {
        @GET("get_personal_broadcast.php")
        Call<List<PersonalBroadcastResponse>> getPersonalBroadcast(@Query("groupID") String groupID);
    }

    /**
     * apk 버전체크 & 업데이트
     * @param forceUpdate
     */
    public static void checkForUpdate(boolean forceUpdate) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Service_CheckForUpdate service = retrofit.create(Service_CheckForUpdate.class);
        final Call<FirmwareInfoResponse> call = service.checkForUpdate();

        call.enqueue(new Callback<FirmwareInfoResponse>() {
            @Override
            public void onResponse(Call<FirmwareInfoResponse> call, Response<FirmwareInfoResponse> response) {

                if(response.isSuccessful()) {
                    FirmwareInfoResponse result = response.body();

                    // 셋업에서 실행된 경우
                    app.gNewFirmwareVersion = result.firmware_version;
                    app.gNewFirmwarePath = convertAsHTTPPath(result.firmware_path);
                    app.gNewFirmwareDate = result.firmware_modify_dt;
                    boolean updateNeeded = !(app.gNewFirmwareVersion == null || app.gNewFirmwareVersion.isEmpty() || app.gNewFirmwareVersion.equals(app.gFirmwareVersion));
                    LogUtil.d("checkForUpdate result : " + updateNeeded);

                    if(forceUpdate) {
                        if(updateNeeded) {
                            // 관리자페이지에서 강제실행된 경우, 일단 player동작 아닐 때만 업데이트
                            if(app.mCurrentContext instanceof MainActivity) {
                                ((MainActivity) app.mCurrentContext).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((MainActivity) app.mCurrentContext).firmwareDownloadAndUpdate();
                                    }
                                });
                            }
                        }
                        else {
                            ((MainActivity)mContext).showToast(R.string.msg_firmware_no_update);
                        }
                    } else {
                        ((MainActivity) app.mCurrentContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((MainActivity) app.mCurrentContext).redrawFirmwareResult(updateNeeded);
                            }
                        });
                    }

                }

            }

            @Override
            public void onFailure(Call<FirmwareInfoResponse> call, Throwable t) {

            }

        });
    }


    /**
     * 셋탑박스 메뉴, Vod List, Banner Image 가져오기
     */
    public static void getSTBData() {

        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Service_GetStbData service = retrofit.create(Service_GetStbData.class);
        final Call<StbDataResponse> call = service.getStbData();

        call.enqueue(new Callback<StbDataResponse>() {
            @Override
            public void onResponse(Call<StbDataResponse> call, Response<StbDataResponse> response) {

                if(response.isSuccessful()) {
                    StbDataResponse result = response.body();

                    // Menu
                    localDB.clearMenu();

                    for(StbDataResponse.StbMenu m: result.menu) {
                        localDB.putMenu(m.menu_seq, m.up_menu_seq, m.menu_name);
                    }

                    // Vod List
                    // CMS에 등록된 모든 VOD 정보를 로컬DB에 넣는다 (주석: reload 2017.4.25)
                    ArrayList<String> oldVOD = localDB.getAllVODIDs();
                    localDB.clearVOD();

                    for(StbDataResponse.StbVod v: result.vod) {

                        String fileFormat = v.VIDEO_PATH.substring(v.VIDEO_PATH.lastIndexOf(".")+1, v.VIDEO_PATH.length()).toUpperCase();
                        String thumbPath = convertAsHTTPPath(v.THUMBNAIL_PATH);
                        String videoPath = convertAsStreamingPath(v.VIDEO_PATH);

                        LogUtil.d("vod " + v.ID + ", " + v.TITLE + ", " + v.MENU + ", " + v.THUMBNAIL_PATH + ", " + v.VIDEO_PATH + ", " + v.REGISTER_DT);

                        localDB.putVOD(v.ID, v.TITLE, v.MENU, thumbPath, videoPath, v.REGISTER_DT, v.hit, v.VOD_PLAY_TIME, v.RESOLUTION, fileFormat, v.BITRATE, v.VIDEO_CODEC, v.AUDIO_CODEC);
                    }

                    localDB.refreshBookark();

                    app.banner1URL = convertAsHTTPPath(result.banner.BANNER1_IMG_PATH);
                    app.banner2URL = convertAsHTTPPath(result.banner.BANNER2_IMG_PATH);
                    LogUtil.d("banner 1 - " + app.banner1URL + ", 2 - " + app.banner2URL);


                    ArrayList<String> newVOD = localDB.getAllVODIDs();
                    // check if vod changes
                    boolean isVODchanged = false;

                    if(oldVOD.size() != newVOD.size())
                        isVODchanged = true;
                    else {
                        for(String oldID : oldVOD) {
                            if(!newVOD.contains(oldID))
                                isVODchanged = true;
                        }
                    }
                    ((MainActivity)mContext).redrawUI(isVODchanged);
                }
            }

            @Override
            public void onFailure(Call<StbDataResponse> call, Throwable t) {

            }
        });
    }


    /**
     * 방송 스케쥴 가져오기
     */
    public static void getSchedule() {

        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Service_GetSchedule service = retrofit.create(Service_GetSchedule.class);
        final Call<List<ScheduleResponse>> call = service.getSchedule(app.myGroupID);

        call.enqueue(new Callback<List<ScheduleResponse>>() {
            @Override
            public void onResponse(Call<List<ScheduleResponse>> call, Response<List<ScheduleResponse>> response) {

                if(response.isSuccessful()) {
                    List<ScheduleResponse> result = response.body();

                    app.schedule.clear();
                    Date now = new Date();
                    boolean presentSchedule = false; // 바로 방송해야하는 경우 true로 set!
                    boolean isCurrentPlay=false; // 현재 재생 중인 스케쥴과 동일한 정보인지 체크용

                    try {
                        for(ScheduleResponse sch : result) {

                            Date start = Utils.sdf_YYYYMMDDHHMMSS.parse(sch.start);
                            Date end = Utils.sdf_YYYYMMDDHHMMSS.parse(sch.end);
                            int caption_text_color = converToIntColor(sch.caption_text_color);
                            int caption_bg_color = converToIntBGColor(sch.caption_bg_color);

                            long duration = 0;
                            if(start.after(now))
                                duration = end.getTime() - start.getTime();
                            else
                                duration = end.getTime() - now.getTime();

                            LogUtil.d("schedule " + sch.id + ", " + start + ", " + end + ", " + sch.source_type + ", ");

                            Intent intent = new Intent((MainActivity)mContext, PlayerActivity.class);
                            intent.putExtra("TEMPLATE", 1);
                            intent.putExtra("END_TIME", end.getTime());     // ghlee 2017.3.29
                            intent.putExtra("START_TIME", start.getTime()); // ghlee 2017.3.29

                            if(sch.source_type.equals("LIVE")) {

                                intent.putExtra("PLAY_MODE", PlayerActivity.PLAY_SCHEDULE_LIVE);
                                intent.putExtra("TITLE", sch.name);
                                intent.putExtra("URI", sch.live_stream_url);
                                intent.putExtra("DURATION", duration);
                                intent.putExtra("CAPTION", sch.caption);
                                intent.putExtra("CAPTION_SIZE", sch.caption_size);
                                intent.putExtra("CAPTION_SPEED", sch.caption_speed);
                                intent.putExtra("CAPTION_TEXT_COLOR", caption_text_color);
                                intent.putExtra("CAPTION_BG_COLOR", caption_bg_color);
                            }
                            else if(sch.source_type.equals("VOD")) {

                                ArrayList<String> vodIDList = new ArrayList<>();
                                ArrayList<String> vodList = new ArrayList<>();
                                ArrayList<Integer> vodDurationList = new ArrayList<>();
                                for(ScheduleResponse.VOD_Json v: sch.vodlist) {
                                    vodIDList.add(v.vod_id);
                                    vodList.add(convertAsStreamingPath(v.file_path));
                                    vodDurationList.add(convertToIntTime(v.vod_play_time));
                                    LogUtil.d("fildPath added : " + v.file_path);
                                }

                                intent.putExtra("PLAY_MODE", PlayerActivity.PLAY_SCHEDULE_VOD);
                                intent.putExtra("TITLE", sch.name);
                                intent.putStringArrayListExtra("VOD_ID_LIST", vodIDList);
                                intent.putStringArrayListExtra("VOD_PATH_LIST", vodList);
                                intent.putIntegerArrayListExtra("VOD_DURATION_LIST", vodDurationList);
                                intent.putExtra("DURATION", duration);
                                intent.putExtra("CAPTION", sch.caption);
                                intent.putExtra("CAPTION_SIZE", sch.caption_size);
                                intent.putExtra("CAPTION_SPEED", sch.caption_speed);
                                intent.putExtra("CAPTION_TEXT_COLOR", caption_text_color);
                                intent.putExtra("CAPTION_BG_COLOR", caption_bg_color);
                                LogUtil.d("VOD added : " + vodList.size());
                                LogUtil.d("" + vodList.toArray(new String[vodList.size()]));
                            }

                            LogUtil.d("Set schedule " + sch.name);

                            String startTime = Utils.sdf_YYYYMMDDHHMM.format(start);
                            String nowTime = Utils.sdf_YYYYMMDDHHMM.format(now);

                            LogUtil.d("new schedule - " + startTime + " / " + nowTime);

                            if(start.after(now)) {
                                // 현재시간 이후의 스케쥴이라면 Map에 추가
                                app.schedule.put(startTime, intent);
                                LogUtil.d("put schedule : " + startTime);
                            } else {
                                // 바로 방송해야할 스케쥴 일때 (현재시간 이전에 시작되었어야 하는 스케쥴)
                                presentSchedule = true;

                                // 현재 플레이어가 재생 중일때 쿼리결과의 스케쥴과 현재 재생중인 스케쥴정보와 비교한다
                                if(app.mCurrentContext instanceof PlayerActivity) {
                                    PlayerActivity player = (PlayerActivity)app.mCurrentContext;
                                    Intent iPlayer = player.getIntent();
                                    // 재생정보 인텐트 비교
                                    isCurrentPlay = Utils.isEqualsPlayerIntent(iPlayer, intent);
                                }

                                // 현재 재생중인 정보와 다르다면 새로운 방송을 시작한다.
                                if(!isCurrentPlay) {
                                    mContext.sendBroadcast(new Intent(SednApplication.ACTION_SEDN_STOP_PLAYER));

                                    LogUtil.d("already playing schedule : " + startTime);
                                    intent.putExtra("ELAPSED", now.getTime() - start.getTime());
                                    ((MainActivity) mContext).startBroadcast(intent);
                                }
                            }
                        } // While Loop End

                        LogUtil.d("app.getStatus : " + app.getStatus() + ", present " + presentSchedule);

                        // 업데이트결과 현재 방송중인 스케줄이 없으면, 삭제된 것이므로 방송 중단
                        // 현재 vod play 중이라면 중지시키면 안되지!!!! (add ghlee)
                        if(!presentSchedule && app.getStatus() != SednApplication.STATUS_VOD) {
                            LogUtil.d("player force stop");
                            mContext.sendBroadcast(new Intent(SednApplication.ACTION_SEDN_STOP_PLAYER));
                        }

                        if (app.getStatus() == SednApplication.STATUS_ON)   // 토스트 메시지는 방송중에는 출력하지 않는다.
                            ((MainActivity)mContext).showToast(R.string.msg_schedule_updated);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ScheduleResponse>> call, Throwable t) {

            }
        });
    }


    /**
     * 오늘의 방송일정 가져오기
     * @return
     */
    public static ArrayList<ScheduleItem> getTodaySchedule() {

        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return null;

        ArrayList<ScheduleItem> resultList = new ArrayList<>();

        Thread apiThread = new Thread(() -> {

            // 스레드 내부에서 동기방식으로 호출한다.
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(getBaseUrl())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            Service_GetTodaySchedule service = retrofit.create(Service_GetTodaySchedule.class);
            Call<List<TodayScheduleResponse>> call = service.getTodaySchedule(app.myGroupID);

            try {
                List<TodayScheduleResponse> result = call.execute().body();

                for(TodayScheduleResponse sch : result) {
                    ScheduleItem item = new ScheduleItem();
                    item.name = sch.name;
                    item.image_url = convertAsHTTPPath(sch.image_path);
                    item.durationStr = sch.duration;
                    item.desc = sch.desc_text;
                    item.target = sch.target;

                    resultList.add(item);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        apiThread.start();

        try {
            // 스레드가 종료될때까지 대기한다.
            apiThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultList;

    }


    /**
     * 라이브방송일정 가져오기 - 채널별로
     * @param ch_idx
     * @return
     */
    public static ArrayList<ScheduleItem> getLiveScheduleList(String ch_idx) {

        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return null;

        ArrayList<ScheduleItem> resultList = new ArrayList<>();

        Thread apiThread = new Thread(() -> {

            // 스레드 내부에서 동기방식으로 호출한다.
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(getBaseUrl())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            Service_GetLiveSchedule service = retrofit.create(Service_GetLiveSchedule.class);
            Call<List<LiveScheduleResponse>> call = service.getLiveSchedule(ch_idx);

            try {
                List<LiveScheduleResponse> result = call.execute().body();

                for(LiveScheduleResponse sch : result) {
                    ScheduleItem item = new ScheduleItem();
                    item.name = sch.name;
                    item.durationStr = sch.duration;
                    item.start_time = sch.start;
                    item.end_time = sch.end;
                    item.image_url = convertAsHTTPPath(sch.image_path);
                    item.desc = sch.desc_text;
                    item.color_code = converToIntColor(sch.color);
                    item.target = sch.target;
                    item.dateStr = sch.play_date;
                    item.playTimeStr = sch.play_time;

                    resultList.add(item);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        apiThread.start();

        try {
            // 스레드가 종료될때까지 대기한다.
            apiThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultList;

    }


    /**
     * Live 채널 목록을 가져온다.
     * @return
     */
    public static ArrayList<ChannelItem> getChannelList() {

        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return null;

        ArrayList<ChannelItem> resultList = new ArrayList<>();

        Thread apiThread = new Thread(() -> {

            // 스레드 내부에서 동기방식으로 호출한다.
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(getBaseUrl())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            Service_GetChannelList service = retrofit.create(Service_GetChannelList.class);
            Call<List<ChannelListResponse>> call = service.getChannelList();

            try {
                List<ChannelListResponse> result = call.execute().body();

                for(ChannelListResponse r : result) {
                    ChannelItem item = new ChannelItem();
                    item.channelIndex = r.ch_idx;
                    item.channelName = r.ch_nm;

                    resultList.add(item);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        apiThread.start();

        try {
            // 스레드가 종료될때까지 대기한다.
            apiThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultList;
    }


    /**
     * DB테이블에서 STB 환경설정 값을 읽어온다.
     */
    public static void getConfiguration() {

        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Service_GetConfiguration service = retrofit.create(Service_GetConfiguration.class);
        final Call<ConfigResponse> call = service.getConfiguration();

        call.enqueue(new Callback<ConfigResponse>() {
            @Override
            public void onResponse(Call<ConfigResponse> call, Response<ConfigResponse> response) {

                if(response.isSuccessful()) {
                    ConfigResponse r = response.body();

                    try {
                        // 1. 스트리밍 서버 URL 설정 ...........................
                        if(r.STREAMING_SERVER_URL != null)
                            app.gStreamingServer = r.STREAMING_SERVER_URL;

                        // 2. Logo 설정 ....................................
                        boolean logoUpdated = false;
                        Date logo_modify_dt = Utils.sdf_YYYYMMDDHHMMSS.parse(r.LOGO_MODIFY_DT);

                        // 로고 업데이트 시간 비교 - 서버측 수정일 이전에 변경했다면 변경한다.
                        Date last_logo_dt = Utils.getLastLogoDT(mContext);
                        if(last_logo_dt == null || last_logo_dt.before(logo_modify_dt)) {
                            LogUtil.d("logo should be updated - " + last_logo_dt + " < " + logo_modify_dt);
                            Utils.setLastLogoDT(mContext, logo_modify_dt);
                            Utils.setSTBLogoImageYN(mContext, r.LOGO_IMG_YN);
                            if(r.LOGO_IMG_YN.equals("Y")) {
                                if(r.LOGO_IMG_PATH != null)
                                    ((MainActivity)mContext).downloadConfFile(convertAsHTTPPath(r.LOGO_IMG_PATH), "LOGO", SednApplication.LOGO_IMAGE_FILE_NAME);
                            } else {
                                Utils.setSTBLogoText(mContext, r.LOGO_TEXT);
                                ((MainActivity)mContext).runOnUiThread(() -> ((MainActivity)mContext).setLogoText());
                            }
                            logoUpdated = true;
                        }

                        if(!logoUpdated) {
                            ((MainActivity)mContext).runOnUiThread(() -> ((MainActivity)mContext).restoreSavedLogo());
                        }

                        // 3. 배경 이미지/비디오 설정 ..........................
                        Date bg_modify_dt = Utils.sdf_YYYYMMDDHHMMSS.parse(r.BG_MODIFY_DT);

                        // 배경 업데이트 시간 비교
                        Date last_bg_dt = Utils.getLastBGDT(mContext);
                        if(last_bg_dt == null || last_bg_dt.before(bg_modify_dt)) {
                            LogUtil.d("Background should be updated - " + last_bg_dt + " < " + bg_modify_dt);
                            Utils.setLastBGDT(mContext, bg_modify_dt);
                            Utils.setSTBBGImageYN(mContext, r.BG_IMG_YN);
                            if(r.BG_IMG_YN.equals("Y")) {
                                if(r.BG_IMG_PATH != null)
                                    ((MainActivity)mContext).downloadConfFile(convertAsHTTPPath(r.BG_IMG_PATH), "BG_IMG", SednApplication.BG_IMAGE_FILE_NAME);
                            } else {
                                if(r.BG_VIDEO_PATH != null)
                                    ((MainActivity)mContext).downloadConfFile(convertAsHTTPPath(r.BG_VIDEO_PATH), "BG_VIDEO", SednApplication.BG_VIDEO_FILE_NAME);
                            }
                        }

                        // 배경 이미지(영상)은 DB 설정 확인 후 저장 상태 복원
                        ((MainActivity)mContext).runOnUiThread(() -> ((MainActivity)mContext).restoreBackground());

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ConfigResponse> call, Throwable t) {

            }
        });
    }

    /**
     * VOD Play Log 전송
     * @param vod_id
     */
    public static void insertVODHistory(String vod_id) {

        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Service_InsertVodHistory service = retrofit.create(Service_InsertVodHistory.class);
        final Call<Void> call = service.insertVodHistory(vod_id, app.myID);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {

            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });
    }


    /**
     * STB 정보를 가져오고 없으면 자동으로 등록한다.
     */
    public static void getSTBInfo() {

        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Service_GetStbInfo service = retrofit.create(Service_GetStbInfo.class);
        final Call<StbInfoResponse> call = service.getStbInfo(app.myMAC, app.myIP);

        call.enqueue(new Callback<StbInfoResponse>() {
            @Override
            public void onResponse(Call<StbInfoResponse> call, Response<StbInfoResponse> response) {

                if(response.isSuccessful()) {
                    StbInfoResponse r = response.body();

                    app.myID = r.id;
                    app.myName = r.stb_name;
                    app.myGroupID = r.group_id;
                    app.myGroupName = r.group_name;

                    if(1 == r.is_new) {
                        ((MainActivity)mContext).showToast(mContext.getResources().getString(R.string.msg_new_device_registered));
                    }

                    ((MainActivity)mContext).redrawSTBInfo();
                }
            }

            @Override
            public void onFailure(Call<StbInfoResponse> call, Throwable t) {

            }
        });
    }

    /**
     * 셋탑박스 상태 업데이트
     * 1: off, 2: on, 3: vod, 4: broadcast
     * @param updateTimestamp
     */
    public static void updateSTBStatus(boolean updateTimestamp) {

        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Service_UpdateStbStatus service = retrofit.create(Service_UpdateStbStatus.class);

        int add_timestamp = 0;

        if(updateTimestamp) {
            add_timestamp = 1;
        }

        final Call<Void> call = service.updateStbStatus(app.getStatus(), app.myMAC , add_timestamp);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {

            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });
    }


    /**
     * 주기적으로 셋탑박스가 구동중임을 알린다.
     */
    public static void sendPing() {

        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Service_SendPing service = retrofit.create(Service_SendPing.class);
        Call<Void> call = service.sendPing(app.myMAC);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {

            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {

            }
        });
    }



    /**
     * 오늘의 VOD방송 스케쥴 가져오기 - 파일 미리 다운로드 기능을 위해서 필요
     * 현재로부터 24시간 이내의 VOD스케쥴을 가져온다.
     * 2017.4.24 ghlee --> http api 방식으로 변경 2017.5.26
     */
    public static void getVodSchedule_24Hour() {

        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getBaseUrl())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Service_GetVodSchedule24Hour service = retrofit.create(Service_GetVodSchedule24Hour.class);
        final Call<List<ScheduleResponse>> call = service.getVodSchedule24Hour(app.myGroupID);

        call.enqueue(new Callback<List<ScheduleResponse>>() {
            @Override
            public void onResponse(Call<List<ScheduleResponse>> call, Response<List<ScheduleResponse>> response) {

                if(response.isSuccessful()) {
                    List<ScheduleResponse> result = response.body();

                    app.todaySchedule.clear();
                    Date now = new Date();

                    try {
                        for(ScheduleResponse sch : result) {
                            String id = sch.id;
                            Date start = Utils.sdf_YYYYMMDDHHMMSS.parse(sch.start);
                            String sourceType = sch.source_type;

                            Intent intent = new Intent((MainActivity)mContext, PlayerActivity.class);


                            if(sourceType.equals("VOD")) {

                                ArrayList<String> vodIDList = new ArrayList<>();
                                ArrayList<String> vodList = new ArrayList<>();

                                for(ScheduleResponse.VOD_Json v : sch.vodlist) {
                                    String vodID = v.vod_id;
                                    String filePath = convertAsStreamingPath(v.file_path);
                                    vodIDList.add(vodID);
                                    vodList.add(filePath);

                                    LogUtil.d("fildPath added : " + filePath);
                                }
                                intent.putExtra("PLAY_MODE", PlayerActivity.PLAY_SCHEDULE_VOD);
                                intent.putStringArrayListExtra("VOD_ID_LIST", vodIDList);
                                intent.putStringArrayListExtra("VOD_PATH_LIST", vodList);
                                LogUtil.d("VOD added : " + vodList.size());
                                LogUtil.d("" + vodList.toArray(new String[vodList.size()]));
                            }

                            String startTime = Utils.sdf_YYYYMMDDHHMM.format(start);
                            String nowTime = Utils.sdf_YYYYMMDDHHMM.format(now);

                            LogUtil.d("new schedule - " + startTime + " / " + nowTime);

                            if(start.after(now)) {
                                // 현재시간 이후의 스케쥴이라면 Map에 추가
                                app.todaySchedule.put(startTime, intent);
                                LogUtil.d("put schedule : " + startTime);
                            }

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ScheduleResponse>> call, Throwable t) {

            }
        });
    }


    /**
     * 개인방송 정보 가져오기
     */
    public static ArrayList<PersonalBroadcastResponse> getPersonalBroadcast() {

        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return null;

        ArrayList<PersonalBroadcastResponse> resultList = new ArrayList<>();

        Thread apiThread = new Thread(() -> {

            // 스레드 내부에서 동기방식으로 호출한다.
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(getBaseUrl())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            Service_GetPersonalBroadcastInfo service = retrofit.create(Service_GetPersonalBroadcastInfo.class);
            Call<List<PersonalBroadcastResponse>> call = service.getPersonalBroadcast(app.myGroupID);

            try {
                List<PersonalBroadcastResponse> result = call.execute().body();

                for(PersonalBroadcastResponse item : result) {
                    resultList.add(item);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        apiThread.start();

        try {
            // 스레드가 종료될때까지 대기한다.
            apiThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultList;

    }

}
