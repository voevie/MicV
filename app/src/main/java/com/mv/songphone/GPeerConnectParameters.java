package com.mv.songphone;

public class GPeerConnectParameters {
    public  boolean videoCallEnabled;
    public  boolean audioCallEnabled;
    public  boolean loopback;
    public  boolean tracing;
    public  int videoWidth;
    public  int videoHeight;
    public  int videoFps;
    public  int videoStartBitrate;
    public String videoCodec;
    public  boolean videoCodecHwAcceleration;
    public  boolean captureToTexture;
    public  int audioStartBitrate;
    public String audioCodec;
    public  boolean noAudioProcessing;
    public  boolean aecDump;
    public  boolean useOpenSLES;
    public  boolean videoFlexfecEnabled;
    public  boolean disableBuiltInAEC;
    public  boolean disableBuiltInAGC;
    public  boolean disableBuiltInNS;
    public  boolean disableWebRtcAGCAndHPF;
    public  boolean useLegacyAudioDevice;

    /*音视频的相关参数*/
    public GPeerConnectParameters() {
        this.videoCallEnabled = true;
        this.audioCallEnabled=true;
        this.loopback=false;
        this.tracing = false;
        this.videoWidth = 640;
        this.videoHeight = 480;
        this.videoFps = 15;
        this.videoStartBitrate = 0;
        this.videoCodec = "VP9";
        this.videoCodecHwAcceleration = true;
        this.captureToTexture = false;
        this.audioStartBitrate = 0;
        this.audioCodec = "opus";
        this.noAudioProcessing = false;
        this.aecDump = false;
        this.useOpenSLES = false;
        this.disableBuiltInAEC=false;
        this.disableBuiltInAGC=false;
        this.disableBuiltInNS=false;
        this.disableWebRtcAGCAndHPF=false;
        this.useLegacyAudioDevice=false;
    }
}
