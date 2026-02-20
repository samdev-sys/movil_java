package com.icass.chatfirebase.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.icass.chatfirebase.StartVelocida.ServicesGPS;
import com.icass.chatfirebase.StartVelocida.StartVel;
import com.icass.chatfirebase.activity.PrincipalActivity;
import com.icass.chatfirebase.data.LocalData;
import com.icass.chatfirebase.managers.ConnectionManager;
import com.icass.chatfirebase.managers.ConnectionManager.EstadoConexionDef;
import com.icass.chatfirebase.managers.ProgressManager;
import com.icass.chatfirebase.managers.ProgressManager.ProgresoDef;
import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.Utils;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ServicesPrincipal extends JobService {
    private static final String TAG = ServicesPrincipal.class.getSimpleName();

    private static final int PERIOD_MS = 5000;

    boolean banCancelarReconectar = true;
    public static final String Apagado = "UNABLE";

    public String concatIdVehiculo = "";
    public int contador = 0;
    public Utils.StateBluetooth localsttate;

    @NonNull
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            LogUtils.d(TAG, "Entro en el metodo OnReceive de " + TAG);
            final Bundle bundle = intent.getExtras();
            final String action = intent.getAction();

            //LogUtils.print("Action: " + action);

            switch (action) {
                case ProgressManager.INIT: {
                    ServicesPrincipal.this.banCancelarReconectar = true;
                    break;
                }

                case ProgressManager.CONEXION_OBD: {
                    final String state = bundle.getString("STATE");

                    switch (state) {
                        case "NO_DISPONIBLE": {
                            ServicesPrincipal.this.localsttate = Utils.StateBluetooth.NO_DISPONIBLE;
                            // TODO Remove Toast Toast.makeText(ServicesPrincipal.this, "No disponible", Toast.LENGTH_LONG).show();
                            break;
                        }

                        case "NO_HABILITADO": {
                            ServicesPrincipal.this.localsttate = Utils.StateBluetooth.NO_HABILITADO;
                            // TODO Remove Toast Toast.makeText(ServicesPrincipal.this, "No habilitado", Toast.LENGTH_LONG).show();
                            break;
                        }

                        case "NO_ENCONTRADO": {
                            ServicesPrincipal.this.localsttate = Utils.StateBluetooth.NO_ENCONTRADO;
                            // TODO Remove Toast Toast.makeText(ServicesPrincipal.this, "No encontrado", Toast.LENGTH_LONG).show();
                            break;
                        }

                        case "CONECTADO_OK": {
                            final String nameDevice = bundle.getString("NAME_DEVICE");
                            ServicesPrincipal.this.localsttate = Utils.StateBluetooth.CONECTADO;

                            final StringBuilder sb = new StringBuilder();
                            sb.append("Conectado a:\n");
                            sb.append(nameDevice); // Muestra el nombre del dispositivo conectado
                            // TODO Remove Toast Toast.makeText(ServicesPrincipal.this, "Conectado", Toast.LENGTH_LONG).show();
                            break;
                        }

                        case "RECONECTAR": {
                            final int contador = bundle.getInt("CONTADOR");

                            if(ServicesPrincipal.this.banCancelarReconectar) {
                                ServicesPrincipal.this.localsttate = Utils.StateBluetooth.RECONECTANDO;
                                StringBuilder sb2 = new StringBuilder();
                                sb2.append("Reconectando... (");
                                sb2.append(contador);
                                sb2.append(")");
                            }

                            break;
                        }

                        default: {
                            LogUtils.d(TAG,"Otro state: " + state);
                            break;
                        }
                    }

                    break;
                }

                case ProgressManager.WRITE_LOG: {
                    final String str = bundle.getString("DATA");
                    LogUtils.d(TAG,"DATA: " + str);

                    final StringBuilder sb3 = new StringBuilder();
                    sb3.append("\n");
                    sb3.append(str);

                    break;
                }

                case ProgressManager.MSG_OBD: {
                    final boolean ban = bundle.getBoolean("BAN");
                    final String data = bundle.getString("DATA");

                    final boolean result = data.contains(Apagado);

                    /*
                    if(data.length() >= 200) {
                        if(!comrpobarSinDoble.equals(data)) {
                            comrpobarSinDoble = data;
                            LogUtils.d("ServPrincipal", "String Largo");
                            LogUtils.d("ServPrincipal", data);
                            if(isOnline(context) == true) {
                                LogUtils.d("ServPrincipal", "Si hay Internet");
                                if(MensajeSinInternt.size() != 0) {
                                    LogUtils.d("MensajeInternet", String.valueOf(MensajeSinInternt.size()));
                                    for(int i = 0; i < MensajeSinInternt.size();i++) {
                                        String error_actual = MensajeSinInternt.get(i);
                                        if(error_actual!=null) {
                                            LogUtils.d("Mensaje Enviado", "String");
                                           // ConexionBluetooth.dataServer.enviarMsgAlServer(error_actual);
                                        } else {
                                            LogUtils.d("MensajeInternet","Ya np hay nada en la cadena");
                                        }

                                        if(i == MensajeSinInternt.size()) {
                                            LogUtils.d("MensajeInternet", "CadenaBorrada");
                                            MensajeSinInternt.clear();
                                        }
                                    }
                                   // MensajeSinInternt.clear();
                                } else { LogUtils.d("ServPrincipal","Sin cadena");}
                            }
                            else {
                                LogUtils.d("SrrvPrincipal","No hay internet");
                                String Guardar = data + "SI";
                                LogUtils.d("ServPrimcipal",Guardar);
                                MensajeSinInternt.add(Guardar);
                            }
                        } else {
                            LogUtils.d("ServPrincipal", "Las cadenas son iguales");
                        }
                    }

                     */

                    if(data.equals("APAGAR")) {
                        // TODO Remove Toast Toast.makeText(ServicesPrincipal.this, "Apagado", Toast.LENGTH_LONG).show();

                        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            Intent newIntent = new Intent(context, StartVel.class);
                            PendingIntent pendingIntent = PendingIntent.getService(context, 1, newIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), PERIOD_MS, pendingIntent);
                        } else {
                            scheduleJob(context);
                        }
                    }

                    if(result) {
                        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                            Intent newIntent = new Intent(context, StartVel.class);
                            PendingIntent pendingIntent = PendingIntent.getService(context, 1, newIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), PERIOD_MS, pendingIntent);
                        } else {
                            scheduleJob(context);
                        }

                        stopSelf();
                        LogUtils.d(TAG,"JobBluetooth Destruida finish" );
                    }

                    try {
                        final LocalData localData = new LocalData();

                        if(PrincipalActivity.isEnviar && ban) {
                            concatIdVehiculo = Utils.strConcat + Constants.ID_VEHICULO;
                        }

                        if(!ban) {
                            StringBuilder sb5 = new StringBuilder();
                            sb5.append("\n");
                            sb5.append(data);
                            sb5.append(concatIdVehiculo);
                            concatIdVehiculo = "";
                            return;
                        }

                        return;
                    } catch (Exception ex) {
                        LogUtils.e(TAG,"broadcastReceiver "+ ex);
                    }

                    break;
                }

                case ProgressManager.SUM_CONTADOR: {
                    ServicesPrincipal.this.contador = ServicesPrincipal.this.contador + 1;
                    break;
                }

                case ProgressManager.REINICIAR: {
                    /*
                    LogUtils.print("REINICIANDO!!");

                    ConnectionManager.getInstance().setEstadoActual(EstadoConexionDef.ESTADO_DESCONECTADO);
                    ProgressManager.getInstance().setProgreso(ProgresoDef.ESTADO_DESCONECTADO);
                    stopService(new Intent(getApplicationContext(), ConexionService.class));

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            LogUtils.print("Reiniciando servicio");
                            startService(new Intent(getApplicationContext() , ConexionService.class));
                        }
                    }, 3000);

                    */
                    break;
                }

                default: {
                    LogUtils.d(TAG,"Otro caso: " + action);
                }
            }
