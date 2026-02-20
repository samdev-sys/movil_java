package com.icass.chatfirebase.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.icass.chatfirebase.R;
import com.icass.chatfirebase.broadcasts.AlarmLoggerReceiver;
import com.icass.chatfirebase.broadcasts.AlarmNotificationReceiver;
import com.icass.chatfirebase.utils.Constants;

public class AlarmActivity extends AppCompatActivity {
    private AlarmManager mAlarmManager;
    private PendingIntent mNotificationReceiverPendingIntent, mLoggerReceiverPendingIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alarm);

        // 1.  Get the AlarmManager Service
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // 2.  Create PendingIntent to start the AlarmNotificationReceiver
        Intent mNotificationReceiverIntent = new Intent(AlarmActivity.this, AlarmNotificationReceiver.class);
        mNotificationReceiverPendingIntent = PendingIntent.getBroadcast(AlarmActivity.this, 0, mNotificationReceiverIntent, 0);

        // Create PendingIntent to start the AlarmLoggerReceiver
        Intent mLoggerReceiverIntent = new Intent(AlarmActivity.this, AlarmLoggerReceiver.class);
        mLoggerReceiverPendingIntent = PendingIntent.getBroadcast(AlarmActivity.this, 0, mLoggerReceiverIntent, 0);

        // Repeating Alarm Button
        final Button repeatingAlarmButton = (Button) findViewById(R.id.repeating_alarm_button);
        repeatingAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), Constants.INTERVAL_ONE_MINUTE, mNotificationReceiverPendingIntent);

                mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), Constants.INTERVAL_ONE_MINUTE, mLoggerReceiverPendingIntent);

                Toast.makeText(getApplicationContext(), "Repeating Alarm Set", Toast.LENGTH_LONG).show();
            }
        });

        // Cancel Repeating Alarm Button
        final Button cancelRepeatingAlarmButton = (Button) findViewById(R.id.cancel_repeating_alarm_button);
        cancelRepeatingAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlarmManager.cancel(mNotificationReceiverPendingIntent);
                mAlarmManager.cancel(mLoggerReceiverPendingIntent);

                Toast.makeText(getApplicationContext(), "Repeating Alarms Cancelled", Toast.LENGTH_LONG).show();
            }
        });
    }
}