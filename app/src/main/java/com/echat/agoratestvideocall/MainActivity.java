package com.echat.agoratestvideocall;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class MainActivity extends AppCompatActivity {

    private boolean boolCallEnd;
    private boolean boolMutes;
    private ImageView btnCall;
    private ImageView btnMute;
    private ImageView btnSwitchCam;
    private RtcEngine mRtcEngine;
    private IRtcEngineEventHandler mRtcEventHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initcasting();
        mRtcEventHandler = new IRtcEngineEventHandler() {
            @Override
            public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
                Log.i("uid video", uid + "");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setupRemoteVideo(uid);
                    }
                });
            }


        };
        initializeAgoraEngine();

    }

    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
            joinChannel();
            setupLocalVideo();
            setupVideoProfile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private void setupVideoProfile() {
        mRtcEngine.enableVideo();
        mRtcEngine.setVideoProfile(Constants.VIDEO_PROFILE_360P, false);mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }

    private void setupLocalVideo() {
        FrameLayout container = (FrameLayout) findViewById(R.id.local_video_view_container);
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_ADAPTIVE, 0));
    }
    private void joinChannel() {
        mRtcEngine.joinChannel(null, "aye", "Extra Optional Data", new Random().nextInt(10000000)+1); // if you do not specify the uid, Agora will assign one.
    }

    private void setupRemoteVideo(int uid) {
        FrameLayout container = (FrameLayout) findViewById(R.id.remote_video_view_container);

        if (container.getChildCount() >= 1) {
            return;
        }

        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        container.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_ADAPTIVE, uid));
        surfaceView.setTag(uid);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!boolCallEnd) {
            leaveChannel();
        }
        RtcEngine.destroy();
    }


    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    public void onCallClicked(View view) {
        if (boolCallEnd) {
            startCall();
            boolCallEnd = false;
            btnCall.setImageResource(R.drawable.ic_call_end_black_24dp);
        } else {
            endCall();
            boolCallEnd = true;
            btnCall.setImageResource(R.drawable.ic_video_call_black_24dp);
        }

        showButtons(!boolCallEnd);
    }


    private void startCall() {
//        setupLocalVideo();
//        joinChannel();
        initializeAgoraEngine();
    }

    private void endCall() {
        FrameLayout container1 = (FrameLayout) findViewById(R.id.remote_video_view_container);
        FrameLayout container2 = (FrameLayout) findViewById(R.id.local_video_view_container);
        container1.removeView(RtcEngine.CreateRendererView(getBaseContext()));
        container2.removeView(RtcEngine.CreateRendererView(getBaseContext()));
        leaveChannel();
//        RtcEngine.destroy();
    }

    public void onClickCamera(View view) {
        mRtcEngine.switchCamera();
    }
    public void onClickMute(View view) {
        boolMutes = !boolMutes;
        mRtcEngine.muteLocalAudioStream(boolMutes);
        int res = boolMutes ? R.drawable.ic_mic_off_black_24dp : R.drawable.ic_mic_black_24dp;
        btnMute.setImageResource(res);
    }
    private void showButtons(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        btnMute.setVisibility(visibility);
        btnSwitchCam.setVisibility(visibility);
    }

    private void initcasting() {
        btnCall = findViewById(R.id.img_call);
        btnMute = findViewById(R.id.img_mute);
        btnSwitchCam = findViewById(R.id.img_cam);
    }
}