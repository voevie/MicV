package com.mv.internet;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.mv.internet.NetInfo.CMD_DISCOVERY_LEAVE;

public class ConService {

    // 创建循环任务线程用于间隔的发送上线消息，获取局域网内其他的用户
    private ScheduledExecutorService discoverService = Executors.newScheduledThreadPool(1);
    // 创建7个线程的固定大小线程池，分别执行DiscoverServer
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    // 加入、退出组播组消息
    private HeartJob heartJob;
    private Receiver receiver;
    private UDPReceiver udpReceiver;

    public static final int DISCOVERING_SEND = 0;
    public static final int DISCOVERING_RECEIVE = 1;
    public static final int DISCOVERING_LEAVE = 2;
    public static final int DISCOVERING_RESPONSE=3;
    public static final int DISCOVERING_NEO=4;
    public static final int HEART=5;

    private Context context;

    private Handler heartHandler;
    private Handler myHandler;


    public ConService(Context context) {
        this.context = context;
        heartHandler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what==HEART){
                    heart((String) msg.obj);
                }
            }
        };
        myHandler= new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if(msg.what==HEART){
                    heart((String) msg.obj);
                }  else if (msg.what == DISCOVERING_RECEIVE) {
                    receiveRequest( msg.obj);
                } else if(msg.what==DISCOVERING_RESPONSE){
                    findNewUser((String) msg.obj);
                } else if (msg.what == DISCOVERING_LEAVE) {
                    removeUser((String) msg.obj);
                }else if(msg.what==DISCOVERING_NEO){
                    negotiate((String) msg.obj);
                }
            }
        };
    }

    public void start(){
        // 初始化探测线程
        heartJob = new HeartJob(null);
        receiver = new Receiver(heartHandler,context);
        udpReceiver=new UDPReceiver(myHandler,context);
        heartJob.setCommand(NetInfo.CmdHeart());
        // 启动探测局域网内其余用户的线程（每分钟扫描一次）
        discoverService.scheduleAtFixedRate(heartJob, 0, 3, TimeUnit.SECONDS);
        threadPool.execute(receiver);
        threadPool.execute(udpReceiver);
    }

    public void free(){
        receiver.free();
        udpReceiver.free();
        discoverService.shutdown();
        threadPool.shutdown();
    }

    private InterNetCallback inetCallback=null;

    private void heart(String address){
        if(inetCallback!=null){
            inetCallback.heart(address);
        }
    }

    private void receiveRequest(Object object){
        if(inetCallback!=null){
            inetCallback.receiveRequest(object);
        }
    }

    private void findNewUser(String ipAddress) {
        if(inetCallback!=null){
            inetCallback.findNewUser(ipAddress);
        }
    }

    private void removeUser(String ipAddress) {
        if(inetCallback!=null){
            inetCallback.removeUser(ipAddress);
        }
    }

    private void negotiate(String message){
        if(inetCallback!=null){
            inetCallback.negotiate(message);
        }
    }


    public void registerCallback(InterNetCallback callback){
        inetCallback=callback;
    }

    public void unregisterCallback(){
        inetCallback=null;
        heartJob.setCommand(CMD_DISCOVERY_LEAVE);
    }








}
