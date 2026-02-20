package com.icass.chatfirebase;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.icass.chatfirebase.broadcasts.AlarmLoggerReceiver;
import com.icass.chatfirebase.data.LocalData;
import com.icass.chatfirebase.interfaces.CallbackService;
import com.icass.chatfirebase.managers.ConnectionManager;
import com.icass.chatfirebase.managers.ResourceManager;
import com.icass.chatfirebase.services.CheckearColaComandos;
import com.icass.chatfirebase.services.CheckearColaComandos.EventCheckProcesos;
import com.icass.chatfirebase.services.Procesos;
import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.DateUtils;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.Utils;
import com.icass.chatfirebase.utils.Utils.TipoEnvioDef;

import java.util.ArrayList;
import java.util.List;

public class DataServer {
    private static final String TAG = DataServer.class.getSimpleName();
    private static volatile DataServer instance;

    private DataServer() {
    }

    public static DataServer initInstance() {
        instance = null;

        return getInstance();
    }

    public static DataServer getInstance() {
        if (instance == null) {
            synchronized (DataServer.class) {
                if (instance == null) {
                    instance = new DataServer();
                }
            }
        }

        return instance;
    }

    public boolean banHiloIniciado = false;
    public boolean banHiloSiempre;
    public boolean banInit = false;
    private Thread hiloEncuesta; // Thread es un único flujo de control dentro de un programa.
    public CheckearColaComandos servicesCheckProcesos; //se crea una variable con la clase "CheckearColaComandos" y utilizarlo para crear instancias.
    private final List<String> listaEnvios = new ArrayList<>();
    private boolean primerEnvio = true;
    private boolean primeraEncuesta = true;
    private boolean reiniciar = false;
    private boolean alarmaActivada = true;
    final Context context = ResourceManager.getInstance().getContext();
    final Intent loggerIntentReceiver = new Intent(context, AlarmLoggerReceiver.class);

    private AlarmManager alarmManager;
    private CallbackService listener;
    private PendingIntent pendingIntent;

    public void initialize(Service service, EventCheckProcesos evenProcesos, @NonNull CallbackService listener) { //recibe los parámetros envíados de la clase "ConexionBluetooth"
        LogUtils.d(TAG, "Se crea la instancia de la clase " + TAG);
        final Context context = ResourceManager.getInstance().getContext();
        //Constants.ID_VEHICULO = Constants.ID_VEHICULO //devuelve el contexto vinculado a "mService"
        this.banHiloSiempre = true;
        this.servicesCheckProcesos = new CheckearColaComandos(service, evenProcesos); //manda llamar el método "CheckearColaComandos" con sus respectivos parámetros.
        this.servicesCheckProcesos.iniciarServicio(); //manda llamar el método "iniciarServicio"
        recibirDataServidor();//invoca el método "recibirDataServidor" de está clase.
        this.listener = listener;
        this.alarmManager = null;
    }

