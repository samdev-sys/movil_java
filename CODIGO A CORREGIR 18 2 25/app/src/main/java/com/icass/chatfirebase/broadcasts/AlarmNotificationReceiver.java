package com.icass.chatfirebase.broadcasts;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.icass.chatfirebase.R;
import com.icass.chatfirebase.activity.AlarmActivity;
import com.icass.chatfirebase.utils.LogUtils;

import java.text.DateFormat;
import java.util.Date;

public class AlarmNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = AlarmNotificationReceiver.class.getSimpleName();

    // Notification ID to allow for future updates
    private static final int MY_NOTIFICATION_ID = 1;

    // Notification Text Elements
    private final CharSequence tickerText = "Are You Playing Angry Birds Again!";
    private final CharSequence contentTitle = "A Kind Reminder";
    private final CharSequence contentText = "Get back to studying!!";

    // Notification Sound and Vibration on Arrival
    private final long[] mVibratePattern = {0, 200, 200, 300};

    @Override
    public void onReceive(Context context, Intent intent) {

        LogUtils.d(TAG, "Se llama el receiver en "+ TAG);

        // Notification Action Elements
        final Intent mNotificationIntent = new Intent(context, AlarmActivity.class);
        final PendingIntent mContentIntent = PendingIntent.getActivity(context, 0, mNotificationIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        final Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.alert);

        final Notification.Builder notificationBuilder = new Notification.Builder(
                context).setTicker(tickerText)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setAutoCancel(true)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(mContentIntent)
                .setSound(soundUri)
                .setVibrate(mVibratePattern);

        // Pass the Notification to the NotificationManager:
        final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(MY_NOTIFICATION_ID, notificationBuilder.build());

        LogUtils.d(TAG, "Sending notification at:" + DateFormat.getDateTimeInstance().format(new Date()));
    }
}