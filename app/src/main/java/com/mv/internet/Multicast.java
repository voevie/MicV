package com.mv.internet;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;


public class Multicast {

    // 组播Socket
    private MulticastSocket multicastSocket;
    // IPV4地址
    private InetAddress inetAddress;

//    private   Multicast multicast = new Multicast();

    private   DatagramSocket unicast;

    public Multicast() {
        try {
            inetAddress = InetAddress.getByName(Constants.MULTI_BROADCAST_IP);
            multicastSocket = new MulticastSocket(Constants.MULTI_BROADCAST_PORT);
            multicastSocket.setLoopbackMode(true);
            multicastSocket.joinGroup(inetAddress);
//            multicastSocket.setTimeToLive(4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public  Multicast getMulticast() {
//        return multicast;
//    }

    public MulticastSocket getMulticastSocket() {
        return multicastSocket;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public void free() {
        if (multicastSocket != null) {
            try {
                multicastSocket.leaveGroup(inetAddress);
                multicastSocket.close();
                multicastSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public  DatagramSocket getUnicastSocket(){
        try{
            if(unicast==null){
                unicast=new DatagramSocket(Constants.UNICAST_PORT);
            }
        }catch (SocketException e){
            e.printStackTrace();
        }
        return null;
    }
}
