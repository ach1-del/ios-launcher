package com.zyvo.ioslauncher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.Holder> {

    public interface OnAppClickListener {
        void onAppClick(AppInfo app);
    }

    private final List<AppInfo> apps = new ArrayList<>();
    private final OnAppClickListener listener;

    public AppsAdapter(OnAppClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<AppInfo> newApps) {
        apps.clear();
        apps.addAll(newApps);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AppInfo app = apps.get(position);
        holder.icon.setImageDrawable(app.fallbackIcon);
        holder.label.setText(app.label);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAppClick(app);
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.appIcon);
            label = itemView.findViewById(R.id.appLabel);
        }
    }
}
