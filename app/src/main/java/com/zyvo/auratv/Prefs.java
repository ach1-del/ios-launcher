package com.zyvo.auratv;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Small, dependency-free store for playlists, favorites and settings. */
public class Prefs {
    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("aura_tv", Context.MODE_PRIVATE);
    }

    // ---------- sources ----------

    public List<Source> sources() {
        List<Source> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp.getString("sources", "[]"));
            for (int i = 0; i < a.length(); i++) {
                out.add(Source.from(a.getJSONObject(i)));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public void saveSources(List<Source> list) {
        JSONArray a = new JSONArray();
        for (Source s : list) a.put(s.json());
        sp.edit().putString("sources", a.toString()).apply();
    }

    public Source sourceById(String id) {
        if (id == null) return null;
        for (Source s : sources()) if (id.equals(s.id)) return s;
        return null;
    }

    public void upsert(Source s) {
        List<Source> list = sources();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(s.id)) {
                list.set(i, s);
                saveSources(list);
                return;
            }
        }
        list.add(s);
        saveSources(list);
    }

    public void deleteSource(String id) {
        List<Source> list = sources();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).id.equals(id)) list.remove(i);
        }
        saveSources(list);
    }

    public String lastSourceId() {
        return sp.getString("last_source", "");
    }

    public void setLastSourceId(String id) {
        sp.edit().putString("last_source", id).apply();
    }

    // ---------- favorites / recent ----------

    public List<Channel> favorites() {
        List<Channel> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp.getString("favorites", "[]"));
            for (int i = 0; i < a.length(); i++) out.add(Channel.from(a.getJSONObject(i)));
        } catch (Exception ignored) {
        }
        return out;
    }

    private void saveFavorites(List<Channel> list) {
        JSONArray a = new JSONArray();
        for (Channel c : list) a.put(c.json());
        sp.edit().putString("favorites", a.toString()).apply();
    }

    public boolean isFavorite(String id) {
        for (Channel c : favorites()) if (c.id.equals(id)) return true;
        return false;
    }

    /** @return true when the channel is now a favorite. */
    public boolean toggleFavorite(Channel c) {
        List<Channel> list = favorites();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(c.id)) {
                list.remove(i);
                saveFavorites(list);
                return false;
            }
        }
        list.add(0, c);
        saveFavorites(list);
        return true;
    }

    public List<Channel> recent() {
        List<Channel> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp.getString("recent", "[]"));
            for (int i = 0; i < a.length(); i++) out.add(Channel.from(a.getJSONObject(i)));
        } catch (Exception ignored) {
        }
        return out;
    }

    public void addRecent(Channel c) {
        List<Channel> list = recent();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).id.equals(c.id)) list.remove(i);
        }
        list.add(0, c);
        while (list.size() > 30) list.remove(list.size() - 1);
        JSONArray a = new JSONArray();
        for (Channel ch : list) a.put(ch.json());
        sp.edit().putString("recent", a.toString()).apply();
    }

    public void clearFavorites() {
        sp.edit().putString("favorites", "[]").apply();
    }

    // ---------- settings ----------

    public String lang() {
        return sp.getString("lang", "en");
    }

    public void setLang(String lang) {
        sp.edit().putString("lang", lang).apply();
    }

    public int aspect() {
        return sp.getInt("aspect", 0);
    }

    public void setAspect(int mode) {
        sp.edit().putInt("aspect", mode).apply();
    }
}
