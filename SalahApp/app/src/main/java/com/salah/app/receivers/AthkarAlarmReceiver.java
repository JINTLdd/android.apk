package com.salah.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.os.VibrationEffect;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;

import com.salah.app.utils.AlarmScheduler;
import com.salah.app.utils.NotificationHelper;
import com.salah.app.utils.PreferencesManager;
import com.salah.app.models.UserSettings;

/** Fires Morning / Evening Athkar reminder notifications + re-schedules for next day. */
public class AthkarAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AthkarAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra(AlarmScheduler.EXTRA_ATHKAR_TYPE);
        if (type == null) type = "morning";
        Log.i(TAG, "AthkarAlarm fired: " + type);

        try {
            android.app.Notification n = NotificationHelper.buildAthkarNotification(context, type);
            int notifId = "morning".equals(type) ? 201 : 202;
            NotificationManagerCompat.from(context).notify(notifId, n);
            UserSettings s = PreferencesManager.load(context);
            if (s.vibrateOnAlarm) vibrate(context);
        } catch (SecurityException se) {
            Log.e(TAG, "Notification permission missing", se);
        }

        // Re-schedule for tomorrow same hour/minute.
        UserSettings s = PreferencesManager.load(context);
        if ("morning".equals(type) && s.morningAthkarEnabled) {
            AlarmScheduler.scheduleAthkar(context, "morning",
                s.morningAthkarHour, s.morningAthkarMinute);
        } else if ("evening".equals(type) && s.eveningAthkarEnabled) {
            AlarmScheduler.scheduleAthkar(context, "evening",
                s.eveningAthkarHour, s.eveningAthkarMinute);
        }
    }

    private void vibrate(Context ctx) {
        try {
            Vibrator v;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                v = vm != null ? vm.getDefaultVibrator() : null;
            } else {
                v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (v == null) return;
            long[] pattern = {0, 200, 100, 200};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                v.vibrate(pattern, -1);
            }
        } catch (Throwable ignored) {}
    }
}
