package com.example.fubuki.inertial_navigator;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private SensorManager sm;
    //需要两个Sensor
    private Sensor aSensor;
    private Sensor mSensor;
    private TextView textView1, textView2, textView3;
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];
    private float[] orientationValues = new float[3];
    //加速度相关
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private float gravity [] = new float[3];
    private float linear_acceleration [] = new float[3];
    private TextView textView4, textView5, textView6;
    private TextView textView7, textView8;

    private static final String TAG = "sensor";

    private PaintBoard paintBoard;

    private static final int NEW_ANGLE = 1;
    private static final int SET_ABS_ACC = 2;
    private static final int SET_VEL = 3;
    private static final int UPDATE_POS = 4;

    //绝对坐标加速度
    private float absolute_acc_x;
    private float absolute_acc_y;

    private float v_start_x,v_current_x,v_start_y,v_current_y,position_x,position_y;

    //???
    private float[] I = new float[9];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        v_start_x = 0;
        v_start_y = 0;

        textView1 = (TextView)findViewById(R.id.textView);
        textView2 = (TextView)findViewById(R.id.textView2);
        textView3 = (TextView)findViewById(R.id.textView3);

        textView4 = (TextView)findViewById(R.id.textView4);
        textView5 = (TextView)findViewById(R.id.textView5);
        textView6 = (TextView)findViewById(R.id.textView6);

        textView7 = (TextView)findViewById(R.id.textView7);
        textView8 = (TextView)findViewById(R.id.textView8);

        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sm.registerListener(myListener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(myListener, mSensor,SensorManager.SENSOR_DELAY_NORMAL);

        //更新显示数据的方法
        calculateOrientation();

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(new SensorEventListener() {

            public void onSensorChanged(SensorEvent event) {

                /*System.out.println("x---" + event.values[0]);
                System.out.println("y---" + event.values[1]);
                System.out.println("z---" + event.values[2]);*/
                final float alpha = 0.8f;

                //用滤波器过滤掉杂质
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                //分别对应z,x,y
                linear_acceleration[0] = event.values[0] - gravity[0];
                linear_acceleration[1] = event.values[1] - gravity[1];
                linear_acceleration[2] = event.values[2] - gravity[2];

                //System.out.println(linear_acceleration[0]);
                //System.out.println(linear_acceleration[1]);
                //System.out.println(linear_acceleration[2]);
                textView4.setText(Float.toString(linear_acceleration[0]));
                textView5.setText(Float.toString(linear_acceleration[1]));
                textView6.setText(Float.toString(linear_acceleration[2]));
            }

            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        }, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        int width = outMetrics.widthPixels;
        int height = outMetrics.heightPixels;
        int paintHeight = (int) (height*0.5);

        position_x = width/2;
        position_y = height/4;

        paintBoard = findViewById(R.id.paint_board);

        paintBoard.setWindow(width,height);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,paintHeight);
        paintBoard.setLayoutParams(layoutParams);

        paintBoard.setAngle(0);

        mHandler.postDelayed(r, 100);
    }

    //再次强调：注意activity暂停的时候释放
    public void onPause(){
        sm.unregisterListener(myListener);
        super.onPause();
    }

    final SensorEventListener myListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticFieldValues = sensorEvent.values;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = sensorEvent.values;
            calculateOrientation();
        }
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private  void calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
        SensorManager.getOrientation(R, orientationValues);

        // 要经过一次数据格式的转换，转换为度
        values[0] = (float) Math.toDegrees(orientationValues[0]);

        Message tempMsg = new Message();
        tempMsg.what = NEW_ANGLE;
        tempMsg.obj = Float.toString(values[0]);
        handler.sendMessage(tempMsg);

        textView1.setText(Float.toString(values[0]));
        values[1] = (float) Math.toDegrees(orientationValues[1]);
        textView2.setText(Float.toString(values[1]));
        values[2] = (float) Math.toDegrees(orientationValues[2]);
        textView3.setText(Float.toString(values[2]));
    }

    private Handler handler = new Handler(){

        public void handleMessage(Message msg){
            switch (msg.what){
                case NEW_ANGLE:
                    paintBoard.setAngle(convertToFloat(msg.obj.toString(),0));
                    break;
                case SET_ABS_ACC:
                    textView7.setText(Float.toString(absolute_acc_x));
                    textView8.setText(Float.toString(absolute_acc_y));
                    break;
                case SET_VEL:
                    textView7.setText(Float.toString(v_start_x));
                    textView8.setText(Float.toString(v_start_y));
                    break;
                case UPDATE_POS:
                    paintBoard.invalidate();
                    break;
                default:
                    break;
            }
        }
    };//import android.os.Handler;

    //string转float
    public static float convertToFloat(String number, float defaultValue) {
        if (TextUtils.isEmpty(number)) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(number);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void accDecompose(){
        absolute_acc_x = linear_acceleration[0] * (float)(Math.cos(orientationValues[2]) * Math.cos(orientationValues[0]))
                        + linear_acceleration[1] * (float)(Math.cos(orientationValues[1]) * Math.sin(orientationValues[0]))
                        - linear_acceleration[2] * (float)(Math.sin(orientationValues[2]) * Math.cos(orientationValues[1]));

        absolute_acc_y = linear_acceleration[1] * (float)(Math.cos(orientationValues[0]) * Math.cos(orientationValues[1]))
                - linear_acceleration[0] * (float)(Math.cos(orientationValues[2]) * Math.sin(orientationValues[0]))
                - linear_acceleration[2] * (float)(Math.sin(orientationValues[1]) * Math.sin(orientationValues[2]));

        Message tempMsg = new Message();
        tempMsg.what = SET_ABS_ACC;
        handler.sendMessage(tempMsg);
        return;
    }

    private void getVelocity(){
        v_current_x = v_start_x + absolute_acc_x;
        v_current_y = v_start_y + absolute_acc_y;
        v_start_x = (float)0.5*(v_current_x + v_start_x);
        v_start_y = (float)0.5*(v_current_y + v_start_y);
        //Message tempMsg = new Message();
        //tempMsg.what = SET_VEL;
        //handler.sendMessage(tempMsg);
        return ;
    }

    private void getPosition(){
        position_x = position_x + v_current_x;
        position_y = position_y + v_current_y;
        paintBoard.positionXList.add(position_x);
        paintBoard.positionYList.add(position_y);
        Message tempMsg = new Message();
        tempMsg.what = UPDATE_POS;
        handler.sendMessage(tempMsg);
        return ;
    }
    Handler mHandler = new Handler();
    Runnable r = new Runnable() {

        @Override
        public void run() {
            accDecompose();
            getVelocity();
            getPosition();
            //每隔1s循环执行run方法
            mHandler.postDelayed(this, 100);
        }
    };
}
