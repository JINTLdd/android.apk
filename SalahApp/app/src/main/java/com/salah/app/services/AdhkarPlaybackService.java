package com.salah.app.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.salah.app.R;
import com.salah.app.activities.MainActivity;
import com.salah.app.utils.AudioDownloader;
import com.salah.app.utils.NotificationHelper;

/**
 * Foreground service that plays a single dhikr's audio. Designed so playback continues
 * uninterrupted when the user backgrounds or kills the activity — the system kills
 * the service only when stop() is called or the audio completes.
 */
public class AdhkarPlaybackService extends Service {
    private static final String TAG = "AdhkarPlayback";
    public static final String ACTION_PLAY = "com.salah.app.adhkar.PLAY";
    public static final String ACTION_STOP = "com.salah.app.adhkar.STOP";
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TITLE = "title";

    private MediaPlayer player;
    private String currentTitle = "";

    public static void play(Context ctx, String url, String title) {
        if (url == null || url.isEmpty()) return;
        Intent i = new Intent(ctx, AdhkarPlaybackService.class);
        i.setAction(ACTION_PLAY);
        i.putExtra(EXTRA_URL, url);
        i.putExtra(EXTRA_TITLE, title);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i);
        else ctx.startService(i);
    }

    public static void stopNow(Context ctx) {
        Intent i = new Intent(ctx, AdhkarPlaybackService.class);
        i.setAction(ACTION_STOP);
        try { ctx.startService(i); } catch (Throwable ignored) {}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            release(); stopSelf(); return START_NOT_STICKY;
        }
        if (!ACTION_PLAY.equals(action)) { stopSelf(); return START_NOT_STICKY; }

        String url = intent.getStringExtra(EXTRA_URL);
        currentTitle = intent.getStringExtra(EXTRA_TITLE);
        startForeground(NotificationHelper.NID_FOREGROUND, buildNotification(currentTitle));
        playUrl(url);
        return START_NOT_STICKY;
    }

    private void playUrl(String url) {
        release();
        try {
            String uri = AudioDownloader.resolvePlaybackUri(this, url);
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
            player.setDataSource(uri);
            player.setOnPreparedListener(MediaPlayer::start);
            player.setOnCompletionListener(mp -> { release(); stopSelf(); });
            player.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error " + what + "/" + extra + " url=" + url);
                release(); stopSelf(); return true;
            });
            player.prepareAsync();
        } catch (Throwable t) {
            Log.e(TAG, "playUrl failed: " + url, t);
            release(); stopSelf();
        }
    }

    private void release() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
                player = null;
            }
        } catch (Throwable ignored) {}
        try { stopForeground(STOP_FOREGROUND_REMOVE); } catch (Throwable ignored) {}
    }

    private android.app.Notification buildNotification(String title) {
        Intent stop = new Intent(this, AdhkarPlaybackService.class).setAction(ACTION_STOP);
        PendingIntent stopPI = PendingIntent.getService(this, 0, stop,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent open = new Intent(this, MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPI = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, NotificationHelper.CH_FOREGROUND)
            .setSmallIcon(R.drawable.ic_athkar)
            .setContentTitle(getString(R.string.notif_playing_adhkar))
            .setContentText(title == null ? "" : title)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPI)
            .addAction(R.drawable.ic_stop, getString(R.string.notif_action_stop), stopPI)
            .build();
    }

    @Override
    public void onDestroy() { release(); super.onDestroy(); }

    @Nullable @Override
    public IBinder onBind(Intent intent) { return null; }
}
