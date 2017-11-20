package com.inucreative.sednlauncher.CustomView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.inucreative.sednlauncher.R;
import com.inucreative.sednlauncher.Util.LogUtil;

/**
 * Created by Jskim on 2016-06-04.
 * ASTextView - Auto Sizable TextView
 * By default, the height of TextView determines font size, leaving no margin at top and bottom
 */
public class ASTextView extends TextView {
    float mTextSizePercent;
    float mPaddingLeftPercent;  // width 에 대한 비율
    boolean mAdjustTopForAscent;

    public ASTextView(Context context) {
        super(context);
    }

    public ASTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ASTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ASTextView);
        mTextSizePercent = ta.getFloat(R.styleable.ASTextView_textSizePercent, 95.0f);
        mAdjustTopForAscent = ta.getBoolean(R.styleable.ASTextView_adjustTopForAscent, false);

        TypedArray taView = context.obtainStyledAttributes(attrs, R.styleable.PercentPaddingView);
        mPaddingLeftPercent = taView.getFloat(R.styleable.PercentPaddingView_paddingLeftPercent, 0f);
        //LogUtil.d("mTextSizePercent " + mTextSizePercent);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        setTextSize(TypedValue.COMPLEX_UNIT_PX, (float)h * mTextSizePercent / 100);
        setPadding(Math.round((float)w * mPaddingLeftPercent / 100), 0, 0, 0);
        setIncludeFontPadding(false);
    }

    Paint.FontMetricsInt fontMetricsInt;
    @Override
    protected void onDraw(Canvas canvas) {
        if (mAdjustTopForAscent){
            if (fontMetricsInt == null){
                fontMetricsInt = new Paint.FontMetricsInt();
                getPaint().getFontMetricsInt(fontMetricsInt);
            }
            canvas.translate(0, fontMetricsInt.top - fontMetricsInt.ascent);
        }
        super.onDraw(canvas);
    }
}
