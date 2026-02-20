package com.icass.chatfirebase.managers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.icass.chatfirebase.R;
import com.icass.chatfirebase.notifications.StateNotification;
import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.NotificationsUtils;
import com.icass.chatfirebase.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;

public class ConnectionManager {
    private static final String TAG = ConnectionManager.class.getSimpleName();
    private static volatile ConnectionManager instance;

    private Thread hiloSupervisar; // Thread es un único flujo de control dentro de un programa.
    private int estadoActual = EstadoConexionDef.ESTADO_APAGADO;
    public static final int TIEMPO_ESPERA = 30000;
    public static final int NOTIFICATION_ID = 9008;
    public int banderaInicio = 0;
    public int contador = 0;
    public String bufferComandoEncendido= "";
    public boolean encendido06 = false;

    public EstadoVehiculo estadoVehiculo = null;
    public boolean estadoApagado = false;

    @NonNull
    private final HashMap<Integer, Boolean> result = new HashMap<>();

    @NonNull
    final Handler handler = new Handler(Looper.getMainLooper());//Hacer el cmabio con Mylopper y observar funcionamiento

    private int progress = 0;

    private ConnectionManager() {
        this.result.put(1, false);
        this.result.put(2, false);
        this.result.put(3, false);
        this.result.put(4, false);
        this.result.put(5, false);
    }

    public static ConnectionManager getInstance() {
        if(instance == null) {
            synchronized (ConnectionManager.class) {
                if(instance == null) {
                    instance = new ConnectionManager();
                }
            }
        }

        return instance;
    }

    public void initialize() {
        // Iniciar supervisión del vehículo
        // supervisar();
    }

    public void setEstadoActual(@EstadoConexionDef int estadoActual) {
        this.estadoActual = estadoActual;
        actualizarEstado();
        LogUtils.d(TAG,"Actualizacion de Estado a : " + getEstado());
        StateNotification.getInstance().notifyState();
    }

