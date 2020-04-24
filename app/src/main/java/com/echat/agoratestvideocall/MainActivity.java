package com.echat.agoratestvideocall;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

import io.agora.rtc.video.VideoEncoderConfiguration;

public class MainActivity extends AppCompatActivity {
    private RtcEngine mRtcEngine;
    private boolean boolCallEnd;
    private boolean boolMutes;

    private FrameLayout publisherContainer;
    private RelativeLayout subscriberContainer;
    private SurfaceView mLocalView;
    private SurfaceView mRemoteView;

    private ImageView btnCall;
    private ImageView btnMute;
    private ImageView btnSwitchCam;


    public static final String TAG = "MyActivity";

    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initcasting();
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {

            //starting the call step by step
            initializeEngine();
            setupVideoConfig();
            setupLocalVideo();
            joinChannel();
        }
    }


    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, final int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("Get Started:","uid: "+uid);
                }
            });
        }

        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {//
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onUserOffline(final int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("User Left:","uid: "+uid);
                    onRemoteUserLeft();
                }
            });
        }
    };
    private void setupRemoteVideo(int uid) {
        // Only one remote video view is available for this
        // tutorial. Here we check if there exists a surface
        // view tagged as this uid.
        int count = subscriberContainer.getChildCount();
        View view = null;
        for (int i = 0; i < count; i++) {
            View v = subscriberContainer.getChildAt(i);
            if (v.getTag() instanceof Integer && ((int) v.getTag()) == uid) {
                view = v;
            }
        }

        if (view != null) {
            return;
        }

        mRemoteView = RtcEngine.CreateRendererView(getBaseContext());
        subscriberContainer.addView(mRemoteView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
        mRemoteView.setTag(uid);
    }

    private void onRemoteUserLeft() {
        removeRemoteVideo();
    }

    private void removeRemoteVideo() {
        if (mRemoteView != null) {
            subscriberContainer.removeView(mRemoteView);
        }
        mRemoteView = null;
    }


    private void initializeEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void setupVideoConfig() {

        mRtcEngine.enableVideo();
        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }

    private void setupLocalVideo() {
        mLocalView = RtcEngine.CreateRendererView(getBaseContext());
        mLocalView.setZOrderMediaOverlay(true);
        publisherContainer.addView(mLocalView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(mLocalView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void joinChannel() {
        String token = getString(R.string.agora_access_token);
        if (TextUtils.isEmpty(token) || TextUtils.equals(token, "00640d42e285267418394dc159415cfc050IAAuvQDDtnBxnpbOMVEpCA74pUZaTbovr/ZHBd/znK6rDHejk78AAAAAEACX4x4ZnPajXgEAAQCB9qNe")) {
            token = null; // default, no token
        }
        mRtcEngine.joinChannel(token, "demoChannel1", "Extra Optional Data", 0);
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

    public void onClickMute(View view) {
        boolMutes = !boolMutes;
        mRtcEngine.muteLocalAudioStream(boolMutes);
        int res = boolMutes ? R.drawable.ic_mic_off_black_24dp : R.drawable.ic_mic_black_24dp;
        btnMute.setImageResource(res);
    }

    public void onClickCamera(View view) {
        mRtcEngine.switchCamera();
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
        setupLocalVideo();
        joinChannel();
    }

    private void endCall() {
        removeLocalVideo();
        removeRemoteVideo();
        leaveChannel();
    }

    private void removeLocalVideo() {
        if (mLocalView != null) {
            publisherContainer.removeView(mLocalView);
        }
        mLocalView = null;
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
        publisherContainer = findViewById(R.id.rel_pub_container);
        subscriberContainer = findViewById(R.id.rel_sub_container);

    }
    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }

        return true;
    }
}
