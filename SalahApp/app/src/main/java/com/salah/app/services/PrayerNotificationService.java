package com.salah.app.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import com.salah.app.utils.NotificationHelper;

/**
 * Helper service that can be used by JS/JNI bridges or future modules to post
 * non-foreground (silent) notifications without touching the AlarmManager.
 */
public class PrayerNotificationService extends Service {
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_TYPE = "type";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String type = intent.getStringExtra(EXTRA_TYPE);
            android.app.Notification n = NotificationHelper.buildAthkarNotification(
                this, type == null ? "morning" : type);
            try {
                NotificationManagerCompat.from(this).notify(
                    type != null && type.equals("evening") ? 202 : 201, n);
            } catch (SecurityException ignored) {}
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
