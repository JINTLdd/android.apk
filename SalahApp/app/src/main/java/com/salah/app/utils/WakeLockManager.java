package com.salah.app.utils;

import android.content.Context;
import android.os.PowerManager;

/** Thin wrapper around PowerManager.WakeLock for adhan playback. */
public class WakeLockManager {
    private static PowerManager.WakeLock current;

    public static synchronized void acquire(Context ctx, long timeoutMs) {
        if (current != null && current.isHeld()) return;
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;
        current = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
            "SalahApp::AdhanWakeLock"
        );
        current.setReferenceCounted(false);
        current.acquire(timeoutMs);
    }

    public static synchronized void release() {
        if (current != null && current.isHeld()) {
            try { current.release(); } catch (Throwable ignored) {}
        }
        current = null;
    }
}
