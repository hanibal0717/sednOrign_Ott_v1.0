package com.inucreative.sednlauncher.Client.Model;

import java.util.List;

/**
 * Created by apple on 2017. 5. 24..
 */

public class ScheduleResponse {

    public String id;
    public String name;
    public String start;
    public String end;
    public String source_type;
    public String caption;
    public int caption_size;
    public int caption_speed;
    public String caption_text_color;
    public String caption_bg_color;
    public String live_stream_url;

    public List<VOD_Json> vodlist;

    public class VOD_Json {
        public String vod_id;
        public String file_path;
        public String vod_play_time;
    }
}
