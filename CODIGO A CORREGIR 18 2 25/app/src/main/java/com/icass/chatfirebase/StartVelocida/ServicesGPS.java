package com.icass.chatfirebase.StartVelocida;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.icass.chatfirebase.DataServer;
import com.icass.chatfirebase.managers.ResourceManager;
import com.icass.chatfirebase.services.ServicesPrincipal;
import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.LogUtils;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ServicesGPS extends Service {

    private static final String TAG = ServicesGPS.class.getSimpleName();

    private static final int PERIOD_MS = 5000;
    private static boolean banderaActivity = false;
    private static String command = "1";

    @NonNull
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) { // metodo que esta para la escucha de mensajes del servidor y si no se encuentra activado el BT
            LogUtils.d(TAG, "Se llama el receiver en ServicesGPS");
            //Push notification Firebase
            command = intent.getStringExtra("command");

            final SharedPreferences sharedPreferences = getSharedPreferences(Constants.TAG_TYPE_USER, Context.MODE_PRIVATE);
            final SharedPreferences.Editor editor = sharedPreferences.edit();  //Constante de editor
            LogUtils.d("COMANDGPS", command);

            if(command.length() > 1 && command.length() < 6) {
                // Guardaremos el comando para utilizarlo mas adelante
                editor.putString("Comando", command); // se guarda admin en user
                editor.apply();

                banderaActivity = true;

                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    LogUtils.d("scehdulJob","menorLolipop");

                    Intent newIntent = new Intent(context, ServicesPrincipal.class);
                    PendingIntent pendingIntent = PendingIntent.getService(context, 1, newIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), PERIOD_MS, pendingIntent);
                } else {
                    scheduleJob();
                }
           }
        }
    };

    public ServicesGPS() {

    }

    private void iniciarServicioPrincipal() {
        LogUtils.d(TAG,"iniciarServicioPrincipal");
        LogUtils.d(TAG,"COMANDOif: " + command);
        LogUtils.d(TAG,"ServicioGPSKm: " + banderaActivity);

        if(!banderaActivity) {
            banderaActivity = true;

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                LogUtils.d("scehdulJob","menorLolipop");

                final Intent newIntent = new Intent(this, ServicesPrincipal.class);
                PendingIntent pendingIntent = PendingIntent.getService(this, 1, newIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager manager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
                manager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), PERIOD_MS, pendingIntent);
            } else {
                scheduleJob();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void scheduleJob() {
        final Context context = ResourceManager.getInstance().getContext();
        LogUtils.d("scehdulJob","MyorLolipop");
        ComponentName serviceComponent = new ComponentName(context, ServicesPrincipal.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        //builder.setPeriodic(PERIOD_MS);

        builder.setMinimumLatency(PERIOD_MS);
        builder.setOverrideDeadline(PERIOD_MS);
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }

    @Override
    public void onCreate() {
        LogUtils.d(TAG, "Se regsitra el receiver en " + TAG);

        registerReceiver(broadcastReceiver, new IntentFilter("FirebaseCommandReceived"));

        banderaActivity = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d("JobServicio","onStart");

        final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.UNLOCK_TAG);
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
        wakeLock.release();

        iniciarServicioPrincipal();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        LogUtils.d("ServicioGPS", String.valueOf(banderaActivity));

        unregisterReceiver(broadcastReceiver); //se "borra" el registro de la variable de tipo BroadcastReceiver
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}