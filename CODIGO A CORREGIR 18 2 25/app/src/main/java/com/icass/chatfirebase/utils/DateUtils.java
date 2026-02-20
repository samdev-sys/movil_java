package com.icass.chatfirebase.utils;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    @NonNull
    public static String getDateFormatted() {
        long timestamp = System.currentTimeMillis();
        // "dd/MM/yyyy hh:mm:ss a"
        final SimpleDateFormat format = new SimpleDateFormat("HHmmddMMyy", Locale.getDefault());
        return format.format(new Date(timestamp));
    }

    @NonNull
    public static String getDateTimeFormatted(long timestamp) {
        final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault());
        return format.format(new Date(timestamp));
    }

    @NonNull
    public static String getDateTimeFormatted() {
        final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale.getDefault());
        return format.format(new Date(System.currentTimeMillis()));
    }
}