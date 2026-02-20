package com.icass.chatfirebase.retrofit.connection;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.icass.chatfirebase.R;
import com.icass.chatfirebase.managers.NetworkManager;
import com.icass.chatfirebase.managers.ResourceManager;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.NotificationsUtils;

import java.util.logging.Logger;

public class NetworkNotification {
    private final String TAG = NetworkNotification.class.getSimpleName();
    private static volatile NetworkNotification instance;

    private static final int NOTIFICATION_ID = 9009;

    private NetworkNotification() {

    }

    public static NetworkNotification getInstance() {
        if(instance == null) {
            synchronized (NetworkNotification.class) {
                if(instance == null) {
                    instance = new NetworkNotification();
                }
            }
        }

        return instance;
    }

    public void networkNotification() {
        final Context context = ResourceManager.getInstance().getContext();
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        final boolean networkConnected = NetworkManager.getInstance().isNetworkConnected();

        if(networkConnected) {
            LogUtils.d(TAG,"Red conectada");
//            notificationManager.cancel(NOTIFICATION_ID);
        } else {
            LogUtils.d(TAG,"Red sin conexion");
//            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification);
//            final String networkStatus = "Sin conexión a internet";
//            final String CHANNEL_ID = NotificationsUtils.createNotificationChannel();
//            final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
//
//            notificationBuilder.setOngoing(true)
//                    .setContentTitle("OBD WEB")
//                    .setContentText(networkStatus)
//                    .setSmallIcon(R.drawable.ic_notification)
//                    .setLargeIcon(bitmap)
//                    .setOnlyAlertOnce(true);
//
//            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                notificationBuilder.setPriority(NotificationManager.IMPORTANCE_MIN);
//            }
//
//            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                notificationBuilder.setCategory(Notification.CATEGORY_SERVICE);
//            }
//
//            final Notification notification = notificationBuilder.build();
//            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }
}