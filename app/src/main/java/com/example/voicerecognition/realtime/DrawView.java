package com.example.voicerecognition.realtime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.util.ArrayList;

public class DrawView extends ImageView {

    private ArrayList<Rect> rectList;
    Paint paint;

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        rectList = new ArrayList<>();

    }

    public void addRect(Rect rect) {
        this.rectList.add(rect);

        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(rectList.size() != 0) {
            for(Rect rect: rectList) {
                canvas.drawRect(rect, paint);
            }
        }
    }
}
