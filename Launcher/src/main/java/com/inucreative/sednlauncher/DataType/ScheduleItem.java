package com.inucreative.sednlauncher.DataType;

/**
 * Created by Jskim on 2016-10-08.
 */
public class ScheduleItem {
    public String image_url;
    public String name;
    public String durationStr;
    public String target;
    public String desc;
    public String dateStr;
    public String playTimeStr;

    public int start_time;  // 00:00 부터 분단위로
    public int end_time;  // 00:00 부터 분단위로
    public int color_code;
}
