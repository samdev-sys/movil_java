package com.icass.chatfirebase.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.icass.chatfirebase.DataServer;
import com.icass.chatfirebase.activity.AlertaActivity;
import com.icass.chatfirebase.activity.MainActivity2;
import com.icass.chatfirebase.activity.helpGPS;
import com.icass.chatfirebase.data.LocalData;
import com.icass.chatfirebase.managers.ConnectionManager;
import com.icass.chatfirebase.managers.LockManager;
import com.icass.chatfirebase.managers.ResourceManager;
import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.DateUtils;
import com.icass.chatfirebase.utils.GPSUtil;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.Utils;

/**
 * NOTE: There can only be one service in each app that receives FCM messages. If multiple
 * are declared in the Manifest then the first one will be chosen.
 * <p>
 * In order to make this Java sample functional, you must remove the following from the Kotlin messaging
 * service in the AndroidManifest.xml:
 * <p>
 * <intent-filter>
 * <action android:name="com.google.firebase.MESSAGING_EVENT" />
 * </intent-filter>
 */
public class FirebaseMessagingServiceImpl extends FirebaseMessagingService {
    private static final String TAG = FirebaseMessagingServiceImpl.class.getSimpleName();

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        final Intent commandIntent = new Intent("FirebaseCommandReceived");

        LogUtils.d(TAG, "onMessageReceived");

        long sentTime = remoteMessage.getSentTime();
        LogUtils.d(TAG,"SentTime: " + sentTime);

        long currentTime = System.currentTimeMillis();
        LogUtils.d(TAG, "CurrentTime: " + currentTime);

        long timeElapsed = currentTime - sentTime;
        LogUtils.d(TAG,"TimeElapsed: " + timeElapsed);

        if (timeElapsed > 10000) {
            LogUtils.d(TAG,"Notificación caducada");
            return;
        } else {
            LogUtils.d(TAG,"Notificación vigente");
        }

        if (remoteMessage.getData().containsKey("command")) {
            String command = remoteMessage.getData().get("command");
            LogUtils.d(TAG,"onMessageReceived: " + command);

            final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            final boolean isScreenOn = Build.VERSION.SDK_INT >= 20 ? powerManager.isInteractive() : powerManager.isScreenOn();

            // ¿La pantalla esta encendida?
            if (isScreenOn) {
                LogUtils.d(TAG, "¡La pantalla esta encendida!");
            }

            // Se comprueba que el titulo no este nulo
            if (command != null) {
                if (command.contains("##")) {
                    command = command.replace("##", "");
                    if (!command.isEmpty()){
                        LogUtils.d(TAG, "If de ##");
                        showNotificationActivity(command);
                    }
                } else if (command.toLowerCase().contains("ubica")){
                    LogUtils.d(TAG, "If de ubica");
                    final Intent launchIntent = new Intent(getApplicationContext(), helpGPS.class);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                } else if (command.toLowerCase().contains("estado")){
                    LogUtils.d(TAG, "Se pide el estado del vehiculo mediante comandos");
                    final String date = DateUtils.getDateFormatted();

                    String stateV = ConnectionManager.getInstance().getEstado();

                    if (stateV!=null){
                        stateV = "%%%" + date + stateV;
                    }else {
                        stateV = "%%%" + date + "SIN_CONEXION";
                    }
                    DataServer.getInstance().enviarMsgAlServer(stateV, Utils.TipoEnvioDef.ESTADO);

                } else {
                    LogUtils.d(TAG, "else de ##");
                    commandIntent.putExtra("command", command);
                    LogUtils.d(TAG, "Command Received: " + command);
                    LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  78");
                    sendBroadcast(commandIntent);
                }
            }
        }
    }

    private void showNotificationActivity(@NonNull String command) {
        new LocalData().setCommand(command);

        final PowerManager powerManager = (PowerManager) getBaseContext().getSystemService(Context.POWER_SERVICE);
        final boolean isScreenOn = Build.VERSION.SDK_INT >= 20 ? powerManager.isInteractive() : powerManager.isScreenOn();

        if (isScreenOn) {
            LogUtils.d(TAG,"isScreenOn");

            LockManager.getInstance().unlock(this);

            final Intent launchIntent = new Intent(this, AlertaActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(launchIntent);
        } else {
            LockManager.getInstance().unlock(this);

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    LockManager.getInstance().unlock(getApplicationContext());

                    final Intent launchIntent = new Intent(getApplicationContext(), AlertaActivity.class);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(launchIntent);
                }
            }, 1000);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        LogUtils.d(TAG,"Token: " + token);
        super.onNewToken(token);
    }
}