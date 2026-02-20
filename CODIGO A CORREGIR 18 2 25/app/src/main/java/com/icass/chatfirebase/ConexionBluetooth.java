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
    private OutputStream outputStream; //Clase que sirve para escribir ficheros byte a byte. escribe 'n' bytes en el fichero, concretamente, los del array 'b', empezando en la posición 'start'.
    private BluetoothSocket socket;
    private boolean primeraConexion;
    private String respuestaOBD = "";
    private String lastErro403 = "";

    public ConexionBluetooth(@NonNull BluetoothDevice bluetoothDevice, @NonNull Service service, @NonNull CallbackService listener) { // Recibe el valor de los parámetros mandados de la clase "ConexionService"
        LogUtils.d(TAG, "Se crea la instancia de la clase "+ TAG);
        this.bluetoothDevice = bluetoothDevice;
        this.service = service;
        this.banHiloSiempre = true;
        primeraConexion = true;

        DataServer.initInstance().initialize(service, new EventCheckProcesos() {
            public void enviarOBD(String comando, boolean banMess) {
                try {
                    Log.d(TAG,"Se llama al metodo enviarOBD: " + comando + " banMessage: " + banMessage);
                    ComandoOBD = comando;
                    banMessage = banMess;
                    write(comando); // Manda el comando recibido al método write de está clase.
                    banderaComandos = comando.contains(comando0105);
                } catch (Exception ex) { // Mensaje de error en caso de no funcionar en try.
                    String messageError = "ERROR AL ENVIAR AL OBD: " + ex;
                    Log.e(TAG, messageError);
                }
            }
        }, listener);
    }

    private void iniciarConexion() {
        LogUtils.d(TAG, "Intentando Conexion Bluetooth con el dispositivo:" +  bluetoothDevice.getName()+ " L91");

        try {
            final ParcelUuid[] uuids = this.bluetoothDevice.getUuids();
            for(ParcelUuid uuid: uuids) {
                    this.socket = this.bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid.toString()));
                    this.socket.connect();
                    this.outputStream = socket.getOutputStream();
                    this.inputStream = socket.getInputStream();
                    break;
            }
            LogUtils.d(TAG,"---Conexion exitosa al dispositivo, se inicia envio de comandos de configuaracion L102---");
            intentandoConexion = false;
            bluetoothEncendido = true;// eliminar esta bandera
            DataServer.getInstance().servicesCheckProcesos.resetBanderas();
            iniciarEnvioConfiguracion();
        } catch (IOException ex){
            LogUtils.d(TAG,"Conexion sin exito al dispositivo, exception "+ ex.getMessage() + "L105");
            ConnectionManager.getInstance().setEstadoActual(EstadoConexionDef.ESTADO_DESCONECTADO);
            ProgressManager.getInstance().setProgreso(ProgresoDef.ESTADO_DESCONECTADO);
            Utils.sleep(TAG + "Se esperan 30 segundos para reintentar conectar",30000);
            intentandoConexion = false;
            if (!intentandoConexion){
                LogUtils.d(TAG, "inputStrema es nulo porque no hay conexion,se intenta hacer conexion");
                intentandoConexion = true;
                iniciarConexion();
            } else {
                LogUtils.d(TAG, "inputStrema es nulo porque no hay conexion, algun hilo ya esta intentando la conexion");
            }
        }

    }

    private void iniciarEnvioConfiguracion(){
        crearAlarma();
        try {
            DataServer.getInstance().banHiloSiempre = true;
            Log.d(TAG, "Se van a enviar los comandos de configuracion L145");//imprime está leyenda
            if (comando.length() >= 2) {
                DataServer.getInstance().setInitComandosWeb();
            } else {
                DataServer.getInstance().initConfiguracion("Conexion Bluetooth L132"); // Inicia secuencia de comandos de la clase "DataServer.java"
            }
        } catch (Exception ex) { // Mensaje de error en caso de no funcionar en try.
            LogUtils.d(TAG, "Catch al intentar enviar comandos" + ex);
            if(socket != null) {
                try {
                    socket.close();
                } catch (IOException ioException) {
                    LogUtils.d(TAG, "Error al cerrar el socket "+ ioException);
                }
            }

            LogUtils.d(TAG, "ERROR INIT " + ex.getMessage());

            final Intent intent = new Intent(ProgressManager.CONEXION_OBD);
            intent.putExtra("STATE", "RECONECTAR");
            intent.putExtra("CONTADOR", 1);
            LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  148");
            service.sendBroadcast(intent);//lo manda al canal de transmisión.
            if (!intentandoConexion){
                LogUtils.d(TAG, "Error al enviar comandos, se intenta hacer conexion");
                intentandoConexion = true;
                iniciarConexion();
            } else {
                LogUtils.d(TAG, "Error al enviar comandos no se intenta, algun hilo ya esta intentando la conexion");
            }
        }

    }

    private void soloComando(@NonNull String comando) { // Cuando llega un comando y esta en velocidad
        LogUtils.d(TAG, "soloComando: " + comando);

        if(DataServer.getInstance().servicesCheckProcesos != null) { // Si los valores son nulos
            final Procesos proceso = new Procesos(true, comando, false, true);
            DataServer.getInstance().servicesCheckProcesos.pushProcesoMessage(proceso);
        }
    }

    public void write(@NonNull String message) throws IOException {
        LogUtils.d(TAG, "write: " + message);
        message = message.contains("AT") ? message : message + "1";
        final String data = message + "\r";

        if(outputStream != null) {
            outputStream.write(data.getBytes());
            outputStream.flush(); // Envía lo que está en la variable "outputStream2" de inmediato a los flujos que periódicamente vacían su contenido
        } else {
            if (!intentandoConexion){
                LogUtils.d(TAG, "OutputStream es nulo porque no hay conexion,se intenta hacer conexion");
                intentandoConexion = true;
                iniciarConexion();
            } else {
                LogUtils.d(TAG, "OutputStream es nulo porque no hay conexion, algun hilo ya esta intentando la conexion");
            }
        }
    }

    public void reconectar() {//recibe el valor de la varible contador
        LogUtils.d(TAG, "Metodo Reconectar");
        Utils.sleep("1", 10000);
        try {
            // Código bluetooth que sirve para enviar una cadena simple a través de bluetooth
            final ParcelUuid[] uuids = this.bluetoothDevice.getUuids();
            for(ParcelUuid uuid : uuids) { //recorre la cadena
                try {
                    this.socket = this.bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid.toString()));
                    this.socket.connect();
                    this.outputStream = socket.getOutputStream();
                    this.inputStream = socket.getInputStream();
                    break;
                } catch (IOException ex) {
                    LogUtils.d(TAG, "init 2 "+ ex);
                    ConnectionManager.getInstance().setEstadoActual(EstadoConexionDef.ESTADO_DESCONECTADO);
                    ProgressManager.getInstance().setProgreso(ProgresoDef.ESTADO_DESCONECTADO);
                    // java.io.IOException: read failed, socket might closed or timeout, read ret: -1

                    closeStreams();
                    LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  202");
                    //service.sendBroadcast(new Intent(ProgressManager.REINICIAR));
                }
            }

            LogUtils.d(TAG, "RECONECTADO");
            Intent intent = new Intent(ProgressManager.CONEXION_OBD);
            intent.putExtra("STATE", "CONECTADO_OK");
            intent.putExtra("NAME_DEVICE", this.bluetoothDevice.getName());////obtiene el valor de la variable "device"

            DataServer.getInstance().initConfiguracion("setInitComandos 2"); // Inicia secuencia de comandos de la clase "DataServer.java" no estaba!!!
            LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  214");
            service.sendBroadcast(intent); // Lo manda a la transmisión
        } catch (Exception ex) {
            LogUtils.d(TAG, "reconectar 1" + ex.getMessage());
            ConnectionManager.getInstance().setEstadoActual(EstadoConexionDef.ESTADO_DESCONECTADO);
            ProgressManager.getInstance().setProgreso(ProgresoDef.ESTADO_DESCONECTADO);

            final String sb = "ERROR RECONECTAR " + ex.getMessage();
            LogUtils.e(TAG, sb);

            Intent intent = new Intent(ProgressManager.CONEXION_OBD);
            intent.putExtra("STATE", "RECONECTAR");
            intent.putExtra("CONTADOR", 1); // se le suma 1 al contador
            LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  225");
            service.sendBroadcast(intent);// Envía esos valores a la transmisión

            try {
                if(socket != null) {
                    socket.close();
                }
            } catch (IOException exception) {
                LogUtils.e(TAG,"reconectar" + exception.getMessage());
            }

            new Thread(new Runnable() {
                public void run() {
                    Utils.sleep("4", 1000);

                    LogUtils.d(TAG, "reconectar: " + contador2);
                }
            }).start(); // Inicia el proceso
        }
    }

    public void cerrar() {//Se cierra la conexion solo en la clase bluetooht
        try {
            LogUtils.d(TAG, "Se cierra la conexion con el metodo cerrar() en " + TAG);
            //DataServer.getInstance().cerrarHilo();
            //this.banHiloSiempre = false;
            bluetoothEncendido = false;
            this.outputStream = null;
            this.inputStream = null;
            this.socket.close();

            banApagado = true;
        } catch (Exception ex) {
            LogUtils.e(TAG,"Error al cerrar la conexion" + ex.getMessage());
        }
    }

    private void closeStreams() {
        try {
            this.outputStream.close();
        } catch (Exception ex) {
            LogUtils.e(TAG,"Error on close outputStream: " + ex);
        }

        try {
            this.inputStream.close();
        } catch (Exception ex) {
            LogUtils.e(TAG, "Error on close inputStream: " + ex);
        }

        try {
            this.socket.close();
        } catch (Exception ex) {
            LogUtils.e(TAG, "Error on close socket: " + ex);
        }

        try {
            this.socket = null;
        } catch (Exception ex) {
            LogUtils.e(TAG, "Error on null socket: " + ex);
        }
    }

    public void crearAlarma() {
        final Context context = ResourceManager.getInstance().getContext();
        final Intent loggerIntentReceiver = new Intent(context, alarmEstadoVehiculo.class);
        Log.d("TEST", "Alarma para estado de auto");
        if (alarmManager == null) {
            LogUtils.d(TAG, "Alarm managger es null");
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            pendingIntent = PendingIntent.getBroadcast(context, 7236, loggerIntentReceiver, PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent == null) {
                LogUtils.d(TAG, "Pendding item null");
                pendingIntent = PendingIntent.getBroadcast(context, 7235, loggerIntentReceiver, 0);
            }
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), Constants.INTERVAL_30_SECONDS, pendingIntent);
        }
    }


    @Override
    public void run() {
        LogUtils.d(TAG, "--------Se inicia Hilo de clase Conexion Bluetooth----------");
        this.banHiloSiempre = true;
        LogUtils.d(TAG, "Proceso de inciar conexion Iniciado L 335 ----------");
        if (!intentandoConexion){
            LogUtils.d(TAG, "se intenta hacer conexion");
            intentandoConexion = true;
            iniciarConexion();
        } else {
            LogUtils.d(TAG, "Alguin ya esta ya esta intentando la conexion");
        }
        LogUtils.d(TAG, "Proceso de inciar conexion finalizado 343----------");
        Utils.sleep("2", 2000);

        String errorSent = "";
        StringBuilder mSb = new StringBuilder();
        String cadFix = "";
        ArrayList<String> list = new ArrayList<>();

        JSONArray jsonArray = new JSONArray();
        LogUtils.d(TAG, "Antes de entrar al ciclo While para recibir respuestas ");

        while(this.banHiloSiempre) {//Aqui se van recibiendo las respuestas de los comandos
            Log.d("TEST", "Dentro del while L 355");
            if(inputStream != null) {
                /*if(ConnectionManager.getInstance().isApagado() && DataServer.getInstance().servicesCheckProcesos.sizeProcesos() > 5) {//Limppiar cola//
                    DataServer.getInstance().servicesCheckProcesos.clearAllProcesos();
                    CheckearColaComandos.banEnviar = false;
                    mSb = new StringBuilder();
                }*/

                Log.d(TAG, "InpusStream esta activo");
                try {
                    Log.d(TAG, "Valor de bandera banEnviar " + CheckearColaComandos.banEnviar);
                    //Log.d(TAG, "Dentro del Try L318");
                    final byte[] buffer = new byte[1024];
                    final String strReceived = new String(buffer, 0, inputStream.read(buffer));
                    //LogUtils.d(TAG, "LEIDO TEMP: " + strReceived);

                    if(this.banIniciar) {
                        if(strReceived.contains(">")) {//aqui se hace los cambios por la cadena
                            mSb.append(strReceived);
                            cadFix = "";
                            list.clear();
                            cadFix = mSb.toString().replaceAll("\r\n", " ").replace(">", "");
//                            LogUtils.d(TAG, "Valor de la cadena completa Sin saltos " + cadFix);
                            if (!cadFix.contains("DATA") && !cadFix.contains("ELM") && !cadFix.contains("SEAR")){
                                list = new ArrayList<>(Arrays.asList(cadFix.split(" ")));
                            } else {
                                list = new ArrayList<>(Arrays.asList(cadFix.split("\r\n")));
                            }
                            LogUtils.d(TAG, "Valor de List " + list);
                            //TODO Antes de concatenar debemos validar que si sea un comando valido
                            respuestaOBD = list.size() > 1 ? list.get(0).replace("\r\n", "").replace(" ", "").replace(">", "") :
                                    mSb.toString().replace("\r\n", "").replace(" ", "").replace(">", "");
                            LogUtils.d(TAG, "Respuesta OBD " + respuestaOBD);
                            //respuestaOBD = strReceived.replace("\r\n", "").replace(" ", "").replace(">", "");ghg

                            if (respuestaValida(ComandoOBD, respuestaOBD, comandoAuxiliar) ) {
                                //Si el codigo es diferente al ID matar app
                                /*if (ComandoOBD.contains("@2")){
                                    Log.d("TEST", "Comando AT@2");
                                    if (!respuestaOBD.contains(Constants.ID_VEHICULO)){
                                        Log.d("TEST", "ID Invalido XXXX");
                                        alarmManager.cancel(pendingIntent);
                                        new LocalData().setCodigo( "forceClose", "true");
                                        Context context = ResourceManager.getInstance().getContext();
                                        Intent launchIntent = new Intent(context, MainActivity2.class);
                                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        context.startActivity(launchIntent);
                                    } else {
                                        new LocalData().setCodigo( "forceClose", "false");
                                        Log.d("TEST", "ID valido");
                                    }
                                }*/
                                DataServer.getInstance().servicesCheckProcesos.comandoSiguiente = true;
                                //mSb.append(respuestaOBD);
                                String error_actual = respuestaOBD;
//                                Log.d("GABO", "Valor de error actual " + error_actual);

                                if(error_actual.trim().compareTo("") != 0) {
//                                    LogUtils.d(TAG, "WEBCOMAND: " + ComandoOBD);

                                    if(ComandoOBD.equals("03") && comando.length() >= 2) {//Se supone que hay una respuesta para anaizar codigos
                                        LogUtils.d(TAG, "Entra en el if linea 335" ); //Aqui no entra
                                        Ban_Message_velocidad = true;
                                        soloComando(comando);
                                        error_actual = "";
                                        comando = "1";
                                        LogUtils.d(TAG, "comm: " + comando);
                                    }

                                    if(banMessage) { // para saber cuando enviar el mensaje que se recibio
                                        LogUtils.d(TAG, "Entra en el if linea 70" );
                                        final String date = DateUtils.getDateFormatted();

                                        if(ComandoOBD.equals("03") ) {//Se supone que hay una respuesta para anaizar codigos
                                            if(NetworkManager.getInstance().isNetworkConnected()) {
                                                error_actual = "" + "###" + date + error_actual;
                                            } else {
                                                error_actual = "SI" + "###" + date + error_actual;
                                            }
                                        } else {
                                            if(NetworkManager.getInstance().isNetworkConnected()) {
                                                error_actual = "" + "###" + date + error_actual;
                                            } else {
                                                error_actual = "SI" + "###" + date + error_actual;
                                            }
                                        }

//                                        LogUtils.e(TAG, "enviarMsgAlServer (3): " + error_actual);
//                                    DataServer.getInstance().enviarMsgTest(jsonArray.toString());// se envia cadena
                                        jsonArray = new JSONArray();
                                        //TODO Se envia comando
//                                        LogUtils.d(TAG, "Envio Comando");
                                        DataServer.getInstance().enviarMsgAlServer(error_actual, TipoEnvioDef.COMANDO);
                                        DataServer.getInstance().servicesCheckProcesos.banMessage = false;
//                                        LogUtils.e(TAG, "MANDARWEB: " + error_actual);
                                        if(PrincipalActivity.banSend) {
                                            if(PrincipalActivity.isConcatenar) {
                                                banMessage = true;
                                            }
                                        } else {
                                            banMessage = false;
                                        }

                                        if(Ban_Message_velocidad) { // bandera que sirve para saber que terminando la tarea de recibir el comando, se apague la parte del bluetooth
                                            Ban_Message_velocidad = false;
                                            LogUtils.d(TAG,"comandoWeb: Apagar");
                                            Intent intentData3 = new Intent(ProgressManager.MSG_OBD);
                                            intentData3.putExtra("DATA", "APAGAR");
                                            LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  353");
                                            service.sendBroadcast(intentData3);
                                        }
                                    }

                                    StateNotification.getInstance().notifyState("Comando: " + ComandoOBD + "::" + error_actual);
//                                    LogUtils.d(TAG, "Comando: " + ComandoOBD + " Respuesta: " + error_actual);
                                    final JSONObject jsonObject = new JSONObject();
                                    jsonObject.put("Comando", ComandoOBD);
                                    jsonObject.put("Respuesta", error_actual);
                                    jsonArray.put(jsonObject);

                                    LogUtils.d(TAG, "LEIDO COMPLETO..." + error_actual);
                                    final boolean result = error_actual.contains(NODATA);

                                    final String respuesta = error_actual + "";
                                    ConnectionManager.getInstance().comprobarRespuesta(ComandoOBD, respuesta);//Metodo solo para cambia estado de notificacion
                                    //TODO Aqui se tiene que agregar la valiadcion y llammar a un comando especial en caso de que el comadno regrese un NO DATA

                                    if (ConnectionManager.getInstance().getEstado().equals("Encendido") && primeraConexion) {
                                        DataServer.getInstance().agregarEncuesta();
                                        primeraConexion = false;
                                    }

                                    if(banderaComandos && result && bluetoothEncendido) {
                                        LogUtils.d(TAG,"comando: true");
                                        final Intent intent = new Intent(ProgressManager.MSG_OBD);
                                        intent.putExtra("DATA", "APAGAR");
                                        LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  385");
                                        service.sendBroadcast(intent);
                                    }
                                    Log.d("TEST1", "Valor de banSend Y de bluetoothEncendido: " + PrincipalActivity.banSend+ " - " + bluetoothEncendido);

                                    if(PrincipalActivity.banSend && bluetoothEncendido) {//TODO
                                        if(PrincipalActivity.isConcatenar && !banMessage) {
                                            error_actual = CheckearColaComandos.strConcat + error_actual.replace(CheckearColaComandos.strReplace, "");
                                            LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  391");
                                            service.sendBroadcast(new Intent(ProgressManager.SUM_CONTADOR));
                                        }

                                        final String sb4 = "MSG WEB TO OBD SEND; " + error_actual;
                                        LogUtils.d(TAG, sb4);
                                        Intent intent = new Intent(ProgressManager.MSG_OBD);

                                        intent.putExtra("BAN", true);
                                        intent.putExtra("DATA", error_actual);
                                        LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  400");
                                        service.sendBroadcast(intent);

                                        if(PrincipalActivity.isEnviar) {//TODO Se envia diagnostico
                                            final boolean isPrimerEnvio = DataServer.getInstance().isPrimerEnvio();

                                            if(NetworkManager.getInstance().isOnline(ResourceManager.getInstance().getContext())) {
                                                Log.d("TEST1", "Dispositivo tieene conexion se prepara envio de diagnostico");
                                                String cadenaToSend = "";
                                                if (diagnosticoSI.length() > 0 ){
                                                    cadenaToSend = "%%" + diagnosticoSI + error_actual;
                                                    diagnosticoSI = "";
                                                } else {
                                                    cadenaToSend = "%%" + error_actual;
                                                }

                                                if(isPrimerEnvio) { //TODO Se envia la primera cadena, revisar si esto es necesario
                                                    LogUtils.d(TAG, "enviarMsgAlServer (Primer envio 1): ");
                                                    DataServer.getInstance().setPrimerEnvio(false);
                                                }

                                                //DataServer.getInstance().enviarMsgTest(jsonArray.toString());
                                                //jsonArray = new JSONArray();
                                                //TODO Envio Diagnositico
                                                Log.d(TAG, "Envio Diagnostico");
                                                Log.d("TEST1", "Cadena que se envia a Diagnostico: " + cadenaToSend);
                                                DataServer.getInstance().enviarMsgAlServer(cadenaToSend, TipoEnvioDef.DIAGNOSTICO);

                                                //StateNotification.getInstance().notifyServer();
                                            } else {
                                                LogUtils.d("TEST1","Internet: No hay Interet, se guarda la cadena");
                                                if (diagnosticoSI.isEmpty()){
                                                    diagnosticoSI = "SI" + error_actual + "XX";
                                                } else {
                                                    diagnosticoSI = diagnosticoSI + error_actual + "XX";
                                                }
                                                LogUtils.d("TEST1","Cadena actual SI: " + diagnosticoSI);
                                            }
                                        } else {
                                            LogUtils.d(TAG, "ERROR IS ENVIAR IS FALSE");
                                        }

                                        DataServer.getInstance().servicesCheckProcesos.popProceso();
                                        CheckearColaComandos.banEnviar = false;
                                        CheckearColaComandos.strConcat = "";
                                    } else {
                                        if(PrincipalActivity.isConcatenar && bluetoothEncendido ) {
                                            final String strConcat = error_actual.replace(CheckearColaComandos.strReplace, "");
                                            CheckearColaComandos.strConcat = CheckearColaComandos.strConcat + strConcat;
                                        } else if(errorSent.compareTo(error_actual) != 0 && bluetoothEncendido) {
                                            final String sb6 = "RECIBIDO _ SON DIFERENTES: " + error_actual + "-" + errorSent + " L439";
                                            LogUtils.d(TAG, sb6);

                                            if(PrincipalActivity.isEnviar) {
                                                LogUtils.d("TEST", "Si paso el if de enviar, dentro de 4300, valor de error actual " + error_actual);

                                                if (validaRespuesta03(error_actual)){
                                                    final String date = DateUtils.getDateFormatted();
                                                    if(NetworkManager.getInstance().isOnline(ResourceManager.getInstance().getContext())) {
                                                        String cadenaToSend = "";
                                                        error_actual = ""+ "###" + date + error_actual;
                                                        if (fallaSI.length() > 0 ){
                                                            cadenaToSend = fallaSI + "###" + error_actual;
                                                            fallaSI = "";
                                                        } else {
                                                            cadenaToSend = error_actual;
                                                        }
                                                        DataServer.getInstance().enviarMsgAlServer(cadenaToSend, TipoEnvioDef.COMANDO_4300);
                                                    } else {
                                                        if (fallaSI.isEmpty()){
                                                            fallaSI = "SI" + "###" + date + error_actual + "XX";
                                                        } else {
                                                            fallaSI = fallaSI + "###" + date + error_actual + "XX";
                                                        }
                                                    }
                                                } else {
                                                    Log.d("TEST", "El valor des 4300, no se envia cadena");
                                                }
                                            }
                                            errorSent = error_actual;

                                        }

                                        DataServer.getInstance().servicesCheckProcesos.popProceso();

                                        new Thread(new Runnable() {
                                            public void run() {
                                                CheckearColaComandos.banEnviar = false;
                                            }
                                        }).start();
                                    }

                                    Intent intentData3 = new Intent(ProgressManager.MSG_OBD);
                                    intentData3.putExtra("BAN", false);
                                    intentData3.putExtra("DATA", error_actual);
//                                    LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  494");
                                    service.sendBroadcast(intentData3);
                                    mSb = new StringBuilder();
                                }
                            } else {//Aqui se mete el coamndo para enviar un comando auxiliar
//                                LogUtils.d(TAG, "Se envia comando 0106 como comando auxiliar");
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
                    // Se elimina llamado a getMessage por posiblemente ser nulo y se pone toString para asegurar que tenga un crash
                    final String messageError = ex.toString();
                    LogUtils.e(TAG, "Catch 511 Thread Run " + messageError);
                    if(messageError.contains("socket closed")) { // Si el contenido de la variable "msgError" contiene la leyenda "socket closed"
                        Log.d("TEST", "Error del socket closed");
                        if (!intentandoConexion){
                            String parametro = new LocalData().getCodigo( "forceClose");
                            Log.d("TEST", "Valor de parametro force close L 6212  " + parametro);
                            if (parametro.equalsIgnoreCase("false")){
                                LogUtils.d(TAG, "OutputStream es nulo porque no hay conexion,se intenta hacer conexion");
                                intentandoConexion = true;
                                bluetoothEncendido = false;
                                this.outputStream = null;
                                this.inputStream = null;
                                iniciarConexion();
                            }
                        } else {
                            LogUtils.d(TAG, "OutputStream es nulo porque no hay conexion, algun hilo ya esta intentando la conexion");
                        }
                    }
                }
            } else {
                Log.d(TAG, "Se va a inciar Conexion para asignar el inputStrema");
                LogUtils.d(TAG, "Proceso de inciar conexion Iniciado L 525 ----------");
                if (!intentandoConexion){
                    LogUtils.d(TAG, "inputStrema es nulo porque no hay conexion,se intenta hacer conexion");
                    intentandoConexion = true;
                    iniciarConexion();
                } else {
                    LogUtils.d(TAG, "inputStrema es nulo porque no hay conexion, algun hilo ya esta intentando la conexion");
                }
                Log.d(TAG, "Proceso de inciar conexion finalizado L527----------");
            }
        }
        Log.d(TAG, "Se cierra Hilo de clase Conexion Bluetooth");
    }

    public boolean respuestaValida(String comando, String respuestaInicial, Boolean comAuxiliar) {
        boolean respuesta = true;
        LogUtils.d(TAG, "Comando: " + comando + " la respuesta es |" + respuestaInicial + "|");
        if (!comAuxiliar){
            if (!comando.contains("AT") && !comando.contains(Constants.comandoEncendido) && !comando.contains(Constants.comandoEncendido1)){//Si el comando es diferente a los de configuración o al de encendido
                //LogUtils.d(TAG, "No es comando de configuracion y de encendido");
                if(respuestaInicial.contains(NODATA)){
                    respuesta = false;
                } else {
                    //LogUtils.d(TAG, "La respuesta trae inforamcion!!!");
                }
            }
        } else {
            //LogUtils.d(TAG, "Es un coamndo Auxiliar");
            comandoAuxiliar = false;
        }
        return respuesta;
    }

    public String respuestaAuxiliar(String comando){
        String respuesta = comando;
        switch (comando){
            case "012F":
                respuesta = "22F42F";
            break;
            default:
        }
        return respuesta;
    }

    public boolean validaRespuesta03(String codigoRespuesta){
        if (codigoRespuesta.toLowerCase().contains("n")) return false;
        for (char caracter : codigoRespuesta.substring(2).toCharArray()){
            if (caracter != '0')
                return true;
        }
        return false;
    }
}