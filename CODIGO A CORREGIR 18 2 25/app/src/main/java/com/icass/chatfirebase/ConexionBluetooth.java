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
        if (!intentandoConexion){
            intentandoConexion = true;
            iniciarConexion();
        }
        Utils.sleep("2", 2000);

        String errorSent = "";
        StringBuilder mSb = new StringBuilder();
        String cadFix = "";
        ArrayList<String> list = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();

        while(this.banHiloSiempre) {
            if(inputStream != null) {
                try {
                    final byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) continue;
                    
                    final String strReceived = new String(buffer, 0, bytesRead);

                    if(this.banIniciar) {
                        if(strReceived.contains(">")) {
                            mSb.append(strReceived);
                            cadFix = mSb.toString().replaceAll("\r\n", " ").replace(">", "");
                            
                            if (!cadFix.contains("DATA") && !cadFix.contains("ELM") && !cadFix.contains("SEAR")){
                                list = new ArrayList<>(Arrays.asList(cadFix.split(" ")));
                            } else {
                                list = new ArrayList<>(Arrays.asList(cadFix.split("\r\n")));
                            }
                            
                            respuestaOBD = list.size() > 1 ? list.get(0).replace("\r\n", "").replace(" ", "").replace(">", "") :
                                    mSb.toString().replace("\r\n", "").replace(" ", "").replace(">", "");
                            
                            LogUtils.d(TAG, "Respuesta OBD " + respuestaOBD);

                            if (respuestaValida(ComandoOBD, respuestaOBD, comandoAuxiliar) ) {
                                DataServer.getInstance().servicesCheckProcesos.comandoSiguiente = true;
                                String error_actual = respuestaOBD;
                                String comandoLimpio = ComandoOBD != null ? ComandoOBD.trim() : "";

                                if(!error_actual.trim().isEmpty()) {
                                    
                                    // CORRECCIÓN LÍNEA 335: Se evalúa comandoLimpio en lugar de la variable 'comando' genérica
                                    if(comandoLimpio.equals("03")) {
                                        LogUtils.d(TAG, "Entra en procesamiento de códigos de error (03)");
                                        Ban_Message_velocidad = true;
                                        soloComando(comando); // Se asume que 'comando' contiene el parámetro necesario para soloComando
                                        error_actual = "";
                                        comando = "1";
                                    }

                                    if(banMessage) {
                                        LogUtils.d(TAG, "Procesando mensaje para servidor");
                                        final String date = DateUtils.getDateFormatted();
                                        String prefijo = NetworkManager.getInstance().isNetworkConnected() ? "" : "SI";
                                        
                                        // Unificación de formato de error_actual
                                        error_actual = prefijo + "###" + date + error_actual;

                                        jsonArray = new JSONArray();
                                        DataServer.getInstance().enviarMsgAlServer(error_actual, TipoEnvioDef.COMANDO);
                                        DataServer.getInstance().servicesCheckProcesos.banMessage = false;

                                        if(PrincipalActivity.banSend) {
                                            if(PrincipalActivity.isConcatenar) banMessage = true;
                                        } else {
                                            banMessage = false;
                                        }

                                        if(Ban_Message_velocidad) {
                                            Ban_Message_velocidad = false;
                                            Intent intentData3 = new Intent(ProgressManager.MSG_OBD);
                                            intentData3.putExtra("DATA", "APAGAR");
                                            service.sendBroadcast(intentData3);
                                        }
                                    }

                                    StateNotification.getInstance().notifyState("Comando: " + ComandoOBD + "::" + error_actual);
                                    final JSONObject jsonObject = new JSONObject();
                                    jsonObject.put("Comando", ComandoOBD);
                                    jsonObject.put("Respuesta", error_actual);
                                    jsonArray.put(jsonObject);

                                    ConnectionManager.getInstance().comprobarRespuesta(ComandoOBD, error_actual);

                                    if (ConnectionManager.getInstance().getEstado().equals("Encendido") && primeraConexion) {
                                        DataServer.getInstance().agregarEncuesta();
                                        primeraConexion = false;
                                    }

                                    if(banderaComandos && error_actual.contains(NODATA) && bluetoothEncendido) {
                                        final Intent intent = new Intent(ProgressManager.MSG_OBD);
                                        intent.putExtra("DATA", "APAGAR");
                                        service.sendBroadcast(intent);
                                    }

                                    if(PrincipalActivity.banSend && bluetoothEncendido) {
                                        if(PrincipalActivity.isConcatenar && !banMessage) {
                                            error_actual = CheckearColaComandos.strConcat + error_actual.replace(CheckearColaComandos.strReplace, "");
                                            service.sendBroadcast(new Intent(ProgressManager.SUM_CONTADOR));
                                        }

                                        Intent intent = new Intent(ProgressManager.MSG_OBD);
                                        intent.putExtra("BAN", true);
                                        intent.putExtra("DATA", error_actual);
                                        service.sendBroadcast(intent);

                                        if(PrincipalActivity.isEnviar) {
                                            if(NetworkManager.getInstance().isOnline(ResourceManager.getInstance().getContext())) {
                                                String cadenaToSend = diagnosticoSI.length() > 0 ? "%%" + diagnosticoSI + error_actual : "%%" + error_actual;
                                                diagnosticoSI = "";
                                                DataServer.getInstance().enviarMsgAlServer(cadenaToSend, TipoEnvioDef.DIAGNOSTICO);
                                            } else {
                                                diagnosticoSI += (diagnosticoSI.isEmpty() ? "SI" : "") + error_actual + "XX";
                                            }
                                        }

                                        DataServer.getInstance().servicesCheckProcesos.popProceso();
                                        CheckearColaComandos.banEnviar = false;
                                        CheckearColaComandos.strConcat = "";
                                    } else {
                                        if(PrincipalActivity.isConcatenar && bluetoothEncendido ) {
                                            CheckearColaComandos.strConcat += error_actual.replace(CheckearColaComandos.strReplace, "");
                                        } else if(!errorSent.equals(error_actual) && bluetoothEncendido) {
                                            if(PrincipalActivity.isEnviar && validaRespuesta03(error_actual)) {
                                                final String date = DateUtils.getDateFormatted();
                                                if(NetworkManager.getInstance().isOnline(ResourceManager.getInstance().getContext())) {
                                                    String formatted = "###" + date + error_actual;
                                                    String cadenaToSend = fallaSI.length() > 0 ? fallaSI + "###" + formatted : formatted;
                                                    fallaSI = "";
                                                    DataServer.getInstance().enviarMsgAlServer(cadenaToSend, TipoEnvioDef.COMANDO_4300);
                                                } else {
                                                    fallaSI += (fallaSI.isEmpty() ? "SI" : "") + "###" + date + error_actual + "XX";
                                                }
                                            }
                                            errorSent = error_actual;
                                        }
                                        DataServer.getInstance().servicesCheckProcesos.popProceso();
                                        CheckearColaComandos.banEnviar = false;
                                    }

                                    Intent intentData3 = new Intent(ProgressManager.MSG_OBD);
                                    intentData3.putExtra("BAN", false);
                                    intentData3.putExtra("DATA", error_actual);
                                    service.sendBroadcast(intentData3);
                                    mSb = new StringBuilder();
                                }
                            } else {
                                mSb = new StringBuilder();
                                comandoAuxiliar = true;
                                ComandoOBD = respuestaAuxiliar(ComandoOBD);
                                write(ComandoOBD);
                            }
                        } else {
                            mSb.append(strReceived);
                        }
                    }
                } catch (Exception ex) {
                    final String messageError = ex.toString();
                    if(messageError.contains("socket closed")) {
                        if (!intentandoConexion){
                            String parametro = new LocalData().getCodigo( "forceClose");
                            if ("false".equalsIgnoreCase(parametro)){
                                intentandoConexion = true;
                                bluetoothEncendido = false;
                                this.outputStream = null;
                                this.inputStream = null;
                                iniciarConexion();
                            }
                        }
                    }
                }
            } else {
                if (!intentandoConexion){
                    intentandoConexion = true;
                    iniciarConexion();
                }
            }
        }
    }

    public boolean respuestaValida(String comando, String respuestaInicial, Boolean comAuxiliar) {
        if (comAuxiliar) {
            comandoAuxiliar = false;
            return true;
        }
        if (comando != null && !comando.contains("AT") && !comando.contains(Constants.comandoEncendido) && !comando.contains(Constants.comandoEncendido1)){
            return !respuestaInicial.contains(NODATA);
        }
        return true;
    }

    public String respuestaAuxiliar(String comando){
        if ("012F".equals(comando)) return "22F42F";
        return comando;
    }

    public boolean validaRespuesta03(String codigoRespuesta){
        if (codigoRespuesta == null || codigoRespuesta.toLowerCase().contains("n") || codigoRespuesta.length() < 3) return false;
        for (char caracter : codigoRespuesta.substring(2).toCharArray()){
            if (caracter != '0') return true;
        }
        return false;
    }
}