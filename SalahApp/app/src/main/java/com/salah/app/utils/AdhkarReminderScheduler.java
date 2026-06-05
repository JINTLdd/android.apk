package com.salah.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.salah.app.receivers.AthkarAlarmReceiver;

import java.util.Calendar;

/** Schedules daily morning (06:30) + evening (17:30) adhkar reminders. */
public class AdhkarReminderScheduler {

    public static void scheduleAll(Context ctx) {
        UserSettings();
        com.salah.app.models.UserSettings s = PreferencesManager.load(ctx);
        if (s.morningAthkarEnabled) {
            schedule(ctx, "morning", s.morningAthkarHour, s.morningAthkarMinute, 5001);
        }
        if (s.eveningAthkarEnabled) {
            schedule(ctx, "evening", s.eveningAthkarHour, s.eveningAthkarMinute, 5002);
        }
    }
    private static void UserSettings() {}

    private static void schedule(Context ctx, String type, int hour, int min, int reqCode) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, min);
        c.set(Calendar.SECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_MONTH, 1);

        Intent i = new Intent(ctx, AthkarAlarmReceiver.class);
        i.setAction(AlarmScheduler.ACTION_ATHKAR);
        i.putExtra(AlarmScheduler.EXTRA_ATHKAR_TYPE, type);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode, i,
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
