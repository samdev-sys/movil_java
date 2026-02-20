package com.icass.chatfirebase.activity;

import android.os.Bundle;

import com.icass.chatfirebase.DataServer;
import com.icass.chatfirebase.R;
import com.icass.chatfirebase.base.BaseActivity;
import com.icass.chatfirebase.utils.DateUtils;
import com.icass.chatfirebase.utils.GPSUtil;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.Utils;

public class helpGPS extends BaseActivity {
    private static final String TAG = helpGPS.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_gps);
        enviarUbicacion();
    }

    private void enviarUbicacion() {
        LogUtils.d("helpGPS", "Metodo enviar ubicacion dentro de helpGPS");
        final String date = DateUtils.getDateFormatted();
        GPSUtil gpsUtil = new GPSUtil(this);

        String stateV = gpsUtil.getLocation();

        if (stateV != null) {
            stateV = "&&&" + date + "," + stateV;
        } else {
            stateV = "&&&" + date + "," + "SIN_CONEXION";
        }
        super.finish();
        DataServer.getInstance().enviarMsgAlServer(stateV, Utils.TipoEnvioDef.UBICA);
    }
}