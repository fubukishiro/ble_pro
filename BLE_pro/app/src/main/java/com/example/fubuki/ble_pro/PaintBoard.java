package com.example.fubuki.ble_pro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

public class PaintBoard extends View {
    private float radius;
    private static final String TAG = "PaintBoard";
    private int windowWidth;
    private int windowHeight;
    private float nodeDistance;
    private boolean isNode;
    private int nodeColor;

    public PaintBoard(Context context, AttributeSet attrs) {
        super(context, attrs);
        isNode = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //paint a circle
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        PathEffect dashPathEffect = new DashPathEffect(new float[]{5,5},1);
        paint.setPathEffect(dashPathEffect);
        canvas.drawCircle(windowWidth/2, windowHeight/4, radius*5, paint);

        Paint centerPoint = new Paint();
        centerPoint.setColor(Color.GREEN);
        //设置抗锯齿
        centerPoint.setAntiAlias(true);
        //设置画笔粗细
        centerPoint.setStrokeWidth(2);
        //设置是否为空心
        centerPoint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(windowWidth/2, windowHeight/4, 1*5, centerPoint);

        if(radius > 0) {
            //paint string
            paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(30);
            canvas.drawText("安全距离:" + radius+"m", windowWidth/2 + radius*5, windowHeight/4 + radius*5, paint);
        }

        if(isNode) {
            //红点
            Paint nodePoint = new Paint();
            nodePoint.setColor(nodeColor);
            nodePoint.setAntiAlias(true);
            //设置画笔粗细
            nodePoint.setStrokeWidth(2);
            //设置是否为空心
            nodePoint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawCircle(windowWidth/2+nodeDistance*5, windowHeight/4, 1*5, nodePoint);

            //距离信息
            paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(30);
            canvas.drawText("距离信息:" + nodeDistance +"m", windowWidth/2 +10+ nodeDistance*5, windowHeight/4, paint);
        }
    }

    public void reDraw(float distance){
        radius = distance;
        if(nodeDistance > 0){
            if(nodeDistance > radius)
                nodeColor = Color.RED;
            else
                nodeColor = Color.BLUE;
        }
        invalidate();
        return;
    }

    public void setWindow(int width,int height){
        windowWidth = width;
        windowHeight = height;
        invalidate();
        return;
    }

    public void addNode(String str){
        nodeDistance = MainActivity.convertToFloat(str,0);
        isNode = true;
        if(nodeDistance > radius){
            nodeColor = Color.RED;
        }else{
            nodeColor = Color.BLUE;
        }
        //nodeColor = Color.YELLOW;
        invalidate();
        return;
    }

}
