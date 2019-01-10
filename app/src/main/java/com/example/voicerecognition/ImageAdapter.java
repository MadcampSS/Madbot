package com.example.voicerecognition;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    ArrayList<String> imgPaths;

    WindowManager wm;
    Display display;
    Point size;


    public ImageAdapter(Context c, ArrayList<String> imgPaths) {
        mContext = c;
        this.imgPaths = imgPaths;

        wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        display = wm.getDefaultDisplay();
        size = new Point();
    }

    public int getCount() {
        return imgPaths.size();
    }

    public Object getItem(int position) {
        return imgPaths.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        Canvas canvas;
        if (convertView == null) {

            display.getSize(size);
            int width = size.x;
            int height = size.y;

            int imageLength = (size.x / 4);

            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(imageLength, imageLength));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        Glide.with(mContext).load(getItem(position)).into(imageView);
        return imageView;
    }
}