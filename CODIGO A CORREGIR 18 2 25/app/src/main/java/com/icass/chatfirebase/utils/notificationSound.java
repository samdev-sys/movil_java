package com.icass.chatfirebase.utils;

import android.content.Context;
import android.media.MediaPlayer;

public class notificationSound {
    private Context context;
    MediaPlayer mediaPlayer;

    public notificationSound(Context context, int resid){
        this.context = context;
        mediaPlayer = MediaPlayer.create(this.context, resid);
        playNotificationSound();
    }

    public void playNotificationSound() {
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }
    public void stopSound(){
        mediaPlayer.stop();
    }
}
