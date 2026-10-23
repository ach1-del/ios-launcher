package com.zyvo.auratv;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.widget.TextView;

public class SettingsActivity extends BaseActivity {
    private Prefs prefs;
    private TextView chipEn, chipAr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = new Prefs(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        chipEn = findViewById(R.id.chipEn);
        chipAr = findViewById(R.id.chipAr);
        refreshLang();
        chipEn.setOnClickListener(v -> setLang("en"));
        chipAr.setOnClickListener(v -> setLang("ar"));

        findViewById(R.id.rowClear).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage(R.string.clear_data)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear_data, (d, w) -> {
                    prefs.clearFavorites();
                    Ui.toast(this, getString(R.string.removed_fav));
                })
                .show());

        TextView version = findViewById(R.id.tvVersion);
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version.setText("Aura TV " + pi.versionName);
        } catch (Exception e) {
            version.setText("Aura TV 2.0");
        }
    }

    private void refreshLang() {
        boolean ar = "ar".equals(prefs.lang());
        Ui.chip(chipEn, !ar);
        Ui.chip(chipAr, ar);
    }

    private void setLang(String lang) {
        if (lang.equals(prefs.lang())) return;
        prefs.setLang(lang);
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finishAffinity();
    }
}
