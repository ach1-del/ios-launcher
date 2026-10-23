package com.zyvo.auratv;

import org.json.JSONException;
import org.json.JSONObject;

/** One playable channel: live stream, movie or series episode. */
public class Channel {
    public static final int KIND_LIVE = 0;
    public static final int KIND_MOVIE = 1;

    public String id = "";
    public String name = "";
    public String logo = "";
    public String group = "";
    public String url = "";
    public String cmd = "";
    public String sourceId = "";
    public int kind = KIND_LIVE;

    public JSONObject json() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("name", name);
            o.put("logo", logo);
            o.put("group", group);
            o.put("url", url);
            o.put("cmd", cmd);
            o.put("sourceId", sourceId);
            o.put("kind", kind);
        } catch (JSONException ignored) {
        }
        return o;
    }

    public static Channel from(JSONObject o) {
        Channel c = new Channel();
        c.id = o.optString("id", "");
        c.name = o.optString("name", "");
        c.logo = o.optString("logo", "");
        c.group = o.optString("group", "");
        c.url = o.optString("url", "");
        c.cmd = o.optString("cmd", "");
        c.sourceId = o.optString("sourceId", "");
        c.kind = o.optInt("kind", 0);
        return c;
    }
}
