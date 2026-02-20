package com.icass.chatfirebase;

import androidx.annotation.NonNull;

import java.util.Date;

public class ChatMessage {
    @NonNull
    private String messageText = "";

    @NonNull
    private String messageUser = "";

    private long messageTime;

    public ChatMessage() {

    }

    public ChatMessage(@NonNull String messageText, @NonNull String messageUser) {
        this.messageText = messageText;
        this.messageUser = messageUser;
        this.messageTime = new Date().getTime();
    }

    public void setMessageText(@NonNull String messageText) {
        this.messageText = messageText;
    }

    @NonNull
    public String getMessageText() {
        return this.messageText;
    }

    public void setMessageUser(@NonNull String messageUser) {
        this.messageUser = messageUser;
    }

    @NonNull
    public String getMessageUser() {
        return this.messageUser;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

    public long getMessageTime() {
        return messageTime;
    }
}