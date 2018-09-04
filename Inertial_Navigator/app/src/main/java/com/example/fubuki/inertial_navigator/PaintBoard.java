package com.example.fubuki.inertial_navigator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PaintBoard extends View {
    private float radius;
    private static final String TAG = "PaintBoard";
    private int windowWidth;
    private int windowHeight;
    private float nodeDistance;
    private boolean isNode;
    private int nodeColor;

    private Bitmap baseBitmap;
    private Canvas canvas;

    private float arrowAngle;

    public List<Float> positionXList = new ArrayList<Float>(); //轨迹坐标的列表
    public List<Float> positionYList = new ArrayList<Float>(); //轨迹坐标的列表

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
        canvas.drawCircle(windowWidth/2, windowHeight/4, 20*5, paint);

        Paint centerPoint = new Paint();
        centerPoint.setColor(Color.GREEN);
        //设置抗锯齿
        centerPoint.setAntiAlias(true);
        //设置画笔粗细
        centerPoint.setStrokeWidth(2);
        //设置是否为空心
        centerPoint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(windowWidth/2, windowHeight/4, 1*5, centerPoint);

        drawTria(canvas,(float) windowWidth/2, (float) windowHeight/4, (float) windowWidth/2, (float) windowHeight/4-30,
                30, 10);

        for(int i = 0;i < positionXList.size();i++){
            Paint tracePoint = new Paint();
            tracePoint.setColor(Color.YELLOW);
            //设置抗锯齿
            tracePoint.setAntiAlias(true);
            //设置画笔粗细
            tracePoint.setStrokeWidth(2);
            //设置是否为空心
            tracePoint.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawCircle(positionXList.get(i), positionYList.get(i), 1*5, tracePoint);
        }
    }

    public void setWindow(int width,int height){
        windowWidth = width;
        windowHeight = height;
        invalidate();
        return;
    }

    public void setAngle(float angle){
        arrowAngle = angle;
        invalidate();
        return;
    }

    protected void drawTria(Canvas canvas,float fromX, float fromY, float toX, float toY,
                            int height, int bottom) {
        Paint triPaint = new Paint();
        //画布旋转实现旋转，需要先save
        canvas.save();
        canvas.rotate(arrowAngle,windowWidth/2,windowHeight/4);
        canvas.drawLine(fromX, fromY, toX, toY, triPaint);
        float juli = (float) Math.sqrt((toX - fromX) * (toX - fromX)
                + (toY - fromY) * (toY - fromY));// 获取线段距离
        float juliX = toX - fromX;// 有正负，不要取绝对值
        float juliY = toY - fromY;// 有正负，不要取绝对值
        float dianX = toX - (height / juli * juliX);
        float dianY = toY - (height / juli * juliY);
        float dian2X = fromX + (height / juli * juliX);
        float dian2Y = fromY + (height / juli * juliY);
//终点的箭头
        Path path = new Path();
        path.moveTo(toX, toY);// 此点为三边形的起点
        path.lineTo(dianX + (bottom / juli * juliY), dianY
                - (bottom / juli * juliX));
        path.lineTo(dianX - (bottom / juli * juliY), dianY
                + (bottom / juli * juliX));
        path.close(); // 使这些点构成封闭的三边形
        canvas.drawPath(path, triPaint);
        canvas.restore();
//显示图像

    }

}
