package com.icass.chatfirebase.StartVelocida;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.icass.chatfirebase.services.ConexionService;
import com.icass.chatfirebase.utils.LogUtils;

import java.util.List;

public class BootUpReceiver extends BroadcastReceiver {
    private static final String TAG = BootUpReceiver.class.getSimpleName();
    private static final Long PERIOD_MS = 5000L;
    private static final int BOOT_SERVICE_CODE = 1;
    private static final int BOOT_JOB_ID = 10;

    @Override
    public void onReceive(Context context, Intent intent) {
        addAutoStartup(context);

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            LogUtils.d(TAG, "***********Se llama el AutoStartBoot***************");
            final Intent intentService = new Intent(context, ConexionService.class);
            @SuppressLint("UnspecifiedImmutableFlag") final PendingIntent pendingIntent = PendingIntent.getService(context, BOOT_SERVICE_CODE, intentService, PendingIntent.FLAG_ONE_SHOT);
            final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), PERIOD_MS, pendingIntent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP) // este metodo funciona cuando el telefono tiene SDK de LOLLIPOP, se evaluara su uso.
    public static void scheduleJob(Context context) {
//        final ComponentName serviceComponent = new ComponentName(context, StartVel.class);
        final ComponentName serviceComponent = new ComponentName(context, ConexionService.class);
        final JobInfo.Builder builder = new JobInfo.Builder(BOOT_JOB_ID, serviceComponent);

        builder.setMinimumLatency(PERIOD_MS);
        builder.setOverrideDeadline(PERIOD_MS);

        final JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }

    private void addAutoStartup(@NonNull Context context) {
        try {
            final Intent intent = new Intent();
            final String manufacturer = android.os.Build.MANUFACTURER;

            if ("xiaomi".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            } else if ("oppo".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            } else if ("vivo".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
            } else if ("Letv".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"));
            } else if ("Honor".equalsIgnoreCase(manufacturer)) {
                intent.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"));
            }

            final List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

            if (list.size() > 0) {
                context.startActivity(intent);
            }
        } catch (Exception ex) {
            LogUtils.e(TAG, "addAutoStartup" + ex);
        }
    }
}