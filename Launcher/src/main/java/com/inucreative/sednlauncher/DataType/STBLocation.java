package com.inucreative.sednlauncher.DataType;

/**
 * Created by apple on 2017. 3. 13..
 */

public class STBLocation {
    Double mLat;
    Double mLon;

    public STBLocation(Double lat, Double lon) {
        mLat = lat;
        mLon = lon;
    }

    public Double getLatitude() {
        return mLat;
    }
    public Double getLogitude() {
        return mLon;
    }
}
