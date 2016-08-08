package com.example.testdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    private DeviceListAdapter deviceListAdapter;//listview适配器
    private ListView bluetooth_list;
    private Button start, search;
    private final int REQUEST_ENABLE_BT = 0;
    private int change = 0;//用于修改按钮text
    private int change2 = 0;
    public BluetoothAdapter mBluetoothAdapter;
    private int connectState;
    private String name;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            // 获得已经搜索到的蓝牙设备
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 搜索到的不是已经绑定的蓝牙设备
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    // 显示在ListView上
                    final Beacon beacon = new Beacon(device.getName(), "", device.getAddress());

                    deviceListAdapter.addDevice(beacon);
                    deviceListAdapter.notifyDataSetChanged();
                }
                // 如果查找到的设备符合要连接的设备，处理
                if (device.getName().equalsIgnoreCase(name)) {
                    mBluetoothAdapter.cancelDiscovery();//搜索蓝牙占用资源，一旦找到需要连接的设备后及时关闭搜索
                    connectState = device.getBondState();
                    switch (connectState) {
                        //未配对
                        case BluetoothDevice.BOND_NONE:
                            //配对
                            try {
                                Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                                createBondMethod.invoke(device);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        //已配对
                        case BluetoothDevice.BOND_BONDED:
                            try {
                                connect(device);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
                // 搜索完成
            } else if (action
                    .equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("搜索蓝牙设备");
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                //状态改变的广播
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName().equalsIgnoreCase(name)) {
                    connectState = device.getBondState();
                    switch (connectState) {
                        case BluetoothDevice.BOND_NONE:
                            break;
                        case BluetoothDevice.BOND_BONDING:
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            try {
                                //连接
                                connect(device);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //解除注册
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //检测手机是否有蓝牙
        hasFeature();
        //初始化蓝牙适配器
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        //初始化界面
        bluetooth_list = (ListView) this.findViewById(R.id.bluetooth_list);
        deviceListAdapter = new DeviceListAdapter(this);
        bluetooth_list.setAdapter(deviceListAdapter);
        start = (Button) this.findViewById(R.id.start);
        search = (Button) this.findViewById(R.id.search);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //监听listView单击事件--->连接匹配蓝牙
        bluetooth_list.setOnItemClickListener(new ItemClickEvent());

        // 获取所有已经绑定的蓝牙设备
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        if (devices.size() > 0) {
            for (BluetoothDevice bluetoothDevice : devices) {
                Beacon beacon = new Beacon(bluetoothDevice.getName(), "", bluetoothDevice.getAddress());

                deviceListAdapter.addDevice(beacon);
                deviceListAdapter.notifyDataSetChanged();
            }
        }
        // 注册用以接收到已搜索到的蓝牙设备的receiver
        IntentFilter mFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        mFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, mFilter);
        // 注册搜索完时的receiver
        mFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, mFilter);


        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (change == 0) {
                    if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        // 设置蓝牙可见性，最多300秒
                        enableBtIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else {
                        Toast.makeText(MainActivity.this, "蓝牙已经打开了", Toast.LENGTH_SHORT)
                                .show();
                    }
                    start.setText("关闭蓝牙");
                    change = 1;
                } else {
                    mBluetoothAdapter.disable();
                    start.setText("打开蓝牙");
                    change = 0;
                }
            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "允许本地蓝牙被附近的其它蓝牙设备发现", Toast.LENGTH_SHORT)
                        .show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "不允许蓝牙被附近的其它蓝牙设备发现", Toast.LENGTH_SHORT)
                        .show();
                finish();
            }
        }
    }


    //不支持蓝牙设备退出提示
    public void hasFeature() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.quit, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    //搜索按钮
    public void onClick_Search(View v) {

        deviceListAdapter.clear();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();// 如果正在搜索，就先取消搜索
            Toast.makeText(this, "停止了搜索", Toast.LENGTH_SHORT).show();
            search.setText("搜索");
        } else {
            Toast.makeText(this, "正在索搜中......", Toast.LENGTH_SHORT).show();
            setProgressBarIndeterminateVisibility(true);
            search.setText("停止");
            // 开始搜索蓝牙设备,搜索到的蓝牙设备通过广播返回
            mBluetoothAdapter.startDiscovery();
        }

    }

    private void connect(BluetoothDevice device) throws IOException {
        //固定的UUID
        final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
        UUID uuid = UUID.fromString(SPP_UUID);
        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
        socket.connect();
    }


    //连接蓝牙
    private final class ItemClickEvent implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            String text = bluetooth_list.getItemAtPosition(i) + "";
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();

        }
    }

}