    public void setResult(int id, boolean result) {
        this.result.put(id, result);
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean statusConfiguracion() {
        final Boolean result1 = result.get(1);
        final Boolean result2 = result.get(2);
        final Boolean result3 = result.get(3);
        final Boolean result4 = result.get(4);
        final Boolean result5 = result.get(5);

        LogUtils.d(TAG,"Status configuracion OBDII: {R1: " + result1 + ", " + "R2: " + result2 + ", " + "R3: " + result3 + ", " + "R4: " + result4 + ", " + "R5: " + result5 + "}");
        return result1 != null && result2 != null && result3 != null && result4 != null && result5 != null && result1 && result2 && result3 && result4 && result5;
    }

    public int getProgress() {
        return progress;
    }

    public boolean isDesconectado() { return estadoActual == EstadoConexionDef.ESTADO_DESCONECTADO;}

    public boolean isEncendido() {
        return estadoActual == EstadoConexionDef.ESTADO_ENCENDIDO;
    }

    public boolean isApagado() {
        return estadoActual == EstadoConexionDef.ESTADO_APAGADO;
    }

    public boolean isContacto() {
        return estadoActual == EstadoConexionDef.ESTADO_CONTACTO;
    }

    public boolean isConectado() {
        return estadoActual == EstadoConexionDef.ESTADO_CONECTADO;
    }



    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            EstadoConexionDef.ESTADO_DESCONECTADO,
            EstadoConexionDef.ESTADO_APAGADO,
            EstadoConexionDef.ESTADO_CONTACTO,
            EstadoConexionDef.ESTADO_ENCENDIDO,
            EstadoConexionDef.ESTADO_CONECTADO
    })
    public @interface EstadoConexionDef {
        int ESTADO_DESCONECTADO = 1;
        int ESTADO_APAGADO = 2;
        int ESTADO_CONTACTO = 3;
        int ESTADO_ENCENDIDO = 4;
        int ESTADO_CONECTADO = 5;
    }

    public interface EstadoVehiculo {
        void reportar();
    }

    public void comprobarRespuesta(@NonNull String comandoOBD, @NonNull String respuestaInicial) {//TODO
        if(comandoOBD.equals("01" + Constants.comandoEncendido) || comandoOBD.equals("01" + Constants.comandoEncendido1)) {
            Log.d("TEST", "Validando comando encendido en metodo comprobarRespuesta");
            String respuesta = respuestaInicial
                    .replace("SEARCHING...", "")
                    .replace("ACT", "")
                    .replace("ALERT", "")
                    .trim();

            respuesta = respuesta.split(" ").length > 1 ? respuesta.split(" ")[1]: respuesta;//TODO

            LogUtils.d(TAG,"Comando enviado: |" + comandoOBD + "|");
            LogUtils.d(TAG,"Respuesta del comando: |" + respuesta + "|");

            if (respuesta.toLowerCase().contains("n")) {
                estadoApagado = true;
                ConnectionManager.getInstance().setEstadoActual(EstadoConexionDef.ESTADO_APAGADO);//Aqui cambia el estado a apagado,
                return;
            }
            if (comandoOBD.equals("01" + Constants.comandoEncendido)) {
                if (!respuesta.contains("0000")) {
                    encendido06 = true;
                } else {
                    encendido06 = false;
                }
                estadoApagado = false;
            } else if (comandoOBD.equals("01" + Constants.comandoEncendido1)) {
                if (!respuesta.contains("0000") && encendido06) {
                    ConnectionManager.getInstance().setEstadoActual(EstadoConexionDef.ESTADO_ENCENDIDO);
                } else if (!estadoApagado){
                    ConnectionManager.getInstance().setEstadoActual(EstadoConexionDef.ESTADO_CONTACTO);
                }
                estadoApagado = false;
                encendido06 = false;
            }
//        } else if(comandoOBD.equals("ATZ") || comandoOBD.equals("ATSP0") || comandoOBD.equals("ATL1") || comandoOBD.equals("ATS0") || comandoOBD.equals("ATE0")) {
        } else if(comandoOBD.equals("ATZ") || comandoOBD.equals(Constants.comandoATSP) || comandoOBD.equals("ATL1") || comandoOBD.equals("ATS0") || comandoOBD.equals("ATE0")) {
            final String respuesta = respuestaInicial
                    .replace("SEARCHING...", "")
                    .replace(" ", "")
                    .trim();

            LogUtils.d(TAG,"comprobarRespuesta: |" + comandoOBD + "|");
            LogUtils.d(TAG,"Respuesta: |" + respuesta + "|");

            switch (comandoOBD) {
                case "ATZ": {
                    setResult(1, respuesta.contains("L") || respuesta.contains("M") || respuesta.contains("O"));
                    break;
                }

               // case "ATSP0":
                case "ATSP00":{
                    setResult(2, respuesta.contains("OK"));
                    break;
                }

                case "ATL1": {
                    setResult(3, respuesta.contains("OK"));
                    break;
                }

                case "ATS0": {
                    setResult(4, respuesta.contains("OK"));
                    break;
                }

                case "ATE0": {
                    setResult(5, respuesta.contains("OK"));
                    break;
                }
            }
        } else {
            LogUtils.d(TAG,"Comando: " + comandoOBD + " - Respuesta: |" + respuestaInicial + "|");
        }
    }

    public void lanzarSupervisar(){
        supervisar(2, estadoVehiculo);
    }

    public void supervisar(int modo, @NonNull EstadoVehiculo listener) {//Aqui hay que meter el contador para solicitarlo en alarMlogger Receiver
        LogUtils.d(TAG,"Metodo supervisar L214");

        if(banderaInicio == 0 || modo == 2) {
            if (banderaInicio == 0) {
                Log.d(TAG, "Se asigno el listener de Vehiculo");
                estadoVehiculo = listener;
            }
            banderaInicio = 1;//Se revisa cada 30 segundos
            String cadena = modo == 1 ? "Se llama de DataServer" : "Se llama desde la alarma";
            Log.d(TAG, cadena);
        } else {
            Log.d(TAG, "Ya se encuentra supervisando, salir");
            return;
        }

        if(isDesconectado()) {
            LogUtils.d(TAG,"No esta conectado el Bluetooth");
        } else if(isEncendido()) {
            LogUtils.d(TAG,"isEncendido, continua proceso normal");
        } else if(isContacto()) {
            LogUtils.d(TAG,"isContacto");
        } else if(isApagado()) {
            LogUtils.d(TAG,"isApagado");
        }

        listener.reportar();
    }

    private void retardar(@NonNull EstadoVehiculo listener) {
        LogUtils.d(TAG, "Se llama al metodo retardar");

        if (hiloSupervisar == null){ //Se valida si hay un hilo asignado
            LogUtils.d(TAG, "Se crea el hilo en el metodo retardar, para llamar a supervisar");
            hiloSupervisar = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true){
                        LogUtils.d(TAG, "Hilo duerme 30 segundos");
                        Utils.sleep(TAG, 30000);
                        LogUtils.d(TAG, "Hilo despierta y ejecuta supervisar + ID del hilo " + hiloSupervisar.getId());
                        supervisar(2, listener);
                    }
                }
            });
            hiloSupervisar.start();
        } else {
            LogUtils.d(TAG, "Ya hay un hilo ejecutandose");
        }

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    LogUtils.d(TAG, "Hilo duerme 30 segundos");
//                    Thread.sleep(TIEMPO_ESPERA);
//                    LogUtils.d(TAG, "Hilo despierta y ejecuta supervisar");
//                    supervisar(2, listener);
//                } catch (InterruptedException e) {
//                    LogUtils.e(TAG, "Error en el hilo del metodo retardar");
//                }
//            }
//        }).start();

