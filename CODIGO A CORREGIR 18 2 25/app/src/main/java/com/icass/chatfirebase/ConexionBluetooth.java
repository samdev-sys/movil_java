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

import com.icass.chatfirebase.activity.PrincipalActivity;
import com.icass.chatfirebase.broadcasts.alarmEstadoVehiculo;
import com.icass.chatfirebase.interfaces.CallbackService;
import com.icass.chatfirebase.managers.ConnectionManager;
import com.icass.chatfirebase.managers.ConnectionManager.EstadoConexionDef;
import com.icass.chatfirebase.managers.NetworkManager;
import com.icass.chatfirebase.managers.ProgressManager;
import com.icass.chatfirebase.managers.ProgressManager.ProgresoDef;
import com.icass.chatfirebase.managers.ResourceManager;
import com.icass.chatfirebase.notifications.StateNotification;
import com.icass.chatfirebase.services.Procesos;
import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.DateUtils;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.Utils;
import com.icass.chatfirebase.utils.Utils.TipoEnvioDef;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ConexionBluetooth extends Thread {
    private static final String TAG = "ConexionBluetooth";

    private boolean banHiloSiempre;
    private boolean intentandoConexion = false;
    private boolean comandoAuxiliar = false;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    @NonNull
    private final BluetoothDevice bluetoothDevice;
    public boolean banMessage;
    private String comando = "1";
    private String ComandoOBD = "";
    private final Service service;
    private InputStream inputStream;
    private OutputStream outputStream;
    private BluetoothSocket socket;
    private String respuestaOBD = "";

    public ConexionBluetooth(@NonNull BluetoothDevice bluetoothDevice, @NonNull Service service, @NonNull CallbackService listener) {
        this.bluetoothDevice = bluetoothDevice;
        this.service = service;
        this.banHiloSiempre = true;

        DataServer.initInstance().initialize(service, (comandoEnv, banMess) -> {
            try {
                ComandoOBD = comandoEnv;
                banMessage = banMess;
                write(comandoEnv);
            } catch (Exception ex) {
                Log.e(TAG, "Error al enviar: " + ex.getMessage());
            }
        }, listener);
    }

    private void iniciarConexion() {
        LogUtils.d(TAG, "Conectando a: " + bluetoothDevice.getName());
        try {
            ParcelUuid[] uuids = this.bluetoothDevice.getUuids();
            for (ParcelUuid uuid : uuids) {
                this.socket = this.bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid.toString()));
                this.socket.connect();
                this.outputStream = socket.getOutputStream();
                this.inputStream = socket.getInputStream();
                break;
            }
            intentandoConexion = false;
            iniciarEnvioConfiguracion();
        } catch (IOException ex) {
            ConnectionManager.getInstance().setEstadoActual(EstadoConexionDef.ESTADO_DESCONECTADO);
            Utils.sleep("Reintentando en 15s", 15000);
            intentandoConexion = false;
            iniciarConexion();
        }
    }

    private void iniciarEnvioConfiguracion() {
        crearAlarma();
        try {
            if (ComandoOBD != null && ComandoOBD.trim().length() >= 2) {
                DataServer.getInstance().setInitComandosWeb();
            } else {
                DataServer.getInstance().initConfiguracion("Inicio");
            }
        } catch (Exception e) {
            handleError();
        }
    }

    public void write(@NonNull String message) throws IOException {
        String data = (message.contains("AT") ? message : message + "1") + "\r";
        if (outputStream != null) {
            outputStream.write(data.getBytes());
            outputStream.flush();
        } else {
            handleError();
        }
    }

    @Override
    public void run() {
        LogUtils.d(TAG, "Hilo iniciado - Corrección Multitrama activa");
        this.banHiloSiempre = true;
        if (!intentandoConexion) {
            intentandoConexion = true;
            iniciarConexion();
        }

        StringBuilder mSb = new StringBuilder();

        while (this.banHiloSiempre) {
            if (inputStream != null) {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead <= 0) continue;

                    String part = new String(buffer, 0, bytesRead);
                    mSb.append(part);

                    // Esperamos el prompt '>' que indica fin de transmisión del OBD
                    if (part.contains(">")) {
                        String fullResponse = mSb.toString().trim();
                        LogUtils.d(TAG, "Raw acumulado: " + fullResponse);

                        if (ComandoOBD != null && ComandoOBD.contains("0902")) {
                            // PROCESAMIENTO ESPECIAL PARA VIN (Múltiples líneas)
                            respuestaOBD = procesarVIN(fullResponse);
                        } else {
                            // PROCESAMIENTO ESTÁNDAR (Comandos cortos)
                            respuestaOBD = fullResponse.replaceAll("[\r\n ]", "").replace(">", "");
                        }

                        if (respuestaValida(ComandoOBD, respuestaOBD, comandoAuxiliar)) {
                            enviarYContinuar(respuestaOBD);
                        } else {
                            desbloquearCola();
                        }
                        
                        mSb.setLength(0); // Limpiamos buffer para el siguiente comando
                    }
                } catch (IOException e) {
                    handleError();
                    break;
                }
            }
        }
    }

    private String procesarVIN(String rawData) {
        // Elimina ecos y prompt
        String data = rawData.replace(">", "").replace("0902", "").trim();
        String[] tramas = data.split("\r");
        StringBuilder vinBuilder = new StringBuilder();

        for (String trama : tramas) {
            String t = trama.trim().replace(" ", "");
            if (t.isEmpty()) continue;

            // Filtro para tramas CAN (0:, 1:, 2:)
            if (t.matches("^[0-9]:.*")) {
                String clean = t.substring(2);
                if (clean.startsWith("4902")) vinBuilder.append(clean.substring(6));
                else vinBuilder.append(clean);
            } 
            else if (t.length() > 10) {
                if (t.startsWith("4902")) vinBuilder.append(t.substring(6));
                else vinBuilder.append(t);
            }
        }

        String result = vinBuilder.toString();
        // El VIN son 17 caracteres (34 en hex). Tomamos los últimos 34.
        return result.length() > 34 ? result.substring(result.length() - 34) : result;
    }

    private void enviarYContinuar(String respuesta) {
        // Desbloquear flujo para que no se congele la cola
        if (DataServer.getInstance().servicesCheckProcesos != null) {
            DataServer.getInstance().servicesCheckProcesos.comandoSiguiente = true;
        }

        if (banMessage) {
            String date = DateUtils.getDateFormatted();
            String prefix = NetworkManager.getInstance().isNetworkConnected() ? "" : "SI";
            DataServer.getInstance().enviarMsgAlServer(prefix + "###" + date + respuesta, TipoEnvioDef.COMANDO);
            banMessage = false;
        }

        StateNotification.getInstance().notifyState("OBD " + ComandoOBD + ": " + respuesta);
        desbloquearCola();
    }

    private void desbloquearCola() {
        if (DataServer.getInstance().servicesCheckProcesos != null) {
            DataServer.getInstance().servicesCheckProcesos.comandoSiguiente = true;
            DataServer.getInstance().servicesCheckProcesos.popProceso();
        }
    }

    private void handleError() {
        if (!intentandoConexion) {
            intentandoConexion = true;
            iniciarConexion();
        }
    }

    public boolean respuestaValida(String comando, String respuestaInicial, Boolean comAuxiliar) {
        if (comAuxiliar) { comandoAuxiliar = false; return true; }
        if (comando == null) return false;
        return !respuestaInicial.contains("DATA") && !respuestaInicial.contains("ERROR");
    }

    public void crearAlarma() {
        Context context = ResourceManager.getInstance().getContext();
        if (alarmManager == null) {
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, alarmEstadoVehiculo.class);
            pendingIntent = PendingIntent.getBroadcast(context, 7235, intent, PendingIntent.FLAG_IMMUTABLE);
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), Constants.INTERVAL_30_SECONDS, pendingIntent);
        }
    }
}