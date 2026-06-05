package com.salah.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.salah.app.receivers.HourlyDuaReceiver;

/**
 * Schedules a dua notification every 2 hours (between 08:00 and 22:00 only — avoids
 * waking the user during sleep hours).
 */
public class HourlyDuaScheduler {
    private static final int REQ = 9001;
    private static final long INTERVAL_MS = 2 * 60 * 60 * 1000L; // 2 hours

    public static void scheduleNext(Context ctx) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        long now = System.currentTimeMillis();
        long target = now + INTERVAL_MS;
        c.setTimeInMillis(target);
        int hour = c.get(java.util.Calendar.HOUR_OF_DAY);
        // Quiet hours: 22:00 → 08:00. If target falls in quiet zone, shift to 08:00 next day.
        if (hour >= 22 || hour < 8) {
            c.set(java.util.Calendar.HOUR_OF_DAY, 8);
            c.set(java.util.Calendar.MINUTE, 0);
            c.set(java.util.Calendar.SECOND, 0);
            if (c.getTimeInMillis() <= now) c.add(java.util.Calendar.DAY_OF_MONTH, 1);
            target = c.getTimeInMillis();
        }

        Intent i = new Intent(ctx, HourlyDuaReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, REQ, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pi);
        }
    }

    public static void cancel(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, HourlyDuaReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, REQ, i,
            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) am.cancel(pi);
    }
}
