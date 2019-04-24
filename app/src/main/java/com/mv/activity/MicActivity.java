package com.mv.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;


import com.mv.songphone.RTCClient;

public class MicActivity extends Activity {

    private String device="phone";
    private RTCClient rtcClient=null;
    private String userId;
    private RadioGroup va_group;

    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE"
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mic);
        findViewById(R.id.start_mic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rtcClient==null){
                    userId=getLocalIPAddress().trim();
                    rtcClient=new RTCClient(MicActivity.this,userId,"123456",device);
                    rtcClient.call();
                }else{
                    if(!rtcClient.isConnection){
                        rtcClient.release();
                        rtcClient=null;
                        rtcClient=new RTCClient(MicActivity.this,userId,"123456",device);
                        rtcClient.call();
                    }
                }
            }
        });

        findViewById(R.id.stop_mic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rtcClient!=null){
                    rtcClient.release();
                    rtcClient=null;
                }
            }
        });

        va_group=(RadioGroup)findViewById(R.id.va_group);
        va_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int id=group.getCheckedRadioButtonId();
                if(id==R.id.rb_phone){
                    device="phone";
                }
                if(id==R.id.rb_tv){
                    device="tv";
                }
            }
        });

//        permission();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(rtcClient!=null){
            rtcClient.release();
            rtcClient=null;
        }
    }

    private long mExitTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();
            } else {
                if(rtcClient!=null){
                    rtcClient.release();
                    rtcClient=null;
                }
                finish();
            }
            return true;
        } else {
            super.onKeyDown(keyCode, event);
            return false;
        }
    }

    private void permission(){
        boolean denied=false;
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                denied=true;
            }
        }
        if(denied){
            PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(MicActivity.this,
                    new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.CAMERA},
                    new PermissionsResultAction() {
                        @Override
                        public void onGranted() {
                        }
                        @Override
                        public void onDenied(String permission) {
                            Toast.makeText(MicActivity.this,"需要语音相关权限",Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
        }
        if(Build.VERSION.SDK_INT>=21){
            PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(MicActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO},
                    new PermissionsResultAction() {
                        @Override
                        public void onGranted() {
                        }
                        @Override
                        public void onDenied(String permission) {
                        }
                    });
        }
    }


    public  String formatIpAddress(int ipAdress) {

        return (ipAdress & 0xFF) + "." + ((ipAdress >> 8) & 0xFF) + "." + ((ipAdress >> 16) & 0xFF) + "."
                + (ipAdress >> 24 & 0xFF);
    }

    //获取本地IP函数
    public   String getLocalIPAddress() {
        //获取wifi服务
        WifiManager wifiManager = (WifiManager) this.getApplicationContext()
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
