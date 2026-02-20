package com.icass.chatfirebase.activity;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.icass.chatfirebase.R;
import com.icass.chatfirebase.StartVelocida.ServicesGPS;
import com.icass.chatfirebase.StartVelocida.StartVel;
import com.icass.chatfirebase.data.LocalData;
import com.icass.chatfirebase.managers.ProgressManager;
import com.icass.chatfirebase.services.ConexionService;
import com.icass.chatfirebase.services.FirebaseMessagingServiceImpl;
import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.Utils;
import com.icass.chatfirebase.utils.Utils.StateBluetooth;

import java.util.Timer;
import java.util.TimerTask;

public class PrincipalActivity extends AppCompatActivity {
    private static final String TAG = PrincipalActivity.class.getSimpleName();

    public static boolean banSend = false;
    public static boolean isConcatenar = false;
    public static boolean isEnviar = false;
    boolean banCancelarReconectar = true;
    public static final String Apagado = "UNABLE";
    public static final String Apagado2 = "Apagado";

    public String concatIdVehiculo = "";
    public int contador = 0;
    public ImageView iStatus;
    public TextView lblContador;
    public TextView lblInfoConexion;
    public StateBluetooth localsttate;
    public ScrollView scView;
    public TextView tvLog;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            LogUtils.d(TAG, "Se llama el receiver en PrincipalActivity");

            char c;
            char c2 = 3;

            final Bundle ex = intent.getExtras();
            final String action = intent.getAction();
            LogUtils.d(TAG, "Action: " + action + " with hash: " + action.hashCode());

