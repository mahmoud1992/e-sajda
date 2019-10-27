/*
 * Copyright (C) 2013 youten
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.issc.ui;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.issc.R;

import java.util.List;

/**
 * スキャンされたBLEデバイスリストのAdapter
 */
public class DeviceAdapter extends ArrayAdapter<BluetoothDevice> {
    private static final String PREFIX_RSSI = "RSSI:";
    private List<BluetoothDevice> mList;
    private LayoutInflater mInflater;
    private int mResId;

    public DeviceAdapter(Context context, int resId, List<BluetoothDevice> objects) {
        super(context, resId, objects);
        mResId = resId;
        mList = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BluetoothDevice item = (BluetoothDevice) getItem(position);

        if (convertView == null) {
            convertView = mInflater.inflate(mResId, null);
        }
        TextView name = (TextView) convertView.findViewById(R.id.device_name);


        if(item.getName()!=null)
        {
            name.setText(item.getName());
        }
        else
        {
            name.setText("Unknown Device");

        }
        TextView address = (TextView) convertView.findViewById(R.id.device_address);
        address.setText(item.getAddress());
      /*  TextView rssi = (TextView) convertView.findViewById(R.id.device_rssi);
        rssi.setText(PREFIX_RSSI + Integer.toString(item.getRssi()));*/

        return convertView;
    }

    /** add or update BluetoothDevice */
    public void update(BluetoothDevice newDevice, int rssi, byte[] scanRecord) {
        if ((newDevice == null) || (newDevice.getAddress() == null)) {
            return;
        }

        boolean contains = false;
        for (BluetoothDevice device : mList) {
            if (newDevice.getAddress().equals(device.getAddress())) {
                contains = true;
                break;
            }
        }
        if (!contains) {
            // add new BluetoothDevice
            mList.add(newDevice);
        }
        notifyDataSetChanged();
    }
}
