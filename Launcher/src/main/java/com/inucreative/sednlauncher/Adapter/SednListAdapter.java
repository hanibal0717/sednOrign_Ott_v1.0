package com.inucreative.sednlauncher.Adapter;

import android.app.Instrumentation;
import android.content.Context;
import android.support.percent.PercentLayoutHelper;
import android.support.percent.PercentRelativeLayout;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.inucreative.sednlauncher.Activity.MainActivity;
import com.inucreative.sednlauncher.DataType.ListviewBaseItem;
import com.inucreative.sednlauncher.DataType.VODItem;
import com.inucreative.sednlauncher.R;
import com.inucreative.sednlauncher.Util.SednItemList;
import com.inucreative.sednlauncher.Util.LogUtil;

import java.util.ArrayList;

/**
 * Created by jinsupkim on 2016. 6. 7..
 */
public class SednListAdapter extends ArrayAdapter<ListviewBaseItem> {
    private Context mContext;
    private ArrayList<ListviewBaseItem> mItems;
    private ListView mParent;
    private int mNumVisibleItem;
    private float mPaddingPercent;
    private int mSelectedPosition;
    private int mBackgroundSelector;
    private int mTextGravity;
    private float mMarginLeftPercent;
    private View mToTop, mToBottom, mToLeft, mToRight;
    private SednItemList.OnSednItemSelectListener mContentSelectListener;
    private View[] contentView;

    // scroll시 pixsel단위로 상하 움직임 방지
    private int mFirstVisiblePosition;

    private static final int TIMESTAMP_TAG = 0xffffffff;
    private static final int FOCUS_TAG = 0xfffffffe;

    public SednListAdapter(Context context, int resourceID, ArrayList<ListviewBaseItem> items, View parent, int backgroundSelector, SednItemList.OnSednItemSelectListener contentSelectListener,
                           View toTop, View toBottom, View toLeft, View toRight, int numVisibleItem, float paddingPercent, int textGravity, float marginLeftPercent) {
        super(context, resourceID, items);
        mContext = context;
        mItems = items;
        mParent = (ListView)parent;
        mBackgroundSelector = backgroundSelector;
        mContentSelectListener = contentSelectListener;
        mNumVisibleItem = numVisibleItem;
        mPaddingPercent = paddingPercent;
        mSelectedPosition = -1;
        mTextGravity = textGravity;
        mMarginLeftPercent = marginLeftPercent;
        mToTop = toTop;
        mToBottom = toBottom;
        mToLeft = toLeft;
        mToRight = toRight;

        mFirstVisiblePosition = 0;
    }

    public void setItems(ArrayList<ListviewBaseItem> list) {
        contentView = new View[list.size()];
        for(int i=0; i<list.size(); i++)
            contentView[i] = null;

        mItems.clear();
        for(ListviewBaseItem item: list)
            mItems.add(item);

        mFirstVisiblePosition = 0;

        notifyDataSetChanged();
    }

    public int size() {
        return mItems.size();
    }

    public ListviewBaseItem getSelectedItem() {
        return mItems.get(mSelectedPosition);
    }

    public void setSelected(int position) {
        mSelectedPosition = position;
    }

    public void setToTop(View v) {
        mToTop = v;
    }
    public void setToLeft(View v) {
        mToLeft = v;
    }
    public void setToRight(View v) {
        mToRight = v;
    }

