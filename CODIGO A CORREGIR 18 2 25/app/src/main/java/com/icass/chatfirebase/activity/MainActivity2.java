package com.icass.chatfirebase.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Process;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.icass.chatfirebase.R;
import com.icass.chatfirebase.data.LocalData;
import com.icass.chatfirebase.managers.ConnectionManager;
import com.icass.chatfirebase.managers.LockManager;
import com.icass.chatfirebase.managers.NetworkManager;
import com.icass.chatfirebase.managers.ResourceManager;
import com.icass.chatfirebase.notifications.StateNotification;
import com.icass.chatfirebase.services.ConexionService;
import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import xyz.kumaraswamy.autostart.Autostart;


public class MainActivity2 extends AppCompatActivity {
    private final String TAG = MainActivity2.class.getSimpleName();
    private static final int PERIOD_MS = 8000;
    private static final int REQUEST_LOCATION_PERMISSION = 9001;
    private static final int REQUEST_BLUETOOTH_PERMISSION_SCAN = 9002;
    private static final int REQUEST_BLUETOOTH_PERMISSION_CONNECT = 9003;
    private static final int REQUEST_BLUETOOTH_ON = 9004;
    private static final int REQUEST_PERMISSION_BATTERY = 9005;
    @Nullable
    private final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

    private AlertDialog alertDialog;
    private TextView tvOBDStatus;

    public boolean banderaYaHayOBD = false;
    public int contador = 0;
    private boolean isVisible = false;
    private boolean isComplete = false;
    private boolean iniciarServicioBandera = false;
    private boolean permisosOk = false;
    private boolean permisosSobreAPP = false;
    private boolean encimaApp = false;
    private boolean dialogOverlays = false;
    private String version = "1.2.5";

    @NonNull
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            LogUtils.d(TAG, "Se llama el receiver en Main Activity 2");
            if(Build.VERSION.SDK_INT >= 31) {
                if(ActivityCompat.checkSelfPermission(context, Constants.BLUETOOTH_SCAN_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                    requestBluetoothPermission();
                    return;
                }
            }

            final String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                LogUtils.d(TAG,"Dispositivo encontrado dentro de receiver");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device == null) {
                    LogUtils.d(TAG,"Dispositivo nulo dentro de receiver");
                    return;
                } else {
                    LogUtils.d(TAG,"Dispositivo válido dentro de receiver");
                }

                final String deviceName = device.getName();

                if(deviceName == null) {
                    LogUtils.d(TAG,"Nombre no válido dentro de receiver");
                    return;
                }

                final boolean deviceFound = deviceName.contains(Constants.DEVICE_NAME);
                LogUtils.d(TAG,"Dispositivo (1): " + deviceName + "dentro de receiver");

