package com.inucreative.sednlauncher.Threads;

import android.content.Context;
import android.content.Intent;

import com.inucreative.sednlauncher.Activity.MainActivity;
import com.inucreative.sednlauncher.Activity.PlayerActivity;
import com.inucreative.sednlauncher.DataType.ChannelItem;
import com.inucreative.sednlauncher.DataType.ScheduleItem;
import com.inucreative.sednlauncher.R;
import com.inucreative.sednlauncher.SednApplication;
import com.inucreative.sednlauncher.Util.LogUtil;
import com.inucreative.sednlauncher.Util.Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Jskim on 2016-07-25.
 */
public class SednDBClient {
    private static Context mContext;
    private static SednApplication app;
    private static LocalDBManager localDB;

    public SednDBClient (Context context) {
        mContext = context;
        app = (SednApplication)((MainActivity)context).getApplication();
        localDB = ((MainActivity)context).mLocalDBManager;
    }

    private static Connection connectSednDB() {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return null;

        Connection conn = null;

        //String connStr = "jdbc:mysql://" + app.gSednServer + ":" + app.gDBPort + "/" + app.gDBName;
        String connStr = "jdbc:mariadb://" + app.gDBIP + ":" + app.gDBPort + "/" + app.gDBName;
        String connID = app.gDBUserID;
        String connPW = app.gDBPW;

        try {
            conn = DriverManager.getConnection(connStr, connID, connPW);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return conn;
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

    // "HH:mm:ss" 스트링을 초단위 int로 변환
    private static int convertToIntTime(String time_str) {
        String[] part = time_str.split(":");
        int hh = Integer.parseInt(part[0]);
        int mm = Integer.parseInt(part[1]);
        int ss = Integer.parseInt(part[2]);

        return hh * 60 * 60 + mm * 60 + ss;
    }

    // Templete 함수
    public static void sample() {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection mySQLConnection = connectSednDB();
                    if(mySQLConnection == null) return;
                    Statement stmt = mySQLConnection.createStatement();
                    String query = "";
                    stmt.executeUpdate(query);

                    mySQLConnection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cmsConnectThread.start();
    }

    public static void checkForUpdate(boolean forceUpdate) {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String firmware_version = "";
                String firmware_path = "";
                String firmware_modify_dt = "";

                try {
                    Connection mySQLConnection = connectSednDB();
                    if(mySQLConnection == null) return;
                    Statement stmt = mySQLConnection.createStatement();
                    String query = "select firmware_version, firmware_path, firmware_modify_dt from tb_stb_configuration";
                    ResultSet resultSet = stmt.executeQuery(query);
                    while(resultSet.next()) {
                        firmware_version = resultSet.getString("firmware_version");
                        firmware_path = resultSet.getString("firmware_path");
                        firmware_modify_dt = resultSet.getString("firmware_modify_dt");
                    }

                    // 셋업에서 실행된 경우
                    app.gNewFirmwareVersion = firmware_version;
                    app.gNewFirmwarePath = convertAsHTTPPath(firmware_path);
                    app.gNewFirmwareDate = firmware_modify_dt;
                    boolean updateNeeded = !(app.gNewFirmwareVersion == null || app.gNewFirmwareVersion.isEmpty() || app.gNewFirmwareVersion.equals(app.gFirmwareVersion));
                    LogUtil.d("checkForUpdate result : " + updateNeeded);
                    if(forceUpdate) {
                        if(updateNeeded) {
                            // 관리자페이지에서 강제실행된 경우, 일단 player동작 아닐 때만 업데이트
                            if(app.mCurrentContext instanceof MainActivity) {
                                ((MainActivity) mContext).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((MainActivity) mContext).firmwareDownloadAndUpdate();
                                    }
                                });
                            }
                        }
                    } else {
                        ((MainActivity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((MainActivity) mContext).redrawFirmwareResult(updateNeeded);
                            }
                        });
                    }

