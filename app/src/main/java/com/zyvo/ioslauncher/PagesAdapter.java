package com.zyvo.ioslauncher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Splits the full app list into pages of 8 (4 columns x 2 rows) for the home screen ViewPager. */
public class PagesAdapter extends RecyclerView.Adapter<PagesAdapter.PageHolder> {

    static final int PER_PAGE = 8;

    private final List<List<AppInfo>> pages = new ArrayList<>();
    private final AppsAdapter.OnAppClickListener listener;

    public PagesAdapter(AppsAdapter.OnAppClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<AppInfo> apps) {
        pages.clear();
        for (int i = 0; i < apps.size(); i += PER_PAGE) {
            pages.add(new ArrayList<>(apps.subList(i, Math.min(i + PER_PAGE, apps.size()))));
        }
        notifyDataSetChanged();
    }

    public int pageCount() {
        return pages.size();
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_page, parent, false);
        return new PageHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        AppsAdapter adapter = new AppsAdapter(listener);
        adapter.submit(pages.get(position));
        holder.grid.setAdapter(adapter);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageHolder extends RecyclerView.ViewHolder {
        final RecyclerView grid;

        PageHolder(@NonNull View itemView) {
            super(itemView);
            grid = itemView.findViewById(R.id.pageGrid);
            grid.setLayoutManager(new GridLayoutManager(itemView.getContext(), 4));
            grid.setHasFixedSize(true);
        }
    }
}