    public void initConfiguracion(@NonNull String origen) {
        Log.d(TAG, "Se inicia la configuracion del OBD, desde " + origen);
//        ProgressManager.getInstance().setProgreso(ProgresoDef.ENCUESTA_INICIAL);

        //this.servicesCheckProcesos.banEnviar = false;
        this.servicesCheckProcesos.clearAllProcesos(); // Limpia el ArrayList de la clase "CheckearColaComandos"
        this.servicesCheckProcesos.contadorComandos = 0;
        // Reset (reiniciar)
        //final Procesos validID = new Procesos("AT@2");
        //this.servicesCheckProcesos.pushProceso(validID);

        final Procesos proceso1 = new Procesos("ATZ");
        this.servicesCheckProcesos.pushProceso(proceso1);


        final Procesos proceso2 = new Procesos(Constants.comandoATSP);
//        final Procesos proceso3 = new Procesos("ATSP0");
        this.servicesCheckProcesos.pushProceso(proceso2);

        final Procesos proceso3 = new Procesos("ATE0");
        this.servicesCheckProcesos.pushProceso(proceso3);


        final Procesos proceso4 = new Procesos("ATS0");
        this.servicesCheckProcesos.pushProceso(proceso4);

        final Procesos proceso5 = new Procesos("ATL1");
        this.servicesCheckProcesos.pushProceso(proceso5);

        final Procesos proceso6 = new Procesos("01" + Constants.comandoEncendido);
        this.servicesCheckProcesos.pushProceso(proceso6);

        final Procesos proceso7 = new Procesos("01" + Constants.comandoEncendido1);
        this.servicesCheckProcesos.pushProceso(proceso7);

        LogUtils.d(TAG, "Cantidad de comandos despues de agregar los de configuracion : " + servicesCheckProcesos.getColaProcesos().size());
//        LogUtils.d(TAG, "Valor de banEnviar : " + CheckearColaComandos.banEnviar);
//        LogUtils.d(TAG, "Valor de comandoSiguiente : " + servicesCheckProcesos.comandoSiguiente);
//        LogUtils.d(TAG, "Valor de banMessage : " + servicesCheckProcesos.banMessage);

        LogUtils.d(TAG, "Se va a instanciar el metodo supervisar de connection manager en " + TAG + "L124");
        crearAlarma();

//        ConnectionManager.getInstance().banderaInicio = 0;
        ConnectionManager.getInstance().supervisar(1, new ConnectionManager.EstadoVehiculo() {//Se instancia la clase Supervisar
            @Override
            public void reportar() {
                LogUtils.d(TAG, "Se ejecuta el metodo reportar");
                if (listener != null) {
                    listener.callback();
                }

                final boolean statusConfiguracion = ConnectionManager.getInstance().statusConfiguracion();
                final boolean estadoAuto = ConnectionManager.getInstance().getEstado().equals("ENCENDIDO");
                final int size = servicesCheckProcesos.getColaProcesos().size();
                LogUtils.d(TAG, "Estado del auto " + estadoAuto + " statusconfiguracion " + statusConfiguracion);
                if (statusConfiguracion && estadoAuto) {
                    LogUtils.d(TAG, "Auto encendido y configuracion correcta, Se llama al metodo initEncuesta L138");
                    initEncuesta(); // Manda llamar el método encuesta
                } else {
                    LogUtils.d(TAG, "Se llama a initConfiguracion desde el Else L145");
//                    initConfiguracion("OBD no configurado o auto no esta encendido");
                    restartInitCommands(size);
                }
            }
        });
    }

    public void setInitComandosWeb() {
        servicesCheckProcesos.banEnviar = false;
        this.servicesCheckProcesos.clearAllProcesos();
        this.servicesCheckProcesos.contadorComandos = 0;

        // (false, "", false, false);
        // Reset (reiniciar)
        final Procesos proceso1 = new Procesos("ATZ");
        this.servicesCheckProcesos.pushProceso(proceso1);

        //        final Procesos proceso3 = new Procesos("ATSP0");
        final Procesos proceso2 = new Procesos(Constants.comandoATSP);
        this.servicesCheckProcesos.pushProceso(proceso2);


        final Procesos proceso3 = new Procesos("ATE0");
        this.servicesCheckProcesos.pushProceso(proceso3);



        final Procesos proceso4 = new Procesos("ATS0");
        this.servicesCheckProcesos.pushProceso(proceso4);

        final Procesos proceso5 = new Procesos("ATL1");
        this.servicesCheckProcesos.pushProceso(proceso5);

        final Procesos proceso6 = new Procesos("01" + Constants.comandoEncendido); // ver el estado
        this.servicesCheckProcesos.pushProceso(proceso6);

        final Procesos proceso7 = new Procesos("01" + Constants.comandoEncendido1); // ver el estado
        this.servicesCheckProcesos.pushProceso(proceso7);
        //pro Es un objeto para la clase "Procesos" para instanciar está clase, comparte todos tus atributos y comportamiento.
        final Procesos proceso8 = new Procesos(false, "03", false, true);
        DataServer.this.servicesCheckProcesos.pushProceso(proceso8);
    }

    public void cerrarHilo() {
        if (alarmManager == null) {
            final Context context = ResourceManager.getInstance().getContext();
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }

        try {
            if (this.hiloEncuesta != null) {
                this.hiloEncuesta.interrupt();
            }
        } catch (Exception ex) {
            LogUtils.e(TAG, "CerrarHilo" + ex);
        }

        this.banHiloSiempre = false;
        this.hiloEncuesta = null;
        CheckearColaComandos.strConcat = "";
        this.servicesCheckProcesos.cerrarHilo();
    }

    public void agregarEncuesta() {
        Log.d("TEST", "Si entra al metodo de agregar encuesta");
        final Procesos proceso = new Procesos(false, "03", false, true);
        servicesCheckProcesos.pushProceso(proceso);
        LogUtils.d(TAG, "Se agrega la lista de comandos de encuesta");
        setSecuenciasComandos();
    }

