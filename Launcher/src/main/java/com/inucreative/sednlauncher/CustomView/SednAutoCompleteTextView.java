package com.inucreative.sednlauncher.CustomView;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.inucreative.sednlauncher.Activity.MainActivity;
import com.inucreative.sednlauncher.R;
import com.inucreative.sednlauncher.Util.LogUtil;

/**
 * Created by Jskim on 2016-06-04.
 * ASTextView - Auto Sizable TextView
 * By default, the height of TextView determines font size, leaving no margin at top and bottom
 */
public class SednAutoCompleteTextView extends AutoCompleteTextView implements TextView.OnEditorActionListener, View.OnFocusChangeListener {
    Context mContext;

    String mDefaultText;
    boolean isKeyboardShown;

    public SednAutoCompleteTextView(Context context) {
        super(context);
    }

    public SednAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SednAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mContext = context;

        isKeyboardShown = false;

        setOnEditorActionListener(this);
        setOnFocusChangeListener(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setIncludeFontPadding(false);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        return false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(hasFocus) {
            LogUtil.d("edit focused in");
                ((MainActivity) mContext).focusedEditText = (EditText) v;
                ((MainActivity) mContext).notifyRemoteClient(((EditText) v).getText().toString());
                setSelection(getText().length());
            //LogUtil.d("edit focused in " + ((MainActivity)mContext).focusedEditText);
        } else {
            ((MainActivity)mContext).focusedEditText = null;
            LogUtil.d("edit focused out");
        }
    }
}
