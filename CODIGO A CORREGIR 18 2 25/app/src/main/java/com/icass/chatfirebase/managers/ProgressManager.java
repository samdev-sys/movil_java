package com.icass.chatfirebase.managers;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.icass.chatfirebase.R;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.NotificationsUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class ProgressManager {
    private static final String TAG = ProgressManager.class.getSimpleName();

    private static volatile ProgressManager instance;

    private int progreso = ProgresoDef.ESTADO_DESCONECTADO;

    @NonNull
    private final List<String> filters = new ArrayList<>();

    public static final String INIT = "INIT";
    public static final String CONEXION_OBD = "CONEXION_OBD";
    public static final String WRITE_LOG = "WRITE_LOG";
    public static final String MSG_OBD = "MSG_OBD";
    public static final String SUM_CONTADOR = "SUM_CONTADOR";
    public static final String REINICIAR = "REINICIAR";

    private ProgressManager() {
        this.filters.clear();
        this.filters.add(INIT);
        this.filters.add(CONEXION_OBD);
        this.filters.add(WRITE_LOG);
        this.filters.add(MSG_OBD);
        this.filters.add(SUM_CONTADOR);
        this.filters.add(REINICIAR);
    }

    public static ProgressManager getInstance() {
        if (instance == null) {
            synchronized (ProgressManager.class) {
                if (instance == null) {
                    instance = new ProgressManager();
                }
            }
        }

        return instance;
    }

    public void setProgreso(@ProgresoDef int progreso) {
        this.progreso = progreso;
        actualizarEstado();
        LogUtils.d(TAG,"Progreso: " + getEstado()+ " L65");
    }

    @ProgresoDef
    public int getEstadoActual() {
        return progreso;
    }

    public boolean isConectado() {
        return progreso == ProgresoDef.ESTADO_CONECTADO;
    }

    public boolean isDesconectado() {
        return progreso == ProgresoDef.ESTADO_DESCONECTADO;
    }

    public boolean isEncuestaInicial() {
        return progreso == ProgresoDef.ENCUESTA_INICIAL;
    }

    public boolean isEncuesta() {
        return progreso == ProgresoDef.ENCUESTA;
    }

    public boolean isEspera() {
        return progreso == ProgresoDef.ESTADO_ESPERA;
    }

    @NonNull
    public List<String> getFilters() {
        return filters;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            ProgresoDef.ESTADO_CONECTADO,
            ProgresoDef.ESTADO_DESCONECTADO,
            ProgresoDef.ENCUESTA_INICIAL,
            ProgresoDef.ENCUESTA,
            ProgresoDef.ESTADO_ESPERA
    })
    public @interface ProgresoDef {
        int ESTADO_CONECTADO = 1;
        int ESTADO_DESCONECTADO = 2;
        int ENCUESTA_INICIAL = 3;
        int ENCUESTA = 4;
        int ESTADO_ESPERA = 5;
    }

    public void actualizarEstado() {
        final Context context = ResourceManager.getInstance().getContext();
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        //notificationManager.notify(NOTIFICATION_ID, getNotification(context));
    }

    @NonNull
    public Notification getNotification(@NonNull Context context) {
        final String result = ProgressManager.getInstance().getEstado();

        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification);
        final String CHANNEL_ID = NotificationsUtils.createNotificationChannel();
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        notificationBuilder.setOngoing(true)
                .setContentTitle("Estado Conexión")
                .setContentText(result)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(bitmap)
                .setOnlyAlertOnce(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.setPriority(NotificationManager.IMPORTANCE_MIN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setCategory(Notification.CATEGORY_SERVICE);
        }

        return notificationBuilder.build();
    }

    @NonNull
    public String getEstado() {
        if (isConectado()) {
            return "Conectado";
        } else if (isDesconectado()) {
            return "Desconectado";
        } else if (isEncuestaInicial()) {
            return "Encuesta Inicial";
        } else if (isEncuesta()) {
            return "Encuesta";
        } else if (isEspera()) {
            return "En espera";
        }

        return "Desconocido";
    }
}