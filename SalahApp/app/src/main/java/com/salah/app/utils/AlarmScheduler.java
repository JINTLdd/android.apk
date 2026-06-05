package com.salah.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.salah.app.models.Location;
import com.salah.app.models.PrayerTime;
import com.salah.app.models.UserSettings;
import com.salah.app.receivers.AthkarAlarmReceiver;
import com.salah.app.receivers.PrayerAlarmReceiver;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Schedules exact-and-while-idle alarms for the next prayer + Morning/Evening Athkar.
 * Uses AlarmManager.setExactAndAllowWhileIdle so alarms fire even in Doze and when the
 * app is killed.
 */
public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";
    public static final String ACTION_PRAYER = "com.salah.app.PRAYER_ALARM";
    public static final String ACTION_ATHKAR = "com.salah.app.ATHKAR_ALARM";

    public static final String EXTRA_PRAYER_ID = "prayer_id";
    public static final String EXTRA_PRAYER_NAME_AR = "prayer_name_ar";
    public static final String EXTRA_ATHKAR_TYPE = "athkar_type"; // "morning" | "evening"

    /** Cancels and re-schedules ALL alarms (used on settings change + boot). */
    public static void rescheduleAll(Context ctx) {
        cancelAll(ctx);
        UserSettings settings = PreferencesManager.load(ctx);
        Location loc = PreferencesManager.loadLocation(ctx);
        if (loc == null) {
            Log.w(TAG, "No saved location — skipping alarm scheduling");
            return;
        }

        // Schedule the next prayer alarm only (the receiver re-schedules the following one
        // after each fire — keeps things robust against alarm-quota throttling).
        if (settings.adhanEnabled) {
            PrayerTime next = PrayerTimesCalculator.nextPrayer(loc, settings);
            if (next != null) schedulePrayer(ctx, next);
        }

        if (settings.morningAthkarEnabled) {
            scheduleAthkar(ctx, "morning", settings.morningAthkarHour, settings.morningAthkarMinute);
        }
        if (settings.eveningAthkarEnabled) {
            scheduleAthkar(ctx, "evening", settings.eveningAthkarHour, settings.eveningAthkarMinute);
        }
    }

    public static void schedulePrayer(Context ctx, PrayerTime p) {
        Intent intent = new Intent(ctx, PrayerAlarmReceiver.class);
        intent.setAction(ACTION_PRAYER);
        intent.putExtra(EXTRA_PRAYER_ID, p.prayer.id);
        intent.putExtra(EXTRA_PRAYER_NAME_AR, p.getArabicName());

        PendingIntent pi = PendingIntent.getBroadcast(
            ctx, requestCodeForPrayer(p.prayer.id), intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        setExact(ctx, p.time.getTime(), pi);
        Log.i(TAG, "Scheduled " + p.prayer.id + " at " + p.time);

        // Also schedule a pre-prayer reminder 10 minutes before the adhan.
        long preTime = p.time.getTime() - 10L * 60_000L;
        if (preTime > System.currentTimeMillis()) {
            Intent pre = new Intent(ctx, com.salah.app.receivers.PrePrayerReceiver.class);
            pre.putExtra(com.salah.app.receivers.PrePrayerReceiver.EXTRA_PRAYER_AR, p.getArabicName());
            pre.putExtra(com.salah.app.receivers.PrePrayerReceiver.EXTRA_MINUTES, 10);
            PendingIntent prePI = PendingIntent.getBroadcast(
                ctx, requestCodeForPrayer(p.prayer.id) + 500, pre,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            setExact(ctx, preTime, prePI);
            Log.i(TAG, "Pre-prayer reminder for " + p.prayer.id + " at " + new java.util.Date(preTime));
        }
    }

    public static void scheduleAthkar(Context ctx, String type, int hour, int minute) {
        Calendar c = Calendar.getInstance(TimeZone.getDefault());
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        // If today's time already passed, schedule for tomorrow.
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(ctx, AthkarAlarmReceiver.class);
        intent.setAction(ACTION_ATHKAR);
        intent.putExtra(EXTRA_ATHKAR_TYPE, type);

        PendingIntent pi = PendingIntent.getBroadcast(
            ctx, requestCodeForAthkar(type), intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        setExact(ctx, c.getTimeInMillis(), pi);
        Log.i(TAG, "Scheduled " + type + " athkar at " + c.getTime());
    }

    public static void cancelAll(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        for (String id : new String[]{"fajr", "dhuhr", "asr", "maghrib", "isha"}) {
            Intent i = new Intent(ctx, PrayerAlarmReceiver.class).setAction(ACTION_PRAYER);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, requestCodeForPrayer(id), i,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) am.cancel(pi);
        }
        for (String t : new String[]{"morning", "evening"}) {
            Intent i = new Intent(ctx, AthkarAlarmReceiver.class).setAction(ACTION_ATHKAR);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, requestCodeForAthkar(t), i,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) am.cancel(pi);
        }
    }

    private static void setExact(Context ctx, long triggerAtMillis, PendingIntent pi) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                } else {
                    // Fall back to inexact — still better than nothing on devices that deny exact alarms.
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to set exact alarm", e);
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        }
    }

    private static int requestCodeForPrayer(String prayerId) {
        return 1000 + Math.abs(prayerId.hashCode() % 1000);
    }

    private static int requestCodeForAthkar(String type) {
        return 2000 + Math.abs(type.hashCode() % 1000);
    }
}
