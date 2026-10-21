package com.zyvo.ioslauncher;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.collection.LruCache;

import java.io.IOException;
import java.io.InputStream;

/** Loads iOS-style icons shipped in assets/icons/<packageName>.png with a small memory cache. */
public final class IconLoader {

    private static final LruCache<String, Drawable> CACHE = new LruCache<>(64);

    private IconLoader() {
    }

    public static Drawable get(Context context, String packageName) {
        Drawable cached = CACHE.get(packageName);
        if (cached != null) {
            return cached;
        }
        AssetManager assets = context.getAssets();
        try (InputStream in = assets.open("icons/" + packageName + ".png")) {
            Bitmap bmp = BitmapFactory.decodeStream(in);
            if (bmp == null) {
                return null;
            }
            Drawable d = new BitmapDrawable(context.getResources(), bmp);
            CACHE.put(packageName, d);
            return d;
        } catch (IOException e) {
            return null;
        }
    }
}
