package com.inucreative.sednlauncher.Threads;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import com.inucreative.sednlauncher.DataType.ListviewBaseItem;
import com.inucreative.sednlauncher.DataType.MenuItem;
import com.inucreative.sednlauncher.DataType.VODItem;
import com.inucreative.sednlauncher.SednApplication;
import com.inucreative.sednlauncher.Util.LogUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Jskim on 2016-08-04.
 */
public class LocalDBManager extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "SednLocalDatabase.db";
    private static final int DATABASE_VERSION = 1;

    Context mContext;
    SQLiteDatabase mDB;

    public LocalDBManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        LogUtil.d("Database constructor called");
        mContext = context;

        String dbPath=mContext.getDatabasePath(DATABASE_NAME).getPath();
        LogUtil.d("Database path : " + dbPath);

        mDB = getWritableDatabase();
    }

    private void execSQL(SQLiteDatabase db, String sql) {
        try {
            db.execSQL(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onCreate(SQLiteDatabase db) {
        LogUtil.d("creating Database");
        String createSQL;

        createSQL = "create table MENU ("
                + "ID       text primary key, "
                + "UP_ID    text, "
                + "NAME     text)";
        execSQL(db, createSQL);

        createSQL = "create table VOD ("
                + "ID               text primary key, "
                + "TITLE            text, "
                + "MENU             text, "
                + "THUMBNAIL_PATH   text, "
                + "VIDEO_PATH       text, "
                + "REGISTER_DT      text, "
                + "HIT              integer, "
                + "PLAY_TIME        text, "
                + "RESOLUTION       text, "
                + "FILE_FORMAT      text, "
                + "BITRATE          text, "
                + "VIDEO_CODEC      text, "
                + "AUDIO_CODEC      text)";
        execSQL(db, createSQL);

        createSQL = "create table BOOKMARK ("
                + "VOD_ID           text primary key, "
                + "TIMESTAMP        integer)";
        execSQL(db, createSQL);

        createSQL = "create table SCHEDULE ("
                + "ID           text primary key, "
                + "NAME         text, "
                + "START        text, "
                + "END          text, "
                + "SOURCE_TYPE  text, "
                + "VOD_LIST     text, "
                + "STREAM_URL   text)";
        execSQL(db, createSQL);
    }

    public void onOpen(SQLiteDatabase db) {
        LogUtil.d("Opening database");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LogUtil.d("Upgrading database");
    }

    public void clearMenu() {
        execSQL(mDB, "delete from MENU");
    }

    public void putMenu(String id, String up_id, String name) {
        String sql = "insert into MENU (ID, UP_ID, NAME ) values ('" + id + "', '" + up_id + "', '" + name + "')";
        execSQL(mDB, sql);
    }

    public void clearVOD() {
        execSQL(mDB, "delete from VOD");
    }

    public void putVOD(String id, String title, String menu, String thumbPath, String videoPath, String registerDT, String hit, String playTime, String resolution, String fileFormat, String bitrate, String videoCodec, String audioCodec) {
        String sql = "insert into VOD (ID, TITLE, MENU, THUMBNAIL_PATH, VIDEO_PATH, REGISTER_DT, HIT, PLAY_TIME, RESOLUTION, FILE_FORMAT, BITRATE, VIDEO_CODEC, AUDIO_CODEC) values ('" + id + "', '" + title + "', '" + menu + "', '" + thumbPath + "', '" + videoPath + "', '" + registerDT + "', " + hit + ", '" + playTime + "', '" + resolution + "', '" + fileFormat + "', '" + bitrate + "', '" + videoCodec + "', '" + audioCodec + "')";
        execSQL(mDB, sql);
    }

    public ArrayList<ListviewBaseItem> getRootMenu() {
        ArrayList<ListviewBaseItem> result = new ArrayList<>();

        String sql = "select ID, NAME from MENU where UP_ID = 1";
        Cursor cursor = mDB.rawQuery(sql, null);

        while(cursor.moveToNext()) {
            String id = cursor.getString(0);
            String name = cursor.getString(1);
            MenuItem item = new MenuItem(id, name);
            result.add(item);
        }
        cursor.close();

        return result;
    }

    public ArrayList<ListviewBaseItem> getSubMenu(String up_id) {
        ArrayList<ListviewBaseItem> result = new ArrayList<>();

        String sql = "select ID, NAME from MENU where UP_ID = " + up_id;
        Cursor cursor = mDB.rawQuery(sql, null);

        while(cursor.moveToNext()) {
            String id = cursor.getString(0);
            String name = cursor.getString(1);
            MenuItem item = new MenuItem(id, name);
            result.add(item);
        }
        cursor.close();

        return result;
    }

    public ArrayList<ListviewBaseItem> getVODList(String menu_id) {
        String condition = "where MENU like '%" + menu_id + "%'";
        return getVODListByCondition(condition);
    }

    public String getMenuName(String menu_id) {
        String result = "";

        String sql = "select NAME from MENU where ID = " + menu_id;
        Cursor cursor = mDB.rawQuery(sql, null);

        while(cursor.moveToNext()) {
            result = cursor.getString(0);
        }
        cursor.close();

        return result;
    }

    public ArrayList<ListviewBaseItem> getVODListByCondition(String condition) {
        ArrayList<ListviewBaseItem> result = new ArrayList<>();

        String sql = "select ID, TITLE, THUMBNAIL_PATH, VIDEO_PATH, REGISTER_DT, HIT, PLAY_TIME, MENU, RESOLUTION, FILE_FORMAT, BITRATE, VIDEO_CODEC, AUDIO_CODEC from VOD " + condition;
        Cursor cursor = mDB.rawQuery(sql, null);

        while(cursor.moveToNext()) {
            String id = cursor.getString(0);
            String title = cursor.getString(1);
            String thumbPath = cursor.getString(2);
            String videoPath = cursor.getString(3);
            String regDT = cursor.getString(4);
            int hit = cursor.getInt(5);
            String playTime = cursor.getString(6);

            String resolution = cursor.getString(8);
            String fileFormat = cursor.getString(9);
            String bitrate = cursor.getString(10);
            String videoCodec = cursor.getString(11);
            String audioCodec = cursor.getString(12);

            VODItem item = new VODItem(id, title, thumbPath, videoPath, regDT, hit, playTime, resolution, fileFormat, bitrate, videoCodec, audioCodec);

            String menu = cursor.getString(7);
            String firstSet = (menu.split(","))[1];
            String categories[] = firstSet.split("`");
            //LogUtil.d("VOD menu ids " + menu + "| " + firstSet + "| " + categories[0]);
            String categoryStr = "";

            for(int i = 0; i < categories.length; i++) {
                if(i > 0) categoryStr += "/";
                categoryStr += getMenuName(categories[i]);
                item.mMenuID[i] = categories[i];
            }
            item.mCategory = categoryStr;

            result.add(item);
        }
        cursor.close();

        return result;
    }

    public ArrayList<ListviewBaseItem> getVODListWithOrderLimit(String order, int limit) {
        String condition = "order by " + order + " limit " + limit;
        return getVODListByCondition(condition);
    }

    public ArrayList<ListviewBaseItem> getRecentVODList() {
        return getVODListWithOrderLimit("REGISTER_DT desc, ID desc", 10);
    }
    public ArrayList<ListviewBaseItem> getMostVODList() {
        return getVODListWithOrderLimit("HIT desc", 5);
    }

    public ArrayList<String> getAllVODTitles() {
        ArrayList<String> result = new ArrayList<>();

        String sql = "select TITLE from VOD";
        Cursor cursor = mDB.rawQuery(sql, null);

        while(cursor.moveToNext()) {
            String title = cursor.getString(0);
            result.add(title);
        }
        cursor.close();

        return result;
    }

    public boolean isBookmarked(String vod_id) {
        boolean res = false;

        String sql = "select VOD_ID from BOOKMARK where VOD_ID = '" + vod_id + "'";
        Cursor cursor = mDB.rawQuery(sql, null);
        if(cursor.getCount() > 0)
            res = true;
        cursor.close();
        return res;
    }

    public void addToBookmark(String vod_id) {
        if(!isBookmarked(vod_id)) {
            String sql = "insert into BOOKMARK (VOD_ID, TIMESTAMP) values ('" + vod_id + "', " + System.currentTimeMillis() + ")";
            execSQL(mDB, sql);
        }
    }

    public void removeFromBookmark(String vod_id) {
        String sql = "delete from BOOKMARK where VOD_ID = '" + vod_id + "'";
        execSQL(mDB, sql);
    }

    public boolean toggleBookmark(String vod_id) {
        boolean isAdded = false;

        if(isBookmarked(vod_id))
            removeFromBookmark(vod_id);
        else {
            addToBookmark(vod_id);
            isAdded = true;
        }

        return isAdded;
    }

    public ArrayList<ListviewBaseItem> getBookmarkedVOD() {
        String condition = "A, BOOKMARK B where A.ID = B.VOD_ID order by B.TIMESTAMP desc";
        return getVODListByCondition(condition);
    }

    public void refreshBookark() {
        String sql = "delete from BOOKMARK where VOD_ID not in (select ID from VOD)";
        execSQL(mDB, sql);
    }

    public static File getConfigFolder() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
    }

    // Download 관련 API는 셋탑 파일 시스템을 직접조회한다.
    // 추후 성능 이슈 제기되면 DB에 index를 유지하는 방법으로 수정해야 함.
    public static File getDownloadFolder() {

        if(SednApplication.mUseSataHdd) {
            // SATA HDD/SSD가 장착된 상태에서는 SDHC 는 무시한다.
            return new File(SednApplication.mSataHddPath);
        }
        else if(SednApplication.mUseSDCARD) {
            return new File(SednApplication.mSdcardPath);
        }
        else {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        }
    }
    public static File getDownloadedVOD(String id) {
        return new File(getDownloadFolder().getPath() + "/" + id + ".mp4");
    }

    public void removeFile(String vod_id) {
        File file = getDownloadedVOD(vod_id);
        file.delete();
    }

    public static boolean isDownloaded(String vod_id) {
        File file = getDownloadedVOD(vod_id);
        return file.exists();
    }

    public ArrayList<ListviewBaseItem> getDownloadedVOD() {
        ArrayList<ListviewBaseItem> result = new ArrayList<>();

        File downDir = getDownloadFolder();
        String[] fileNameList = downDir.list();
        File[] fileList = downDir.listFiles();

        if(fileNameList != null) {
            for (int i = 0; i < fileNameList.length; i++) {
                String id = fileNameList[i].replace(".mp4", "");

                if(id.contains(".temp")) {
                    id = id.replace(".temp", "");
                }

                String condition = "where ID = '" + id + "'";
                ArrayList<ListviewBaseItem> vod = getVODListByCondition(condition);
                if(vod.isEmpty()) {
                    fileList[i].delete();
                } else {
                    result.addAll(vod);
                }
            }
        }
        return result;
    }

    public ArrayList<ListviewBaseItem> searchVOD(String keyword) {
        String condition = "where TITLE like '%" + keyword + "%'";
        return getVODListByCondition(condition);
    }

    public ArrayList<String> getAllVODIDs() {
        ArrayList<String> result = new ArrayList<>();

        String sql = "select ID from VOD";
        Cursor cursor = mDB.rawQuery(sql, null);

        while(cursor.moveToNext()) {
            String id = cursor.getString(0);
            result.add(id);
        }
        cursor.close();

        return result;
    }
}
