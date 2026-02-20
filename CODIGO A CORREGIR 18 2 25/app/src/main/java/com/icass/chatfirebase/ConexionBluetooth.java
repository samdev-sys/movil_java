package com.icass.chatfirebase;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.icass.chatfirebase.activity.AlertaActivity;
import com.icass.chatfirebase.activity.MainActivity2;
import com.icass.chatfirebase.activity.PrincipalActivity;
import com.icass.chatfirebase.broadcasts.AlarmLoggerReceiver;
import com.icass.chatfirebase.broadcasts.alarmEstadoVehiculo;
import com.icass.chatfirebase.data.LocalData;
import com.icass.chatfirebase.interfaces.CallbackService;
import com.icass.chatfirebase.managers.ConnectionManager;
import com.icass.chatfirebase.managers.ConnectionManager.EstadoConexionDef;
import com.icass.chatfirebase.managers.NetworkManager;
import com.icass.chatfirebase.managers.ProgressManager;
import com.icass.chatfirebase.managers.ProgressManager.ProgresoDef;
import com.icass.chatfirebase.managers.ResourceManager;
import com.icass.chatfirebase.notifications.StateNotification;
import com.icass.chatfirebase.services.CheckearColaComandos;
import com.icass.chatfirebase.services.CheckearColaComandos.EventCheckProcesos;
import com.icass.chatfirebase.services.Procesos;
import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.DateUtils;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.Utils;
import com.icass.chatfirebase.utils.Utils.TipoEnvioDef;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class ConexionBluetooth extends Thread {
    private static final String TAG = ConexionBluetooth.class.getSimpleName();

    private boolean banHiloSiempre;
    public boolean banIniciar = true;
    private boolean bluetoothEncendido = false;
    private boolean intentandoConexion = false;
    private boolean respuestaValidada = false;
    private boolean comandoAuxiliar = false;

    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    @NonNull
    private final BluetoothDevice bluetoothDevice;
    public boolean banMessage;
    public String StringsinInternet = "";
    public String diagnosticoSI = "";
    public String fallaSI = "";
    private final int contador2 = 0;
    public  boolean banApagado = false;
    private boolean Ban_Message_velocidad = false;
    private String comando = "1";
    public static final String comando0105 = "0105";
    public static final String NODATA = "DATA";
    public boolean banderaComandos = false;
    private String ComandoOBD = "";
    private final Service service;
    private InputStream inputStream;
    private OutputStream outputStream; 
    private BluetoothSocket socket;
    private boolean primeraConexion;
    private String respuestaOBD = "";
    private String lastErro403 = "";

    public ConexionBluetooth(@NonNull BluetoothDevice bluetoothDevice, @NonNull Service service, @NonNull CallbackService listener) {
        LogUtils.d(TAG, "Se crea la instancia de la clase "+ TAG);
        this.bluetoothDevice = bluetoothDevice;
        this.service = service;
        this.banHiloSiempre = true;
        primeraConexion = true;

        DataServer.initInstance().initialize(service, new EventCheckProcesos() {
            public void enviarOBD(String comandoEnv, boolean banMess) {
                try {
                    Log.d(TAG,"Se llama al metodo enviarOBD: " + comandoEnv + " banMessage: " + banMess);
                    ComandoOBD = comandoEnv;
                    banMessage = banMess;
                    write(comandoEnv); 
                    banderaComandos = comandoEnv.contains(comando0105);
                } catch (Exception ex) { 
                    String messageError = "ERROR AL ENVIAR AL OBD: " + ex;
                    Log.e(TAG, messageError);
                }
            }
        }, listener);
    }

    private void iniciarConexion() {
        LogUtils.d(TAG, "Intentando Conexion Bluetooth con el dispositivo:" +  bluetoothDevice.getName());

        try {
            final ParcelUuid[] uuids = this.bluetoothDevice.getUuids();
            for(ParcelUuid uuid: uuids) {
                    this.socket = this.bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid.toString()));
                    this.socket.connect();
                    this.outputStream = socket.getOutputStream();
                    this.inputStream = socket.getInputStream();
                    break;
            }
            LogUtils.d(TAG,"---Conexion exitosa al dispositivo---");
            intentandoConexion = false;
            bluetoothEncendido = true;
            DataServer.getInstance().servicesCheckProcesos.resetBanderas();
            iniciarEnvioConfiguracion();
        } catch (IOException ex){
            LogUtils.d(TAG,"Conexion sin exito: "+ ex.getMessage());
            ConnectionManager.getInstance().setEstadoActual(EstadoConexionDef.ESTADO_DESCONECTADO);
            ProgressManager.getInstance().setProgreso(ProgresoDef.ESTADO_DESCONECTADO);
            Utils.sleep(TAG + " Esperando 30 seg para reintentar", 30000);
            intentandoConexion = false;
            if (!intentandoConexion){
                intentandoConexion = true;
                iniciarConexion();
            }
        }
    }

    private void iniciarEnvioConfiguracion(){
        crearAlarma();
        try {
            DataServer.getInstance().banHiloSiempre = true;
            Log.d(TAG, "Enviando comandos de configuracion");
            // Se corrige validación de longitud para comandos web iniciales
            if (ComandoOBD != null && ComandoOBD.trim().length() >= 2) {
                DataServer.getInstance().setInitComandosWeb();
            } else {
                DataServer.getInstance().initConfiguracion("Conexion Bluetooth");
            }
        } catch (Exception ex) {
            LogUtils.d(TAG, "Catch al intentar enviar comandos" + ex);
            if(socket != null) {
                try { socket.close(); } catch (IOException ioException) { }
            }
            final Intent intent = new Intent(ProgressManager.CONEXION_OBD);
            intent.putExtra("STATE", "RECONECTAR");
            intent.putExtra("CONTADOR", 1);
            service.sendBroadcast(intent);
            if (!intentandoConexion){
                intentandoConexion = true;
                iniciarConexion();
            }
        }
    }

    private void soloComando(@NonNull String comandoEnv) {
        LogUtils.d(TAG, "soloComando: " + comandoEnv);
        if(DataServer.getInstance().servicesCheckProcesos != null) {
            final Procesos proceso = new Procesos(true, comandoEnv, false, true);
            DataServer.getInstance().servicesCheckProcesos.pushProcesoMessage(proceso);
        }
    }

    public void write(@NonNull String message) throws IOException {
        LogUtils.d(TAG, "write: " + message);
        message = message.contains("AT") ? message : message + "1";
        final String data = message + "\r";

        if(outputStream != null) {
            outputStream.write(data.getBytes());
            outputStream.flush();
        } else {
            if (!intentandoConexion){
                intentandoConexion = true;
                iniciarConexion();
            }
        }
    }

    public void reconectar() {
        LogUtils.d(TAG, "Metodo Reconectar");
        Utils.sleep("1", 10000);
        try {
            final ParcelUuid[] uuids = this.bluetoothDevice.getUuids();
            for(ParcelUuid uuid : uuids) {
                try {
                    this.socket = this.bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid.toString()));
                    this.socket.connect();
                    this.outputStream = socket.getOutputStream();
                    this.inputStream = socket.getInputStream();
                    break;
                } catch (IOException ex) {
                    ConnectionManager.getInstance().setEstadoActual(EstadoConexionDef.ESTADO_DESCONECTADO);
                    ProgressManager.getInstance().setProgreso(ProgresoDef.ESTADO_DESCONECTADO);
                    closeStreams();
                }
            }

            LogUtils.d(TAG, "RECONECTADO");
            Intent intent = new Intent(ProgressManager.CONEXION_OBD);
            intent.putExtra("STATE", "CONECTADO_OK");
            intent.putExtra("NAME_DEVICE", this.bluetoothDevice.getName());
            DataServer.getInstance().initConfiguracion("setInitComandos 2");
            service.sendBroadcast(intent);
        } catch (Exception ex) {
            ConnectionManager.getInstance().setEstadoActual(EstadoConexionDef.ESTADO_DESCONECTADO);
            ProgressManager.getInstance().setProgreso(ProgresoDef.ESTADO_DESCONECTADO);

            Intent intent = new Intent(ProgressManager.CONEXION_OBD);
            intent.putExtra("STATE", "RECONECTAR");
            intent.putExtra("CONTADOR", 1);
            service.sendBroadcast(intent);

            try { if(socket != null) socket.close(); } catch (IOException exception) { }

            new Thread(new Runnable() {
                public void run() {
                    Utils.sleep("4", 1000);
                }
            }).start();
        }
    }

    public void cerrar() {
        try {
            bluetoothEncendido = false;
            this.outputStream = null;
            this.inputStream = null;
            if(this.socket != null) this.socket.close();
            banApagado = true;
        } catch (Exception ex) {
            LogUtils.e(TAG,"Error al cerrar la conexion" + ex.getMessage());
        }
    }

    private void closeStreams() {
        try { if(this.outputStream != null) this.outputStream.close(); } catch (Exception ex) { }
        try { if(this.inputStream != null) this.inputStream.close(); } catch (Exception ex) { }
        try { if(this.socket != null) this.socket.close(); } catch (Exception ex) { }
        this.socket = null;
    }

    public void crearAlarma() {
        final Context context = ResourceManager.getInstance().getContext();
        final Intent loggerIntentReceiver = new Intent(context, alarmEstadoVehiculo.class);
        if (alarmManager == null) {
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            pendingIntent = PendingIntent.getBroadcast(context, 7236, loggerIntentReceiver, PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent == null) {
                pendingIntent = PendingIntent.getBroadcast(context, 7235, loggerIntentReceiver, 0);
            }
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), Constants.INTERVAL_30_SECONDS, pendingIntent);
        }
    }

    @Override
