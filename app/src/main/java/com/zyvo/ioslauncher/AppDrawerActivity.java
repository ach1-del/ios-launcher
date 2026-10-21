package com.zyvo.ioslauncher;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Full app list with live search — opened from the home search bar. */
public class AppDrawerActivity extends Activity {

    private final List<AppInfo> allApps = new ArrayList<>();
    private AppsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        RecyclerView grid = findViewById(R.id.drawerGrid);
        grid.setLayoutManager(new GridLayoutManager(this, 4));
        adapter = new AppsAdapter(app -> {
            launchApp(app);
            finish();
        });
        grid.setAdapter(adapter);

        EditText search = findViewById(R.id.drawerSearch);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });

        AppRepository.loadAsync(this, apps -> {
            allApps.clear();
            allApps.addAll(apps);
            runOnUiThread(() -> filter(search.getText().toString()));
        });
    }

    private void filter(String query) {
        String q = query.trim().toLowerCase(Locale.getDefault());
        if (q.isEmpty()) {
            adapter.submit(allApps);
            return;
        }
        List<AppInfo> matched = new ArrayList<>();
        for (AppInfo app : allApps) {
            if (app.label.toLowerCase(Locale.getDefault()).contains(q)
                    || app.packageName.toLowerCase(Locale.getDefault()).contains(q)) {
                matched.add(app);
            }
        }
        adapter.submit(matched);
    }

    private void launchApp(AppInfo app) {
        android.content.Intent launch = getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (launch != null) {
            launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        }
    }
}
