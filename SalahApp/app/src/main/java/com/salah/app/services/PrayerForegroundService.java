package com.salah.app.services;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.salah.app.utils.NotificationHelper;

/**
 * Light foreground service that can be used to keep the app alive briefly
 * during sensitive operations (e.g. computing tomorrow's schedule, network
 * sync). Reserved for future use — currently a no-op stub.
 */
public class PrayerForegroundService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        android.app.Notification n = NotificationHelper.buildForegroundNotification(this, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationHelper.NID_FOREGROUND, n,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NotificationHelper.NID_FOREGROUND, n);
        }
        // Do work then stop.
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
