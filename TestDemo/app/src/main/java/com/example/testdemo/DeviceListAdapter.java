package com.example.testdemo;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by 贲 on 2016/8/8.
 */
public class DeviceListAdapter extends BaseAdapter {

    private ArrayList<Beacon> devices;
    private LayoutInflater layoutInflater;
    private Activity context;//获取上下文

    public DeviceListAdapter(Activity activity) {
        super();
        context = activity;
        devices = new ArrayList<Beacon>();
        layoutInflater = context.getLayoutInflater();
    }

    public void addDevice(Beacon device) {
        if (device == null) {
            return;
        }
        for (int i = 0; i < devices.size(); i++) {
            String btAddress = devices.get(i).bluetoothAddress;
            if (btAddress.equals(device.bluetoothAddress)) {
                //若已存在，则不添加
                devices.add(i, device);
                devices.remove(i);
            }
        }
        devices.add(device);
    }

    public Beacon getDevice(int posion) {
        return devices.get(posion);
    }

    public void clear() {
        devices.clear();
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int i) {
        return devices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            view = layoutInflater.inflate(R.layout.bluetooth_list, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view
                    .findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view
                    .findViewById(R.id.device_name);
            viewHolder.deviceUUID = (TextView) view
                    .findViewById(R.id.device_beacon_uuid);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        Beacon device = devices.get(i);
        final String deviceName = device.name;
        if (deviceName != null && deviceName.length() > 0) {
            viewHolder.deviceName.setText(deviceName);
        } else {
            viewHolder.deviceName.setText("Unknown service");
        }

        viewHolder.deviceAddress.setText(device.bluetoothAddress);
        viewHolder.deviceUUID.setText(device.Uuid);

        return view;
    }

    class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceUUID;
    }
}