    public void initEncuesta() {
//        LogUtils.d(TAG, "Variable hilo encuesta = " + hiloEncuesta);
        Log.d(TAG, "tamaño de la cola de procesos: " + servicesCheckProcesos.getColaProcesos().size());
        if (servicesCheckProcesos.getColaProcesos().size() < 8) {
            LogUtils.d(TAG, "Se agrega encuesta cuando la cola es menor a 7");
            agregarEncuesta();
        }


//        if (this.hiloEncuesta == null) { //si "hiloEncuesta" es = a null
//            this.hiloEncuesta = new Thread(new Runnable() {
//                public void run() { //se crea un nuevo hilo
//                    final int FLOW = 1;
//                    if (FLOW == 1) {
//                        LogUtils.d(TAG, "Se agrega encuesta por primera vez L222");
//                        agregarEncuesta();//
//
//                        if (reiniciar) {
//                            reiniciar = false;
//                            cancelarAlarma();
//                        }
//                    }
//                }
//            });
//
//            this.hiloEncuesta.start();
//        } else if (servicesCheckProcesos.getColaProcesos().size() < 7 && ConnectionManager.getInstance().contador > 4 ) {
//            LogUtils.d(TAG, "Se agrega encuesta cuando la cola es menor a 7 y el contador es igual a 5 ");
//            agregarEncuesta();
//        }
    }

