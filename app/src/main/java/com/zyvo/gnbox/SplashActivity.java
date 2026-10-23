package com.zyvo.gnbox;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashActivity extends BaseActivity {
    private MediaPlayer sound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = Ui.screen(this);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundResource(R.drawable.bg_screen);

        TextView logo = Ui.tv(this, "GN-RS8", 44, getColor(R.color.accent2));
        logo.setLetterSpacing(0.08f);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo);

        TextView model = Ui.tv(this, "MINI HD PLUS", 16, getColor(R.color.accent));
        model.setLetterSpacing(0.35f);
        model.setGravity(Gravity.CENTER);
        model.setPadding(0, Ui.dp(this, 4), 0, 0);
        root.addView(model);

        TextView soc = Ui.tv(this, "GX6605S  •  V3.44", 12, getColor(R.color.sub));
        soc.setGravity(Gravity.CENTER);
        soc.setPadding(0, Ui.dp(this, 10), 0, 0);
        root.addView(soc);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setIndeterminate(true);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(Ui.dp(this, 180), Ui.dp(this, 4));
        bp.topMargin = Ui.dp(this, 40);
        root.addView(bar, bp);

        TextView loading = Ui.tv(this, getString(R.string.splash_loading), 12, getColor(R.color.sub));
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, Ui.dp(this, 12), 0, 0);
        root.addView(loading);

        View footer = new View(this);
        root.addView(footer, new LinearLayout.LayoutParams(1, Ui.dp(this, 60)));

        setContentView(root);

        try {
            sound = MediaPlayer.create(this, R.raw.boot);
            if (sound != null) sound.start();
        } catch (Exception ignored) {}

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 2600);
    }

    @Override
    protected void onDestroy() {
        if (sound != null) {
            try { sound.release(); } catch (Exception ignored) {}
            sound = null;
        }
        super.onDestroy();
    }
}
