package com.salah.app.activities;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.salah.app.R;
import com.salah.app.services.AdhkarPlaybackService;
import com.salah.app.utils.AdhkarRepository;
import com.salah.app.utils.PreferencesManager;
import com.salah.app.models.DhikrItem;

import java.util.List;

/**
 * Full-screen popup shown immediately after the adhan finishes. Displays the Madinah
 * mosque background + the dua-after-adhan text. Auto-plays its audio.
 */
public class PostAdhanActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        PreferencesManager.applyTheme(this);
        // Show over lock screen + turn screen on so the user definitely sees it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }
        setContentView(R.layout.activity_post_adhan);

        TextView body = findViewById(R.id.txt_post_adhan_body);
        List<DhikrItem> list = AdhkarRepository.getCategory(this, "afterAdhan");
        String audioUrl = null;
        if (!list.isEmpty()) {
            body.setText(list.get(0).text);
            audioUrl = list.get(0).audioUrl;
        }
        if (audioUrl != null && !audioUrl.isEmpty()) {
            AdhkarPlaybackService.play(this, audioUrl, getString(R.string.dua_after_adhan));
        }

        MaterialButton btnClose = findViewById(R.id.btn_close_post);
        btnClose.setOnClickListener(v -> {
            AdhkarPlaybackService.stopNow(this);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        AdhkarPlaybackService.stopNow(this);
        super.onDestroy();
    }
}
