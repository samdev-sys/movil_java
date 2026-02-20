package com.icass.chatfirebase.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.icass.chatfirebase.DataServer;
import com.icass.chatfirebase.services.Procesos;
import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.LogUtils;

public class alarmEstadoVehiculo extends BroadcastReceiver {

    private static final String TAG = alarmEstadoVehiculo.class.getName();
    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtils.d(TAG, "------------------------------Se lanza la alarma para validar estado del vehiculo-----------------------------------");
        LogUtils.d(TAG, "Cantidad de coamndos actuales en cola antes de agregar los de validación: " + DataServer.getInstance().servicesCheckProcesos.sizeProcesos());
        DataServer.getInstance().servicesCheckProcesos.contadorComandos = 0;

        final Procesos proceso1 = new Procesos("ATZ");
        DataServer.getInstance().servicesCheckProcesos.pushProceso(proceso1);//Se utiliza getInstance para asegurar que estemos trabajndo sobre la misma clase y cola de procesos

        final Procesos proceso2 = new Procesos("ATE0");
        DataServer.getInstance().servicesCheckProcesos.pushProceso(proceso2);

        final Procesos proceso3 = new Procesos(Constants.comandoATSP);
//      final Procesos proceso3 = new Procesos("ATSP0");
        DataServer.getInstance().servicesCheckProcesos.pushProceso(proceso3);

        final Procesos proceso4 = new Procesos("ATS0");
        DataServer.getInstance().servicesCheckProcesos.pushProceso(proceso4);

        final Procesos proceso5 = new Procesos("ATL1");
        DataServer.getInstance().servicesCheckProcesos.pushProceso(proceso5);

        Procesos procesoEncendido = new Procesos( "01" + Constants.comandoEncendido);
        DataServer.getInstance().servicesCheckProcesos.pushProceso(procesoEncendido);//Se envia el comando 0110, para saber el estado del vehiculo
        Procesos procesoEncendido1 = new Procesos( "01" + Constants.comandoEncendido1);
        DataServer.getInstance().servicesCheckProcesos.pushProceso(procesoEncendido1);//Se envia el comando 0110, para saber el estado del vehiculo
    }
}