    // Manda la secuencia de comandos de acuerdo al proceso "initEncuesta"
    public void setSecuenciasComandos() {
        final Procesos proceso = new Procesos(false, "ATE0", false, false);
        final Procesos procesoEncendido = new Procesos("01" + Constants.comandoEncendido);
        final Procesos procesoEncendido1 = new Procesos("01" + Constants.comandoEncendido1);

        final Procesos pro1 = new Procesos(false, "0105", true, true);
        this.servicesCheckProcesos.pushProceso(pro1);

        final Procesos pro2 = new Procesos(false, "0106", true, true);
        this.servicesCheckProcesos.pushProceso(pro2);

        final Procesos pro3 = new Procesos(false, "0107", true, true);
        this.servicesCheckProcesos.pushProceso(pro3);

        final Procesos pro4 = new Procesos(false, "0108", true, true);
        this.servicesCheckProcesos.pushProceso(pro4);

        final Procesos pro5 = new Procesos(false, "0109", true, true);
        this.servicesCheckProcesos.pushProceso(pro5);

        final Procesos pro6 = new Procesos(false, "010B", true, true);
        this.servicesCheckProcesos.pushProceso(pro6);

        final Procesos pro7 = new Procesos(false, "010C", true, true);
        this.servicesCheckProcesos.pushProceso(pro7);

        final Procesos pro8 = new Procesos(false, "0110", true, true);
        this.servicesCheckProcesos.pushProceso(pro8);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro9 = new Procesos(false, "0114", true, true);
        this.servicesCheckProcesos.pushProceso(pro9);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro10 = new Procesos(false, "0115", true, true);
        this.servicesCheckProcesos.pushProceso(pro10);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro11 = new Procesos(false, "0116", true, true);
        this.servicesCheckProcesos.pushProceso(pro11);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro12 = new Procesos(false, "0117", true, true);
        this.servicesCheckProcesos.pushProceso(pro12);

        this.servicesCheckProcesos.pushProceso(proceso);

        this.servicesCheckProcesos.pushProceso(procesoEncendido);
        this.servicesCheckProcesos.pushProceso(procesoEncendido1);

        final Procesos pro13 = new Procesos(false, "0118", true, true);
        this.servicesCheckProcesos.pushProceso(pro13);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro14 = new Procesos(false, "0119", true, true);
        this.servicesCheckProcesos.pushProceso(pro14);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro15 = new Procesos(false, "011A", true, true);
        this.servicesCheckProcesos.pushProceso(pro15);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro16 = new Procesos(false, "011B", true, true);
        this.servicesCheckProcesos.pushProceso(pro16);

        this.servicesCheckProcesos.pushProceso(proceso);

        this.servicesCheckProcesos.pushProceso(procesoEncendido);
        this.servicesCheckProcesos.pushProceso(procesoEncendido1);

        final Procesos pro17 = new Procesos(false, "0124", true, true);
        this.servicesCheckProcesos.pushProceso(pro17);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro18 = new Procesos(false, "0125", true, true);
        this.servicesCheckProcesos.pushProceso(pro18);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro19 = new Procesos(false, "0126", true, true);
        this.servicesCheckProcesos.pushProceso(pro19);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro20 = new Procesos(false, "0127", true, true);
        this.servicesCheckProcesos.pushProceso(pro20);

        this.servicesCheckProcesos.pushProceso(proceso);

        this.servicesCheckProcesos.pushProceso(procesoEncendido);
        this.servicesCheckProcesos.pushProceso(procesoEncendido1);

        final Procesos pro21 = new Procesos(false, "0128", true, true);
        this.servicesCheckProcesos.pushProceso(pro21);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro22 = new Procesos(false, "0129", true, true);
        this.servicesCheckProcesos.pushProceso(pro22);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro23 = new Procesos(false, "012A", true, true);
        this.servicesCheckProcesos.pushProceso(pro23);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro24 = new Procesos(false, "012B", true, true);
        this.servicesCheckProcesos.pushProceso(pro24);

        this.servicesCheckProcesos.pushProceso(proceso);

        this.servicesCheckProcesos.pushProceso(procesoEncendido);
        this.servicesCheckProcesos.pushProceso(procesoEncendido1);

        final Procesos pro25 = new Procesos(false, "0134", true, true);
        this.servicesCheckProcesos.pushProceso(pro25);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro26 = new Procesos(false, "0135", true, true);
        this.servicesCheckProcesos.pushProceso(pro26);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro27 = new Procesos(false, "0136", true, true);
        this.servicesCheckProcesos.pushProceso(pro27);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro28 = new Procesos(false, "0137", true, true);
        this.servicesCheckProcesos.pushProceso(pro28);

        this.servicesCheckProcesos.pushProceso(proceso);

        this.servicesCheckProcesos.pushProceso(procesoEncendido);
        this.servicesCheckProcesos.pushProceso(procesoEncendido1);

        final Procesos pro29 = new Procesos(false, "0138", true, true);
        this.servicesCheckProcesos.pushProceso(pro29);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro30 = new Procesos(false, "0139", true, true);
        this.servicesCheckProcesos.pushProceso(pro30);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro31 = new Procesos(false, "013A", true, true);
        this.servicesCheckProcesos.pushProceso(pro31);

        this.servicesCheckProcesos.pushProceso(proceso);

        final Procesos pro32 = new Procesos(false, "013B", true, true);
        this.servicesCheckProcesos.pushProceso(pro32);

        final Procesos pro33 = new Procesos(false, "0131", true, true);
        this.servicesCheckProcesos.pushProceso(pro33);

        this.servicesCheckProcesos.pushProceso(procesoEncendido);
        this.servicesCheckProcesos.pushProceso(procesoEncendido1);

        final Procesos pro34 = new Procesos(false, "0142", true, true);
        this.servicesCheckProcesos.pushProceso(pro34);

        final Procesos pro35 = new Procesos(false, "0145", true, true);
        this.servicesCheckProcesos.pushProceso(pro35);

        final Procesos pro36 = new Procesos(false, "011F", true, true);
        this.servicesCheckProcesos.pushProceso(pro36);

        final Procesos pro37 = new Procesos(false, "010D", true, true);
        this.servicesCheckProcesos.pushProceso(pro37);

        final Procesos pro38 = new Procesos(false, "012F", true, true);
        this.servicesCheckProcesos.pushProceso(pro38);

        final Procesos pro39 = new Procesos(false, "01A6", true, true);
        this.servicesCheckProcesos.pushProceso(pro39);

        final Procesos pro40 = new Procesos(true, "0104", true, true);
        this.servicesCheckProcesos.pushProceso(pro40);


        /*
        // Para agregar un proceso de prueba en la encuesta principal es importante indicar el parámetro "banWeb" del ultimo comando como true
        Ya que este indica que es el ultimo comando para enviar a firebase, los anteriores deben estar en false o se enviará por separado.
        final Procesos pro38 = new Procesos(true, "01A6", true, true);
        this.servicesCheckProcesos.pushProceso(pro38);
        */

        this.servicesCheckProcesos.pushProceso(procesoEncendido);
        this.servicesCheckProcesos.pushProceso(procesoEncendido1);
    }

