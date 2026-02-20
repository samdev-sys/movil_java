package com.icass.chatfirebase.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.icass.chatfirebase.managers.ResourceManager;

public class NotificationsUtils {
    public static final String CHANNEL_ID = "Monitoreo OBD Channel ID (1)";
    public static final String CHANNEL_NAME = "Monitoreo OBD Channel (1)";
    public static final String DESCRIPTION = "Monitoreo OBD";

    public static String createNotificationChannel() {
        final Context context = ResourceManager.getInstance().getContext();
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription(DESCRIPTION);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.GREEN);
            notificationChannel.enableVibration(false);
            notificationChannel.setVibrationPattern(new long[]{0L});
            notificationChannel.setShowBadge(true);
            notificationChannel.setSound(null, null);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        return CHANNEL_ID;
    }

    public static void playNotificationSound(@NonNull Context context, int resid) {
        final MediaPlayer mediaPlayer = MediaPlayer.create(context, resid);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.stop();
            }
        }, 3000);
    }
}