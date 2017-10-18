package com.hl3hl3.arcoremeasure;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.HashMap;

/**
 * view for debug
 * Created by rebecca on 2017/10/18.
 */

public class OverlayView extends View {

    public OverlayView(Context context) {
        this(context, null);
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint.setColor(Color.RED);
        paint.setAntiAlias(true);
        paint.setDither(true);
    }
    final Paint paint = new Paint();
    final HashMap<String, Point> pointMap = new HashMap<>();

    public void setPoint(String tag, float x, float y){
        pointMap.put(tag, new Point((int)x, (int)y));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(pointMap == null){
            return;
        }

        for(String key : pointMap.keySet()){
            Point point = pointMap.get(key);
            canvas.drawCircle(point.x, point.y, 20, paint);
            Log.d("OverlayView", "drawCircle");
        }

    }

}
