package com.icass.chatfirebase;

import android.util.Log;

import androidx.multidex.MultiDexApplication;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.icass.chatfirebase.managers.ConnectionManager;
import com.icass.chatfirebase.managers.ResourceManager;
import com.icass.chatfirebase.repositories.NetworkRepository;
import com.icass.chatfirebase.utils.FileUtils;
import com.icass.chatfirebase.utils.LogUtils;

public class MonitorOBDApplication extends MultiDexApplication {

    private final String TAG = MonitorOBDApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        crearDirectorioLog();
        LogUtils.d(TAG, "--------Se instancia la aplicacion------------------");

        FirebaseApp.initializeApp(this);
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);

        ResourceManager.getInstance().initialize(this);
        ConnectionManager.getInstance().initialize();

        NetworkRepository.getInstance().registerNetwork();
        DataServer.getInstance().limpiarAlarma();
        }

    public void crearDirectorioLog(){
        String pathDocuments = "/Documento/";
        LogUtils.PATH = getBaseContext().getApplicationInfo().dataDir + pathDocuments;
        if (!FileUtils.fileExists("")) {
            Log.e("OBD", FileUtils.createDirectory("") + " ");
        }
    }
}