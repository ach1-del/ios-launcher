package com.zyvo.gnbox;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = Ui.screen(this);
        root.addView(Ui.header(this, getString(R.string.settings_title)));

        TextView langLabel = Ui.tv(this, getString(R.string.language), 15, getColor(R.color.accent));
        langLabel.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 8));
        root.addView(langLabel);

        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);

        RadioButton en = new RadioButton(this);
        en.setText(R.string.english);
        en.setTextColor(getColor(R.color.text));
        en.setId(1001);

        RadioButton ar = new RadioButton(this);
        ar.setText(R.string.arabic);
        ar.setTextColor(getColor(R.color.text));
        ar.setId(1002);

        group.addView(en);
        group.addView(ar);
        group.check("ar".equals(Prefs.lang(this)) ? 1002 : 1001);
        root.addView(group);

        group.setOnCheckedChangeListener((g, id) -> {
            String lang = id == 1002 ? "ar" : "en";
            if (!lang.equals(Prefs.lang(this))) {
                Prefs.setLang(this, lang);
                Intent i = new Intent(this, SplashActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
        });

        root.addView(Ui.line(this));

        TextView aboutLabel = Ui.tv(this, getString(R.string.about), 15, getColor(R.color.accent));
        aboutLabel.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 8));
        root.addView(aboutLabel);

        TextView about = Ui.tv(this, getString(R.string.about_text), 13, getColor(R.color.sub));
        about.setLineSpacing(Ui.dp(this, 5), 1f);
        root.addView(about, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView version = Ui.tv(this, "GN-RS8 Box 1.0  •  firmware V3.44 (10092026)", 12, getColor(R.color.sub));
        version.setPadding(0, Ui.dp(this, 24), 0, 0);
        root.addView(version);

        setContentView(root);
    }
}
