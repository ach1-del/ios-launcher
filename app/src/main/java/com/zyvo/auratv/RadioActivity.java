package com.zyvo.auratv;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RadioActivity extends BaseActivity {
    private static class Station {
        String name = "";
        String url = "";
        String tags = "";
        String country = "";
    }

    private final List<Station> stations = new ArrayList<>();
    private Adapter adapter;
    private EditText etSearch;
    private View progress, nowBar;
    private TextView tvNow, btnPlay;
    private ExoPlayer player;
    private int playing = -1;
    private final Handler debounce = new Handler(Looper.getMainLooper());
    private Runnable pending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio);

        etSearch = findViewById(R.id.etSearch);
        progress = findViewById(R.id.progress);
        nowBar = findViewById(R.id.nowBar);
        tvNow = findViewById(R.id.tvNow);
        btnPlay = findViewById(R.id.btnPlay);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvRadio);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter();
        rv.setAdapter(adapter);

        player = new ExoPlayer.Builder(this).build();
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(), true);
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Ui.toast(RadioActivity.this, getString(R.string.player_error));
                btnPlay.setText(R.string.play);
            }
        });

        btnPlay.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
                btnPlay.setText(R.string.play);
            } else if (player.getMediaItemCount() > 0) {
                player.play();
                btnPlay.setText(R.string.pause);
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadNow();
                return true;
            }
            return false;
        });
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (pending != null) debounce.removeCallbacks(pending);
                pending = RadioActivity.this::loadNow;
                debounce.postDelayed(pending, 650);
            }
        });

        load("");
    }

    private void loadNow() {
        load(etSearch.getText().toString().trim());
    }

    private void load(String query) {
        progress.setVisibility(View.VISIBLE);
        Bg.work(() -> {
            String base = "https://de1.api.radio-browser.info/json/stations/";
            String url;
            if (query.isEmpty()) {
                url = base + "topclick/40?hidebroken=true";
            } else {
                url = base + "search?limit=60&hidebroken=true&order=clickcount&reverse=true&name="
                        + URLEncoder.encode(query, "UTF-8");
            }
            Map<String, String> h = new HashMap<>();
            h.put("User-Agent", Fetcher.UA);
            String body = Fetcher.get(url, h);
            JSONArray arr = new JSONArray(body);
            List<Station> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;
                String u = o.optString("url_resolved", o.optString("url", ""));
                if (u.isEmpty()) continue;
                Station st = new Station();
                st.name = o.optString("name", "Radio");
                st.url = u;
                st.tags = o.optString("tags", "");
                st.country = o.optString("country", "");
                out.add(st);
            }
            return out;
        }, new Bg.Done<List<Station>>() {
            @Override
            public void ok(List<Station> value) {
                progress.setVisibility(View.GONE);
                stations.clear();
                stations.addAll(value);
                adapter.notifyDataSetChanged();
                if (stations.isEmpty()) Ui.toast(RadioActivity.this, getString(R.string.radio_error));
            }

            @Override
            public void fail(Exception e) {
                progress.setVisibility(View.GONE);
                Ui.toast(RadioActivity.this, getString(R.string.radio_error));
            }
        });
    }

    private void play(int index) {
        Station st = stations.get(index);
        playing = index;
        adapter.notifyDataSetChanged();
        player.setMediaItem(MediaItem.fromUri(st.url));
        player.prepare();
        player.play();
        nowBar.setVisibility(View.VISIBLE);
        tvNow.setText(st.name);
        btnPlay.setText(R.string.pause);
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final TextView name, tags;

            VH(View v) {
                super(v);
                name = v.findViewById(R.id.tvName);
                tags = v.findViewById(R.id.tvTags);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_radio, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Station st = stations.get(position);
            h.name.setText(st.name);
            StringBuilder meta = new StringBuilder();
            if (!st.country.isEmpty()) meta.append(st.country);
            if (!st.tags.isEmpty()) {
                if (meta.length() > 0) meta.append(" · ");
                String t = st.tags.replace(",", " · ");
                if (t.length() > 60) t = t.substring(0, 60);
                meta.append(t);
            }
            h.tags.setText(meta.toString());
            h.itemView.setAlpha(position == playing ? 0.65f : 1f);
            h.itemView.setOnClickListener(v -> {
                int pos = h.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) play(pos);
            });
        }

        @Override
        public int getItemCount() {
            return stations.size();
        }
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
