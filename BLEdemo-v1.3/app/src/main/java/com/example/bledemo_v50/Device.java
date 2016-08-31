package com.example.bledemo_v50;

/**
 * Created by 贲 on 2016/8/26.
 */
public class Device {
    private double distance;
    private String hex;
    private String mac;
    private int major;
    private double minor;
    private String name = "";
    private int rssi;
    private int txPower;
    private String uuid;

    public double getDistance()
    {
        return this.distance;
    }

    public String getHex()
    {
        return this.hex;
    }


    public String getMac()
    {
        return this.mac;
    }

    public int getMajor()
    {
        return this.major;
    }

    public double getMinor()
    {
        return this.minor;
    }

    public String getName()
    {
        return this.name;
    }

    public int getRssi()
    {
        return this.rssi;
    }

    public int getTxPower()
    {
        return this.txPower;
    }

    public String getUUID()
    {
        return this.uuid;
    }

    public void setDistance(double paramDouble)
    {
        this.distance = paramDouble;
    }

    public void setHex(String paramString)
    {
        this.hex = paramString;
    }

    public void setMac(String paramString)
    {
        this.mac = paramString;
    }

    public void setMajor(int paramInt)
    {
        this.major = paramInt;
    }

    public void setMinor(double paramDouble)
    {
        this.minor = paramDouble;
    }

    public void setName(String paramString)
    {
        this.name = paramString;
    }

    public void setRssi(int paramInt)
    {
        this.rssi = paramInt;
    }

    public void setTxPower(int paramInt)
    {
        this.txPower = paramInt;
    }

    public void setUUID(String paramString)
    {
        this.uuid = paramString;
    }
}
