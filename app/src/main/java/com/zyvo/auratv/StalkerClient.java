package com.zyvo.auratv;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Minimal Stalker/MAG portal client (handshake + channel list + link creation). */
public class StalkerClient {
    private final String base;
    private final String mac;
    private String token = "";

    public StalkerClient(Source s) {
        String b = Source.cleanUrl(s.url);
        if (b.endsWith("/portal.php")) b = b.substring(0, b.length() - "/portal.php".length());
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (!b.startsWith("http://") && !b.startsWith("https://")) b = "http://" + b;
        this.base = b;
        this.mac = s.mac == null ? "" : s.mac.trim().toUpperCase();
    }

    private Map<String, String> headers() {
        Map<String, String> h = new HashMap<>();
        h.put("Cookie", "mac=" + mac + "; stb_lang=en; timezone=Europe/London");
        if (!token.isEmpty()) h.put("Authorization", "Bearer " + token);
        h.put("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 2 rev: 250 Safari/533.3");
        h.put("X-User-Agent", "Model: MAG250; Link: WiFi");
        h.put("Referer", base + "/");
        h.put("Accept", "*/*");
        return h;
    }

    private String raw(String params) throws IOException, org.json.JSONException {
        return Fetcher.get(base + "/portal.php?" + params, headers());
    }

    private JSONObject call(String params) throws IOException, org.json.JSONException {
        String body = raw(params);
        int start = body.indexOf('{');
        if (start > 0) body = body.substring(start);
        return new JSONObject(body);
    }

    public void handshake() throws IOException, org.json.JSONException {
        token = "";
        JSONObject r = call("type=stb&action=handshake&token=&prehash=0");
        JSONObject js = r.optJSONObject("js");
        if (js != null) {
            token = js.optString("token", "");
        }
        if (token.isEmpty()) {
            token = r.optString("token", "");
        }
        if (token.isEmpty()) throw new IOException("Portal handshake failed");
    }

    public void check() throws IOException, org.json.JSONException {
        handshake();
        JSONObject r = call("type=itv&action=get_genres");
        if (r.optJSONArray("js") == null && r.optJSONObject("js") == null) {
            throw new IOException("Portal rejected the login");
        }
    }

    public List<Channel> channels() throws IOException, org.json.JSONException {
        handshake();
        Map<String, String> genres = new HashMap<>();
        try {
            JSONObject g = call("type=itv&action=get_genres");
            JSONArray arr = g.optJSONArray("js");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o != null) genres.put(o.optString("id"), o.optString("title", ""));
                }
            }
        } catch (Exception ignored) {
        }

        JSONObject r = call("type=itv&action=get_all_channels");
        JSONObject js = r.optJSONObject("js");
        JSONArray data = null;
        if (js != null) data = js.optJSONArray("data");
        if (data == null) data = r.optJSONArray("js");

        List<Channel> out = new ArrayList<>();
        if (data == null) return out;
        for (int i = 0; i < data.length(); i++) {
            JSONObject o = data.optJSONObject(i);
            if (o == null) continue;
            Channel c = new Channel();
            String id = o.optString("id", o.optString("ch_id", ""));
            if (id.isEmpty()) continue;
            c.name = o.optString("name", "Channel " + id);
            c.logo = o.optString("logo", o.optString("tv_icon", ""));
            c.group = genres.get(o.optString("tv_genre_id", ""));
            if (c.group == null) c.group = "";
            c.cmd = o.optString("cmd", "");
            c.id = "s" + id;
            c.kind = Channel.KIND_LIVE;
            c.url = base + "/play/live.php?mac=" + mac + "&stream=" + id + "&extension=m3u8";
            out.add(c);
        }
        return out;
    }

    /** Resolves a real stream URL for the portal. Returns "" when unavailable. */
    public String createLink(String cmd) throws IOException, org.json.JSONException {
        if (token.isEmpty()) handshake();
        String q = "type=itv&action=create_link&cmd=" + URLEncoder.encode(cmd, "UTF-8")
                + "&series=0&forced_storage=0&disable_ad=0&download=0&force_ch_link_check=0";
        JSONObject r = call(q);
        Object js = r.opt("js");
        String link = "";
        if (js instanceof JSONObject) {
            link = ((JSONObject) js).optString("cmd", "");
        } else if (js instanceof String) {
            link = (String) js;
        }
        link = link.trim();
        if (link.startsWith("ffmpeg ")) link = link.substring(7).trim();
        else if (link.startsWith("auto ")) link = link.substring(5).trim();
        return link;
    }
}
