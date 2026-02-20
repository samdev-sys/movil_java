package com.icass.chatfirebase.utils;

public class Constants {
    public static final String TAG = "MonitoreoOBD";
    public static final String UNLOCK_TAG = "MonitoreoOBD:INFO";

   // public static final String DEVICE_NAME = "MacBook Air de Brian";
    public static final String DEVICE_NAME = "OBDII";
    public static String ID_VEHICULO = "4455";
//    public static String ID_VEHICULO = "1112";

    public static String comandoEncendido = "06";
    public static String comandoEncendido1 = "0C";
    public static String comandoATSP = "ATSP00";

    public static final String TAG_CODIGO = "codigoUser";
    public static final String TAG_TYPE_USER = "typeUser";
    public static final String TAG_ALERT = "Alert";
    public static final boolean PRUEBAS = false;

    public static final int TIEMPO_ESPERA_INICIO = 5000;
    public static final int TIEMPO_ESPERA_GPS_CLOSE = 1000;
    public static final long INTERVAL_30_SECONDS = 30 * 1000L;
    public static final long INTERVAL_ONE_MINUTE = 60 * 1000L;
    public static final long INTERVAL_THREE_MINUTES = 3 * 60 * 1000L;

    // Permissions
    public static final String BLUETOOTH_CONNECT_PERMISSION = "android.permission.BLUETOOTH_CONNECT";
    public static final String BLUETOOTH_SCAN_PERMISSION = "android.permission.BLUETOOTH_SCAN";
    public static final String ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
}