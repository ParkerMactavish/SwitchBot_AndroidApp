package com.example.arc_app.Adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arc_app.MainActivity;
import com.example.arc_app.R;
import static com.example.arc_app.MainActivity.regDevicesMac;
import static com.example.arc_app.MainActivity.regDevicesType;

import java.util.ArrayList;

public class RegisteredAdapter extends RecyclerView.Adapter<RegisteredAdapter.RegisteredHolder> {
    private LayoutInflater inflater;
    private ArrayList<String> regDevices;
    private Context mContext;
    private RegListener mRegListener;
    private String TAG = "RegisteredAdapter";


    public RegisteredAdapter(Context ctx, ArrayList<String> regDevices){
        mContext = ctx;
        inflater = LayoutInflater.from(mContext);
        this.regDevices = regDevices;
    }

    @Override
    public RegisteredAdapter.RegisteredHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.registered_devices_layout, parent, false);
        RegisteredHolder holder = new RegisteredHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(RegisteredAdapter.RegisteredHolder holder, int position) {
        holder.device.setText(regDevices.get(position));
        if(regDevicesType.get(position).equals("light")){
            holder.device.setTextColor(Color.parseColor("#000000"));
        }
        else if(regDevicesType.get(position).equals("air_cond")){
            holder.device.setTextColor(Color.parseColor("#4682B4"));
        }
    }

    @Override
    public int getItemCount() {
        return regDevices.size();
    }

    public void addData(String name) {
        regDevices.add(regDevices.size(), name);
        notifyItemInserted(regDevices.size());
    }
    public void removeData(int position) {
        regDevices.remove(position);
        regDevicesMac.remove(position);
        regDevicesType.remove(position);
        notifyItemRemoved(position);
        notifyDataSetChanged();
    }

    class RegisteredHolder extends RecyclerView.ViewHolder{
        TextView device;

        public RegisteredHolder(View itemView) {
            super(itemView);
            device = (TextView) itemView.findViewById(R.id.device);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String deviceName = regDevices.get(getAdapterPosition());
                    Toast toast = Toast.makeText(mContext, deviceName,Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER,0,0);
                    toast.show();
                    String array[] = deviceName.split("/");
                    mRegListener.onClick(array[0], getAdapterPosition());
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    final int index = getAdapterPosition();
                    ArrayList<String> titles = new ArrayList<>();
                    if(regDevicesType.get(index).equals("air_cond")) {
                        titles.add("設定開啟訊號");
                        titles.add("設定關閉訊號");
                    }
                    titles.add("刪除裝置");
                    ListAdapter adapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_list_item_1, titles);
                    AlertDialog dialog = new AlertDialog.Builder(view.getContext())
                            .setAdapter(adapter, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if(regDevicesType.get(index).equals("light"))
                                        which+=2;
                                    try {
                                        makeDialog(which, index).show();
                                    }catch (Exception e){
                                        Log.e(TAG, e.toString());
                                    }

                                }
                            }).create();
                    dialog.show();

                    return false;
                }
            });
        }
    }

    public void setRegListener(RegListener regListener){
        mRegListener = regListener;
    }
    public interface RegListener{
        void onClick(String id, int index);
        void onDelete(String id, int index);
        void onSetOpen(String id, int index);
        void onSetClose(String id, int index);
    }

    private AlertDialog makeDialog(int what, final int index){
        AlertDialog returnedDialog = null;

        if(what == 0 || what ==1){
            String msg;
            String array[] = regDevices.get(index).split("/");
            if(what==0){
                msg = "設定開啟訊號";
                try {
                    mRegListener.onSetOpen(array[0], index);
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            }else{
                msg = "設定關閉訊號";
                try {
                    mRegListener.onSetClose(array[0], index);
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            }
            Toast toast = Toast.makeText(mContext, msg,Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER,0,0);
            toast.show();
        }
        else if(what == 2) {
            returnedDialog = new AlertDialog.Builder(mContext)
                    .setTitle("刪除裝置 " + regDevices.get(index) + " ?")
                    .setPositiveButton("確認", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast toast = Toast.makeText(mContext, "已刪除裝置: " + regDevices.get(index), Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            String array[] = regDevices.get(index).split("/");
                            try {
                                mRegListener.onDelete(array[0], index);
                            } catch (Exception e) {
                                Log.d(TAG, e.toString());
                            }
                            removeData(index);
                        }
                    }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    })
                    .create();
        }
        return returnedDialog;
    }
}