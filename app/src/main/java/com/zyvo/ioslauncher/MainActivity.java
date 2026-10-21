package com.zyvo.ioslauncher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.viewpager2.widget.ViewPager2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private TextView timeView;
    private TextView dateView;
    private PagesAdapter pagesAdapter;
    private final List<AppInfo> allApps = new ArrayList<>();

    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            clockHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView wallpaper = findViewById(R.id.wallpaper);
        wallpaper.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.wallpaper));

        timeView = findViewById(R.id.clockTime);
        dateView = findViewById(R.id.clockDate);

        ViewPager2 pages = findViewById(R.id.pages);
        pagesAdapter = new PagesAdapter(this::launchApp);
        pages.setAdapter(pagesAdapter);

        pages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
        });

        findViewById(R.id.searchBar).setOnClickListener(v ->
                startActivity(new Intent(this, AppDrawerActivity.class)));

        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        clockHandler.post(clockTick);
        loadApps();
    }

    @Override
    protected void onPause() {
        clockHandler.removeCallbacks(clockTick);
        super.onPause();
    }

    private void loadApps() {
        AppRepository.loadAsync(this, apps -> runOnUiThread(() -> {
            allApps.clear();
            allApps.addAll(apps);
            pagesAdapter.submit(allApps);
            buildDock();
            updateDots(0);
        }));
    }

    private void buildDock() {
        LinearLayout dock = findViewById(R.id.dock);
        dock.removeAllViews();
        List<AppInfo> favorites = pickDockApps();
        int size = (int) (60 * getResources().getDisplayMetrics().density);
        for (AppInfo app : favorites) {
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(app.fallbackIcon);
            icon.setLayoutParams(new LinearLayout.LayoutParams(size, size));
            icon.setPadding(size / 10, 0, size / 10, 0);
            final Intent launch = getPackageManager().getLaunchIntentForPackage(app.packageName);
            if (launch != null) {
                icon.setOnClickListener(v -> launchApp(app));
            }
            dock.addView(icon);
        }
    }

    /** Prefers themed/system favorites for the dock: phone, messages, browser, camera. */
    private List<AppInfo> pickDockApps() {
        String[] wanted = {
                "com.android.dialer", "com.google.android.dialer", "com.android.contacts",
                "com.android.mms", "com.google.android.apps.messaging",
                "com.android.browser", "com.android.chrome",
                "com.android.camera", "com.android.camera2"
        };
        List<AppInfo> dock = new ArrayList<>();
        for (String pkg : wanted) {
            if (dock.size() >= 4) break;
            for (AppInfo app : allApps) {
                if (app.packageName.equals(pkg) && !containsDock(dock, app)) {
                    dock.add(app);
                    break;
                }
            }
        }
        // pad with first apps if some are missing
        for (AppInfo app : allApps) {
            if (dock.size() >= 4) break;
            if (!containsDock(dock, app)) dock.add(app);
        }
        return dock;
    }

    private boolean containsDock(List<AppInfo> dock, AppInfo app) {
        for (AppInfo d : dock) {
            if (d.packageName.equals(app.packageName)) return true;
        }
        return false;
    }

    private void launchApp(AppInfo app) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        }
    }

    private void updateDots(int selected) {
        LinearLayout dots = findViewById(R.id.dots);
        dots.removeAllViews();
        int count = pagesAdapter.pageCount();
        int dp = (int) (6 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp, dp);
            lp.setMargins(dp / 2, 0, dp / 2, 0);
            dot.setLayoutParams(lp);
            dot.setBackground(getDrawable(R.drawable.bg_dot));
            dot.setAlpha(i == selected ? 1f : 0.4f);
            dots.addView(dot);
        }
    }

    private void updateClock() {
        Date now = new Date();
        timeView.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
        dateView.setText(new SimpleDateFormat("EEEE، d MMMM", Locale.getDefault()).format(now));
    }

    @Override
    public void onBackPressed() {
        // launcher never exits on back
    }
}
