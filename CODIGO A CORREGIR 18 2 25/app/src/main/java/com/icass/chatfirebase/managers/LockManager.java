package com.icass.chatfirebase.managers;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;

import androidx.annotation.NonNull;

import com.icass.chatfirebase.utils.Constants;

@SuppressWarnings("deprecation")
public class LockManager {
    private static volatile LockManager instance;

    private LockManager() {

    }

    public static LockManager getInstance() {
        if(instance == null) {
            synchronized(LockManager.class) {
                if(instance == null) {
                    instance = new LockManager();
                }
            }
        }

        return instance;
    }

    public void unlock(@NonNull Context context) {
        // Unlock the screen
        final PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, Constants.UNLOCK_TAG);
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);

        final KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        final KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock(Constants.TAG);
        keyguardLock.disableKeyguard();
    }
}