package com.icass.chatfirebase.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.icass.chatfirebase.R;
import com.icass.chatfirebase.managers.ConnectionManager;
import com.icass.chatfirebase.managers.ResourceManager;
import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.NotificationsUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StateNotification {
    private static volatile StateNotification instance;

    private static final int NOTIFICATION_ID = 9010;
    private static final int NOTIFICATION_SERVER_ID = 9011;

    @NonNull
    private String state = "";

    private StateNotification() {

    }

    public static StateNotification getInstance() {
        if(instance == null) {
            synchronized (StateNotification.class) {
                if(instance == null) {
                    instance = new StateNotification();
                }
            }
        }

        return instance;
    }

    public void notifyState() {
        if(Constants.PRUEBAS) {
            notifyState(state);
        }
    }

    public void notifyState(@NonNull String state) {
        if(Constants.PRUEBAS) {
            this.state = state;

            final boolean isEncendido = ConnectionManager.getInstance().isEncendido();
            final boolean isContacto = ConnectionManager.getInstance().isContacto();
            final boolean isApagado = ConnectionManager.getInstance().isApagado();

            if(isEncendido) {
                updateNotification(state);
            } else if(isContacto) {
                if(Constants.PRUEBAS) {
                    updateNotification(state);
                } else {
                    updateNotification("En pausa");
                }
            } else if(isApagado) {
                updateNotification("En pausa");
            }
        }
    }

    private void updateNotification(@NonNull String state) {
        if(Constants.PRUEBAS) {
            final Context context = ResourceManager.getInstance().getContext();
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification);
            final String CHANNEL_ID = NotificationsUtils.createNotificationChannel();
            final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);

            notificationBuilder
                    .setOngoing(true)
                    .setContentTitle("Comprobación de estado")
                    .setContentText(state)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setLargeIcon(bitmap)
                    .setOnlyAlertOnce(true);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationBuilder.setPriority(NotificationManager.IMPORTANCE_MIN);
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notificationBuilder.setCategory(Notification.CATEGORY_SERVICE);
            }

            final Notification notification = notificationBuilder.build();
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    public void notifyServer() {
        if(Constants.PRUEBAS) {
            final Context context = ResourceManager.getInstance().getContext();
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification);
            final String CHANNEL_ID = NotificationsUtils.createNotificationChannel();
            final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);

            notificationBuilder
                    .setContentTitle("Enviado a Firebase")
                    .setContentText("Último envío: " + getCurrentDateTimeFormatted())
                    .setSmallIcon(R.drawable.ic_notification)
                    .setLargeIcon(bitmap);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationBuilder.setPriority(NotificationManager.IMPORTANCE_MIN);
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notificationBuilder.setCategory(Notification.CATEGORY_SERVICE);
            }

            final Notification notification = notificationBuilder.build();
            notificationManager.notify(NOTIFICATION_SERVER_ID, notification);
        }
    }

    @NonNull
    private String getCurrentDateTimeFormatted() {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault());
        final long timestamp = System.currentTimeMillis();
        final Date date = new Date(timestamp);
        return simpleDateFormat.format(date);
    }
}