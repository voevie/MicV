package com.mv.songphone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedList;

import com.mv.internet.ConService;
import com.mv.internet.Constants;
import com.mv.internet.InterNetCallback;


public class RTCClient implements GPeer.PeerEvents{

    private LooperExecutor executor;
    private GPeerFactory gPeerFactory;
    private Activity activity=null;
    private String userId=null;
    private String roomId=null;
    private String srState;
    private boolean videoEnabled;
    private String  otherIpAddr=null;
    private String otherUserId=null;
    private String device;

    private ConService conService;
    boolean ordered=false;
    private int seq=0;
    public boolean isConnection=false;
    private DatagramSocket sendSocket=null;

    private LinkedList<String> messageQueue=new LinkedList<>();

    private void onNegotiateMessage(String message){
        try {
            Log.d("negotiate==", message);

            JSONObject user=new JSONObject(message);
            String type = user.optString("type");
            if(type.equals("add")){
                onAddNewPeer(user.getString("p2pId"));
            }else if (type.equals("candidate")) {
                IceCandidate candidate = new IceCandidate(user.getString("id"), user.getInt("label"), user.getString("candidate"));
                onAddRemoteIceCandidate(candidate,user.getString("p2pId"));
            } else if (type.equals("answer")) {
                SessionDescription sdp = new SessionDescription(SessionDescription.Type.fromCanonicalForm(type), user.getString("sdp"));
                onSetRemoteDescription(sdp,user.getString("p2pId"));
//                removeCachePeer(user.getString("p2pId"));
            } else if (type.equals("offer")) {
                SessionDescription sdp = new SessionDescription(SessionDescription.Type.fromCanonicalForm(type), user.getString("sdp"));
                onSetRemoteDescription(sdp,user.getString("p2pId"));
                onCreateAnswer(user.getString("p2pId"));
            } else if (type.equals("bye")) {
                onClosePeerConnection(user.getString("p2pId"));
            } else {
                reportError("Unexpected WebSocket message: " );
            }

        } catch (JSONException e) {
            reportError("WebSocket message JSON parsing error: " + e.toString());
        }
    }

    public RTCClient(Activity activity, String userId, String roomId, String device){
        this.activity=activity;
        this.userId=userId;
        this.roomId=roomId;
        this.device=device;
        this.srState=device.equals("phone")?"s":"r";
        this.videoEnabled=false;
        onRtcClientCreate();
    }

    public void call(){
        gPeerFactory.initialAudioManager();

        conService=new ConService(activity);
        conService.registerCallback(new InterNetCallback() {
            @Override
            public void receiveRequest(Object object) {
                try{
                    JSONObject tmp=(JSONObject)object;
                    String dev=tmp.getString("device");
                    String p2pId=tmp.getString("ip");
                    Log.e("ip",p2pId);
                    if(!device.equals(dev)&&device.equals("tv")){
                        if(otherIpAddr==null){
                            onAddNewPeer(p2pId);
                        }
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if(peer==null){
                                        Thread.sleep(800);
                                    }
                                    sendAck();
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }catch(JSONException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void heart(String address) {
                if(otherIpAddr==null||(otherIpAddr!=null&&otherIpAddr.equals(address))){
                    otherIpAddr=address;
                    otherUserId=address;
                    new Thread(){
                        @Override
                        public void run() {
                            super.run();
                            sendReq();
                        }
                    }.start();
                }
            }

            @Override
            public void findNewUser(String message) {
                if(device.equals("phone")&&peer==null){
                    onCreatePeer();
                }
            }

            @Override
            public void removeUser(String message) {
                disconnect();
            }

            @Override
            public void negotiate(String message) {
                try{
                    Log.e("receive-negociate",message);
                    JSONObject user=new JSONObject(message);
                    int seqNumber = user.getInt("seq");
                    if(messageQueue.isEmpty()){
                        if(seqNumber==0){
                            ordered=true;
                            onNegotiateMessage(message);
                        }else{
                            if(ordered){
                                onNegotiateMessage(message);
                            }else{
                                messageQueue.add(message);
                                ordered=false;
                            }
                        }
                    }else{
                        if(seqNumber==0){
                            onNegotiateMessage(message);
                            for(int i=0;i<messageQueue.size();i++){
                                onNegotiateMessage(messageQueue.get(i));
                            }
                        }else{
                            messageQueue.add(message);
                        }
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
        conService.start();

    }
    private void onRtcClientCreate(){
        Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler(activity));
        activity.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        executor = new LooperExecutor();
        executor.requestStart();
        GPeerConnectParameters parameters=new GPeerConnectParameters();
        parameters.videoCallEnabled=videoEnabled;
        gPeerFactory=new GPeerFactory(activity,parameters);
//        permission();
        onCreatePeerFactory();
    }


    private void onCreatePeerFactory() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                gPeerFactory.initialize();
            }
        });
    }

    public void release(){
        if(isConnection){
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    sendLeave();
                }
            });
            disconnect();
        }
        conService.free();
    }

