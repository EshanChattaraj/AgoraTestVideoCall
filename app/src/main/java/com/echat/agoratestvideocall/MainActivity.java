package com.echat.agoratestvideocall;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import java.util.Random;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

import com.echat.agoratestvideocall.adapter.Constant;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private boolean boolCallEnd, boolMutes;
    private ImageView btnCall, btnMute, btnSwitchCam, btnLeaveMeeting;
    private RtcEngine mRtcEngine;
    private IRtcEngineEventHandler mRtcEventHandler;
    private FrameLayout localContainer, remoteContainer;
    private SurfaceView surfacelocalView, surfaceRemoteView ;
    RelativeLayout relativeLayout;
    RequestQueue requestQueue;
    String getToken = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initcasting();
        getTokenfromurl();

        mRtcEventHandler = new IRtcEngineEventHandler() {
            @Override
            public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("uid video", String.valueOf(uid));
                        setupRemoteVideo(uid);
                    }
                });
            }


        };

        if (!Constant.isNetworkAvailable(getApplicationContext()) == true) {
            showSnackBar("OOPS! NO INTERNET " +
                    "Please check your network connection. " +
                    "Try Again");
        } else {
        initializeAgoraEngine();
        }
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


    private void getTokenfromurl(){

        StringRequest stringRequest = new StringRequest(Request.Method.GET, Constant.getToken,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            getToken = obj.getString("token");
                            Log.i("getToken", getToken);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //displaying the error in toast if occurrs
                        Log.i("error.getMessage()", String.valueOf(error.getMessage()));
                        Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        //creating a request queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        //adding the string request to request queue
        requestQueue.add(stringRequest);
    }


    private void joinChannel() {
        mRtcEngine.joinChannel(getToken, "eshan", "Extra Optional Data", new Random().nextInt(10000000)+1); // if you do not specify the uid, Agora will assign one.
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
                .setMessage("Are you sure you want to Leave Meeting and Exit?")
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
        relativeLayout = findViewById(R.id.relative_layout);


    }

    public void showSnackBar(String errors) {
        Snackbar.make(relativeLayout, errors, Snackbar.LENGTH_INDEFINITE)
                .setAction("CLOSE", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        moveTaskToBack(true);
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(1);
                    }
                })
                .setActionTextColor(getResources().getColor(android.R.color.holo_red_light))
                .show();
    }


}