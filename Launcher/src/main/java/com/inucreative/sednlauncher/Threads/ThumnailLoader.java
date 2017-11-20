package com.inucreative.sednlauncher.Threads;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.inucreative.sednlauncher.R;
import com.inucreative.sednlauncher.Util.LogUtil;

import java.io.InputStream;

/**
 * Created by Jskim on 2016-07-30.
 */
public class ThumnailLoader extends AsyncTask<String, Void, Bitmap> {
    ImageView mImageView;

    public ThumnailLoader(ImageView imageView) {
        mImageView = imageView;
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            LogUtil.d("Thumbnail Download Error", e.getMessage());
            //e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
        if(result != null) {
            mImageView.setBackgroundResource(0);
            mImageView.setImageBitmap(result);
        } else {
            mImageView.setBackgroundResource(R.drawable.thumbnail_small);
        }
    }
}