    private void disconnect() {
        isConnection=false;

        if(peer!=null){
            peer.close();
        }
        gPeerFactory.releaseAudioManger();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                gPeerFactory.destroy();
            }
        });
    }

    private void reportError(final String description) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                disconnectWithErrorMessage(description);
            }
        });
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        new AlertDialog.Builder(activity)
                .setTitle("error")
                .setMessage(errorMessage)
                .setCancelable(false)
                .setNeutralButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        disconnect();
                    }
                }).create().show();
    }

    private void onAddRemoteIceCandidate(final IceCandidate candidate, String p2pId){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                peer.addRemoteIceCandidate(candidate);
            }
        });
    }

    private void onSetRemoteDescription(final SessionDescription sdp, String p2pId){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(peer==null){
                    peer=gPeerFactory.createPeer(RTCClient.this,roomId,userId,p2pId,srState);
                    peer.createPeerConnection();
                }
                peer.setRemoteDescription(sdp);
            }
        });
    }

    GPeer peer=null;
    private void onCreatePeer(){
        peer =gPeerFactory.createPeer(this,roomId,userId,otherUserId,srState);
        peer.createPeerConnection();
        peer.createOffer();
    }

    private void onAddNewPeer(String p2pId){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                peer=gPeerFactory.createPeer(RTCClient.this,roomId,userId,p2pId,srState);
                peer.createPeerConnection();
            }
        });
//        onCreatePeerConnection();
    }

    private void onCreatePeerConnection(){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                peer.createPeerConnection();
            }
        });
    }
    private void onCreateAnswer(String p2pId){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                peer.createAnswer();
            }
        });
    }


    private void onClosePeerConnection(String p2pId){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                peer.close();
                disconnect();
            }
        });
    }

    @Override
    public void onIceConnected(String p2pid) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isConnection=true;
            }
        });
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates, String peerId) {

    }

    @Override
    public void sendOfferSdp(SessionDescription sdp, String roomId, String userId, String p2pId) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                jsonPut(json,"cmd","NEO");
                jsonPut(json, "sdp", sdp.description);
                jsonPut(json, "type", "offer");
                jsonPut(json,"p2pId",p2pId);
                jsonPut(json,"roomId",roomId);
                jsonPut(json,"userId",userId);
                jsonPut(json,"seq",seq);
                sendData(json.toString());
                seq++;
            }
        });
    }

    @Override
    public void sendAnswerSdp(SessionDescription sdp, String roomId, String userId, String p2pId) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                jsonPut(json,"cmd","NEO");
                jsonPut(json, "sdp", sdp.description);
                jsonPut(json, "type", "answer");
                jsonPut(json,"p2pId",p2pId);
                jsonPut(json,"roomId",roomId);
                jsonPut(json,"userId",userId);
                jsonPut(json,"seq",seq);
                sendData(json.toString());
                seq++;
            }
        });
    }

    @Override
    public void sendLocalIceCandidate(IceCandidate candidate, String roomId, String userId, String p2pId) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                jsonPut(json,"cmd","NEO");
                jsonPut(json, "type", "candidate");
                jsonPut(json, "label", candidate.sdpMLineIndex);
                jsonPut(json, "id", candidate.sdpMid);
                jsonPut(json, "candidate", candidate.sdp);
                jsonPut(json,"p2pId",p2pId);
                jsonPut(json,"roomId",roomId);
                jsonPut(json,"userId",userId);
                jsonPut(json,"seq",seq);
                sendData(json.toString());
                seq++;
            }
        });
    }

    @Override
    public void onIceDisconnected(String p2pid) {
        disconnect();
    }

    private  void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendData(String message){
        try {
            byte[] feedback = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(feedback, feedback.length,
                    InetAddress.getByName(otherIpAddr), Constants.UNICAST_PORT);
            if(sendSocket==null) sendSocket=new DatagramSocket();
            sendSocket.send(sendPacket);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendAck(){
        try{
            JSONObject object=new JSONObject();
            object.put("id",userId);
            object.put("cmd","DIS_ACK");
            sendData(object.toString().trim());
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void sendLeave(){
        try{
            JSONObject object=new JSONObject();
            object.put("id",userId);
            object.put("cmd","DIS_LEAVE");
            sendData(object.toString().trim());
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void sendReq(){
        try{
            JSONObject object=new JSONObject();
            object.put("cmd","DIS_REQ");
            object.put("device",device);
            sendData(object.toString().trim());
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

}
