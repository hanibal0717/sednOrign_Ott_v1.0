package com.inucreative.sednlauncher.Client.Model;

import java.util.List;

/**
 * Created by apple on 2017. 5. 24..
 */

public class StbDataResponse {
    public List<StbMenu> menu;
    public List<StbVod> vod;
    public StbBanner banner;

    public class StbMenu {
        public String menu_seq;
        public String up_menu_seq;
        public String menu_name;
    }

    public class StbVod {
        public String ID;
        public String TITLE;
        public String MENU;
        public String THUMBNAIL_PATH;
        public String VIDEO_PATH;
        public String REGISTER_DT;
        public String VOD_PLAY_TIME;
        public String RESOLUTION;
        public String BITRATE;
        public String VIDEO_CODEC;
        public String AUDIO_CODEC;
        public String hit;
    }

    public class StbBanner {
        public String BANNER1_IMG_PATH;
        public String BANNER2_IMG_PATH;
    }


}
