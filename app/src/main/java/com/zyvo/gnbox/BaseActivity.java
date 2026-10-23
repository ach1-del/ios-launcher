package com.zyvo.gnbox;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

public class BaseActivity extends Activity {
    @Override
    protected void attachBaseContext(Context base) {
        Locale locale = new Locale(Prefs.lang(base));
        Locale.setDefault(locale);
        Configuration cfg = new Configuration(base.getResources().getConfiguration());
        cfg.setLocale(locale);
        super.attachBaseContext(base.createConfigurationContext(cfg));
    }
}
