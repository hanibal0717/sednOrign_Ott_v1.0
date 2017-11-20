package com.inucreative.sednlauncher.Service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.inucreative.sednlauncher.Threads.LocalDBManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * File Download Service
 * GongHee
 * 2017.4.26
 */
public class FileDownloadService extends IntentService {

    private static final String TAG = "DownloadService";

    private static final String ACTION_DOWNLOAD = "com.inucreative.sednlauncher.Service.action.DOWNLOAD";
    public static final String ACTION_DOWNLOAD_RESULT = "com.inucreative.sednlauncher.Service.action.DOWNLOAD_RESULT";

    private static final String EXTRA_PARAM_TYPE = "com.inucreative.sednlauncher.Service.extra.TYPE";
    private static final String EXTRA_PARAM_URL = "com.inucreative.sednlauncher.Service.extra.URL";
    private static final String EXTRA_PARAM_PATH = "com.inucreative.sednlauncher.Service.extra.PATH";
    private static final String EXTRA_PARAM_VODID = "com.inucreative.sednlauncher.Service.extra.VODID";

    public static ArrayList<String> queue_VodId = new ArrayList<>();

    public FileDownloadService() {
        super("FileDownloadService");
        Log.i(TAG, "FileDownloadService~");
    }

    /**
     * Activity에서 호출하는 스태틱 함수
     * @param context
     * @param type
     * @param vodUrl
     * @param vodPath
     */
    public static void startActionDownload(Context context, String type, String vodUrl, String vodPath, String vodId) {

        // 큐에 넣는다. (인텐트 서비스의 큐를 직접 제어하면 좋겠지만. 방법을 모르니 이렇게 한다.)
        queue_VodId.add(vodId);

        Intent intent = new Intent(context, FileDownloadService.class);
        intent.setAction(ACTION_DOWNLOAD);
        intent.putExtra(EXTRA_PARAM_TYPE, type);
        intent.putExtra(EXTRA_PARAM_URL, vodUrl);
        intent.putExtra(EXTRA_PARAM_PATH, vodPath);
        intent.putExtra(EXTRA_PARAM_VODID, vodId);

        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DOWNLOAD.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM_TYPE);
                final String param2 = intent.getStringExtra(EXTRA_PARAM_URL);
                final String param3 = intent.getStringExtra(EXTRA_PARAM_PATH);
                final String param4 = intent.getStringExtra(EXTRA_PARAM_VODID);
                handleActionDownload(param1, param2, param3, param4);
            }
        }
    }

    /**
     * 실제 다운로드 처리
     * @param sUrl
     * @param destPath
     * @param vodId
     */
    private void handleActionDownload(String type, String sUrl, String destPath, String vodId) {
        Log.d(TAG, "handleActionDownload start! " + sUrl);

        try {
            URL url = new URL(sUrl);
            URLConnection conection = url.openConnection();
            conection.connect();

            // download the file
            InputStream input = new BufferedInputStream(url.openStream(), 8192);

            // Output stream
            OutputStream output = new FileOutputStream(destPath);

            byte data[] = new byte[1024];

            int count;
            while ((count = input.read(data)) != -1) {

                // writing data to file
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();

            // 파일명 변경 (.temp 제거)
            String downDir = LocalDBManager.getDownloadFolder().getAbsolutePath();
            File from = new File(destPath);
            File to = new File(downDir, vodId + ".mp4");
            from.renameTo(to);

            // 결과 전송
            sendResult(true, type, vodId);

        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());

            File file = new File(destPath);
            file.delete();

            // 결과 전송
            sendResult(false, type, vodId);
        }


        Log.d(TAG, "handleActionDownload end! " + sUrl);
    }


    /**
     * BR로 결과 전송
     * @param result
     * @param type
     * @param vodId
     */
    private void sendResult(boolean result, String type, String vodId) {

        queue_VodId.remove(vodId);

        Log.d(TAG, "sendResult: " + vodId + "-Queue Size:" + queue_VodId.size());

        Intent intent = new Intent();
        intent.setAction(ACTION_DOWNLOAD_RESULT);
        intent.putExtra("result", result);
        intent.putExtra("type", type);
        intent.putExtra("vodId", vodId);
        sendBroadcast(intent);
    }

    /**
     * 이미 다운로드 큐에 있는지 체크
     * @param vodId
     * @return
     */
    public static boolean isDownloadingOrEnQueue(String vodId) {

        for (String item: queue_VodId) {
            if(item.equals(vodId)) {
                Log.d(TAG, "Already VodId!");
                return true;
            }
        }

        return false;
    }

    /**
     * 큐 크기 반환
     * @return
     */
    public static int getCurrentQueueSize() {
        return queue_VodId.size();
    }
}
