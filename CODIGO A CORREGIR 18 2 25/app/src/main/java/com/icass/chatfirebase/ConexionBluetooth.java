package com.icass.chatfirebase;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import com.icass.chatfirebase.activity.MainActivity2;
import com.icass.chatfirebase.activity.PrincipalActivity;
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
import java.util.UUID;

public class ConexionBluetooth extends Thread {
    private static final String TAG = ConexionBluetooth.class.getSimpleName();

    private boolean banHiloSiempre;
    public boolean banIniciar = true;
    private boolean bluetoothEncendido = false;
    private boolean intentandoConexion = false;
    private boolean comandoAuxiliar = false;

    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    @NonNull
    private final BluetoothDevice bluetoothDevice;
    public boolean banMessage;
    public String diagnosticoSI = "";
    public String fallaSI = "";
    public boolean banApagado = false;
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

    public ConexionBluetooth(@NonNull BluetoothDevice bluetoothDevice, @NonNull Service service, @NonNull CallbackService listener) {
        this.bluetoothDevice = bluetoothDevice;
        this.service = service;
        this.banHiloSiempre = true;
        primeraConexion = true;

        DataServer.initInstance().initialize(service, (comandoEnv, banMess) -> {
            try {
                ComandoOBD = comandoEnv;
                banMessage = banMess;
                write(comandoEnv);
                banderaComandos = comandoEnv.contains(comando0105);
            } catch (Exception ex) {
                Log.e(TAG, "ERROR AL ENVIAR AL OBD: " + ex);
            }
        }, listener);
    }

