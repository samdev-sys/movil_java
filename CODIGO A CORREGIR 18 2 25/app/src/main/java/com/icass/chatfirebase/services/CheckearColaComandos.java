package com.icass.chatfirebase.services;

import android.app.Service;
import android.content.Intent;
import android.nfc.Tag;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.icass.chatfirebase.activity.PrincipalActivity;
import com.icass.chatfirebase.managers.ProgressManager;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class CheckearColaComandos {
    private static final String TAG = CheckearColaComandos.class.getSimpleName();

    public static boolean banEnviar = false;
    public static String strConcat = "";
    public static String strReplace = "";
    public int contadorComandos = 0;
    private int TIME = 1000;
    private boolean banHiloSiempre;
    private EventCheckProcesos callback;
    public boolean banMessage = false;
    public boolean comandoSiguiente = true;

    @Nullable
    private Thread thread = null;

    @NonNull
    private final Service service;

    @NonNull
    public static List<Procesos> colaProcesos = new ArrayList<>(); // List de los procesos envíados de la clase "DataServer", estos pueden ser modificados, eliminados o añadidos.

    @NonNull
    public static List<Procesos> colaMessage = new ArrayList<>();

    public interface EventCheckProcesos {
        void enviarOBD(String str, boolean banMessage); //manda llamar el método "enviarOBD" de la clase "ConexionBluetooth.java"
    }

    public CheckearColaComandos(@NonNull Service service, @NonNull EventCheckProcesos callback) {
        this.callback = callback; //la variable "callback" toma el valor de callback2.
        this.service = service; //la variable "mService" toma el valor de mService2.
        this.banHiloSiempre = true; //banHiloSiempre es de tipo booleano con valor de true.
    }

    public static String getSizeColas(){
        return "Procesos: " + colaProcesos.size() + " Mensajes: " + colaMessage.size();
    }

    public void clearAllProcesos() { //limpia el arreglo
        this.colaProcesos.clear();
        banEnviar = false;
    }

    public void resetBanderas(){
        Log.d("Cola de comandos", "Reset de banderas ");
        banEnviar = false;
        comandoSiguiente = true;
    }

    public void clearAllMessages() { //crea un array nuevo
        this.colaMessage.clear();
    }

    @NonNull
    public List<Procesos> getColaProcesos() {
        return colaProcesos;
    }

    public void pushProceso(Procesos proceso) { //método que es utilizado en "DataServer" por "setSecuenciasComandos"
        this.colaProcesos.add(proceso); //Agrega el proceso anteriomente envíado al ArrayList
    }

    public void pushProcesoMessage(Procesos proceso) { //método que es utilizado en "DataServer" por "setSecuenciasComandos"

        this.colaMessage.add(proceso); //Agrega el proceso anteriomente envíado al ArrayList
    }

    public void popProceso() { //método utiizado por "ConexionBluetooth.java" en el método "run"
        if(this.colaProcesos.size() > 0) { //si el tamaño del array es mayor a 0
            this.colaProcesos.remove(0); //elimina el primer elemento
        }
    }
    public int sizeProcesos(){
        return colaProcesos.size();
    }

    public String estadoHilo(){
        return " "+this.thread.isAlive();
    }

    public void cerrarHilo() {
        this.banHiloSiempre = false;
//        this.colaMessage.clear();
//        this.colaProcesos.clear(); //TODO REvisar
    }

    public synchronized void message() {
        final Procesos proceso = CheckearColaComandos.this.colaMessage.get(0);
        final String comandoMess = proceso.getComando();

        // La variable de tipo string "strReplace" será igual al resultado de la línea anterior
        CheckearColaComandos.strReplace = "41" + proceso.getComando().substring(2);

        // Concatena el mensaje de comillas anterior con el comando de la posición "0"
        LogUtils.d(TAG,"1. MSG ENVIAR A OBD: " + proceso.getComando());
        final Intent intent = new Intent(ProgressManager.WRITE_LOG);

        // Concatena el mensaje de comillas anterior con el comando de la posición "0"
        final String msg2 = ">" + proceso.getComando();
        intent.putExtra("DATA", msg2); // Lanza el intentData con el mensaje de sb3
        LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  99");
        service.sendBroadcast(intent);
        callback.enviarOBD(comandoMess, banMessage = true);

        clearAllMessages();
    }

    public void iniciarServicio() {
        LogUtils.d(TAG, " Iniciar servicio en Chekear cola de comandos");
        if(thread == null) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    LogUtils.d(TAG, " Run hilo thread en chekear cola de comandos");
                    while(CheckearColaComandos.this.banHiloSiempre) {//mientras que la variable "banHiloSiempre" de está clase sea = a true TODO Siempre se esta ejecutando
                        //Log.d("TEST", "Dentro del while de chekear cola comandos");
                        try {
                            if(CheckearColaComandos.this.colaProcesos.size() > 0 || CheckearColaComandos.this.colaMessage.size() > 0) { // Si el ArrayList "colaProcesos" es mayor a 0 y "banEnviar" en diferente al valor
                                if (!CheckearColaComandos.banEnviar) {
                                    Log.d("TEST", "Flujo dentro del if ban Eviar en Cola de comandos");
                                    CheckearColaComandos.banEnviar = true;

                                    if (colaProcesos.size() > 0) {
                                        //LogUtils.d(TAG, "Numero de procesos en cola: " + colaProcesos.size());
                                    }

                                    if (colaMessage.size() > 0) {
                                        //LogUtils.d(TAG, "Numero de mensajes en cola: " + colaMessage.size());
                                    }

                                    if (CheckearColaComandos.this.colaMessage.size() > 0 && CheckearColaComandos.this.colaProcesos.size() == 0) {
                                        LogUtils.d(TAG, "If linea 132");
                                        message();
                                    } else {
                                        if (colaMessage.size() > 0) {
                                            banMessage = true;
                                            message();
                                        } else if (!banMessage) {
                                            if (comandoSiguiente) {
                                                final Procesos proceso = colaProcesos.get(0); // El objeto "pro" obtiene el valor "0" del ArrayList "colaProcesos"
                                                LogUtils.d(TAG, "Proceso descripcion -> Comando: " + proceso.getComando() + "(is enviar, isconcatenar, + isBanWeb)" + "(" + proceso.isEnviar() + "," + proceso.isConcatenar() + "," + proceso.isBanWEB() + ")");
                                                PrincipalActivity.banSend = proceso.isBanWEB();
                                                PrincipalActivity.isEnviar = proceso.isEnviar();
                                                PrincipalActivity.isConcatenar = proceso.isConcatenar();
                                                final String comando = proceso.getComando();

                                                // La variable de tipo string "strReplace" será igual al resultado de la línea anterior
                                                CheckearColaComandos.strReplace = "41" + proceso.getComando().substring(2);

                                                // Concatena el mensaje de comillas anterior con el comando de la posición "0"
                                                LogUtils.d(TAG, "2. MSG ENVIAR A OBD: " + proceso.getComando());
                                                Intent intent = new Intent(ProgressManager.WRITE_LOG);

                                                // Concatena el mensaje de comillas anterior con el comando de la posición "0"
                                                final String sb3 = ">" + proceso.getComando();
                                                intent.putExtra("DATA", sb3); //lanza el intentData con el mensaje de sb3
                                                LogUtils.d(TAG, "Se envia un broadcast ---- Linea:  144");
                                                service.sendBroadcast(intent); //aquí envía la información del intentData a la transmisión
                                                if (contadorComandos < 6) {
                                                    contadorComandos++;
                                                    LogUtils.d(TAG, "Deley para comandos AT");
                                                    Utils.sleep("Delay comando", 500);
                                                }
                                                comandoSiguiente = false;
                                                callback.enviarOBD(comando, banMessage = false);

                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) { //mensaje de error en caso de no funcionar el thread.
                            final String messageError = "MSG ERROR SERVICIO CHECK: " + ex;
                            clearAllProcesos();
                            Log.d("TEST", "Error en chekear cola de comandos");
                        }
                    }
                }
            });

            thread.start(); // Comienza el hilo.
        }
    }
}