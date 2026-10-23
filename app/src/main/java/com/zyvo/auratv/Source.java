package com.zyvo.auratv;

import org.json.JSONException;
import org.json.JSONObject;

/** A playlist connection: Xtream Codes, Stalker portal or M3U. */
public class Source {
    public String id = "";
    public String name = "";
    public String type = "xtream"; // xtream | stalker | m3u
    public String url = "";
    public String user = "";
    public String pass = "";
    public String mac = "";
    public String cmd = ""; // stalker stream command prefix, per channel

    public static Source from(JSONObject o) {
        Source s = new Source();
        s.id = o.optString("id", "");
        s.name = o.optString("name", "");
        s.type = o.optString("type", "xtream");
        s.url = o.optString("url", "");
        s.user = o.optString("user", "");
        s.pass = o.optString("pass", "");
        s.mac = o.optString("mac", "");
        s.cmd = o.optString("cmd", "");
        return s;
    }

    public JSONObject json() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("name", name);
            o.put("type", type);
            o.put("url", url);
            o.put("user", user);
            o.put("pass", pass);
            o.put("mac", mac);
            o.put("cmd", cmd);
        } catch (JSONException ignored) {
        }
        return o;
    }

    public String typeLabel() {
        if ("stalker".equals(type)) return "Stalker";
        if ("m3u".equals(type)) return "M3U";
        return "Xtream";
    }

    public static String cleanUrl(String u) {
        if (u == null) return "";
        u = u.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }
}
