package com.salah.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.salah.app.receivers.KahfReminderReceiver;

import java.util.Calendar;

/** Schedules Friday 06:00 reminder to read Surat Al-Kahf. Reschedules weekly. */
public class KahfReminderScheduler {
    private static final int REQ = 7501;
    public static final int HOUR = 6;
    public static final int MIN = 0;

    public static void schedule(Context ctx) {
        Calendar c = Calendar.getInstance();
        int today = c.get(Calendar.DAY_OF_WEEK);
        int diff = (Calendar.FRIDAY - today + 7) % 7;
        if (diff == 0 && (c.get(Calendar.HOUR_OF_DAY) > HOUR
            || (c.get(Calendar.HOUR_OF_DAY) == HOUR && c.get(Calendar.MINUTE) >= MIN))) {
            diff = 7;
        }
        c.add(Calendar.DAY_OF_MONTH, diff);
        c.set(Calendar.HOUR_OF_DAY, HOUR);
        c.set(Calendar.MINUTE, MIN);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        Intent i = new Intent(ctx, KahfReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, REQ, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
        }
    }
}