public void run() {
    LogUtils.d(TAG, "--------Se inicia Hilo de clase Conexion Bluetooth----------");
    this.banHiloSiempre = true;
    if (!intentandoConexion) {
        intentandoConexion = true;
        iniciarConexion();
    }
    Utils.sleep("2", 2000);

    StringBuilder mSb = new StringBuilder();

    while (this.banHiloSiempre) {
        if (inputStream != null) {
            try {
                byte[] buffer = new byte[1024];
                int bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) continue;

                String strReceived = new String(buffer, 0, bytesRead);
                mSb.append(strReceived);

                // Esperamos hasta que el ELM327 nos de la señal de "listo" (>)
                if (strReceived.contains(">")) {
                    String respuestaCompleta = mSb.toString().trim();
                    
                    // Identificamos el tipo de procesamiento según el comando enviado
                    if (ComandoOBD != null && ComandoOBD.contains("0902")) {
                        respuestaOBD = procesarVIN(respuestaCompleta);
                    } else {
                        // Limpieza estándar para comandos cortos (010D, 0105, etc.)
                        respuestaOBD = respuestaCompleta.replaceAll("[\r\n ]", "").replace(">", "");
                    }

                    LogUtils.d(TAG, "Respuesta Final Procesada: " + respuestaOBD);

                    if (respuestaValida(ComandoOBD, respuestaOBD, comandoAuxiliar)) {
                        // Llamamos a la lógica interna de procesamiento y envío
                        procesarRespuestaValida(respuestaOBD);
                    }

                    mSb.setLength(0); // Vaciamos el buffer para el siguiente comando
                }
            } catch (IOException e) {
                LogUtils.e(TAG, "Error de lectura BT: " + e.getMessage());
                // Aquí deberías gestionar la reconexión
                break; 
            }
        }
    }
}

