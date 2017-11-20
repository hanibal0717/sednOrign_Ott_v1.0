package com.inucreative.sednlauncher.RssModel;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.io.Serializable;

/**
 * Created by apple on 2017. 3. 9..
 */
@Root(name = "item", strict = false)
public class FeedItem implements Serializable {
    @Element(name = "title")
    public String title;

}