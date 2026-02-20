package com.icass.chatfirebase.StartVelocida;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.LogUtils;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class StartVel extends JobService {
    private final static String TAG = "StartVel";

    @Override
    public boolean onStartJob(JobParameters params) {
        final SharedPreferences sharedPreferences = getSharedPreferences(Constants.TAG_TYPE_USER, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();  //Constante de editor
        editor.putString("Comando", "1"); // se guarda admin en user
        editor.apply();

        LogUtils.d(TAG,"onStartJobStart Vel");

        final Intent intent = new Intent(this, ServicesGPS.class);
        startService(intent);

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}