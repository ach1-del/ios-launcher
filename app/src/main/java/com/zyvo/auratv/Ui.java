package com.zyvo.auratv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

/** Tiny UI helpers shared by every screen. */
public final class Ui {
    private Ui() {
    }

    public static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static void toast(Context c, String msg) {
        android.widget.Toast.makeText(c, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    public static void chip(TextView v, boolean active) {
        v.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip);
        v.setTextColor(active ? Color.parseColor("#0B0E14") : Color.parseColor("#F2F5FA"));
    }

    public static void immersive(Activity a) {
        Window w = a.getWindow();
        w.setStatusBarColor(Color.BLACK);
        w.setNavigationBarColor(Color.BLACK);
        w.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    public static void openPlayer(Context c, Channel ch, String sourceId) {
        Intent i = new Intent(c, PlayerActivity.class);
        i.putExtra("name", ch.name);
        i.putExtra("url", ch.url);
        i.putExtra("logo", ch.logo);
        i.putExtra("id", ch.id);
        i.putExtra("cmd", ch.cmd);
        i.putExtra("kind", ch.kind);
        i.putExtra("sourceId", ch.sourceId.isEmpty() ? sourceId : ch.sourceId);
        c.startActivity(i);
    }

    public static void openWeb(Context c, String url, String title) {
        Intent i = new Intent(c, WebActivity.class);
        i.putExtra("url", url);
        i.putExtra("title", title);
        c.startActivity(i);
    }

    public static String safe(String s) {
        return s == null ? "" : s;
    }
}
