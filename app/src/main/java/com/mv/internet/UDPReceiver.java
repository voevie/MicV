package com.mv.internet;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * Created by admin on 2019/1/30.
 */

public class UDPReceiver implements Runnable {

    private Context context;
    protected Handler handler;
    private boolean running=true;
    private Unicast unicast;

    public UDPReceiver(Handler handler, Context context1) {
        this.context=context1;
        this.handler = handler;
        this.unicast=new Unicast();
    }

    @Override
    public void run() {
        while (running) {
            // 设置接收缓冲段
            byte[] receivedData = new byte[1400];
            DatagramPacket datagramPacket = new DatagramPacket(receivedData, receivedData.length);
            try {
                // 接收数据报文
                unicast.getReceiveSocket().receive(datagramPacket);
                Log.e("cmd",new String(datagramPacket.getData()).trim());
                Log.e("cmd","localIP="+getLocalIPAddress()+"---remmoteIP="+datagramPacket.getAddress().toString().replace("/",""));
                // 判断数据报文类型，并做相应处理
                handleCmdData(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void handleCmdData(DatagramPacket packet){
        if(!packet.getAddress().toString().equals("/" + getLocalIPAddress())){
            try{
                JSONObject object=new JSONObject(new String(packet.getData()).trim());
                switch (object.getString("cmd").trim()){
                    case "DIS_HEART":
                        handleHeart(packet);
                        break;
                    case "DIS_REQ":
                        handleReqData(packet);
                        break;
                    case "DIS_ACK":
                        handleAckData(packet);
                        break;
                    case "NEO":
                        handleNeoData(packet);
                        break;
                    case "DIS_LEAVE":
                        handleLeaveData(packet);
                        break;
                        default:
                            break;
                }
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
    }

    private void handleHeart(DatagramPacket packet){
        Message msg = new Message();
        msg.what = ConService.HEART;
        msg.obj = packet.getAddress().toString().replace("/","").trim();
        handler.sendMessage(msg);
    }

    private void handleReqData(DatagramPacket packet){
        try{
            JSONObject tmp=new JSONObject(new String(packet.getData()).trim());
            JSONObject json=new JSONObject();
            json.put("ip",packet.getAddress().toString().replace("/","").trim());
            json.put("device",tmp.getString("device"));

            Message msg = new Message();
            msg.what = ConService.DISCOVERING_RECEIVE;
            msg.obj = json;
            handler.sendMessage(msg);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void handleNeoData(DatagramPacket packet){
        Message msg = new Message();
        msg.what = ConService.DISCOVERING_NEO;
        msg.obj = new String(packet.getData()).trim();
        handler.sendMessage(msg);
    }

    private void handleAckData(DatagramPacket packet){
        try{
            String content = new String(packet.getData()).trim();
            JSONObject tmp=new JSONObject(content);

            Message msg = new Message();
            msg.what = ConService.DISCOVERING_RESPONSE;
            msg.obj = tmp.getString("id");
            handler.sendMessage(msg);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void handleLeaveData(DatagramPacket packet){
        try{
            String content = new String(packet.getData()).trim();
            JSONObject tmp=new JSONObject(content);
            Message msg = new Message();
            msg.what = ConService.DISCOVERING_LEAVE;
            msg.obj = tmp.getString("id");
            handler.sendMessage(msg);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * 发送Handler消息
     *
     * @param content 内容
     */
    private void sendMsg2MainThread(String content, int msgWhat) {
        Message msg = new Message();
        msg.what = msgWhat;
        msg.obj = content;
        handler.sendMessage(msg);
    }

    public void free() {
        running=false;
        if (unicast != null)
            unicast.free();
    }

    public  String formatIpAddress(int ipAdress) {

        return (ipAdress & 0xFF) + "." + ((ipAdress >> 8) & 0xFF) + "." + ((ipAdress >> 16) & 0xFF) + "."
                + (ipAdress >> 24 & 0xFF);
    }

    //获取本地IP函数
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
