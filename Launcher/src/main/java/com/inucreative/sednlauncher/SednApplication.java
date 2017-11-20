package com.inucreative.sednlauncher;

import android.content.Context;
import android.content.Intent;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.inucreative.sednlauncher.Activity.MainActivity;
import com.inucreative.sednlauncher.Client.RetrofitClient;
import com.inucreative.sednlauncher.DataType.STBLocation;
import com.inucreative.sednlauncher.DataType.VODItem;
import com.inucreative.sednlauncher.HdmiCec.TvControl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Jskim on 2016-07-20.
 */
public class SednApplication extends MultiDexApplication {
    public Context mCurrentContext;

    public boolean initializedForNetwork; // 부팅 후 최초로 네트웍 연결시 초기화하기 위한 플래그

    private int playerVolume;
    public int getVolume() {
        return playerVolume;
    }
    public void setVolume(int vol) {
        playerVolume = vol;
    }

    public static final String gModelName = "SEDN ORIGIN v1.0";
    public String gFirmwareVersion;
    public String gFirmwareDate;
    public String gNewFirmwareVersion;
    public String gNewFirmwareDate;
    public String gNewFirmwarePath;
    public String gSednServer;      // IP
    public String gSednServerPort;
    public String gSednPushPort;
    public String gStreamingServer; // 현재 streaming도 http이므로 PORT는 SednServerPort를 공유한다.
    public String gDBIP;
    public String gDBPort = "3306";
    public String gDBName = "vcms";
    public String gDBUserID = "root";
    public String gDBPW = "mysql()dlsn";

//    public String gDBPort;
//    public String gDBName;
//    public String gDBUserID;
//    public String gDBPW;

    public String banner1URL = "";
    public String banner2URL = "";

    public int gStorageRetention;
    public static final int RETENTION_3_MONTH = 1;
    public static final int RETENTION_6_MONTH = 2;
    public static final int RETENTION_NONE = 3;

    public static final String SEDN_UPLOAD_ROOT = "/home/sedn"; // 서버상의 UPLOAD 폴더 root

    public static final String LOGO_IMAGE_FILE_NAME = "logo.img";
    public static final String BG_IMAGE_FILE_NAME = "background.img";
    public static final String BG_VIDEO_FILE_NAME = "background_video.mp4";

    public static final String  ACTION_SEDN_STOP_PLAYER = "com.inucreative.sednlauncher.STOP_PLAYER";

    public static final String BLUETOOTH_UUID = "3e6eb1e4-ba1b-4de8-802c-830bee9a1403";

    //public static final int VOD_DOWNLOAD_RANDOM_DELAY = 60* 60 * 1000;   // 자동 동기화 시간에 다운로드 시작 시점까지의 random delay 최대값 (msec)
    public static final int VOD_DOWNLOAD_RANDOM_DELAY = 5 * 1000;   // 자동 동기화 시간에 다운로드 시작 시점까지의 random delay 최대값 (msec)

    public String myID;
    public String myName;
    public String myGroupID;
    public String myGroupName;

    public String myIP;
    public String myMAC;
    private int mStatus;

    public HashMap<String, Intent> schedule = new HashMap<>();
    public HashMap<String, Intent> todaySchedule = new HashMap<>();

    public STBLocation mSTBLocation;   // 장비 geoIP로 검색한 위도/경도
    public MainActivity mMainActivity;
    public static String OPENWEATHER_APPID = "871eb127e42475595b7dce1d5540daa8";

    public static final int PLAYER_DONE = 1;
    public static final int PLAYER_ERROR = 2;

    public static final int STATUS_OFF = 1;
    public static final int STATUS_ON = 2;
    public static final int STATUS_VOD = 3;
    public static final int STATUS_BROADCAST = 4;

    public void setStatus(int status) {
        setStatus(status, false);
    }

    public void setStatus(int status, boolean updateTimestamp) {
        mStatus = status;
        //SednDBClient.updateSTBStatus(updateTimestamp);
        RetrofitClient.updateSTBStatus(updateTimestamp);
    }

    public int getStatus() {
        return mStatus;
    }

    // 동기화 시각에 VOD 자동 다운로드 기능
    public ArrayList<VODItem> downloadVODList = new ArrayList<>();
    public int downloadVODIndex;

    // HDMI-CEC를 이용한 TV On/Off Control 2017.03.16
    public static TvControl mTvControl = new TvControl();
    public static int mPlayerReadyTimeCount = 5; // 재생 시작전 카운트 표시

    public static boolean mUseSDCARD=false;
    public static String mSdcardPath = "/storage/sdcard1/Download";

    public static boolean mUseSataHdd=false;
    public static String mSataHddPath = "/storage/udisk0/Download";

    public static String SEDN_MANAGER_URL="http://manager.sedn.tv/php/";

    public static SednApplication app;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

        // SATA HDD Check
        File fileSata = new File(mSataHddPath);
        if(!fileSata.exists()) {
            fileSata.mkdirs();
        }

        if(fileSata.canWrite()) {
            mUseSataHdd = true;
        }
        else {

            // SDHC Check
            File file = new File(mSdcardPath);
            if (!file.exists()) {
                file.mkdirs();
            }

            if (file.canWrite()) {
                mUseSDCARD = true;
            }
        }

    }
}
