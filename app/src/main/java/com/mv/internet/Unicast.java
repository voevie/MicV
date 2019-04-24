package com.mv.internet;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;


public class Unicast {

    byte[] receiveMsg = new byte[512];
    private DatagramPacket receivePacket;
    private DatagramSocket receiveSocket;

    private DatagramPacket sendPacket;
    private DatagramSocket sendSocket;

//    private static final Unicast unicast = new Unicast();

    public Unicast() {
        try {
            // 初始化接收Socket
            receivePacket = new DatagramPacket(receiveMsg, receiveMsg.length);
            receiveSocket = new DatagramSocket(Constants.UNICAST_PORT);
            // 初始化发送Socket
            sendSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public DatagramSocket getReceiveSocket(){
        try{
            if(receiveSocket==null){
                receiveSocket = new DatagramSocket(Constants.UNICAST_PORT);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return receiveSocket;
    }

    public DatagramSocket getSendSocket(){
        try{
            if(sendSocket==null){
                sendSocket = new DatagramSocket();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return sendSocket;
    }

//    public static Unicast getUnicast() {
//        return unicast;
//    }

    public void free() {
        if (receiveSocket != null) {
            try {
                receiveSocket.close();
                receiveSocket = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
