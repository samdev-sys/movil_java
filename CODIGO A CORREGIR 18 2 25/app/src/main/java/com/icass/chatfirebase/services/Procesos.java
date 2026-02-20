package com.icass.chatfirebase.services;

import androidx.annotation.NonNull;

public class Procesos { //Atributos se mandan llamar al momento de querer apilar un comando en el método "pushProcesos" de la clase "CheckearColaComandos.java"
    private final boolean banWEB;

    @NonNull
    private final String comando;

    private final boolean concatenar;

    private final boolean enviar;

    public Procesos(boolean banWEB, @NonNull String comando, boolean concatenar, boolean enviar) {
        this.banWEB = banWEB;
        this.comando = comando;
        this.concatenar = concatenar;
        this.enviar = enviar;
    }

    public Procesos(@NonNull String comando) {
        this.banWEB = false;
        this.comando = comando;
        this.concatenar = false;
        this.enviar = false;
    }

    public boolean isBanWEB() {
        return banWEB;
    }

    @NonNull
    public String getComando() {
        return comando;
    }

    public boolean isConcatenar() {
        return concatenar;
    }

    public boolean isEnviar() {
        return enviar;
    }
}