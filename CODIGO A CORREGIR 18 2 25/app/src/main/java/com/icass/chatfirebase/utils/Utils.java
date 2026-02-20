package com.icass.chatfirebase.utils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    public static String strConcat = "@";

    public enum StateBluetooth { //estado de conexión del bluetooth
        NO_DISPONIBLE,
        NO_HABILITADO,
        CONECTADO,
        DESCONECTADO,
        NO_ENCONTRADO,
        RECONECTANDO
    }

    public static void sleep(@NonNull String Tag, long millis) {
        if(!Tag.equals("1")) {
            LogUtils.d(TAG,"Sleep: " + Tag);
        }

        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            LogUtils.e(TAG,"Error::" + Tag);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            TipoEnvioDef.DIAGNOSTICO,
            TipoEnvioDef.COMANDO,
            TipoEnvioDef.COMANDO_4300,
            TipoEnvioDef.ESTADO,
            TipoEnvioDef.UBICA
    })
    public @interface TipoEnvioDef {
        int DIAGNOSTICO = 0;
        int COMANDO = 1;
        int COMANDO_4300 = 2;
        int ESTADO = 3;
        int UBICA=4;
    }
}