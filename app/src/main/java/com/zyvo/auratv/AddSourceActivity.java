package com.zyvo.auratv;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AddSourceActivity extends BaseActivity {
    private static final int PICK_M3U = 41;

    private Prefs prefs;
    private String editingId = "";
    private String type = "xtream";

    private EditText etName, etServer, etUser, etPass, etPortal, etMac, etM3u;
    private LinearLayout groupXtream, groupStalker, groupM3u;
    private TextView chipXtream, chipStalker, chipM3u, status, btnTest;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_source);
        prefs = new Prefs(this);
        editingId = getIntent().getStringExtra("id") == null ? "" : getIntent().getStringExtra("id");

        etName = findViewById(R.id.etName);
        etServer = findViewById(R.id.etServer);
        etUser = findViewById(R.id.etUser);
        etPass = findViewById(R.id.etPass);
        etPortal = findViewById(R.id.etPortal);
        etMac = findViewById(R.id.etMac);
        etM3u = findViewById(R.id.etM3uUrl);
        groupXtream = findViewById(R.id.groupXtream);
        groupStalker = findViewById(R.id.groupStalker);
        groupM3u = findViewById(R.id.groupM3u);
        chipXtream = findViewById(R.id.chipXtream);
        chipStalker = findViewById(R.id.chipStalker);
        chipM3u = findViewById(R.id.chipM3u);
        status = findViewById(R.id.tvStatus);
        btnTest = findViewById(R.id.btnTest);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        chipXtream.setOnClickListener(v -> setType("xtream"));
        chipStalker.setOnClickListener(v -> setType("stalker"));
        chipM3u.setOnClickListener(v -> setType("m3u"));
        findViewById(R.id.btnPickFile).setOnClickListener(v -> pickFile());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        btnTest.setOnClickListener(v -> test());

        String preset = getIntent().getStringExtra("type");
        if (preset != null) setType(preset);

        if (!editingId.isEmpty()) {
            Source s = prefs.sourceById(editingId);
            if (s != null) fill(s);
        }
    }

    private void fill(Source s) {
        setType(s.type);
        etName.setText(s.name);
        etServer.setText(s.url);
        etUser.setText(s.user);
        etPass.setText(s.pass);
        etPortal.setText(s.url);
        etMac.setText(s.mac);
        etM3u.setText(s.url);
    }

    private void setType(String t) {
        type = t;
        Ui.chip(chipXtream, "xtream".equals(t));
        Ui.chip(chipStalker, "stalker".equals(t));
        Ui.chip(chipM3u, "m3u".equals(t));
        groupXtream.setVisibility("xtream".equals(t) ? View.VISIBLE : View.GONE);
        groupStalker.setVisibility("stalker".equals(t) ? View.VISIBLE : View.GONE);
        groupM3u.setVisibility("m3u".equals(t) ? View.VISIBLE : View.GONE);
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, PICK_M3U);
        } catch (Exception e) {
            Ui.toast(this, getString(R.string.error));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_M3U || resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) throw new Exception("empty");
            String text = Fetcher.read(in);
            if (!text.contains("#EXTM3U") && !text.contains("#EXTINF")) {
                Ui.toast(this, getString(R.string.error));
                return;
            }
            File dir = new File(getFilesDir(), "playlists");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "m3u_" + System.currentTimeMillis() + ".m3u");
            try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
                w.write(text);
            }
            etM3u.setText(f.getAbsolutePath());
            if (etName.getText().toString().trim().isEmpty()) {
                String display = uri.getLastPathSegment();
                etName.setText(display == null ? "M3U" : display);
            }
            showStatus(getString(R.string.test_ok), true);
        } catch (Exception e) {
            showStatus(getString(R.string.error), false);
        }
    }

    private void showStatus(String msg, boolean ok) {
        status.setText(msg);
        status.setTextColor(getResources().getColor(ok ? R.color.ok : R.color.danger));
        status.setVisibility(View.VISIBLE);
    }

    /** Returns a validated source or null (and shows why). */
    private Source build() {
        Source s = new Source();
        s.id = editingId.isEmpty() ? "src_" + System.currentTimeMillis() : editingId;
        s.type = type;
        String name = etName.getText().toString().trim();

        if ("xtream".equals(type)) {
            String server = etServer.getText().toString().trim();
            String user = etUser.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            if (server.isEmpty() || user.isEmpty()) {
                showStatus(getString(R.string.error) + ": " + getString(R.string.server_url), false);
                return null;
            }
            if (!server.startsWith("http://") && !server.startsWith("https://")) server = "http://" + server;
            s.url = Source.cleanUrl(server);
            s.user = user;
            s.pass = pass;
        } else if ("stalker".equals(type)) {
            String portal = etPortal.getText().toString().trim();
            String mac = normalizeMac(etMac.getText().toString());
            if (portal.isEmpty()) {
                showStatus(getString(R.string.error) + ": " + getString(R.string.portal_url), false);
                return null;
            }
            if (mac.isEmpty()) {
                showStatus(getString(R.string.error) + ": " + getString(R.string.mac_address), false);
                return null;
            }
            if (!portal.startsWith("http://") && !portal.startsWith("https://")) portal = "http://" + portal;
            s.url = Source.cleanUrl(portal);
            s.mac = mac;
        } else {
            String link = etM3u.getText().toString().trim();
            if (link.isEmpty()) {
                showStatus(getString(R.string.error) + ": " + getString(R.string.file_hint), false);
                return null;
            }
            if (!link.startsWith("http://") && !link.startsWith("https://") && !link.startsWith("/")) {
                showStatus(getString(R.string.error) + ": " + getString(R.string.file_hint), false);
                return null;
            }
            s.url = link;
        }

        if (name.isEmpty()) name = s.typeLabel() + " " + (sources().size() + 1);
        s.name = name;
        return s;
    }

    private List<Source> sources() {
        return prefs.sources();
    }

    private static String normalizeMac(String input) {
        String hex = input == null ? "" : input.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();
        if (hex.length() == 10) hex = "001A79" + hex;
        if (hex.length() != 12) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i += 2) {
            if (i > 0) sb.append(':');
            sb.append(hex, i, i + 2);
        }
        return sb.toString();
    }

    private void test() {
        if (busy) return;
        Source s = build();
        if (s == null) return;
        busy = true;
        btnTest.setText(R.string.loading);
        Bg.work(() -> {
            if ("xtream".equals(s.type)) {
                XtreamClient c = new XtreamClient(s);
                c.check();
                return c.channels(Channel.KIND_LIVE).size();
            } else if ("stalker".equals(s.type)) {
                StalkerClient c = new StalkerClient(s);
                c.check();
                return c.channels().size();
            } else {
                String text = s.url.startsWith("http")
                        ? Fetcher.get(s.url)
                        : Fetcher.readFile(s.url.replace("file://", ""));
                return M3uParser.parse(text, s.id).size();
            }
        }, new Bg.Done<Integer>() {
            @Override
            public void ok(Integer count) {
                busy = false;
                btnTest.setText(R.string.test);
                showStatus(getString(R.string.test_ok) + " — " + count + " " + getString(R.string.live), true);
            }

            @Override
            public void fail(Exception e) {
                busy = false;
                btnTest.setText(R.string.test);
                showStatus(getString(R.string.error) + ": " + Ui.safe(e.getMessage()), false);
            }
        });
    }

    private void save() {
        Source s = build();
        if (s == null) return;
        prefs.upsert(s);
        prefs.setLastSourceId(s.id);
        Ui.toast(this, getString(R.string.source_saved));
        finish();
    }
}
