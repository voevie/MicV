package com.mv.internet;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.net.DatagramPacket;

public class Receiver implements Runnable {

    private Context context;
    protected Handler handler;
    private boolean running=true;
    private Multicast multicast;

    public Receiver(Handler handler, Context context1) {
        this.handler = handler;
        this.context=context1;
        this.multicast=new Multicast();
    }

    @Override
    public void run() {
        while (running) {
            // 设置接收缓冲段
            byte[] receivedData = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(receivedData, receivedData.length);
            try {
                // 接收数据报文
                multicast.getMulticastSocket().receive(datagramPacket);
                Log.e("cmd","HEART");
                Log.e("cmd","localIP="+getLocalIPAddress()+"---remmoteIP="+datagramPacket.getAddress().toString().replace("/",""));
                if(!datagramPacket.getAddress().toString().equals("/" + getLocalIPAddress())){
                    Message msg = new Message();
                    msg.what = ConService.HEART;
                    msg.obj = datagramPacket.getAddress().toString().replace("/","");
                    handler.sendMessage(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void free() {
        running=false;
        multicast.free();
    }

    public  String formatIpAddress(int ipAdress) {

        return (ipAdress & 0xFF) + "." + ((ipAdress >> 8) & 0xFF) + "." + ((ipAdress >> 16) & 0xFF) + "."
                + (ipAdress >> 24 & 0xFF);
    }

    public   String getLocalIPAddress() {
        //获取wifi服务
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        //判断wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return formatIpAddress(ipAddress);
    }

}
