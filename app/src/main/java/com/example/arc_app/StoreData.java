package com.example.arc_app;

import android.content.Context;
import android.util.Log;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import static com.example.arc_app.MainActivity.regDevices;
import static com.example.arc_app.MainActivity.filename;
import static com.example.arc_app.MainActivity.regDevicesMac;
import static com.example.arc_app.MainActivity.regDevicesType;

public class StoreData {
    private FileInputStream fin;
    private FileOutputStream fout;
    private static String TAG = "StoreData";
    private Context mContext;

    public StoreData(Context context){
        mContext = context;
    }

    public void loadData(){
        //open file
        try {
            //沒有此檔案的話，在結束app時fout會自動創造一個
            fin = mContext.openFileInput(filename);
        }catch (Exception e){
            Log.d(TAG,e.toString());
        }
        //get data
        try {
            int c;
            int i=0;
            byte[] temp = new byte[99999];
            while( (c = fin.read()) != -1){
                temp[i] = (byte) c;
                i++;
            }
            String[] array = (new String(temp,"utf-8")).split(" ");
            for(String device : array) {
                if (!device.trim().isEmpty()) {
                    String[] pair = device.split("@");  //將device和mac切開
                    regDevices.add(pair[0]);
                    pair = pair[1].split("#");  //將mac和type切開
                    regDevicesMac.add(pair[0]);
                    regDevicesType.add(pair[1]);
                    Log.d(TAG, "device: "+ regDevices.get(regDevices.size()-1));
                    Log.d(TAG, "mac: "+ regDevicesMac.get(regDevicesMac.size()-1));
                    Log.d(TAG, "type: "+ regDevicesType.get(regDevicesType.size()-1));
                }
            }
        } catch (Exception e) {
            Log.d(TAG,e.toString());
        }
        //close file
        try {
            fin.close();
        }catch (Exception e){
            Log.d(TAG,e.toString());
        }
    }

    public void storeData(){
        //open file
        try {
            fout = mContext.openFileOutput(filename, Context.MODE_PRIVATE);
        }catch (Exception e){
            Log.d(TAG,e.toString());
        }
        //write data
        String data="";
        int i = 0;
        for(String device : regDevices){
            data+=device;
            data+="@";  //區隔device和mac
            data+=regDevicesMac.get(i);
            data+="#";  //區隔mac和type
            data+=regDevicesType.get(i);
            data+=" ";
            i++;
        }
        data.trim();
        try {
            fout.write(data.getBytes("utf-8"));
        }catch (Exception e){
            Log.d(TAG,e.toString());
        }
        //close file
        try {
            fout.close();
        }catch (Exception e){
            Log.d(TAG,e.toString());
        }
    }

}
