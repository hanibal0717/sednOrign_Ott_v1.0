package com.inucreative.sednlauncher.CustomView;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.percent.PercentLayoutHelper;
import android.support.percent.PercentRelativeLayout;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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
public class ASEditText extends EditText implements TextView.OnEditorActionListener, View.OnFocusChangeListener {
    Context mContext;

    float mTextSizePercent;
    float mPaddingLeftPercent;
    String mDefaultText;
    boolean isKeyboardShown;

    public ASEditText(Context context) {
        super(context);
    }

    public ASEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ASEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mContext = context;

        isKeyboardShown = false;

        TypedArray taTextView = context.obtainStyledAttributes(attrs, R.styleable.ASTextView);
        mTextSizePercent = taTextView.getFloat(R.styleable.ASTextView_textSizePercent, 95.0f);

        TypedArray taView = context.obtainStyledAttributes(attrs, R.styleable.PercentPaddingView);
        mPaddingLeftPercent = taView.getFloat(R.styleable.PercentPaddingView_paddingLeftPercent, 0f);

        TypedArray taEditText = context.obtainStyledAttributes(attrs, R.styleable.ASEditText);
        mDefaultText = taEditText.getString(R.styleable.ASEditText_defaultText);
        //LogUtil.d("mDefaultText " + mDefaultText);

        setOnEditorActionListener(this);
        setOnFocusChangeListener(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)h * mTextSizePercent / 100);
        setPadding(Math.round((float)w * mPaddingLeftPercent / 100), 0, 0, 0);
        setIncludeFontPadding(false);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        return false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(hasFocus) {
            ((MainActivity)mContext).focusedEditText = (EditText)v;
            ((MainActivity)mContext).notifyRemoteClient(((EditText)v).getText().toString());
            setSelection(getText().length());
            LogUtil.d("edit focused in " + ((MainActivity)mContext).focusedEditText);
        } else {
            ((MainActivity)mContext).focusedEditText = null;
            LogUtil.d("edit focused out");
        }
    }
}
