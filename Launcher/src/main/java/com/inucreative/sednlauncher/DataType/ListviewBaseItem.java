package com.inucreative.sednlauncher.DataType;

/**
 * Created by Jskim on 2016-06-22.
 */
public class ListviewBaseItem {
    private String mID;
    private String mName;

    public ListviewBaseItem(String id, String name) {
        mID = id;
        mName = name;
    }

    public String getID() { return mID; }
    public String getName() {
        return mName;
    }
}
