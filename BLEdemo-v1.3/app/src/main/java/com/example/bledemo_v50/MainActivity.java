package com.example.bledemo_v50;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    public Device device;
    private static String realAddress;//需要接收的mac地址
    private final static int REQUEST_ENABLE_BT = 1;
    private boolean RECEIVE = false;
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    TextView tv_status;
    Button startButton;
    EditText info;
    /*
    写文件属性
     */
    File f = new File(Environment.getExternalStorageDirectory(), "DATA.txt");
    FileWriter fileWriter = null;
    BufferedWriter bf;
    /*
    需要接收的数值
     */
    //打印所需值
    String timestr;
    int realmajor;
    int realpower;
    double reald1;
    double reald2;
    String realuuid;
    String realname;
    String needMajor = "000";//输入的需求major
    /*
    实际接收数值
     */
    int major, low, high;//Major     i+20-21
    int power;//剩余电量  i+24
    double d1;//温度  (i+ 23)+ (i+22)/10.0D
    double d2;//距离
    String uuid;//uuid
    String name;//名字  getname
    String address;//mac地址  getaddress

    List<BluetoothDevice> devicelist = new ArrayList<BluetoothDevice>();
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice;
    BluetoothGatt bluetoothGatt;
    BluetoothGattCharacteristic controlCharacteristic;

    /*
    主页listview
     */
    private ListView list_data;
    //  private List<Map<String, Object>> listItems;
    List<Map<String, Object>> datalist = new ArrayList<Map<String, Object>>();
    private DataAdapter dataAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_status = (TextView) findViewById(R.id.tv_status);
        info = (EditText) findViewById(R.id.major_info);

        startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    Toast.makeText(MainActivity.this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
                    finish();
                } else if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                    startButton.setText("关闭蓝牙");
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    bluetoothAdapter.disable();
                    startButton.setText("打开蓝牙");
                }
            }
        });
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        list_data = (ListView) findViewById(R.id.list_data);
        dataAdapter = new DataAdapter(this, datalist);
        list_data.setAdapter(dataAdapter);
    }

    //数据添加到表中
    public List<Map<String, Object>> getData() {

        while (RECEIVE) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("time", timestr + "");
            map.put("major", realmajor + "");
            map.put("minor", reald1 + "");
            map.put("distance", reald2 / 10 + "\n");
            datalist.add(map);
            dataAdapter.notifyDataSetChanged();
            Log.e("", "\ntime:" + timestr + "\nmajor:" + realmajor + "\nminor:" + reald1 + "\ndistance:" + reald2);
            //写入txt文件
            writeData();
            RECEIVE = false;
        }
        return datalist;
    }

    /*
    写txt文件
     */
    private void writeData() {
        boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED); //判断sd卡是否存在
        if (sdCardExist) {
            String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();//获取SDCard目录
            String filepath = "";//默认文件路径
            if (f.exists()) {
                filepath = f.getAbsolutePath();
            } else {
                filepath = "不适用";
            }
        } else if (!f.exists()) {
            try {
                f.createNewFile();//创建txt文件 如：testData.txt文件
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            fileWriter = new FileWriter(f, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        bf = new BufferedWriter(fileWriter);
        try {
            bf.write(timestr + " " + realmajor + " " + reald1 + "\n");
            bf.flush();
            bf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    搜索按钮事件
     */
    public void scanAction(View v) {
        devicelist.clear();
        if (!(bluetoothAdapter == null) && bluetoothAdapter.isEnabled()) {
            RECEIVE = false;
            bluetoothAdapter.startLeScan(mScanCallback);
            showDeviceListDialog();
        } else {
            Log.e("", "请先开启蓝牙");
            Toast.makeText(this, "请先开启蓝牙", Toast.LENGTH_SHORT);
        }

    }

    //获取需要的major数值
    public void Okey(View view) {
        needMajor = info.getText().toString();
        tv_status.setText("正在搜寻major为" + needMajor + "的BLE数据");
    }

    private LeScanCallback mScanCallback;

    {
        mScanCallback = new LeScanCallback() {

            //广播下接收消息
            @Override
            public void onLeScan(final BluetoothDevice bluetoothDevice, int rssi,
                                 byte[] values) {
                int i = 5;
                if (((0xFF & values[(i + 2)]) == 2) && ((0xFF & values[i + 3]) == 21)) {
                    byte[] arrayOfByte1 = new byte[16];
                    System.arraycopy(values, i + 4, arrayOfByte1, 0, 16);
                    String str1 = MainActivity.bytesToHex(arrayOfByte1);
                    uuid = str1.substring(0, 8) + "-" + str1.substring(8, 12) + "-" + str1.substring(12, 16) + "-" + str1.substring(16, 20) + "-" + str1.substring(20, 32);
                    name = bluetoothDevice.getName();
                    address = bluetoothDevice.getAddress();
                    byte[] arrayOfByte2 = new byte[2];
                    System.arraycopy(values, i + 20, arrayOfByte2, 0, 2);
                    low = BCDToInt(arrayOfByte2[1]);
                    high = BCDToInt(arrayOfByte2[0]);
                    major = low + high * 100;
                    Log.e("", "minorHexString:" + (0xFF & values[(i + 23)]) + " " + (0xFF & values[(i + 22)]) + " " + (0xFF & values[(i + 22)]) / 10);
                    d1 = (0xFF & values[(i + 23)]) + (0xFF & values[(i + 22)]) / 10.0D;
                    power = values[i + 24];
                    d2 = MainActivity.calculateAccuracy(power, rssi);
                    String hex = MainActivity.bytesToHex(values);
                    timestr = new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(new Date(System.currentTimeMillis()));
                    Log.e("", "当前时间：" + timestr);
                    Log.e("BLE", "Name：" + name + "\nMac：" + address + " \nUUID：" + uuid + "\nMajor：" + major + "\nMinor：" + d1 + "\nTxPower：" + power + "\nrssi：" + rssi + "\ndistance：" + d2);
                    tv_status.setText("请稍等...");
                    if (needMajor == (major + "")) {
                        Log.e("", "接收指定数据");
                        RECEIVE = true;
                        tv_status.setText("目前接收的编号是：" + major);
                        realmajor = major;
                        realpower = power;
                        reald1 = d1;
                        reald2 = d2;
                        realuuid = uuid;
                        realname = name;
                        getData();
                    } else if (needMajor == "000") {
                        Log.e("", "接收所有数据");
                        RECEIVE = true;
                        tv_status.setText("目前接收的编号是：所有数据");
                        realmajor = major;
                        realpower = power;
                        reald1 = d1;
                        reald2 = d2;
                        realuuid = uuid;
                        realname = name;
                        getData();
                    } else {
                        tv_status.setText("请输入正确的major数...");
                    }

                }

                runOnUiThread(new Runnable() {//放在主线程中进行

                                  @Override
                                  public void run() {

                                      if (!devicelist.contains(bluetoothDevice)) {
                                          devicelist.add(bluetoothDevice);
                                          adapter.notifyDataSetChanged();
                                      }
                                  }
                              }
                );

            }
        }
        ;
    }

    /*
    BCD to 10str
     */
    public static String bcd2Str(byte[] bytes) {
        StringBuffer temp = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            temp.append((byte) ((bytes[i] & 0xf0) >>> 4));
            temp.append((byte) (bytes[i] & 0x0f));
        }
        return temp.toString().substring(0, 1).equalsIgnoreCase("0") ? temp
                .toString().substring(1) : temp.toString();
    }

    private static int BCDToInt(byte bcd) {
        return (0xff & (bcd >> 4)) * 10 + (0xf & bcd);
    }

    /*
    *距离计算
    * paramInt : power      paramDouble : rssi
    * d = Math.pow(10,a)
    *a = (Math.abs(rssi)-A)/Math.pow(10,n)
    */
    protected static double calculateAccuracy(int paramInt, double paramDouble) {
        if (paramDouble == 0.0D)
            return -1.0D;
        double iRssi = Math.abs(paramDouble);
        double power = (iRssi - 59) / (10 * 2.0);
        return Math.pow(10, power);
//        double d = paramDouble * 1.0D / paramInt;
//        if (d < 1.0D)
//            return Math.pow(d, 10.0D);
//        return 0.111D + 0.89976D * Math.pow(d, 7.7095D);
    }

    /*
    *uuid计算
     */
    private static String bytesToHex(byte[] paramArrayOfByte) {
        char[] arrayOfChar = new char[2 * paramArrayOfByte.length];
        for (int i = 0; ; i++) {
            if (i >= paramArrayOfByte.length)
                return new String(arrayOfChar);
            int j = 0xFF & paramArrayOfByte[i];
            arrayOfChar[(i * 2)] = hexArray[(j >>> 4)];
            arrayOfChar[(1 + i * 2)] = hexArray[(j & 0xF)];
        }
    }

    Dialog dialog;

    private void showDeviceListDialog() {
        Button btn_dialgo_cancle = null;
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.dialog_scan_device, null);
        dialog = new Dialog(this, R.style.MyDialog);
        // ContentView
        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.show();
        btn_dialgo_cancle = (Button) view
                .findViewById(R.id.btn_dialog_scan_cancle);
        ListView listView = (ListView) view.findViewById(R.id.listview_device);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                                    int position, long arg3) {
                //界面消失，并传值给view
                dialog.dismiss();
            }
        });
        btn_dialgo_cancle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        /*
        *@连接方法改变过程
         */
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, final int newState) {
            super.onConnectionStateChange(gatt, status, newState);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    switch (newState) {
//                        case BluetoothGatt.STATE_CONNECTED:
//                            tv_status.setText("已连接");
//                            Log.e("","已经连接了....");
//                            bluetoothGatt.discoverServices();//寻找服务，调用onServicesDiscovered()回调函数
//                            break;
//                        case BluetoothGatt.STATE_CONNECTING:
//                            tv_status.setText("连接中-测试");
//                            break;
//                        case BluetoothGatt.STATE_DISCONNECTED:
//                            tv_status.setText("已断开");
//                            Log.e("","已经断开了.....");
//                            while (true) {
//
//                             break;
//                            }
//                            break;
//                        case BluetoothGatt.STATE_DISCONNECTING:
//                            tv_status.setText("正在断开");
//                            break;
//                        default:
//                            break;
//                    }
//                }
//            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
//            Log.e("", "已经进入onServicesDiscovered");
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                Log.e("", "status=" + status);
//                List<BluetoothGattService> services = bluetoothGatt.getServices();
//                for (BluetoothGattService bluetoothGattService : services) {
//                    Log.e("", " server:" + bluetoothGattService.getUuid().toString());
//
//                    List<BluetoothGattCharacteristic> characteristic = bluetoothGattService.getCharacteristics();
//                    for (BluetoothGattCharacteristic bluetoothGattCharacteristic : characteristic) {
//                        Log.e("", " charac:" + bluetoothGattCharacteristic.getUuid().toString());
//                        if (bluetoothGattCharacteristic.getUuid().toString().equals("FDA50693-A4E2-4FB1-AFCF-C6EB07647825")) {
//                            controlCharacteristic = bluetoothGattCharacteristic;
//                        }
//                    }
//                }
//            } else {
//                Log.e("", "status===" + status);
//            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.e("", " company id:" + descriptor);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    private BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {

            return devicelist.size();
        }

        @Override
        public long getItemId(int arg0) {

            return 0;
        }

        @Override
        public Object getItem(int arg0) {

            return null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.list_item_scan, null);
                viewHolder = new ViewHolder();
                viewHolder.tv_device = (TextView) convertView
                        .findViewById(R.id.tv_device);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.tv_device.setText(devicelist.get(position).getName()
                    + "  " + devicelist.get(position).getAddress());
            return convertView;
        }

        class ViewHolder {
            TextView tv_device;
        }
    };


}
