package com.mv.songphone;

import android.content.Context;
import android.util.Log;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaCodecVideoEncoder;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.LegacyAudioDeviceModule;
import org.webrtc.voiceengine.WebRtcAudioManager;
import org.webrtc.voiceengine.WebRtcAudioRecord;
import org.webrtc.voiceengine.WebRtcAudioTrack;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.util.Set;

public class GPeerFactory implements GPeer.FactoryEvents {

    private boolean videoSourceStopped;
    private String TAG="GPeerFactory";
    private Context context;
    private EglBase rootEglBase=null;
    private PeerConnectionFactory factory=null;
    private VideoSource videoSource=null;
    private AudioSource audioSource=null;
    AppRTCAudioManager audioManager=null;
    private MediaConstraints videoConstraints=null;
    private MediaConstraints audioConstraints=null;
    private CameraVideoCapturer videoCapturer=null;
    private PeerConnectionFactory.Options options = null;
    private GPeerConnectParameters connectParameters=null;

    private static final int HD_VIDEO_WIDTH = 1280;
    private static final int HD_VIDEO_HEIGHT = 720;
    private static final int MAX_VIDEO_WIDTH = 1280;
    private static final int MAX_VIDEO_HEIGHT = 1280;
    private static final int MAX_VIDEO_FPS = 30;
    private static final String MAX_VIDEO_WIDTH_CONSTRAINT = "maxWidth";
    private static final String MIN_VIDEO_WIDTH_CONSTRAINT = "minWidth";
    private static final String MAX_VIDEO_HEIGHT_CONSTRAINT = "maxHeight";
    private static final String MIN_VIDEO_HEIGHT_CONSTRAINT = "minHeight";
    private static final String MAX_VIDEO_FPS_CONSTRAINT = "maxFrameRate";
    private static final String MIN_VIDEO_FPS_CONSTRAINT = "minFrameRate";
    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT= "googAutoGainControl";
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT  = "googHighpassFilter";
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
    private static final String FIELD_TRIAL_AUTOMATIC_RESIZE = "WebRTC-MediaCodecVideoEncoder-AutomaticResize/Enabled/";
    private static final String VIDEO_FLEXFEC_FIELDTRIAL =
            "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/";
    private static final String VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL = "WebRTC-IntelVP8/Enabled/";
    private static final String VIDEO_H264_HIGH_PROFILE_FIELDTRIAL =
            "WebRTC-H264HighProfile/Enabled/";
    private static final String DISABLE_WEBRTC_AGC_FIELDTRIAL =
            "WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/";
    private static final String VIDEO_CODEC_H264_HIGH = "H264 High";
    public GPeerFactory(final Context context, GPeerConnectParameters  parameters){
        this.context=context;
        connectParameters=parameters;
        rootEglBase = EglBase.create();
        this.videoSourceStopped=connectParameters.videoCallEnabled;
    }

    public void initialize(){
        createFactory();
    }

    public void initialAudioManager(){
        audioManager = AppRTCAudioManager.create(context);
        Log.d(TAG, "Starting the audio manager...");
        audioManager.start(new AppRTCAudioManager.AudioManagerEvents() {
            @Override
            public void onAudioDeviceChanged(
                    AppRTCAudioManager.AudioDevice audioDevice, Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {
                onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
            }
        });
    }
    private void onAudioManagerDevicesChanged(
            final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
    }

    AudioDeviceModule createLegacyAudioDevice() {
        // Enable/disable OpenSL ES playback.
        if (!connectParameters.useOpenSLES) {
            Log.d(TAG, "Disable OpenSL ES audio even if device supports it");
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true /* enable */);
        } else {
            Log.d(TAG, "Allow OpenSL ES audio if device supports it");
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(false);
        }

        if (connectParameters.disableBuiltInAEC) {
            Log.d(TAG, "Disable built-in AEC even if device supports it");
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
        } else {
            Log.d(TAG, "Enable built-in AEC if device supports it");
            WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(false);
        }

        if (connectParameters.disableBuiltInNS) {
            Log.d(TAG, "Disable built-in NS even if device supports it");
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);
        } else {
            Log.d(TAG, "Enable built-in NS if device supports it");
            WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(false);
        }

        WebRtcAudioRecord.setOnAudioSamplesReady(null);

