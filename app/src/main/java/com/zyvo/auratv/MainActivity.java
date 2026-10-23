package com.zyvo.auratv;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

public class MainActivity extends BaseActivity {
    private Prefs prefs;
    private final List<Source> sources = new ArrayList<>();
    private Source current;
    private int mode = 0; // 0 live, 1 movies, 2 favorites
    private int loadGen = 0;
    private String category = "";
    private String sourcesSig = "";

    private final List<Channel> all = new ArrayList<>();
    private final List<Channel> shown = new ArrayList<>();
    private final Map<String, List<Channel>> cache = new HashMap<>();

    private EditText etSearch;
    private LinearLayout modeRow;
    private TextView empty, onboarding;
    private RecyclerView rvSources, rvCategories, rvChannels;
    private ChipAdapter srcAdapter, catAdapter;
    private ChannelAdapter chAdapter;
    private final List<TextView> modeChips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = new Prefs(this);

        etSearch = findViewById(R.id.etSearch);
        modeRow = findViewById(R.id.modeRow);
        empty = findViewById(R.id.tvEmpty);
        onboarding = findViewById(R.id.onboarding);
        rvSources = findViewById(R.id.rvSources);
        rvCategories = findViewById(R.id.rvCategories);
        rvChannels = findViewById(R.id.rvChannels);

        rvSources.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvCategories.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvChannels.setLayoutManager(new LinearLayoutManager(this));

        srcAdapter = new ChipAdapter();
        srcAdapter.onPick = index -> {
            if (index < 0 || index >= sources.size()) return;
            current = sources.get(index);
            prefs.setLastSourceId(current.id);
            category = "";
            srcAdapter.selected = index;
            srcAdapter.notifyDataSetChanged();
            load();
        };
        rvSources.setAdapter(srcAdapter);

        catAdapter = new ChipAdapter();
        catAdapter.onPick = index -> {
            category = index <= 0 ? "" : catAdapter.items.get(index);
            catAdapter.selected = index;
            catAdapter.notifyDataSetChanged();
            applyFilter();
        };
        rvCategories.setAdapter(catAdapter);

