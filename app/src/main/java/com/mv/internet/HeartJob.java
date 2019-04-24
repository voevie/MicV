package com.mv.internet;

import android.os.Handler;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * Created by admin on 2019/1/25.
 */

public class HeartJob implements Runnable {
    private String command;
    protected Handler handler;
    private Multicast multicast;

    public HeartJob(Handler handler) {
        this.handler = handler;
        this.multicast=new Multicast();
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public void run() {
        if (command != null) {
            byte[] data = command.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(
                    data, data.length, multicast.getInetAddress(), Constants.MULTI_BROADCAST_PORT);
            try {
                multicast.getMulticastSocket().send(datagramPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
