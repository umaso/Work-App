package com.example.testdemo;

/**
 * Created by 贲 on 2016/8/8.
 */
public class Beacon {

    public String name;//蓝牙的名字
    public String Uuid;//Uuid
    public String bluetoothAddress;//地址

    public Beacon(String name ,String Uuid,String bluetoothAddress)
    {
        this.name = name ;
        this.Uuid = Uuid ;
        this.bluetoothAddress = bluetoothAddress;

    }
}
