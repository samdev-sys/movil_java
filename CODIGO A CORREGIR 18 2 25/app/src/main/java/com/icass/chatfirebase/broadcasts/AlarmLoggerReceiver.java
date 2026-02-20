package com.icass.chatfirebase.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.icass.chatfirebase.DataServer;
import com.icass.chatfirebase.managers.ConnectionManager;
import com.icass.chatfirebase.services.Procesos;
import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.Utils;

import java.text.DateFormat;
import java.util.Date;

public class AlarmLoggerReceiver extends BroadcastReceiver {
    private static final String TAG = LogUtils.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.d(TAG, "*****************Se llama la Alarma para enviar comandos a las: " + DateFormat.getDateTimeInstance().format(new Date()));
        //LogUtils.d(TAG, "El auto esta encendido se manda a llamar a supervisar");
        ConnectionManager.getInstance().lanzarSupervisar();
    }
}