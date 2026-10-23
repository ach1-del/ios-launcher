package com.zyvo.auratv;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Xtream Codes player API client. */
public class XtreamClient {
    private final String base;
    private final String user;
    private final String pass;

    public XtreamClient(Source s) {
        String b = Source.cleanUrl(s.url);
        int cut = b.indexOf("/player_api.php");
        if (cut > 0) b = b.substring(0, cut);
        cut = b.indexOf("/get.php");
        if (cut > 0) b = b.substring(0, cut);
        if (!b.startsWith("http://") && !b.startsWith("https://")) b = "http://" + b;
        this.base = b;
        this.user = s.user;
        this.pass = s.pass;
    }

    private String api(String query) throws IOException {
        StringBuilder u = new StringBuilder(base).append("/player_api.php?username=")
                .append(URLEncoder.encode(user)).append("&password=").append(URLEncoder.encode(pass));
        if (query != null && !query.isEmpty()) u.append("&").append(query);
        return Fetcher.get(u.toString());
    }

    /** Throws when the credentials are rejected. */
    public void check() throws IOException {
        JSONObject o = new JSONObject(api(null));
        JSONObject info = o.optJSONObject("user_info");
        if (info == null) throw new IOException("Unexpected server response");
        String auth = info.optString("auth", "1");
        String status = info.optString("status", "Active");
        if ("0".equals(auth) || (!status.isEmpty() && !status.equalsIgnoreCase("Active")
                && !status.equalsIgnoreCase("Trial"))) {
            throw new IOException("Login rejected");
        }
    }

    public List<Channel> channels(int kind) throws IOException {
        boolean vod = kind == Channel.KIND_MOVIE;
        String catAction = vod ? "get_vod_categories" : "get_live_categories";
        String listAction = vod ? "get_vod_streams" : "get_live_streams";
        if (vod) listAction = "get_vod_streams";

        Map<String, String> catNames = new HashMap<>();
        try {
            JSONArray cats = new JSONArray(api("action=" + catAction));
            for (int i = 0; i < cats.length(); i++) {
                JSONObject c = cats.getJSONObject(i);
                catNames.put(c.optString("category_id"), c.optString("category_name", ""));
            }
        } catch (Exception ignored) {
        }

        JSONArray arr = new JSONArray(api("action=" + listAction));
        List<Channel> out = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            Channel ch = new Channel();
            ch.kind = kind;
            String sid = o.optString("stream_id", o.optString("id", ""));
            ch.name = o.optString("name", "Channel " + sid);
            ch.logo = o.optString("stream_icon", o.optString("cover_big", ""));
            String catId = o.optString("category_id", "");
            ch.group = catNames.containsKey(catId) ? catNames.get(catId) : "";
            ch.id = "x" + sid;
            if (vod) {
                String ext = o.optString("container_extension", "");
                if (ext.isEmpty()) ext = "mp4";
                ch.url = base + "/movie/" + user + "/" + pass + "/" + sid + "." + ext;
            } else {
                ch.url = base + "/live/" + user + "/" + pass + "/" + sid + ".m3u8";
            }
            out.add(ch);
        }
        return out;
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }
}
