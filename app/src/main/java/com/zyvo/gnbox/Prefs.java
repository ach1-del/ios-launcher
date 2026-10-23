package com.zyvo.gnbox;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("gnbox", Context.MODE_PRIVATE);
    }

    public static String lang(Context c) {
        return sp(c).getString("lang", "en");
    }

    public static void setLang(Context c, String lang) {
        sp(c).edit().putString("lang", lang).apply();
    }
}
