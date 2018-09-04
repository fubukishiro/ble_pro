package com.example.fubuki.ble_pro;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,View.OnClickListener{

    private List<String> bluetoothDevices = new ArrayList<String>(); //保存搜索到的列表
    private ArrayAdapter<String> arrayAdapter; //ListView的适配器

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    private BluetoothGatt bluetoothGatt;
    //bluetoothDevice是dervices中选中的一项 bluetoothDevice=dervices.get(i);
    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private BluetoothDevice bluetoothDevice;

    private List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();//存放扫描结果

    private static final String TAG = "MainActivity";

    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;

    private static final int UPDATE_STATUS= 1;
    private static final int DISCONN_BLE = 2;
    private static final int UPDATE_LIST = 3;
    private static final int ADD_NODE = 4;

    private TextView statusText;

    private PaintBoard paintBoard;

    private Vibrator distanceVibrator;

    private float saveDistance; //设定的安全距离

    private float rcvDis; //从终端接收回来的距离

    @Override
    protected void onCreate(Bundle savedInstanceState)   {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查手机是否支持BLE，不支持则退出
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "您的设备不支持蓝牙BLE，将关闭", Toast.LENGTH_SHORT).show();
            finish();
        }

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);  // 弹对话框的形式提示用户开启蓝牙
        }

        Button searchBtn = findViewById(R.id.searchBtn);
        Button addNodeBtn = findViewById(R.id.addNode);
        Button setDistBtn = findViewById(R.id.setDistance);

        searchBtn.setOnClickListener(this);
        addNodeBtn.setOnClickListener(this);
        setDistBtn.setOnClickListener(this);

        statusText = (TextView) findViewById(R.id.statusText);

        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        int width = outMetrics.widthPixels;
        int height = outMetrics.heightPixels;
        int paintHeight = (int) (height*0.5);
        paintBoard = findViewById(R.id.paint_board);

        paintBoard.setWindow(width,height);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,paintHeight);
        paintBoard.setLayoutParams(layoutParams);

        saveDistance = 10;

        distanceVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

    }

    //断开BLE连接
    private void disconnect_BLE(){
        bluetoothGatt.disconnect();
        Message tempMsg = new Message();
        tempMsg.what = DISCONN_BLE;
        handler.sendMessage(tempMsg);
    }

    private void addNode(){
        EditText msg = findViewById(R.id.editText);
        String tmpStr = "addr"+msg.getText().toString()+"end";
        byte[] msgBytes = tmpStr.getBytes();
        try {
            bluetoothGattCharacteristic.setValue(msgBytes);
            bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDistance(){
        EditText msg = findViewById(R.id.editText);
        String tmpStr = "dis"+msg.getText().toString()+"end";
        saveDistance = convertToFloat(msg.getText().toString(),0);
        byte[] msgBytes = tmpStr.getBytes();
        try {
            //bluetoothGattCharacteristic.setValue(msgBytes);
            //bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
            Log.e(TAG,"Distance is "+ saveDistance);
            if(saveDistance > rcvDis)
                distanceVibrator.cancel();
            else{
                long [] vibratePattern = {100,400,100,400}; // 停止 开启 停止 开启
                //第二个参数表示使用pattern第几个参数作为震动时间重复震动，如果是-1就震动一次
                distanceVibrator.vibrate(vibratePattern,2);
            }
            paintBoard.reDraw(saveDistance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(View view){
        switch(view.getId()){
            case R.id.searchBtn:
                if(bluetoothGatt == null){
                    actionAlertDialog();
                }else{
                    disconnect_BLE();
                    bluetoothGatt = null;
                }
                break;
            case R.id.addNode:
                addNode();
                break;
            case R.id.setDistance:
                setDistance();
                break;
            default:
                break;
        }
    }

    private void actionAlertDialog(){
       /* Context context = getApplicationContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.ble_devices, (ViewGroup)findViewById(R.id.layout_device));
        ListView myListView = (ListView) layout.findViewById(R.id.device_list);
        arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                bluetoothDevices);
        //绑定适配器
        myListView.setAdapter(arrayAdapter);
        myListView.setOnItemClickListener(this);

        builder = new AlertDialog.Builder(MainActivity.this,R.style.Theme_AppCompat_Light_Dialog_Alert);
        builder.setView(layout);

        alertDialog = builder.create();
        Log.e(TAG,"hi");
        alertDialog.show();

        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(scanCallback);//android5.0把扫描方法单独弄成一个对象了（alt+enter添加），扫描结果储存在devices数组中。最好在startScan()前调用stopScan()。

        handler.postDelayed(runnable, 10000);*/

        View bottomView = View.inflate(MainActivity.this,R.layout.ble_devices,null);//填充ListView布局
        ListView lvDevices = (ListView) bottomView.findViewById(R.id.device_list);//初始化ListView控件
        arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1,
                bluetoothDevices);
        lvDevices.setAdapter(arrayAdapter);
        lvDevices.setOnItemClickListener(this);

        builder= new AlertDialog.Builder(MainActivity.this)
                .setTitle("蓝牙列表").setView(bottomView);//在这里把写好的这个listview的布局加载dialog中
        alertDialog = builder.create();
        alertDialog.show();

        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(scanCallback);//android5.0把扫描方法单独弄成一个对象了（alt+enter添加），扫描结果储存在devices数组中。最好在startScan()前调用stopScan()。

        handler.postDelayed(runnable, 10000);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult results) {
            super.onScanResult(callbackType, results);
            BluetoothDevice device = results.getDevice();
            if (!devices.contains(device)) {  //判断是否已经添加
                devices.add(device);//也可以添加devices.getName()到列表，这里省略            }
                // callbackType：回调类型
                // result：扫描的结果，不包括传统蓝牙        }
                bluetoothDevices.add(device.getName() + ":"
                        + device.getAddress() + "\n");
                //更新字符串数组适配器，显示到listview中
               // arrayAdapter.notifyDataSetChanged();
                Message tempMsg = new Message();
                tempMsg.what = UPDATE_LIST;
                handler.sendMessage(tempMsg);
            }
        }
    };

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            bluetoothLeScanner.stopScan(scanCallback);
        }
    };

    @Override
    public void onItemClick(AdapterView<?>parent, View view, int position, long id) {
        bluetoothDevice = devices.get(position);
        bluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    alertDialog.dismiss();

                    Message tempMsg = new Message();
                    tempMsg.what = UPDATE_STATUS;
                    tempMsg.obj = "当前连接设备:"+bluetoothDevice.getName();
                    handler.sendMessage(tempMsg);

                    //setTitle("成功建立连接");
                    //gatt.discoverServices(); //连接成功，开始搜索服务
                    try {
                        Thread.sleep(600);
                        Log.i(TAG, "Attempting to start service discovery:"
                                + gatt.discoverServices());
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        Log.i(TAG, "Fail to start service discovery:");
                        e.printStackTrace();
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    //setTitle("连接断开");
                }
                return;
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, final int status) {
                //此函数用于接收数据
                super.onServicesDiscovered(gatt, status);
                Log.d(TAG, "Hi discovered!");
                String service_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
                String characteristic_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";

                bluetoothGattService = bluetoothGatt.getService(UUID.fromString(service_UUID));
                bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(characteristic_UUID));

                if (bluetoothGattCharacteristic != null) {
                    gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true); //用于接收数据
                    //Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_LONG).show();
                    for (BluetoothGattDescriptor dp : bluetoothGattCharacteristic.getDescriptors()) {
                        if (dp != null) {
                            if ((bluetoothGattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            } else if ((bluetoothGattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                                dp.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            }
                            gatt.writeDescriptor(dp);
                        }
                    }
                    Log.d(TAG, "服务连接成功");
                } else {
                    //Toast.makeText(MainActivity.this, "发现服务失败", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "服务失败");
                    return;
                }
                return;
            }
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
                super.onCharacteristicChanged(gatt,characteristic);
                //发现服务后的响应函数
                Log.e(TAG,"真的有收到东西！");
                byte[] bytesReceive = characteristic.getValue();
                String msgStr = new String(bytesReceive);
                //Pattern pattern = Pattern.compile("([0-9]+.[0-9]+)");
                Pattern pattern = Pattern.compile("[(?<=addr|dis) (?=end)]+");
                String[] strs = pattern.split(msgStr);
                /*for (int i=0;i<strs.length;i++) {
                    Log.e(TAG,strs[i]+"\n");
                }*/
                rcvDis = convertToFloat(strs[1],0);
                if(rcvDis > saveDistance) {
                    long [] vibratePattern = {100,400,100,400}; // 停止 开启 停止 开启
                    //第二个参数表示使用pattern第几个参数作为震动时间重复震动，如果是-1就震动一次
                    distanceVibrator.vibrate(vibratePattern,2);
                }else{
                    distanceVibrator.cancel();
                }
                Message tempMsg = new Message();
                tempMsg.what = ADD_NODE;
                tempMsg.obj = strs[1];
                handler.sendMessage(tempMsg);
                // Log.e(TAG,msgStr);
                return;
            }
        });
    }

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

    private Handler handler = new Handler(){

        public void handleMessage(Message msg){
            Button searchBtn = findViewById(R.id.searchBtn);
            switch (msg.what){
                case UPDATE_STATUS:
                    statusText.setText(msg.obj.toString());
                    searchBtn.setText("断开蓝牙");
                    searchBtn.setBackgroundResource(R.drawable.cancelbutton);
                    break;
                case DISCONN_BLE:
                    statusText.setText("未连接到蓝牙");
                    searchBtn.setText("搜索蓝牙");
                    searchBtn.setBackgroundResource(R.drawable.buttonshape);
                    break;
                case UPDATE_LIST:
                    arrayAdapter.notifyDataSetChanged();
                    break;
                case ADD_NODE:
                    paintBoard.addNode(msg.obj.toString());
                    break;
                default:
                    break;
            }
        }
    };//import android.os.Handler;
}