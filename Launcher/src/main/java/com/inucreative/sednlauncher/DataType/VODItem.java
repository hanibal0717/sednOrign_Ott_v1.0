package com.inucreative.sednlauncher.DataType;

import java.util.ArrayList;

/**
 * Created by JohnPersonal on 2016-06-06.
 */
public class VODItem extends ListviewBaseItem {
    private String mThumbPath;
    private String mVideoPath;
    public String mRegisterDT;
    public String mCategory;
    public String[] mMenuID;
    public int mHit;
    public String mPlayTime;
    public String mResolution;
    public String mFileFormat;
    public String mBitrate;
    public String mVideoCodec;
    public String mAudioCodec;

    public VODItem(String id, String title, String thumb_path, String video_path) {
        super(id, title);

        mThumbPath = thumb_path;
        mVideoPath = video_path;
        mMenuID = new String[]{null, null};
    }

    public VODItem(String id, String title, String thumb_path, String video_path, String register_dt, int hit, String play_time, String resolution, String file_format, String bitrate, String video_codec, String audio_codec) {
        super(id, title);

        mThumbPath = thumb_path;
        mVideoPath = video_path;
        mRegisterDT = register_dt;
        mHit = hit;
        mPlayTime = play_time;
        mMenuID = new String[]{null, null};

        mResolution = resolution;
        mFileFormat = file_format;
        mBitrate = bitrate;
        mVideoCodec = video_codec;
        mAudioCodec = audio_codec;
    }

    public String getThumbnailPath() { return mThumbPath; }
    public String getVideoPath() { return mVideoPath; }
}
