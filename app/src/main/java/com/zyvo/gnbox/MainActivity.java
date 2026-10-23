package com.zyvo.gnbox;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView clock;
    private TextView date;
    private TextView net;
    private View netDot;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            Date now = new Date();
            clock.setText(new SimpleDateFormat("HH:mm", Locale.US).format(now));
            date.setText(new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(now));
            updateNetwork();
            handler.postDelayed(this, 30000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        clock = findViewById(R.id.tvClock);
        date = findViewById(R.id.tvDate);
        net = findViewById(R.id.tvNet);
        netDot = findViewById(R.id.netDot);
        LinearLayout grid = findViewById(R.id.grid);

        addTile(grid, R.drawable.ic_iptv, getString(R.string.iptv), IptvActivity.class);
        addTile(grid, R.drawable.ic_youtube, getString(R.string.youtube), null, "https://m.youtube.com");
        addTile(grid, R.drawable.ic_browser, getString(R.string.browser), null, "https://www.google.com");
        addTile(grid, R.drawable.ic_radio, getString(R.string.radio), RadioActivity.class);
        addTile(grid, R.drawable.ic_weather, getString(R.string.weather), WeatherActivity.class);
        addTile(grid, R.drawable.ic_media, getString(R.string.media), MediaActivity.class);
        addTile(grid, R.drawable.ic_info, getString(R.string.firmware), InfoActivity.class);
        addTile(grid, R.drawable.ic_settings, getString(R.string.settings), SettingsActivity.class);
    }

    private void addTile(LinearLayout grid, int icon, String label, Class<?> target) {
        addTile(grid, icon, label, target, null);
    }

    private void addTile(LinearLayout grid, int icon, String label, Class<?> target, String webUrl) {
        LinearLayout tile = Ui.col(this);
        tile.setGravity(Gravity.CENTER);
        tile.setBackgroundResource(R.drawable.bg_tile_ripple);
        tile.setPadding(Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 14));
        tile.setClickable(true);
        tile.setFocusable(true);

        ImageView img = new ImageView(this);
        img.setImageResource(icon);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42));
        ip.gravity = Gravity.CENTER_HORIZONTAL;
        tile.addView(img, ip);

        TextView name = Ui.tv(this, label, 13, getColor(R.color.text));
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, Ui.dp(this, 10), 0, 0);
        tile.addView(name);

        final Intent intent;
        if (webUrl != null) {
            intent = new Intent(this, WebActivity.class);
            intent.putExtra("url", webUrl);
            intent.putExtra("title", label);
        } else {
            intent = new Intent(this, target);
        }
        tile.setOnClickListener(v -> startActivity(intent));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5));
        if (grid.getChildCount() == 0 || ((LinearLayout) grid.getChildAt(grid.getChildCount() - 1)).getChildCount() >= 3) {
            LinearLayout newRow = Ui.row(this);
            grid.addView(newRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        ((LinearLayout) grid.getChildAt(grid.getChildCount() - 1)).addView(tile, lp);
    }

    private void updateNetwork() {
        boolean online = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            Network active = cm.getActiveNetwork();
            if (active != null) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(active);
                online = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }
        } catch (Exception ignored) {}
        net.setText(online ? R.string.online : R.string.offline);
        net.setTextColor(getColor(online ? R.color.ok : R.color.sub));
        netDot.getBackground().setTint(getColor(online ? R.color.ok : R.color.bad));
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        ticker.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }
}