    private void iniciarConexion() {
        LogUtils.d(TAG, "Intentando Conexion Bluetooth con: " + bluetoothDevice.getName());
        try {
            final ParcelUuid[] uuids = this.bluetoothDevice.getUuids();
            for (ParcelUuid uuid : uuids) {
                this.socket = this.bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid.toString()));
                this.socket.connect();
                this.outputStream = socket.getOutputStream();
                this.inputStream = socket.getInputStream();
                break;
            }
            intentandoConexion = false;
            bluetoothEncendido = true;
            DataServer.getInstance().servicesCheckProcesos.resetBanderas();
            iniciarEnvioConfiguracion();
        } catch (IOException ex) {
            ConnectionManager.getInstance().setEstadoActual(EstadoConexionDef.ESTADO_DESCONECTADO);
            ProgressManager.getInstance().setProgreso(ProgresoDef.ESTADO_DESCONECTADO);
            Utils.sleep(TAG + " Reintentando en 30s", 30000);
            intentandoConexion = false;
            iniciarConexion();
        }
    }

    private void iniciarEnvioConfiguracion() {
        crearAlarma();
        try {
            DataServer.getInstance().banHiloSiempre = true;
            if (ComandoOBD != null && ComandoOBD.trim().length() >= 2) {
                DataServer.getInstance().setInitComandosWeb();
            } else {
                DataServer.getInstance().initConfiguracion("Conexion Bluetooth");
            }
        } catch (Exception ex) {
            handleReconnection();
        }
    }

    public void write(@NonNull String message) throws IOException {
        message = message.contains("AT") ? message : message + "1";
        final String data = message + "\r";
        if (outputStream != null) {
            outputStream.write(data.getBytes());
            outputStream.flush();
        } else {
            handleReconnection();
        }
    }

    @Override
    public void run() {
        LogUtils.d(TAG, "-------- Hilo BT Iniciado (Fix Multitrama) ----------");
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
                    if (bytesRead <= 0) continue;

                    String strReceived = new String(buffer, 0, bytesRead);
                    mSb.append(strReceived);

                    // Esperamos el prompt '>' que indica fin de respuesta
                    if (strReceived.contains(">")) {
                        String respuestaRaw = mSb.toString().trim();
                        
                        // 1. Procesamiento según el comando
                        if (ComandoOBD != null && ComandoOBD.contains("0902")) {
                            respuestaOBD = procesarVIN(respuestaRaw);
                        } else if (ComandoOBD != null && ComandoOBD.equals("03")) {
                            respuestaOBD = respuestaRaw.replaceAll("[\r\n ]", "").replace(">", "");
                            Ban_Message_velocidad = true;
                            soloComando(comando);
                        } else {
                            respuestaOBD = respuestaRaw.replaceAll("[\r\n ]", "").replace(">", "");
                        }

                        // 2. Validación y envío
                        if (respuestaValida(ComandoOBD, respuestaOBD, comandoAuxiliar)) {
                            procesarRespuestaValida(respuestaOBD);
                        } else {
                            liberarCola();
                        }
                        mSb.setLength(0);
                    }
                } catch (IOException e) {
                    handleReconnection();
                    break;
                }
            }
        }
    }

    private String procesarVIN(String rawData) {
        String limpio = rawData.replace(">", "").replace("0902", "").trim();
        String[] tramas = limpio.split("\r");
        StringBuilder vinHex = new StringBuilder();
        for (String trama : tramas) {
            String t = trama.trim().replace(" ", "");
            if (t.isEmpty()) continue;
            if (t.matches("^[0-9]:.*")) {
                String contenido = t.substring(2);
                if (contenido.startsWith("4902")) vinHex.append(contenido.substring(6));
                else vinHex.append(contenido);
            } else if (t.length() > 10) {
                if (t.startsWith("4902")) vinHex.append(t.substring(6));
                else vinHex.append(t);
            }
        }
        String resultado = vinHex.toString();
        return resultado.length() > 34 ? resultado.substring(resultado.length() - 34) : resultado;
    }

    private void procesarRespuestaValida(String respuesta) {
        if (DataServer.getInstance().servicesCheckProcesos != null) {
            DataServer.getInstance().servicesCheckProcesos.comandoSiguiente = true;
        }

        if (banMessage) {
            final String date = DateUtils.getDateFormatted();
            String prefix = NetworkManager.getInstance().isNetworkConnected() ? "" : "SI";
            DataServer.getInstance().enviarMsgAlServer(prefix + "###" + date + respuesta, TipoEnvioDef.COMANDO);
            banMessage = false;
        }

        StateNotification.getInstance().notifyState("OBD: " + ComandoOBD + " -> " + respuesta);
        liberarCola();
    }

    private void liberarCola() {
        if (DataServer.getInstance().servicesCheckProcesos != null) {
            DataServer.getInstance().servicesCheckProcesos.comandoSiguiente = true;
            DataServer.getInstance().servicesCheckProcesos.popProceso();
        }
    }

    private void handleReconnection() {
        if (!intentandoConexion) {
            intentandoConexion = true;
            bluetoothEncendido = false;
            iniciarConexion();
        }
    }

    private void soloComando(@NonNull String comandoEnv) {
        if (DataServer.getInstance().servicesCheckProcesos != null) {
            final Procesos proceso = new Procesos(true, comandoEnv, false, true);
            DataServer.getInstance().servicesCheckProcesos.pushProcesoMessage(proceso);
        }
    }

    public boolean respuestaValida(String comando, String respuestaInicial, Boolean comAuxiliar) {
        if (comAuxiliar) { comandoAuxiliar = false; return true; }
        if (comando == null) return false;
        if (!comando.contains("AT") && !comando.contains(Constants.comandoEncendido)) {
            return !respuestaInicial.contains(NODATA);
        }
        return true;
    }

    public void crearAlarma() {
        final Context context = ResourceManager.getInstance().getContext();
        if (alarmManager == null) {
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, alarmEstadoVehiculo.class);
            pendingIntent = PendingIntent.getBroadcast(context, 7235, intent, PendingIntent.FLAG_IMMUTABLE);
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), Constants.INTERVAL_30_SECONDS, pendingIntent);
        }
    }

    public void cerrar() {
        try {
            bluetoothEncendido = false;
            this.banHiloSiempre = false;
            if (this.socket != null) this.socket.close();
            banApagado = true;
        } catch (Exception ex) {
            Log.e(TAG, "Error al cerrar socket");
        }
    }
}