            switch (action.hashCode()) {
                case -1321695035:
                    if (action.equals(ProgressManager.CONEXION_OBD)) {
                        c = 1;
                        break;
                    }
                case 2252048: {
                    if (action.equals(ProgressManager.INIT)) {
                        c = 0;
                        break;
                    }
                }

                case 1381547684: {
                    if (action.equals(ProgressManager.WRITE_LOG)) {
                        c = 2;
                        break;
                    }
                }

                case 2060944140:
                    if (action.equals(ProgressManager.SUM_CONTADOR)) {
                        c = 4;
                        break;
                    }
                case 2063004403:
                    if (action.equals(ProgressManager.MSG_OBD)) {
                        c = 3;
                        break;
                    }
                default:
                    c = 65535;
                    break;
            }
            switch (c) { //Estados de la conexión de bluetooth de los dispositivos
                case 0:
                    LogUtils.d(TAG, "banCancelarReconectar");
                    PrincipalActivity.this.banCancelarReconectar = true;
                    return;
                case 1:
                    String state = ex.getString("STATE");
                    switch (state.hashCode()) {
                        case 171245299:
                            if (state.equals("NO_HABILITADO")) {
                                c2 = 1;
                                break;
                            }
                        case 269722188:
                            break;
                        case 953307153:
                            if (state.equals("NO_DISPONIBLE")) {
                                c2 = 0;
                                break;
                            }
                        case 1106853176:
                            if (state.equals("RECONECTAR")) {
                                c2 = 5;
                                break;
                            }
                        case 1496095299:
                            if (state.equals("CONECTADO_OK")) {
                                c2 = 4;
                                break;
                            }
                        case 1578936851:
                            if (state.equals("NO_ENCONTRADO")) {
                                c2 = 2;
                                break;
                            }
                        default:
                            c2 = 65535;
                            break;
                    }
                    switch (c2) { //Acciones para el estado enviado anteriormente de la conexión vía bluetooth de los dispositivos
                        case 0:
                            PrincipalActivity.this.localsttate = StateBluetooth.NO_DISPONIBLE;
                            PrincipalActivity.this.lblInfoConexion.setText("Bluetooth no disponible");
                            PrincipalActivity.this.iStatus.setImageDrawable(ContextCompat.getDrawable(PrincipalActivity.this, R.drawable.cross_circle));
                            return;
                        case 1:
                            PrincipalActivity.this.localsttate = StateBluetooth.NO_HABILITADO;
                            PrincipalActivity.this.lblInfoConexion.setText("El bluetooth no está encendido");
                            PrincipalActivity.this.iStatus.setImageDrawable(ContextCompat.getDrawable(PrincipalActivity.this, R.drawable.cross_circle));
                            return;
                        case 2:
                            PrincipalActivity.this.localsttate = StateBluetooth.NO_ENCONTRADO;
                            PrincipalActivity.this.lblInfoConexion.setText("No hay un dispositivo emparejado");
                            PrincipalActivity.this.iStatus.setImageDrawable(ContextCompat.getDrawable(PrincipalActivity.this, R.drawable.cross_circle));
                            return;
                        case 3:
                            PrincipalActivity.this.lblInfoConexion.setText("Conectando...");
                            PrincipalActivity.this.iStatus.setImageDrawable(ContextCompat.getDrawable(PrincipalActivity.this, R.drawable.cross_circle));
                            return;
                        case 4:
                            String nameDevice = ex.getString("NAME_DEVICE");
                            PrincipalActivity.this.localsttate = StateBluetooth.CONECTADO;
                            TextView access$000 = PrincipalActivity.this.lblInfoConexion;  //access$000 es el estado de la conexión y se muestra en la etiqueta "lblInfoConexion"
                            StringBuilder sb = new StringBuilder();
                            sb.append("Conectado a:\n");
                            sb.append(nameDevice); //muestra el nombre del dispositivo conectado
                            access$000.setText(sb.toString()); //anexa el estado de la conexión con el nombre del dispositivo
                            PrincipalActivity.this.iStatus.setImageDrawable(ContextCompat.getDrawable(PrincipalActivity.this, R.drawable.check_circle)); //el ícono cambia de rojo a verde
                            return;
                        case 5:
                            int contador = ex.getInt("CONTADOR");
                            if (PrincipalActivity.this.banCancelarReconectar) {
                                PrincipalActivity.this.localsttate = StateBluetooth.RECONECTANDO;
                                TextView access$0002 = PrincipalActivity.this.lblInfoConexion;
                                StringBuilder sb2 = new StringBuilder();
                                sb2.append("Reconectando... (");
                                sb2.append(String.valueOf(contador));
                                sb2.append(")");
                                access$0002.setText(sb2.toString());
                                PrincipalActivity.this.iStatus.setImageDrawable(ContextCompat.getDrawable(PrincipalActivity.this, R.drawable.cross_circle));
                                if (contador > 5) {
                                    finish();
                                    Intent incontador = new Intent(PrincipalActivity.this, MainActivity2.class);
                                    startActivity(incontador);

                                    LogUtils.d(TAG, "stopService: 6");
                                    stopService(new Intent(getApplicationContext(), ConexionService.class));//finliza el servicio y manda llamar a la clase "ConexionService.java"
                                }

                                return;
                            }
                            return;
                        default:
                            return;
                    }
                case 2:
                    String str = ex.getString("DATA");


                    TextView access$200 = PrincipalActivity.this.tvLog;
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("\n");
                    sb3.append(str);
                    access$200.append(sb3.toString());

                    if (PrincipalActivity.this.scView != null) {
                        PrincipalActivity.this.scView.post(new Runnable() {
                            public void run() {
                                try {
                                    PrincipalActivity.this.scView.fullScroll(130);
                                } catch (Exception ex) {
                                    LogUtils.d(TAG, "fullScroll 1" + ex.getMessage());
                                }
                            }
                        });

                        return;
                    }
                    return;
                case 3:
                    boolean ban = ex.getBoolean("BAN");
                    String data = ex.getString("DATA");

                    boolean result = data.contains(Apagado);
                    boolean apagado2 = data.contains(Apagado2);

                    if (data.equals("APAGAR")) {
                        finishAffinity();
                        killBackgroundProcesses("com.icass.chatfirebase");
                        Intent in = new Intent(PrincipalActivity.this, MainActivity2.class);
                        startActivity(in);
                    }

                    if (result) {
                        finishAffinity();
                        Intent in = new Intent(PrincipalActivity.this, MainActivity2.class);
                        startActivity(in);

                        LogUtils.d(TAG,"Actividad Destruida finish");
                    }

                    try {
                        LocalData localData = new LocalData();
                        if (PrincipalActivity.isEnviar && ban) {

                            PrincipalActivity principalActivity = PrincipalActivity.this;
                            StringBuilder sb4 = new StringBuilder();
                            sb4.append(Utils.strConcat);
                            sb4.append(Constants.ID_VEHICULO);
                            principalActivity.concatIdVehiculo = sb4.toString();
                        }
                        if (!ban) {
                            TextView access$2002 = PrincipalActivity.this.tvLog;
                            StringBuilder sb5 = new StringBuilder();
                            sb5.append("\n");
                            sb5.append(data);
                            sb5.append(PrincipalActivity.this.concatIdVehiculo);
                            access$2002.append(sb5.toString());
                            if (PrincipalActivity.this.scView != null) {
                                PrincipalActivity.this.scView.post(new Runnable() {
                                    public void run() {
                                        try {
                                            PrincipalActivity.this.scView.fullScroll(130);
                                        } catch (Exception ex) {
                                            LogUtils.d(TAG, "fullScroll 2" + ex.getMessage());
                                        }
                                    }
                                });
                            }
                            PrincipalActivity.this.concatIdVehiculo = "";
                            return;
                        }
                        return;
                    } catch (Exception e) {
                        return;
                    }
                case 4: {
                    contador = PrincipalActivity.this.contador + 1;
                    lblContador.setText(String.valueOf(PrincipalActivity.this.contador));
                    break;
                }
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal2);
        this.contador = 0;
        initComponent();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            LogUtils.d(TAG, "stopService: 9");
            final Intent service1 = new Intent(PrincipalActivity.this, StartVel.class);
            stopService(service1);

