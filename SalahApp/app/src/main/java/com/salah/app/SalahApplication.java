package com.salah.app;

import android.app.Application;
import com.salah.app.utils.NotificationHelper;
import com.salah.app.utils.PreferencesManager;

/**
 * Application class — initializes notification channels and re-schedules alarms
 * on first launch / process start.
 */
public class SalahApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Create high-importance notification channels for prayer / athkar alarms.
        NotificationHelper.createAllChannels(this);
        // Apply the saved RTL/Arabic locale + dark/light theme at process start.
        PreferencesManager.applyTheme(this);
    }
}
