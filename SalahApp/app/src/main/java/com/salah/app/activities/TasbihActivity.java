package com.salah.app.activities;

import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.salah.app.R;
import com.salah.app.utils.PreferencesManager;

/** 5-stage tasbih: 33x Subhanallah → 33x Alhamdulillah → 33x La ilaha illa Allah → 33x Allahu Akbar → 100x Astaghfirullah. */
public class TasbihActivity extends AppCompatActivity {

    private static final String[] PHRASES = {
        "سبحان اللّه",
        "الحمد للّه",
        "لا إله إلا اللّه",
        "اللّه أكبر",
        "أستغفر اللّه"
    };
    private static final int[] TARGETS = {33, 33, 33, 33, 100};

    private int stage = 0;
    private int count = 0;

    private TextView txtPhrase, txtCount, txtTarget, txtStage;
    private MaterialButton btnTap, btnReset;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        PreferencesManager.applyTheme(this);
        setContentView(R.layout.activity_tasbih);

        txtPhrase = findViewById(R.id.txt_phrase);
        txtCount = findViewById(R.id.txt_count);
        txtTarget = findViewById(R.id.txt_target);
        txtStage = findViewById(R.id.txt_stage);
        btnTap = findViewById(R.id.btn_tap);
        btnReset = findViewById(R.id.btn_reset);

        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        btnTap.setOnClickListener(v -> onTap());
        btnReset.setOnClickListener(v -> reset());
        update();
    }

    private void onTap() {
        count++;
        vibrate(40);
        if (count >= TARGETS[stage]) {
            vibrate(200);
            stage++;
            count = 0;
            if (stage >= PHRASES.length) {
                Toast.makeText(this, R.string.tasbih_completed, Toast.LENGTH_LONG).show();
                stage = 0;
            }
        }
        update();
    }

    private void reset() { stage = 0; count = 0; update(); }

    private void update() {
        txtPhrase.setText(PHRASES[stage]);
        txtCount.setText(String.valueOf(count));
        btnTap.setText(String.valueOf(count));
        txtTarget.setText(getString(R.string.tasbih_target_fmt, TARGETS[stage]));
        txtStage.setText(getString(R.string.tasbih_stage_fmt, stage + 1, PHRASES.length));
    }

    private void vibrate(long ms) {
        try {
            Vibrator v;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                v = vm != null ? vm.getDefaultVibrator() : null;
            } else {
                v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            }
            if (v == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(ms);
            }
        } catch (Throwable ignored) {}
    }
}
