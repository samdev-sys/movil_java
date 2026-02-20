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

import com.icass.chatfirebase.broadcasts.alarmEstadoVehiculo;
import com.icass.chatfirebase.interfaces.CallbackService;
import com.icass.chatfirebase.managers.ConnectionManager;
import com.icass.chatfirebase.managers.ConnectionManager.EstadoConexionDef;
import com.icass.chatfirebase.managers.NetworkManager;
import com.icass.chatfirebase.managers.ResourceManager;
import com.icass.chatfirebase.notifications.StateNotification;
import com.icass.chatfirebase.services.CheckearColaComandos.EventCheckProcesos;
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

        DataServer.initInstance().initialize(service, new EventCheckProcesos() {
            @Override
            public void enviarOBD(String comandoEnv, boolean banMess) {
                try {
                    ComandoOBD = comandoEnv;
                    banMessage = banMess;
                    write(comandoEnv);
                } catch (Exception ex) {
                    Log.e(TAG, "Error al enviar: " + ex.getMessage());
                }
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
        LogUtils.d(TAG, "Hilo iniciado - Monitor de VIN Multitrama activo");
        this.banHiloSiempre = true;
        if (!intentandoConexion) {
            intentandoConexion = true;
            iniciarConexion();
        }

        StringBuilder bufferAcumulado = new StringBuilder();

        while (this.banHiloSiempre) {
            if (inputStream != null) {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead <= 0) continue;

                    String fragmento = new String(buffer, 0, bytesRead);
                    bufferAcumulado.append(fragmento);

                    // CLAVE: El adaptador ELM327 envía ">" al terminar la respuesta completa
                    if (fragmento.contains(">")) {
                        String respuestaCompleta = bufferAcumulado.toString().trim();
                        LogUtils.d(TAG, "Respuesta Raw: " + respuestaCompleta);

                        if (ComandoOBD != null && ComandoOBD.contains("0902")) {
                            // PROCESAMIENTO MULTITRAMA PARA EL VIN
                            respuestaOBD = procesarVIN(respuestaCompleta);
                        } else if (ComandoOBD != null && ComandoOBD.equals("03")) {
                            // Lógica para códigos de error
                            respuestaOBD = respuestaCompleta.replaceAll("[\r\n ]", "").replace(">", "");
                            soloComando("1"); 
                        } else {
                            // Respuesta estándar
                            respuestaOBD = respuestaCompleta.replaceAll("[\r\n ]", "").replace(">", "");
                        }

                        if (respuestaValida(ComandoOBD, respuestaOBD, comandoAuxiliar)) {
                            enviarYContinuar(respuestaOBD);
                        } else {
                            desbloquearCola();
                        }
                        
                        bufferAcumulado.setLength(0); // Limpiar buffer para el siguiente comando
                    }
                } catch (IOException e) {
                    handleError();
                    break;
                }
            }
        }
    }

    private String procesarVIN(String rawData) {
        String data = rawData.replace(">", "").replace("0902", "").trim();
        String[] lineas = data.split("\r");
        StringBuilder vinFinal = new StringBuilder();

        for (String linea : lineas) {
            String t = linea.trim().replace(" ", "");
            if (t.isEmpty()) continue;

            // Filtro para tramas CAN (0:, 1:, 2:)
            if (t.matches("^[0-9]:.*")) {
                String clean = t.substring(2);
                if (clean.startsWith("4902")) vinFinal.append(clean.substring(6));
                else vinFinal.append(clean);
            } 
            else if (t.length() > 10) {
                if (t.startsWith("4902")) vinFinal.append(t.substring(6));
                else vinFinal.append(t);
            }
        }

        String result = vinFinal.toString();
        // El VIN son 17 caracteres (34 en hexadecimal)
        return result.length() > 34 ? result.substring(result.length() - 34) : result;
    }

    private void enviarYContinuar(String respuesta) {
        // Desbloquear flujo de la cola de comandos
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

    private void soloComando(@NonNull String comandoEnv) {
        if(DataServer.getInstance().servicesCheckProcesos != null) {
            final Procesos proceso = new Procesos(true, comandoEnv, false, true);
            DataServer.getInstance().servicesCheckProcesos.pushProcesoMessage(proceso);
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

    public void cerrar() {
        try {
            this.banHiloSiempre = false;
            if(this.socket != null) this.socket.close();
            this.inputStream = null;
            this.outputStream = null;
        } catch (Exception ex) {
            LogUtils.e(TAG,"Error al cerrar: " + ex.getMessage());
        }
    }
}