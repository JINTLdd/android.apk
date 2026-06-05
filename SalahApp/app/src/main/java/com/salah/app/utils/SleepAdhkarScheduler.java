package com.salah.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.salah.app.receivers.AthkarAlarmReceiver;

import java.util.Calendar;

/** Auto-schedules the sleep adhkar to start playing at 21:00 daily. */
public class SleepAdhkarScheduler {
    public static final int HOUR = 21;
    public static final int MIN = 0;
    private static final int REQ = 7077;

    public static void schedule(Context ctx) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, HOUR);
        c.set(Calendar.MINUTE, MIN);
        c.set(Calendar.SECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_MONTH, 1);

        Intent i = new Intent(ctx, AthkarAlarmReceiver.class);
        i.setAction(AlarmScheduler.ACTION_ATHKAR);
        i.putExtra(AlarmScheduler.EXTRA_ATHKAR_TYPE, "sleep");
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
