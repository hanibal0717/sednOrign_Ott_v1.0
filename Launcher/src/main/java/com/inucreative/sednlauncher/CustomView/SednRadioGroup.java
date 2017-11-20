package com.inucreative.sednlauncher.CustomView;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.RadioButton;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jskim on 2016-11-17.
 */
public class SednRadioGroup {
    List<RadioButton> mRadios = new ArrayList<>();
    int mSelectedIndex;

    public SednRadioGroup(Activity activity, View.OnKeyListener keyListener, int... radioIDs) {
        for(int i = 0; i < radioIDs.length; i++) {
            RadioButton rb = (RadioButton)activity.findViewById(radioIDs[i]);
            if(rb != null) {
                //LogUtil.d("SednRadioGroup add child - " + rb.toString());
                rb.setTag(i);
                rb.setOnClickListener(onClick);
                rb.setOnKeyListener(keyListener);
                mRadios.add(rb);
            }
        }
        mSelectedIndex = 0;
    }

    View.OnClickListener onClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            checkButtonView((RadioButton)v);
        }
    };

    private void checkButtonView(RadioButton button) {
        for(RadioButton rb : mRadios) {
            //LogUtil.d("SednRadioGroup uncheck child - " + rb.toString());
            rb.setChecked(false);
        }

        //LogUtil.d("SednRadioGroup check child - " + button.toString());
        button.setChecked(true);
        mSelectedIndex = (int)button.getTag();
    }

    public int size() { return mRadios.size(); };
    public int getSelectedIndex() {
        return mSelectedIndex;
    }
    public RadioButton getButtonView(int index) {
        return mRadios.get(index);
    }

    public void checkButton(int index) {
        checkButtonView(getButtonView(index));
    }

    public void setEnabled(int index, boolean enable) {
        RadioButton button = mRadios.get(index);
        button.setEnabled(enable);
        if(enable)
            button.setTextColor(Color.WHITE);
        else
            button.setTextColor(Color.GRAY);
    }
}
