package com.inucreative.sednlauncher.Util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextPaint;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.inucreative.sednlauncher.Activity.PlayerActivity.PLAY_SCHEDULE_LIVE;
import static com.inucreative.sednlauncher.Activity.PlayerActivity.PLAY_SCHEDULE_VOD;

/**
 * Created by Jskim on 2016-07-25.
 */
public class Utils {
    public static final SimpleDateFormat sdf_YYYYMMDD_dot = new SimpleDateFormat("yyyy.MM.dd");
    public static final SimpleDateFormat sdf_HHMM = new SimpleDateFormat("HH:mm");
    public static final SimpleDateFormat sdf_YYYYMMDDHHMM = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static final SimpleDateFormat sdf_YYYYMMDDHHMMSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat sdf_YYYYMMDD = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat sdf_HHMMSS = new SimpleDateFormat("HH:mm:ss");

    private static final String SPNAME_FOR_SERVICEURL = "SednServiceURL";
    private static final String SPNAME_FOR_SERVICEPORT = "SednServicePort";
    private static final String SPNAME_FOR_PUSHPORT = "SednPushPort";
    private static final String SPNAME_FOR_STREAMINGURL = "SednStreamingURL";
    private static final String SPNAME_FOR_LOGO_IMG_YN = "SednLogoImgYN";
    private static final String SPNAME_FOR_LOGO_TEXT = "SednLogoText";
    private static final String SPNAME_FOR_LOGO_DT = "SednLogoDT";
    private static final String SPNAME_FOR_BG_IMG_YN = "SednBGImgYN";
    private static final String SPNAME_FOR_BG_DT = "SednBGDT";
    private static final String SPNAME_FOR_FW_DT = "SednFWDT";
    private static final String SPNAME_FOR_RETENTION = "SednRetention";

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sbuf = new StringBuilder();
        for(int idx=0; idx < bytes.length; idx++) {
            int intVal = bytes[idx] & 0xff;
            if (intVal < 0x10) sbuf.append("0");
            sbuf.append(Integer.toHexString(intVal).toUpperCase());
        }
        return sbuf.toString();
    }

    /**
     * Get utf8 byte array.
     * @param str
     * @return  array of NULL if error was found
     */
    public static byte[] getUTF8Bytes(String str) {
        try { return str.getBytes("UTF-8"); } catch (Exception ex) { return null; }
    }

    /**
     * Load UTF8withBOM or any ansi text file.
     * @param filename
     * @return
     * @throws java.io.IOException
     */
    public static String loadFileAsString(String filename) throws java.io.IOException {
        final int BUFLEN=1024;
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(filename), BUFLEN);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFLEN);
            byte[] bytes = new byte[BUFLEN];
            boolean isUTF8=false;
            int read,count=0;
            while((read=is.read(bytes)) != -1) {
                if (count==0 && bytes[0]==(byte)0xEF && bytes[1]==(byte)0xBB && bytes[2]==(byte)0xBF ) {
                    isUTF8=true;
                    baos.write(bytes, 3, read-3); // drop UTF8 bom marker
                } else {
                    baos.write(bytes, 0, read);
                }
                count+=read;
            }
            return isUTF8 ? new String(baos.toByteArray(), "UTF-8") : new String(baos.toByteArray());
        } finally {
            try{ is.close(); } catch(Exception ex){}
        }
    }

    /**
     * Returns MAC address of the given interface name.
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return  mac address or empty string
     */
    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac==null) return "";
                StringBuilder buf = new StringBuilder();
                for (int idx=0; idx<mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
                return buf.toString();
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    /**
     * Get IP address from first non-localhost interface
     * @param ipv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    public static String getServiceURL(Context context) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        String str_bookmark = pref.getString(SPNAME_FOR_SERVICEURL, "");

        return str_bookmark;
    }

    public static void setServiceURL(Context context, String data) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(SPNAME_FOR_SERVICEURL, data);
        editor.commit();
    }

    public static String getServicePort(Context context) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        String result = pref.getString(SPNAME_FOR_SERVICEPORT, "");

        return result;
    }

    public static String getPushPort(Context context) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        String result = pref.getString(SPNAME_FOR_PUSHPORT, "");

        return result;
    }

    public static void setServicePort(Context context, String data) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(SPNAME_FOR_SERVICEPORT, data);
        editor.commit();
    }

    public static void setPushPort(Context context, String data) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(SPNAME_FOR_PUSHPORT, data);
        editor.commit();
    }

    private static final long MEGA_BYTE = 1048576;

    /**
     * Calculates total space on disk
     * @param external  If true will query external disk, otherwise will query internal disk.
     * @return Number of mega bytes on disk.
     */
    public static int totalSpace(boolean external)
    {
        StatFs statFs = getStats(external);
        long total = (statFs.getBlockCountLong() * statFs.getBlockSizeLong()) / MEGA_BYTE;
        return (int) total;
    }


    public static int totalSpace(String path)
    {
        StatFs statFs = new StatFs(path);
        long total = (statFs.getBlockCountLong() * statFs.getBlockSizeLong()) / MEGA_BYTE;
        return (int) total;
    }


    /**
     * Calculates free space on disk
     * @param external  If true will query external disk, otherwise will query internal disk.
     * @return Number of free mega bytes on disk.
     */
    public static long freeSpaceInBytes(boolean external)
    {
        StatFs statFs = getStats(external);
        long availableBlocks = statFs.getAvailableBlocksLong();
        long blockSize = statFs.getBlockSizeLong();
        long freeBytes = availableBlocks * blockSize;

        return freeBytes;
    }

    public static long freeSpaceInBytes(String path)
    {
        StatFs statFs = new StatFs(path);

        long availableBlocks = statFs.getAvailableBlocksLong();
        long blockSize = statFs.getBlockSizeLong();
        long freeBytes = availableBlocks * blockSize;

        return freeBytes;
    }

    /**
     * Calculates occupied space on disk
     * @param external  If true will query external disk, otherwise will query internal disk.
     * @return Number of occupied mega bytes on disk.
     */
    public static int busySpace(boolean external)
    {
        StatFs statFs = getStats(external);
        long total = (statFs.getBlockCountLong() * statFs.getBlockSizeLong());
        long free  = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());

        return (int) ((total - free) / MEGA_BYTE);
    }


    public static int busySpace(String path)
    {
        StatFs statFs = new StatFs(path);
        long total = (statFs.getBlockCountLong() * statFs.getBlockSizeLong());
        long free  = (statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong());

        return (int) ((total - free) / MEGA_BYTE);
    }


    private static StatFs getStats(boolean external){
        String path;

        if (external){
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        else{
            path = Environment.getRootDirectory().getAbsolutePath();
        }

        return new StatFs(path);
    }

    public static String getScanResultSecurity(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] securityModes = { "WEP", "PSK", "EAP" };

        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }

        return "OPEN";
    }

    public static int connectToAP(WifiManager wifiManager, String ssid, String passkey) {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        String networkSSID = ssid;
        String networkPass = passkey;
        int res = -1;

        List<ScanResult> scanResultList = wifiManager.getScanResults();

        for (ScanResult result : scanResultList) {
            if (result.SSID.equals(networkSSID)) {

                String securityMode = getScanResultSecurity(result);

                if (securityMode.equalsIgnoreCase("OPEN")) {
                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    res = wifiManager.addNetwork(wifiConfiguration);
                    boolean b = wifiManager.enableNetwork(res, true);
                } else if (securityMode.equalsIgnoreCase("WEP")) {

                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.wepKeys[0] = "\"" + networkPass + "\"";
                    wifiConfiguration.wepTxKeyIndex = 0;
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    res = wifiManager.addNetwork(wifiConfiguration);
                    boolean b = wifiManager.enableNetwork(res, true);
                }

                wifiConfiguration.SSID = "\"" + networkSSID + "\"";

                wifiConfiguration.preSharedKey = "\"" + networkPass + "\"";
                wifiConfiguration.hiddenSSID = true;
                wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

                res = wifiManager.addNetwork(wifiConfiguration);
                wifiManager.enableNetwork(res, true);

                boolean changeHappen = wifiManager.saveConfiguration();
                if(res != -1 && changeHappen){

                }else{
                    //Log.d(TAG, "*** Change NOT happen");
                }
            }
        }
        return res;
    }

    public static String getSTBLogoImageYN(Context context) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        String res = pref.getString(SPNAME_FOR_LOGO_IMG_YN, "Y");

        return res;
    }

    public static void setSTBLogoImageYN(Context context, String data) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(SPNAME_FOR_LOGO_IMG_YN, data);
        editor.commit();
    }

    public static String getSTBLogoText(Context context) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        String res = pref.getString(SPNAME_FOR_LOGO_TEXT, null);

        return res;
    }

    public static void setSTBLogoText(Context context, String data) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(SPNAME_FOR_LOGO_TEXT, data);
        editor.commit();
    }

    public static Date getLastLogoDT(Context context) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        String res = pref.getString(SPNAME_FOR_LOGO_DT, null);

        Date res_date = null;
        if(res != null) {
            try {
                res_date = sdf_YYYYMMDDHHMMSS.parse(res);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return res_date;
    }

    public static void setLastLogoDT(Context context, Date data) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(SPNAME_FOR_LOGO_DT, sdf_YYYYMMDDHHMMSS.format(data));
        editor.commit();
    }

    public static String getSTBBGImageYN(Context context) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        String res = pref.getString(SPNAME_FOR_BG_IMG_YN, "Y");

        return res;
    }

    public static void setSTBBGImageYN(Context context, String data) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(SPNAME_FOR_BG_IMG_YN, data);
        editor.commit();
    }

    public static Date getLastBGDT(Context context) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        String res = pref.getString(SPNAME_FOR_BG_DT, null);

        Date res_date = null;
        if(res != null) {
            try {
                res_date = sdf_YYYYMMDDHHMMSS.parse(res);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return res_date;
    }

    public static void setLastBGDT(Context context, Date data) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(SPNAME_FOR_BG_DT, sdf_YYYYMMDDHHMMSS.format(data));
        editor.commit();
    }

    public static int getRetentionPeriod(Context context) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        int res = pref.getInt(SPNAME_FOR_RETENTION, 3);

        return res;
    }

    public static void setRetentionPeriod(Context context, int data) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(SPNAME_FOR_RETENTION, data);
        editor.commit();
    }

    public static void emptyDir(File dir) {
        if(dir.isDirectory()) {
            File[] listFile = dir.listFiles();
            for(int i=0; i<listFile.length; i++) {
                listFile[i].delete();
            }
        }
    }

    public static void removeAllSP(Context context) {
        SharedPreferences pref = context.getSharedPreferences("pref", Activity.MODE_PRIVATE);
        pref.edit().clear().commit();
    }


    /**
     * Pad a target string of text with spaces on the right to fill a target
     * width
     *
     * @param text The target text
     * @param paint The TextPaint used to measure the target text and
     *            whitespaces
     * @param width The target width to fill
     * @return the original text with extra padding to fill the width
     */
    public static CharSequence padText(CharSequence text, TextPaint paint, int width) {

        // First measure the width of the text itself
        Rect textbounds = new Rect();
        paint.getTextBounds(text.toString(), 0, text.length(), textbounds);

        /**
         * check to see if it does indeed need padding to reach the target width
         */
        if (textbounds.width() > width) {
            return text;
        }

    /*
     * Measure the text of the space character (there's a bug with the
     * 'getTextBounds() method of Paint that trims the white space, thus
     * making it impossible to measure the width of a space without
     * surrounding it in arbitrary characters)
     */
        String workaroundString = "a a";
        Rect spacebounds = new Rect();
        paint.getTextBounds(workaroundString, 0, workaroundString.length(), spacebounds);

        Rect abounds = new Rect();
        paint.getTextBounds(new char[] {
                'a'
        }, 0, 1, abounds);

        float spaceWidth = spacebounds.width() - (abounds.width() * 2);
        spaceWidth = spaceWidth - 0.55f; //보정

    /*
     * measure the amount of spaces needed based on the target width to fill
     * (using Math.ceil to ensure the maximum whole number of spaces)
     */
        int amountOfSpacesNeeded = (int)Math.ceil((width - textbounds.width()) / spaceWidth);

        // pad with spaces til the width is less than the text width
        return amountOfSpacesNeeded > 0 ? padRight(text.toString(), text.toString().length()
                + amountOfSpacesNeeded) : text;
    }

    /**
     * Pads a string with white space on the right of the original string
     *
     * @param s The target string
     * @param n The new target length of the string
     * @return The target string padded with whitespace on the right to its new
     *         length
     */
    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    public static String twoDigit(String str) {
        if(str.length() == 1) str = "0" + str;
        return str;
    }

    public static Drawable scaleImage (Resources res, Drawable image, float scaleFactor) {

        if ((image == null) || !(image instanceof BitmapDrawable)) {
            return image;
        }

        Bitmap b = ((BitmapDrawable)image).getBitmap();

        int sizeX = Math.round(image.getIntrinsicWidth() * scaleFactor);
        int sizeY = Math.round(image.getIntrinsicHeight() * scaleFactor);

        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, sizeX, sizeY, false);

        image = new BitmapDrawable(res, bitmapResized);

        return image;

    }

    /**
     * PlayerActivity용 Intent 가 동일한지 비교한다
     * 현재 방송중인 정보와 새로 전달받은 스케쥴이 동일한지 체크하기 위해서 사용
     * @param i1
     * @param i2
     * @return
     */
    public static boolean isEqualsPlayerIntent(Intent i1, Intent i2) {
        int playerMode1 = i1.getIntExtra("PLAY_MODE", 0);
        int playerMode2 = i2.getIntExtra("PLAY_MODE", 0);

        if(playerMode1 != playerMode2)
            return false;

        boolean bEquals = false;

        if(Objects.equals(i1.getStringExtra("TITLE"), i2.getStringExtra("TITLE"))
            && Objects.equals(i1.getStringExtra("CAPTION"), i2.getStringExtra("CAPTION"))
            && i1.getIntExtra("CAPTION_SIZE", 1) == i2.getIntExtra("CAPTION_SIZE", 1)
            && i1.getIntExtra("CAPTION_SPEED", 1) == i2.getIntExtra("CAPTION_SPEED", 1)
            && i1.getIntExtra("CAPTION_TEXT_COLOR", 1) == i2.getIntExtra("CAPTION_TEXT_COLOR", 1)
            && i1.getIntExtra("CAPTION_BG_COLOR", Color.TRANSPARENT) == i2.getIntExtra("CAPTION_BG_COLOR", Color.TRANSPARENT)
            && i1.getIntExtra("TEMPLATE", 1) == i2.getIntExtra("TEMPLATE", 1)
            && i1.getLongExtra("START_TIME", 0) == i2.getLongExtra("START_TIME", 0)
            && i1.getLongExtra("END_TIME", 0) == i2.getLongExtra("END_TIME", 0) ) {

            bEquals = true;
        }

        if(bEquals) {

            if (PLAY_SCHEDULE_VOD == playerMode1) {
                ArrayList<String> vodIdList1 = i1.getStringArrayListExtra("VOD_ID_LIST");
                ArrayList<String> vodIdList2 = i2.getStringArrayListExtra("VOD_ID_LIST");

                if (!vodIdList1.containsAll(vodIdList2))
                    bEquals = false;
            } else if (PLAY_SCHEDULE_LIVE == playerMode1) {
                String sURI1 = i1.getStringExtra("URI");
                String sURI2 = i2.getStringExtra("URI");

                if (!sURI1.equals(sURI2))
                    bEquals = false;
            }
        }

        return bEquals;
    }


    /**
     * SystemUI 숨기기
     */
    public static void hideSystemUI(View decorView) {

        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
//        decorView.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }


    /**
     * APK Build Date 가져오기
     * @param context
     * @return
     */
    public static String getApkBuildDate(Context context) {

        String buildDate="";

        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            ZipFile zf = new ZipFile(ai.sourceDir);
            ZipEntry ze = zf.getEntry("META-INF/MANIFEST.MF");
            long time = ze.getTime();

            buildDate = Utils.sdf_YYYYMMDD.format(new Date(time));

        } catch (Exception e) {
        }

        return buildDate;
    }

    public static String getDayOfWeek() {
        Calendar cal = Calendar.getInstance();
        String sWeek = null;

        int nWeek = cal.get(Calendar.DAY_OF_WEEK);
        if(1 == nWeek) {
            sWeek = "일요일";
        }
        else if(2 == nWeek) {
            sWeek = "월요일";
        }
        else if(3 == nWeek) {
            sWeek = "화요일";
        }
        else if(4 == nWeek) {
            sWeek = "수요일";
        }
        else if(5 == nWeek) {
            sWeek = "목요일";
        }
        else if(6 == nWeek) {
            sWeek = "금요일";
        }
        else if(7 == nWeek) {
            sWeek = "토요일";
        }

        return sWeek;
    }

}
