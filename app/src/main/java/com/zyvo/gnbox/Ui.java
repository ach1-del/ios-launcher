package com.zyvo.gnbox;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    private Ui() {}

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static TextView tv(Context c, CharSequence text, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    public static LinearLayout col(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lpw(float weight) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
    }

    public static LinearLayout.LayoutParams margins(LinearLayout.LayoutParams p, int l, int t, int r, int b) {
        p.setMargins(l, t, r, b);
        return p;
    }

    public static View spacer(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    public static EditText edit(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setHintTextColor(c.getColor(R.color.sub));
        e.setTextColor(c.getColor(R.color.text));
        e.setTextSize(14);
        e.setSingleLine(true);
        e.setBackgroundResource(R.drawable.bg_input);
        e.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        return e;
    }

    public static Button btn(Context c, String label, boolean primary) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(primary ? c.getColor(R.color.bg1) : c.getColor(R.color.text));
        b.setBackgroundResource(primary ? R.drawable.bg_button : R.drawable.bg_button_dark);
        b.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8));
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        return b;
    }

    public static LinearLayout screen(Context c) {
        LinearLayout l = col(c);
        l.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16));
        return l;
    }

    public static LinearLayout header(Activity a, String title) {
        LinearLayout h = row(a);
        TextView back = tv(a, "‹", 30, a.getColor(R.color.accent));
        back.setPadding(dp(a, 8), 0, dp(a, 16), dp(a, 2));
        back.setOnClickListener(v -> a.finish());
        h.addView(back);
        h.addView(tv(a, title, 19, a.getColor(R.color.text)));
        return h;
    }

    public static TextView hint(Context c, String text) {
        TextView t = tv(c, text, 12, c.getColor(R.color.sub));
        t.setPadding(0, dp(c, 8), 0, dp(c, 4));
        return t;
    }

    public static TextView status(Context c) {
        TextView t = tv(c, "", 13, c.getColor(R.color.accent2));
        t.setPadding(0, dp(c, 8), 0, dp(c, 8));
        return t;
    }

    public static View line(Context c) {
        View v = new View(c);
        v.setBackgroundColor(Color.parseColor("#1C4060"));
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 1)));
        return v;
    }
}
