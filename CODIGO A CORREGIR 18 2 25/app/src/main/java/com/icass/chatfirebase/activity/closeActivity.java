package com.icass.chatfirebase.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.icass.chatfirebase.R;
import com.icass.chatfirebase.data.LocalData;

public class closeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_close);
        Log.d("TEST", "onCreate closeActivty");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("TEST", "onResume closeActivty");
        String packageName = "com.icass.chatfirebase";
        ActivityManager activityManager = (ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.killBackgroundProcesses(packageName);
        finishAffinity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("TEST", "onDestroycloseActivty");
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}