    private void selectItem(View v, boolean setFocus) {
        mSelectedPosition = (int)v.getTag();
        LogUtil.d("select item position : " + mSelectedPosition);
        mParent.setItemChecked(mSelectedPosition, true);
        float totalHeight = ((View)mParent.getParent()).getHeight();    // listlayout 상위의 view에 content_preview가 붙으므로...
        mContentSelectListener.onSednItemSelect(mItems.get(mSelectedPosition), mSelectedPosition, (float)(v.getTop() + mParent.getTop()) / totalHeight, (float)(v.getHeight()) / totalHeight, setFocus);
    }
    private void makeKeyEvent(int keyCode) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                (new Instrumentation()).sendKeyDownUpSync(keyCode);
            }
        }).start();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = contentView[position];

        if(v == null) {
            LogUtil.d("create new");
            LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            contentView[position] = li.inflate(R.layout.listview_category, null);
            v = contentView[position];
            v.setBackgroundResource(mBackgroundSelector);
            v.setTag(position);


            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean focusClick = false;
                    if(v.getTag(FOCUS_TAG) != null) {
                        if((boolean)v.getTag(FOCUS_TAG)) {
                            focusClick = true;
                        }
                        v.setTag(FOCUS_TAG, false);
                    }
                    LogUtil.d("Click state - " + focusClick);
                    selectItem(v, !focusClick);
                }
            });
            v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
               @Override
               public void onFocusChange(View v, boolean hasFocus) {

                   TextView tv = (TextView)v.findViewById(R.id.tvName);
                   long repeatGap = Long.MAX_VALUE;
                   if(v.getTag(TIMESTAMP_TAG) != null)
                       repeatGap = System.currentTimeMillis() - (long)v.getTag(TIMESTAMP_TAG);
                   v.setTag(TIMESTAMP_TAG, System.currentTimeMillis());
                   LogUtil.d("ListviewBaseItem Focus Changed " + tv.getText() + " : " + hasFocus + ", gap : " + repeatGap);

                   if(hasFocus && repeatGap > 200) {
                       v.setTag(FOCUS_TAG, true);
                       makeKeyEvent(KeyEvent.KEYCODE_DPAD_CENTER);
                   }
               }
            });

            v.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    int position = (int)v.getTag();
                    if(event.getAction() == KeyEvent.ACTION_DOWN) {
                        LogUtil.d("sednlistadapter keycode : " + keyCode);
                        switch(keyCode) {
                            case KeyEvent.KEYCODE_DPAD_LEFT:
                                if(mToLeft != null) {
                                    // left가 있는 경우에는 back키로 동작
                                    makeKeyEvent(KeyEvent.KEYCODE_BACK);
                                }
                                return true;
                            case KeyEvent.KEYCODE_DPAD_RIGHT:
                                // right가 있는 경우에는 click으로 동작
                                if(mToRight != null) {
                                    selectItem(v, true);
                                }
                                return true;
                            case KeyEvent.KEYCODE_DPAD_UP:
                                {
                                    int nextPosition = 0;

                                    if(position > 0) {
                                        if (mFirstVisiblePosition == position)
                                            mFirstVisiblePosition--;
                                        nextPosition = position - 1;
                                    } else {
                                        int numDisplayed = mNumVisibleItem > mItems.size() ? mItems.size() : mNumVisibleItem;
                                        mFirstVisiblePosition = mItems.size() - numDisplayed;
                                        nextPosition = mItems.size() - 1;
                                    }
                                    int offset = 0;
                                    if(mFirstVisiblePosition < nextPosition)
                                        offset = mParent.getChildAt(nextPosition - mFirstVisiblePosition).getTop();
                                    mParent.setSelectionFromTop(nextPosition, offset);
                                }
                                return true;
                            case KeyEvent.KEYCODE_DPAD_DOWN:
                                {
                                    int nextPosition = 0;

                                    if(position < mItems.size() - 1) {
                                        if (position - mFirstVisiblePosition + 1 == mNumVisibleItem)
                                            mFirstVisiblePosition++;
                                        nextPosition = position + 1;
                                    } else {
                                        mFirstVisiblePosition = 0;
                                        nextPosition = 0;
                                    }

                                    int numDisplayed = mNumVisibleItem > mItems.size() ? mItems.size() : mNumVisibleItem;
                                    int offset = mParent.getChildAt(numDisplayed - 1).getTop();
                                    if(mFirstVisiblePosition + numDisplayed - 1 > nextPosition) {
                                        offset = mParent.getChildAt(nextPosition - mFirstVisiblePosition).getTop();
                                    }
                                    mParent.setSelectionFromTop(nextPosition, offset);
                                }
                                return true;
                        }
                    }
                    return false;
                }
            });

        }
        else {
            int tag_pos = (int)v.getTag();
            TextView temp = (TextView)v.findViewById(R.id.tvName);
            LogUtil.d("View exist : " + position + ", " + tag_pos + ", " + temp.getText());
        }

        v.setTag(position);

        int paddingHeight = Math.round((float)mParent.getHeight() * mPaddingPercent / 100f);
        int itemHeight = (mParent.getHeight() - paddingHeight * (mNumVisibleItem - 1)) / mNumVisibleItem;
        PercentRelativeLayout.LayoutParams params = new PercentRelativeLayout.LayoutParams(PercentRelativeLayout.LayoutParams.MATCH_PARENT, itemHeight);
        v.setLayoutParams(params);
        mParent.setDividerHeight(paddingHeight);

        ListviewBaseItem item = mItems.get(position);

        // 성능개선 위해 viewholder 패턴 적용 검토
        if(item != null) {
            TextView tvName = (TextView)v.findViewById(R.id.tvName);
            tvName.setText(item.getName());
            tvName.setGravity(mTextGravity);

            PercentRelativeLayout.LayoutParams tvNameLayoutParamsparams = (PercentRelativeLayout.LayoutParams) tvName.getLayoutParams();
            PercentLayoutHelper.PercentLayoutInfo info = tvNameLayoutParamsparams.getPercentLayoutInfo();
            info.leftMarginPercent = mMarginLeftPercent;
            LogUtil.d("item name : " + tvName.getText() + ", " + v.hasFocus());

            if(item instanceof VODItem) {
                ImageView ivBookmarked = (ImageView) v.findViewById(R.id.ivBoomarked);
                if (((MainActivity) mContext).mLocalDBManager.isBookmarked(item.getID())) {
                    ivBookmarked.setVisibility(View.VISIBLE);
                } else {
                    ivBookmarked.setVisibility(View.INVISIBLE);
                }

                ImageView ivDownloaded = (ImageView) v.findViewById(R.id.ivDownloaded);
                if (((MainActivity) mContext).mLocalDBManager.isDownloaded(item.getID())) {
                    ivDownloaded.setVisibility(View.VISIBLE);
                } else {
                    ivDownloaded.setVisibility(View.INVISIBLE);
                }
            }
        }

        return v;
    }
}
