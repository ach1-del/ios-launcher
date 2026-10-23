package com.zyvo.gnbox;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class InfoActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = Ui.screen(this);
        root.addView(Ui.header(this, getString(R.string.info_title)));

        TextView body = Ui.tv(this, getString(R.string.fw_loading), 13, getColor(R.color.text));
        body.setTypeface(android.graphics.Typeface.MONOSPACE);
        body.setLineSpacing(Ui.dp(this, 4), 1f);
        body.setTextIsSelectable(true);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        try (InputStream in = getAssets().open("fw_info.txt")) {
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
            r.close();
            body.setText(sb.toString());
        } catch (Exception e) {
            body.setText(R.string.net_error);
        }
    }
}
