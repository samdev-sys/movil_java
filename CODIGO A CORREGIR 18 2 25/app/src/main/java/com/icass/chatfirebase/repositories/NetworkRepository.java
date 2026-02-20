package com.icass.chatfirebase.repositories;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.icass.chatfirebase.managers.NetworkManager;
import com.icass.chatfirebase.managers.NetworkManager.NetworkStateDef;
import com.icass.chatfirebase.managers.ResourceManager;
import com.icass.chatfirebase.retrofit.NetworkListener;
import com.icass.chatfirebase.retrofit.connection.NetworkNotification;
import com.icass.chatfirebase.retrofit.connection.NetworkStateReceiver;
import com.icass.chatfirebase.retrofit.requests.ConnectionRequest;
import com.icass.chatfirebase.utils.LogUtils;

@SuppressLint({"ObsoleteSdkInt", "NewApi"})
public class NetworkRepository {
    private static final String TAG = NetworkRepository.class.getSimpleName();

    private static volatile NetworkRepository instance;

    @NetworkStateDef
    private int network  = NetworkStateDef.DEFAULT;

    @NonNull
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            LogUtils.d(TAG,"onConnected N");
            NetworkManager.getInstance().setConnected(true);
            setNetwork(NetworkStateDef.CONNECTED);
            }

        @Override
        public void onLost(@NonNull Network network) {
            LogUtils.d(TAG,"onDisconnected N");
            NetworkManager.getInstance().setConnected(false);
            setNetwork(NetworkStateDef.DISCONNECTED);
        }
    };

    private NetworkRepository() {    }

    public static NetworkRepository getInstance() {
        if(instance == null) {
            synchronized(NetworkRepository.class) {
                if(instance == null) {
                    instance = new NetworkRepository();
                }
            }
        }

        return instance;
    }

    public int getNetworkState() {
        return network;
    }

    public void setNetwork(@NetworkStateDef int network) {
        this.network = network;
        NetworkNotification.getInstance().networkNotification();
    }

    public void registerNetwork() {
        final Context context = ResourceManager.getInstance().getContext();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LogUtils.d(TAG,"register registerNetwork Android > Nougat");

            final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(connectivityManager == null) {
                NetworkManager.getInstance().setConnected(false);
                return;
            }

            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        } else {
            LogUtils.d(TAG,"register registerNetwork Android < MarshMallow");

            final NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver(context, new NetworkListener() {
                @Override
                public void onConnected() {
                    LogUtils.d(TAG,"onConnected M");
                    NetworkManager.getInstance().setConnected(true);
                    setNetwork(NetworkStateDef.CONNECTED);
                }

                @Override
                public void onDisconnected() {
                    LogUtils.d(TAG,"onDisconnected M");
                    NetworkManager.getInstance().setConnected(false);
                    setNetwork(NetworkStateDef.DISCONNECTED);
                }
            });
            LogUtils.d(TAG, "Se regsitra el receiver en " + TAG);

            context.registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }

        handler.postDelayed(new Runnable() {//TODO este metodo se ejecuta 2 segundo despues desde OBDAPP
            @Override
            public void run() {
                if(NetworkManager.getInstance().isConnected()) {
                    testConnection();
                } else {
                    NetworkManager.getInstance().setConnected(false);
                    setNetwork(NetworkStateDef.DISCONNECTED);
                }
            }
        }, 2000);
    }

    public void unregisterNetwork(@NonNull Context context) {
        LogUtils.d(TAG,"register unregisterNetwork");

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager == null) {
            NetworkManager.getInstance().setConnected(false);
            return;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    private void testConnection() {
        LogUtils.d(TAG,"Testing connection");

        final ConnectionRequest request = new ConnectionRequest(new NetworkListener() {
            @Override
            public void onConnected() {
                NetworkManager.getInstance().setConnected(true);
                setNetwork(NetworkStateDef.CONNECTED);
            }

            @Override
            public void onDisconnected() {
                NetworkManager.getInstance().setConnected(false);
                setNetwork(NetworkStateDef.DISCONNECTED);

                // Retry request
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        testConnection();
                    }
                }, 10000);
            }
        });
        request.execute(); //Se ejecuta la prueba de conexion
    }
}