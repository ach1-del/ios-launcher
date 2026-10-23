package com.zyvo.auratv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tiny async logo loader with memory cache (no external image library). */
public final class Thumbs {
    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(120);
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Thumbs() {
    }

    public static void load(ImageView view, String url, int fallback) {
        view.setTag(url == null ? "" : url);
        if (url == null || url.isEmpty() || !url.startsWith("http")) {
            view.setImageResource(fallback);
            return;
        }
        Bitmap cached = CACHE.get(url);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        view.setImageResource(fallback);
        final String key = url;
        POOL.execute(() -> {
            Bitmap bmp = download(key);
            if (bmp == null) return;
            CACHE.put(key, bmp);
            MAIN.post(() -> {
                Object tag = view.getTag();
                if (tag instanceof String && key.equals(tag)) {
                    view.setImageBitmap(bmp);
                }
            });
        });
    }

    private static Bitmap download(String urlStr) {
        HttpURLConnection c = null;
        try {
            URL url = new URL(urlStr);
            c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(10000);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", Fetcher.UA);
            if (c.getResponseCode() >= 400) return null;

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream in1 = c.getInputStream();
            BitmapFactory.decodeStream(in1, null, bounds);
            in1.close();
            c.disconnect();

            int sample = 1;
            int max = Math.max(bounds.outWidth, bounds.outHeight);
            while (max / sample > 320) sample *= 2;

            c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(10000);
            c.setRequestProperty("User-Agent", Fetcher.UA);
            InputStream in2 = c.getInputStream();
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap bmp = BitmapFactory.decodeStream(in2, null, opts);
            in2.close();
            return bmp;
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
