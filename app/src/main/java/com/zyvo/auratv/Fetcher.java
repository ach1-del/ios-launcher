package com.zyvo.auratv;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Minimal HTTP text client with timeouts, gzip and redirects. */
public final class Fetcher {
    public static final String UA = "AuraTV/2.0 (Android)";

    private Fetcher() {
    }

    public static String get(String urlStr) throws IOException {
        return get(urlStr, null);
    }

    public static String get(String urlStr, Map<String, String> headers) throws IOException {
        HttpURLConnection c = null;
        try {
            URL url = new URL(urlStr);
            c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(20000);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", UA);
            c.setRequestProperty("Accept", "*/*");
            c.setRequestProperty("Accept-Encoding", "gzip");
            if (headers != null) {
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    c.setRequestProperty(e.getKey(), e.getValue());
                }
            }
            int code = c.getResponseCode();
            InputStream raw = code >= 400 ? c.getErrorStream() : c.getInputStream();
            if (raw == null) throw new IOException("HTTP " + code);
            InputStream in = raw;
            String enc = c.getContentEncoding();
            if (enc != null && enc.toLowerCase().contains("gzip")) {
                in = new GZIPInputStream(raw);
            }
            String body = read(in);
            if (code >= 400) {
                throw new IOException("HTTP " + code + ": " + preview(body));
            }
            return body;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    public static String readFile(String path) throws IOException {
        try (InputStream in = new FileInputStream(path)) {
            return read(in);
        }
    }

    public static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String preview(String s) {
        if (s == null) return "";
        s = s.replace('\n', ' ').trim();
        return s.length() > 140 ? s.substring(0, 140) : s;
    }
}
