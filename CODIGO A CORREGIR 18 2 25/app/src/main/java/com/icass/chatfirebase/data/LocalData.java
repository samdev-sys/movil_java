package com.icass.chatfirebase.data;

import android.content.Context;
import android.content.SharedPreferences.Editor;

import androidx.annotation.NonNull;

import com.icass.chatfirebase.config.ConfigApp;
import com.icass.chatfirebase.managers.ResourceManager;
import com.icass.chatfirebase.utils.Constants;

public class LocalData {
    private final String TAG_CODE = "Codigo";

    public void setCodigo(@NonNull String name, @NonNull String parametro) {
        final Context context = ResourceManager.getInstance().getContext();
        final Editor editor = context.getSharedPreferences(ConfigApp.localFileData, Context.MODE_PRIVATE).edit();
        editor.putString(name, parametro);
        editor.apply();
    }

    @NonNull
    public String getCodigo(@NonNull String name) {
        final Context context = ResourceManager.getInstance().getContext();
        return context.getSharedPreferences(ConfigApp.localFileData, Context.MODE_PRIVATE).getString(name, "");
    }

    public void setCommand(@NonNull String command) {
        final Context context = ResourceManager.getInstance().getContext();
        final Editor editor = context.getSharedPreferences(ConfigApp.localFileData, Context.MODE_PRIVATE).edit();
        editor.putString(Constants.TAG_ALERT, command);
        editor.apply();
    }

    @NonNull
    public String getCommand() {
        final Context context = ResourceManager.getInstance().getContext();
        return context.getSharedPreferences(ConfigApp.localFileData, Context.MODE_PRIVATE).getString(Constants.TAG_ALERT, "");
    }
}