            LogUtils.d(TAG,"stopService: 10");
            final Intent service2 = new Intent(PrincipalActivity.this, ServicesGPS.class);
            stopService(service2);
        }

        final IntentFilter filter = new IntentFilter();
        for (String item : ProgressManager.getInstance().getFilters()) {
            filter.addAction(item);
        }
        LogUtils.d(TAG, "Se regsitra el receiver en " + TAG);

        registerReceiver(broadcastReceiver, filter);

        startService(new Intent(this, ConexionService.class));//manda llamar la clase "ConexionService.java"

        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                boolean sentAppToBackground = moveTaskToBack(true);

                if (!sentAppToBackground) {
                    final Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    startActivity(intent);
                }
            }
        };

        final Timer timer = new Timer();
        timer.schedule(task, 3000);
    }

    public void onDestroy() {
        super.onDestroy();
        LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  326");
        sendBroadcast(new Intent("stopSelf"));

        // amKillProcess("com.icass.chatfirebase");
        killBackgroundProcesses("com.icass.chatfirebase");

        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
        LogUtils.d(TAG,"DESTROY ACTIVITY");

        try {
            LogUtils.d(TAG,"stopService: 11");
            stopService(new Intent(this, ConexionService.class));//para el servicio y manda llamar a la clase "ConexionService.java"

            LogUtils.d(TAG, "stopService: 12");
            stopService(new Intent(this, FirebaseMessagingServiceImpl.class));
        } catch (Exception ex) {
            LogUtils.e(TAG, ex.getMessage());
        }
    }

    public void killBackgroundProcesses(String packageName) {
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        am.killBackgroundProcesses(packageName);
        LogUtils.d("Muerto", "Background");
    }

    private void initComponent() { //pone los id de cada componente en el que se hizo una instancia.
        this.iStatus = findViewById(R.id.iStatus);
        this.tvLog = findViewById(R.id.tvLog);
        this.lblContador = findViewById(R.id.lblContador);
        this.scView = findViewById(R.id.scView);
        this.lblInfoConexion = findViewById(R.id.lblInfoConexion);
    }
}