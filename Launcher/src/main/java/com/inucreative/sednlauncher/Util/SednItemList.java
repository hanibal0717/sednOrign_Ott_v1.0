package com.inucreative.sednlauncher.Util;

import android.content.Context;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;

import com.inucreative.sednlauncher.Adapter.SednListAdapter;
import com.inucreative.sednlauncher.DataType.ListviewBaseItem;
import com.inucreative.sednlauncher.R;

import java.util.ArrayList;

/**
 * Created by JohnPersonal on 2016-06-10.
 */
public class SednItemList {
    public static SednListAdapter BuildList(Context context, ListView listview, ImageView upArrow, ImageView downArrow
                                    , ArrayList<ListviewBaseItem> items, OnSednItemSelectListener contentSelectListener, View toTop, View toBottom, View toLeft, View toRight
                                    , int rscBackground, int numVisibleItem, float paddingPercent, int textGravity, float marginLeftPercent) {
        SednListAdapter adapter = new SednListAdapter(context, R.layout.listview_category, items, listview, rscBackground, contentSelectListener,
                toTop, toBottom, toLeft, toRight, numVisibleItem, paddingPercent, textGravity, marginLeftPercent);
        listview.setAdapter(adapter);
        listview.setItemsCanFocus(true);
        final ImageView mUp = upArrow;
        final ImageView mDown = downArrow;
        listview.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                LogUtil.d("onScroll " + view.getVisibility());
                if(view.getVisibility() == View.VISIBLE) {
                    if (firstVisibleItem > 0)
                        mUp.setVisibility(View.VISIBLE);
                    else
                        mUp.setVisibility(View.INVISIBLE);

                    if (firstVisibleItem + visibleItemCount < totalItemCount)
                        mDown.setVisibility(View.VISIBLE);
                    else
                        mDown.setVisibility(View.INVISIBLE);
                } else {
                    mUp.setVisibility(View.INVISIBLE);
                    mDown.setVisibility(View.INVISIBLE);
                }
            }
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }
        });

        return adapter;
    }

    public interface OnSednItemSelectListener {
        void onSednItemSelect(ListviewBaseItem item, int index, float topPercent, float heightPercent, boolean setFocus);
    }
}
