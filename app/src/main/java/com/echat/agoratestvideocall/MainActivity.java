package com.echat.agoratestvideocall;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

    private boolean boolCallEnd, boolMutes;
    private ImageView btnCall, btnMute, btnSwitchCam, btnLeaveMeeting;
    private RtcEngine mRtcEngine;
    private IRtcEngineEventHandler mRtcEventHandler;
    private FrameLayout localContainer, remoteContainer;
    private SurfaceView surfacelocalView, surfaceRemoteView ;

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



    private void joinChannel() {
        mRtcEngine.joinChannel("00640d42e285267418394dc159415cfc050IABq3mrs38FnFBwhKNu9QN2dtSaulgn3dAzG0lwYnlQeW3OMUn4AAAAAEAC6jTl70SypXgEAAQC1LKle", "eshan", "Extra Optional Data", new Random().nextInt(10000000)+1); // if you do not specify the uid, Agora will assign one.
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
        surfacelocalView = RtcEngine.CreateRendererView(getBaseContext());

        surfacelocalView.setZOrderMediaOverlay(true);
        localContainer.addView(surfacelocalView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfacelocalView, VideoCanvas.RENDER_MODE_ADAPTIVE, 0));
    }


    private void setupRemoteVideo(int uid) {
        surfaceRemoteView = RtcEngine.CreateRendererView(getBaseContext());
        if (remoteContainer.getChildCount() >= 1) {
            return;
        }


        remoteContainer.addView(surfaceRemoteView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceRemoteView, VideoCanvas.RENDER_MODE_ADAPTIVE, uid));
        surfaceRemoteView.setTag(uid);

    }

    private void removeLocalVideo() {
        if (surfacelocalView != null) {
            localContainer.removeView(surfacelocalView);
        }
        surfacelocalView = null;
    }
    private void removeRemoteVideo() {
        if (surfaceRemoteView != null) {
            remoteContainer.removeView(surfaceRemoteView);
        }
        surfaceRemoteView = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!boolCallEnd) {
            leaveChannel();
        }
        RtcEngine.destroy();
        mRtcEngine = null;
    }


    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }
    public void OnClickLeave(View view) {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
//                        MainActivity.super.onBackPressed();
                        finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    public void onCallClicked(View view) {
        if (boolCallEnd) {
            startCall();
            boolCallEnd = false;
            btnCall.setImageResource(R.drawable.ic_call_end_black_24dp);
            btnLeaveMeeting.setVisibility(View.GONE);
        } else {
            endCall();
            boolCallEnd = true;
            btnCall.setImageResource(R.drawable.ic_video_call_black_24dp);
            btnLeaveMeeting.setVisibility(View.VISIBLE);
        }

        showButtons(!boolCallEnd);
    }


    private void startCall() {
//        setupLocalVideo();
//        joinChannel();
        initializeAgoraEngine();
    }

    private void endCall() {
        removeLocalVideo();
        removeRemoteVideo();
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
        localContainer = findViewById(R.id.local_video_view_container);
        remoteContainer = findViewById(R.id.remote_video_view_container);
        btnLeaveMeeting = findViewById(R.id.img_leave);



    }


}