                if(deviceFound) {
                    if(device.createBond()) {
                        tvOBDStatus.setText("Emparejando...");

                        Utils.sleep("3", 10000);

                        dispositivoEmparejado();

                        Utils.sleep("5", 2000);

                        if(contador != 100) {
                            LogUtils.d(TAG,"Buscar de nuevo");
                            tvOBDStatus.setText("Error, Buscando...");
                            startDiscovery();
                        }
                    } else {
                        LogUtils.e(TAG,"Algo salio mal dentro de receiver");
                    }
                } else {
                    LogUtils.d(TAG,"Device encontrado: " + deviceName);
                }
            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                LogUtils.d(TAG, "ACTION_DISCOVERY_FINISHED");
                startDiscovery();
                contador = contador + 1;
                if(contador >= 4) {
                    finishAffinity();
                }
            } else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                LogUtils.d(TAG, "ACTION_DISCOVERY_STARTED");

                if(contador >= 3) {
                    alertDialog.show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new LocalData().setCodigo( "forceClose", "false");
        setContentView(R.layout.activity_main2);
        tvOBDStatus = findViewById(R.id.tvOBDStatus);
        LogUtils.d("GABO", "Version de la aplicación " + version);
        LogUtils.d(TAG, "OnCreate" + TAG);
        isVisible = true;


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            unlockDevice();
        }

        final String command = new LocalData().getCommand();
        manageCommand(command);

        alertDialog = new AlertDialog.Builder(this).setMessage("OBD no encontrado").create(); //Alert Dialog Personalizado

        tvOBDStatus = findViewById(R.id.tvOBDStatus);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        LogUtils.d(TAG, "Se regsitra el receiver en " + TAG);
        registerReceiver(receiver, filter);
        cancelDiscovery();

        initFirebaseChannel();

        final String codigo = Constants.ID_VEHICULO;
        final SharedPreferences sharedPreferences = getSharedPreferences(Constants.TAG_TYPE_USER, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.TAG_CODIGO, codigo); // Se guarda admin en user
        editor.apply();

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if(task.isSuccessful()) {
                    // Get new FCM registration token
                    String token = task.getResult();
                    LogUtils.d(TAG,"FirebaseToken: " + token);
                    ingresarApp(codigo);
                } else {
                    LogUtils.d(TAG,"Fetching FCM registration token failed: " + task.getException());
                }
            }
        });

        requestBluetoothPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtils.d("TEST", "---------->OnResume de " + TAG);
        String parametro = new LocalData().getCodigo( "forceClose");
        Log.d("TEST", "Valor de parametros  " + parametro);
        if (parametro.equalsIgnoreCase("true")){
            stopService(new Intent(getApplicationContext() , ConexionService.class));
            Intent launchIntent = new Intent(this, closeActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launchIntent);
            finish();
        } else {
            if (validatePermissions()){
                permisoEncimaApp();
                isVisible = true;
            }
//            LogUtils.d("TEST", "Valor isComplete: " + isComplete);
//            LogUtils.d("TEST", "Valor iniciar Servbicio Bandera: " + iniciarServicioBandera);
//            LogUtils.d("TEST", "Valor encimaAPP: " + encimaApp);


            if (isComplete && encimaApp && iniciarServicioBandera){
//                LogUtils.d("TEST", "Segundo if _On Resume");
                finish();
            } else if(!isComplete && !iniciarServicioBandera && encimaApp) { //Se agrega validacion para que solo se inicie una unica ocasion el servicio
//                LogUtils.d("TEST", "Primer if _On Resume");
                LogUtils.d(TAG, "Se inicia Servicio en OnResume de si la bandera coincide"+ TAG);
                iniciarServicioBandera  = true;
                iniciarServicio();
            } else {
                LogUtils.d("TEST", "Else on REsume");
                LogUtils.d(TAG,"Servicio no completo");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("TEST", "onDestroy");
        alertDialog.dismiss();

        cancelDiscovery();

        unregisterReceiver();
        //android.os.Process.killProcess(android.os.Process.myPid());

    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("TEST", "OnNewIntent");
        final String command = new LocalData().getCommand();
        manageCommand(command);
    }

    private void manageCommand(@NonNull String command) {
        if(command.isEmpty()) {
            LogUtils.d(TAG,"Command is empty");
          //  new LocalData().setCommand("ejemplo_de_comando");
           // startActivity(new Intent(this, AlertaActivity.class));
        } /*else if (command.contains("ubica")){
            LogUtils.d(TAG,"Command: " + command);
            startActivity(new Intent(this, helpGPS.class));
        }*/ else {
            LogUtils.d(TAG,"Command: " + command);
            startActivity(new Intent(this, AlertaActivity.class));
        }
    }

    private void requestEnableBluetooth() {
        if(Build.VERSION.SDK_INT >= 31) {
            if(ActivityCompat.checkSelfPermission(this, Constants.BLUETOOTH_SCAN_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermission();
                return;
            }
        }

        // Ask for Bluetooth Permission
        if(adapter != null) {
            if(adapter.isEnabled()) {
                LogUtils.d(TAG,"Bluetooth ON");
                batteryPermission();
            } else {
                LogUtils.d(TAG,"Bluetooth OFF");
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_BLUETOOTH_ON);
            }
        } else {
            batteryPermission();
        }
    }

    @SuppressLint("BatteryLife")
    private void batteryPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final String packageName = getPackageName();
            final PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE); //le da el control del estado de energía del dispositivo.

            if(powerManager.isIgnoringBatteryOptimizations(packageName)) {
                autorunPermission();
            } else {
                LogUtils.d(TAG,"Request Battery Permission");
                final Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivityForResult(intent, REQUEST_PERMISSION_BATTERY);
            }
        } else {
            autorunPermission();
        }
    }

    private void autorunPermission() {
        final String manufacturer = android.os.Build.MANUFACTURER;

        if(manufacturer.equalsIgnoreCase("xiaomi")) {
            LogUtils.d(TAG,"Es Xiaomi");

            try {
                // make sure device is MIUI device, else an
                // exception will be thrown at initialization
                final Autostart autostart = new Autostart(this);
                final Autostart.State state = autostart.getAutoStartState();

                if(state == Autostart.State.ENABLED) {
                    // now we are also sure that autostart is enabled
                    LogUtils.d(TAG,"AutoRun Enabled");
                    dispositivoEmparejado();
                    searchBtDevices();
                } else if(state == Autostart.State.DISABLED) {
                    // now we are sure that autostart is disabled
                    // ask user to enable it manually in the settings app
                    LogUtils.d(TAG,"AutoRun Disabled");

                    final Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));

                    final List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                    if(list.size() > 0) {
                        startActivity(intent);
                    }
                } else {
                    LogUtils.d(TAG,"Otro State: " + state); // NO_INFO
                    dispositivoEmparejado();
                    searchBtDevices();
                }
            } catch (Exception ex) {
                LogUtils.e(TAG, ex.getMessage());
            }
        } else {
            LogUtils.d(TAG, "No es Xiaomi");

            try {
                final Intent intent = new Intent();
                if(manufacturer.equalsIgnoreCase("xiaomi")) {
                    intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                } else if(manufacturer.equalsIgnoreCase("oppo")) {
                    intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
                } else if(manufacturer.equalsIgnoreCase("vivo")) {
                    intent.setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
                } else if(manufacturer.equalsIgnoreCase("Letv")) {
                    intent.setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"));
                } else if(manufacturer.equalsIgnoreCase("Honor")) {
                    intent.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"));
                }

                final List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

                if(list.size() > 0) {
                    startActivity(intent);
                } else {
                    dispositivoEmparejado();
                    searchBtDevices();
                }
            } catch (Exception ex) {
                LogUtils.e(TAG, ex.getMessage());
            }
        }
    }

    private void searchBtDevices() {
        if(Build.VERSION.SDK_INT >= 31) {
            if(ActivityCompat.checkSelfPermission(this, Constants.BLUETOOTH_SCAN_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermission();
                return;
            }
        }

        if(adapter == null) {
            return;
        }

        if(adapter.isDiscovering()) {
            LogUtils.d(TAG,"Ya existe una búsqueda activa");
        } else {
            startDiscovery();
            LogUtils.d(TAG,"Nueva Busqueda iniciada");
        }
    }

    public void dispositivoEmparejado() {
        try {
            if(Build.VERSION.SDK_INT >= 31) {
                if(ActivityCompat.checkSelfPermission(this, Constants.BLUETOOTH_SCAN_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                    requestBluetoothPermission();
                    return;
                }
            }

            if(adapter == null) {
                return;
            }

            final Set<BluetoothDevice> pairedDevice = adapter.getBondedDevices();

            if(pairedDevice.size() > 0) {
                for(BluetoothDevice device : pairedDevice) {
                    final String deviceName = device.getName();
                    LogUtils.d(TAG,"Dispositivo (2): " + deviceName);

                    // device emparejado, extraer nombre
                    if(deviceName.equals(Constants.DEVICE_NAME)) {
                        banderaYaHayOBD = true;
                    }
                }
            }

            if(banderaYaHayOBD) {
                tvOBDStatus.setText("Vinculado con ÉXITO"); // en la lista de disp. apareados está OBDII
                unregisterReceiver();
                contador = 100;
                LogUtils.d(TAG,"Ya se encuentra emparejado");

                if (!iniciarServicioBandera && !isComplete && encimaApp) { //Se agrega validacion para que solo se inicie una unica ocasion el servicio
                    iniciarServicioBandera = true;
                    iniciarServicio();
                }

            } else {
                LogUtils.d(TAG,"No se encuentra dispositivo emparejado");
                Toast.makeText(this, "DEBE EMPAREJA EL TELÉFONO AL OBD BLUETOOTH Y REINTENTAR LA INSTALACIÓN DE LA APLICACIÓN", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception ex) {
            LogUtils.e(TAG,"Finish dispositivoEmparejado Ex: " + ex);
            finish();
        }
    }

    private void iniciarServicio() {
        isComplete = true;
        LogUtils.d(TAG, "Metodo iniciarServicio en " + TAG);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                LogUtils.d(TAG,"Iniciando Servicio ConexionService desde Main Activity");
                startService(new Intent(getApplicationContext() , ConexionService.class));
            }
        }, 3000);
        Home();
    }

    private void unregisterReceiver() {
        try {
            unregisterReceiver(receiver);
        } catch (Exception ex) {
            LogUtils.e(TAG,"No registrado: " + ex);
        }
    }

    private void initFirebaseChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            final String channelId = getString(R.string.channel_id);
            final String channelName = getString(R.string.app_name);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW));
        }
    }

    // Método OnClick del botón ingresar para tener acceso a "PrincipalActivity.java"
    public void ingresarApp(final String codigo) {
        Log.d("TEST", "subscribe code: " + codigo);
        // Este método muestra una Task, que se puede usar en un objeto de escucha de finalización para determinar si la suscripción se realizó correctamente.
        FirebaseMessaging.getInstance().subscribeToTopic(codigo).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Log.d("TEST", "Subscripcion correcta" + codigo) ;
            }
        });

        // Almacena el código en alguna casilla de Firebase
        new LocalData().setCodigo( "codigo", codigo);
    }

    private void startDiscovery() {
        if(Build.VERSION.SDK_INT >= 31) {
            if(ActivityCompat.checkSelfPermission(this, Constants.BLUETOOTH_SCAN_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermission();
                return;
            }
        }

        if(adapter == null) {
            return;
        }

        adapter.startDiscovery(); //TODO Esta parte no deberia ejevcutarse ya que el dispositivo se emparejo con anterioridad
    }

    private void cancelDiscovery() {
        if(Build.VERSION.SDK_INT >= 31) {
            if(ActivityCompat.checkSelfPermission(this, Constants.BLUETOOTH_SCAN_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermission();
                return;
            }
        }

        if(adapter == null) {
            return;
        }

        adapter.cancelDiscovery();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void unlockDevice() {
        LockManager.getInstance().unlock(this);

        //Unlock the screen
        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    private void permisoEncimaApp(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                LogUtils.d("TEST", "If permiso aPP OVERLAYS");
                if(!dialogOverlays){
                    LogUtils.d("TEST", "Show OVERLAYS");
                    dialogOverlays = true;
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Para el correcto funcionamiento, por favor habiliar el permiso de aparecer por encima de otra aplicaciones")
                            .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialogOverlays = false;
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:" + getPackageName()));
                                    startActivity(intent);
                                }
                            });
                    builder.create();
                    builder.show();
                }
            } else {
                LogUtils.d("TEST", "If permiso aPP");
                encimaApp = true;
            }
        }
    }

    private boolean validatePermissions(){
        LogUtils.d("TEST", "Valor sdk: " + Build.VERSION.SDK_INT);
        boolean p1 = ContextCompat.checkSelfPermission(this, Constants.BLUETOOTH_CONNECT_PERMISSION) == PackageManager.PERMISSION_GRANTED;
        boolean p2 = ContextCompat.checkSelfPermission(this, Constants.BLUETOOTH_SCAN_PERMISSION) == PackageManager.PERMISSION_GRANTED;
        boolean p3 = ContextCompat.checkSelfPermission(this, Constants.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        LogUtils.d("TEST", "Permiso 1: " + p1);
        LogUtils.d("TEST", "Permiso 2: " + p2);
        LogUtils.d("TEST", "Permiso 3: " + p3);


        return Build.VERSION.SDK_INT >= 31 ? p1 && p2 && p3 : p3;
    }
    private void requestBluetoothPermission() {
        if(Build.VERSION.SDK_INT >= 31) {
            final int hasBluetoothConnectPermission = ContextCompat.checkSelfPermission(this, Constants.BLUETOOTH_CONNECT_PERMISSION);
            final int hasBluetoothScanPermission = ContextCompat.checkSelfPermission(this, Constants.BLUETOOTH_SCAN_PERMISSION);
            final int hasLocationPermission = ContextCompat.checkSelfPermission(this, Constants.ACCESS_FINE_LOCATION);

            final List<String> permissions = new ArrayList<>();

            if(hasBluetoothConnectPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Constants.BLUETOOTH_CONNECT_PERMISSION);
            }

            if(hasBluetoothScanPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Constants.BLUETOOTH_SCAN_PERMISSION);
            }

            if(hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Constants.ACCESS_FINE_LOCATION);
            }

            if(permissions.isEmpty()) {
                requestEnableBluetooth();
            } else {
                final String[] requestPermissions = new String[]{Constants.BLUETOOTH_CONNECT_PERMISSION, Constants.BLUETOOTH_SCAN_PERMISSION, Constants.ACCESS_FINE_LOCATION};
                requestPermissions(requestPermissions, REQUEST_BLUETOOTH_PERMISSION_SCAN);
            }
        } else if (ContextCompat.checkSelfPermission(getApplicationContext(), Constants.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{Constants.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            requestEnableBluetooth();
        }
    }

    public void Home(){
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                LogUtils.d(TAG, "Timer para cerrar el Main Activit 2");
                StateNotification.getInstance().notifyState("En pausa");
                ConnectionManager.getInstance().setEstadoActual(ConnectionManager.EstadoConexionDef.ESTADO_DESCONECTADO);
                if(isVisible) {
                    //Utils.sleep("Main", 5000);
                    //finish();
                    final Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    startActivity(intent);
                }
            }
        };

        final Timer timer = new Timer();
        timer.schedule(task, Constants.TIEMPO_ESPERA_INICIO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LogUtils.d(TAG, "Metodo onRequestPermissionsResult request code: " + requestCode + " Lista permisos" + permissions.toString() + "Lista grntResults" + grantResults.toString());
        boolean alert = false;
        String permisosFaltantes = "Por favor habilita los siguientes permisos para poder utilizar la aplicación OBD APP<br>";
        for (int i=0; i<permissions.length;i++ ){
            LogUtils.d(TAG, "Metodo Permiso" + permissions[i] + "grantResults" + grantResults[i] );
            if(permissions[i].equals(Constants.BLUETOOTH_SCAN_PERMISSION)){
                if (grantResults[i]!= PackageManager.PERMISSION_GRANTED){
                    alert = true;
                    permisosFaltantes += "<b>Pemisos para usar Bluetooth</b><br>";
                }
            } else if (permissions[i].contains(Constants.ACCESS_FINE_LOCATION)){
                if (grantResults[i]!= PackageManager.PERMISSION_GRANTED){
                    alert = true;
                    permisosFaltantes += "<b>Pemisos para conocer la ubicaion</b><br>";
                }
            }
        }
        if (alert){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(Html.fromHtml(permisosFaltantes))
                    .setTitle("Atención")
                    .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    });
            builder.create();
            builder.show();
        } else {
            requestEnableBluetooth();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtils.d(TAG, "Metodo onActiviryResult request code: " + requestCode );


        if(requestCode == REQUEST_BLUETOOTH_ON) {
            if(resultCode == RESULT_OK) {
                LogUtils.d(TAG,"Bluetooth Enabled successful");
                batteryPermission();
            } else {
                LogUtils.d(TAG,"Error con el permiso con el codigo L654 " + requestCode);
                Toast.makeText(this, "Error, para usar la aplicación debe permitir encender el Bluetooth, inténtelo nuevamente", Toast.LENGTH_LONG).show();
                finish();
            }
        } else if(requestCode == REQUEST_PERMISSION_BATTERY) {
            final PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(getPackageName());

                if(isIgnoringBatteryOptimizations) {
                    // Ignoring battery optimization
                    LogUtils.d(TAG, "Battery Permission Successful");
                    //permisoEncimaApp();
                    autorunPermission();
                } else {
                    // Not ignoring battery optimization
                    LogUtils.d(TAG,"Error con el permiso con el codigo L670" + requestCode);
                    Toast.makeText(this, "Error, para usar la aplicación debe permitir dejar de optimizar el uso de la batería, inténtelo nuevamente", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }
}