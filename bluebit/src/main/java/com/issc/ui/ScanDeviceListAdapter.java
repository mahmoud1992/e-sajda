package com.issc.ui;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.issc.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by kanivel.j on 20-03-2018.
 */

public class ScanDeviceListAdapter extends RecyclerView.Adapter<ScanDeviceListAdapter.ViewHolder>{


    private ArrayList<BluetoothDevice> bluetoothDevices;

    Context context;
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case

        TextView txtDeviceName;

        TextView txtDeviceAddress;
        CardView cardView;
        LinearLayout layout;


        public ViewHolder(View v) {
            super(v);

            txtDeviceName=(TextView)v.findViewById(R.id.device_name);
            txtDeviceAddress=(TextView)v.findViewById(R.id.device_address);
/*
            cardView=(CardView) v.findViewById(R.id.cardview);
*/
            layout=(LinearLayout) v.findViewById(R.id.layout);

        }
    }


    public ScanDeviceListAdapter(Context context,ArrayList<BluetoothDevice> bluetoothDevices)
    {
        this.context=context;
        this.bluetoothDevices=bluetoothDevices;
    }



    @Override
    public ScanDeviceListAdapter.ViewHolder onCreateViewHolder( ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View rootView = inflater.inflate(R.layout.listitem_device, parent, false);
        return new ViewHolder(rootView);

    }

    @Override
    public void onBindViewHolder(ScanDeviceListAdapter.ViewHolder holder, final int position) {


            if(bluetoothDevices.get(position).getName()==null)
            {
                holder.txtDeviceName.setText("Unknown Device");
            }
            else
            {
                holder.txtDeviceName.setText(bluetoothDevices.get(position).getName());

            }

            holder.txtDeviceAddress.setText(bluetoothDevices.get(position).getAddress());


        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e("OnclickViewHolder","OnclickViewHolder");

                onClickListener.onClick(bluetoothDevices.get(position));


            }
        });



            /*holder.cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    onClickListener.onClick(bluetoothDevices.get(position));

                }
            });*/





    }

    @Override
    public int getItemCount() {
        return bluetoothDevices.size();
    }




    /** add or update BluetoothDevice */
    public void update(BluetoothDevice newDevice, int rssi, byte[] scanRecord) {
        if ((newDevice == null) || (newDevice.getAddress() == null)) {
            return;
        }

        boolean contains = false;
        for (BluetoothDevice device : bluetoothDevices) {
            if (newDevice.getAddress().equals(device.getAddress())) {
                contains = true;
                //device.se(rssi); // update
                break;
            }
        }
        if (!contains) {
            // add new BluetoothDevice
            bluetoothDevices.add(newDevice);
        }
        notifyDataSetChanged();
    }


    public void setOnClickListener(OnClickListener onClickListener)
    {
        this.onClickListener=onClickListener;
    }


    public OnClickListener onClickListener;

    public interface OnClickListener
    {
        void onClick(BluetoothDevice bluetoothDevice);
    }

}
