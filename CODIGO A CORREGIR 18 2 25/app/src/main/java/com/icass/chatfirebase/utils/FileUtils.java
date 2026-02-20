package com.icass.chatfirebase.utils;

import java.io.File;
import java.io.FileOutputStream;

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();
    public static boolean fileExists(String path) {
        boolean exists = false;
        File file = new File(LogUtils.PATH + path);
        if (file.exists()) {
            exists = true;
        }
        return exists;
    }

    public static boolean createDirectory(String name) {
        boolean b = false;
        String path = LogUtils.PATH + name;
        File docum = new File(path);
        if (!docum.exists() && !docum.isDirectory()) {
            b = docum.mkdir();
        } else {
            LogUtils.e(TAG, "Carpeta ya existe  " + path);
        }
        return b;
    }

    public static boolean writeInFile(String file, String chain) {
        boolean success;
        try {
            file = LogUtils.PATH + file;
            FileOutputStream outputStream = new FileOutputStream(new File(file));
            outputStream.write(chain.getBytes());
            outputStream.close();
            success = true;
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage());
            success = false;
        }
        return success;
    }
}