    public void initHiloEsperaLectura() {
        this.banHiloIniciado = true;
        new Thread(new Runnable() {//TODO validar si es necesario un hilo para cambiar una variable
            public void run() {
                Utils.sleep("6", 2000);//TODO Se inicia este hilo al cambiar la variable banInit
                DataServer.this.banInit = true;
            }
        }).start();
    }


    public void agregarEnvioPendiente(@NonNull String msg) {
        LogUtils.d(TAG, "Envío agregado: " + msg);
        listaEnvios.add(msg);
    }

    public void setPrimerEnvio(boolean primerEnvio) {
        this.primerEnvio = primerEnvio;
    }

    public boolean isPrimerEnvio() {
        return primerEnvio;
    }

    public void setPrimeraEncuesta(boolean primeraEncuesta) {
        this.primeraEncuesta = primeraEncuesta;
    }

    public boolean isPrimeraEncuesta() {
        return primeraEncuesta;
    }

    public void enviarEnviosPendientes() {//Envia los envios pendientes,solo se manda el primero que no se pudo mandar
        if (listaEnvios.isEmpty()) {
            return;
        }

        LogUtils.d(TAG, "Tamaño listaEnvios: " + listaEnvios.size());
        final String msg = listaEnvios.get(0);
        LogUtils.d(TAG, "Envio pendiente: " + msg);
        enviarMsgAlServer(msg, TipoEnvioDef.DIAGNOSTICO);

        listaEnvios.clear();
    }

