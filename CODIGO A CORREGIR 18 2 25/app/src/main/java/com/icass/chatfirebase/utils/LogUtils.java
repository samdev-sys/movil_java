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

    private static void escribirArchivo(String TAG, String texto) {
        if (PATH == null) {
            Log.e(TAG1, "LogUtils path not initialized. Cannot write to file.");
            return;
        }
        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());
            final String fileName = "log_monitoreo_" + sdf.format(new Date()) + ".txt";
            final File file = new File(PATH, fileName);

            final SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            final String time = sdf2.format(Calendar.getInstance().getTime());

            final FileWriter writer = new FileWriter(file, true);
            writer.append(time).append("  ").append(TAG).append("  ").append(texto).append("\n");
            writer.flush();
            writer.close();
        } catch (Exception e) {
            Log.e("ERROR", "CATCH: " + e);
        }
    }
}