        // Set audio record error callbacks.
        WebRtcAudioRecord.setErrorCallback(new WebRtcAudioRecord.WebRtcAudioRecordErrorCallback() {
            @Override
            public void onWebRtcAudioRecordInitError(String errorMessage) {
                Log.e(TAG,errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordStartError(
                    WebRtcAudioRecord.AudioRecordStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG,errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordError(String errorMessage) {
                Log.e(TAG,errorMessage);
            }
        });

        WebRtcAudioTrack.setErrorCallback(new WebRtcAudioTrack.ErrorCallback() {
            @Override
            public void onWebRtcAudioTrackInitError(String errorMessage) {
                Log.e(TAG,errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackStartError(
                    WebRtcAudioTrack.AudioTrackStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG,errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackError(String errorMessage) {
                Log.e(TAG,errorMessage);
            }
        });

        return  LegacyAudioDeviceModule.Create();
    }

    AudioDeviceModule createJavaAudioDevice() {
        // Set audio record error callbacks.
        JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback = new JavaAudioDeviceModule.AudioRecordErrorCallback() {
            @Override
            public void onWebRtcAudioRecordInitError(String errorMessage) {
                Log.e(TAG,errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordStartError(
                    JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG,errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordError(String errorMessage) {
                Log.e(TAG,errorMessage);
            }
        };

        JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback = new JavaAudioDeviceModule.AudioTrackErrorCallback() {
            @Override
            public void onWebRtcAudioTrackInitError(String errorMessage) {
                Log.e(TAG,errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackStartError(
                    JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG,errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackError(String errorMessage) {
                Log.e(TAG,errorMessage);
            }
        };

        return JavaAudioDeviceModule.builder(context)
                .setSamplesReadyCallback(null)
                .setUseHardwareAcousticEchoCanceler(connectParameters.disableBuiltInAEC)
                .setUseHardwareNoiseSuppressor(connectParameters.disableBuiltInNS)
                .setAudioRecordErrorCallback(audioRecordErrorCallback)
                .setAudioTrackErrorCallback(audioTrackErrorCallback)
                .createAudioDeviceModule();
    }

    private void createFactory(){
//        PeerConnectionFactory.initializeInternalTracer();
        String fieldTrials = "";
        if (connectParameters.videoFlexfecEnabled) {
            fieldTrials += VIDEO_FLEXFEC_FIELDTRIAL;
            Log.d(TAG, "Enable FlexFEC field trial.");
        }
        fieldTrials += VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL;
        if (connectParameters.disableWebRtcAGCAndHPF) {
            fieldTrials += DISABLE_WEBRTC_AGC_FIELDTRIAL;
            Log.d(TAG, "Disable WebRTC AGC field trial.");
        }
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .setFieldTrials(fieldTrials)
                        .setEnableVideoHwAcceleration(connectParameters.videoCodecHwAcceleration)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions());
        if(connectParameters.tracing){
            PeerConnectionFactory.startInternalTracingCapture("/mnt/sdcard/webrtc-trace.txt");
        }
        PeerConnectionFactory.initializeFieldTrials(FIELD_TRIAL_AUTOMATIC_RESIZE);
        if (!connectParameters.useOpenSLES) {
            Log.d(TAG, "Disable OpenSL ES audio even if device supports it");
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true /* enable */);
        } else {
            Log.d(TAG, "Allow OpenSL ES audio if device supports it");
            WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(false);
        }

        WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
        WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(true);
        WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);

        if(connectParameters.loopback){
            options = new PeerConnectionFactory.Options();
            options.networkIgnoreMask = 0;
        }

        final AudioDeviceModule adm = connectParameters.useLegacyAudioDevice
                ? createLegacyAudioDevice()
                : createJavaAudioDevice();

        // Create peer connection factory.
        if (options != null) {
            Log.d(TAG, "Factory networkIgnoreMask option: " + options.networkIgnoreMask);
        }
        final boolean enableH264HighProfile =
                VIDEO_CODEC_H264_HIGH.equals(connectParameters.videoCodec);
        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;

        if (connectParameters.videoCodecHwAcceleration) {
            encoderFactory = new DefaultVideoEncoderFactory(
                    rootEglBase.getEglBaseContext(), true /* enableIntelVp8Encoder */, enableH264HighProfile);
            decoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
        } else {
            encoderFactory = new SoftwareVideoEncoderFactory();
            decoderFactory = new SoftwareVideoDecoderFactory();
        }
        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(adm)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
        if(connectParameters.videoCallEnabled){
            factory.setVideoHwAccelerationOptions(rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
        }
        Log.d(TAG, "GPeer connection factory created.");
        createConstraints();
    }

    private void createConstraints(){
//        createVideoCapturer(new Camera1Enumerator(connectParameters.captureToTexture));
        //create video constraints
        videoConstraints = new MediaConstraints();
        int videoWidth = connectParameters.videoWidth;
        int videoHeight = connectParameters.videoHeight;
        if ((videoWidth == 0 || videoHeight == 0)
                && connectParameters.videoCodecHwAcceleration
                && MediaCodecVideoEncoder.isVp8HwSupported()) {
            videoWidth = HD_VIDEO_WIDTH;
            videoHeight = HD_VIDEO_HEIGHT;
        }
        if (videoWidth > 0 && videoHeight > 0) {
            videoWidth = Math.min(videoWidth, MAX_VIDEO_WIDTH);
            videoHeight = Math.min(videoHeight, MAX_VIDEO_HEIGHT);
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    MIN_VIDEO_WIDTH_CONSTRAINT, Integer.toString(videoWidth)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    MAX_VIDEO_WIDTH_CONSTRAINT, Integer.toString(videoWidth)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    MIN_VIDEO_HEIGHT_CONSTRAINT, Integer.toString(videoHeight)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    MAX_VIDEO_HEIGHT_CONSTRAINT, Integer.toString(videoHeight)));
        }
        int videoFps = connectParameters.videoFps;
        if (videoFps > 0) {
            videoFps = Math.min(videoFps, MAX_VIDEO_FPS);
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    MIN_VIDEO_FPS_CONSTRAINT, Integer.toString(videoFps)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    MAX_VIDEO_FPS_CONSTRAINT, Integer.toString(videoFps)));
        }
        //create audio constraints
        audioConstraints = new MediaConstraints();
        if (connectParameters.noAudioProcessing) {
            Log.d(TAG, "Disabling audio processing");
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                    AUDIO_NOISE_SUPPRESSION_CONSTRAINT , "false"));
        }
        //
        createAudioSource();
    }

    private void createAudioSource(){
        if(factory!=null){
            audioSource = factory.createAudioSource(audioConstraints);
        }
    }

    public EglBase getRootEglBase() {
        if(rootEglBase==null){
            rootEglBase=EglBase.create();
        }
        return rootEglBase;
    }

    public void onToggleSpeaker(boolean state){
        if(audioManager!=null){
//            audioManager.setSpeakerphone(state);
        }
    }

    public void releaseAudioManger(){
        if(audioManager!=null){
//            audioManager.close();
            audioManager.stop();
            audioManager=null;
        }
    }

    public void destroy(){
        if(rootEglBase!=null){
            rootEglBase.release();
            rootEglBase=null;
        }
        if(audioSource!=null){
            audioSource.dispose();
            audioSource=null;
        }
        if(factory!=null){
            factory.dispose();
            factory=null;
        }
        options=null;
        PeerConnectionFactory.stopInternalTracingCapture();
        PeerConnectionFactory.shutdownInternalTracer();
    }

    public GPeer createPeer(GPeer.PeerEvents peerEvents,String roomId,String userId,String p2pId,String srState){
        GPeer peer=new GPeer(roomId,userId,p2pId,srState);
        peer.createPeerConnectionFactory(peerEvents,this,connectParameters);
        return peer;
    }

    @Override
    public PeerConnection onCreatePeerConnection(PeerConnection.RTCConfiguration rtcConfig, MediaConstraints constraints, GPeer.PCObserver observer) {
        if(factory==null){
            return null;
        }
        return factory.createPeerConnection(rtcConfig,constraints,observer);
    }

    @Override
    public MediaStream onCreateLocalMediaStream() {
        if(factory==null){
            return null;
        }
        return factory.createLocalMediaStream("ARDAMS");
    }


    @Override
    public AudioTrack onCreateAudioTrack(String id) {
        return factory.createAudioTrack(id,audioSource);
    }

}
