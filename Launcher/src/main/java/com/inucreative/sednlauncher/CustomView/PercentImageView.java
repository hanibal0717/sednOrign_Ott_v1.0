package com.inucreative.sednlauncher.CustomView;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import com.inucreative.sednlauncher.R;

/**
 * Created by Jskim on 2016-06-04.
 */
public class PercentImageView extends ImageView {
    float mPaddingTopPercent;
    float mPaddingBottomPercent;
    float mPaddingLeftPercent;
    float mPaddingRightPercent;

    public PercentImageView(Context context) {
        super(context);
    }

    public PercentImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PercentImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PercentPaddingView);
        mPaddingTopPercent = ta.getFloat(R.styleable.PercentPaddingView_paddingTopPercent, 0f);
        mPaddingBottomPercent = ta.getFloat(R.styleable.PercentPaddingView_paddingBottomPercent, 0f);
        mPaddingLeftPercent = ta.getFloat(R.styleable.PercentPaddingView_paddingLeftPercent, 0f);
        mPaddingRightPercent = ta.getFloat(R.styleable.PercentPaddingView_paddingRightPercent, 0f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int ph = ((View)getParent()).getHeight();
        int pw = ((View)getParent()).getWidth();

        setPadding((int)((float)pw * mPaddingLeftPercent / 100),
                (int)((float)ph * mPaddingTopPercent / 100),
                (int)((float)pw * mPaddingRightPercent / 100),
                (int)((float)ph * mPaddingBottomPercent / 100));
    }
}
