package com.echat.agoratestvideocall.adapter;

import android.content.Context;
import android.net.ConnectivityManager;

public class Constant {
    public static boolean isNetworkAvailable(Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }

    public static String baseUrl ="https://myownvideocall.herokuapp.com/";
    public static String getToken = baseUrl + "access_token?channel=eshan&uid=1234";
}
