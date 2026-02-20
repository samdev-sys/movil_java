package com.icass.chatfirebase.services;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.icass.chatfirebase.ConexionBluetooth;
import com.icass.chatfirebase.DataServer;
import com.icass.chatfirebase.StartVelocida.ServicesGPS;
import com.icass.chatfirebase.StartVelocida.StartVel;
import com.icass.chatfirebase.activity.MainActivity2;
import com.icass.chatfirebase.interfaces.CallbackService;
import com.icass.chatfirebase.managers.ConnectionManager;
import com.icass.chatfirebase.managers.ProgressManager;
import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.NotificationsUtils;

import java.util.Set;

public class ConexionService extends Service {
    private static final String TAG = ConexionService.class.getSimpleName();

    private ConexionBluetooth conexionBluetooth;

    @NonNull
    private final BroadcastReceiver firebaseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtils.d(TAG, "Se llama el receiver en ConexionService");
            final String command = intent.getStringExtra("command");

            if (conexionBluetooth != null && DataServer.getInstance().servicesCheckProcesos != null && command != null) {
                final Procesos procesos = new Procesos(true, command, false, true); // Crea un objeto para la clase "Procesos" para instanciar está clase.
                DataServer.getInstance().servicesCheckProcesos.pushProcesoMessage(procesos);
                DataServer.getInstance().servicesCheckProcesos.resetBanderas();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        LogUtils.d(TAG, "ConexionService: onCreate");

        try {
            LogUtils.d(TAG, "Se regsitra el receiver en " + TAG);

            registerReceiver(firebaseReceiver, new IntentFilter("FirebaseCommandReceived"));
        } catch (Exception ex) {
            LogUtils.e(TAG, "ConexionService: onCreate" + ex);
        }

        startForegroundService(); //Se inicia las notificaciones

        LogUtils.d(TAG,"Se inicia la configuracion del bluetooth ");
        // Inicia la configuración del bluetooth.
        initConfigBluetooth();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {//Metodo se manda a traer cuando se cierra la visualizacion de la app en el segundo plano

        LogUtils.d(TAG,"Se ejecuta el metodo onStartComand de ConexionServices");

        final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.UNLOCK_TAG);
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);

        return Service.START_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        final Intent restartServiceIntent = new Intent(getApplicationContext(), ConexionService.class);
        restartServiceIntent.setPackage(getPackageName());

        final PendingIntent restartServicePendingIntent = PendingIntent.getService(this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        final AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);
    }

    @Override
    public void onDestroy() { //Destruye la conexión principal
        super.onDestroy();
        Log.d("TEST", "On DEstroy services");

        LogUtils.d(TAG,"ConexionService: onDestroy");
        DataServer.getInstance().limpiarAlarma();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(ConnectionManager.NOTIFICATION_ID);
        }
        try {
            unregisterReceiver(firebaseReceiver);
        } catch (Exception ex) {
            LogUtils.e(TAG,"ConexionService: onDestroy" + ex.getMessage());
        }

        if (this.conexionBluetooth != null) { //si la conexión de bluetooth es diferente a nulo
            this.conexionBluetooth.cerrar(); //Se cierra la conexión
            this.conexionBluetooth = null; //el valor vuelve a nulo
        }
    }

    private void initConfigBluetooth() {
        LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  131");
        sendBroadcast(new Intent(ProgressManager.INIT));

        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter(); // Representa el adaptador Bluetooth del dispositivo local, permite realizar tareas fundamentales de Bluetooth, como iniciar el descubrimiento de dispositivos.

        if (adapter == null) {
            LogUtils.d(TAG,"Bluetooth no disponible");

            final Intent intent = new Intent(ProgressManager.CONEXION_OBD);
            intent.putExtra("STATE", "NO_DISPONIBLE");
            LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  138");
            sendBroadcast(intent); // Entonces se manda al dispositivo el estado de que no está disponible.
        } else if (!adapter.isEnabled()) { // Si el adaptador no está "encendido"...
            LogUtils.d("BT", "No habilitado");

            final Intent intent = new Intent(ProgressManager.CONEXION_OBD);
            intent.putExtra("STATE", "NO_HABILITADO"); //entonces se manda al dispositivo el estado de que no está habilitado
            LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  148");
            sendBroadcast(intent);
        } else {
            if (Build.VERSION.SDK_INT >= 31) { //Se revisan los permisos
                if (ActivityCompat.checkSelfPermission(this, Constants.BLUETOOTH_SCAN_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            final Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
            //Set<BluetoothDevice> "pairedDevice" .getBondeDevices Devuelve el conjunto de BluetoothDeviceobjetos que están vinculados (emparejados) al adaptador local, en este caso a la variable mBadap.
            if (pairedDevices.size() == 0) { //Valida si hay dispositivos vinculdos
                LogUtils.d("BT", "No Encontrado");

                final Intent intent = new Intent(ProgressManager.CONEXION_OBD);
                intent.putExtra("STATE", "NO_ENCONTRADO"); //entonces se manda al dispositivo el estado de que no está encontrado
                LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  164");
                sendBroadcast(intent);
                return;
            }

            final Intent intent = new Intent(ProgressManager.CONEXION_OBD);
            intent.putExtra("STATE", "CONECTANDO");
            LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  171");
            sendBroadcast(intent); // Entonces se manda al dispositivo el estado de que está conectado

            // It es un objeto que nos permite recorrer una lista y presentar por pantalla todos sus elementos, en este caso los elementos de la variable "pairedDevice"
            // El método it.next guardará el valor de la variable, calcula el siguiente actualizando la misma, y devuelve el valor antiguo.
            // El método .hasNext simplemente comprueba si el valor de la variable está dentro del rango.
            for (BluetoothDevice bluetoothDevice : pairedDevices) {
                // Mientras it y sw esten dentro de la conección o rango...
                // Se crea una variable llamada "primerDevice" para permitir crear una conexión con el dispositivo respectivo o consultar información sobre él, como el nombre.
                if (bluetoothDevice.getName().equals(Constants.DEVICE_NAME)) {
                    // Se irá al método de la clase "ConexionBluetooth" mandando como parámetros 3variables.
                    this.conexionBluetooth = new ConexionBluetooth(bluetoothDevice, this, new CallbackService() {
                        @Override
                        public void callback() {
                            ConnectionManager.getInstance().actualizarEstado(ConexionService.this);
                        }
                    });

                    this.conexionBluetooth.start(); // Comienza la conexión.
                    break;
                }
            }
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private void startForegroundService() {
        final String CHANNEL_ID = NotificationsUtils.createNotificationChannel();
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setOngoing(true)
                .setContentTitle("OBD segundo plano")
                .setPriority(Notification.PRIORITY_HIGH); // for under android 26 compatibility

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_MIN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setCategory(Notification.CATEGORY_SERVICE);
        }

        final Intent intent = new Intent(this, MainActivity2.class);
        final PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        }

        notificationBuilder.setContentIntent(pendingIntent);

        startForeground(ConnectionManager.NOTIFICATION_ID, notificationBuilder.build());
    }
}