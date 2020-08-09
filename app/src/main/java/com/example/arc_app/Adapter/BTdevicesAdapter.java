package com.example.arc_app.Adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arc_app.R;

import java.util.ArrayList;

public class BTdevicesAdapter extends RecyclerView.Adapter<BTdevicesAdapter.OptionsHolder> {
    private LayoutInflater inflater;
    private ArrayList<String> btDevices;
    private Context mContext;
    private OptListener mOptListener;

    public BTdevicesAdapter(Context ctx, ArrayList<String> btDevices){
        mContext = ctx;
        inflater = LayoutInflater.from(mContext);
        this.btDevices = btDevices;
    }

    @Override
    public BTdevicesAdapter.OptionsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.registered_devices_layout, parent, false);
        OptionsHolder holder = new OptionsHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(BTdevicesAdapter.OptionsHolder holder, int position) {
        holder.device.setText(btDevices.get(position));
    }

    @Override
    public int getItemCount() {
        return btDevices.size();
    }

    class OptionsHolder extends RecyclerView.ViewHolder{
        TextView device;

        public OptionsHolder(View itemView) {
            super(itemView);
            device = itemView.findViewById(R.id.device);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int index = getAdapterPosition();
                    new AlertDialog.Builder(mContext)
                            .setTitle("和該裝置建立連線?")
                            .setPositiveButton("確認", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mOptListener.onConfirm(index);
                                }
                            }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            mOptListener.onCancel();
                        }
                    })
                            .create()
                            .show();
                }
            });
        }
    }
    public void setOptListener(OptListener optListener){
        mOptListener = optListener;
    }
    public interface OptListener{
        void onConfirm(int index);
        void onCancel();
    }

}