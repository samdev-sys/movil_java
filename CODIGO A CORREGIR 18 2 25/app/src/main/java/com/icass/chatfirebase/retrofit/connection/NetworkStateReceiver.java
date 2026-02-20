package com.icass.chatfirebase.retrofit.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;

import com.icass.chatfirebase.retrofit.NetworkListener;
import com.icass.chatfirebase.utils.LogUtils;

public class NetworkStateReceiver extends BroadcastReceiver {
    private static final String TAG = NetworkStateReceiver.class.getSimpleName();

    @NonNull
    private final ConnectivityManager connectivityManager;

    @NonNull
    private final NetworkListener listener;

    private boolean connected = false;

    public NetworkStateReceiver(@NonNull Context context, @NonNull NetworkListener listener) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listener = listener;

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(this, intentFilter);
        checkStateChanged();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.d(TAG, "Se llama el receiver en " + TAG);

        if (intent == null || intent.getExtras() == null) {
            return;
        }

        final String action = intent.getAction();
        // TODO Bani LogUtils.print("Action: " + action);

        if (checkStateChanged()) {
            notifyState();
        }
    }

    private boolean checkStateChanged() {
        LogUtils.d(TAG,"checkStateChanged");

        boolean previewState = connected;
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        connected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return previewState != connected;
    }

    private void notifyState() {
        LogUtils.d(TAG,"notifyState");

        if (connected) {
            listener.onConnected();
        } else {
            listener.onDisconnected();
        }
    }
}