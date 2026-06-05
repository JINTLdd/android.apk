package com.salah.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.salah.app.receivers.FastingReminderReceiver;

import java.util.Calendar;

/**
 * Schedules weekly fasting reminders:
 *   - Sunday at 21:00 → reminder for Monday's fast
 *   - Wednesday at 21:00 → reminder for Thursday's fast
 *
 * After each fire the receiver re-schedules for next week. AlarmManager.setExactAndAllowWhileIdle
 * is used so it works even when the app is killed.
 */
public class FastingReminderScheduler {
    private static final String TAG = "FastingReminderScheduler";
    public static final int REQ_MONDAY = 8801;
    public static final int REQ_THURSDAY = 8802;
    public static final int HOUR = 21; // 9 PM
    public static final int MIN = 0;

    public static void scheduleAll(Context ctx) {
        scheduleNext(ctx, Calendar.SUNDAY, "monday", REQ_MONDAY);
        scheduleNext(ctx, Calendar.WEDNESDAY, "thursday", REQ_THURSDAY);
    }

    public static void cancelAll(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (int rc : new int[]{REQ_MONDAY, REQ_THURSDAY}) {
            Intent i = new Intent(ctx, FastingReminderReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, rc, i,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) am.cancel(pi);
        }
    }

    private static void scheduleNext(Context ctx, int dayOfWeek, String fastDay, int reqCode) {
        Calendar c = Calendar.getInstance();
        // Move to the next occurrence of the desired weekday at HOUR:MIN
        int today = c.get(Calendar.DAY_OF_WEEK);
        int diff = (dayOfWeek - today + 7) % 7;
        if (diff == 0 && (c.get(Calendar.HOUR_OF_DAY) > HOUR
            || (c.get(Calendar.HOUR_OF_DAY) == HOUR && c.get(Calendar.MINUTE) >= MIN))) {
            diff = 7; // already past today's slot -> next week
        }
        c.add(Calendar.DAY_OF_MONTH, diff);
        c.set(Calendar.HOUR_OF_DAY, HOUR);
        c.set(Calendar.MINUTE, MIN);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        Intent i = new Intent(ctx, FastingReminderReceiver.class);
        i.putExtra(FastingReminderReceiver.EXTRA_DAY, fastDay);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long t = c.getTimeInMillis();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
            }
            Log.i(TAG, "Fasting reminder scheduled (" + fastDay + ") for " + c.getTime());
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
        }
    }
}
