package com.example.arc_app;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.example.arc_app.Adapter.BTdevicesAdapter;
import com.example.arc_app.Adapter.OptionsAdapter;
import com.example.arc_app.Adapter.RegisteredAdapter;
import com.example.arc_app.MQTT.MqttManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    public static ArrayList<String> regDevices = new ArrayList<>();
    public static ArrayList<String> regDevicesMac = new ArrayList<>();
    public static ArrayList<String> regDevicesType = new ArrayList<>();
    private ArrayList<String> optDevices = new ArrayList<>();
    private ArrayList<String> btDevices = new ArrayList<>();
    private ArrayList<BluetoothDevice>  btDevicesObj = new ArrayList<>();
    private ArrayList<String> ID = new ArrayList<>();
    private Button regBtn;
    private Button btBtn;
    public static Dialog dialog;
    private  EditText wifiID;
    private  EditText wifiPW;
    private Button dialogBtn;
    private Button BTdialogBtn;
    private Button BTwifiBtn;
    private Button wifiDismissBtn;
    private Button wifiConfirmBtn;
    private Dialog BTdialog;
    private Dialog WIFIdialog;
    private RecyclerView recyclerView;
    private RegisteredAdapter regAdapter;
    private OptionsAdapter optAdapter;
    private BTdevicesAdapter btAdapter;
    private RecyclerView.LayoutManager layoutManager;
    static public Pair<String,String> deviceChosed;
    //for data store
    private StoreData storeData;
    public static String filename = "datafile.txt";
    //for mqtt
    public static Handler handler_MQTT_message_receive;
    private final String URL="tcp://192.168.137.1:3000";
    private final String clientId="Android_App";
    private String topic = "arc";
    private JSONObject payload;
    //payload格式:
    //action: trig/reg/del
    //target: (device id)
    private BluetoothAdapter mBluetoothAdapter;
    private final int REQUEST_ENABLE_BT=1;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic alertLevel = null;
    private BluetoothGattService linkLossService;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private BluetoothManager bluetoothManager;
    private static final String targetUUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private HashMap place = new HashMap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        regBtn = findViewById(R.id.btnregister);
        regBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(MainActivity.this);
            }
        });
        showRegDevices();
        setTestdata();

        storeData = new StoreData(MainActivity.this);
        storeData.loadData();

        btBtn = findViewById(R.id.btnBT);
        btBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBLEscan();
                showBTDialog(MainActivity.this);
            }
        });

        initMqttHandler();
        initBluetooth();

        new Thread(new Runnable(){
            @Override
            public void run(){
                Log.d(TAG, "connecting to server");
                try {
                    MqttManager.getInstance().createConnection(URL, null, null, clientId);
                    MqttManager.getInstance().subscribe(clientId, 2);
                }catch (Exception e){
                    Log.d(TAG, e.toString());
                }
            }
        }).start();

        // 如果藍芽沒有開啟
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        storeData.storeData();
    }

    private void showRegDevices(){
        recyclerView = findViewById(R.id.regRecycler);
        layoutManager = new LinearLayoutManager(MainActivity.this);
        regAdapter = new RegisteredAdapter(MainActivity.this,regDevices);
        recyclerView.setAdapter(regAdapter);
        recyclerView.setLayoutManager(layoutManager);
        regAdapter.setRegListener(new RegisteredAdapter.RegListener() {
            @Override
            public void onClick(String id, int index) {
                sendMsg("trig",id, index);
            }

            @Override
            public void onDelete(String id, int index) {
                sendMsg("del",id, index);
            }

            @Override
            public void onSetOpen(String id, int index) {
                sendMsg("set_open",id, index);
            }

            @Override
            public void onSetClose(String id, int index) {
                sendMsg("set_close",id, index);
            }
        });
    }

    public void showDialog(Activity activity){
        dialog = new Dialog(activity);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.options_recycler);

        dialogBtn = dialog.findViewById(R.id.btndialog);
        dialogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        RecyclerView recyclerView = dialog.findViewById(R.id.optRecycler);
        final EditText devicename = dialog.findViewById(R.id.device_name);
        final EditText devicemac = dialog.findViewById(R.id.device_mac);
        final RadioGroup devicetype = dialog.findViewById(R.id.device_type);
        optAdapter = new OptionsAdapter(MainActivity.this,optDevices);
        optAdapter.setOptListener(new OptionsAdapter.OptListener(){
            @Override
            public void onConfirm(int which){
                String name = devicename.getText().toString().trim();
                String mac = devicemac.getText().toString().trim();
                if(name == null || name.isEmpty() || mac == null || mac.isEmpty()){
                    Toast toast = Toast.makeText(MainActivity.this, "請輸入裝置ID和MAC",Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
                }
                else {
                    deviceChosed = new Pair<>(name, optDevices.get(which));
                    regDevicesMac.add(mac);
                    int type = devicetype.getCheckedRadioButtonId();
                    String device_type = "";
                    if(type == R.id.light){
                        device_type = "light";
                    }
                    else if(type == R.id.air_cond){
                        device_type = "air_cond";
                    }
                    regDevicesType.add(device_type);
                    updateDevice();
                    dialog.dismiss();
                }
            }
            @Override
            public void onCancel(){
            }
        });
        recyclerView.setAdapter(optAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        dialog.show();
    }

    private void updateDevice(){
        Toast toast = Toast.makeText(this, "已新增裝置: "+deviceChosed.first+"/"+deviceChosed.second,Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();
        regAdapter.addData(deviceChosed.first+"/"+deviceChosed.second);
        sendMsg("reg",deviceChosed.first, regDevices.size()-1);
    }

    private void initMqttHandler(){
        handler_MQTT_message_receive = new Handler(Looper.getMainLooper()){
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if(message.what == 1){
                    //connection built
                    //send stored devices and mac to server
                    payload = new JSONObject();
                    Log.d(TAG,"send stored devices and mac to server ...");
                    for(int i=0; i<regDevices.size(); i++) {
                        final int index = i;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    String device = regDevices.get(index);
                                    String mplace = device.split("/")[1];
                                    String result = device.split("/")[0];
                                    result += "/";
                                    result += (regDevicesType.get(index).equalsIgnoreCase("light"))?0:1;
                                    result += place.get(mplace);
                                    result += "@";
                                    result += regDevicesMac.get(index);
                                    payload.put("target", result);
                                    MqttManager.getInstance().publish("init", 2, payload.toString().getBytes());
                                } catch (Exception e) {
                                    Log.d(TAG, e.toString());
                                }
                            }
                        }).start();
                    }
                }
            }
        };
    }

    private void sendMsg(String action, String id, int index){
        //action: trig/reg/del
        //target: (device id)
        payload = new JSONObject();
        Log.d(TAG,"sendMsg: "+action+"/"+id);
        try {
            String device = regDevices.get(index);
            String mplace = device.split("/")[1];
            String result = id;
            result += "/";
            result += (regDevicesType.get(index).equalsIgnoreCase("light"))?0:1;
            result += place.get(mplace);
            result += "@";
            result += regDevicesMac.get(index);
            payload.put("action", action);
            payload.put("target", result);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    MqttManager.getInstance().publish(topic, 2, payload.toString().getBytes());
                }
            }).start();
        }catch (Exception e){
            Log.d(TAG, e.toString());
        }
    }

    public void showBTDialog(Activity activity){
        BTdialog = new Dialog(activity);
        BTdialog.setCancelable(false);
        BTdialog.setContentView(R.layout.bt_options_recycler);

        BTdialogBtn = BTdialog.findViewById(R.id.BTbtndialog);
        BTwifiBtn = BTdialog.findViewById(R.id.BTbtnWifi);
        BTdialogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BTdialog.dismiss();
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                    }
                });
                Log.d(TAG,"BLE stop scanning...");
                if(mBluetoothGatt!=null) {
                    mBluetoothGatt.disconnect();
                    Log.d(TAG,"BLE device disconnected");
                }
                alertLevel = null;
            }
        });
        BTwifiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showWifiDialog(MainActivity.this);
            }
        });

        RecyclerView recyclerView = BTdialog.findViewById(R.id.BTRecycler);
        final TextView devicename = BTdialog.findViewById(R.id.BT_device_name);
        btAdapter = new BTdevicesAdapter(MainActivity.this,btDevices);
        btAdapter.setOptListener(new BTdevicesAdapter.OptListener(){
            @Override
            public void onConfirm(int which){
                doConnection(btDevicesObj.get(which));
                Log.d(TAG,"doConnection");
            }
            @Override
            public void onCancel(){
            }
        });
        recyclerView.setAdapter(btAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        BTdialog.show();
    }

    public void showWifiDialog(Activity activity){
        WIFIdialog = new Dialog(activity);
        WIFIdialog.setCancelable(false);
        WIFIdialog.setContentView(R.layout.wifi_setup);

        wifiID = WIFIdialog.findViewById(R.id.wifi_name);
        wifiPW = WIFIdialog.findViewById(R.id.wifi_password);
        wifiDismissBtn = WIFIdialog.findViewById(R.id.dismiss);
        wifiConfirmBtn = WIFIdialog.findViewById(R.id.confirm);
        wifiDismissBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WIFIdialog.dismiss();
            }
        });
        wifiConfirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = wifiID.getText().toString();
                String pw = wifiPW.getText().toString();
                String msg = id+";"+pw;

                if(id.isEmpty() || pw.isEmpty()){
                    Toast toast = Toast.makeText(MainActivity.this, "請輸入Wifi名稱和密碼", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                else {
                    if(alertLevel!=null) {  //連線已建立
                        dataSend(msg);
                        Log.d(TAG,"wifi info sent");
                        Toast toast = Toast.makeText(MainActivity.this, "wifi設定成功",Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER,0,0);
                        toast.show();
                    }
                    else{
                        Toast toast = Toast.makeText(MainActivity.this, "藍芽連線中斷", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        Log.e(TAG,"BT disconnected");
                    }
                }
            }
        });

        WIFIdialog.show();
    }

    private void initBluetooth(){
        Log.d(TAG, "initBluetooth()");
        // Initializes Bluetooth adapter.
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Log.d(TAG, "Device doesn't support Bluetooth");
        }

        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        filters = new ArrayList<ScanFilter>();
    }

    private void startBLEscan(){
        btDevices.clear();
        btDevicesObj.clear();

        new Thread(new Runnable(){
            @Override
            public void run(){
                Log.d(TAG, "connecting to server");
                try {
                    mBluetoothAdapter.getBluetoothLeScanner().startScan(filters, settings, mScanCallback);
                }catch (Exception e){
                    Log.d(TAG, e.toString());
                }
            }
        }).start();

        Log.d(TAG, "start BLE scanning...");
        Toast toast = Toast.makeText(MainActivity.this, "正在搜尋藍芽裝置...",Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();
    }

    final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            boolean flag = true;
            for(String d : btDevices){
                if(d==null || d.equals(device.getName())){
                    flag = false;
                    break;
                }
            }
            if(flag) {
                btDevices.add(device.getName());
                btDevicesObj.add(device);
                Log.d(TAG, "BLE device found: "+device.getName());
                Log.d(TAG, "UUID: "+device.getUuids());
                Log.d(TAG, "Address: "+device.getAddress());
            }
        }

        @Override
        public void onScanFailed (int errorCode){
            super.onScanFailed(errorCode);
            Log.e(TAG, "onScanFailed: "+errorCode);
        }
    };

    public void dataSend(String msg){
        byte[] data = {0X01,0X02,0X03};
        try {
            data = msg.getBytes("ASCII");
        }catch (Exception e){
            Log.e(TAG, "dataSend error: "+e.toString());
        }
        alertLevel.setValue(data);
        boolean status = mBluetoothGatt.writeCharacteristic(alertLevel);
        Log.d("TAG", "dataSend status: "+status);
    }

    private void doConnection(BluetoothDevice device){
        mBluetoothGatt = device.connectGatt(MainActivity.this, true, mGattCallback);
        Toast toast = Toast.makeText(MainActivity.this, "正在建立連線...", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "BLE connecting...");
            /**
                         * 連接狀態：
                         *    * The profile is in disconnected state   *public static final int STATE_DISCONNECTED  = 0;
                         *    * The profile is in connecting state     *public static final int STATE_CONNECTING    = 1;
                         *    * The profile is in connected state      *public static final int STATE_CONNECTED    = 2;
                         *    * The profile is in disconnecting state  *public static final int STATE_DISCONNECTING = 3;
                         *
                         */
            if (BluetoothGatt.STATE_CONNECTED == newState) {
                Log.d("TAG", "BLE connected");
                gatt.discoverServices();//必須有，可以讓onServicesDiscovered顯示所有Services
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                    }
                });
                Log.d(TAG,"BLE stop scanning...");
            }else if (BluetoothGatt.STATE_DISCONNECTED == newState){
                Log.d("TAG", "BLE disconnected");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> list = mBluetoothGatt.getServices();
            for (BluetoothGattService bluetoothGattService:list){
                String str = bluetoothGattService.getUuid().toString();
                Log.d(TAG, "found service: " + str);
                List<BluetoothGattCharacteristic> gattCharacteristics = bluetoothGattService
                        .getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    Log.d(TAG, "gattCharacteristic UUID: " + gattCharacteristic.getUuid());
                    if(targetUUID.equals(gattCharacteristic.getUuid().toString())){
                        Log.d(TAG,"======== connection established ========");
                        linkLossService=bluetoothGattService;
                        alertLevel=gattCharacteristic;
                        new Thread(new Runnable(){
                            @Override
                            public void run(){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(TAG, "connecting to BLE device");
                                        try {
                                            Toast toast = Toast.makeText(MainActivity.this, "成功建立連線",Toast.LENGTH_SHORT);
                                            toast.setGravity(Gravity.CENTER,0,0);
                                            toast.show();
                                        }catch (Exception e){
                                            Log.d(TAG, e.toString());
                                        }
                                    }
                                });
                            }
                        }).start();
                    }
                }

            }
            enableNotification(true,gatt,alertLevel);//必須要有，否則接收不到數據
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS){//寫入成功
                Log.d("TAG", "BLE write success");
            }else if (status == BluetoothGatt.GATT_FAILURE){
                Log.e("TAG", "BLE write failed");
            }else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED){
                Log.e("TAG", "BLE write permission denied");
            }

        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) { }
        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) { }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) { }
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) { }
        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) { }
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) { }
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) { }
    };

    private void enableNotification(boolean enable, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (gatt == null || characteristic == null)
            return; //這一步必須要有 否則收不到通知
        gatt.setCharacteristicNotification(characteristic, enable);
    }

    private  void setTestdata(){
        optDevices.add("客廳");
        optDevices.add("書房");
        optDevices.add("大門");
        optDevices.add("玄關");
        optDevices.add("廚房");
        optDevices.add("餐廳");

        place.put("客廳",0);
        place.put("書房",1);
        place.put("大門",2);
        place.put("玄關",3);
        place.put("廚房",4);
        place.put("餐廳",5);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

}

