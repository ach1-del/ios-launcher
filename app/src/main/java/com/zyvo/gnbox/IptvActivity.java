package com.zyvo.gnbox;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class IptvActivity extends BaseActivity {
    private static final int PICK_M3U = 41;

    private EditText urlInput;
    private TextView status;
    private ListView list;
    private VideoView video;
    private LinearLayout playerBox;
    private final List<Ch> channels = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private static class Ch {
        final String name;
        final String url;
        Ch(String n, String u) { name = n; url = u; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = Ui.screen(this);
        root.addView(Ui.header(this, getString(R.string.iptv)));

        LinearLayout urlRow = Ui.row(this);
        urlRow.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        urlInput = Ui.edit(this, getString(R.string.playlist_url));
        urlRow.addView(urlInput, Ui.lpw(1f));
        Button load = Ui.btn(this, getString(R.string.load), true);
        LinearLayout.LayoutParams lp = Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(this, 8);
        urlRow.addView(load, lp);
        root.addView(urlRow);

        LinearLayout pickRow = Ui.row(this);
        Button pick = Ui.btn(this, getString(R.string.pick_file), false);
        pickRow.addView(pick);
        root.addView(pickRow);

        root.addView(Ui.hint(this, getString(R.string.iptv_hint)));

        status = Ui.status(this);
        root.addView(status);

        list = new ListView(this);
        list.setBackgroundColor(Color.TRANSPARENT);
        list.setDividerHeight(0);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView t = (TextView) super.getView(position, convertView, parent);
                t.setTextColor(getColor(R.color.text));
                t.setTextSize(15);
                t.setPadding(Ui.dp(getContext(), 8), Ui.dp(getContext(), 14),
                        Ui.dp(getContext(), 8), Ui.dp(getContext(), 14));
                return t;
            }
        };
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        playerBox = Ui.col(this);
        playerBox.setVisibility(View.GONE);
        video = new VideoView(this);
        video.setMediaController(new MediaController(this));
        playerBox.addView(video, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        Button backToList = Ui.btn(this, getString(R.string.back_to_list), false);
        LinearLayout.LayoutParams blp = Ui.lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = Ui.dp(this, 8);
        playerBox.addView(backToList, blp);
        root.addView(playerBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        load.setOnClickListener(v -> {
            String u = urlInput.getText().toString().trim();
            if (!u.isEmpty()) loadUrl(u);
        });
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, PICK_M3U);
        });
        backToList.setOnClickListener(v -> stopPlayback());
        list.setOnItemClickListener((parent, view, position, id) -> play(channels.get(position)));

        loadSample();
    }

    private void loadSample() {
        try (InputStream in = getAssets().open("sample.m3u")) {
            apply(parse(readAll(in)), false);
        } catch (Exception e) {
            status.setText(R.string.no_channels);
        }
    }

    private void loadUrl(final String url) {
        status.setText(R.string.loading);
        new Thread(() -> {
            String text = null;
            String error = null;
            try {
                text = fetch(url);
            } catch (Exception e) {
                error = e.getMessage();
            }
            final String body = text;
            final String err = error;
            runOnUiThread(() -> {
                if (body == null) {
                    status.setText(getString(R.string.net_error) + (err != null ? " (" + err + ")" : ""));
                } else {
                    apply(parse(body), true);
                }
            });
        }).start();
    }

    private void apply(List<Ch> parsed, boolean fromUrl) {
        channels.clear();
        channels.addAll(parsed);
        adapter.clear();
        for (Ch c : channels) adapter.add(c.name);
        adapter.notifyDataSetChanged();
        if (channels.isEmpty()) {
            status.setText(R.string.no_channels);
        } else {
            status.setText(channels.size() + " " + getString(R.string.channels));
        }
    }

    private String fetch(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "GN-RS8-Box/1.0");
        int code = c.getResponseCode();
        if (code >= 400) throw new Exception("HTTP " + code);
        return readAll(c.getInputStream());
    }

    private String readAll(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) sb.append(line).append('\n');
        r.close();
        return sb.toString();
    }

    private List<Ch> parse(String text) {
        List<Ch> out = new ArrayList<>();
        if (text == null) return out;
        String pending = null;
        int n = 0;
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#EXTINF")) {
                int comma = line.lastIndexOf(',');
                pending = comma >= 0 && comma + 1 < line.length()
                        ? line.substring(comma + 1).trim() : ("Channel " + (n + 1));
            } else if (!line.startsWith("#")) {
                n++;
                out.add(new Ch(pending != null ? pending : ("Channel " + n), line));
                pending = null;
            }
        }
        return out;
    }

    private void play(Ch ch) {
        if (ch.url.startsWith("http") || ch.url.startsWith("rtsp")) {
            list.setVisibility(View.GONE);
            playerBox.setVisibility(View.VISIBLE);
            status.setText(ch.name);
            video.setVideoURI(Uri.parse(ch.url));
            video.setOnPreparedListener(mp -> video.start());
            video.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, R.string.stream_error, Toast.LENGTH_LONG).show();
                stopPlayback();
                return true;
            });
        } else {
            Toast.makeText(this, R.string.stream_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPlayback() {
        try {
            video.stopPlayback();
        } catch (Exception ignored) {}
        playerBox.setVisibility(View.GONE);
        list.setVisibility(View.VISIBLE);
        status.setText(channels.size() + " " + getString(R.string.channels));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_M3U && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try (InputStream in = getContentResolver().openInputStream(data.getData())) {
                apply(parse(readAll(in)), false);
            } catch (Exception e) {
                status.setText(R.string.net_error);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (playerBox.getVisibility() == View.VISIBLE) {
            stopPlayback();
        } else {
            super.onBackPressed();
        }
    }
}
