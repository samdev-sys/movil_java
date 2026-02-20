package com.icass.chatfirebase.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LogUtils {
    private static final String TAG1 = LogUtils.class.getSimpleName();
    public static String PATH;

    public static void initialize(Context context) {
        if (context != null && context.getExternalFilesDir(null) != null) {
            PATH = context.getExternalFilesDir(null).getAbsolutePath() + "/";
        } else {
            Log.e(TAG1, "Could not initialize LogUtils path, context is null or external files directory is not available.");
        }
    }

    public static void d(String TAG, String msj) {
        Log.d(TAG, msj);
        escribirArchivo(TAG, "Debug: " + msj);
    }

    public static void i(String TAG, String msj) {
        Log.i(TAG, msj);
        escribirArchivo(TAG, "Info: " + msj);
    }

    public static void e(String TAG, String msj) {
        Log.e(TAG, msj);
        escribirArchivo(TAG, "Error: " + msj);
    }

    public static void v(String TAG, String msj) {
        Log.v(TAG, msj);
        escribirArchivo(TAG, "Verbose: " + msj);
    }

    private static void escribirArchivo(String TAG, String msj) {
        if (PATH == null) {
            Log.e("LogUtils", "PATH is not initialized. Make sure to call LogUtils.initialize(context) first.");
            return;
        }

        String fileName = "Log.txt";
        try {
            File file = new File(PATH + fileName);
            File directory = new File(file.getParent());
            if (!directory.exists() && !directory.mkdirs()) {
                Log.e(TAG, "Could not create directory: " + directory.getAbsolutePath());
                return;
            }

            fileName = file.getAbsolutePath();
            FileWriter outputStream = new FileWriter(fileName, true);
            Date fecha = new Date(Calendar.getInstance().getTimeInMillis());
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
            outputStream.write("\nFecha: " + formatter.format(fecha) + " TAG: " + TAG + " -> MSG: " + msj);
            outputStream.close();

        } catch (Exception e) {
            Log.e("LogUtils", "Error writing to log file: " + e.toString());
        }
    }
}