//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                // Repetir supervisión del vehículo
//                supervisar(2, listener);//metodo para obtener el estado del vehiculo
//            }
//        }, TIEMPO_ESPERA);
    }

    public void actualizarEstado(@NonNull Service service) {
        final Context context = ResourceManager.getInstance().getContext();
        service.startForeground(NOTIFICATION_ID, getNotification(context));
    }

    public void actualizarEstado() {
        final Context context = ResourceManager.getInstance().getContext();
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, getNotification(context));
    }

    @NonNull
    public Notification getNotification(@NonNull Context context) {
        final boolean encendidoOrContactoOrApagado = ConnectionManager.getInstance().isEncendido() ||
                ConnectionManager.getInstance().isContacto() ||
                ConnectionManager.getInstance().isApagado();

        final String estado = ConnectionManager.getInstance().getEstado();
        final String result;
        final int max = 180;
        final int progress = ConnectionManager.getInstance().getProgress();

        if(encendidoOrContactoOrApagado) {
            result = "Estado del vehículo: " + estado;
        } else {
            result = estado;
        }

        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification);
        final String CHANNEL_ID = NotificationsUtils.createNotificationChannel();
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);

        notificationBuilder.setOngoing(true)
                .setContentTitle("OBD WEB")
                .setContentText(result)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(bitmap)
                .setOnlyAlertOnce(true);

        if(progress != 0 && Constants.PRUEBAS) {
            notificationBuilder.setProgress(max, progress, false);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_MIN);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setCategory(Notification.CATEGORY_SERVICE);
        }

        return notificationBuilder.build();
    }

    @NonNull
    public String getEstado() {
        if(isDesconectado()) {
            return "SIN CONEXIÓN AL OBDII";
        } else if(isEncendido()) {
            return "ENCENDIDO";
        } else if(isApagado()) {
            return "APAGADO";
        } else if(isContacto()) {
            return "CONTACTO";
        } else if (isConectado()){
            return "CONECTADO";
        }
        return "DESCONOCIDO";
    }
}