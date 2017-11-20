package com.inucreative.sednlauncher.Activity;

import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.Instrumentation;
import android.app.PackageInstallObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.StaticIpConfiguration;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.percent.PercentLayoutHelper;
import android.support.percent.PercentRelativeLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.inucreative.sednlauncher.Adapter.SednListAdapter;
import com.inucreative.sednlauncher.Client.Model.PersonalBroadcastResponse;
import com.inucreative.sednlauncher.Client.RetrofitClient;
import com.inucreative.sednlauncher.CustomView.ASEditText;
import com.inucreative.sednlauncher.CustomView.BGVideoView;
import com.inucreative.sednlauncher.CustomView.SednAutoCompleteTextView;
import com.inucreative.sednlauncher.CustomView.SednRadioGroup;
import com.inucreative.sednlauncher.DataType.ChannelItem;
import com.inucreative.sednlauncher.DataType.ListviewBaseItem;
import com.inucreative.sednlauncher.DataType.STBLocation;
import com.inucreative.sednlauncher.DataType.ScheduleItem;
import com.inucreative.sednlauncher.DataType.StbConfig;
import com.inucreative.sednlauncher.DataType.VODItem;
import com.inucreative.sednlauncher.HdmiCec.HdmiCecManager;
import com.inucreative.sednlauncher.R;
import com.inucreative.sednlauncher.SednApplication;
import com.inucreative.sednlauncher.Service.FileDownloadService;
import com.inucreative.sednlauncher.SettingsContentObserver;
import com.inucreative.sednlauncher.SystemUtil.OutputUiManager;
import com.inucreative.sednlauncher.Threads.ConnectivityListener;
import com.inucreative.sednlauncher.Threads.LocalDBManager;
import com.inucreative.sednlauncher.Threads.RemoteServer;
import com.inucreative.sednlauncher.Threads.ScheduleManager;
import com.inucreative.sednlauncher.Threads.SednServer;
import com.inucreative.sednlauncher.Util.LogUtil;
import com.inucreative.sednlauncher.Util.SednItemList;
import com.inucreative.sednlauncher.Util.Utils;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;


public class MainActivity extends AppCompatActivity {

    Handler handler = new Handler();
    //SednDBClient mSednDBClient;
    RetrofitClient mRetrofitClient;
    SednApplication mApp;
    ConnectivityListener mConnectivityListener;
    ConnectivityManager mConnectivityManager;
    public LocalDBManager mLocalDBManager;
    EthernetManager mEthernetManager;
    WifiManager mWifiManager;

    private final int REQUEST_CODE_PLAYER = 1234;

    RemoteServer mRemoteServer;

    // Remocon 관련
    public EditText focusedEditText;

    // Global View -----------------------------------
    BGVideoView vvBackground;
    boolean canPlayVideo;
    ImageView ivImageBackground;
    boolean mVideoBackground;
    ViewFlipper vfScreenFlipper;
    ImageView ivClientLogo;
    TextView tvClientLogo;

    ViewGroup vLayoutHome;
    PercentRelativeLayout vLayoutVOD;
    ViewGroup vLayoutLive;
    ViewGroup vLayoutMypage;
    ViewGroup vLayoutSearch;
    ViewGroup vLayoutSetup;

    TextView tvGroupName;
    TextView tvDeviceName;
    TextView tvBGDate;
    TextView tvBGTime;
    TextClock tcClock;
    ImageView ivWeather;
    TextView tvTemperature;

    TextView tvStatusColor;
    TextView tvStatusText;
    TextView tvStorageUsage;

    // Global Data -----------------------------------
    //STBLocation mSTBLocation;   // 장비 geoIP로 검색한 위도/경도

    // openweathermap에서 얻어온 날씨 이미지와 자체 이미지의 mapping table
    // 참조 https://openweathermap.org/weather-conditions
    public HashMap<String, Integer> mWeatherIconMap;
    int mWeatherImage;
    String mTemperature;

    // Global Animation
    Animation menuClickedAnim;
    Animation quickMenuSlideInAnim;
    Animation quickMenuSlideOutAnim;
    Animation slide_in;     // 뷰플리퍼 애니메이션
    Animation slide_out;    // 뷰플리퍼 애니메이션
    Animation slide_in_r;     // 뷰플리퍼 애니메이션
    Animation slide_out_r;    // 뷰플리퍼 애니메이션

    // Quick Menu ------------------------------------
    public ViewGroup vLayoutQuickmenu;
    ImageView ivMenuList[];
    public boolean quickMenuFocused = true; // 처음실행시에는 quick menu에 포커스
    public TextView tvMenuInfoVOD;
    public TextView tvMenuInfoChannel;
    public TextView tvMenuInfoBookmark;
    public TextView tvMenuInfoDownload;
    public TextView tvMenuInfoIP;
    public TextView tvMenuInfoResolution;
    private static final int MODE_HOME = 0;
    private static final int MODE_VOD = 1;
    private static final int MODE_LIVE = 2;
    private static final int MODE_MYPAGE = 3;
    private static final int MODE_SEARCH = 4;
    private static final int MODE_SETUP = 5;
    int mLauncherMode;

    private static final int menuIDs[] = {
            R.id.ivMenuHome,
            R.id.ivMenuVOD,
            R.id.ivMenuLive,
            R.id.ivMenuMypage,
            R.id.ivMenuSearch,
            R.id.ivMenuSetup
    };

    // 공통 View ------------------------------------
    private static final int previewIDs[] = {
            R.id.ivPreview_play,
            R.id.ivPreview_bookmark,
            R.id.ivPreview_download,
            R.id.ivPreview_info
    };

    // HOME -------------------------------------------
    View homeTodaySchedule;
    View homeSearch;
    View homeRecentVOD;
    View homeMostVOD;
    ImageView homeBanner1;
    ImageView homeBanner2;

    // -------- 오늘의방송일정
    ViewFlipper vfTodaySchedule;
    LinearLayout todayScheduleRolling;
    int todayScheduleNum;
    int todaySchedulePos;
    int todayScheduleRollingNum;
    int todayScheduleRollingPos;

    private static final int TODAY_SCHEDULE_NEXT = 1;
    private static final int TODAY_SCHEDULE_PREV = -1;
    Handler todayScheduleHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            todayScheduleMove(TODAY_SCHEDULE_NEXT);
            sendEmptyMessageDelayed(0, 10000);
        }
    };
    // -------- 검색
    SednAutoCompleteTextView homeSearchBox;
    // -------- 최근등록 VOD
    HorizontalScrollView scrollRecentVOD;
    LinearLayout layoutRecentVOD;
    View recentFocusBox;
    HorizontalScrollView recentVODPosition;
    ArrayList<ListviewBaseItem> mRecentVOD;
    // 가장많이시청한 VOD
    LinearLayout layoutMostVOD;
    ArrayList<ListviewBaseItem> mMostVOD;
    private static final int homeMostVODRankDrawables[] = {
            R.drawable.mostvod_crown,
            R.drawable.mostvod_2,
            R.drawable.mostvod_3,
            R.drawable.mostvod_4,
            R.drawable.mostvod_5
    };
    View.OnKeyListener mostVODKeyListener;
    View.OnClickListener mostVODClickListener;

    int mHomeState;
    private static final int HOME_STATE_Box = 1;
    private static final int HOME_STATE_Box_inside = 2;

    int mHomeSeletedBox;
    private static final int HOME_BOX_TODAY_SCHEDULE = 1;
    private static final int HOME_BOX_SEARCH = 2;
    private static final int HOME_BOX_RECENT_VOD = 3;
    private static final int HOME_BOX_MOST_VOD = 4;

    int mSelectedRecentVOD; // 0 base

    // VOD -------------------------------------------
    ListView[] lvMenuCategory;
    ListView lvVODItems;
    SednListAdapter[] mMenuSednListAdapter;
    float[] leftPos;
    SednListAdapter mVODDataAdapter;
    View vVODContentPreview;
    ImageView[] vVODContentPreviewButton;
    ImageView ivVODThumbnail;
    TextView tvVODContentDate;
    TextView tvVODContentTime;
    TextView tvVODContentHit;
    TextView tvVODContentCate;
    TextView tvVODContentTitle;
    View vVODContentInfo;
    float mtopMarginPercentForVODPreview;

    int mVODState;
    int mVODMenuDepth;
    private static final int VOD_STATE_MenuCategory = 1;
    private static final int VOD_STATE_VODList = 2;
    private static final int VOD_STATE_Preview = 3;
    //private static final int HOME_STATE_Info = 4;


    // LIVE ---------------------------------------------
    TextView tvLiveTodayStr;
    RelativeLayout layoutTimeLine;

    ScrollView svLiveChannel;
    LinearLayout layoutLiveChannel;
    ScrollView svLiveBroadcast;
    HorizontalScrollView hsvLiveBroadcast;
    LinearLayout layoutLiveBroadcast;
    ImageView ivCurTimeBar;

    HorizontalScrollView svLiveBottomList;
    LinearLayout layoutLiveBottomList;

    ViewGroup layoutSchedulePreview;
    TextView tvLiveScheduleName;
    ImageView ivLiveScheduleThumbnail;
    TextView tvLiveSchedulePreviewDate;
    TextView tvLiveSchedulePreviewTime;
    TextView tvLiveScheduleDuration;
    TextView tvLiveScheduleGroup;
    TextView tvLiveScheduleDesc;

    int mLiveState;
    private static final int LIVE_STATE_Channel = 1;
    private static final int LIVE_STATE_Schedule = 2;
    private static final int LIVE_STATE_Preview = 3;

    View.OnKeyListener liveChannelKeyListener;
    View.OnClickListener liveChannelClickListener;
    View.OnKeyListener liveScheduleKeyListener;
    View.OnClickListener liveScheduleClickListener;
    View.OnKeyListener liveBottomKeyListener;           // 개인방송
    View.OnClickListener liveBottomClickListener;       // 개인방송

    int mLiveChannelNum;
    ArrayList<PersonalBroadcastResponse> mPersonalBR = new ArrayList<>();

    // MY PAGE ---------------------------------------------
    View vBookmarkTab;
    View vDownloadTab;
    ListView lvMypageItems;
    SednListAdapter mMypageItemAdapter;
    View vMypageContentPreview;
    ImageView[] vMypageContentPreviewButton;
    ImageView ivMypageThumbnail;
    TextView tvMypageContentDate;
    TextView tvMypageContentTime;
    TextView tvMypageContentHit;
    TextView tvMypageContentCate;
    TextView tvMypageContentTitle;
    View vMypageContentInfo;
    View vNoBookmark;
    View vNoDownload;

    int mMypageMode;
    private static final int MYPAGE_MODE_Bookmark = 1;
    private static final int MYPAGE_MODE_Download = 2;

    int mMypageState;
    private static final int MYPAGE_STATE_Submenu = 1;
    private static final int MYPAGE_STATE_VODList = 2;
    private static final int MYPAGE_STATE_Preview = 3;

    // SEARCH -------------------------------------------
    EditText etSearchText;
    TextView tvGoSearch;
    ListView lvSearchResult;
    SednListAdapter mSearchResultAdapter;
    View vSearchContentPreview;
    ImageView[] vSearchContentPreviewButton;
    ImageView ivSearchContentThumbnail;
    TextView tvSearchContentDate;
    TextView tvSearchContentTime;
    TextView tvSearchContentHit;
    TextView tvSearchContentCate;
    TextView tvSearchContentTitle;
    View vSearchContentInfo;

    int mSearchState;
    private static final int SEARCH_STATE_InputText = 1;
    private static final int SEARCH_STATE_VODList = 2;
    private static final int SEARCH_STATE_Preview = 3;
    //------------------------------------------------

    // SETUP -------------------------------------------
    private static final int setupItemIDs[] = {
            R.id.tvSetupInfomation,
            R.id.tvSetupNetwork,
            R.id.tvSetupDisplay,
            R.id.tvSetupSystem
    };
    TextView[] setupItemList;

    View layoutSetupInformation;
    int mSelectedSetupItemIdx;
    private static final int SETUP_INFORMATION = 0;
    private static final int SETUP_NETWORK = 1;
    private static final int SETUP_DISPLAY = 2;
    private static final int SETUP_SYSTEM = 3;

    int mSetupState;
    private static final int SETUP_STATE_Menu = 1;
    private static final int SETUP_STATE_LeftMenu = 2;
    private static final int SETUP_STATE_Detail = 3;

    boolean isSetupChanged;

    // Left Menu
    View setupLeftMenu; // 메뉴 전체 패널
    View setupArrowUp;
    View setupArrowDown;
    View setupLeftItemUpper;
    TextView setupLeftItemUpperText;
    ImageView setupLeftItemUpperImage;
    View setupLeftItemLower;
    TextView setupLeftItemLowerText;
    ImageView setupLeftItemLowerImage;

    interface OnRedrawCallback {
        void onRedraw();
    }
    class SetupLeftMenuInfo {
        int selected;
        int firstVisible;
        int menuStr[];
        int menuImg[];
        int length;
        int menuViewID[];
        View menuViewDrawable[];
        public OnRedrawCallback redrawCallback[];
        public View firstFocusing[];

        public SetupLeftMenuInfo(int[] str, int[] img, int view[]) {
            selected = 0;
            firstVisible = 0;
            menuStr = str;
            menuImg = img;
            menuViewID = view;
            length = str.length;

            menuViewDrawable = new View[length];
            redrawCallback = new OnRedrawCallback[length];
            firstFocusing =  new View[length];
            for(int i=0; i<length; i++) {
                menuViewDrawable[i] = findViewById(view[i]);
                redrawCallback[i] = null;
                firstFocusing[i] = null;
                LogUtil.d("add new drawable " + menuViewDrawable[i]);
            }

        }

        public void hideView() {
            menuViewDrawable[selected].setVisibility(View.GONE);
        }

        public void showView() {
            menuViewDrawable[selected].setVisibility(View.VISIBLE);
            if(redrawCallback[selected] != null)
                redrawCallback[selected].onRedraw();
        }
    }
    SetupLeftMenuInfo mSetupNetworkInfo;
    SetupLeftMenuInfo mSetupDisplayInfo;
    SetupLeftMenuInfo mSetupSystemInfo;
    SetupLeftMenuInfo[] setupLeftMenuInfos;
    SetupLeftMenuInfo curLeftMenu;

    // Information
    TextView tvSetupInfoModelName;
    TextView tvSetupInfoServiceURL;
    TextView tvSetupInfoIPAddress;
    TextView tvSetupInfoMACAddress;
    TextView tvSetupInfoFWVersion;

    // Network
    private static final int setupNetworkMenuStr[] = {
            R.string.str_setup_network_ethernet,
            R.string.str_setup_network_wifi
    };
    private static final int setupNetworkMenuImg[] = {
            R.drawable.selector_setup_network_ethernet,
            R.drawable.selector_setup_network_wifi
    };
    private static final int setupNetworkMenuView[] = {
            R.id.layoutSetupNetworkEthernet,
            R.id.layoutSetupNetworkWiFi
    };
    private static final int setupNetworkWiFiLevelImg[] = {
            R.drawable.icon_wireless_wifi_04,
            R.drawable.icon_wireless_wifi_03,
            R.drawable.icon_wireless_wifi_02,
            R.drawable.icon_wireless_wifi_01
    };
    TextView tvSetupNetworkEthernetStatus;
    TextView tvSetupNetworkEthernetIPText;
    TextView tvSetupNetworkEthernetIPValue;
    TextView tvSetupNetworkEthernetAPText;
    TextView tvSetupNetworkEthernetAPValue;

    TextView tvSetupNetworkWiFiStatus;
    TextView tvSetupNetworkWiFiIPText;
    TextView tvSetupNetworkWiFiIPValue;
    TextView tvSetupNetworkWiFiAPText;
    TextView tvSetupNetworkWiFiAPValue;

    RadioButton rbSetupEthernetDHCP;
    RadioButton rbSetupEthernetStatic;
    ASEditText etSetupNetworkInfoIPInput;
    ASEditText etSetupNetworkInfoSubnetInput;
    ASEditText etSetupNetworkInfoGatewayInput;
    ASEditText etSetupNetworkInfoDNS1Input;
    ASEditText etSetupNetworkInfoDNS2Input;

    RadioButton rbSetupWifiOn;
    RadioButton rbSetupWifiOff;
    ImageView selectedAPCheck;
    View selectedAP;
    TextView selectedAPName;
    ImageView selectedAPSecure;
    ImageView selectedAPSignal;
    View wifiDivider;
    View wifiAP[];
    TextView wifiAPName[];
    ImageView wifiAPSecure[];
    ImageView wifiAPSignal[];
    ImageView wifiAPListUpArrow;
    ImageView wifiAPListDownArrow;
    int firstVisibleAP;
    boolean connectionResultWaiting = false;

    TextView wifiPWAPName;
    TextView wifiPWGuide;
    ASEditText wifiPWInput;
    TextView wifiConnect;
    CheckBox wifiShowPW;
    TextView wifiShowPWDesc;

    private static final int wifiAPListIDs[] = {
            R.id.setupWiFiAP1,
            R.id.setupWiFiAP2,
            R.id.setupWiFiAP3,
            R.id.setupWiFiAP4
    };

    // HDMI-CEC
    public HdmiCecManager mHdmiCecManager;

    SettingsContentObserver mSettingsContentObserver;


    List<ScanResult> curAPList = null;
    boolean isWifiPwEntering = false;
    private static final int MIN_WIFI_SCAN_INTERVAL_MSEC = 9000;    //9 sec
    long last_scan_time_msec = 0;
    public BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                //LogUtil.d("SCAN_RESULTS_AVAILABLE_ACTION - original " + mLauncherMode + ", " + mSelectedSetupItemIdx + ", " + mSetupNetworkInfo.selected);
                if(mLauncherMode == MODE_SETUP && mSelectedSetupItemIdx == SETUP_NETWORK && mSetupNetworkInfo.selected == 1) {
                    // WiFi Scan will never stop. Just ignore when not necessary
                    //LogUtil.d("SCAN_RESULTS_AVAILABLE_ACTION");
                    long cur_time = System.currentTimeMillis();
                    if(cur_time - last_scan_time_msec > MIN_WIFI_SCAN_INTERVAL_MSEC) {
                        LogUtil.d("Update AP list");
                        last_scan_time_msec = cur_time;
                        curAPList = mWifiManager.getScanResults();
                        if (curAPList != null) {
                            // AP 목록을 업데이트
                            drawAPList(0);
                            //wifiAP[0].requestFocus();
                            firstVisibleAP = 0;
                        }
                    }
                }
            }
            else if(intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifiState = mWifiManager.getWifiState();
                LogUtil.d("Wifi State changed " + wifiState);

//                if(wifiState == WifiManager.WIFI_AP_STATE_ENABLING)
//                    drawNetworkStatus(wifiState);
                if(wifiState == WifiManager.WIFI_STATE_ENABLED || wifiState == WifiManager.WIFI_STATE_DISABLED)
                    redrawNetworkSetup();
            } else if(intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                if(mWifiManager.getConnectionInfo().getSupplicantState() == SupplicantState.DISCONNECTED) {
                    if (connectionResultWaiting) {
                        LogUtil.d("SUPPLICANT_STATE_ changed " + mWifiManager.getConnectionInfo().getSSID() + ", " + mWifiManager.getConnectionInfo().getSupplicantState());
                        showToast(R.string.msg_wifi_connection_failure, Toast.LENGTH_SHORT);
                        connectionResultWaiting = false;
                    }
                }
            }
        }
    };

    // Display
    private static final int setupDisplayMenuStr[] = {
            R.string.str_setup_display_resolution,
            R.string.str_setup_display_cec
    };
    private static final int setupDisplayMenuImg[] = {
            R.drawable.selector_setup_display_resolution,
            R.drawable.selector_setup_display_cec
    };
    private static final int setupDisplayMenuView[] = {
            R.id.layoutSetupDisplayResolution,
            R.id.layoutSetupDisplayCEC
    };

    OutputUiManager mOutputUiManager;

    SednRadioGroup rgResolution;
    SednRadioGroup rgCECOnOff;
    private static final String DISPLAY_RESOLUTION[] = {"480i60hz", "1080i60hz", "2160p60hz420", "480p60hz", "720p60hz", "1080p60hz"};
    private static final String RESOLUTION_TEXT[] = {"480i", "1080i", "4K", "480p", "720p", "1080p"};
    // System
    private static final int setupSystemMenuStr[] = {
            R.string.str_setup_system_server_url,
            R.string.str_setup_system_reboot,
            R.string.str_setup_system_fw_update,
            R.string.str_setup_system_storage_reset
    };
    private static final int setupSystemMenuImg[] = {
            R.drawable.selector_setup_system_server_url,
            R.drawable.selector_setup_system_reboot,
            R.drawable.selector_setup_system_fw_update,
            R.drawable.selector_setup_system_storage_reset
    };
    private static final int setupSystemMenuView[] = {
            R.id.layoutSetupSystemServerURL,
            R.id.layoutSetupSystemReboot,
            R.id.layoutSetupSystemFWUpdate,
            R.id.layoutSetupSystemStorageReset
    };

    EditText etSetupServiceURL;
    EditText etSetupServicePort;
    EditText etSetupPushPort;

    TextView tvFirmwareVersion;
    TextView tvFirmwareDate;
    TextView tvFirmwareNoUpdate;
    TextView tvFirmwareUpdate1;
    TextView tvFirmwareUpdate2;
    TextView tvFirmwareUpdate3;
    TextView tvFirmwareNewVersion;
    TextView setupSystemFirmwareConfirm;
    TextView setupSystemFirmwareCancel;

    private static final int setupStorageRetentionId[] = {
            R.id.rbFileRetention3Month,
            R.id.rbFileRetention6Month,
            R.id.rbFileRetentionNone
    };

    TextView tvSetupStorageTotal;
    TextView tvSetupStorageUsed;
    RadioGroup rgSetupStorageRetention;

    //------------------------------------------------

    // MySQL connection
    Connection mySQLConnection;

    // File Downloader
    public DownloadManager mDownloadManager;
    private BroadcastReceiver mDownloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                LogUtil.d("ACTION_DOWNLOAD_COMPLETE called");
                if(mApp.mCurrentContext instanceof MainActivity) {
                    long download_id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    if(download_id != 0) {
                        Cursor c = mDownloadManager.query(new DownloadManager.Query().setFilterById(download_id));
                        c.moveToFirst();
                        int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                        if(status == DownloadManager.STATUS_SUCCESSFUL) {
                            String title = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE));
                            String file_path = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));

                            LogUtil.d("title " + title);
//                            if(title.startsWith("VOD")) {
//
//                                // 파일명 변경 : *.mp4.temp -> *.mp4
//                                String vodId = title.substring( ("VOD").length());
//                                renameVodFileName(file_path, vodId);
//
//                                showToast(getResources().getString(R.string.msg_download_end));
//                                String id = title.substring(3);
//                                SednListAdapter adapter = null;
//                                switch (mLauncherMode) {
//                                    case MODE_VOD:
//                                        adapter = mVODDataAdapter;
//                                        break;
//                                    case MODE_MYPAGE:
//                                        adapter = mMypageItemAdapter;
//                                        if(mMypageState == MYPAGE_STATE_VODList || mMypageState == MYPAGE_STATE_Preview) {
//                                            LogUtil.d(mMypageItemAdapter.getSelectedItem().getID() + " = " + id);
//                                            if(mMypageItemAdapter.getSelectedItem().getID().equals(id)) {
//                                                vMypageContentPreviewButton[2].setImageResource(R.drawable.icon_download_trash);
//                                            }
//                                        }
//                                        break;
//                                    case MODE_SEARCH:
//                                        adapter = mSearchResultAdapter;
//                                        break;
//                                }
//                                if (adapter != null)
//                                    adapter.notifyDataSetChanged();
//                            } else
                            if(title.startsWith("LOGO")) {
                                setLogoImage();
                            } else if(title.startsWith("BG_IMG")) {
                                setBackgroundImage();
                            } else if(title.startsWith("BG_VIDEO")) {
                                setBackgroundVideo();
                            } else if(title.startsWith("Firmware")) {
                                installNewPackage(file_path);
                            }
