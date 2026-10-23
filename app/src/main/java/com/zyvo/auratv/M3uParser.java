package com.zyvo.auratv;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses #EXTM3U playlists (live channels and VOD entries). */
public final class M3uParser {
    private static final Pattern ATTR = Pattern.compile("([A-Za-z0-9_-]+)=\"([^\"]*)\"");

    private M3uParser() {
    }

    public static List<Channel> parse(String text, String sourceId) {
        List<Channel> out = new ArrayList<>();
        if (text == null) return out;
        String[] lines = text.split("\\r?\\n");
        String name = null, logo = "", group = "", tvgId = "";
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#EXTINF")) {
                String afterComma = line;
                int comma = line.lastIndexOf(',');
                if (comma >= 0) {
                    afterComma = line.substring(comma + 1).trim();
                }
                name = afterComma.isEmpty() ? null : afterComma;
                logo = "";
                group = "";
                tvgId = "";
                Matcher m = ATTR.matcher(line);
                while (m.find()) {
                    String k = m.group(1).toLowerCase(Locale.ROOT);
                    String v = m.group(2);
                    if (k.equals("tvg-logo") || k.equals("logo")) logo = v;
                    else if (k.equals("group-title")) group = v;
                    else if (k.equals("tvg-id")) tvgId = v;
                }
            } else if (line.startsWith("#EXTGRP:")) {
                group = line.substring(8).trim();
            } else if (!line.startsWith("#") && name != null) {
                Channel c = new Channel();
                c.name = name;
                c.logo = logo;
                c.group = group;
                c.url = line;
                c.kind = guessKind(group, line);
                c.id = "m" + safeId(sourceId) + "_" + (tvgId.isEmpty() ? Math.abs(line.hashCode()) : tvgId.replace(' ', '_'));
                out.add(c);
                name = null;
            }
        }
        return out;
    }

    private static int guessKind(String group, String url) {
        String g = group == null ? "" : group.toLowerCase(Locale.ROOT);
        String u = url.toLowerCase(Locale.ROOT);
        if (g.contains("movie") || g.contains("film") || g.contains("cinema") || g.contains("vod")
                || g.contains("series") || u.contains("/movie/")) {
            return Channel.KIND_MOVIE;
        }
        return Channel.KIND_LIVE;
    }

    private static String safeId(String s) {
        return s == null ? "0" : s.replaceAll("[^A-Za-z0-9]", "");
    }
}
