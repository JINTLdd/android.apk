package com.salah.app.utils;

import android.content.Context;
import android.util.Log;
import com.salah.app.models.DhikrItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the adhkar catalog from assets/adhkar.json. Cached in memory after first load.
 */
public class AdhkarRepository {
    private static final String TAG = "AdhkarRepository";
    private static JSONObject root;

    public static synchronized JSONObject root(Context ctx) {
        if (root != null) return root;
        try (InputStream is = ctx.getAssets().open("adhkar.json");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            root = new JSONObject(sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to load adhkar.json", e);
            root = new JSONObject();
        }
        return root;
    }

    /** Returns the list under the given category key (morning/evening/afterPrayer/afterSalah/wakeup/iftitah/afterAdhan/duas/extra). */
    public static List<DhikrItem> getCategory(Context ctx, String key) {
        List<DhikrItem> out = new ArrayList<>();
        try {
            JSONArray arr = root(ctx).optJSONArray(key);
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                DhikrItem d = new DhikrItem();
                d.id = o.optInt("id", i + 1);
                d.title = o.optString("title", "");
                d.text = o.optString("text", "");
                d.count = o.optInt("count", 1);
                d.audioUrl = o.optString("audioUrl", "");
                out.add(d);
            }
        } catch (Exception e) {
            Log.e(TAG, "getCategory failed for " + key, e);
        }
        return out;
    }

    /** Returns the human-readable display name for a category key. */
    public static String getCategoryTitle(String key) {
        switch (key) {
            case "morning":     return "أذكار الصباح";
            case "evening":     return "أذكار المساء";
            case "afterSalah":  return "أذكار بعد الصلاة";
            case "wakeup":      return "أذكار الاستيقاظ";
            case "iftitah":     return "أدعية الاستفتاح";
            case "afterAdhan":  return "دعاء بعد الأذان";
            case "duas":        return "أدعية متنوعة";
            case "afterPrayer": return "أدعية مختارة";
            case "extra":       return "فضائل الأذكار";
            default:            return "الأذكار";
        }
    }
}