        chAdapter = new ChannelAdapter();
        rvChannels.setAdapter(chAdapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilter();
            }
        });

        findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btnAddSource).setOnClickListener(v -> startActivity(new Intent(this, AddSourceActivity.class)));
        findViewById(R.id.btnManage).setOnClickListener(v -> startActivity(new Intent(this, SourcesActivity.class)));

        findViewById(R.id.btnXtream).setOnClickListener(v -> addOfType("xtream"));
        findViewById(R.id.btnStalker).setOnClickListener(v -> addOfType("stalker"));
        findViewById(R.id.btnM3u).setOnClickListener(v -> addOfType("m3u"));

        findViewById(R.id.quickRadio).setOnClickListener(v -> startActivity(new Intent(this, RadioActivity.class)));
        findViewById(R.id.quickYoutube).setOnClickListener(v -> Ui.openWeb(this, "https://m.youtube.com", getString(R.string.youtube)));
        findViewById(R.id.quickBrowser).setOnClickListener(v -> Ui.openWeb(this, "https://www.google.com", getString(R.string.browser)));

        buildModeChips();
    }

    private void addOfType(String type) {
        Intent i = new Intent(this, AddSourceActivity.class);
        i.putExtra("type", type);
        startActivity(i);
    }

    private void buildModeChips() {
        modeRow.removeAllViews();
        modeChips.clear();
        String[] labels = {getString(R.string.live), getString(R.string.movies), getString(R.string.favorites)};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.rightMargin = Ui.dp(this, 8);
            tv.setLayoutParams(p);
            tv.setText(labels[i]);
            tv.setTextSize(13);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(Ui.dp(this, 16), Ui.dp(this, 9), Ui.dp(this, 16), Ui.dp(this, 9));
            tv.setOnClickListener(v -> setMode(index));
            modeRow.addView(tv);
            modeChips.add(tv);
        }
        refreshModeChips();
    }

    private void refreshModeChips() {
        for (int i = 0; i < modeChips.size(); i++) Ui.chip(modeChips.get(i), i == mode);
    }

    private void setMode(int index) {
        if (mode == index) return;
        mode = index;
        category = "";
        refreshModeChips();
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<Source> fresh = prefs.sources();
        String sig = signature(fresh);
        boolean changed = !sig.equals(sourcesSig);
        sources.clear();
        sources.addAll(fresh);
        sourcesSig = sig;
        if (changed) cache.clear();

        if (current == null || prefs.sourceById(current.id) == null) {
            Source last = prefs.sourceById(prefs.lastSourceId());
            current = last != null ? last : (sources.isEmpty() ? null : sources.get(0));
            if (current != null) prefs.setLastSourceId(current.id);
            changed = true;
        }

        List<String> names = new ArrayList<>();
        int selected = -1;
        for (int i = 0; i < sources.size(); i++) {
            Source s = sources.get(i);
            names.add(s.name.isEmpty() ? s.typeLabel() : s.name);
            if (current != null && current.id.equals(s.id)) selected = i;
        }
        srcAdapter.setData(names, selected);
        onboarding.setVisibility(sources.isEmpty() ? View.VISIBLE : View.GONE);

        if (mode == 2) load();
        else if (changed || all.isEmpty()) load();
    }

    private String signature(List<Source> list) {
        StringBuilder b = new StringBuilder();
        for (Source s : list) {
            b.append(s.id).append(s.url).append(s.type).append(s.user).append(s.mac).append(';');
        }
        return b.toString();
    }

    private void load() {
        final int gen = ++loadGen;

        if (mode == 2) {
            all.clear();
            all.addAll(prefs.favorites());
            category = "";
            buildCategories();
            applyFilter();
            return;
        }
        if (current == null) {
            all.clear();
            category = "";
            buildCategories();
            applyFilter();
            return;
        }

        final Source src = current;
        final String key = src.id + "#" + mode + "#" + src.url + "#" + src.user;
        List<Channel> cached = cache.get(key);
        if (cached != null) {
            apply(cached);
            return;
        }

        showLoading();
        Bg.work(() -> fetch(src, mode), new Bg.Done<List<Channel>>() {
            @Override
            public void ok(List<Channel> value) {
                if (gen != loadGen) return;
                cache.put(key, value);
                apply(value);
            }

            @Override
            public void fail(Exception e) {
                if (gen != loadGen) return;
                all.clear();
                shown.clear();
                chAdapter.notifyDataSetChanged();
                catAdapter.setData(new ArrayList<>(), -1);
                empty.setText(getString(R.string.error) + "\n" + Ui.safe(e.getMessage()));
                empty.setVisibility(View.VISIBLE);
            }
        });
    }

    private List<Channel> fetch(Source src, int which) throws Exception {
        List<Channel> out = new ArrayList<>();
        if ("xtream".equals(src.type)) {
            XtreamClient c = new XtreamClient(src);
            out = c.channels(which == 1 ? Channel.KIND_MOVIE : Channel.KIND_LIVE);
        } else if ("stalker".equals(src.type)) {
            if (which == 1) return out;
            out = new StalkerClient(src).channels();
        } else {
            String text = src.url.startsWith("http")
                    ? Fetcher.get(src.url)
                    : Fetcher.readFile(src.url.replace("file://", ""));
            List<Channel> parsed = M3uParser.parse(text, src.id);
            for (Channel ch : parsed) {
                if (which == 1) {
                    if (ch.kind == Channel.KIND_MOVIE) out.add(ch);
                } else {
                    if (ch.kind == Channel.KIND_LIVE) out.add(ch);
                }
            }
        }
        for (Channel ch : out) ch.sourceId = src.id;
        return out;
    }

    private void apply(List<Channel> list) {
        all.clear();
        all.addAll(list);
        category = "";
        buildCategories();
        applyFilter();
        rvChannels.scrollToPosition(0);
    }

    private void buildCategories() {
        TreeSet<String> groups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Channel ch : all) if (ch.group != null && !ch.group.isEmpty()) groups.add(ch.group);
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.all));
        labels.addAll(groups);
        int selected = 0;
        catAdapter.setData(labels, selected);
    }

    private void showLoading() {
        shown.clear();
        chAdapter.notifyDataSetChanged();
        catAdapter.setData(new ArrayList<>(), -1);
        empty.setText(R.string.loading);
        empty.setVisibility(View.VISIBLE);
    }

    private void applyFilter() {
        String q = etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        shown.clear();
        for (Channel ch : all) {
            if (!category.isEmpty() && !category.equals(ch.group)) continue;
            if (!q.isEmpty() && !ch.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            shown.add(ch);
        }
        chAdapter.notifyDataSetChanged();
        if (sources.isEmpty() && mode != 2) {
            empty.setVisibility(View.GONE);
            return;
        }
        if (shown.isEmpty()) {
            if (!q.isEmpty()) empty.setText(R.string.empty_search);
            else if (mode == 2) empty.setText(R.string.empty_favorites);
            else empty.setText(R.string.no_channels);
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
        }
    }

    // ---------------- adapters ----------------

    private class ChipAdapter extends RecyclerView.Adapter<ChipAdapter.VH> {
        List<String> items = new ArrayList<>();
        int selected = -1;
        OnPick onPick;

        void setData(List<String> list, int sel) {
            items = new ArrayList<>(list);
            selected = sel;
            notifyDataSetChanged();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView chip;

            VH(TextView v) {
                super(v);
                chip = v;
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category, parent, false);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            h.chip.setText(items.get(position));
            Ui.chip(h.chip, position == selected);
            h.chip.setOnClickListener(v -> {
                int pos = h.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                selected = pos;
                notifyDataSetChanged();
                if (onPick != null) onPick.pick(pos);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    interface OnPick {
        void pick(int index);
    }

    private class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final ImageView logo, fav;
            final TextView name, group;

            VH(View v) {
                super(v);
                logo = v.findViewById(R.id.ivLogo);
                fav = v.findViewById(R.id.btnFav);
                name = v.findViewById(R.id.tvName);
                group = v.findViewById(R.id.tvGroup);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Channel ch = shown.get(position);
            h.name.setText(ch.name);
            String sub = ch.group;
            if (sub == null || sub.isEmpty()) {
                sub = getString(ch.kind == Channel.KIND_MOVIE ? R.string.movies : R.string.live);
            }
            h.group.setText(sub);
            int fallback = ch.kind == Channel.KIND_MOVIE ? R.drawable.ic_movie : R.drawable.ic_live;
            Thumbs.load(h.logo, ch.logo, fallback);
            h.fav.setImageResource(prefs.isFavorite(ch.id) ? R.drawable.ic_star_filled : R.drawable.ic_star);

            h.itemView.setOnClickListener(v -> {
                prefs.addRecent(ch);
                Ui.openPlayer(MainActivity.this, ch, ch.sourceId);
            });
            h.fav.setOnClickListener(v -> {
                boolean fav = prefs.toggleFavorite(ch);
                Ui.toast(MainActivity.this, getString(fav ? R.string.added_fav : R.string.removed_fav));
                int pos = h.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) notifyItemChanged(pos);
                if (mode == 2) load();
            });
        }

        @Override
        public int getItemCount() {
            return shown.size();
        }
    }
}
