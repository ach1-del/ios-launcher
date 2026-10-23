package com.zyvo.auratv;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SourcesActivity extends BaseActivity {
    private Prefs prefs;
    private final List<Source> items = new ArrayList<>();
    private Adapter adapter;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sources);
        prefs = new Prefs(this);
        empty = findViewById(R.id.tvEmpty);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAdd).setOnClickListener(v -> startActivity(new Intent(this, AddSourceActivity.class)));

        RecyclerView rv = findViewById(R.id.rvSources);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter();
        rv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        items.clear();
        items.addAll(prefs.sources());
        adapter.notifyDataSetChanged();
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmDelete(Source s) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_source)
                .setMessage(s.name)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (d, w) -> {
                    prefs.deleteSource(s.id);
                    onResume();
                })
                .show();
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name;
            final TextView type;
            final ImageView delete;

            VH(View v) {
                super(v);
                icon = v.findViewById(R.id.ivIcon);
                name = v.findViewById(R.id.tvName);
                type = v.findViewById(R.id.tvType);
                delete = v.findViewById(R.id.btnDelete);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_source, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Source s = items.get(position);
            h.name.setText(s.name.isEmpty() ? s.typeLabel() : s.name);
            String host = s.url;
            try {
                String h2 = android.net.Uri.parse(s.url).getHost();
                if (h2 != null) host = h2;
            } catch (Exception ignored) {
            }
            h.type.setText(s.typeLabel() + "  ·  " + host);
            h.icon.setImageResource("stalker".equals(s.type) ? R.drawable.ic_live
                    : "m3u".equals(s.type) ? R.drawable.ic_link : R.drawable.ic_server);
            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(SourcesActivity.this, AddSourceActivity.class);
                i.putExtra("id", s.id);
                startActivity(i);
            });
            h.delete.setOnClickListener(v -> confirmDelete(s));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
