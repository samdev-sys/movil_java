package com.icass.chatfirebase.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.icass.chatfirebase.ChatMessage;
import com.icass.chatfirebase.R;
import com.icass.chatfirebase.base.BaseActivity;
import com.icass.chatfirebase.data.LocalData;
import com.icass.chatfirebase.interfaces.CallbackService;
import com.icass.chatfirebase.managers.LockManager;
import com.icass.chatfirebase.managers.NetworkManager;
import com.icass.chatfirebase.utils.Constants;
import com.icass.chatfirebase.utils.DateUtils;
import com.icass.chatfirebase.utils.LogUtils;
import com.icass.chatfirebase.utils.Utils;
import com.icass.chatfirebase.utils.notificationSound;

public class AlertaActivity extends BaseActivity {
    private notificationSound notificacionSound;
    private static final String TAG = AlertaActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mensaje_alerta);

        changeFullScreen(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            unlockDevice();
        }

        final TextView tvDialogAlert = findViewById(R.id.tvDialogAlert);
        final Button btnOk = findViewById(R.id.btnOk);
        final Button btnCancel = findViewById(R.id.btnCancel);
        final SharedPreferences sharedPreferences = getSharedPreferences(Constants.TAG_TYPE_USER, Context.MODE_PRIVATE);
        final String codigo = sharedPreferences.getString(Constants.TAG_CODIGO, "");

        final String command = new LocalData().getCommand();

        if (command.isEmpty()) {
            finish();
        }

        new LocalData().setCommand("");
        LogUtils.d(TAG, "Clear Command");
        tvDialogAlert.setText(command);
        notificacionSound = new notificationSound(this, R.raw.alert);

        btnOk.setOnClickListener(view -> handleOkButtonClick(codigo, btnOk));

        sendMessageToServer("ALERTA RECIBIDA APP", codigo);

        btnCancel.setOnClickListener(view -> {
            Intent intent = new Intent(AlertaActivity.this, MainActivity2.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (notificacionSound != null) {
            notificacionSound.stopSound();
        }
        LogUtils.d(TAG, "El sonido deberia de parar");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (notificacionSound != null) {
            notificacionSound.playNotificationSound();
        }
        LogUtils.d(TAG, "El sonido deberia de reanudarse");
    }

    private void handleOkButtonClick(String codigo, Button btnOk) {
        notificacionSound.stopSound();
        btnOk.setBackgroundColor(Color.parseColor("#BEF182"));
        String date = DateUtils.getDateFormatted();
        String message = NetworkManager.getInstance().isNetworkConnected() ?
                "%%%%%" + date + "ALERTA RECIBIDA OPERADOR" :
                "%%%%%SI" + date + "ALERTA RECIBIDA OPERADOR";
        enviarMsgAlServer(message, codigo, () -> {
            LogUtils.d("Mensaje", " Mensaje enviado");
            //este codigo manda a la pantalla principal

          //  Intent intent = new Intent(AlertaActivity.this, MainActivity2.class);
           // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
           // startActivity(intent);

            //este cierra la aplicacion

          //  Intent intent = new Intent(Intent.ACTION_MAIN);
            //intent.addCategory(Intent.CATEGORY_HOME);
           // finish();
        });
    }

    private void sendMessageToServer(String messagePrefix, String codigo) {
        String date = DateUtils.getDateFormatted();
        String message = NetworkManager.getInstance().isNetworkConnected() ?
                "%%%%%%" + date + messagePrefix :
                "%%%%%%SI" + date + messagePrefix;
        enviarMsgAlServer(message, codigo, () -> LogUtils.d(TAG, "Mensaje enviado"));
    }

    public void enviarMsgAlServer(@NonNull String msg, @NonNull String codigo, @NonNull CallbackService listener) {
        try {
            String msg2 = msg + Utils.strConcat + codigo;
            LogUtils.d(TAG, "ENVIAR A WEB: " + msg2);

            ChatMessage chatMessage = new ChatMessage(msg2, "OBD");
            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
            DatabaseReference databaseReference = firebaseDatabase.getReference()
                    .child("VEHÍCULOS")
                    .child(codigo + "A")
                    .child("mensajes");

            databaseReference.push().setValue(chatMessage);
            LogUtils.d("Mensaje", " Mensaje de Entregado Enviado");
            LogUtils.d("Mensaje",msg2);

            listener.callback();
        } catch (Exception ex) {
            LogUtils.d(TAG, "enviarMsgAlServer" + ex.getMessage());
            Log.e("chat", "Error" + ex);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void unlockDevice() {
        LockManager.getInstance().unlock(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }
}