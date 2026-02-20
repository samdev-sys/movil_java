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

    31,50

    String resultado = vinHex.toString();
    // Un VIN son 17 caracteres = 34 caracteres hexadecimales
    if (resultado.length() > 34) {
        resultado = resultado.substring(resultado.length() - 34);
    }
    return resultado;
}

private void procesarRespuestaValida(String respuesta) {
    LogUtils.d(TAG, "Procesando Respuesta Final: " + respuesta);
    
    // 1. Avisar a la cola de comandos que ya puede enviar el siguiente
    if (DataServer.getInstance().servicesCheckProcesos != null) {
        DataServer.getInstance().servicesCheckProcesos.comandoSiguiente = true;
    }

    // 2. Lógica de envío al servidor si banMessage está activo
    if (banMessage) {
        final String date = DateUtils.getDateFormatted();
        String prefix = NetworkManager.getInstance().isNetworkConnected() ? "" : "SI";
        String dataFull = prefix + "###" + date + respuesta;

        DataServer.getInstance().enviarMsgAlServer(dataFull, Utils.TipoEnvioDef.COMANDO);
        
        // Limpiar estados
        if (DataServer.getInstance().servicesCheckProcesos != null) {
            DataServer.getInstance().servicesCheckProcesos.banMessage = false;
        }
        banMessage = false;
    }

    // 3. Notificar a la UI
    StateNotification.getInstance().notifyState("OBD: " + ComandoOBD + " :: " + respuesta);
    
    // 4. Eliminar el proceso actual de la lista de pendientes
    if (DataServer.getInstance().servicesCheckProcesos != null) {
        DataServer.getInstance().servicesCheckProcesos.popProceso();
    }
}