//          TODO -------Revisar este parte
            if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                ProgressManager.getInstance().setProgreso(ProgresoDef.ESTADO_CONECTADO);
            } else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                ConnectionManager.getInstance().setEstadoActual(EstadoConexionDef.ESTADO_DESCONECTADO);
                ProgressManager.getInstance().setProgreso(ProgresoDef.ESTADO_DESCONECTADO);
                stopService(new Intent(getApplicationContext(), ConexionService.class));

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        LogUtils.d(TAG, "Reiniciando servicio");
                        startService(new Intent(getApplicationContext() , ConexionService.class));
                    }
                }, 3600000);
            }
//          TODO -------Revisar este parte
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        LogUtils.d(TAG,"ServicesPrincipal: onCreate");
        this.contador = 0;

        LogUtils.d(TAG, "stopService: 7");
        final Intent intentService1 = new Intent(ServicesPrincipal.this, StartVel.class);
        stopService(intentService1);

        LogUtils.d(TAG, "stopService: 8");
        final Intent intentService2 = new Intent(ServicesPrincipal.this, ServicesGPS.class);
        stopService(intentService2);

        final IntentFilter filter = new IntentFilter();
        for(String item: ProgressManager.getInstance().getFilters()) {
            filter.addAction(item);
        }

        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        try {
            LogUtils.d(TAG, "Se regsitra el receiver en " + TAG);

            registerReceiver(broadcastReceiver, filter);
        } catch (Exception ex) {
            LogUtils.e(TAG, ex.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d(TAG,"ServicesPrincipal: onDestroy");

        try {
            getApplicationContext().unregisterReceiver(broadcastReceiver);
        } catch (Exception ex) {
            LogUtils.e(TAG, ex.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void scheduleJob(Context context) {
        final ComponentName serviceComponent = new ComponentName(context, StartVel.class);
        final JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        //builder.setPeriodic(PERIOD_MS);

        builder.setMinimumLatency(PERIOD_MS);
        builder.setOverrideDeadline(PERIOD_MS);
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        startService(new Intent(getApplicationContext() , ConexionService.class));
        // mJobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE );
        //JobInfo.Builder job = new JobInfo.Builder(111, new ComponentName(mcontext, ServicesPrincipal.class)); job.setPeriodic(60000);
        //manda llamar la clase "ConexionService.java"
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}