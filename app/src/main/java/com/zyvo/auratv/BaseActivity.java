package com.zyvo.auratv;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.app.Activity;

import java.util.Locale;

/** Applies the saved language to every screen. */
public class BaseActivity extends Activity {
    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences sp = newBase.getSharedPreferences("aura_tv", Context.MODE_PRIVATE);
        String lang = sp.getString("lang", "en");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration cfg = new Configuration(newBase.getResources().getConfiguration());
        cfg.setLocale(locale);
        super.attachBaseContext(newBase.createConfigurationContext(cfg));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.bg_app);
    }
}