// 2. Nueva función para limpiar y reconstruir el VIN (0902)
private String procesarVIN(String rawData) {
    // 1. Limpieza inicial: quitamos el eco del comando y el prompt
    String limpio = rawData.replace(">", "").replace("0902", "").trim();
    
    // 2. Dividimos por líneas (el OBD envía \r o \r\n entre tramas)
    String[] lineas = limpio.split("\r");
    StringBuilder vinHex = new StringBuilder();

    for (String linea : lineas) {
        String l = linea.trim().replace(" ", ""); // Quitamos espacios intermedios
        if (l.isEmpty()) continue;

        // Si la línea tiene el índice de trama (0:, 1:, 2:), lo quitamos
        if (l.matches("^[0-9]:.*")) {
            vinHex.append(l.substring(2));
        } 
        // Si es la línea de respuesta positiva pero sin índice (común en el primer frame)
        else if (l.startsWith("4902")) {
            // El primer frame a veces trae el contador de bytes (ej: 490201...)
            // Dependiendo del vehículo, el VIN empieza tras el '4902' + 1 byte de modo
            vinHex.append(l);
        }
        // Si es una línea de datos pura (más de 10 caracteres hex)
        else if (l.length() > 10) {
            vinHex.append(l);
        }
    }

    String resultado = vinHex.toString();
    
    // Opcional: Si el VIN empieza con 4902 (Respuesta Servicio 09 PID 02), 
    // a menudo los primeros caracteres no son parte del VIN ASCII. 
    // Los 17 caracteres del VIN suelen estar al final.
    return resultado;
}