    public void enviarMsgAlServer(@NonNull String msg, int tipoEnvio) { //Aqui se envia la cadena a firebase
        LogUtils.d(TAG, "Valor del tipo de envio dentro de enviarMSGALServer: -> " + tipoEnvio);
        if (msg.startsWith("%%NO DATA")) {
            LogUtils.d(TAG, "Mensaje no enviado, incorrecto");
            return;
        }

        final boolean encendido = ConnectionManager.getInstance().isEncendido();
        final boolean contacto = ConnectionManager.getInstance().isContacto();
        String child = Constants.ID_VEHICULO;

        if (tipoEnvio == TipoEnvioDef.DIAGNOSTICO) {
            child += "D";
            LogUtils.d(TAG, "TipoEnvio: DIAGNOSTICO");

            if (encendido) {
                LogUtils.d(TAG, "ENVIAR A WEB -> COCHE ENCENDIDO");
            } else {
                if (Constants.PRUEBAS) {
                    if (contacto) {
                        LogUtils.d(TAG, "ENVIAR A WEB -> DIAGNOSTICO (PRUEBAS[1]) -> COCHE EN CONTACTO");
                    }
                } else {
                    LogUtils.d(TAG, "NO ENVIAR A WEB -> COCHE APAGADO O EN CONTACTO (1)");
                    return;
                }
            }
        } else if (tipoEnvio == TipoEnvioDef.COMANDO) {
            child += "C";
            LogUtils.d(TAG, "TipoEnvio: COMANDO");
            if (encendido || contacto) {
                LogUtils.d(TAG, "ENVIAR A WEB -> COCHE ENCENDIDO O EN CONTACTO");
            } else {
                LogUtils.d(TAG, "NO ENVIAR A WEB -> COCHE APAGADO");
                return;
            }
        } else if (tipoEnvio == TipoEnvioDef.ESTADO) {
            child += "E";
            LogUtils.d(TAG,"TipoEnvio: Estado");
            LogUtils.d(TAG,"El estado del auto es " + msg);
        } else if (tipoEnvio == TipoEnvioDef.COMANDO_4300) { // Comando 4300
            LogUtils.d("TEST", "Respuesta de comando 03 " + msg);
            child += "F";
        } else if (tipoEnvio == TipoEnvioDef.UBICA) {
            child += "U";
        } else {
            LogUtils.d(TAG, "Caso desconocido");
            return;
        }

        try {
            // Concatena la variable "msg", con lo que tenga strConcat y el id del vehiculo.

            String messageVehiculo = msg + Utils.strConcat + Constants.ID_VEHICULO;
            if (Constants.ID_VEHICULO.contains("111")) {
                messageVehiculo = DateUtils.getDateTimeFormatted() + " - " + messageVehiculo;
            }
            final String messageWeb = "ENVIAR A WEB: " + messageVehiculo;
            LogUtils.d(TAG, messageWeb);
            LogUtils.d(TAG, "-------Envio de cadena a firebase---------------");

            final ChatMessage chatMessage = new ChatMessage(messageVehiculo, "OBD");
            final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
            final DatabaseReference databaseReference = firebaseDatabase.getReference()
                    .child("VEHÍCULOS")
                    .child(child)
                    .child("mensajes");

            databaseReference.push().setValue(chatMessage).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    LogUtils.d(TAG, "La cadena se envio correctamente");//Aqui es donde se envia la cadena y si se envio correcta manda este mensaje
                    //Aqui hay que generar la alarma dentro de 3 minutos.
                    ConnectionManager.getInstance().contador = 0;
                    listaEnvios.clear();
                    //crearAlarma();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception ex) {
                    LogUtils.e(TAG, "Envio onFailure: " + ex);
                }
            }).addOnCanceledListener(new OnCanceledListener() {
                @Override
                public void onCanceled() {
                    LogUtils.d(TAG, "Envio onCanceled");
                }
            });
        } catch (Exception ex) {
            String messageError = "Error: " + ex;
            LogUtils.e(TAG, messageError);
        }
    }

    // Recibe los datos del servidor para almacenarlos en Firebase
    public void recibirDataServidor() {
        final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference databaseReference = firebaseDatabase.getReference()
                .child("VEHÍCULOS")
                .child(Constants.ID_VEHICULO)
                .child("mensajes");

        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (!DataServer.this.banHiloIniciado) {
                    DataServer.this.initHiloEsperaLectura(); // Invoca el método "initHiloEsperLectura" de está clase.
                }

                final ChatMessage msg = dataSnapshot.getValue(ChatMessage.class);

                if (msg != null && DataServer.this.banInit && msg.getMessageUser().toLowerCase().contains("comando")) {
                    LogUtils.d(TAG, "Coamndo valido desde Firebase");
                    LogUtils.d(TAG, "MSG WEB TO OBD: " + msg.getMessageText());

                    //Se agregar el comando recibido del servidor a la cola de procesos
                    final Procesos pro = new Procesos(true, msg.getMessageText(), false, true);
                    DataServer.this.servicesCheckProcesos.pushProceso(pro);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void restartInitCommands(int colaComandos) {//TODO validar si es necesario llamar este metodo
        if (colaComandos != 0) {
            LogUtils.d(TAG, "Cola de comandos con valores");
            Utils.sleep(TAG, 10000);
//            return;
        }

        LogUtils.d(TAG, "Se llama init configuracion de restarInitComandos l 706");
        initConfiguracion("Encuesta inválida");
    }

    public void reiniciar() {
        this.reiniciar = true;
    }

    public void crearAlarma() {
        Log.d("TEST", "Metodo Crear Alarma para lanzar Supervisar " + TAG);
        Log.d("TEST", "Valor de alarm Manager " + alarmManager);
        if (alarmManager == null) {
            LogUtils.d(TAG, "Se genera la alarma debe activarse en 3 minutos");
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            pendingIntent = PendingIntent.getBroadcast(context, 7235, loggerIntentReceiver, PendingIntent.FLAG_NO_CREATE);
            LogUtils.d(TAG, "Valor de pendingItent " + pendingIntent);
            if (pendingIntent == null) {
                LogUtils.d(TAG, "PendingIntent Null, se genera la alarma para cada 3 minutos");
                pendingIntent = PendingIntent.getBroadcast(context, 7235, loggerIntentReceiver, 0);
            }
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), Constants.INTERVAL_THREE_MINUTES, pendingIntent);
        }
    }


    public void limpiarAlarma() {
        pendingIntent = PendingIntent.getBroadcast(context, 7235, loggerIntentReceiver, PendingIntent.FLAG_NO_CREATE);
        LogUtils.d(TAG, "Valor de alarmManager = " + alarmManager + " valor de pendingItent " + pendingIntent + " Metodo limpiar");
        if (pendingIntent != null) {
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            LogUtils.d(TAG, "Se destruye la alarma");
            alarmManager.cancel(pendingIntent);
            pendingIntent = PendingIntent.getBroadcast(context, 7235, loggerIntentReceiver, PendingIntent.FLAG_NO_CREATE);
            alarmManager = null;
            LogUtils.d(TAG, "Valor de alarmManager = " + alarmManager + " valor de pendingItent " + pendingIntent + " Despues de cancelarAlarma");
        }
    }
}