                    mySQLConnection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cmsConnectThread.start();
    }

    public static void updateStreamingURL() {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String streaming_server_url = null;

                try {
                    Connection mySQLConnection = connectSednDB();
                    if(mySQLConnection == null) return;
                    Statement stmt = mySQLConnection.createStatement();
                    String query = "select STREAMING_SERVER_URL from TB_STB_CONFIGURATION";
                    ResultSet resultSet = stmt.executeQuery(query);
                    while(resultSet.next()) {
                        streaming_server_url = resultSet.getString("STREAMING_SERVER_URL");
                    }

                    if(streaming_server_url != null)
                        app.gStreamingServer = streaming_server_url;

                    mySQLConnection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cmsConnectThread.start();
    }

    public static void updateSTBLogo() {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String logo_img_yn = null;
                String logo_img_path = null;
                String logo_text = null;
                Date logo_modify_dt = null;
                boolean logoUpdated = false;

                try {
                    Connection mySQLConnection = connectSednDB();
                    if(mySQLConnection == null) return;
                    Statement stmt = mySQLConnection.createStatement();
                    String query = "select LOGO_IMG_YN, LOGO_IMG_PATH, LOGO_TEXT, LOGO_MODIFY_DT from TB_STB_CONFIGURATION";
                    ResultSet resultSet = stmt.executeQuery(query);
                    while(resultSet.next()) {
                        logo_img_yn = resultSet.getString("LOGO_IMG_YN");
                        logo_img_path = resultSet.getString("LOGO_IMG_PATH");
                        logo_text = resultSet.getString("LOGO_TEXT");
                        logo_modify_dt = Utils.sdf_YYYYMMDDHHMMSS.parse(resultSet.getString("LOGO_MODIFY_DT"));
                    }

                    // 업데이트 시간 비교
                    Date last_logo_dt = Utils.getLastLogoDT(mContext);
                    if(last_logo_dt == null || last_logo_dt.before(logo_modify_dt)) {
                        LogUtil.d("logo should be updated - " + last_logo_dt + " < " + logo_modify_dt);
                        Utils.setLastLogoDT(mContext, logo_modify_dt);
                        Utils.setSTBLogoImageYN(mContext, logo_img_yn);
                        if(logo_img_yn.equals("Y")) {
                            if(logo_img_path != null)
                                ((MainActivity)mContext).downloadConfFile(convertAsHTTPPath(logo_img_path), "LOGO", SednApplication.LOGO_IMAGE_FILE_NAME);
                        } else {
                            Utils.setSTBLogoText(mContext, logo_text);
                            ((MainActivity)mContext).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((MainActivity)mContext).setLogoText();
                                }
                            });
                        }
                        logoUpdated = true;
                    }
                    mySQLConnection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if(!logoUpdated) {
                    ((MainActivity)mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((MainActivity)mContext).restoreSavedLogo();
                        }
                    });
                }
            }
        });
        cmsConnectThread.start();
    }

    public static void updateSTBBG() {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String bg_img_yn = null;
                String bg_img_path = null;
                String bg_video_path = null;
                Date bg_modify_dt = null;

                try {
                    Connection mySQLConnection = connectSednDB();
                    if(mySQLConnection == null) return;
                    Statement stmt = mySQLConnection.createStatement();
                    String query = "select BG_IMG_YN, BG_IMG_PATH, BG_VIDEO_PATH, BG_MODIFY_DT from TB_STB_CONFIGURATION";
                    ResultSet resultSet = stmt.executeQuery(query);
                    while(resultSet.next()) {
                        bg_img_yn = resultSet.getString("BG_IMG_YN");
                        bg_img_path = resultSet.getString("BG_IMG_PATH");
                        bg_video_path = resultSet.getString("BG_VIDEO_PATH");
                        bg_modify_dt = Utils.sdf_YYYYMMDDHHMMSS.parse(resultSet.getString("BG_MODIFY_DT"));
                    }

                    // 업데이트 시간 비교
                    Date last_bg_dt = Utils.getLastBGDT(mContext);
                    if(last_bg_dt == null || last_bg_dt.before(bg_modify_dt)) {
                        LogUtil.d("Background should be updated - " + last_bg_dt + " < " + bg_modify_dt);
                        Utils.setLastBGDT(mContext, bg_modify_dt);
                        Utils.setSTBBGImageYN(mContext, bg_img_yn);
                        if(bg_img_yn.equals("Y")) {
                            if(bg_img_path != null)
                                ((MainActivity)mContext).downloadConfFile(convertAsHTTPPath(bg_img_path), "BG_IMG", SednApplication.BG_IMAGE_FILE_NAME);
                        } else {
                            if(bg_video_path != null)
                                ((MainActivity)mContext).downloadConfFile(convertAsHTTPPath(bg_video_path), "BG_VIDEO", SednApplication.BG_VIDEO_FILE_NAME);
                        }
                    }
                    mySQLConnection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 배경 이미지(영상)은 DB 설정 확인 후 저장 상태 복원
                ((MainActivity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((MainActivity)mContext).restoreBackground();
                    }
                });

            }
        });
        cmsConnectThread.start();
    }

    public static void insertVODHistory(String vod_id) {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Date now = new Date();
                    String curDate = Utils.sdf_YYYYMMDD.format(now);
                    String curTime = Utils.sdf_HHMMSS.format(now);
                    Connection mySQLConnection = connectSednDB();
                    if(mySQLConnection == null) return;
                    Statement stmt = mySQLConnection.createStatement();
                    String query = "insert into tb_stb_vod_history(VOD_IDX, STB_ID, PLAY_DATE, PLAY_TIME) values('" + vod_id + "', '" + app.myID + "', '" + curDate + "', '" + curTime + "')";;
                    LogUtil.d(query);
                    stmt.executeUpdate(query);

                    mySQLConnection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cmsConnectThread.start();
    }

    // STB 정보를 가져오고 없으면 자동으로 등록한다.
    public static void getSTBInfo() {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection mySQLConnection = connectSednDB();
                    if(mySQLConnection == null) return;
                    LogUtil.d("getSTBInfo connection " + mySQLConnection);

                    Statement stmt = mySQLConnection.createStatement();
                    String query = "select a.id as id, a.name as stb_name, a.group_id as group_id, b.name as group_name from tb_stb a, tb_stb_group b where a.group_id = b.id and mac='" + app.myMAC + "'";
                    LogUtil.d(query);
                    ResultSet resultSet = stmt.executeQuery(query);
                    if(resultSet.next() == false) {
                        // 미등록 장비인 경우 신규 등록
                        LogUtil.d("register new STB");
                        app.myID = "0";
                        app.myName = mContext.getResources().getString(R.string.str_new_device);
                        app.myGroupID = "1";
                        app.myGroupName = "ROOT";

                        String insert_query = "insert into tb_stb (name, mac, ip_addr, group_id, status, last_on_time, reg_dt) values ('" + app.myName + "', '" + app.myMAC + "', '" + app.myIP + "', 1, " + app.STATUS_ON + ", '" + Utils.sdf_YYYYMMDDHHMMSS.format(new Date()) + "', '" + Utils.sdf_YYYYMMDDHHMMSS.format(new Date()) + "')";
                        LogUtil.d(insert_query);
                        stmt.executeUpdate(insert_query);
                        ((MainActivity)mContext).showToast(mContext.getResources().getString(R.string.msg_new_device_registered));
                    } else {
                        String id = resultSet.getString("id");
                        String stb_name = resultSet.getString("stb_name");
                        String group_id = resultSet.getString("group_id");
                        String group_name = resultSet.getString("group_name");
                        app.myID = id;
                        app.myName = stb_name;
                        app.myGroupID  =group_id;
                        app.myGroupName = group_name;
                        LogUtil.d("STB info " + id + ", " + stb_name + ", " + group_id + ", " + group_name);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                ((MainActivity)mContext).redrawSTBInfo();
            }
        });
        cmsConnectThread.start();
    }

    public static void updateSTBStatus(boolean updateTimestamp) {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection mySQLConnection = connectSednDB();
                    if(mySQLConnection == null) return;
                    Statement stmt = mySQLConnection.createStatement();

                    String query = "update tb_stb set status = " + app.getStatus();
                    if(updateTimestamp) {
                        query += ", last_on_time = '" + Utils.sdf_YYYYMMDDHHMMSS.format(new Date()) + "'";
                    }
                    query += " where mac='" + app.myMAC + "'";
                    LogUtil.d(query);
                    stmt.executeUpdate(query);

                    mySQLConnection.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cmsConnectThread.start();
    }

    public static void sendPing() {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection mySQLConnection = connectSednDB();
                    if(mySQLConnection == null) return;
                    Statement stmt = mySQLConnection.createStatement();

                    String query = "update tb_stb set last_ping_time = now()";
                    query += " where mac='" + app.myMAC + "'";
                    LogUtil.d(query);
                    stmt.executeUpdate(query);

                    mySQLConnection.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cmsConnectThread.start();
    }

    // Menu, VOD, Channel 등등... 데이터 싱크
    public static void getSTBData() {

        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection mySQLConnection = connectSednDB();
                    if(mySQLConnection == null) return;
                    LogUtil.d("getSTBData connection " + mySQLConnection);

                    Statement stmt = mySQLConnection.createStatement();
                    ResultSet resultSet = stmt.executeQuery("select menu_seq, up_menu_seq, menu_name from tb_user_menu");
                    localDB.clearMenu();
                    while(resultSet.next()) {
                        String id = resultSet.getString("menu_seq");
                        String up_id = resultSet.getString("up_menu_seq");
                        String name = resultSet.getString("menu_name");
                        localDB.putMenu(id, up_id, name);
                    }

                    // CMS에 등록된 모든 VOD 정보를 로컬DB에 넣는다 (주석: reload 2017.4.25)
                    ArrayList<String> oldVOD = localDB.getAllVODIDs();
                    localDB.clearVOD();
                    String query = "select a.VOD_IDX as ID, a.VOD_TITLE as TITLE, a.MENU as MENU, b.FILE_PATH as THUMBNAIL_PATH, c.FILE_PATH as VIDEO_PATH, a.REG_DT as REGISTER_DT, a.VOD_PLAY_TIME"
                            + ", c.RESOLUTION as RESOLUTION, c.BITRATE as BITRATE, c.VIDEO_CODEC as VIDEO_CODEC, c.AUDIO_CODEC as AUDIO_CODEC"
                            + " from tb_vod_data a , tb_attach_file b, tb_attach_file c"
                            + " where a.VOD_IDX = b.DATA_IDX and a.VOD_IDX = C.DATA_IDX and a.del_yn='N'"
                            + " and b.gubun='T' and b.main_img_falg='Y' and c.GUBUN='M' and c.trans = '00'";
                    resultSet = stmt.executeQuery(query);
                    while(resultSet.next()) {
                        String id = resultSet.getString("ID");
                        String title = resultSet.getString("TITLE");
                        String menu = resultSet.getString("MENU");
                        String thumbPath = resultSet.getString("THUMBNAIL_PATH");
                        thumbPath = convertAsHTTPPath(thumbPath);
                        String videoPath = resultSet.getString("VIDEO_PATH");
                        videoPath = convertAsStreamingPath(videoPath);
                        String registerDT = resultSet.getString("REGISTER_DT");
                        String playTime = resultSet.getString("VOD_PLAY_TIME");
                        LogUtil.d("vod " + id + ", " + title + ", " + menu + ", " + thumbPath + ", " + videoPath + ", " + registerDT);

                        String resolution = resultSet.getString("RESOLUTION");
                        String fileFormat = videoPath.substring(videoPath.lastIndexOf(".")+1, videoPath.length()).toUpperCase();
                        String bitrate = resultSet.getString("BITRATE");
                        String videoCodec = resultSet.getString("VIDEO_CODEC").toUpperCase();
                        String audioCodec = resultSet.getString("AUDIO_CODEC").toUpperCase();

                        String hit = "0";
                        String query1 = "select count(*) as hit from tb_stb_vod_history where vod_idx = " + id;
                        Statement stmt1 = mySQLConnection.createStatement();
                        ResultSet resultSet1 = stmt1.executeQuery(query1);
                        while(resultSet1.next()) {
                            hit = resultSet1.getString("hit");
                        }
                        localDB.putVOD(id, title, menu, thumbPath, videoPath, registerDT, hit, playTime, resolution, fileFormat, bitrate, videoCodec, audioCodec);
                    }
                    localDB.refreshBookark();

                    resultSet = stmt.executeQuery("select BANNER1_IMG_PATH, BANNER2_IMG_PATH from tb_stb_configuration");
                    while(resultSet.next()) {
                        String banner1 = resultSet.getString("BANNER1_IMG_PATH");
                        String banner2 = resultSet.getString("BANNER2_IMG_PATH");
                        app.banner1URL = convertAsHTTPPath(banner1);
                        app.banner2URL = convertAsHTTPPath(banner2);
                        LogUtil.d("banner 1 - " + app.banner1URL + ", 2 - " + app.banner2URL);
                    }

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
                    mySQLConnection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cmsConnectThread.start();
    }

    /**
     * 방송 스케쥴 가져오기
     */
    public static void getSchedule() {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection mySQLConnection = connectSednDB();;
                    if(mySQLConnection == null) return;
                    LogUtil.d("getSchedule connection " + mySQLConnection);

                    Statement stmt = mySQLConnection.createStatement();

                    // 방송 스케줄 가져오기
                    //String query = "select id, name, start, end, source_type, caption from tb_stb_schedule where target_type = 'ALL' and start > now() order by start";
                    String query = "select id, name, start, end, source_type, caption, caption_size, caption_speed, caption_text_color, caption_bg_color from tb_stb_schedule A left join tb_stb_schedule_group B on A.ID = B.SCHEDULE_ID"
                                    + " where (target_type = 'ALL' or (target_type = 'GROUP' and B.GROUP_ID = " + app.myGroupID + ")) and end > now() group by id order by start";
                    ResultSet resultSet = stmt.executeQuery(query);
                    LogUtil.d("getSchedule query - " + query);
                    app.schedule.clear();
                    Date now = new Date();
                    boolean presentSchedule = false; // 바로 방송해야하는 경우 true로 set!
                    boolean isCurrentPlay=false; // 현재 재생 중인 스케쥴과 동일한 정보인지 체크용

                    while(resultSet.next()) {
                        String id = resultSet.getString("id");
                        String name = resultSet.getString("name");
                        Date start = Utils.sdf_YYYYMMDDHHMMSS.parse(resultSet.getString("start"));
                        Date end = Utils.sdf_YYYYMMDDHHMMSS.parse(resultSet.getString("end"));
                        String sourceType = resultSet.getString("source_type");
                        String caption = resultSet.getString("caption");
                        int caption_size = resultSet.getInt("caption_size");
                        int caption_speed = resultSet.getInt("caption_speed");
                        int caption_text_color = converToIntColor(resultSet.getString("caption_text_color"));
                        int caption_bg_color = converToIntBGColor(resultSet.getString("caption_bg_color"));
                        long duration = 0;
                        if(start.after(now))
                            duration = end.getTime() - start.getTime();
                        else
                            duration = end.getTime() - now.getTime();

                        LogUtil.d("schedule " + id + ", " + start + ", " + end + ", " + sourceType + ", ");

                        Statement stmt1 = mySQLConnection.createStatement();
                        Intent intent = new Intent((MainActivity)mContext, PlayerActivity.class);
                        intent.putExtra("TEMPLATE", 1);
                        intent.putExtra("END_TIME", end.getTime());     // ghlee 2017.3.29
                        intent.putExtra("START_TIME", start.getTime()); // ghlee 2017.3.29

                        if(sourceType.equals("LIVE")) {
                            String query1 = "select live_stream_url from tb_stb_schedule where id = " + id;
                            LogUtil.d(query1);
                            ResultSet resultSet1 = stmt1.executeQuery(query1);
                            while(resultSet1.next()) {
                                String liveStreamUrl = resultSet1.getString("live_stream_url");
                                intent.putExtra("PLAY_MODE", PlayerActivity.PLAY_SCHEDULE_LIVE);
                                intent.putExtra("TITLE", name);
                                intent.putExtra("URI", liveStreamUrl);
                                intent.putExtra("DURATION", duration);
                                intent.putExtra("CAPTION", caption);
                                intent.putExtra("CAPTION_SIZE", caption_size);
                                intent.putExtra("CAPTION_SPEED", caption_speed);
                                intent.putExtra("CAPTION_TEXT_COLOR", caption_text_color);
                                intent.putExtra("CAPTION_BG_COLOR", caption_bg_color);
                            }
                        } else if(sourceType.equals("VOD")) {
                            String query1 = "select a.vod_id, file_path, b.vod_play_time from tb_stb_schedule_vod a, tb_vod_data b, tb_attach_file c where a.vod_id = b.VOD_IDX and b.VOD_IDX = c.DATA_IDX and c.GUBUN='M' and c.trans = '00' and a.schedule_id="+id+" order by play_order";
                            ResultSet resultSet1 = stmt1.executeQuery(query1);
                            ArrayList<String> vodIDList = new ArrayList<>();
                            ArrayList<String> vodList = new ArrayList<>();
                            ArrayList<Integer> vodDurationList = new ArrayList<>();
                            while(resultSet1.next()) {
                                String vodID = resultSet1.getString("vod_id");
                                String filePath = resultSet1.getString("file_path");
                                String playTime = resultSet1.getString("vod_play_time");
                                filePath = convertAsStreamingPath(filePath);
                                vodIDList.add(vodID);
                                vodList.add(filePath);
                                vodDurationList.add(convertToIntTime(playTime));
                                LogUtil.d("fildPath added : " + filePath);
                            }
                            intent.putExtra("PLAY_MODE", PlayerActivity.PLAY_SCHEDULE_VOD);
                            intent.putExtra("TITLE", name);
                            intent.putStringArrayListExtra("VOD_ID_LIST", vodIDList);
                            intent.putStringArrayListExtra("VOD_PATH_LIST", vodList);
                            intent.putIntegerArrayListExtra("VOD_DURATION_LIST", vodDurationList);
                            intent.putExtra("DURATION", duration);
                            intent.putExtra("CAPTION", caption);
                            intent.putExtra("CAPTION_SIZE", caption_size);
                            intent.putExtra("CAPTION_SPEED", caption_speed);
                            intent.putExtra("CAPTION_TEXT_COLOR", caption_text_color);
                            intent.putExtra("CAPTION_BG_COLOR", caption_bg_color);
                            LogUtil.d("VOD added : " + vodList.size());
                            LogUtil.d("" + vodList.toArray(new String[vodList.size()]));
                        }
                        LogUtil.d("Set schedule " + name);

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

                    mySQLConnection.close();

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

        });
        cmsConnectThread.start();


    }

    private static String getTargetString(Connection mySQLConnection, String target_type, String id) {
        String result = "";

        try {
            Statement stmt1 = mySQLConnection.createStatement();
            if (target_type.equals("GROUP")) {
                String query1 = "select B.name from tb_stb_schedule_group A, tb_stb_group B where A.group_id = B.id and schedule_id = " + id;
                int count = 0;
                ResultSet resultSet1 = stmt1.executeQuery(query1);
                while (resultSet1.next()) {
                    String groupName = resultSet1.getString("name");
                    if (count == 1)
                        result += ", ";
                    else if (count == 2) {
                        result += " " + mContext.getResources().getString(R.string.str_target_more);
                        break;
                    }
                    result += groupName;
                    count++;
                }
            } else {
                result = mContext.getResources().getString(R.string.str_target_all);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 오늘의 방송일정 가져오기
     * @return
     */
    public static ArrayList<ScheduleItem> getTodaySchedule() {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return null;

        ArrayList<ScheduleItem> result = new ArrayList<>();
        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection mySQLConnection = connectSednDB();;
                    if(mySQLConnection == null) return;

                    Statement stmt = mySQLConnection.createStatement();

//                    String query = "select id, name, concat(substring(time(start), 1, 5), ' ~ ', substring(time(end), 1, 5)) duration, target_type, image_path, desc_text from tb_stb_schedule"
//                            + " where !((date(start) < date(now()) and date(end) < date(now())) or (date(start) > date(now()) and date(end) > date(now()))) order by start";

                    // 2017.4.19 ghlee. 현재 STB의 그룹이 방송대상에 포함되는 스케쥴을 가져온다.
                    String query = "select id, name, concat(substring(time(start), 1, 5), ' ~ ', substring(time(end), 1, 5)) duration, target_type, image_path, desc_text" +
                            " from tb_stb_schedule aa" +
                            " left outer join tb_stb_schedule_group bb on aa.id=bb.SCHEDULE_ID" +
                            " where start > now() and dayofyear(end)=dayofyear(now()) and (target_type='ALL' or bb.GROUP_ID=" + app.myGroupID + ")" +
                            " group by id" +
                            " order by start;";

                    ResultSet resultSet = stmt.executeQuery(query);

                    while(resultSet.next()) {

                        String id = resultSet.getString("id");
                        String name = resultSet.getString("name");
                        String duration = resultSet.getString("duration");
                        String target_type = resultSet.getString("target_type");
                        String image_path = resultSet.getString("image_path");
                        String desc_text = resultSet.getString("desc_text");

                        String target = getTargetString(mySQLConnection, target_type, id);

                        LogUtil.d("today schedule " + name + ", " + target_type + ", " + image_path + ", " + desc_text + ", ");

                        ScheduleItem item = new ScheduleItem();
                        item.name = name;
                        item.image_url = convertAsHTTPPath(image_path);
                        item.durationStr = duration;
                        item.desc = desc_text;
                        item.target = target;

                        result.add(item);
                    }
                    mySQLConnection.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
        cmsConnectThread.start();

        try {
            cmsConnectThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static ArrayList<ChannelItem> getChannelList() {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return null;

        ArrayList<ChannelItem> result = new ArrayList<>();
        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection mySQLConnection = connectSednDB();
                    if(mySQLConnection == null) return;
                    Statement stmt = mySQLConnection.createStatement();
                    String query = "select ch_idx, ch_nm from tb_channel_info";
                    ResultSet resultSet = stmt.executeQuery(query);

                    while(resultSet.next()) {
                        ChannelItem item = new ChannelItem();

                        String ch_idx = resultSet.getString("ch_idx");
                        String ch_nm = resultSet.getString("ch_nm");
                        item.channelIndex = ch_idx;
                        item.channelName = ch_nm;
                        result.add(item);
                    }
                    mySQLConnection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cmsConnectThread.start();

        try {
            cmsConnectThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static int converToIntColor(String color) {
        return (int)(Long.parseLong("ff" + color.substring(1), 16)); // #ffffff 의 형태여야 함
    }

    public static int converToIntBGColor(String color) {
        if(color == null || color.isEmpty())
            return 0;

        return (int)(Long.parseLong("99" + color.substring(1), 16)); // #ffffff 의 형태여야 함
    }

    public static ArrayList<ScheduleItem> getLiveScheduleList(String ch_idx) {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return null;

        ArrayList<ScheduleItem> result = new ArrayList<>();
        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection mySQLConnection = connectSednDB();
                    if(mySQLConnection == null) return;
                    Statement stmt = mySQLConnection.createStatement();
                    String query = "select id, name, date(start) play_date, TIMEDIFF(end, start) play_time, concat(substring(time(start), 1, 5), ' ~ ', substring(time(end), 1, 5)) duration, hour(start) * 60 + minute(start) start, hour(end) * 60 + minute(end) end, target_type, image_path, desc_text, color from tb_stb_schedule"
                            + " where source_type = 'LIVE' and live_ch_idx = " + ch_idx + " and !((date(start) < date(now()) and date(end) < date(now())) or (date(start) > date(now()) and date(end) > date(now()))) order by start";
                    ResultSet resultSet = stmt.executeQuery(query);

                    while(resultSet.next()) {
                        ScheduleItem item = new ScheduleItem();

                        String id = resultSet.getString("id");
                        String name = resultSet.getString("name");
                        String duration = resultSet.getString("duration");
                        int start = resultSet.getInt("start");
                        int end = resultSet.getInt("end");
                        String target_type = resultSet.getString("target_type");
                        String image_path = resultSet.getString("image_path");
                        String desc_text = resultSet.getString("desc_text");
                        String color = resultSet.getString("color");
                        String playDate = resultSet.getString("play_date");
                        String playTime = resultSet.getString("play_time");

                        item.name = name;
                        item.durationStr = duration;
                        item.start_time = start;
                        item.end_time = end;
                        item.image_url = convertAsHTTPPath(image_path);
                        item.desc = desc_text;
                        item.color_code = converToIntColor(color);
                        item.target = getTargetString(mySQLConnection, target_type, id);
                        item.dateStr = playDate;
                        item.playTimeStr = playTime;

                        result.add(item);
                    }
                    mySQLConnection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        cmsConnectThread.start();

        try {
            cmsConnectThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }


    /**
     * 오늘의 VOD방송 스케쥴 가져오기 - 파일 미리 다운로드 기능을 위해서 필요
     * 현재로부터 24시간 이내의 VOD스케쥴을 가져온다.
     * 2017.4.24 ghlee
     */
    public static void getVodSchedule_24Hour() {
        if(app.gSednServer == null || app.gSednServer.isEmpty())
            return;

        Thread cmsConnectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection mySQLConnection = connectSednDB();
                    if(mySQLConnection == null) return;
                    LogUtil.d("getSchedule connection " + mySQLConnection);

                    Statement stmt = mySQLConnection.createStatement();

                    // 방송 스케줄 가져오기
                    //String query = "select id, name, start, end, source_type, caption from tb_stb_schedule where target_type = 'ALL' and start > now() order by start";
                    String query = "select id, name, start, end, source_type, caption, caption_size, caption_speed, caption_text_color, caption_bg_color from tb_stb_schedule A left join tb_stb_schedule_group B on A.ID = B.SCHEDULE_ID"
                            + " where source_type='VOD' and (target_type = 'ALL' or (target_type = 'GROUP' and B.GROUP_ID = " + app.myGroupID + ")) and end > now() and start < DATE_ADD(now(), INTERVAL 24 HOUR) group by id order by start";
                    ResultSet resultSet = stmt.executeQuery(query);
                    LogUtil.d("getSchedule query - " + query);
                    app.todaySchedule.clear();
                    Date now = new Date();

                    while(resultSet.next()) {
                        String id = resultSet.getString("id");
                        Date start = Utils.sdf_YYYYMMDDHHMMSS.parse(resultSet.getString("start"));
                        String sourceType = resultSet.getString("source_type");

                        Statement stmt1 = mySQLConnection.createStatement();
                        Intent intent = new Intent((MainActivity)mContext, PlayerActivity.class);

                        if(sourceType.equals("VOD")) {
                            String query1 = "select a.vod_id, file_path, b.vod_play_time from tb_stb_schedule_vod a, tb_vod_data b, tb_attach_file c where a.vod_id = b.VOD_IDX and b.VOD_IDX = c.DATA_IDX and c.GUBUN='M' and c.trans = '00' and a.schedule_id="+id+" order by play_order";
                            ResultSet resultSet1 = stmt1.executeQuery(query1);
                            ArrayList<String> vodIDList = new ArrayList<>();
                            ArrayList<String> vodList = new ArrayList<>();

                            while(resultSet1.next()) {
                                String vodID = resultSet1.getString("vod_id");
                                String filePath = resultSet1.getString("file_path");
                                filePath = convertAsStreamingPath(filePath);
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
                    mySQLConnection.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
        cmsConnectThread.start();


    }
}
