package com.salah.app.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.util.Log;

import com.salah.app.models.DhikrItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Downloads all adhkar MP3 URLs once and stores them in app-private storage.
 * Subsequent playback reads the local files (offline support).
 */
public class AudioDownloader {
    private static final String TAG = "AudioDownloader";
    public static final String FLAG_DOWNLOADED = "audio_downloaded";

    public interface Progress {
        void onProgress(int currentIndex, int total, String currentTitle);
        void onComplete(int succeeded, int failed);
        void onError(String message);
    }

    private static File audioDir(Context ctx) {
        File d = new File(ctx.getFilesDir(), "audio");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    /** Returns local File for a given remote URL (sanitized filename). */
    public static File localFileFor(Context ctx, String url) {
        if (url == null || url.isEmpty()) return null;
        String name = url.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.length() > 120) name = name.substring(name.length() - 120);
        return new File(audioDir(ctx), name);
    }

    /** Returns either local path (if downloaded) or original URL. */
    public static String resolvePlaybackUri(Context ctx, String url) {
        if (url == null || url.isEmpty()) return null;
        File f = localFileFor(ctx, url);
        if (f != null && f.exists() && f.length() > 1024) {
            return f.getAbsolutePath();
        }
        return url;
    }

    /** Collects every audioUrl from every category in adhkar.json + adhan voices. */
    public static List<String> collectAllUrls(Context ctx) {
        List<String> urls = new ArrayList<>();
        JSONObject root = AdhkarRepository.root(ctx);
        String[] cats = {"morning","afterSalah","wakeup","iftitah","afterAdhan","duas","afterPrayer"};
        for (String c : cats) {
            for (DhikrItem d : AdhkarRepository.getCategory(ctx, c)) {
                if (d.audioUrl != null && d.audioUrl.startsWith("http") && !urls.contains(d.audioUrl)) {
                    urls.add(d.audioUrl);
                }
            }
        }
        // Adhan voices
        JSONArray ad = root.optJSONArray("adhanVoices");
        if (ad != null) {
            for (int i = 0; i < ad.length(); i++) {
                String u = ad.optJSONObject(i).optString("url", "");
                if (!u.isEmpty() && !urls.contains(u)) urls.add(u);
            }
        }
        return urls;
    }

    /** Synchronous bulk-download. Must be called from a background thread. */
    public static void downloadAll(Context ctx, Progress cb) {
        List<String> urls = collectAllUrls(ctx);
        int total = urls.size();
        int ok = 0, fail = 0;
        for (int i = 0; i < total; i++) {
            String url = urls.get(i);
            if (cb != null) cb.onProgress(i, total, url);
            try {
                File out = localFileFor(ctx, url);
                if (out != null && out.exists() && out.length() > 1024) {
                    ok++;
                    continue;
                }
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(60000);
                c.connect();
                if (c.getResponseCode() == 200) {
                    try (InputStream is = c.getInputStream();
                         FileOutputStream fos = new FileOutputStream(out)) {
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = is.read(buf)) != -1) fos.write(buf, 0, r);
                    }
                    ok++;
                } else {
                    fail++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Download failed: " + url, e);
                fail++;
            }
        }
        if (cb != null) cb.onComplete(ok, fail);
        ctx.getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean(FLAG_DOWNLOADED, ok > 0).apply();
    }

    public static boolean isDownloaded(Context ctx) {
        return ctx.getSharedPreferences("salah_prefs", Context.MODE_PRIVATE)
            .getBoolean(FLAG_DOWNLOADED, false);
    }
}
