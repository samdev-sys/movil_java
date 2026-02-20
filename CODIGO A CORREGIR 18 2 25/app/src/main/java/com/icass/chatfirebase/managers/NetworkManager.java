package com.icass.chatfirebase.managers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

public class NetworkManager {
    private static volatile NetworkManager instance;
    private static final String TAG = NetworkManager.class.getSimpleName();

    private NetworkManager() {

    }

    @NonNull
    public static NetworkManager getInstance() {
        if(instance == null) {
            synchronized (NetworkManager.class) {
                if(instance == null) {
                    instance = new NetworkManager();
                }
            }
        }

        return instance;
    }

    private boolean connected = false;

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isNetworkConnected() {
        return connected;
    }

    public boolean isOnline(Context context) {
        Log.d("GABO", "Metodo is online");

        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        RunnableFuture<Boolean> futureRun = new FutureTask<Boolean>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Log.d("GABO", "Se intento la conexion call");
                if ((networkInfo .isAvailable()) && (networkInfo .isConnected())) {
                    try {
                        HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
                        urlc.setRequestProperty("User-Agent", "Test");
                        urlc.setRequestProperty("Connection", "close");
                        urlc.setConnectTimeout(1500);
                        urlc.connect();
                        Log.d("GABO", "Se intento la conexion");
                        return (urlc.getResponseCode() >= 200 && urlc.getResponseCode() < 300);
                    } catch (IOException e) {
                        Log.e(TAG, "Error checking internet connection", e);
                    }
                } else {
                    Log.d(TAG, "No network available!");
                }
                return false;
            }
        });

        new Thread(futureRun).start();


        try {
            return futureRun.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }

    }

    public boolean isConnected() {
        final Context context = ResourceManager.getInstance().getContext();
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            NetworkStateDef.DEFAULT,
            NetworkStateDef.CONNECTED,
            NetworkStateDef.DISCONNECTED,
    })
    public @interface NetworkStateDef {
        int DEFAULT = 0;
        int CONNECTED = 1;
        int DISCONNECTED = 2;
    }
}