//                            else if(title.startsWith("BATCH_DOWN")) {
//                                LogUtil.d("BATCH Down complete - " + file_path);
//
//                                // 파일명 변경 : *.mp4.temp -> *.mp4
//                                String vodId = title.substring( ("BATCH_DOWN").length());
//                                renameVodFileName(file_path, vodId);
//
//
//                                mApp.downloadVODIndex++;
//                                if(mApp.downloadVODIndex < mApp.downloadVODList.size()) {
//                                    requestDownload("BATCH_DOWN", mApp.downloadVODList.get(mApp.downloadVODIndex));
//                                } else {
//                                    showToast(R.string.msg_batch_down_complete);
//                                }
//                            }

                            // 다운로드 후 남은 용량 업데이트
                            updateSTBStatus();
                        } else {
                            showToast(getResources().getString(R.string.msg_download_failed));
                        }
                    }
                }
            }
        }
    };

    public void downloadConfFile(String path, String type, String fileName) {
        // 존재하면 지움
        File file = LocalDBManager.getConfigFolder();
        file = new File(file.getPath() + "/" + fileName);

        if(file.exists())
            file.delete();

        if(path == null || path.isEmpty()) return;

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(path));
        request.setTitle(type);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOCUMENTS, fileName);
        ensureFreeSpaceAndDownload(request, path);
        //mDownloadManager.enqueue(request);
    }

    public void setLogoImage() {
        File logoFile = LocalDBManager.getConfigFolder();
        logoFile = new File(logoFile.getPath() + "/" + SednApplication.LOGO_IMAGE_FILE_NAME);
        LogUtil.d("setLogoImage " + logoFile);
        if(logoFile.exists()) {
            tvClientLogo.setVisibility(View.GONE);
            ivClientLogo.setVisibility(View.VISIBLE);
            ivClientLogo.setImageURI(Uri.fromFile(logoFile));
        }
    }

    public void setLogoText() {
        String logo_text = Utils.getSTBLogoText(this);
        LogUtil.d("setLogotext " + logo_text);
        if(logo_text != null) {
            ivClientLogo.setVisibility(View.GONE);
            tvClientLogo.setVisibility(View.VISIBLE);
            tvClientLogo.setText(logo_text);
        }
    }

    public void setBackgroundImage() {
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        file = new File(file.getPath() + "/" + SednApplication.BG_IMAGE_FILE_NAME);

        ivImageBackground.setVisibility(View.VISIBLE);
        vvBackground.setVisibility(View.GONE);
        LogUtil.d("setBackgroundImage file : " + file.toString());
        if(file.exists()) {
            ivImageBackground.setImageResource(0);
            ivImageBackground.setImageURI(Uri.fromFile(file));
        } else {
            ivImageBackground.setImageResource(R.drawable.bg);
        }

        mVideoBackground = false;
    }

    public void playBGVideo() {
        // 부팅 후 비디오가 뜨기 전에 PlayerActivity가 뜨는 경우에 대한 방어 필요
        ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        vvBackground.start();
    }

    public void setBackgroundVideo() {
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        file = new File(file.getPath() + "/"+ SednApplication.BG_VIDEO_FILE_NAME);
        LogUtil.d("setBackgroundVideo file : " + file.toString());
        if(file.exists()) {
            ivImageBackground.setVisibility(View.INVISIBLE);
            vvBackground.setVisibility(View.VISIBLE);
            vvBackground.setVideoPath(file.getPath());
            vvBackground.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(true);
                    mp.setVolume(0.0f, 0.0f);
                }
            });
            mVideoBackground = true;
            if(canPlayVideo)
                playBGVideo();
        } else {
            ivImageBackground.setVisibility(View.VISIBLE);
            vvBackground.setVisibility(View.GONE);
            ivImageBackground.setImageResource(R.drawable.bg);
            mVideoBackground = false;
        }
    }

    public void restoreBackground() {
        if(Utils.getSTBBGImageYN(this).equals("Y")) {   // 배경화면
            setBackgroundImage();
        } else {
            setBackgroundVideo();
        }
    }

    public void initializeForNetework() {
        LogUtil.d("initializeForNetework-S");

        mApp.initializedForNetwork = true;

        // Sedn Manager Server로부터 기본 환경설정을 읽어온다.
        retrofit_getConfig(mApp.myMAC);


        LogUtil.d("initializeForNetework-E");
    }

    /**
     * 통합 Manager 서버로부터 환경설정값을 읽어온 이후에 초기화
     */
    private void initSetTopBox() {
        // 장비 정보 셋업
        mApp.myIP = Utils.getIPAddress(true);
        mApp.setStatus(mApp.STATUS_ON, true);

        LogUtil.d("myIP " + mApp.myIP + ", " + Utils.getMACAddress("wlan0") + ", " + Utils.getMACAddress("eth0"));

        focusedEditText = null;

        if(mApp.gSednServer != null && !mApp.gSednServer.isEmpty()) {
            // STB 서버 기동
            SednServer sednServer = new SednServer(this);

            // STB 정보를 가져오고 없으면 자동으로 등록한다.
            //SednDBClient.getSTBInfo();
            RetrofitClient.getSTBInfo();

            // UI가 만들어지고 난 후 데이터를 가져온다.
            //SednDBClient.getSTBData();   // Menu, VOD, Channel 등등...
            RetrofitClient.getSTBData();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //SednDBClient.getSchedule();
                    RetrofitClient.getSchedule();
                }
            }, 6000); // org 3000

            // STB 설정 정보 가져옴
            /*SednDBClient.updateSTBLogo();
            SednDBClient.updateSTBBG();
            SednDBClient.updateStreamingURL();*/
            RetrofitClient.getConfiguration();

        } else {
            showToast(getResources().getString(R.string.msg_no_server_url));
            restoreBackground();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LogUtil.d("MainActivity onCreate");

        // 네비게이션바 숨기기
        Utils.hideSystemUI(getWindow().getDecorView());

        // Mysql DB 연동
        try {
            //Class.forName("com.mysql.jdbc.Driver");
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        mApp = (SednApplication)getApplication();
        mApp.initializedForNetwork = false;
        mApp.mMainActivity = this;

        // 네트웍 연결과 무관한 것들 초기화
        mConnectivityListener = new ConnectivityListener(this);
        mConnectivityManager = ((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE));
        mEthernetManager = (EthernetManager)getSystemService(Context.ETHERNET_SERVICE);
        mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        mLocalDBManager = new LocalDBManager(this);
        //mSednDBClient = new SednDBClient(this); // 변수는 사용되지 않는다.
        mRetrofitClient = new RetrofitClient(this); // 변수는 사용되지 않는다.
        mDownloadManager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        mOutputUiManager = new OutputUiManager(this);

        // 한국 타임존으로 강제 변경
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.setTimeZone("GMT+9:00");

        mApp.gDBIP = Utils.getServiceURL(this);
        mApp.gSednServer = Utils.getServiceURL(this);
        mApp.gSednServerPort = Utils.getServicePort(this);
        mApp.gSednPushPort = Utils.getPushPort(this);
        mApp.myMAC = Utils.getMACAddress("eth0");

        try{
            PackageManager pm = getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            mApp.gFirmwareVersion = packageInfo.versionName;
            mApp.gFirmwareDate = Utils.getApkBuildDate(this);

        } catch(Exception e) {
            e.printStackTrace();
        }

        // 변수 초기화
        canPlayVideo = false;       // OnResume에서 surface가 준비될 때 true로 한다.
        mVideoBackground = false;
        mLauncherMode = MODE_HOME;
        menuClickedAnim = AnimationUtils.loadAnimation(this, R.anim.quick_icon_clicked);
        quickMenuSlideInAnim = AnimationUtils.loadAnimation(this, R.anim.quickmenu_slide_in);
        quickMenuSlideOutAnim = AnimationUtils.loadAnimation(this, R.anim.quickmenu_slide_out);
        slide_in = AnimationUtils.loadAnimation(this, R.anim.menu_slide_in);
        slide_out = AnimationUtils.loadAnimation(this, R.anim.menu_slide_out);
        slide_in_r = AnimationUtils.loadAnimation(this, R.anim.menu_slide_in_r);
        slide_out_r = AnimationUtils.loadAnimation(this, R.anim.menu_slide_out_r);
        vfScreenFlipper = (ViewFlipper) findViewById(R.id.vfScreenFlipper);

        ScheduleManager scheduleManager = new ScheduleManager(this);

        // 파일 보관 기간
        mApp.gStorageRetention = Utils.getRetentionPeriod(this);
        if(mApp.gStorageRetention < SednApplication.RETENTION_3_MONTH)
            mApp.gStorageRetention = SednApplication.RETENTION_3_MONTH;

        // View 초기화
        initBackground();
        initMenu();

        initHome();
        initVOD();
        initLive();
        initMypage();
        initSearch();
        initSetup();

        // 리모컨 서버 기동
        mRemoteServer = new RemoteServer(this);

        // 네트웍 미연결시에도 가능한 만큼 보여준다.
        // STB 상태
        updateSTBStatus();
        // 메뉴 패널 정보
        updateMenuInfo();

        // 홈버튼 포커스
        Handler focusHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                LogUtil.d("Set focus to Home icon");
                ivMenuList[MODE_HOME].requestFocus();
                ivMenuList[MODE_HOME].requestFocusFromTouch();
            }
        };
        focusHandler.sendEmptyMessageDelayed(0, 1000);

        registSdcardMountBR();

        registVolumeChangedListener();
    }

    @Override
    protected void onResume() {
        super.onResume();

        LogUtil.d("MainActivity onResume()");

        mApp.mCurrentContext = this;

        canPlayVideo = true;
        if (mVideoBackground) {
            LogUtil.d("onResume background video start");
            playBGVideo();
        }

        mConnectivityListener.start();

        IntentFilter downloadCompleteFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(mDownloadCompleteReceiver, downloadCompleteFilter);

        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        wifiFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(mWifiReceiver, wifiFilter);

        // vod download br
        IntentFilter vodDownloadSuccessFilter = new IntentFilter(FileDownloadService.ACTION_DOWNLOAD_RESULT);
        registerReceiver(mVodDownloadSuccessReceiver, vodDownloadSuccessFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtil.d("MainActivity onPause");

        mConnectivityListener.stop();   // may never be called.
    }

    private void clearSelection(ListView lv) {
        lv.setSelection(0);
        for(int i=0; i < lv.getCount(); i++) {
            lv.setItemChecked(i, false);
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        LogUtil.d("top onKeyDown : " + keyCode + ", launcherMode : " + mLauncherMode + ", quickmenuFocused : " + quickMenuFocused);
        // 전원 키
        if(keyCode == KeyEvent.KEYCODE_STB_POWER) {
            try {
                Process p = Runtime.getRuntime().exec(new String[] {"su", "-c", "am start -n android/com.android.internal.app.ShutdownActivity"});
                p.waitFor();
                p.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 바로가기 키 처리
        if(keyCode == KeyEvent.KEYCODE_BUTTON_1) {
            quickMenuTo(MODE_HOME);
            return true;
        } else if(keyCode == KeyEvent.KEYCODE_BUTTON_2) {
            quickMenuTo(MODE_VOD);
            return true;
        } else if(keyCode == KeyEvent.KEYCODE_BUTTON_3) {
            quickMenuTo(MODE_LIVE);
            return true;
        } else if(keyCode == KeyEvent.KEYCODE_BUTTON_4) {
            quickMenuTo(MODE_MYPAGE);
            return true;
        } else if(keyCode == KeyEvent.KEYCODE_BUTTON_5) {
            quickMenuTo(MODE_SEARCH);
            return true;
        } else if(keyCode == KeyEvent.KEYCODE_BUTTON_6) {
            quickMenuTo(MODE_SETUP);
            return true;
        }

        if(quickMenuFocused) {
            if(keyCode == KeyEvent.KEYCODE_ESCAPE) {
                ivMenuList[MODE_HOME].requestFocus();
            }
        } else if (mLauncherMode == MODE_HOME) {
            if(keyCode == KeyEvent.KEYCODE_ESCAPE) {
                ivMenuList[MODE_HOME].requestFocus();
                quickMenuFocused = true;
                if(mHomeState == HOME_STATE_Box_inside && mHomeSeletedBox == HOME_BOX_TODAY_SCHEDULE) {
                    startTodayScheduleAutoScroll();
                }
                homeTodaySchedule.setSelected(false);
                homeSearch.setSelected(false);
                homeRecentVOD.setSelected(false);
                homeMostVOD.setSelected(false);
                recentFocusBox.setVisibility(View.INVISIBLE);
            } else if(keyCode == KeyEvent.KEYCODE_BACK) {
                if(mHomeState == HOME_STATE_Box) {
                    ivMenuList[MODE_HOME].requestFocus();
                    quickMenuFocused = true;
                } else {
                    switch (mHomeSeletedBox) {
                        case HOME_BOX_TODAY_SCHEDULE:
                            homeTodaySchedule.requestFocus();
                            homeTodaySchedule.setSelected(false);
                            startTodayScheduleAutoScroll();
                            break;
                        case HOME_BOX_SEARCH:
                            homeSearch.requestFocus();
                            homeSearch.setSelected(false);
                            break;
                        case HOME_BOX_RECENT_VOD:
                            homeRecentVOD.requestFocus();
                            homeRecentVOD.setSelected(false);
                            recentFocusBox.setVisibility(View.INVISIBLE);
                            break;
                        case HOME_BOX_MOST_VOD:
                            homeMostVOD.requestFocus();
                            homeMostVOD.setSelected(false);
                            break;
                    }
                    mHomeState = HOME_STATE_Box;
                }
            }
        } else if (mLauncherMode == MODE_VOD) {
            // back key 및 나가기 키 처리
            if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
                //if (mVODState == VOD_STATE_MenuCategory && mVODMenuDepth == 1) {
                ivMenuList[MODE_VOD].requestFocus();
                vVODContentInfo.setVisibility(View.INVISIBLE);
                vVODContentPreview.setVisibility(View.INVISIBLE);
                lvMenuCategory[1].setVisibility(View.INVISIBLE);
                lvVODItems.setVisibility(View.INVISIBLE);
                clearSelection(lvMenuCategory[0]);
                quickMenuFocused = true;
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                switch (mVODState) {
                    case VOD_STATE_MenuCategory:
                        if (mVODMenuDepth == 1) {
                            ivMenuList[MODE_VOD].requestFocus();
                            lvMenuCategory[1].setVisibility(View.INVISIBLE);
                            lvVODItems.setVisibility(View.INVISIBLE);
                            clearSelection(lvMenuCategory[0]);
                            quickMenuFocused = true;
                        } else if (mVODMenuDepth == 2) {
                            lvMenuCategory[0].requestFocus();
                            lvVODItems.setVisibility(View.INVISIBLE);
                            lvMenuCategory[1].setVisibility(View.INVISIBLE);
                            mVODMenuDepth = 1;
                        }
                        break;
                    case VOD_STATE_VODList:
                        lvMenuCategory[mVODMenuDepth - 1].requestFocus();
                        lvVODItems.setVisibility(View.INVISIBLE);
                        mVODState = VOD_STATE_MenuCategory;
                        break;
                    case VOD_STATE_Preview:
                        lvVODItems.requestFocus();
                        vVODContentInfo.setVisibility(View.INVISIBLE);
                        mVODState = VOD_STATE_VODList;
                        break;
                }
            }
        } else if (mLauncherMode == MODE_LIVE) {
            if(keyCode == KeyEvent.KEYCODE_ESCAPE) {
                layoutSchedulePreview.setVisibility(View.INVISIBLE);
                ivMenuList[MODE_LIVE].requestFocus();
                quickMenuFocused = true;
            } else if(keyCode == KeyEvent.KEYCODE_BACK) {
                switch(mLiveState) {
                    case LIVE_STATE_Channel:
                        ivMenuList[MODE_LIVE].requestFocus();
                        quickMenuFocused = true;
                        break;
                    case LIVE_STATE_Preview:
                        mLiveState = LIVE_STATE_Schedule;
                        layoutSchedulePreview.setVisibility(View.INVISIBLE);
                        break;
                }
            }
        } else if (mLauncherMode == MODE_MYPAGE) {
            if(keyCode == KeyEvent.KEYCODE_ESCAPE) {
                ivMenuList[MODE_MYPAGE].requestFocus();
                lvMypageItems.setVisibility(View.INVISIBLE);
                clearSelection(lvMypageItems);
                vMypageContentInfo.setVisibility(View.INVISIBLE);
                vMypageContentPreview.setVisibility(View.INVISIBLE);
                vNoBookmark.setVisibility(View.INVISIBLE);
                vNoDownload.setVisibility(View.INVISIBLE);
                quickMenuFocused = true;
            }
            else if(keyCode == KeyEvent.KEYCODE_BACK) {
                switch (mMypageState) {
                    case MYPAGE_STATE_Submenu:
                        ivMenuList[MODE_MYPAGE].requestFocus();
                        lvMypageItems.setVisibility(View.INVISIBLE);
                        vNoBookmark.setVisibility(View.INVISIBLE);
                        vNoDownload.setVisibility(View.INVISIBLE);
                        quickMenuFocused = true;
                        break;
                    case MYPAGE_STATE_VODList:
                        clearSelection(lvMypageItems);
                        if(mMypageMode == MYPAGE_MODE_Bookmark)
                            vBookmarkTab.requestFocus();
                        else
                            vDownloadTab.requestFocus();
                        vMypageContentPreview.setVisibility(View.INVISIBLE);
                        mMypageState = MYPAGE_STATE_Submenu;
                        break;
                    case MYPAGE_STATE_Preview:
                        lvMypageItems.requestFocus();
                        vMypageContentInfo.setVisibility(View.INVISIBLE);
                        mMypageState = MYPAGE_STATE_VODList;
                        break;
                }
            }
        } else if (mLauncherMode == MODE_SEARCH) {
            // 검색은 back과 exit 분리
            if(keyCode == KeyEvent.KEYCODE_BACK) {
                LogUtil.d("search back - " + mSearchState);
                switch (mSearchState) {
                    case SEARCH_STATE_InputText:
                        etSearchText.setText("");
                        ivMenuList[MODE_SEARCH].requestFocus();
                        quickMenuFocused = true;
                        break;
                    case SEARCH_STATE_VODList:
                        etSearchText.requestFocus();
                        clearSelection(lvSearchResult);
                        lvSearchResult.setVisibility(View.INVISIBLE);
                        vSearchContentPreview.setVisibility(View.INVISIBLE);
                        vSearchContentInfo.setVisibility(View.INVISIBLE);
                        mSearchState = SEARCH_STATE_InputText;
                        break;
                    case SEARCH_STATE_Preview:
                        lvSearchResult.requestFocus();
                        vSearchContentInfo.setVisibility(View.INVISIBLE);
                        mSearchState = SEARCH_STATE_VODList;
                        break;
                }
            } else if(keyCode == KeyEvent.KEYCODE_ESCAPE) {
                switch (mSearchState) {
                    case SEARCH_STATE_InputText:
                    case SEARCH_STATE_VODList:
                    case SEARCH_STATE_Preview:
                        etSearchText.setText("");
                        clearSelection(lvSearchResult);
                        lvSearchResult.setVisibility(View.INVISIBLE);
                        vSearchContentInfo.setVisibility(View.INVISIBLE);
                        vSearchContentPreview.setVisibility(View.INVISIBLE);

                        ivMenuList[MODE_SEARCH].requestFocus();
                        quickMenuFocused = true;
                        break;
                }
            }
        } else if (mLauncherMode == MODE_SETUP) {
            // 변경된 내용 저장 체크
            if(keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_BACK) {
                if(mSetupState == SETUP_STATE_Detail) {
                    if(!(curLeftMenu == mSetupNetworkInfo && curLeftMenu.selected == 1 && isWifiPwEntering)) {
                        if(isSetupChanged) {
                            AlertDialog.Builder alert_confirm = new AlertDialog.Builder(MainActivity.this);
                            alert_confirm.setTitle(R.string.str_setup_exit_without_save);
                            alert_confirm.setCancelable(false).setPositiveButton(R.string.str_setup_confirm, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    handleBackKeyInSetup(keyCode);
                                    curLeftMenu.showView();
                                }
                            }).setNegativeButton(R.string.str_setup_cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            AlertDialog alert = alert_confirm.create();
                            alert.show();
                            return true;
                        }
                    }
                }
            }

            handleBackKeyInSetup(keyCode);
        }

        return false;
    }

    private void handleBackKeyInSetup(int keyCode) {
        if(keyCode == KeyEvent.KEYCODE_ESCAPE) {
            ivMenuList[MODE_SETUP].requestFocus();
            quickMenuFocused = true;
        } else if(keyCode == KeyEvent.KEYCODE_BACK) {
            switch(mSetupState) {
                case SETUP_STATE_Menu:
                    ivMenuList[MODE_SETUP].requestFocus();
                    quickMenuFocused = true;
                    break;
                case SETUP_STATE_LeftMenu:
                    setupItemList[mSelectedSetupItemIdx].requestFocus();
                    mSetupState = SETUP_STATE_Menu;
                    break;
                case SETUP_STATE_Detail:
                    if(curLeftMenu == mSetupNetworkInfo && curLeftMenu.selected == 1) {  // wifi
                        if(isWifiPwEntering) {
                            isWifiPwEntering = false;
                            wifiAP[0].requestFocus();
                            wifiPWAPName.setVisibility(View.INVISIBLE);
                            wifiPWGuide.setVisibility(View.INVISIBLE);
                            wifiPWInput.setVisibility(View.INVISIBLE);
                            wifiConnect.setVisibility(View.INVISIBLE);
                            wifiShowPW.setVisibility(View.INVISIBLE);
                            wifiShowPWDesc.setVisibility(View.INVISIBLE);
                            break;
                        }
                    }
                    setupBackToLeftMenu();
                    break;
            }
        }
    }

    private void setupBackToLeftMenu() {
        if(curLeftMenu.firstVisible == curLeftMenu.selected)
            setupLeftItemUpper.requestFocus();
        else
            setupLeftItemLower.requestFocus();
        mSetupState = SETUP_STATE_LeftMenu;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case REQUEST_CODE_PLAYER:
                if(resultCode == RESULT_OK) {
                    Bundle bundle = intent.getExtras();
                    int resStatus = bundle.getInt("returnVal");

                    if(resStatus == SednApplication.PLAYER_ERROR) {
                        showToast(getResources().getString(R.string.msg_cannot_play_video));
                    }
                }
                break;
        }
    }

    public void showToast(int strID) {
        showToast(getResources().getString(strID));
    }
    public void showToast(int strID, int length) {
        showToast(getResources().getString(strID), length);
    }
    public void showToast(String str) {
        showToast(str, Toast.LENGTH_LONG);
    }
    public void showToast(String str, int length) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, str, length).show();
            }
        });
    }

    public void redrawSTBInfo() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                tvGroupName.setText(mApp.myGroupName);
                tvDeviceName.setText(mApp.myName);
                LogUtil.d("redrawSTB " + mApp.myGroupName + ", " + mApp.myName);
            }
        });
    }

    public void setRecentVODPosition(int pos) {

        if(1 >= mRecentVOD.size())
            return;

        recentVODPosition.smoothScrollTo(910 - pos * 910 / (mRecentVOD.size()-1), 0);
    }

    private class DownloadImageViewListTask extends AsyncTask<ArrayList<String>, Integer, String> {
        ArrayList<ImageView> imageViewList;
        ArrayList<Bitmap> bitmapList;
        public DownloadImageViewListTask(ArrayList<ImageView> list) {
            imageViewList = list;
            bitmapList = new ArrayList<>();
        }

        protected String doInBackground(ArrayList<String>... list) {
            ArrayList<String> urlList = list[0];

            for(int i = 0; i < urlList.size(); i++) {
                Bitmap mBitmap= null;
                try {
                    InputStream in = new java.net.URL(urlList.get(i)).openStream();
                    mBitmap = BitmapFactory.decodeStream(in);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                bitmapList.add(mBitmap);
                publishProgress(i);
            }

            return "";
        }

        @Override
        protected void onProgressUpdate(Integer... pos) {
            LogUtil.d("thumb pos" + imageViewList.get(pos[0]) + ", " + pos[0]);
            imageViewList.get(pos[0]).setImageBitmap(bitmapList.get(pos[0]));
        }

        @Override
        protected void onPostExecute(String dummy) {
            LogUtil.d("onPostExecute");
        }
    }
    public void updateMenuInfo() {
        tvMenuInfoVOD.setText(String.format("%,d", mLocalDBManager.getAllVODTitles().size()) + " Clip");
        tvMenuInfoChannel.setText("3 CH");
        tvMenuInfoBookmark.setText(String.format("%,d", mLocalDBManager.getBookmarkedVOD().size()) + " Bookmark");
        tvMenuInfoDownload.setText(String.format("%,d", mLocalDBManager.getDownloadedVOD().size()) + " Download");
        tvMenuInfoResolution.setText(getDisplayText(mOutputUiManager.mOutputModeManager.getCurrentOutputMode()));
    }

    private String getDisplayText(String modeStr) {
        for(int i = 0; i < DISPLAY_RESOLUTION.length; i++) {
            if(DISPLAY_RESOLUTION[i].equals(modeStr)) {
                return RESOLUTION_TEXT[i];
            }
        }

        return "";
    }

    // 장비의 전체 UI를 업데이트한다.
    public void redrawUI(boolean isVODchanged) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                LogUtil.d("MainActivity redrawUI()");
                LayoutInflater inflater = getLayoutInflater();

                // STB 상태
                updateSTBStatus();

                // 메뉴 패널 정보
                updateMenuInfo();

                // - 홈 화면 구성 --------------------
                // ------- 오늘의 방송일정
                vfTodaySchedule.removeAllViews();
                todayScheduleRolling.removeAllViews();
                //ArrayList<ScheduleItem> todayScheduleList = SednDBClient.getTodaySchedule();
                ArrayList<ScheduleItem> todayScheduleList = RetrofitClient.getTodaySchedule();

                todayScheduleNum = todayScheduleList.size();
                stopTodayScheduleAutoScroll();
                if(todayScheduleNum > 0) {
                    //ArrayList<ImageView> scheduleImageList = new ArrayList<>();
                    //ArrayList<String> scheduleImageURLList = new ArrayList<>();
                    todayScheduleRollingNum = todayScheduleNum < 5 ? todayScheduleNum : 5;
                    for (int i = 0; i < todayScheduleNum; i++) {
                        ScheduleItem curItem = todayScheduleList.get(i);

                        LinearLayout todayScheduleItem = (LinearLayout) inflater.inflate(R.layout.today_schedule_item, vfTodaySchedule, false);
                        TextView tvScheduleName = (TextView) todayScheduleItem.findViewById(R.id.tvScheduleName);
                        TextView tvScheduleDuration = (TextView) todayScheduleItem.findViewById(R.id.tvScheduleDuration);
                        TextView tvScheduleGroup = (TextView) todayScheduleItem.findViewById(R.id.tvScheduleGroup);
                        TextView tvScheduleDesc = (TextView) todayScheduleItem.findViewById(R.id.tvScheduleDesc);
                        tvScheduleName.setText(curItem.name);
                        tvScheduleDuration.setText(curItem.durationStr);
                        tvScheduleGroup.setText(curItem.target);
                        tvScheduleDesc.setText(curItem.desc);
                        vfTodaySchedule.addView(todayScheduleItem);

                        if (!curItem.image_url.isEmpty()) {
                            ImageView ivScheduleImage = (ImageView) todayScheduleItem.findViewById(R.id.ivScheduleImage);

                            //scheduleImageList.add(ivScheduleImage);
                            //scheduleImageURLList.add(curItem.image_url);
                            Picasso.with(getApplicationContext()).load(curItem.image_url).into(ivScheduleImage);


                            LogUtil.d("today schedule thumb : " + curItem.image_url);
                        }
                    }
                    for (int i = 0; i < todayScheduleRollingNum; i++) {
                        ImageView rolling_point = new ImageView(MainActivity.this);
                        if (i == 0)
                            rolling_point.setImageResource(R.drawable.flip_select);
                        else
                            rolling_point.setImageResource(R.drawable.flip_deselect);
                        rolling_point.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(18, 18);
                        params.leftMargin = 3;
                        params.rightMargin = 3;
                        rolling_point.setLayoutParams(params);
                        todayScheduleRolling.addView(rolling_point);
                    }
                    //new DownloadImageViewListTask(scheduleImageList).execute(scheduleImageURLList);
                    todaySchedulePos = 0;
                    todayScheduleRollingPos = 0;
                    startTodayScheduleAutoScroll();
                } else {
                    // 등록된 일정이 없는 경우
                    LinearLayout todayScheduleItem = (LinearLayout) inflater.inflate(R.layout.today_schedule_none, vfTodaySchedule, false);
                    vfTodaySchedule.addView(todayScheduleItem);
                }

                // ------ 최근등록 VOD
                mRecentVOD = mLocalDBManager.getRecentVODList();
                layoutRecentVOD.removeAllViews();

                for(int i = 0; i < mRecentVOD.size(); i++) {
                    VODItem curItem = (VODItem)mRecentVOD.get(i);
                    RelativeLayout vod_view = (RelativeLayout)inflater.inflate(R.layout.item_recent_vod, layoutRecentVOD, false);
                    layoutRecentVOD.addView(vod_view);
                    // VOD 썸네일
                    ImageView tvThumbnail = (ImageView)vod_view.findViewById(R.id.ivThumbnail);
                    Picasso.with(getApplicationContext()).load(curItem.getThumbnailPath()).into(tvThumbnail);

                    LogUtil.d("recent VOD : " + curItem.getThumbnailPath());
                    // 기타 정보들
                    TextView tvDate = (TextView)vod_view.findViewById(R.id.tvRecentVODDate);
                    tvDate.setText(curItem.mRegisterDT);
                    TextView tvTime = (TextView)vod_view.findViewById(R.id.tvRecentVODTime);
                    tvTime.setText(curItem.mPlayTime);
                    TextView tvHit = (TextView)vod_view.findViewById(R.id.tvRecentVODHit);
                    tvHit.setText(String.format("%,d", curItem.mHit));
                    TextView tvCate = (TextView)vod_view.findViewById(R.id.tvRecentVODCate);
                    tvCate.setText(curItem.mCategory);
                    TextView tvTitle = (TextView)vod_view.findViewById(R.id.tvRecentVODTitle);
                    tvTitle.setText(curItem.getName());
                }

                mSelectedRecentVOD = 0;
                setRecentVODPosition(0);
                scrollRecentVOD.setScrollX(0); // 2017.4.21 ghlee 갱신될때 스크롤 위치를 초기화

                // ------ 가장많이 시청한 VOD
                mMostVOD = mLocalDBManager.getMostVODList();
                layoutMostVOD.removeAllViews();
                for(int i = 0; i < mMostVOD.size(); i++) {
                    VODItem curItem = (VODItem)mMostVOD.get(i);
                    LinearLayout mostVODItem = (LinearLayout)inflater.inflate(R.layout.most_vod_item, layoutMostVOD, false);
                    mostVODItem.setTag(i);
                    layoutMostVOD.addView(mostVODItem);
                    ImageView ivRank = (ImageView)mostVODItem.findViewById(R.id.ivVODRank);
                    ivRank.setImageResource(homeMostVODRankDrawables[i]);
                    TextView tvName = (TextView)mostVODItem.findViewById(R.id.tvVODName);
                    tvName.setText(curItem.getName());
                    TextView tvHit = (TextView)mostVODItem.findViewById(R.id.tvVODHit);
                    tvHit.setText(String.format("%,d", curItem.mHit));
                    mostVODItem.setOnKeyListener(mostVODKeyListener);
                    mostVODItem.setOnClickListener(mostVODClickListener);
                }

                // 배너 1, 2
                ArrayList<ImageView> banner = new ArrayList<>();
                ArrayList<String> bannerURL = new ArrayList<>();
                if(mApp.banner1URL != null && !mApp.banner1URL.isEmpty()) {
                    banner.add(homeBanner1);
                    bannerURL.add(mApp.banner1URL);
                }
                if(mApp.banner2URL != null && !mApp.banner2URL.isEmpty()) {
                    banner.add(homeBanner2);
                    bannerURL.add(mApp.banner2URL);
                }
                if(banner.size() > 0)
                    new DownloadImageViewListTask(banner).execute(bannerURL);


                // 라이브 메뉴
                layoutLiveChannel.removeAllViews();
                layoutLiveBroadcast.removeAllViews();

                //ArrayList<ChannelItem> channelList = SednDBClient.getChannelList();
                ArrayList<ChannelItem> channelList = RetrofitClient.getChannelList();

                mLiveChannelNum = channelList.size();
                for(int i = 0; i < channelList.size(); i++) {
                    ChannelItem curChannel = channelList.get(i);
                    LinearLayout channel_item_view = (LinearLayout)inflater.inflate(R.layout.item_live_channel, layoutLiveChannel, false);
                    TextView channelNum = (TextView)channel_item_view.findViewById(R.id.tvLiveChannelNum);
                    TextView channelName = (TextView)channel_item_view.findViewById(R.id.tvLiveChannelName);
                    channelNum.setText("CH "+ Utils.twoDigit(curChannel.channelIndex));
                    channelName.setText(curChannel.channelName);
                    channel_item_view.setOnClickListener(liveChannelClickListener);
                    channel_item_view.setOnKeyListener(liveChannelKeyListener);
                    layoutLiveChannel.addView(channel_item_view);

                    // 각 채널별 방송 정보
                    //ArrayList<ScheduleItem> scheduleList = SednDBClient.getLiveScheduleList(curChannel.channelIndex);
                    ArrayList<ScheduleItem> scheduleList = RetrofitClient.getLiveScheduleList(curChannel.channelIndex);

                    RelativeLayout broadcast_line_view = (RelativeLayout) inflater.inflate(R.layout.item_live_broadcast_line, layoutLiveBroadcast, false);
                    int lastFilledTime = 0;
                    for(int j = 0; j < scheduleList.size(); j++) {
                        ScheduleItem curSchedule = scheduleList.get(j);
                        // 앞쪽에 빈 영역이 있으면 채운다.
                        if(lastFilledTime < curSchedule.start_time) {
                            View dummy = getScheduleDummy(lastFilledTime, curSchedule.start_time);
                            broadcast_line_view.addView(dummy);
                        }
                        LinearLayout broadcast_view = (LinearLayout) inflater.inflate(R.layout.item_live_broadcast, broadcast_line_view, false);
                        // 화면 표시 텍스트 설정
                        TextView tvName = (TextView)broadcast_view.findViewById(R.id.tvName);
                        TextView tvDuration = (TextView)broadcast_view.findViewById(R.id.tvDuration);
                        TextView tvTarget = (TextView)broadcast_view.findViewById(R.id.tvTarget);
                        tvName.setText(curSchedule.name);
                        tvDuration.setText(curSchedule.durationStr);
                        tvTarget.setText(curSchedule.target);

                        // 위치 설정
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((curSchedule.end_time - curSchedule.start_time) * 7404 / 1440 - 5, 150);
                        params.setMargins(curSchedule.start_time * 7404 / 1440, 0, 0, 0);
                        broadcast_view.setLayoutParams(params);

                        // 색상 설정 (selector drawable 생성)
                        GradientDrawable focused = new GradientDrawable();
                        focused.setShape(GradientDrawable.RECTANGLE);
                        focused.setColor(curSchedule.color_code);
                        focused.setStroke(5, Color.WHITE);
                        ColorDrawable normal = new ColorDrawable();
                        normal.setColor(curSchedule.color_code);
                        StateListDrawable scheduleSelector = new StateListDrawable();
                        scheduleSelector.addState(new int[] {android.R.attr.state_focused}, focused);
                        scheduleSelector.addState(new int[] {android.R.attr.state_selected}, focused);
                        scheduleSelector.addState(new int[] {}, normal);
                        broadcast_view.setBackgroundDrawable(scheduleSelector);

                        // 기타
                        broadcast_view.setFocusable(true);
                        broadcast_view.setTag(curSchedule);
                        broadcast_view.setOnClickListener(liveScheduleClickListener);
                        broadcast_view.setOnKeyListener(liveScheduleKeyListener);

                        broadcast_line_view.addView(broadcast_view);
                        lastFilledTime = curSchedule.end_time;
                    }
                    // 마지막 스케줄 뒷부분 영역 채우기
                    broadcast_line_view.addView(getScheduleDummy(lastFilledTime, 1440));
                    layoutLiveBroadcast.addView(broadcast_line_view);
                }
                // 사용 채널이 3개 미만일 경우
                for(int i = 0; i < (3 - channelList.size()); i++) {
                    LinearLayout channel_item_view = (LinearLayout)inflater.inflate(R.layout.item_live_channel, layoutLiveChannel, false);
                    channel_item_view.setFocusable(false);
                    TextView channelNum = (TextView)channel_item_view.findViewById(R.id.tvLiveChannelNum);
                    TextView channelName = (TextView)channel_item_view.findViewById(R.id.tvLiveChannelName);
                    channelNum.setText("");
                    channelName.setText(getResources().getString(R.string.str_channel_not_available));
                    layoutLiveChannel.addView(channel_item_view);

                    RelativeLayout broadcast_line_view = (RelativeLayout) inflater.inflate(R.layout.item_live_broadcast_line, layoutLiveBroadcast, false);
                    broadcast_line_view.addView(getScheduleDummy(0, 1440));
                    layoutLiveBroadcast.addView(broadcast_line_view);
                }

                layoutSchedulePreview.setVisibility(View.INVISIBLE);

                /*// 개인방송 정보 읽어오기 - ghlee 2017.6
                mPersonalBR.clear();
                mPersonalBR = RetrofitClient.getPersonalBroadcast();

                // 하단 레이아웃. 샘플로 4개를 수동으로 넣어둔다.
                layoutLiveBottomList.removeAllViews();

                RelativeLayout live_bottom_view;
                TextView live_bottom_view_name, live_bottom_view_state;
                ImageView ivThumbnail;

                for(int i=0; i<4;i++) {
                    PersonalBroadcastResponse br = mPersonalBR.get(i);
                    live_bottom_view = (RelativeLayout)inflater.inflate(R.layout.item_live_bottom, layoutLiveBottomList, false);
                    live_bottom_view_name = (TextView)live_bottom_view.findViewById(R.id.tvLiveBottomName);
                    live_bottom_view_state = (TextView)live_bottom_view.findViewById(R.id.tvLiveBottomState);
                    ivThumbnail = (ImageView)live_bottom_view.findViewById(R.id.ivThumbnail);
                    live_bottom_view_name.setText(br.title);
                    live_bottom_view_state.setText(br.status);

                    if(br.status.equals("ON-AIR"))
                        live_bottom_view_state.setTextColor(getResources().getColor(R.color.red));
                    else
                        live_bottom_view_state.setTextColor(getResources().getColor(R.color.lightgray2));


                    Picasso.with(getApplicationContext()).load(br.thumbnail).into(ivThumbnail);

                    live_bottom_view.setOnClickListener(liveBottomClickListener);
                    live_bottom_view.setOnKeyListener(liveBottomKeyListener);
                    layoutLiveBottomList.addView(live_bottom_view);
                }*/

                // 하드코딩된 개인방송
                /*// 하단 레이아웃. 샘플로 4개를 수동으로 넣어둔다.
                layoutLiveBottomList.removeAllViews();

                RelativeLayout live_bottom_view;
                TextView live_bottom_view_name, live_bottom_view_state;
                ImageView ivThumbnail;

                live_bottom_view = (RelativeLayout)inflater.inflate(R.layout.item_live_bottom, layoutLiveBottomList, false);
                live_bottom_view_name = (TextView)live_bottom_view.findViewById(R.id.tvLiveBottomName);
                live_bottom_view_state = (TextView)live_bottom_view.findViewById(R.id.tvLiveBottomState);
                live_bottom_view_name.setText("LIVE #1");
                live_bottom_view_state.setText("ON-AIR");
                live_bottom_view_state.setTextColor(getResources().getColor(R.color.red));
                live_bottom_view.setOnClickListener(liveBottomClickListener);
                live_bottom_view.setOnKeyListener(liveBottomKeyListener);
                layoutLiveBottomList.addView(live_bottom_view);

                live_bottom_view = (RelativeLayout)inflater.inflate(R.layout.item_live_bottom, layoutLiveBottomList, false);
                live_bottom_view_name = (TextView)live_bottom_view.findViewById(R.id.tvLiveBottomName);
                live_bottom_view_state = (TextView)live_bottom_view.findViewById(R.id.tvLiveBottomState);
                live_bottom_view_name.setText("LIVE #2");
                live_bottom_view_state.setText("OFF-AIR");
                live_bottom_view_state.setTextColor(getResources().getColor(R.color.lightgray2));
                live_bottom_view.setOnClickListener(liveBottomClickListener);
                live_bottom_view.setOnKeyListener(liveBottomKeyListener);
                layoutLiveBottomList.addView(live_bottom_view);

                live_bottom_view = (RelativeLayout)inflater.inflate(R.layout.item_live_bottom, layoutLiveBottomList, false);
                live_bottom_view_name = (TextView)live_bottom_view.findViewById(R.id.tvLiveBottomName);
                live_bottom_view_state = (TextView)live_bottom_view.findViewById(R.id.tvLiveBottomState);
                live_bottom_view_name.setText("LIVE #3");
                live_bottom_view_state.setText("OFF-AIR");
                live_bottom_view_state.setTextColor(getResources().getColor(R.color.lightgray2));
                live_bottom_view.setOnClickListener(liveBottomClickListener);
                live_bottom_view.setOnKeyListener(liveBottomKeyListener);
                layoutLiveBottomList.addView(live_bottom_view);

                live_bottom_view = (RelativeLayout)inflater.inflate(R.layout.item_live_bottom, layoutLiveBottomList, false);
                live_bottom_view_name = (TextView)live_bottom_view.findViewById(R.id.tvLiveBottomName);
                live_bottom_view_state = (TextView)live_bottom_view.findViewById(R.id.tvLiveBottomState);
                live_bottom_view_name.setText("LIVE #4");
                live_bottom_view_state.setText("ON-AIR");
                live_bottom_view_state.setTextColor(getResources().getColor(R.color.red));
                live_bottom_view.setOnClickListener(liveBottomClickListener);
                live_bottom_view.setOnKeyListener(liveBottomKeyListener);
                layoutLiveBottomList.addView(live_bottom_view);*/



                // VOD 1-depth 메뉴 세팅
                mMenuSednListAdapter[0].setItems(mLocalDBManager.getRootMenu());

                // 화면 갱신 시나리오
                // 1. SETUP 상태는 화면 갱신에 영향받지 않음
                // 2. VOD 목록이 변경된 경우 무조건 HOME으로 전환
                // 3. VOD 변경 없으면 HOME과 LIVE 상태에서만 포커스 이동(퀵메뉴로)하고, 나머지 상태는 영향받지 않음
                if(mLauncherMode != MODE_SETUP) {
                    if(isVODchanged) {
                        if (mLauncherMode == MODE_HOME) {                 // HOME일 경우 포커스만 이동
                            ivMenuList[mLauncherMode].requestFocusFromTouch();
                            quickMenuFocused = true;
                        } else {        // 다른 메뉴상태이면 HOME으로 전환
                            switchMenu(MODE_HOME);
                        }
                    } else {
                        if(mLauncherMode == MODE_HOME || mLauncherMode == MODE_LIVE) {
                            ivMenuList[mLauncherMode].requestFocusFromTouch();
                            quickMenuFocused = true;
                      }
                    }

                    // HOME 화면에서 최근등록VOD, 인기VOD에 포커스가 되어있을때 스케쥴이 갱신되면 포커스 해제 - ghlee 2017.4.12
                    if(HOME_BOX_TODAY_SCHEDULE == mHomeSeletedBox) {
                        homeTodaySchedule.clearFocus();
                        homeTodaySchedule.setSelected(false);
                        todayScheduleRolling.clearFocus();
                    }
                    else if(HOME_BOX_SEARCH == mHomeSeletedBox) {
                        homeSearch.clearFocus();
                        homeSearch.setSelected(false);
                    }
                    else if(HOME_BOX_RECENT_VOD  == mHomeSeletedBox) {
                        homeRecentVOD.clearFocus();
                        homeRecentVOD.setSelected(false);
                        layoutRecentVOD.clearFocus();
                        recentFocusBox.setVisibility(View.INVISIBLE);
                    }
                    else if(HOME_BOX_MOST_VOD == mHomeSeletedBox) {
                        homeMostVOD.clearFocus();
                        homeMostVOD.setSelected(false);
                        layoutMostVOD.clearFocus();
                    }

                }
            }
        });
    }

    private void updateLiveTimeBar() {
        Calendar cal = Calendar.getInstance();
        int curMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        LogUtil.d("updateLiveTimeBar - " + curMin);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)ivCurTimeBar.getLayoutParams();
        params.setMargins(curMin * 7404 / 1440 - 10, 0, 0, 0);  // 이미지 두께의 절반만큼 당김
        ivCurTimeBar.setLayoutParams(params);
    }

    private void focusToTimeBar() {
        hsvLiveBroadcast.post(new Runnable() {
            @Override
            public void run() {
                Calendar cal = Calendar.getInstance();
                int curMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                curMin -= 60; // 1 시간 앞부터 보여줌
                LogUtil.d("focusToTimeBar - " + curMin);
                hsvLiveBroadcast.scrollTo(curMin * 7404 / 1440, 0);
            }
        });
    }

    private View getScheduleDummy(int start, int end) {
        View dummy = new View(MainActivity.this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((end - start) * 7404 / 1440 - 5, 150);
        params.setMargins(start * 7404 / 1440, 0, 0, 0);
        dummy.setLayoutParams(params);
        dummy.setFocusable(false);
        dummy.setBackgroundResource(R.color.liveBGColor);

        return dummy;
    }
    private void startTodayScheduleAutoScroll() {
        if(todayScheduleNum > 1)
            todayScheduleHandler.sendEmptyMessageDelayed(0, 10000);
    }

    private void stopTodayScheduleAutoScroll() {
        todayScheduleHandler.removeMessages(0);
    }

    private void todayScheduleMove(int offset) {
        if(todayScheduleNum > 0) {
            int new_pos = (todaySchedulePos + offset + todayScheduleNum) % todayScheduleNum;
            int new_rolling_pos = (todayScheduleRollingPos + offset + todayScheduleRollingNum) % todayScheduleRollingNum;

            ImageView old_view = (ImageView) todayScheduleRolling.getChildAt(todayScheduleRollingPos);
            ImageView new_view = (ImageView) todayScheduleRolling.getChildAt(new_rolling_pos);

            todaySchedulePos = new_pos;
            todayScheduleRollingPos = new_rolling_pos;

            vfTodaySchedule.setDisplayedChild(todaySchedulePos);
            old_view.setImageResource(R.drawable.flip_deselect);
            new_view.setImageResource(R.drawable.flip_select);
        }
    }

    public void restoreSavedLogo() {
        // 저장된 STB 설정 복원
        if(Utils.getSTBLogoImageYN(this).equals("Y")) {    // 로고
            setLogoImage();
        } else {
            setLogoText();
        }

    }

    // Background 영상, 로고, 시간, 날씨 세팅
    private void initBackground() {

        ivClientLogo = (ImageView)findViewById(R.id.ivClientLogo);
        tvClientLogo = (TextView)findViewById(R.id.tvClientLogo);

        vvBackground = (BGVideoView) findViewById(R.id.vvBackground);
        ivImageBackground = (ImageView) findViewById(R.id.ivImageBackground);
        // 배경 이미지(영상)은 DB설정 상태 확인 후 복원

        tvStatusColor = (TextView) findViewById(R.id.tvStatusColor);
        tvStatusText = (TextView) findViewById(R.id.tvStatusText);
        tvStorageUsage = (TextView) findViewById(R.id.tvStorageUsage);

        tvGroupName = (TextView) findViewById(R.id.tvGroupName);
        tvDeviceName = (TextView) findViewById(R.id.tvDeviceName);

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
                updateLiveTimeBar();
            }
        });

        mWeatherIconMap = new HashMap<>();
        mWeatherIconMap.put("01d", R.drawable.weather_01);
        mWeatherIconMap.put("01n", R.drawable.weather_10);
        mWeatherIconMap.put("02d", R.drawable.weather_02);
        mWeatherIconMap.put("02n", R.drawable.weather_09);
        mWeatherIconMap.put("03d", R.drawable.weather_03);
        mWeatherIconMap.put("03n", R.drawable.weather_03);
        mWeatherIconMap.put("04d", R.drawable.weather_03);
        mWeatherIconMap.put("04n", R.drawable.weather_03);
        mWeatherIconMap.put("09d", R.drawable.weather_04);
        mWeatherIconMap.put("09n", R.drawable.weather_04);
        mWeatherIconMap.put("10d", R.drawable.weather_05);
        mWeatherIconMap.put("10n", R.drawable.weather_05);
        mWeatherIconMap.put("11d", R.drawable.weather_06);
        mWeatherIconMap.put("11n", R.drawable.weather_06);
        mWeatherIconMap.put("13d", R.drawable.weather_07);
        mWeatherIconMap.put("13n", R.drawable.weather_07);
        mWeatherIconMap.put("50d", R.drawable.weather_08);
        mWeatherIconMap.put("50n", R.drawable.weather_08);

        mApp.mSTBLocation = null;
        ivWeather = (ImageView)findViewById(R.id.ivBGWeather);
        tvTemperature = (TextView)findViewById(R.id.tvBGTemperature);
        // 날씨, 온도
        TimerTask mWeatherUpdateTask = new TimerTask() {
            @Override
            public void run() {

                if(mApp.mSTBLocation == null) {

                    /*// 내 좌표 가져오기
                    try {
                        Uri uri = new Uri.Builder()
                                .scheme("http")
                                .authority("ip-api.com")
                                .path("json")
                                .build();
                        URL url = new URL(uri.toString());
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(10000);
                        conn.setRequestMethod("GET");

                        int resCode = conn.getResponseCode();
                        LogUtil.d("ip-api result code - " + resCode);
                        if (resCode == HttpURLConnection.HTTP_OK) {
                            String response = new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine();
                            JSONObject obj = new JSONObject(response);
                            mSTBLocation = new STBLocation((Double)obj.get("lat"), (Double)obj.get("lon"));
                            LogUtil.d("my location : " + mSTBLocation.getLatitude() + ", " + mSTBLocation.getLogitude());
                        }
                        conn.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/

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

                            // 좌표로 현재 날씨 가져오기
                            getWeather();
                        }

                        http.disconnect();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        };
        Timer mWeatherTimer = new Timer();
        mWeatherTimer.schedule(mWeatherUpdateTask, 0, 30 * 60 * 1000); // 30분
    }

    private void switchMenu(int menuNum) {
        //v.startAnimation(menuClickedAnim);
        LogUtil.d("switch Menu - " + menuNum + ", " + mLauncherMode);

        if(menuNum != mLauncherMode) {
            if(menuNum == MODE_HOME && mLauncherMode != MODE_HOME) {
                updateMenuInfo();
                vLayoutQuickmenu.startAnimation(quickMenuSlideInAnim);                  // 메뉴 펼침
            }
            if(menuNum != MODE_HOME && mLauncherMode == MODE_HOME) {
                vLayoutQuickmenu.startAnimation(quickMenuSlideOutAnim);                // 메뉴 접힘
            }

            ivMenuList[mLauncherMode].setSelected(false);
            ivMenuList[menuNum].setSelected(true);


            boolean bNext = false;

            // 뷰플리퍼 애니메이션 변경
            if(MODE_SETUP == menuNum && MODE_HOME == mLauncherMode) {
                vfScreenFlipper.setInAnimation(slide_in_r);
                vfScreenFlipper.setOutAnimation(slide_out_r);
            }
            else if(menuNum > mLauncherMode || (MODE_HOME == menuNum && MODE_SETUP == mLauncherMode)) {
                vfScreenFlipper.setInAnimation(slide_in);
                vfScreenFlipper.setOutAnimation(slide_out);
                bNext = true;
            }
            else {
                vfScreenFlipper.setInAnimation(slide_in_r);
                vfScreenFlipper.setOutAnimation(slide_out_r);
            }

            mLauncherMode = menuNum;

            vfScreenFlipper.setDisplayedChild(mLauncherMode);

//            if(bNext)
//                vfScreenFlipper.showNext();
//            else
//                vfScreenFlipper.showPrevious();
        }

        resetMode(mLauncherMode);
    }

    private void switchMenu(View v) {
        int menuNum = -1;

        int id = v.getId();
        for (int i = 0; i < menuIDs.length; i++) {
            if (menuIDs[i] == id)
                menuNum = i;
        }
        switchMenu(menuNum);
    }

    private void initMenu() {
        // menu listener 등록
        View.OnFocusChangeListener menuFocusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                LogUtil.d("menu onFocusChange - " + hasFocus);

                // 포커스 받은 아이콘이 곧바로 업데이트되지 않는 문제 패치
                if(hasFocus) {
                    v.invalidate();
                    v.postInvalidate();
                    v.refreshDrawableState();

                    ((View)(v.getParent())).invalidate();
                    ((View)(v.getParent())).postInvalidate();
                    ((View)(v.getParent())).refreshDrawableState();
                }

                //if(mLauncherMode != MODE_HOME)
                {
                    if (hasFocus && quickMenuFocused) {
                        LogUtil.d("Menu Focused : " + v.getId() + ", " + quickMenuFocused);
                        switchMenu(v);
                    }
                }
            }
        };
        View.OnClickListener menuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int menuNum = -1;

                int id = v.getId();
                for (int i = 0; i < menuIDs.length; i++) {
                    if (menuIDs[i] == id)
                        menuNum = i;
                }
                v.startAnimation(menuClickedAnim);
                if(mLauncherMode == MODE_HOME) {
                    if(menuNum == MODE_HOME)
                        focusDefaultItem(menuNum);
                    else
                        switchMenu(v);
                } else
                    focusDefaultItem(menuNum);
            }
        };
        View.OnHoverListener menuHoverListener = new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                if (!v.isFocused())
                    v.requestFocus();
                return true;
            }
        };
        View.OnKeyListener menuKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                int id = v.getId();
                if(event.getAction() == KeyEvent.ACTION_DOWN) {
                    LogUtil.d("menu key - " + id);
                    // HOME에서의 UP과 SETUP에서의 DOWN 처리.
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP && id == R.id.ivMenuHome) {
                        ivMenuList[MODE_SETUP].requestFocus();
                        return true;
                    }

                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && id == R.id.ivMenuSetup) {
                        ivMenuList[MODE_HOME].requestFocus();
                        return true;
                    }

                    // 메뉴별/상태별 포커스 이동 처리
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        return true;
                    }

                }
                return false;
            }
        };

        ivMenuList = new ImageView[menuIDs.length];
        for (int i = 0; i < menuIDs.length; i++) {
            ivMenuList[i] = (ImageView) findViewById(menuIDs[i]);
            ivMenuList[i].setOnFocusChangeListener(menuFocusChangeListener);
            ivMenuList[i].setOnClickListener(menuClickListener);
            ivMenuList[i].setOnHoverListener(menuHoverListener);
            ivMenuList[i].setOnKeyListener(menuKeyListener);
        }
        ivMenuList[MODE_HOME].setSelected(true);

        vLayoutHome = (PercentRelativeLayout) findViewById(R.id.layoutHome);
        vLayoutVOD = (PercentRelativeLayout) findViewById(R.id.layoutVOD);
        vLayoutLive = (ViewGroup) findViewById(R.id.layoutLive);
        vLayoutMypage = (ViewGroup) findViewById(R.id.layoutMyPage);
        vLayoutSearch = (ViewGroup) findViewById(R.id.layoutSearch);
        vLayoutSetup = (ViewGroup) findViewById(R.id.layoutSetup);

        vLayoutLive.setVisibility(View.GONE);
        vLayoutMypage.setVisibility(View.GONE);
        vLayoutSearch.setVisibility(View.GONE);
        vLayoutSetup.setVisibility(View.GONE);

        vLayoutQuickmenu = (ViewGroup)findViewById(R.id.layoutQuickmenu);

        tvMenuInfoVOD = (TextView)findViewById(R.id.tvMenuInfoVOD);
        tvMenuInfoChannel = (TextView)findViewById(R.id.tvMenuInfoChannel);
        tvMenuInfoBookmark = (TextView)findViewById(R.id.tvMenuInfoBookmark);
        tvMenuInfoDownload = (TextView)findViewById(R.id.tvMenuInfoDownload);
        tvMenuInfoIP = (TextView)findViewById(R.id.tvMenuInfoIP);
        tvMenuInfoResolution = (TextView)findViewById(R.id.tvMenuInfoResolution);
    }

    private void initHome() {
        mHomeState = HOME_STATE_Box;

        // 각 박스별 레이아웃
        homeTodaySchedule = findViewById(R.id.homeTodaySchedule);
        homeSearch = findViewById(R.id.homeSearch);
        homeRecentVOD = findViewById(R.id.homeRecentVOD);
        homeMostVOD = findViewById(R.id.homeMostVOD);

        // 박스 공용 listener 등록
        View.OnKeyListener homeBoxKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        int id = v.getNextFocusUpId();
                        if(id != -1) {
                            findViewById(id).requestFocus();
                        }
                        return true;
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        int id = v.getNextFocusDownId();
                        if(id != -1) {
                            findViewById(id).requestFocus();
                        }
                        return true;
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        int id = v.getNextFocusLeftId();
                        if(id != -1) {
                            findViewById(id).requestFocus();
                        }
                        return true;
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        int id = v.getNextFocusRightId();
                        if(id != -1) {
                            findViewById(id).requestFocus();
                        }
                        return true;
                    }
                }
                return false;
            }
        };

        View.OnClickListener homeBoxClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHomeState = HOME_STATE_Box_inside;

                int id = v.getId();
                switch (id) {
                    case R.id.homeTodaySchedule:
                        if(todayScheduleNum > 0) {
                            homeTodaySchedule.setSelected(true);
                            mHomeSeletedBox = HOME_BOX_TODAY_SCHEDULE;
                            todayScheduleRolling.requestFocus();
                            stopTodayScheduleAutoScroll();
                            // 깜박임 효과
                            ImageView selectedPoint = (ImageView) todayScheduleRolling.getChildAt(todayScheduleRollingPos);
                            Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.today_schedule_clicked);
                            selectedPoint.startAnimation(anim);
                        }
                        break;
                    case R.id.homeSearch:
                        homeSearch.setSelected(true);
                        mHomeSeletedBox = HOME_BOX_SEARCH;
                        homeSearchBox.setAdapter(new ArrayAdapter<String>(MainActivity.this, R.layout.search_dropdown, mLocalDBManager.getAllVODTitles()));
                        homeSearchBox.requestFocus();
                        break;
                    case R.id.homeRecentVOD:
                        homeRecentVOD.setSelected(true);
                        mHomeSeletedBox = HOME_BOX_RECENT_VOD;
                        layoutRecentVOD.requestFocus();
                        recentFocusBox.setVisibility(View.VISIBLE);
                        break;
                    case R.id.homeMostVOD:
                        homeMostVOD.setSelected(true);
                        mHomeSeletedBox = HOME_BOX_MOST_VOD;
                        layoutMostVOD.getChildAt(0).requestFocus();
                        break;
                }
            }
        };

        homeTodaySchedule.setOnKeyListener(homeBoxKeyListener);
        homeSearch.setOnKeyListener(homeBoxKeyListener);
        homeRecentVOD.setOnKeyListener(homeBoxKeyListener);
        homeMostVOD.setOnKeyListener(homeBoxKeyListener);
        homeTodaySchedule.setOnClickListener(homeBoxClickListener);
        homeSearch.setOnClickListener(homeBoxClickListener);
        homeRecentVOD.setOnClickListener(homeBoxClickListener);
        homeMostVOD.setOnClickListener(homeBoxClickListener);


        // 오늘의 방송일정
        vfTodaySchedule = (ViewFlipper)findViewById(R.id.vfTodayScheduleFlipper);
        todayScheduleRolling = (LinearLayout)findViewById(R.id.todayScheduleRolling);
        todayScheduleRolling.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        todayScheduleMove(TODAY_SCHEDULE_NEXT);
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        todayScheduleMove(TODAY_SCHEDULE_PREV);
                        return true;
                    }
                }
                return false;
            }
        });

        // 검색
        homeSearchBox = (SednAutoCompleteTextView)findViewById(R.id.homeSearchBox);
        homeSearchBox.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction() == KeyEvent.ACTION_DOWN) {
                    if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        int len = homeSearchBox.getText().length();
                        int cursor_pos = homeSearchBox.getSelectionEnd();
                        if(len == cursor_pos)   // 오른쪽으로 벗어나지 못하게 막는다.
                            return true;
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if(!homeSearchBox.isPopupShowing())
                            return true;
                    }
                }
                return false;
            }
        });
        homeSearchBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(homeSearchBox.getText().length() > 0) {
                    switchMenu(MODE_SEARCH);
                    etSearchText.setText(homeSearchBox.getText());
                    quickMenuFocused = false;
                    doSearch();
                }
            }
        });

        // 최근등록 VOD
        scrollRecentVOD = (HorizontalScrollView)findViewById(R.id.scrollRecentVOD);
        layoutRecentVOD = (LinearLayout)findViewById(R.id.layoutRecentVOD);
        recentFocusBox = findViewById(R.id.recentFocusBox);
        recentVODPosition = (HorizontalScrollView)findViewById(R.id.recentVODPosition);
        layoutRecentVOD.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                LogUtil.d("recent VOD key listener");
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if(mSelectedRecentVOD < (mRecentVOD.size()-1)) mSelectedRecentVOD++;
                        scrollRecentVOD.smoothScrollTo(mSelectedRecentVOD * (360+37), 0);
                        setRecentVODPosition(mSelectedRecentVOD);
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if(mSelectedRecentVOD > 0) mSelectedRecentVOD--;
                        scrollRecentVOD.smoothScrollTo(mSelectedRecentVOD * (360+37), 0);
                        setRecentVODPosition(mSelectedRecentVOD);
                        return true;
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        return true;
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return true;
                    }
                }

                return false;
            }
        });
        layoutRecentVOD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //LogUtil.d("Selected VOD " + mRecentVOD.get(mSelectedRecentVOD).getName() + ", " + mRecentVOD.get(mSelectedRecentVOD).getID());
                switchMenu(MODE_VOD);
                expandToVOD((VODItem)(mRecentVOD.get(mSelectedRecentVOD)));
            }
        });

        // 가장 많이 시청한 VOD
        layoutMostVOD = (LinearLayout)findViewById(R.id.layoutMostVOD);
        mostVODKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                int pos = (int)v.getTag();
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if(pos == 0) {
                            layoutMostVOD.getChildAt(layoutMostVOD.getChildCount() - 1).requestFocus();
                            return true;
                        }
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if(pos == mMostVOD.size() - 1) {
                            layoutMostVOD.getChildAt(0).requestFocus();
                            return true;
                        }
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return true;
                    }
                }
                return false;
            }
        };
        mostVODClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = (int)v.getTag();
                switchMenu(MODE_VOD);
                expandToVOD((VODItem)(mMostVOD.get(pos)));
            }
        };

        // 배너 1, 2
        homeBanner1 = (ImageView)findViewById(R.id.homeBanner1);
        homeBanner2 = (ImageView)findViewById(R.id.homeBanner2);
    }

    private void initVOD() {

        mVODState = VOD_STATE_MenuCategory;
        mVODMenuDepth = 1;

        lvMenuCategory = new ListView[2];
        mMenuSednListAdapter = new SednListAdapter[2];
        leftPos = new float[2];

        lvMenuCategory[0] = (ListView) findViewById(R.id.lv1stCategory);
        lvMenuCategory[1] = (ListView) findViewById(R.id.lv2ndCategory);
        lvVODItems = (ListView) findViewById(R.id.lvVODItems);

        PercentRelativeLayout.LayoutParams params1 = (PercentRelativeLayout.LayoutParams) lvMenuCategory[1].getLayoutParams();
        PercentLayoutHelper.PercentLayoutInfo info1 = params1.getPercentLayoutInfo();
        leftPos[0] = info1.leftMarginPercent;
        PercentRelativeLayout.LayoutParams params2 = (PercentRelativeLayout.LayoutParams) lvVODItems.getLayoutParams();
        PercentLayoutHelper.PercentLayoutInfo info2 = params2.getPercentLayoutInfo();
        leftPos[1] = info2.leftMarginPercent;

        lvMenuCategory[1].setVisibility(View.INVISIBLE);
        lvVODItems.setVisibility(View.INVISIBLE);

        SednItemList.OnSednItemSelectListener depth1ContentSelectListener = new SednItemList.OnSednItemSelectListener() {
            @Override
            public void onSednItemSelect(ListviewBaseItem item, int index, float topPercent, float heightPercent, boolean setFocus) {
                selectVOD1Depth(item.getID(), setFocus);
            }
        };
        mMenuSednListAdapter[0] = SednItemList.BuildList(this, lvMenuCategory[0], (ImageView) findViewById(R.id.iv1stUpArrow), (ImageView) findViewById(R.id.iv1stDownArrow),
                new ArrayList<ListviewBaseItem>(), depth1ContentSelectListener, null, null, null, lvMenuCategory[1],
                R.drawable.selector_vod_1st_category, 9, 2.2727f, Gravity.CENTER, 0f);

        SednItemList.OnSednItemSelectListener depth2ContentSelectListener = new SednItemList.OnSednItemSelectListener() {
            @Override
            public void onSednItemSelect(ListviewBaseItem item, int index, float topPercent, float heightPercent, boolean setFocus) {
                vVODContentPreview.setVisibility(View.INVISIBLE);
                vVODContentInfo.setVisibility(View.INVISIBLE);
                lvVODItems.setVisibility(View.INVISIBLE);
                mVODState = VOD_STATE_MenuCategory;
                mVODMenuDepth = 2;

                ArrayList<ListviewBaseItem> vodList = mLocalDBManager.getVODList(item.getID());
                if (vodList.size() > 0) {
                    PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) lvVODItems.getLayoutParams();
                    PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
                    info.leftMarginPercent = leftPos[mVODMenuDepth -1];
                    lvVODItems.getParent().requestLayout();

                    lvVODItems.setVisibility(View.VISIBLE);
                    mVODDataAdapter.setToLeft(lvMenuCategory[1]);

                    mVODDataAdapter.setItems(vodList);

                    if(setFocus) {
                        mVODState = VOD_STATE_VODList;
                        lvVODItems.requestFocus();
                        lvVODItems.setSelection(0);
                    }

                    for (int i = 0; i < vodList.size(); i++)
                        lvVODItems.setItemChecked(i, false);
                }
            }
        };
        mMenuSednListAdapter[1] = SednItemList.BuildList(this, lvMenuCategory[1], (ImageView) findViewById(R.id.iv2ndUpArrow), (ImageView) findViewById(R.id.iv2ndDownArrow),
                new ArrayList<ListviewBaseItem>(), depth2ContentSelectListener, null, null, lvMenuCategory[0], lvVODItems,
                R.drawable.selector_vod_2nd_category, 9, 2.2727f, Gravity.CENTER, 0f);

        SednItemList.OnSednItemSelectListener depth3ContentSelectListener = new SednItemList.OnSednItemSelectListener() {
            @Override
            public void onSednItemSelect(ListviewBaseItem item, int index, float topPercent, float heightPercent, boolean setFocus) {
                vVODContentInfo.setVisibility(View.INVISIBLE);

                PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) vVODContentPreview.getLayoutParams();
                PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
                LogUtil.d("params " + topPercent + ", " + info.heightPercent);
                if (topPercent + heightPercent + info.heightPercent < 1)
                    info.topMarginPercent = topPercent + heightPercent;
                else
                    info.topMarginPercent = topPercent - info.heightPercent;
                mtopMarginPercentForVODPreview = info.topMarginPercent;
                ivVODThumbnail.setBackgroundResource(0);
                ivVODThumbnail.setImageBitmap(null);
                vVODContentPreviewButton[1].setImageResource(R.drawable.icon_bookmark);
                vVODContentPreviewButton[2].setImageResource(R.drawable.icon_down);

                VODItem curItem = (VODItem)mVODDataAdapter.getSelectedItem();
                tvVODContentDate.setText(curItem.mRegisterDT);
                tvVODContentTime.setText(curItem.mPlayTime);
                tvVODContentHit.setText(String.format("%,d", curItem.mHit));
                tvVODContentCate.setText(curItem.mCategory);
                tvVODContentTitle.setText(curItem.getName());

                vVODContentPreview.setVisibility(View.VISIBLE);

                LogUtil.d("thumb down : " + ivVODThumbnail.toString() + ", " + curItem.getThumbnailPath());

                //new ThumnailLoader(ivVODThumbnail).execute(curItem.getThumbnailPath());
                Picasso.with(getApplicationContext()).load(curItem.getThumbnailPath()).into(ivVODThumbnail);

                if(setFocus) {
                    mVODState = VOD_STATE_Preview;
                    vVODContentPreview.requestFocus();
                }
            }
        };
        mVODDataAdapter = SednItemList.BuildList(this, lvVODItems, (ImageView) findViewById(R.id.ivItemsUpArrow), (ImageView) findViewById(R.id.ivItemsDownArrow),
                new ArrayList<ListviewBaseItem>(), depth3ContentSelectListener, null, null, lvMenuCategory[1], null,
                R.drawable.selector_vod_item_category, 9, 2.2727f, Gravity.LEFT | Gravity.CENTER_VERTICAL, 30f / 1006f);

        vVODContentPreview = findViewById(R.id.vod_content_preview);
        // VOD의 Preview는 overlapping view이므로 hover handler를 달아서 뒤에 있는 listview를 막아야 한다.
        vVODContentPreview.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                return true;
            }
        });

        View.OnClickListener vodPreviewClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                VODItem selectedItem = (VODItem)mVODDataAdapter.getSelectedItem();
                switch(id) {
                    case R.id.ivPreview_play:
                        playVOD(PlayerActivity.PLAY_STB_VOD, selectedItem);
                        break;
                    case R.id.ivPreview_bookmark:
                        setBookmark(selectedItem.getID());
                        mVODDataAdapter.notifyDataSetChanged();
                        break;
                    case R.id.ivPreview_download:
                        requestDownload(selectedItem);
                        break;
                }
            }
        };
        View.OnKeyListener vodPreviewKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                int id = v.getId();
                if(event.getAction() == KeyEvent.ACTION_DOWN) {
                    // Right / Left 키는 무조건 막는다.
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
                        return true;
                    // 최상위에서의 UP과 최하위에서의 DOWN 처리
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP && id == previewIDs[0]) {
                        vVODContentPreviewButton[previewIDs.length-1].requestFocus();
                        return true;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && id == previewIDs[previewIDs.length - 1]) {
                        vVODContentPreviewButton[0].requestFocus();
                        return true;
                    }
                }
                return false;
            }
        };
        vVODContentPreviewButton = new ImageView[previewIDs.length];
        for (int i = 0; i < previewIDs.length; i++) {
            vVODContentPreviewButton[i] = (ImageView) vVODContentPreview.findViewById(previewIDs[i]);

            //ivMenuList[i].setOnFocusChangeListener(menuFocusChangeListener);
            vVODContentPreviewButton[i].setOnClickListener(vodPreviewClickListener);
            vVODContentPreviewButton[i].setOnKeyListener(vodPreviewKeyListener);
            //iv.setOnHoverListener(previewHoverListener);
        }
        vVODContentPreviewButton[previewIDs.length-1].setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) vVODContentInfo.getLayoutParams();
                    PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
                    info.topMarginPercent = mtopMarginPercentForVODPreview + 0.285f;
                    LogUtil.d("info window : " + info.topMarginPercent + " + " + info.heightPercent);
                    // info 뷰가 최하단 이하로 내려간 경우 처리
                    if(info.topMarginPercent + info.heightPercent > 1.0f)
                        info.topMarginPercent = 1.0f - info.heightPercent;
                    vVODContentInfo.setLayoutParams(params);
                    setContentInfoView(vVODContentInfo, (VODItem)mVODDataAdapter.getSelectedItem());
                    vVODContentInfo.setVisibility(View.VISIBLE);
                } else {
                    vVODContentInfo.setVisibility(View.INVISIBLE);
                }
            }
        });
        ivVODThumbnail = (ImageView) vVODContentPreview.findViewById(R.id.ivPreview_thumbnail);
        tvVODContentDate = (TextView) vVODContentPreview.findViewById(R.id.tvVODDate);
        tvVODContentTime = (TextView) vVODContentPreview.findViewById(R.id.tvVODTime);
        tvVODContentHit = (TextView) vVODContentPreview.findViewById(R.id.tvVODHit);
        tvVODContentCate = (TextView) vVODContentPreview.findViewById(R.id.tvVODCate);
        tvVODContentTitle = (TextView) vVODContentPreview.findViewById(R.id.tvVODTitle);

        vVODContentPreview.setVisibility(View.INVISIBLE);

        vVODContentInfo = findViewById(R.id.vod_content_info);
        vVODContentInfo.setVisibility(View.INVISIBLE);
    }

    private void expandToVOD(VODItem item) {
        LogUtil.d("expand to " + item.mMenuID[0] + ", " + item.mMenuID[1]);

        lvVODItems.setVisibility(View.VISIBLE);
        vVODContentPreview.setVisibility(View.INVISIBLE);
        vVODContentInfo.setVisibility(View.INVISIBLE);

        if(item.mMenuID[1] != null)
            mVODMenuDepth = 2;
        else
            mVODMenuDepth = 1;

        PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) lvVODItems.getLayoutParams();
        PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
        info.leftMarginPercent = leftPos[mVODMenuDepth -1];

        if(mVODMenuDepth == 2) {
            // 2 depth
            lvMenuCategory[1].setVisibility(View.VISIBLE);
            mMenuSednListAdapter[0].setToRight(lvMenuCategory[1]);
            mMenuSednListAdapter[1].setItems(mLocalDBManager.getSubMenu(item.mMenuID[0]));
            for(int i = 0; i < mMenuSednListAdapter[0].size(); i++) {
                ListviewBaseItem menuItem = mMenuSednListAdapter[0].getItem(i);
                if(menuItem.getID().equals(item.mMenuID[0])) {
                    lvMenuCategory[0].setItemChecked(i, true);
                    lvMenuCategory[0].setSelection(i);
                }
            }

            mMenuSednListAdapter[0].setToRight(lvVODItems);
            mVODDataAdapter.setToLeft(lvMenuCategory[0]);
            mVODDataAdapter.setItems(mLocalDBManager.getVODList(item.mMenuID[1]));
            for(int i = 0; i < mMenuSednListAdapter[1].size(); i++) {
                ListviewBaseItem menuItem = mMenuSednListAdapter[1].getItem(i);
                if(menuItem.getID().equals(item.mMenuID[1])) {
                    lvMenuCategory[1].setItemChecked(i, true);
                    lvMenuCategory[1].setSelection(i);
                }
            }
        } else {
            // 1 depth
            lvMenuCategory[1].setVisibility(View.INVISIBLE);
            mMenuSednListAdapter[0].setToRight(lvVODItems);
            mVODDataAdapter.setToLeft(lvMenuCategory[0]);
            mVODDataAdapter.setItems(mLocalDBManager.getVODList(item.mMenuID[0]));
            for(int i = 0; i < mMenuSednListAdapter[0].size(); i++) {
                ListviewBaseItem menuItem = mMenuSednListAdapter[0].getItem(i);
                if(menuItem.getID().equals(item.mMenuID[0])) {
                    lvMenuCategory[0].setItemChecked(i, true);
                    lvMenuCategory[0].setSelection(i);
                }
            }
        }

        for(int i = 0; i < mVODDataAdapter.size(); i++) {
            ListviewBaseItem vodItem = mVODDataAdapter.getItem(i);
            LogUtil.d("vod item - " + vodItem.getID() + " : " + item.getID());
            if(vodItem.getID().equals(item.getID())) {
                lvVODItems.setItemChecked(i, true);
                lvVODItems.requestFocus();
                lvVODItems.setSelection(i);
            }
        }
        mVODState = VOD_STATE_VODList;
        quickMenuFocused = false;
    }

    private void selectVOD1Depth(String index, boolean setFocus) {
        LogUtil.d("selectVOD1Depth - " + index + ", " + setFocus);
        lvMenuCategory[1].setVisibility(View.INVISIBLE);
        lvVODItems.setVisibility(View.INVISIBLE);
        vVODContentPreview.setVisibility(View.INVISIBLE);
        vVODContentInfo.setVisibility(View.INVISIBLE);
        mVODState = VOD_STATE_MenuCategory;
        mVODMenuDepth = 1;

        ArrayList<ListviewBaseItem> subMenu = mLocalDBManager.getSubMenu(index);
        if (subMenu.size() > 0) {
            // 먼저 하위메뉴가 있는지 체크
            lvMenuCategory[1].setVisibility(View.VISIBLE);
            mMenuSednListAdapter[0].setToRight(lvMenuCategory[1]);

            mMenuSednListAdapter[1].setItems(subMenu);
            mVODDataAdapter.setItems(new ArrayList<ListviewBaseItem>());
            if(setFocus) {
                mVODState = VOD_STATE_MenuCategory;
                mVODMenuDepth = 2;
                lvMenuCategory[1].requestFocus();
            }
            for (int i = 0; i < subMenu.size(); i++)
                lvMenuCategory[1].setItemChecked(i, false);
        } else {
            // 없으면 하위 VOD 존재하는지 체크
            ArrayList<ListviewBaseItem> vodList = mLocalDBManager.getVODList(index);
            if(vodList.size() > 0) {
                lvVODItems.setVisibility(View.VISIBLE);
                PercentRelativeLayout.LayoutParams params = (PercentRelativeLayout.LayoutParams) lvVODItems.getLayoutParams();
                PercentLayoutHelper.PercentLayoutInfo info = params.getPercentLayoutInfo();
                info.leftMarginPercent = leftPos[mVODMenuDepth -1];
                LogUtil.d("mVOD, leftMarginPercent - " + mVODMenuDepth + ", " + info.leftMarginPercent + ", " + lvVODItems.getLeft());

                mMenuSednListAdapter[0].setToRight(lvVODItems);
                mVODDataAdapter.setToLeft(lvMenuCategory[0]);

                mVODDataAdapter.setItems(vodList);

                if(setFocus) {
                    mVODState = VOD_STATE_VODList;
                    lvVODItems.requestFocus();
                    lvVODItems.setSelection(0);
                }

                for (int i = 0; i < vodList.size(); i++)
                    lvVODItems.setItemChecked(i, false);
            }
        }
    }
    private void initLive() {
        tvLiveTodayStr = (TextView)findViewById(R.id.tvLiveTodayStr);
        tvLiveTodayStr.setText(Utils.getDayOfWeek());


        layoutTimeLine = (RelativeLayout)findViewById(R.id.layoutTimeLine);

        svLiveChannel = (ScrollView)findViewById(R.id.svLiveChannel);
        layoutLiveChannel = (LinearLayout)findViewById(R.id.layoutLiveChannel);

        svLiveBroadcast = (ScrollView)findViewById(R.id.svLiveBroadcast);
        hsvLiveBroadcast = (HorizontalScrollView) findViewById(R.id.hsvLiveBroadcast);
        layoutLiveBroadcast = (LinearLayout)findViewById(R.id.layoutLiveBroadcast);

        ivCurTimeBar = (ImageView)findViewById(R.id.ivCurTimeBar);

        // timeline 그리기
        for(int hour = 0; hour < 24; hour++) {
            ImageView ivBar = new ImageView(this);
            ivBar.setImageResource(R.drawable.time_bar);
            RelativeLayout.LayoutParams barParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            barParams.setMargins(hour * 60 * 7404 / 1440, 0, 0, 0);
            ivBar.setLayoutParams(barParams);
            layoutTimeLine.addView(ivBar);

            TextView tvHour = new TextView(this);
            tvHour.setTextColor(Color.WHITE);
            tvHour.setText(Utils.twoDigit(""+hour) + ":00");
            tvHour.setTextSize(TypedValue.COMPLEX_UNIT_PX, 24);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 30);
            params.setMargins(hour * 60 * 7404 / 1440 + 5, 0, 0, 0);
            tvHour.setLayoutParams(params);
            layoutTimeLine.addView(tvHour);
        }

        // 스크롤 뷰 싱크
        svLiveChannel.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if(mLiveState == LIVE_STATE_Channel) {
                    int scrollX = svLiveChannel.getScrollX();
                    int scrollY = svLiveChannel.getScrollY();

                    LogUtil.d("channel scroll to - " + scrollX + ", " + scrollY);
                    svLiveBroadcast.scrollTo(scrollX, scrollY);
                }
            }
        });
        svLiveBroadcast.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if(mLiveState == LIVE_STATE_Schedule) {
                    int scrollX = svLiveBroadcast.getScrollX();
                    int scrollY = svLiveBroadcast.getScrollY();

                    LogUtil.d("schedule scroll to - " + scrollX + ", " + scrollY);
                    svLiveChannel.scrollTo(scrollX, scrollY);
                }
            }
        });

        // 각종 리스너
        liveChannelClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup layoutView = (ViewGroup)v.getParent();
                int index = layoutView.indexOfChild(v);
                focusFirstSchedule(index);
            }
        };
        liveChannelKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    ViewGroup layoutView = (ViewGroup)v.getParent();
                    int numChannel = layoutView.getChildCount();
                    int index = layoutView.indexOfChild(v);
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if(index == 0) {
                            return true;
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if(index == mLiveChannelNum - 1) {
                            if( layoutLiveBottomList.getChildCount() > 0) {
                                layoutLiveBottomList.getChildAt(0).requestFocus();
                            }
                            return true;
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        return true;
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        focusFirstSchedule(index);
                        return true;
                    }
                }
                return false;
            }
        };
        liveScheduleClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScheduleItem curItem = (ScheduleItem)v.getTag();

                ViewGroup layoutSchedule = (ViewGroup) v.getParent();
                ViewGroup layoutChannel = (ViewGroup) layoutSchedule.getParent();
                int channel_index = layoutChannel.indexOfChild(layoutSchedule);

                int y_pos = channel_index * 155 + 210;
                int y_offset = svLiveChannel.getScrollY();
                int y_display = y_pos - y_offset;
                if(y_display < 0) y_display = 0;
                if(y_display > 488) y_display = 488;

                int x_start = curItem.start_time *  7404 / 1440 + 500;
                int x_offset = hsvLiveBroadcast.getScrollX();
                int x_display = x_start - x_offset;
                if(x_display > 1103) x_display -= 720;  // 화면의 절반 이상인 경우 앞으로 이동
                if(x_display > 897) x_display = 897;    // EPG 영역 바깥으로 나가지 않도록

                LogUtil.d("live channel offset - " + y_offset);

                mLiveState = LIVE_STATE_Preview;
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)layoutSchedulePreview.getLayoutParams();
                params.setMargins(x_display, y_display, 0, 0);
                layoutSchedulePreview.setLayoutParams(params);
                layoutSchedulePreview.setVisibility(View.VISIBLE);



                if (!curItem.image_url.isEmpty()) {
                    ArrayList<ImageView> scheduleImageList = new ArrayList<>();
                    ArrayList<String> scheduleImageURLList = new ArrayList<>();

                    scheduleImageList.add(ivLiveScheduleThumbnail);
                    scheduleImageURLList.add(curItem.image_url);

                    new DownloadImageViewListTask(scheduleImageList).execute(scheduleImageURLList);
                }

                tvLiveScheduleName.setText(curItem.name);
                tvLiveSchedulePreviewDate.setText(curItem.dateStr);
                tvLiveSchedulePreviewTime.setText(curItem.playTimeStr);
                tvLiveScheduleDuration.setText(curItem.durationStr);
                tvLiveScheduleGroup.setText(curItem.target);
                tvLiveScheduleDesc.setText(curItem.desc);
            }
        };
        liveScheduleKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(mLiveState == LIVE_STATE_Schedule) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        ViewGroup layoutSchedule = (ViewGroup) v.getParent();
                        ViewGroup layoutChannel = (ViewGroup) layoutSchedule.getParent();
                        int numSchedule = layoutSchedule.getChildCount();
                        int numChannel = layoutChannel.getChildCount();
                        int schedule_index = layoutSchedule.indexOfChild(v);
                        int channel_index = layoutChannel.indexOfChild(layoutSchedule);
                        int first_schedule_channel = -1;
                        int last_schedule_channel = -1;
                        for (int i = 0; i < numChannel; i++) {
                            ViewGroup curChannel = (ViewGroup) layoutChannel.getChildAt(i);
                            if (curChannel.getChildCount() > 1 || curChannel.getChildAt(0).isFocusable()) {
                                // 편성 스케줄이 있는 채널
                                if (first_schedule_channel == -1)
                                    first_schedule_channel = i;
                                last_schedule_channel = i;
                            }
                        }
                        LogUtil.d("schedule keydown " + schedule_index + "/" + numSchedule + ", " + channel_index + "/" + numChannel);
                        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            if (channel_index == first_schedule_channel) {
                                return true;
                            }
                        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            if (channel_index == last_schedule_channel) {
                                return true;
                            }
                        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            // move to the previous focusable
                            while (schedule_index > 0) {
                                schedule_index--;
                                if (layoutSchedule.getChildAt(schedule_index).isFocusable()) {
                                    layoutSchedule.getChildAt(schedule_index).requestFocus();
                                    break;
                                }
                            }
                            return true;
                        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            // move to the next focusable
                            while (schedule_index < numSchedule - 1) {
                                schedule_index++;
                                if (layoutSchedule.getChildAt(schedule_index).isFocusable()) {
                                    layoutSchedule.getChildAt(schedule_index).requestFocus();
                                    break;
                                }
                            }
                            return true;
                        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                            mLiveState = LIVE_STATE_Channel;
                            layoutLiveChannel.getChildAt(channel_index).requestFocus();
                            return true;
                        }
                    }
                    return false;
                }

                // back키와 escape키만 올려보낸다.
                if(!(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE))
                    return true;

                return false;
            }
        };

        // 개인방송 키입력 리스너
        liveBottomKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    ViewGroup layoutView = (ViewGroup)v.getParent();
                    int castCnt = layoutView.getChildCount(); // 개인방송 개수
                    int index = layoutView.indexOfChild(v);   // 현재 인덱스

                    // 위로 누르면 채널번호영역으로 포커스 이동
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        int channelCnt = layoutLiveChannel.getChildCount();
                        if(channelCnt > 0) {
                            layoutLiveChannel.getChildAt(channelCnt-1).requestFocus(); // 마지막 채널로 이동
                            return true;
                        }
                    }
                    else if(KeyEvent.KEYCODE_DPAD_RIGHT == keyCode) {
                        if(castCnt-1 == index) {
                            layoutView.getChildAt(0).requestFocus(); // 마지막에서 우측이동시키면 첫 번째 표시
                            return true;
                        }
                    }
                    else if(KeyEvent.KEYCODE_DPAD_LEFT == keyCode) {
                        if(0 == index) {
                            layoutView.getChildAt(castCnt-1).requestFocus(); // 첫 번째에서 좌측이동시키면 마지막 개인방송 아이템으로 이동
                            return true;
                        }
                    }
                }
                return false;
            }
        };

        // 개인방송 클릭 리스너
        liveBottomClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewGroup layoutView = (ViewGroup)v.getParent();
                int castCnt = layoutView.getChildCount(); // 개인방송 개수
                int index = layoutView.indexOfChild(v);   // 현재 인덱스

                PersonalBroadcastResponse br = mPersonalBR.get(index);

                Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
                intent.putExtra("PLAY_MODE", PlayerActivity.PLAY_STB_LIVE);
                intent.putExtra("TITLE", br.title);
                //intent.putExtra("URI", "rtmp://192.168.100.2:1935/live/myStream");
                intent.putExtra("URI", br.url);
                //intent.putExtra("URI", "http://192.168.100.2:1935/live/myStream/playlist.m3u8"); // 재생은 되지만 딜레이 심함
                intent.putExtra("DURATION", 60*1000);

                sendBroadcast(new Intent(SednApplication.ACTION_SEDN_STOP_PLAYER));
                startBroadcast(intent);

            }
        };

        // Preview
        layoutSchedulePreview = (ViewGroup)findViewById(R.id.layoutSchedulePreview);
        tvLiveScheduleName = (TextView)layoutSchedulePreview.findViewById(R.id.tvLiveScheduleName);
        ivLiveScheduleThumbnail = (ImageView) layoutSchedulePreview.findViewById(R.id.ivLiveScheduleThumbnail);
        tvLiveSchedulePreviewDate = (TextView)layoutSchedulePreview.findViewById(R.id.tvLiveSchedulePreviewDate);
        tvLiveSchedulePreviewTime = (TextView)layoutSchedulePreview.findViewById(R.id.tvLiveSchedulePreviewTime);
        tvLiveScheduleDuration = (TextView)layoutSchedulePreview.findViewById(R.id.tvLiveScheduleDuration);
        tvLiveScheduleGroup = (TextView)layoutSchedulePreview.findViewById(R.id.tvLiveScheduleGroup);
        tvLiveScheduleDesc = (TextView)layoutSchedulePreview.findViewById(R.id.tvLiveScheduleDesc);

        layoutSchedulePreview.setVisibility(View.INVISIBLE);

        // 하단 영역
        svLiveBottomList = (HorizontalScrollView)findViewById(R.id.svLiveBottomList);
        layoutLiveBottomList = (LinearLayout)findViewById(R.id.layoutLiveBottomList);
    }

    private void focusFirstSchedule(int channel_index) {
        ViewGroup scheduleLayout = (ViewGroup)layoutLiveBroadcast.getChildAt(channel_index);
        LogUtil.d("focusFirstSchedule " + channel_index + ", " + scheduleLayout.getChildCount());
        // select the first focusable
        for(int i = 0; i < scheduleLayout.getChildCount(); i++) {
            View curSchedule = scheduleLayout.getChildAt(i);
            if(curSchedule.isFocusable()) {
                mLiveState = LIVE_STATE_Schedule;
                curSchedule.requestFocus();
                break;
            }
        }
    }

    private void initMypage() {
        mMypageState = MYPAGE_STATE_Submenu;

        vBookmarkTab = findViewById(R.id.layoutBookmarkList);
        vDownloadTab = findViewById(R.id.layoutDownloadList);
        View.OnClickListener mypageTabClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                switch(id) {
                    case R.id.layoutBookmarkList:
                        mMypageState = MYPAGE_STATE_VODList;
                        mMypageMode = MYPAGE_MODE_Bookmark;
                        lvMypageItems.requestFocus();
                        break;
                    case R.id.layoutDownloadList:
                        mMypageState = MYPAGE_STATE_VODList;
                        mMypageMode = MYPAGE_MODE_Download;
                        lvMypageItems.requestFocus();
                        break;
                }
            }
        };
        View.OnFocusChangeListener mypageTabFocusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    int id = v.getId();
                    switch (id) {
                        case R.id.layoutBookmarkList:
                            setMyPageState(MYPAGE_MODE_Bookmark);
                            break;
                        case R.id.layoutDownloadList:
                            setMyPageState(MYPAGE_MODE_Download);
                    }
                }
            }
        };
        View.OnKeyListener mypageTabKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction() == KeyEvent.ACTION_DOWN) {
                    if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if(v.getId() == R.id.layoutDownloadList) {
                            return true;
                        }
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        return true;
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return true;
                    }

                }
                return false;
            }
        };
        vBookmarkTab.setOnFocusChangeListener(mypageTabFocusListener);
        vBookmarkTab.setOnClickListener(mypageTabClickListener);
        vBookmarkTab.setOnKeyListener(mypageTabKeyListener);
        vDownloadTab.setOnFocusChangeListener(mypageTabFocusListener);
        vDownloadTab.setOnClickListener(mypageTabClickListener);
        vDownloadTab.setOnKeyListener(mypageTabKeyListener);

        lvMypageItems = (ListView) findViewById(R.id.lvMypageItems);
        SednItemList.OnSednItemSelectListener mypageItemSelectListener = new SednItemList.OnSednItemSelectListener() {
            @Override
            public void onSednItemSelect(ListviewBaseItem item, int index, float topPercent, float heightPercent, boolean setFocus) {
                VODItem curItem = (VODItem)mMypageItemAdapter.getSelectedItem();

                ivMypageThumbnail.setBackgroundResource(0);
                ivMypageThumbnail.setImageBitmap(null);
                //new ThumnailLoader(ivMypageThumbnail).execute(curItem.getThumbnailPath());
                Picasso.with(getApplicationContext()).load(curItem.getThumbnailPath()).into(ivMypageThumbnail);

                tvMypageContentDate.setText(curItem.mRegisterDT);
                tvMypageContentTime.setText(curItem.mPlayTime);
                tvMypageContentHit.setText(String.format("%,d", curItem.mHit));
                tvMypageContentCate.setText(curItem.mCategory);
                tvMypageContentTitle.setText(curItem.getName());

                if(mLocalDBManager.isBookmarked(item.getID()))
                    vMypageContentPreviewButton[1].setImageResource(R.drawable.icon_bookmark_minus);
                else
                    vMypageContentPreviewButton[1].setImageResource(R.drawable.icon_bookmark);
                if(!mLocalDBManager.isDownloaded(item.getID()))
                    vMypageContentPreviewButton[2].setImageResource(R.drawable.icon_down);
                else
                    vMypageContentPreviewButton[2].setImageResource(R.drawable.icon_download_trash);
                vMypageContentPreview.setVisibility(View.VISIBLE);
                if(setFocus) {
                    vMypageContentPreview.requestFocus();
                    mMypageState = MYPAGE_STATE_Preview;
                }
            }
        };

        // 아래 두 줄의 순서 바뀌면 안됨.
        vMypageContentPreview = findViewById(R.id.mypage_content_preview);
        mMypageItemAdapter = SednItemList.BuildList(this, lvMypageItems, (ImageView) findViewById(R.id.ivMyPageItemUpArrow), (ImageView) findViewById(R.id.ivMyPageItemDownArrow),
                new ArrayList<ListviewBaseItem>(), mypageItemSelectListener, vBookmarkTab, null, null, vMypageContentPreview,
                R.drawable.selector_vod_item_category, 7, 2.9411f, Gravity.LEFT | Gravity.CENTER_VERTICAL, 30f / 935f);

        View.OnClickListener mypagePreviewClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                VODItem selectedItem = (VODItem)mMypageItemAdapter.getSelectedItem();
                switch(id) {
                    case R.id.ivPreview_play:
                        playVOD(PlayerActivity.PLAY_STB_VOD, selectedItem);
                        break;
                    case R.id.ivPreview_bookmark:
                        if(mMypageMode == MYPAGE_MODE_Bookmark) {
                            toggleBookmark(selectedItem.getID());
                            mMypageItemAdapter.setItems(mLocalDBManager.getBookmarkedVOD());
                            if(mMypageItemAdapter.size() == 0) {
                                vBookmarkTab.requestFocus();
                                mMypageState = MYPAGE_STATE_Submenu;
                                vNoBookmark.setVisibility(View.VISIBLE);
                            } else {
                                vNoBookmark.setVisibility(View.GONE);
                                lvMypageItems.requestFocus();
                                mMypageState = MYPAGE_STATE_VODList;
                                mMypageItemAdapter.setToTop(vBookmarkTab);
                            }
                            vNoDownload.setVisibility(View.GONE);
                            vMypageContentPreview.setVisibility(View.INVISIBLE);
                            vMypageContentInfo.setVisibility(View.INVISIBLE);
                        } else {
                            if(toggleBookmark(selectedItem.getID())) {
                                vMypageContentPreviewButton[1].setImageResource(R.drawable.icon_bookmark_minus);
                            } else {
                                vMypageContentPreviewButton[1].setImageResource(R.drawable.icon_bookmark);
                            }
                            mMypageItemAdapter.notifyDataSetChanged();
                        }
                        break;
                    case R.id.ivPreview_download:
                        if(mMypageMode == MYPAGE_MODE_Bookmark && !mLocalDBManager.isDownloaded(selectedItem.getID())) {
                            requestDownload(selectedItem);
                        } else {
                            LayoutInflater inflater = getLayoutInflater();
                            final View dialogView = inflater.inflate(R.layout.dialog_delete, null);

                            AlertDialog.Builder alert_confirm = new AlertDialog.Builder(MainActivity.this);
                            alert_confirm.setView(dialogView);
                            AlertDialog alert = alert_confirm.create();

                            TextView confirm = (TextView) dialogView.findViewById(R.id.tvDeleteConfirm);
                            TextView cancel = (TextView) dialogView.findViewById(R.id.tvDeleteCancel);
                            confirm.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    alert.dismiss();
                                    if (mMypageMode == MYPAGE_MODE_Download) {
                                        removeFile(selectedItem.getID());
                                        mMypageItemAdapter.setItems(mLocalDBManager.getDownloadedVOD());
                                        if (mMypageItemAdapter.size() == 0) {
                                            vDownloadTab.requestFocus();
                                            mMypageState = MYPAGE_STATE_Submenu;
                                            vNoDownload.setVisibility(View.VISIBLE);
                                        } else {
                                            vNoDownload.setVisibility(View.GONE);
                                            lvMypageItems.requestFocus();
                                            mMypageState = MYPAGE_STATE_VODList;
                                            mMypageItemAdapter.setToTop(vDownloadTab);
                                        }
                                        vNoBookmark.setVisibility(View.GONE);
                                        vMypageContentPreview.setVisibility(View.INVISIBLE);
                                        vMypageContentInfo.setVisibility(View.INVISIBLE);
                                    } else {
                                        if (mLocalDBManager.isDownloaded(selectedItem.getID())) {
                                            removeFile(selectedItem.getID());
                                            vMypageContentPreviewButton[2].setImageResource(R.drawable.icon_down);

                                        }
                                        mMypageItemAdapter.notifyDataSetChanged();
                                    }
                                }
                            });
                            cancel.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    alert.dismiss();
                                }
                            });

                            alert.show();
                            alert.getWindow().setLayout(570, 250);
                        }
                        break;
                }
            }
        };
        View.OnKeyListener mypagePreviewKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                int id = v.getId();
                if(event.getAction() == KeyEvent.ACTION_DOWN) {
                    // Right / Left 키 다시 막음
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return true;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        return true;
                    }
                    // 최상위에서의 UP과 최하위에서의 DOWN 처리
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP && id == previewIDs[0]) {
                        vMypageContentPreviewButton[previewIDs.length-1].requestFocus();
                        return true;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && id == previewIDs[previewIDs.length - 1]) {
                        vMypageContentPreviewButton[0].requestFocus();
                        return true;
                    }
                }
                return false;
            }
        };
        vMypageContentPreviewButton = new ImageView[previewIDs.length];
        for (int i = 0; i < previewIDs.length; i++) {
            vMypageContentPreviewButton[i] = (ImageView)vMypageContentPreview.findViewById(previewIDs[i]);
            vMypageContentPreviewButton[i].setOnClickListener(mypagePreviewClickListener);
            vMypageContentPreviewButton[i].setOnKeyListener(mypagePreviewKeyListener);
        }
        vMypageContentPreviewButton[previewIDs.length-1].setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    setContentInfoView(vMypageContentInfo, (VODItem)mMypageItemAdapter.getSelectedItem());
                    vMypageContentInfo.setVisibility(View.VISIBLE);
                } else
                    vMypageContentInfo.setVisibility(View.INVISIBLE);
            }
        });
        ivMypageThumbnail = (ImageView)vMypageContentPreview.findViewById(R.id.ivPreview_thumbnail);
        tvMypageContentDate = (TextView) vMypageContentPreview.findViewById(R.id.tvVODDate);
        tvMypageContentTime = (TextView) vMypageContentPreview.findViewById(R.id.tvVODTime);
        tvMypageContentHit = (TextView) vMypageContentPreview.findViewById(R.id.tvVODHit);
        tvMypageContentCate = (TextView) vMypageContentPreview.findViewById(R.id.tvVODCate);
        tvMypageContentTitle = (TextView) vMypageContentPreview.findViewById(R.id.tvVODTitle);

        vMypageContentInfo = findViewById(R.id.mypage_content_info);

        vNoBookmark = findViewById(R.id.mypage_no_bookmark);
        vNoDownload = findViewById(R.id.mypage_no_download);
    }

    private void setMyPageState(int state) {
        lvMypageItems.setVisibility(View.VISIBLE);
        if(state == MYPAGE_MODE_Bookmark) {

            vBookmarkTab.setSelected(true);
            vDownloadTab.setSelected(false);

            mMypageItemAdapter.setItems(mLocalDBManager.getBookmarkedVOD());
            vNoDownload.setVisibility(View.GONE);
            vMypageContentPreview.setVisibility(View.INVISIBLE);
            vMypageContentInfo.setVisibility(View.INVISIBLE);

            if(mMypageItemAdapter.size() == 0) {
                vNoBookmark.setVisibility(View.VISIBLE);
            } else {
                vNoBookmark.setVisibility(View.GONE);
                mMypageItemAdapter.setToTop(vBookmarkTab);
            }
        } else {

            vBookmarkTab.setSelected(false);
            vDownloadTab.setSelected(true);

            mMypageItemAdapter.setItems(mLocalDBManager.getDownloadedVOD());
            vNoBookmark.setVisibility(View.GONE);
            vMypageContentPreview.setVisibility(View.INVISIBLE);
            vMypageContentInfo.setVisibility(View.INVISIBLE);

            if(mMypageItemAdapter.size() == 0) {
                vNoDownload.setVisibility(View.VISIBLE);
            } else {
                vNoDownload.setVisibility(View.GONE);
                mMypageItemAdapter.setToTop(vDownloadTab);
            }
        }
    }

    private void initSearch() {
        mSearchState = SEARCH_STATE_InputText;

        etSearchText = (EditText) findViewById(R.id.etSearchText);
        etSearchText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction() == KeyEvent.ACTION_DOWN) {
                    if(keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        return true;
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return true;
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                        tvGoSearch.requestFocus();
                        return true;
                    }

                }
                return false;
            }
        });

        tvGoSearch = (TextView) findViewById(R.id.tvGoSearch);
        tvGoSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearch();
            }
        });
        tvGoSearch.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction() == KeyEvent.ACTION_DOWN) {
                    if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return true;
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        return true;
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return true;
                    }

                }
                return false;
            }
        });

        lvSearchResult = (ListView) findViewById(R.id.lvSearchResult);
        SednItemList.OnSednItemSelectListener searchResultContentSelectListener = new SednItemList.OnSednItemSelectListener() {
            @Override
            public void onSednItemSelect(ListviewBaseItem item, int index, float topPercent, float heightPercent, boolean setFocus) {
                VODItem curItem = (VODItem)mSearchResultAdapter.getSelectedItem();
                ivSearchContentThumbnail.setBackgroundResource(0);
                ivSearchContentThumbnail.setImageBitmap(null);
                //new ThumnailLoader(ivSearchContentThumbnail).execute(curItem.getThumbnailPath());
                Picasso.with(getApplicationContext()).load(curItem.getThumbnailPath()).into(ivSearchContentThumbnail);

                tvSearchContentDate.setText(curItem.mRegisterDT);
                tvSearchContentTime.setText(curItem.mPlayTime);
                tvSearchContentHit.setText(String.format("%,d", curItem.mHit));
                tvSearchContentCate.setText(curItem.mCategory);
                tvSearchContentTitle.setText(curItem.getName());

                vSearchContentPreviewButton[1].setImageResource(R.drawable.icon_bookmark);
                vSearchContentPreviewButton[2].setImageResource(R.drawable.icon_down);
                vSearchContentPreview.setVisibility(View.VISIBLE);
                if(setFocus) {
                    vSearchContentPreview.requestFocus();
                    mSearchState = SEARCH_STATE_Preview;
                }
            }
        };
        mSearchResultAdapter = SednItemList.BuildList(this, lvSearchResult, (ImageView) findViewById(R.id.ivSearchUpArrow), (ImageView) findViewById(R.id.ivSearchDownArrow),
                new ArrayList<ListviewBaseItem>(), searchResultContentSelectListener, etSearchText, null, null, null,
                R.drawable.selector_vod_item_category, 7, 2.9411f, Gravity.LEFT | Gravity.CENTER_VERTICAL, 30f / 935f);

        vSearchContentPreview = findViewById(R.id.search_content_preview);
        View.OnClickListener searchPreviewClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                VODItem selectedItem = (VODItem)mSearchResultAdapter.getSelectedItem();
                switch(id) {
                    case R.id.ivPreview_play:
                        playVOD(PlayerActivity.PLAY_STB_VOD, selectedItem);
                        break;
                    case R.id.ivPreview_bookmark:
                        setBookmark(selectedItem.getID());
                        mSearchResultAdapter.notifyDataSetChanged();
                        break;
                    case R.id.ivPreview_download:
                        requestDownload(selectedItem);
                        break;
                }
            }
        };
        View.OnKeyListener searchPreviewKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                int id = v.getId();
                if(event.getAction() == KeyEvent.ACTION_DOWN) {
                    // Right / Left 키 다시 막음
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return true;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        return true;
                    }
                    // 최상위에서의 UP과 최하위에서의 DOWN 처리
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP && id == previewIDs[0]) {
                        vSearchContentPreviewButton[previewIDs.length-1].requestFocus();
                        return true;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && id == previewIDs[previewIDs.length - 1]) {
                        vSearchContentPreviewButton[0].requestFocus();
                        return true;
                    }
                }
                return false;
            }
        };
        vSearchContentPreviewButton = new ImageView[previewIDs.length];
        for (int i = 0; i < previewIDs.length; i++) {
            vSearchContentPreviewButton[i] = (ImageView)vSearchContentPreview.findViewById(previewIDs[i]);
            vSearchContentPreviewButton[i].setOnClickListener(searchPreviewClickListener);
            vSearchContentPreviewButton[i].setOnKeyListener(searchPreviewKeyListener);
        }
        vSearchContentPreviewButton[previewIDs.length-1].setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    setContentInfoView(vSearchContentInfo, (VODItem)mSearchResultAdapter.getSelectedItem());
                    vSearchContentInfo.setVisibility(View.VISIBLE);
                } else
                    vSearchContentInfo.setVisibility(View.INVISIBLE);
            }
        });

        ivSearchContentThumbnail = (ImageView)vSearchContentPreview.findViewById(R.id.ivPreview_thumbnail);
        tvSearchContentDate = (TextView) vSearchContentPreview.findViewById(R.id.tvVODDate);
        tvSearchContentTime = (TextView) vSearchContentPreview.findViewById(R.id.tvVODTime);
        tvSearchContentHit = (TextView) vSearchContentPreview.findViewById(R.id.tvVODHit);
        tvSearchContentCate = (TextView) vSearchContentPreview.findViewById(R.id.tvVODCate);
        tvSearchContentTitle = (TextView) vSearchContentPreview.findViewById(R.id.tvVODTitle);

        vSearchContentInfo = findViewById(R.id.search_content_info);
    }

    private void doSearch() {
        String searchWord = etSearchText.getText().toString();
        if(searchWord != null && !searchWord.isEmpty()) {
            vSearchContentPreview.setVisibility(View.INVISIBLE);
            vSearchContentInfo.setVisibility(View.INVISIBLE);

            ArrayList<ListviewBaseItem> searchResult = mLocalDBManager.searchVOD(searchWord);
            mSearchResultAdapter.setItems(searchResult);
            if (searchResult.size() > 0) {
                mSearchState = SEARCH_STATE_VODList;
                lvSearchResult.setVisibility(View.VISIBLE);
                lvSearchResult.requestFocus();
            } else {
                showToast(getResources().getString(R.string.msg_no_search_result));
            }
        } else {
            showToast(getResources().getString(R.string.msg_no_search_word));
        }
    }

    private void initSetup() {
        // 셋업 메뉴 초기화
        setupItemList = new TextView[setupItemIDs.length];
        setupLeftMenu = findViewById(R.id.layoutSetupLeftMenu);

        mSelectedSetupItemIdx = SETUP_INFORMATION;

        for(int i = 0; i < setupItemIDs.length; i++) {
            setupItemList[i] = (TextView)findViewById(setupItemIDs[i]);
            setupItemList[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int selectedIdx = -1;
                    int selectedID = v.getId();
                    for (int i = 0; i < setupItemIDs.length; i++) {
                        if (setupItemIDs[i] == selectedID)
                            selectedIdx = i;
                    }
                    if(selectedIdx != 0 ) {
                        setupLeftItemUpper.requestFocus();
                        mSetupState = SETUP_STATE_LeftMenu;
                    }
                }
            });
            setupItemList[i].setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    int id = v.getId();
                    if(event.getAction() == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            return true;
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && id == setupItemIDs[0]) {
                            setupItemList[setupItemIDs.length-1].requestFocus();
                            return true;
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && id == setupItemIDs[setupItemIDs.length-1]) {
                            setupItemList[0].requestFocus();
                            return true;
                        }
                    }
                    return false;
                }
            });
            setupItemList[i].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        int selectedIdx = -1;
                        int selectedID = v.getId();
                        for (int i = 0; i < setupItemIDs.length; i++) {
                            if (setupItemIDs[i] == selectedID)
                                selectedIdx = i;
                        }
                        switch (selectedID) {
                            case R.id.tvSetupInfomation:
                                setSTBInfo();
                                break;
                            case R.id.tvSetupNetwork:
                                curLeftMenu = mSetupNetworkInfo;
                                break;
                            case R.id.tvSetupDisplay:
                                curLeftMenu = mSetupDisplayInfo;
                                break;
                            case R.id.tvSetupSystem:
                                curLeftMenu = mSetupSystemInfo;
                                break;
                        }
                        curLeftMenu = setupLeftMenuInfos[selectedIdx];

                        if(mSelectedSetupItemIdx != selectedIdx) {
                            switchSetupMenu(selectedIdx);
                        }
                    }
                }
            });
            layoutSetupInformation = findViewById(R.id.layoutSetupInformation);
        }

        // Left Menu
        setupArrowUp = findViewById(R.id.ivSetupArrowUp);
        setupArrowDown = findViewById(R.id.ivSetupArrowDown);
        setupLeftItemUpper =  findViewById(R.id.setupLeftItemUpper);
        setupLeftItemLower = findViewById(R.id.setupLeftItemLower);
        setupLeftItemUpperText = (TextView)setupLeftItemUpper.findViewById(R.id.tvItemName);
        setupLeftItemUpperImage = (ImageView)setupLeftItemUpper.findViewById(R.id.ivItemImage);
        setupLeftItemLowerText = (TextView)setupLeftItemLower.findViewById(R.id.tvItemName);
        setupLeftItemLowerImage = (ImageView)setupLeftItemLower.findViewById(R.id.ivItemImage);
        View.OnKeyListener setupLeftKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction() == KeyEvent.ACTION_DOWN) {
                    if(keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if(v.getId() == R.id.setupLeftItemUpper) {
                            if(curLeftMenu.firstVisible > 0) {
                                curLeftMenu.firstVisible--;
                            } else if(curLeftMenu.firstVisible == 0) {
                                curLeftMenu.firstVisible = curLeftMenu.length - 2;
                                setupLeftItemLower.requestFocus();
                            }
                            redrawSetupLeftMenu();
                            return true;
                        }
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if(v.getId() == R.id.setupLeftItemLower) {
                            if(curLeftMenu.firstVisible < curLeftMenu.length - 2) {
                                curLeftMenu.firstVisible++;
                            } else if(curLeftMenu.firstVisible == curLeftMenu.length - 2) {
                                curLeftMenu.firstVisible = 0;
                                setupLeftItemUpper.requestFocus();
                            }
                            redrawSetupLeftMenu();
                            return true;
                        }
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return true;
                    }

                }
                return false;
            }
        };
        setupLeftItemUpper.setOnKeyListener(setupLeftKeyListener);
        setupLeftItemLower.setOnKeyListener(setupLeftKeyListener);
        setupLeftItemUpper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupLeftItemLower.setSelected(false);
                v.setSelected(true);
                curLeftMenu.hideView();
                curLeftMenu.selected = curLeftMenu.firstVisible;
                curLeftMenu.showView();
                if(curLeftMenu.firstFocusing[curLeftMenu.selected] != null)
                    curLeftMenu.firstFocusing[curLeftMenu.selected].requestFocus();
                mSetupState = SETUP_STATE_Detail;
                isSetupChanged = false;
            }
        });
        setupLeftItemLower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupLeftItemUpper.setSelected(false);
                v.setSelected(true);
                curLeftMenu.hideView();
                curLeftMenu.selected = curLeftMenu.firstVisible + 1;
                curLeftMenu.showView();
                if(curLeftMenu.firstFocusing[curLeftMenu.selected] != null)
                    curLeftMenu.firstFocusing[curLeftMenu.selected].requestFocus();
                mSetupState = SETUP_STATE_Detail;
                isSetupChanged = false;
            }
        });

        // Information 메뉴
        tvSetupInfoModelName = (TextView)findViewById(R.id.tvSetupInfoModelNameValue);
        tvSetupInfoServiceURL = (TextView)findViewById(R.id.tvSetupInfoServiceURLValue);
        tvSetupInfoIPAddress = (TextView)findViewById(R.id.tvSetupInfoIPAddressValue);
        tvSetupInfoMACAddress = (TextView)findViewById(R.id.tvSetupInfoMACAddressValue);
        tvSetupInfoFWVersion = (TextView)findViewById(R.id.tvSetupInfoFWVersionValue);

        TextWatcher setupTextChangedListener = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isSetupChanged = true;
            }
        };

        View.OnClickListener setupCancelClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast(getResources().getString(R.string.msg_setup_cancel));
                curLeftMenu.showView();
                setupBackToLeftMenu();
            }
        };

        // Network
        mSetupNetworkInfo = new SetupLeftMenuInfo(setupNetworkMenuStr, setupNetworkMenuImg, setupNetworkMenuView);
        //-- Ethernet
        View.OnKeyListener setupDetailKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        int id = v.getNextFocusUpId();
                        if(id != -1) {
                            findViewById(id).requestFocus();
                        }
                        return true;
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        int id = v.getNextFocusDownId();
                        if(id != -1) {
                            findViewById(id).requestFocus();
                        }
                        return true;
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        int id = v.getNextFocusLeftId();
                        if(id != -1) {
                            findViewById(id).requestFocus();
                        }
                        return true;
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        int id = v.getNextFocusRightId();
                        if(id != -1) {
                            findViewById(id).requestFocus();
                        }
                        return true;
                    }
                    else if(KeyEvent.KEYCODE_ALT_LEFT == keyCode) { // T95max 에서 A/a 키 입력시 . 으로 변환한다. (2017.4.25 ghlee)
                        new Thread(new Runnable() {
                            public void run() {
                                new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_PERIOD);
                            }
                        }).start();
                    }
                }
                return false;
            }
        };
        tvSetupNetworkEthernetStatus = (TextView)findViewById(R.id.tvSetupNetworkEthernetStatus);
        tvSetupNetworkEthernetIPText = (TextView)findViewById(R.id.tvSetupNetworkEthernetIPAddress);
        tvSetupNetworkEthernetIPValue = (TextView)findViewById(R.id.tvSetupNetworkEthernetIPAddressValue);
        tvSetupNetworkEthernetAPText = (TextView)findViewById(R.id.tvSetupNetworkEthernetAPName);
        tvSetupNetworkEthernetAPValue = (TextView)findViewById(R.id.tvSetupNetworkEthernetAPNameValue);

        rbSetupEthernetDHCP = (RadioButton)findViewById(R.id.rbSetupNetworkEthernetDHCP);
        rbSetupEthernetDHCP.setOnKeyListener(setupDetailKeyListener);
        rbSetupEthernetStatic = (RadioButton)findViewById(R.id.rbSetupNetworkEthernetStatic);
        rbSetupEthernetStatic.setOnKeyListener(setupDetailKeyListener);
        etSetupNetworkInfoIPInput = (ASEditText)findViewById(R.id.etSetupNetworkInfoIPInput);
        etSetupNetworkInfoIPInput.setOnKeyListener(setupDetailKeyListener);

        etSetupNetworkInfoSubnetInput = (ASEditText)findViewById(R.id.etSetupNetworkInfoSubnetInput);
        etSetupNetworkInfoSubnetInput.setOnKeyListener(setupDetailKeyListener);
        etSetupNetworkInfoSubnetInput.addTextChangedListener(setupTextChangedListener);
        etSetupNetworkInfoGatewayInput = (ASEditText)findViewById(R.id.etSetupNetworkInfoGatewayInput);
        etSetupNetworkInfoGatewayInput.setOnKeyListener(setupDetailKeyListener);
        etSetupNetworkInfoGatewayInput.addTextChangedListener(setupTextChangedListener);
        etSetupNetworkInfoDNS1Input = (ASEditText)findViewById(R.id.etSetupNetworkInfoDNS1Input);
        etSetupNetworkInfoDNS1Input.setOnKeyListener(setupDetailKeyListener);
        etSetupNetworkInfoDNS1Input.addTextChangedListener(setupTextChangedListener);
        etSetupNetworkInfoDNS2Input = (ASEditText)findViewById(R.id.etSetupNetworkInfoDNS2Input);
        etSetupNetworkInfoDNS2Input.setOnKeyListener(setupDetailKeyListener);
        etSetupNetworkInfoDNS2Input.addTextChangedListener(setupTextChangedListener);
        etSetupNetworkInfoIPInput.addTextChangedListener(setupTextChangedListener);
        rbSetupEthernetDHCP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSetupChanged = true;
                setEthernetInputEnabled(false);
            }
        });
        rbSetupEthernetStatic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSetupChanged = true;
                setEthernetInputEnabled(true);
            }
        });

        mSetupNetworkInfo.redrawCallback[0] = new OnRedrawCallback() {
            @Override
            public void onRedraw() {
                redrawNetworkSetup();
            }
        };
        mSetupNetworkInfo.firstFocusing[0] = rbSetupEthernetDHCP;

        TextView setupNetworkEthernetSave = (TextView)findViewById(R.id.tvSetupNetworkEthernetSave);
        setupNetworkEthernetSave.setOnKeyListener(setupDetailKeyListener);
        setupNetworkEthernetSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rbSetupEthernetDHCP.isChecked()) {
                    IpConfiguration ip_config = new IpConfiguration();
                    ip_config.setIpAssignment(IpConfiguration.IpAssignment.DHCP);
                    mEthernetManager.setConfiguration(ip_config);
                } else {
                    try {
                        String netMask[] = etSetupNetworkInfoSubnetInput.getText().toString().split("\\.");
                        int prefixLength = 0;
                        for(int i=0; i<netMask.length; i++) {
                            String binaryVal = Integer.toBinaryString(Integer.valueOf(netMask[i]));
                            if(!binaryVal.equals("0"))
                                prefixLength += binaryVal.length();
                        }
                        // Static 설정으로 저장
                        StaticIpConfiguration static_config = new StaticIpConfiguration();
                        static_config.ipAddress = new LinkAddress(etSetupNetworkInfoIPInput.getText().toString()+"/" + prefixLength);
                        static_config.gateway = InetAddress.getByName(etSetupNetworkInfoGatewayInput.getText().toString());
                        static_config.dnsServers.add(InetAddress.getByName(etSetupNetworkInfoDNS1Input.getText().toString()));
                        static_config.dnsServers.add(InetAddress.getByName(etSetupNetworkInfoDNS2Input.getText().toString()));
                        IpConfiguration ip_config = new IpConfiguration();
                        ip_config.setIpAssignment(IpConfiguration.IpAssignment.STATIC);
                        ip_config.setStaticIpConfiguration(static_config);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                isSetupChanged = false;
                showToast(getResources().getString(R.string.msg_setup_saved));
                setupBackToLeftMenu();
            }
        });
        TextView setupNetworkEthernetCancel = (TextView)findViewById(R.id.tvSetupNetworkEthernetCancel);
        setupNetworkEthernetCancel.setOnKeyListener(setupDetailKeyListener);
        setupNetworkEthernetCancel.setOnClickListener(setupCancelClickListener);

        //-- Wifi
        tvSetupNetworkWiFiStatus = (TextView)findViewById(R.id.tvSetupNetworkWiFiStatus);
        tvSetupNetworkWiFiIPText = (TextView)findViewById(R.id.tvSetupNetworkWiFiIPAddress);
        tvSetupNetworkWiFiIPValue = (TextView)findViewById(R.id.tvSetupNetworkWiFiIPAddressValue);
        tvSetupNetworkWiFiAPText = (TextView)findViewById(R.id.tvSetupNetworkWiFiAPName);
        tvSetupNetworkWiFiAPValue = (TextView)findViewById(R.id.tvSetupNetworkWiFiAPNameValue);

        View.OnKeyListener setupWifiAPListKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction() == KeyEvent.ACTION_DOWN) {
                    if(keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if(v.getId() == R.id.setupWiFiAP1) {
                            if(firstVisibleAP > 0) {
                                firstVisibleAP--;
                                drawAPList(firstVisibleAP);
                                return true;
                            }
                        }
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if(v.getId() == R.id.setupWiFiAP4) {
                            if(firstVisibleAP < curAPList.size() - 4) {
                                firstVisibleAP++;
                                drawAPList(firstVisibleAP);
                            }
                            return true; // 리스트 아래로 더 내려가지 못함
                        }
                    } else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    }

                }
                return false;
            }
        };
        View.OnClickListener setupWifiAPClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
                if(networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                    showToast(R.string.msg_already_connected_to_ethernet);
                    return;
                }

                int curIdx = firstVisibleAP;
                for(int i=0; i<4; i++) {
                    if(v.getId() == wifiAPListIDs[i]) {
                        curIdx += i;
                        break;
                    }
                }
                String selectedSSID = curAPList.get(curIdx).SSID;
                boolean isOpen = Utils.getScanResultSecurity(curAPList.get(curIdx)).equals("OPEN");

                if(isOpen) {
                    Utils.connectToAP(mWifiManager, selectedSSID, "");
                } else {
                    isWifiPwEntering = true;
                    wifiPWAPName.setText("'" + selectedSSID +"'");
                    wifiPWAPName.setVisibility(View.VISIBLE);
                    wifiPWGuide.setVisibility(View.VISIBLE);
                    wifiPWInput.setText("");
                    wifiPWInput.setVisibility(View.VISIBLE);
                    wifiConnect.setVisibility(View.VISIBLE);
                    wifiShowPW.setVisibility(View.VISIBLE);
                    wifiShowPWDesc.setVisibility(View.VISIBLE);
                    wifiPWInput.requestFocus();
                }
            }
        };

        CompoundButton.OnCheckedChangeListener wifiOnOffListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked == true) {
                    if(rbSetupWifiOn == buttonView) {
                        LogUtil.d("Wifi on");
                        if(!mWifiManager.isWifiEnabled()) {
                            mWifiManager.setWifiEnabled(true);
                        }
                    } else {
                        LogUtil.d("Wifi off");
                        mWifiManager.setWifiEnabled(false);
                    }
                }
            }
        };

        rbSetupWifiOn = (RadioButton)findViewById(R.id.rbSetupNetworkWiFiOn);
        rbSetupWifiOff = (RadioButton)findViewById(R.id.rbSetupNetworkWiFiOff);
        rbSetupWifiOn.setOnCheckedChangeListener(wifiOnOffListener);
        rbSetupWifiOff.setOnCheckedChangeListener(wifiOnOffListener);
        rbSetupWifiOn.setOnKeyListener(setupDetailKeyListener);
        rbSetupWifiOff.setOnKeyListener(setupDetailKeyListener);

        selectedAPCheck = (ImageView)findViewById(R.id.ivSetupWiFiSelectedAPCheck);
        selectedAP = findViewById(R.id.setupWiFiSelectedAP);
        selectedAP.setFocusable(false);
        selectedAPName = (TextView)selectedAP.findViewById(R.id.tvAPName);
        selectedAPSecure = (ImageView)selectedAP.findViewById(R.id.ivSecureAP);
        selectedAPSignal = (ImageView)selectedAP.findViewById(R.id.ivSignalStrength);
        wifiDivider = findViewById(R.id.setupWiFiDivider);
        wifiAP = new View[4];
        wifiAPName = new TextView[4];
        wifiAPSecure = new ImageView[4];
        wifiAPSignal = new ImageView[4];
        for(int i=0; i<4; i++) {
            wifiAP[i] = findViewById(wifiAPListIDs[i]);
            wifiAP[i].setOnKeyListener(setupWifiAPListKeyListener);
            wifiAP[i].setOnClickListener(setupWifiAPClickListener);
            wifiAPName[i] = (TextView) wifiAP[i].findViewById(R.id.tvAPName);
            wifiAPSecure[i] = (ImageView) wifiAP[i].findViewById(R.id.ivSecureAP);
            wifiAPSignal[i] = (ImageView) wifiAP[i].findViewById(R.id.ivSignalStrength);
        }
        wifiAPListUpArrow = (ImageView)findViewById(R.id.ivSetupWifiArrowUp);
        wifiAPListDownArrow = (ImageView)findViewById(R.id.ivSetupWifiArrowDown);

        wifiPWAPName = (TextView)findViewById(R.id.tvSetupNetworkWiFiPWAPName);
        wifiPWGuide = (TextView)findViewById(R.id.tvSetupNetworkWiFiPWGuide);
        wifiPWInput = (ASEditText)findViewById(R.id.tvSetupNetworkWiFiPWInput);
        wifiPWInput.setOnKeyListener(setupDetailKeyListener);
        wifiConnect = (TextView)findViewById(R.id.tvSetupNetworkWiFiConnect);
        wifiConnect.setOnKeyListener(setupDetailKeyListener);
        wifiShowPW = (CheckBox) findViewById(R.id.cbSetupNetworkWiFiShowPW);
        wifiShowPW.setOnKeyListener(setupDetailKeyListener);
        wifiShowPWDesc = (TextView)findViewById(R.id.tvSetupNetworkWiFiShowPW);
        wifiConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputPW = wifiPWInput.getText().toString();
                String ssid = (String) wifiPWAPName.getText();
                ssid = ssid.substring(1, ssid.length() - 1);

                LogUtil.d("Connect to AP " + ssid);
                connectionResultWaiting = true;
                int res = Utils.connectToAP(mWifiManager, ssid, inputPW);
                if(res == -1) {
                    showToast(R.string.msg_password_too_short);
                    connectionResultWaiting = false;
                }
            }
        });
        wifiShowPW.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(wifiPWInput != null && wifiPWInput.getVisibility() == View.VISIBLE) {
                    if (isChecked) {
                        wifiPWInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    } else {
                        wifiPWInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }
                }
            }
        });
        mSetupNetworkInfo.redrawCallback[1] = new OnRedrawCallback() {
            @Override
            public void onRedraw() {
                redrawNetworkSetup();
            }
        };
        mSetupNetworkInfo.firstFocusing[1] = rbSetupWifiOff;

        // Display
        mSetupDisplayInfo = new SetupLeftMenuInfo(setupDisplayMenuStr, setupDisplayMenuImg, setupDisplayMenuView);
        // --- 해상도
        TextView tvSetupResolutionInfo2 = (TextView)findViewById(R.id.tvSetupResolutionInfo2);
        tvSetupResolutionInfo2.setText(Html.fromHtml(getResources().getString(R.string.str_setup_resolution_info2)));
        TextView setupDisplayResolutionSave = (TextView)findViewById(R.id.tvSetupDisplayResolutionSave);
        setupDisplayResolutionSave.setOnKeyListener(setupDetailKeyListener);
        setupDisplayResolutionSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = rgResolution.getSelectedIndex();
                LogUtil.d("Update resolution to - " + DISPLAY_RESOLUTION[index]);
                mOutputUiManager.change2NewMode(DISPLAY_RESOLUTION[index]);
                mOutputUiManager.updateUiMode();
            }
        });
        TextView setupDisplayResolutionCancel = (TextView)findViewById(R.id.tvSetupDisplayResolutionCancel);
        setupDisplayResolutionCancel.setOnKeyListener(setupDetailKeyListener);
        setupDisplayResolutionCancel.setOnClickListener(setupCancelClickListener);
        rgResolution = new SednRadioGroup(MainActivity.this, setupDetailKeyListener, R.id.rbResolution480i, R.id.rbResolution1080i, R.id.rbResolution4k, R.id.rbResolution480p, R.id.rbResolution720p, R.id.rbResolution1080p);

        mSetupDisplayInfo.firstFocusing[0] = rgResolution.getButtonView(0);
        mSetupDisplayInfo.redrawCallback[0] = new OnRedrawCallback() {
            @Override
            public void onRedraw() {
                String curMode = mOutputUiManager.mOutputModeManager.getCurrentOutputMode();
                LogUtil.d("current mode - " + curMode);
                for(int i = 0; i < DISPLAY_RESOLUTION.length; i++) {
                    if(curMode.equals(DISPLAY_RESOLUTION[i]))
                        rgResolution.checkButton(i);
                }

                ArrayList<String> supportList = new ArrayList<String>(Arrays.asList(mOutputUiManager.mOutputModeManager.getHdmiSupportList().split(",")));
                LogUtil.d("HDMI support list - " + supportList);
                for(int i = 0; i < DISPLAY_RESOLUTION.length; i++) {
                    if(!supportList.contains(DISPLAY_RESOLUTION[i])) {
                        rgResolution.setEnabled(i, false);
                    }
                }
            }
        };

        // --- CEC
        initCecFun();

        TextView setupDisplayCECSave = (TextView)findViewById(R.id.tvSetupDisplayCECSave);
        setupDisplayCECSave.setOnKeyListener(setupDetailKeyListener);
        setupDisplayCECSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    int index = rgCECOnOff.getSelectedIndex();

                    if(0 == index) { // CEC ON
                        mHdmiCecManager.setCecSysfsValue(HdmiCecManager.FUN_CEC, HdmiCecManager.FUN_OPEN);
                        mHdmiCecManager.setCecSysfsValue(HdmiCecManager.FUN_ONE_KEY_PLAY, HdmiCecManager.FUN_CLOSE);
                        mHdmiCecManager.setCecSysfsValue(HdmiCecManager.FUN_ONE_KEY_POWER_OFF, HdmiCecManager.FUN_CLOSE);
                        mHdmiCecManager.setCecSysfsValue(HdmiCecManager.FUN_AUTO_CHANGE_LANGUAGE, HdmiCecManager.FUN_CLOSE);
                        Toast.makeText(getApplication(), "HDMI-CEC ON", Toast.LENGTH_SHORT).show();
                    }
                    else { // CEC OFF
                        mHdmiCecManager.setCecSysfsValue(HdmiCecManager.FUN_CEC, HdmiCecManager.FUN_CLOSE);
                        Toast.makeText(getApplication(), "HDMI-CEC OFF", Toast.LENGTH_SHORT).show();
                    }

/*
                    FileOutputStream outputStream = new FileOutputStream("/dev/input/event1");
                    byte[] buffer = new byte[5];
                    buffer[0] = '1';
                    buffer[1] = '0';
                    buffer[2] = ':';
                    buffer[3] = '3';
                    buffer[4] = '6';

                    outputStream.write(buffer);
                    outputStream.close();
                    LogUtil.d("file written");*/
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        TextView setupDisplayCECCancel = (TextView)findViewById(R.id.tvSetupDisplayCECCancel);
        setupDisplayCECCancel.setOnKeyListener(setupDetailKeyListener);
        setupDisplayCECCancel.setOnClickListener(setupCancelClickListener);


        rgCECOnOff = new SednRadioGroup(MainActivity.this, setupDetailKeyListener, R.id.rbCECOn, R.id.rbCECOff);
        mSetupDisplayInfo.firstFocusing[1] = rgCECOnOff.getButtonView(0);
        mSetupDisplayInfo.redrawCallback[1] = new OnRedrawCallback() {
            @Override
            public void onRedraw() {

                // CEC 설정상태에 따라서 라디오버튼을 표시한다.
                if(mHdmiCecManager.isCecEnabled()) {
                    rgCECOnOff.checkButton(0);
                }
                else {
                    rgCECOnOff.checkButton(1);
                }
            }
        };


        // System
        mSetupSystemInfo = new SetupLeftMenuInfo(setupSystemMenuStr, setupSystemMenuImg, setupSystemMenuView);
        // -- Server URL
        etSetupServiceURL = (EditText)findViewById(R.id.etSetupSystemServiceURLInput);
        etSetupServiceURL.setOnKeyListener(setupDetailKeyListener);
        etSetupServiceURL.addTextChangedListener(setupTextChangedListener);
        etSetupServicePort = (EditText)findViewById(R.id.etSetupSystemServicePortInput);
        etSetupServicePort.setOnKeyListener(setupDetailKeyListener);
        etSetupServicePort.addTextChangedListener(setupTextChangedListener);
        etSetupPushPort = (EditText)findViewById(R.id.etSetupSystemPushPortInput);
        etSetupPushPort.setOnKeyListener(setupDetailKeyListener);
        etSetupPushPort.addTextChangedListener(setupTextChangedListener);
        mSetupSystemInfo.redrawCallback[0] = new OnRedrawCallback() {
            @Override
            public void onRedraw() {
                etSetupServiceURL.setText(mApp.gSednServer);
                etSetupServicePort.setText(mApp.gSednServerPort);
                etSetupPushPort.setText(mApp.gSednPushPort);
            }
        };
        mSetupSystemInfo.firstFocusing[0] = etSetupServiceURL;
        TextView setupSystemServerURLSave = (TextView)findViewById(R.id.tvSetupSystemServerURLSave);
        setupSystemServerURLSave.setOnKeyListener(setupDetailKeyListener);
        setupSystemServerURLSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mApp.gSednServer = etSetupServiceURL.getText().toString();
                mApp.gSednServerPort = etSetupServicePort.getText().toString();
                mApp.gSednPushPort = etSetupPushPort.getText().toString();
                Utils.setServiceURL(MainActivity.this, mApp.gSednServer);
                Utils.setServicePort(MainActivity.this, mApp.gSednServerPort);
                Utils.setPushPort(MainActivity.this, mApp.gSednPushPort);
                isSetupChanged = false;
                showToast(getResources().getString(R.string.msg_setup_saved));
                setupBackToLeftMenu();
            }
        });
        TextView setupSystemServerURLCancel = (TextView)findViewById(R.id.tvSetupSystemServerURLCancel);
        setupSystemServerURLCancel.setOnKeyListener(setupDetailKeyListener);
        setupSystemServerURLCancel.setOnClickListener(setupCancelClickListener);

        // -- Reboot
        TextView setupSystemReboot = (TextView)findViewById(R.id.tvSetupSystemRebootConfirm);
        setupSystemReboot.setOnKeyListener(setupDetailKeyListener);
        setupSystemReboot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rebootSTB();
            }
        });
        TextView setupSystemRebootCancel = (TextView)findViewById(R.id.tvSetupSystemRebootCancel);
        setupSystemRebootCancel.setOnKeyListener(setupDetailKeyListener);
        setupSystemRebootCancel.setOnClickListener(setupCancelClickListener);
        mSetupSystemInfo.firstFocusing[1] = setupSystemReboot;

        //-- Firmware Update
        tvFirmwareVersion = (TextView)findViewById(R.id.tvSetupFirmwareVersionValue);
        tvFirmwareDate = (TextView)findViewById(R.id.tvSetupFirmwareDateValue);
        tvFirmwareNoUpdate = (TextView)findViewById(R.id.tvSetupFirmwareNoUpdate);
        tvFirmwareUpdate1 = (TextView)findViewById(R.id.tvSetupFirmwareUpdate1);
        tvFirmwareUpdate2 = (TextView)findViewById(R.id.tvSetupFirmwareUpdate2);
        tvFirmwareUpdate3 = (TextView)findViewById(R.id.tvSetupFirmwareUpdate3);
        tvFirmwareNewVersion = (TextView)findViewById(R.id.tvSetupFirmwareNewVersion);
        setupSystemFirmwareConfirm = (TextView)findViewById(R.id.tvSetupSystemFirmwareConfirm);
        setupSystemFirmwareConfirm.setOnKeyListener(setupDetailKeyListener);
        setupSystemFirmwareConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 다운로드 후 업데이트
                firmwareDownloadAndUpdate();
            }
        });
        setupSystemFirmwareCancel = (TextView)findViewById(R.id.tvSetupSystemFirmwareCancel);
        setupSystemFirmwareCancel.setOnKeyListener(setupDetailKeyListener);
        setupSystemFirmwareCancel.setOnClickListener(setupCancelClickListener);
        mSetupSystemInfo.firstFocusing[2] = setupSystemFirmwareConfirm;
        mSetupSystemInfo.redrawCallback[2] = new OnRedrawCallback() {
            @Override
            public void onRedraw() {
                tvFirmwareVersion.setText(mApp.gFirmwareVersion);
                tvFirmwareDate.setText("("+ mApp.gFirmwareDate + ")");
                //SednDBClient.checkForUpdate(false);
                RetrofitClient.checkForUpdate(false);
            }
        };

        //-- Storage and Reset
        View.OnClickListener setupChangedClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSetupChanged = true;
            }
        };
        tvSetupStorageTotal = (TextView)findViewById(R.id.tvSetupStorageTotalValue);
        tvSetupStorageUsed = (TextView)findViewById(R.id.tvSetupStorageUsedValue);
        rgSetupStorageRetention = (RadioGroup)findViewById(R.id.rgFileRetention);
        RadioButton rbSetupStorageRetention3Month = (RadioButton)findViewById(R.id.rbFileRetention3Month);
        rbSetupStorageRetention3Month.setOnKeyListener(setupDetailKeyListener);
        rbSetupStorageRetention3Month.setOnClickListener(setupChangedClickListener);
        RadioButton rbSetupStorageRetention6Month = (RadioButton)findViewById(R.id.rbFileRetention6Month);
        rbSetupStorageRetention6Month.setOnKeyListener(setupDetailKeyListener);
        rbSetupStorageRetention6Month.setOnClickListener(setupChangedClickListener);
        RadioButton rbSetupStorageRetentionNone = (RadioButton)findViewById(R.id.rbFileRetentionNone);
        rbSetupStorageRetentionNone.setOnKeyListener(setupDetailKeyListener);
        rbSetupStorageRetentionNone.setOnClickListener(setupChangedClickListener);
        TextView tvSetupSTBResetButton = (TextView)findViewById(R.id.tvSetupSTBResetButton);
        tvSetupSTBResetButton.setOnKeyListener(setupDetailKeyListener);
        tvSetupSTBResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater=getLayoutInflater();
                final View dialogView= inflater.inflate(R.layout.dialog_reset, null);

                AlertDialog.Builder alert_confirm = new AlertDialog.Builder(MainActivity.this);
                alert_confirm.setView(dialogView);
                AlertDialog alert = alert_confirm.create();

                TextView resetConfirm = (TextView)dialogView.findViewById(R.id.tvResetConfirm);
                TextView resetCancel = (TextView)dialogView.findViewById(R.id.tvResetCancel);
                resetConfirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        resetSTB();

                        // 장비 재부팅
                        rebootSTB();
                    }
                });
                resetCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alert.dismiss();
                    }
                });
                alert.show();
                alert.getWindow().setLayout(570, 250);
            }
        });
        TextView tvSetupSystemStorageSave = (TextView)findViewById(R.id.tvSetupSystemStorageSave);
        tvSetupSystemStorageSave.setOnKeyListener(setupDetailKeyListener);
        tvSetupSystemStorageSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int checkIdx = 0;
                for(int i=0; i<setupStorageRetentionId.length; i++) {
                    if(setupStorageRetentionId[i] == rgSetupStorageRetention.getCheckedRadioButtonId())
                        checkIdx = i+1;
                }
                Utils.setRetentionPeriod(MainActivity.this, checkIdx);
                mApp.gStorageRetention = checkIdx;
                isSetupChanged = false;
                showToast(getResources().getString(R.string.msg_setup_saved));
                setupBackToLeftMenu();
            }
        });
        TextView tvSetupSystemStorageCancel = (TextView)findViewById(R.id.tvSetupSystemStorageCancel);
        tvSetupSystemStorageCancel.setOnKeyListener(setupDetailKeyListener);
        setupSystemRebootCancel.setOnClickListener(setupCancelClickListener);
        mSetupSystemInfo.redrawCallback[3] = new OnRedrawCallback() {
            @Override
            public void onRedraw() {
                int total = Utils.totalSpace(LocalDBManager.getDownloadFolder().getAbsolutePath());
                int used = Utils.busySpace(LocalDBManager.getDownloadFolder().getAbsolutePath());
                String total_str = String.format("%.1fG", (float)total/1024.0f);
                String used_str = String.format("%.1fG (%d%%)", (float)used/1024.0f, used * 100 / total);

                tvSetupStorageTotal.setText(total_str);
                tvSetupStorageUsed.setText(used_str);

                rgSetupStorageRetention.check(setupStorageRetentionId[mApp.gStorageRetention-1]);
            }
        };
        mSetupSystemInfo.firstFocusing[3] = rbSetupStorageRetention3Month;

        setupLeftMenuInfos = new SetupLeftMenuInfo[]{null, mSetupNetworkInfo, mSetupDisplayInfo, mSetupSystemInfo};

        // 초기 진입시 Information 선택
        setupItemList[0].setSelected(true);
        mSetupState = SETUP_STATE_Menu;
        //setupViewList[0].setVisibility(View.VISIBLE);
        layoutSetupInformation.setVisibility(View.VISIBLE);
    }

    public void redrawFirmwareResult(boolean updateNeeded) {
        tvFirmwareNoUpdate.setVisibility(View.INVISIBLE);
        tvFirmwareUpdate1.setVisibility(View.INVISIBLE);
        tvFirmwareUpdate2.setVisibility(View.INVISIBLE);
        tvFirmwareUpdate3.setVisibility(View.INVISIBLE);
        tvFirmwareNewVersion.setVisibility(View.INVISIBLE);
        setupSystemFirmwareConfirm.setVisibility(View.INVISIBLE);
        setupSystemFirmwareCancel.setVisibility(View.INVISIBLE);

        if(!updateNeeded) {
            tvFirmwareNoUpdate.setVisibility(View.VISIBLE);
        } else {
            tvFirmwareUpdate1.setVisibility(View.VISIBLE);
            tvFirmwareUpdate2.setVisibility(View.VISIBLE);
            tvFirmwareUpdate3.setVisibility(View.VISIBLE);
            tvFirmwareNewVersion.setVisibility(View.VISIBLE);
            tvFirmwareNewVersion.setText("[Firmware Ver " + mApp.gNewFirmwareVersion + "]");

            setupSystemFirmwareConfirm.setVisibility(View.VISIBLE);
            setupSystemFirmwareCancel.setVisibility(View.VISIBLE);
            setupSystemFirmwareConfirm.requestFocus();
        }
    }

    public boolean toggleBookmark(String id) {
        boolean isAdded = mLocalDBManager.toggleBookmark(id);
        if(isAdded)
            showToast(R.string.msg_bookmark_added, Toast.LENGTH_SHORT);
        else
            showToast(R.string.msg_bookmark_removed, Toast.LENGTH_SHORT);

        return isAdded;
    }

    public void setBookmark(String id) {
        mLocalDBManager.addToBookmark(id);
        showToast(R.string.msg_bookmark_added, Toast.LENGTH_SHORT);
    }

    public void removeFile(String id) {
        mLocalDBManager.removeFile(id);
        showToast(R.string.msg_file_removed, Toast.LENGTH_SHORT);
        updateSTBStatus();
    }
    public void setEthernetInputEnabled(boolean enabled) {
        etSetupNetworkInfoIPInput.setEnabled(enabled);
        etSetupNetworkInfoSubnetInput.setEnabled(enabled);
        etSetupNetworkInfoGatewayInput.setEnabled(enabled);
        etSetupNetworkInfoDNS1Input.setEnabled(enabled);
        etSetupNetworkInfoDNS2Input.setEnabled(enabled);
        etSetupNetworkInfoIPInput.setFocusable(enabled);
        etSetupNetworkInfoSubnetInput.setFocusable(enabled);
        etSetupNetworkInfoGatewayInput.setFocusable(enabled);
        etSetupNetworkInfoDNS1Input.setFocusable(enabled);
        etSetupNetworkInfoDNS2Input.setFocusable(enabled);
    }
    public String getEthernetIpAddress()
    {
        Object localObject = this.mConnectivityManager.getLinkProperties(ConnectivityManager.TYPE_ETHERNET);
        if (localObject == null) {
            return "";
        }
        localObject = ((LinkProperties)localObject).getAllLinkAddresses().iterator();
        while (((Iterator)localObject).hasNext())
        {
            InetAddress localInetAddress = ((LinkAddress)((Iterator)localObject).next()).getAddress();
            if ((localInetAddress instanceof Inet4Address)) {
                return localInetAddress.getHostAddress();
            }
        }
        return "";
    }

    public void drawAPList(int firstIdx) {
        LogUtil.d("drawing AP List from " + firstIdx);

        if(firstIdx > 0)
            wifiAPListUpArrow.setVisibility(View.VISIBLE);
        else
            wifiAPListUpArrow.setVisibility(View.INVISIBLE);
        if(curAPList.size() - firstIdx > 4)
            wifiAPListDownArrow.setVisibility(View.VISIBLE);
        else
            wifiAPListDownArrow.setVisibility(View.INVISIBLE);

        for(int i=0; i<curAPList.size(); i++) {
            ScanResult scanResult = (ScanResult) curAPList.get(i);
            LogUtil.d("AP info " + scanResult.SSID, ", " + WifiManager.calculateSignalLevel(scanResult.level, 4));
        }

        for (int i = 0; i < 4; i++) {
            if(i >= curAPList.size()) {
                wifiAP[i].setVisibility(View.INVISIBLE);
            } else {
                ScanResult scanResult = (ScanResult) curAPList.get(firstIdx + i);
                LogUtil.d("Add AP " + scanResult.SSID, ", " + WifiManager.calculateSignalLevel(scanResult.level, 4));
                wifiAP[i].setVisibility(View.VISIBLE);
                wifiAPName[i].setText(scanResult.SSID);
                wifiAPSignal[i].setImageResource(setupNetworkWiFiLevelImg[WifiManager.calculateSignalLevel(scanResult.level, 4)]);
                if(Utils.getScanResultSecurity(scanResult).equals("OPEN")) {
                    wifiAPSecure[i].setVisibility(View.INVISIBLE);
                } else {
                    wifiAPSecure[i].setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void drawNetworkStatus(int status) {
        if(mLauncherMode == MODE_SETUP) {
            if (mSelectedSetupItemIdx == SETUP_NETWORK) {
                TextView statusView;
                if(mSetupNetworkInfo.selected == 0) {
                    statusView = tvSetupNetworkEthernetStatus;
                } else {
                    statusView = tvSetupNetworkWiFiStatus;
                }
                if(status == WifiManager.WIFI_STATE_ENABLING)
                    statusView.setText(getResources().getString(R.string.str_setup_network_connecting));
            }
        }
    }
    public void redrawNetworkSetup() {
        LogUtil.d("redrawNetworkSetup()");
        if(mLauncherMode == MODE_SETUP) {
            if(mSelectedSetupItemIdx == SETUP_INFORMATION) {
                tvSetupInfoIPAddress.setText(mApp.myIP);
            } else if(mSelectedSetupItemIdx == SETUP_NETWORK) {
                TextView status, ip_text, ip_value, ap_text, ap_value;

                if(mSetupNetworkInfo.selected == 0) {
                    status = tvSetupNetworkEthernetStatus;
                    ip_text = tvSetupNetworkEthernetIPText;
                    ip_value = tvSetupNetworkEthernetIPValue;
                    ap_text = tvSetupNetworkEthernetAPText;
                    ap_value = tvSetupNetworkEthernetAPValue;
                } else {
                    status = tvSetupNetworkWiFiStatus;
                    ip_text = tvSetupNetworkWiFiIPText;
                    ip_value = tvSetupNetworkWiFiIPValue;
                    ap_text = tvSetupNetworkWiFiAPText;
                    ap_value = tvSetupNetworkWiFiAPValue;
                }

                NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
                ip_text.setVisibility(View.INVISIBLE);
                ip_value.setVisibility(View.INVISIBLE);
                ap_text.setVisibility(View.INVISIBLE);
                ap_value.setVisibility(View.INVISIBLE);
                if(networkInfo == null || !networkInfo.isConnected()) {
                    status.setText(getResources().getString(R.string.str_setup_network_not_connected));
                } else {
                    if(networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                        status.setText(getResources().getString(R.string.str_setup_network_ethernet_connected));
                        ip_text.setVisibility(View.VISIBLE);
                        ip_value.setVisibility(View.VISIBLE);
                        ip_value.setText(getEthernetIpAddress());
                    } else if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        status.setText(getResources().getString(R.string.str_setup_network_wifi_connected));
                        ip_text.setVisibility(View.VISIBLE);
                        ip_value.setVisibility(View.VISIBLE);
                        ip_value.setText(Formatter.formatIpAddress(mWifiManager.getConnectionInfo().getIpAddress()));
                        ap_text.setVisibility(View.VISIBLE);
                        ap_value.setVisibility(View.VISIBLE);
                        String ssid = mWifiManager.getConnectionInfo().getSSID();
                        ap_value.setText(ssid.substring(1, ssid.length() - 1));
                    } else {
                        status.setText(getResources().getString(R.string.str_setup_network_unknown));
                    }
                }

                if(mSetupNetworkInfo.selected == 0) {
                    // 유선
                    IpConfiguration configuration = mEthernetManager.getConfiguration();
                    if(configuration.getIpAssignment() == IpConfiguration.IpAssignment.DHCP) {
                        rbSetupEthernetDHCP.setChecked(true);
                        setEthernetInputEnabled(false);
                        try {
                            Runtime runtime = Runtime.getRuntime();
                            Process process;
                            BufferedReader br;

                            process = runtime.exec("getprop dhcp.eth0.ipaddress");
                            br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            etSetupNetworkInfoIPInput.setText(br.readLine());

                            process = runtime.exec("getprop dhcp.eth0.mask");
                            br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            etSetupNetworkInfoSubnetInput.setText(br.readLine());

                            process = runtime.exec("getprop dhcp.eth0.gateway");
                            br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            etSetupNetworkInfoGatewayInput.setText(br.readLine());

                            process = runtime.exec("getprop dhcp.eth0.dns1");
                            br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            etSetupNetworkInfoDNS1Input.setText(br.readLine());

                            process = runtime.exec("getprop dhcp.eth0.dns2");
                            br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            etSetupNetworkInfoDNS2Input.setText(br.readLine());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        rbSetupEthernetStatic.setChecked(true);
                        setEthernetInputEnabled(true);
                    }
                } else {
                    // 무선
                    if(mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                        // Wifi ON
                        LogUtil.d("Wifi On!!!");
                        rbSetupWifiOn.setChecked(true);
                        boolean res = mWifiManager.startScan();
                        LogUtil.d("Start Wifi scan - " + res);

                        wifiPWAPName.setVisibility(View.INVISIBLE);
                        wifiPWGuide.setVisibility(View.INVISIBLE);
                        wifiPWInput.setVisibility(View.INVISIBLE);
                        wifiConnect.setVisibility(View.INVISIBLE);
                        wifiShowPW.setVisibility(View.INVISIBLE);
                        wifiShowPWDesc.setVisibility(View.INVISIBLE);

                        wifiDivider.setVisibility(View.VISIBLE);
                        selectedAPCheck.setVisibility(View.INVISIBLE);
                        selectedAPSecure.setVisibility(View.INVISIBLE);
                        selectedAPSignal.setVisibility(View.INVISIBLE);

                        // 연결된 AP 출력
                        if(networkInfo != null && networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                            String connectedSSID = wifiInfo.getSSID();
                            LogUtil.d("connected AP : " + connectedSSID);

                            selectedAPCheck.setVisibility(View.VISIBLE);
                            selectedAP.setVisibility(View.VISIBLE);
                            selectedAPName.setVisibility(View.VISIBLE);
                            selectedAPName.setText(connectedSSID.substring(1, connectedSSID.length() - 1));
                            selectedAPSignal.setVisibility(View.VISIBLE);
                            selectedAPSignal.setImageResource(setupNetworkWiFiLevelImg[WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 4)]);
                            List<WifiConfiguration> wifiConfigurations = mWifiManager.getConfiguredNetworks();

                            for(WifiConfiguration config : wifiConfigurations) {
                               if(config.SSID.equals(connectedSSID)) {
                                   if(config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK) || config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) ||
                                           config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA2_PSK) || config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X) || config.wepKeys[0] != null) {
                                       selectedAPSecure.setVisibility(View.VISIBLE);
                                   }
                               }
                            }
                        }
                    } else {
                        // Wifi OFF
                        rbSetupWifiOff.setChecked(true);
                        selectedAPCheck.setVisibility(View.INVISIBLE);
                        selectedAP.setVisibility(View.INVISIBLE);
                        selectedAPName.setVisibility(View.INVISIBLE);
                        selectedAPSecure.setVisibility(View.INVISIBLE);
                        selectedAPSignal.setVisibility(View.INVISIBLE);
                        wifiDivider.setVisibility(View.INVISIBLE);
                        for(int i=0; i<4; i++) {
                            wifiAP[i].setVisibility(View.INVISIBLE);
                        }
                        wifiAPListUpArrow.setVisibility(View.INVISIBLE);
                        wifiAPListDownArrow.setVisibility(View.INVISIBLE);

                        wifiPWAPName.setVisibility(View.INVISIBLE);
                        wifiPWGuide.setVisibility(View.INVISIBLE);
                        wifiPWInput.setVisibility(View.INVISIBLE);
                        wifiConnect.setVisibility(View.INVISIBLE);
                        wifiShowPW.setVisibility(View.INVISIBLE);
                        wifiShowPWDesc.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }

        updateSTBStatus();
    }

    private void switchSetupMenu(int selectedIdx) {

        if(mSelectedSetupItemIdx >= SETUP_INFORMATION) {
            setupItemList[mSelectedSetupItemIdx].setSelected(false);;
        }

        // 이전 View 제거
        if(mSelectedSetupItemIdx == SETUP_INFORMATION) {
            layoutSetupInformation.setVisibility(View.GONE);
        } else {
            setupLeftMenuInfos[mSelectedSetupItemIdx].hideView();
        }

        mSelectedSetupItemIdx = selectedIdx;

        // 새로운 View 표시
        if(selectedIdx == 0) {
            setupLeftMenu.setVisibility(View.GONE);
            layoutSetupInformation.setVisibility(View.VISIBLE);
        } else {
            setupLeftMenu.setVisibility(View.VISIBLE);
            curLeftMenu.selected = 0;
            curLeftMenu.firstVisible = 0;
            redrawSetupLeftMenu();
            setupLeftMenuInfos[selectedIdx].showView();
        }

        setupItemList[selectedIdx].setSelected(true);
        isWifiPwEntering = false;
    }

    private void redrawSetupLeftMenu() {
        int upper = curLeftMenu.firstVisible;
        int lower = upper + 1;

        setupLeftItemUpperText.setText(curLeftMenu.menuStr[upper]);
        setupLeftItemUpperImage.setImageResource(curLeftMenu.menuImg[upper]);
        setupLeftItemLowerText.setText(curLeftMenu.menuStr[lower]);
        setupLeftItemLowerImage.setImageResource(curLeftMenu.menuImg[lower]);

        setupLeftItemUpper.setSelected(upper == curLeftMenu.selected);
        setupLeftItemLower.setSelected(lower == curLeftMenu.selected);

        if(curLeftMenu.firstVisible > 0)
            setupArrowUp.setVisibility(View.VISIBLE);
        else
            setupArrowUp.setVisibility(View.GONE);

        if(curLeftMenu.firstVisible < curLeftMenu.length - 2)
            setupArrowDown.setVisibility(View.VISIBLE);
        else
            setupArrowDown.setVisibility(View.GONE);
    }

    private void setSTBInfo() {
        tvSetupInfoModelName.setText(mApp.gModelName);
        tvSetupInfoServiceURL.setText(mApp.gSednServer + ":" + mApp.gSednServerPort);
        tvSetupInfoIPAddress.setText(mApp.myIP);
        tvSetupInfoMACAddress.setText(mApp.myMAC);
        tvSetupInfoFWVersion.setText(mApp.gFirmwareVersion);
    }

    private void updateSTBStatus() {
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            int total = Utils.totalSpace(LocalDBManager.getDownloadFolder().getAbsolutePath());
            int used = Utils.busySpace(LocalDBManager.getDownloadFolder().getAbsolutePath());
            float used_percent = used * 100 / total;
            String used_str = String.format("%d%% used", (int)used_percent);

            if (used_percent <= 80) {
                tvStatusColor.setBackgroundResource(R.drawable.stb_bg3);
                tvStatusText.setText(R.string.str_stb_normal);
            } else {
                tvStatusColor.setBackgroundResource(R.drawable.stb_bg2);
                tvStatusText.setText(R.string.str_stb_warning);
            }
            tvStorageUsage.setText(used_str);
            tvMenuInfoIP.setText("IP " + mApp.myIP);
        } else {
            tvStatusColor.setBackgroundResource(R.drawable.stb_bg);
            tvStatusText.setText(R.string.str_stb_disconnected);
            tvMenuInfoIP.setText("");
        }
    }

    private void setContentInfoView(View layout, VODItem item) {
        LogUtil.d("setContentInfo vod id = " + item.getID() + ", " + item.getName());
        TextView resolution = (TextView)layout.findViewById(R.id.tvInfoResolutionText);
        TextView fileFormat = (TextView)layout.findViewById(R.id.tvInfoFileformatText);
        TextView bitrate = (TextView)layout.findViewById(R.id.tvInfoBitrateText);
        TextView videoCodec = (TextView)layout.findViewById(R.id.tvInfoVideocodecText);
        TextView audioCodec = (TextView)layout.findViewById(R.id.tvInfoAudiocodecText);

        resolution.setText(item.mResolution);
        fileFormat.setText(item.mFileFormat);
        bitrate.setText(item.mBitrate);
        videoCodec.setText(item.mVideoCodec);
        audioCodec.setText(item.mAudioCodec);
    }

    private void focusDefaultItem(int mode) {
        switch(mode) {
            case MODE_HOME:
                homeTodaySchedule.requestFocus();
                mHomeState = HOME_STATE_Box;
                break;
            case MODE_VOD:
                lvMenuCategory[0].setSelection(0);
                lvMenuCategory[0].requestFocus();
                mVODState = VOD_STATE_MenuCategory;
                break;
            case MODE_LIVE:
                layoutLiveChannel.getChildAt(0).requestFocus();
                mLiveState = LIVE_STATE_Channel;
                break;
            case MODE_MYPAGE:
                vBookmarkTab.requestFocus();
                mMypageState = MYPAGE_STATE_Submenu;
                break;
            case MODE_SEARCH:
                etSearchText.requestFocus();
                mSearchState = SEARCH_STATE_InputText;
                break;
            case MODE_SETUP:
                setupItemList[0].requestFocus();
                mSetupState = SETUP_STATE_Menu;
                break;
        }
        quickMenuFocused = false;
    }

    private void quickMenuTo(int mode) {
            switchMenu(mode);
    }

    // 반드시 모드 전환 후 현재 모드에서 호출해야 함.
    private void resetMode(int mode) {
        ivMenuList[mode].requestFocus();
        quickMenuFocused = true;
        switch (mode) {
            case MODE_HOME:
                homeSearchBox.setText("");
                recentFocusBox.setVisibility(View.INVISIBLE);
                homeTodaySchedule.setSelected(false);
                homeSearch.setSelected(false);
                homeRecentVOD.setSelected(false);
                homeMostVOD.setSelected(false);
                break;
            case MODE_VOD:
                clearSelection(lvMenuCategory[0]);
                lvMenuCategory[1].setVisibility(View.INVISIBLE);
                lvVODItems.setVisibility(View.INVISIBLE);
                vVODContentPreview.setVisibility(View.INVISIBLE);
                vVODContentInfo.setVisibility(View.INVISIBLE);
                break;
            case MODE_LIVE:
                updateLiveTimeBar();
                focusToTimeBar();
                break;
            case MODE_MYPAGE:
                break;
            case MODE_SEARCH:
                break;
            case MODE_SETUP:
                setSTBInfo();
                switchSetupMenu(0);
                break;
        }
    }

    public void notifyRemoteClient(String text) {
        mRemoteServer.sendEditTextToClient(text);
    }

    /**
     * 다운로드 요청
     * @param vod
     */
    public void requestDownload(VODItem vod) {

        if(!mLocalDBManager.isDownloaded(vod.getID())) {

            if(requestDownload("VOD", vod)) {
                showToast("'" + vod.getName() + "' " + getResources().getString(R.string.msg_download_start));
            }
        } else {
            showToast(getResources().getString(R.string.msg_already_downloaded));
        }
    }

    /**
     * 다운로드 요청
     * 다운로드된 VOD인지 먼저 체크해야 한다.
     * @param title
     * @param vod
     */
    public boolean requestDownload(String title, VODItem vod) {

        //if(SednApplication.mUseSDCARD)
        {
            // 이곳 작업중 2017.4.26

            // 현재 다운로드가 진행 중인지 여부 체크
            if(FileDownloadService.isDownloadingOrEnQueue(vod.getID()) && title.equals("VOD")) {
                showToast("'" + vod.getName() + "' " + getResources().getString(R.string.msg_already_downloading));
                return false;
            }

            ensureFreeSpaceAndDownloadForService(title, vod);
            return true;
        }
//        else {
//
//            // 현재 다운로드가 진행 중인지 여부 체크
//            if(isDownloadingOrEnQueue(vod.getID())) {
//                showToast("'" + vod.getName() + "' " + getResources().getString(R.string.msg_already_downloading));
//                return false;
//            }
//
//            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(vod.getVideoPath()));
//            request.setTitle(title + vod.getID());
//
//            // 다운로드 시 임시파일명으로 지정 후 완료됐을때 파일명 변경
//            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, vod.getID() + ".mp4.temp");
//            ensureFreeSpaceAndDownload(request, vod.getVideoPath());
//            return true;
//        }
    }

    /**
     * 저장공간 확인 후 다운로드받을 수 있다면 DownloadManager 큐에 넣는다
     * 공간이 부족하다면 오래된 파일을 삭제
     */
    class CheckSpaceThread extends Thread {
        MainActivity mainActivity;
        DownloadManager.Request mRequest;
        String mPath;

        public CheckSpaceThread(MainActivity activity, DownloadManager.Request request, String path) {
            mainActivity = activity;
            mRequest = request;
            mPath = path;
        }

        @Override
        public void run() {
            long file_size = 0L;

            try {
                URL url = new URL(mPath);
                URLConnection connection = url.openConnection();
                connection.connect();
                file_size = (long)(connection.getContentLength());
            } catch (Exception e) {
                e.printStackTrace();
            }

            long freeSpace = Utils.freeSpaceInBytes(true);
            LogUtil.d("downloading file size " + file_size + ", free " + freeSpace);

            boolean canDownload = true;
            while(file_size > freeSpace) {
                canDownload = deleteOldestFile();
                if(!canDownload) break; // no more space. 다운로드 대상 파일이 전체 디스크 용량보다 큼
                freeSpace = Utils.freeSpaceInBytes(true);
                LogUtil.d("new free space " + freeSpace);
            }

            if(canDownload)
                mainActivity.mDownloadManager.enqueue(mRequest);
        }

        private boolean deleteOldestFile() {
            File downDir = LocalDBManager.getDownloadFolder();
            File[] fileList= downDir.listFiles();

            if(fileList.length == 0) return false;

            long oldestModified = Long.MAX_VALUE;
            int oldestIndex = -1;
            for(int i=0; i<fileList.length; i++) {
                long lastModified = fileList[i].lastModified();
                if(oldestModified > lastModified) {
                    oldestModified = lastModified;
                    oldestIndex = i;
                }
            }

            LogUtil.d("deleteing oldest " + fileList[oldestIndex].toString());

            fileList[oldestIndex].delete();
            return true;
        }
    }
    public void ensureFreeSpaceAndDownload(DownloadManager.Request request, String path) {
        CheckSpaceThread checkSpaceThread = new CheckSpaceThread(this, request, path);
        checkSpaceThread.start();
    }

    public void playVOD(int mode, VODItem vod) {
        String path = null;
        if(mLocalDBManager.isDownloaded(vod.getID())) {
            path = LocalDBManager.getDownloadedVOD(vod.getID()).getPath();
            LogUtil.d("Downloaded item! Play from the local storage");
        } else {
            path = vod.getVideoPath();
            LogUtil.d("Not downloaded! Play online");
        }
        //SednDBClient.insertVODHistory(vod.getID());
        RetrofitClient.insertVODHistory(vod.getID());
        playWithVideoPath(mode, vod.getName(), path);
    }

    public void playWithVideoPath(int mode, String title, String videoPath) {

        if(videoPath == null) return;

        LogUtil.d("play url : " + title + ", " + videoPath);
        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
        intent.putExtra("PLAY_MODE", mode);
        intent.putExtra("TITLE", title);
        intent.putExtra("URI", videoPath);
        intent.putExtra("CAPTION", "");
        intent.putExtra("TEMPLATE", 1);
        startActivityForResult(intent, REQUEST_CODE_PLAYER);

    }

    public void startBroadcast(Intent broadcastIntent) {
        LogUtil.d("startBroadcast-S");
        Intent intent = new Intent(MainActivity.this, ScheduleReadyActivity.class);
        intent.putExtra("PlayInfo", broadcastIntent);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        LogUtil.d("startBroadcast-E");
    }

    private void resetSTB() {
        // 다운로드 폴더 삭제
        File downloadDir = LocalDBManager.getDownloadFolder();
        Utils.emptyDir(downloadDir);

        // 로고, 배경 삭제
        File configDir = LocalDBManager.getConfigFolder();
        Utils.emptyDir(configDir);

        // SharedPrefference 삭제
        Utils.removeAllSP(MainActivity.this);

        // 로컬 DB 삭제
        mLocalDBManager.close();
        deleteDatabase(LocalDBManager.DATABASE_NAME);
    }

    private void rebootSTB() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class sednPackageInstallObserver extends PackageInstallObserver {
        public sednPackageInstallObserver() {
            super();
        }

        @Override
        public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras) {
            LogUtil.d("Installation complete!");    // 현재 구조로는 이 코드는 실행되지 않는다.

            Toast.makeText(SednApplication.app, "onPackageInstalled", Toast.LENGTH_SHORT).show();
        }
    }

    public void firmwareDownloadAndUpdate() {
        Handler firmwareHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // 장비 초기화 -> 업데이트시 기존 정보 유지
                //resetSTB();

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mApp.gNewFirmwarePath));
                request.setTitle("Firmware");
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOCUMENTS, "SednLauncher_" + mApp.gNewFirmwareVersion + ".apk");
                mDownloadManager.enqueue(request);
            }
        };
        firmwareHandler.sendEmptyMessageDelayed(0, 5000);

        firmwareHandler.post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder alert = new AlertDialog.Builder(mApp.mCurrentContext);
                alert.setMessage(getResources().getString(R.string.msg_firmware_update_alert));
                alert.show();
            }
        });
    }

    private void installNewPackage(String pkgPath) {

        // 설치중임을 알린다
        Intent intent = new Intent("android.intent.action.SEDN_INSTALLING");
        intent.putExtra("path", pkgPath);
        sendBroadcast(intent);

        // 설치 시작
        PackageManager pm = getPackageManager();
        sednPackageInstallObserver observer = new sednPackageInstallObserver();

        pm.installPackage(Uri.parse("file:"+pkgPath), observer, PackageManager.INSTALL_REPLACE_EXISTING, "com.inucreative.sednlauncher");
    }


    /**
     * CEC Enable 상태 가져오기
     */
    private void initCecFun() {
        if(null == mHdmiCecManager) {
            mHdmiCecManager = new HdmiCecManager(this);
        }

        String str = mHdmiCecManager.getCurConfig();
        if (!mHdmiCecManager.remoteSupportCec()) {
            //Toast.makeText(this, this.getResources().getString(R.string.toast_cec), Toast.LENGTH_LONG).show();
            return;
        }

        // get rid of '0x' prefix
        int cec_config = Integer.valueOf(str.substring(2, str.length()), 16);
        Log.d("CecControl", "cec config str:" + str + ", value:" + cec_config);

        if ((cec_config & HdmiCecManager.MASK_FUN_CEC) != 0) {
            // Cec Enabled

        } else {
            // Cec Disabled
        }
        mHdmiCecManager.setCecEnv(cec_config);
    }


    /**
     * 다운로드 완료 후 임시파일명을 원래 파일명으로 변경한다.
     * @param filePath
     * @param vodId
     */
    private void renameVodFileName(String filePath, String vodId) {

        String downDir = LocalDBManager.getDownloadFolder().getAbsolutePath();

        File from = new File(filePath);
        File to = new File(downDir, vodId + ".mp4");
        from.renameTo(to);
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            //| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            //| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            //| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            //| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );}
    }


    /**
     * 현재 다운로드 중 또는 대기중인지 여부를 반환한다.
     * @param vodId
     * @return
     */
    private boolean isDownloadingOrEnQueue(String vodId) {

        boolean bIsDownloading=false;

        DownloadManager.Query query = null;
        Cursor c = null;

        query = new DownloadManager.Query();
        if(query!=null) {
            query.setFilterByStatus(DownloadManager.STATUS_RUNNING|DownloadManager.STATUS_PENDING);
        } else {
            return false;
        }

        c = mDownloadManager.query(query);
        boolean bExist = c.moveToFirst();

        while(bExist) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            String title = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE));
            String curVodId = title.substring( ("VOD").length());

            if(curVodId.equals(vodId)) {
                if(DownloadManager.STATUS_PENDING == status || DownloadManager.STATUS_RUNNING == status) {
                    bIsDownloading = true;
                    break;
                }
            }

            bExist = c.moveToNext();
        }

        return bIsDownloading;
    }

    /**
     * VOD File Download Receiver
     * ghlee 2017.4.27
     */
    private BroadcastReceiver mVodDownloadSuccessReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            {

                String action = intent.getAction();

                if(action.equals(FileDownloadService.ACTION_DOWNLOAD_RESULT)) {
                    LogUtil.d("ACTION_DOWNLOAD_RESULT called");

                    boolean bIsSuccess = intent.getBooleanExtra("result", false);

                    if(bIsSuccess) {
                        String id = intent.getStringExtra("vodId");
                        String type = intent.getStringExtra("type");

                       if(type.equals("BATCH_DOWN")) {
                            LogUtil.d("BATCH Down complete - " + id);

                            if( 0 == FileDownloadService.getCurrentQueueSize()) {
                                showToast(R.string.msg_batch_down_complete);
                            }
                        }

                        if(mApp.mCurrentContext instanceof MainActivity) {

                            if (type.equals("VOD")) {
                                showToast(getResources().getString(R.string.msg_download_end));

                                SednListAdapter adapter = null;
                                switch (mLauncherMode) {
                                    case MODE_VOD:
                                        adapter = mVODDataAdapter;
                                        break;
                                    case MODE_MYPAGE:
                                        adapter = mMypageItemAdapter;
                                        if (mMypageState == MYPAGE_STATE_VODList || mMypageState == MYPAGE_STATE_Preview) {
                                            LogUtil.d(mMypageItemAdapter.getSelectedItem().getID() + " = " + id);
                                            if (mMypageItemAdapter.getSelectedItem().getID().equals(id)) {
                                                vMypageContentPreviewButton[2].setImageResource(R.drawable.icon_download_trash);
                                            }
                                        }
                                        break;
                                    case MODE_SEARCH:
                                        adapter = mSearchResultAdapter;
                                        break;
                                }

                                if (adapter != null)
                                    adapter.notifyDataSetChanged();
                            }
                        }

                        // 다운로드 후 남은 용량 업데이트
                        updateSTBStatus();
                    }
                    else {
                        showToast(getResources().getString(R.string.msg_download_failed));
                    }

                }
            }
        }
    };

    /**
     * 저장가능여부 체크해서 부족하면 오래된 파일순으로 삭제 후 다운로드 시작
     * - VOD 파일 다운로드용으로 사용 (FileDownloadService)
     */
    class CheckSpaceThreadForService extends Thread {
        MainActivity mainActivity;
        String mTitle;
        VODItem mVod;

        public CheckSpaceThreadForService(MainActivity activity, String title, VODItem vod) {
            mainActivity = activity;
            mTitle = title;
            mVod = vod;
        }

        @Override
        public void run() {
            long file_size = 0L;

            try {
                URL url = new URL(mVod.getVideoPath());
                URLConnection connection = url.openConnection();
                connection.connect();
                file_size = (long)(connection.getContentLength());
            } catch (Exception e) {
                e.printStackTrace();
            }

            long freeSpace = Utils.freeSpaceInBytes(LocalDBManager.getDownloadFolder().getAbsolutePath());
            LogUtil.d("downloading file size " + file_size + ", free " + freeSpace);

            boolean canDownload = true;
            while(file_size > freeSpace) {
                canDownload = deleteOldestFile();
                if(!canDownload) break; // no more space. 다운로드 대상 파일이 전체 디스크 용량보다 큼
                freeSpace = Utils.freeSpaceInBytes(LocalDBManager.getDownloadFolder().getAbsolutePath());
                LogUtil.d("new free space " + freeSpace);
            }

            if(canDownload) {
                String path = LocalDBManager.getDownloadFolder().getAbsolutePath() + File.separator + mVod.getID() + ".mp4.temp";

                File file = new File(path);
                if(file.exists()) {
                    file.delete();
                }

                FileDownloadService.startActionDownload(getApplicationContext(), mTitle, mVod.getVideoPath(), path, mVod.getID());
            }
        }

        private boolean deleteOldestFile() {
            File downDir = LocalDBManager.getDownloadFolder();
            File[] fileList= downDir.listFiles();

            if(fileList.length == 0) return false;

            long oldestModified = Long.MAX_VALUE;
            int oldestIndex = -1;
            for(int i=0; i<fileList.length; i++) {
                long lastModified = fileList[i].lastModified();
                if(oldestModified > lastModified) {
                    oldestModified = lastModified;
                    oldestIndex = i;
                }
            }

            LogUtil.d("deleteing oldest " + fileList[oldestIndex].toString());

            fileList[oldestIndex].delete();
            return true;
        }
    }

    /**
     * 저장공간 체크 후 다운로드 시작
     * @param title
     * @param vod
     */
    public void ensureFreeSpaceAndDownloadForService(String title, VODItem vod) {
        CheckSpaceThreadForService checkSpaceThread = new CheckSpaceThreadForService(this, title, vod);
        checkSpaceThread.start();
    }

    private BroadcastReceiver mSdcardMountReceiver = null;

    /**
     * SDCARD Mount/Unmount BR 등록
     */
    void registSdcardMountBR() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addDataScheme("file");

        mSdcardMountReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                // SATA HDD/SSD가 장착된 상태에서는 SDHC 는 무시한다.
                if(SednApplication.mUseSataHdd)
                    return;

                if(action.equals(Intent.ACTION_MEDIA_MOUNTED) || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {

                    new Handler().postDelayed(() -> {
                        File file = new File(SednApplication.mSdcardPath);
                        if ( !file.exists() )
                        {
                            file.mkdirs();
                        }

                        if(file.canWrite()) {
                            SednApplication.mUseSDCARD = true;
                        }
                        else {
                            SednApplication.mUseSDCARD = false;
                        }

                        // 남은 용량 업데이트
                        updateSTBStatus();
                    }, 3000);
                }
            }
        };

        registerReceiver(mSdcardMountReceiver, filter);
    }



    public interface StbConfigService {
        @GET("sedn_stb_config.php")
        Call<StbConfig> getStbConfig(@Query("mac") String mac);
    }

    /**
     * 셋탑박스 기본 환경설정 읽어오기
     * @param mac
     */
    public void retrofit_getConfig(String mac) {

        final String sMac = mac;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SednApplication.SEDN_MANAGER_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        StbConfigService service = retrofit.create(StbConfigService.class);
        final Call<StbConfig> config = service.getStbConfig(sMac);

        config.enqueue(new Callback<StbConfig>() {
            @Override
            public void onResponse(Call<StbConfig> call, Response<StbConfig> response) {

                if(response.isSuccessful()) {
                    StbConfig config = response.body();

                    mApp.gSednServer = Utils.getServiceURL(mApp);
                    if(mApp.gSednServer.isEmpty()) {
                        mApp.gSednServer = config.cms_ip;
                        Utils.setServiceURL(mApp, config.cms_ip);
                    }

                    mApp.gSednServerPort = Utils.getServicePort(mApp);
                    if(mApp.gSednServerPort.isEmpty()) {
                        mApp.gSednServerPort = config.cms_port;
                        Utils.setServicePort(mApp, config.cms_port);
                    }

                    mApp.gSednPushPort = Utils.getPushPort(mApp);
                    if(mApp.gSednPushPort.isEmpty()) {
                        mApp.gSednPushPort = config.cms_push_port;
                        Utils.setPushPort(mApp, config.cms_push_port);
                    }

                    mApp.gDBIP = config.db_ip;
                    mApp.gDBPort = config.db_port;
                    mApp.gDBUserID = config.db_user;
                    mApp.gDBPW = config.db_pwd;
                    mApp.gDBName = config.db_name;

                    // 통합 Manager 서버로부터 환경설정값을 읽어온 이후에 초기화
                    initSetTopBox();
                }
                else {
                    processManagerServerError();
                }
            }

            @Override
            public void onFailure(Call<StbConfig> call, Throwable t) {
                processManagerServerError();
            }

        });
    }

    /**
     * 매니저서버 오류일때 기본값 설정
     */
    private void processManagerServerError() {

        mApp.gDBIP = Utils.getServiceURL(mApp);
        mApp.gDBPort = "3306";
        mApp.gDBName = "vcms";
        mApp.gDBUserID = "root";
        mApp.gDBPW = "mysql()dlsn";

        initSetTopBox();
    }

    /**
     * 현재 날씨정보를 얻어와서 화면을 갱신한다
     */
    private void getWeather() {

        if(mApp.mSTBLocation == null) return;

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
                mWeatherImage = mWeatherIconMap.get(icon);

                double temp_val = obj.getJSONObject("main").getDouble("temp") - 273.15; // Kelvin 온도로 내려옴
                double round_val = Math.round(temp_val*10d) / 10d;


                mTemperature = String.valueOf(round_val) + getResources().getString(R.string.celsius_mark);
                LogUtil.d("my weather : " + icon + ", " + temp_val);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ivWeather.setImageResource(mWeatherImage);
                        tvTemperature.setText(mTemperature);
                    }
                });
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        getApplicationContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
    }

    /**
     * 볼륨변경 이벤트 처리를 위해서 등록한다.
     */
    private void registVolumeChangedListener() {
        mSettingsContentObserver = new SettingsContentObserver(this,new VolumeChangedHandler());
        getApplicationContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver );
    }


    /**
     * 볼륨 변경됐을때 수신됨
     */
    class VolumeChangedHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            int nVolume = msg.arg1;
            Log.d("Vol", "Volume Changed: " + nVolume);

            mRemoteServer.sendVolumeToClient( String.valueOf(nVolume));
        }
    }
}
