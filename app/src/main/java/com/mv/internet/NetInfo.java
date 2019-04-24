package com.mv.internet;

import org.json.JSONException;
import org.json.JSONObject;

public class NetInfo {

    public static String CMD_HEART="DIS_HEART";
    public static String CMD_DISCOVERY_REQ="DIS_REQ";
    public static String CMD_DISCOVERY_ACK="DIS_ACK";
    public static String CMD_DISCOVERY_LEAVE="DIS_LEAVE";
    public static String CMD_NEO="NEO";

    private String message;

    private static NetInfo info;

    public NetInfo(String message) {
        this.message = message;
    }

    public static NetInfo getInstance(String message){
        if(info==null){
            info=new NetInfo(message);
        }
        return info;
    }

    public String getCmd() {
        try{
            return new JSONObject(message).getString("cmd");
        }catch (JSONException e ){
            e.printStackTrace();
        }
        return null;
    }

    public static String CmdHeart(){
        try{
            JSONObject object= new JSONObject();
            object.put("cmd",CMD_HEART);
            return object.toString();
        }catch (JSONException e){
            e.printStackTrace();
        }
        return null;
    }
//
//    public static String CmdDiscoveryReq() {
//        try{
//            JSONObject object= new JSONObject();
//            object.put("cmd",CMD_DISCOVERY_REQ);
//            object.put("dev", MicActivity.device);
//            return object.toString();
//        }catch (JSONException e){
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    public static String CmdDiscoveryAck(){
//        try{
//            JSONObject object= new JSONObject();
//            object.put("cmd",CMD_DISCOVERY_ACK);
//            object.put("dev", MicActivity.device);
//            return object.toString();
//        }catch (JSONException e){
//            e.printStackTrace();
//        }
//        return null;
//    }

}
