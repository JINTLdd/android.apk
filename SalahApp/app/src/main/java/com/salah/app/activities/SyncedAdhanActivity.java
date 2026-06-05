package com.salah.app.activities;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.salah.app.R;
import com.salah.app.services.AdhanService;
import com.salah.app.utils.AdhkarRepository;
import com.salah.app.utils.PreferencesManager;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Full-screen activity that plays the adhan audio AND displays each phrase in sync
 * using the per-muezzin timing table from adhkar.json (adhanTimings).
 *
 * Required intent extras:
 *   - "muezzin"  : id from adhanVoices ("makkah", "madinah", "kuwait", "quds", "brunei", "other")
 *   - "prayer"   : prayer id ("fajr", "dhuhr", "asr", "maghrib", "isha")
 *
 * The background image switches between day (fajr/dhuhr/asr) and night (maghrib/isha).
 */
public class SyncedAdhanActivity extends AppCompatActivity {
    public static final String EXTRA_MUEZZIN = "muezzin";
    public static final String EXTRA_PRAYER = "prayer";

    private TextView txtTitle, txtPhrase;
    private ImageView imgBg;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable scheduled;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        PreferencesManager.applyTheme(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true); setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        setContentView(R.layout.activity_synced_adhan);

        txtTitle = findViewById(R.id.txt_adhan_title);
        txtPhrase = findViewById(R.id.txt_adhan_phrase);
        imgBg = findViewById(R.id.img_bg);
        MaterialButton btnStop = findViewById(R.id.btn_stop_adhan);

        String muezzin = getIntent().getStringExtra(EXTRA_MUEZZIN);
        if (muezzin == null) muezzin = "madinah";
        String prayer = getIntent().getStringExtra(EXTRA_PRAYER);
        if (prayer == null) prayer = "fajr";

        // Pick a mosque background image specific to each prayer.
        int bgRes;
        switch (prayer) {
            case "fajr":    bgRes = R.drawable.mosque_fajr; break;
            case "dhuhr":   bgRes = R.drawable.mosque_dhuhr; break;
            case "asr":     bgRes = R.drawable.mosque_asr; break;
            case "maghrib": bgRes = R.drawable.mosque_maghrib; break;
            case "isha":    bgRes = R.drawable.mosque_isha; break;
            default:        bgRes = R.drawable.mosque_fajr;
        }
        imgBg.setImageResource(bgRes);

        // Localized prayer name
        String arPrayer = arabicPrayer(prayer);
        txtTitle.setText(getString(R.string.notif_prayer_text_fmt, arPrayer));

        btnStop.setOnClickListener(v -> { stop(); finish(); });

        startSyncedDisplay(muezzin, "fajr".equals(prayer));
    }

    private void startSyncedDisplay(String muezzin, boolean isFajr) {
        JSONObject root = AdhkarRepository.root(this);
        JSONObject timings = root.optJSONObject("adhanTimings");
        if (timings == null) { finish(); return; }
        JSONArray phrases = timings.optJSONArray(muezzin);
        if (phrases == null) phrases = timings.optJSONArray("madinah");
        if (phrases == null) { finish(); return; }

        // Concatenate the phrases (insert fajr extra after 10th index = after second "حي على الفلاح")
        java.util.List<Object[]> seq = new java.util.ArrayList<>();
        for (int i = 0; i < phrases.length(); i++) {
            try {
                JSONArray p = phrases.getJSONArray(i);
                seq.add(new Object[]{ p.getString(0), p.getInt(1) });
                // Insert fajr extras after the 10th element (index 9) = after second "حي على الفلاح"
                if (isFajr && i == 9) {
                    JSONArray fx = root.optJSONArray("adhanFajrExtra");
                    if (fx != null) for (int k = 0; k < fx.length(); k++) {
                        JSONArray e = fx.getJSONArray(k);
                        seq.add(new Object[]{ e.getString(0), e.getInt(1) });
                    }
                }
            } catch (Exception ignored) {}
        }

        // Schedule each phrase
        long delay = 0;
        for (int i = 0; i < seq.size(); i++) {
            final String phrase = (String) seq.get(i)[0];
            handler.postDelayed(() -> txtPhrase.setText(phrase), delay);
            delay += ((Integer) seq.get(i)[1]) * 1000L;
        }
        // Auto-finish 3s after the last phrase
        scheduled = () -> { stop(); finish(); };
        handler.postDelayed(scheduled, delay + 3000);

        // Start the audio playback service alongside (uses bundled adhan_*.mp3 in res/raw).
        // Real audio playback continues through AdhanService.
    }

    private String arabicPrayer(String id) {
        switch (id) {
            case "fajr":    return "الفجر";
            case "dhuhr":   return "الظهر";
            case "asr":     return "العصر";
            case "maghrib": return "المغرب";
            case "isha":    return "العشاء";
            default:        return "الصلاة";
        }
    }

    private void stop() {
        handler.removeCallbacksAndMessages(null);
        AdhanService.class.getSimpleName(); // no-op compile-touch
        sendBroadcast(new android.content.Intent("com.salah.app.STOP_ADHAN"));
    }

    @Override protected void onDestroy() { stop(); super.onDestroy(); }
}
