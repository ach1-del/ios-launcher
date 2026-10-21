package com.zyvo.ioslauncher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Loads installed launcher apps once (async) and maps theme icons by package name. */
public final class AppRepository {

    public interface Callback {
        void onApps(List<AppInfo> apps);
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    private AppRepository() {
    }

    public static void loadAsync(Context context, Callback callback) {
        final Context app = context.getApplicationContext();
        EXEC.execute(() -> {
            List<AppInfo> apps = load(app);
            callback.onApps(apps);
        });
    }

    private static List<AppInfo> load(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ri = pm.queryIntentActivities(main, 0);
        List<AppInfo> apps = new ArrayList<>(ri.size());
        for (ResolveInfo info : ri) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            CharSequence labelCs = info.loadLabel(pm);
            String label = labelCs != null ? labelCs.toString() : pkg;
            Drawable themed = IconLoader.get(context, pkg);
            Drawable fallback = roundedIcon(info.loadIcon(pm));
            apps.add(new AppInfo(pkg, label, themed != null ? themed : fallback));
        }
        Collections.sort(apps, Comparator.comparing(a -> a.label.toLowerCase()));
        return apps;
    }

    /** Cuts any icon into an iOS-like rounded square so mixed apps look uniform. */
    public static Drawable roundedIcon(Drawable src) {
        int size = 256;
        if (src instanceof BitmapDrawable && ((BitmapDrawable) src).getBitmap() != null) {
            Bitmap raw = ((BitmapDrawable) src).getBitmap();
            size = Math.max(raw.getWidth(), raw.getHeight());
        }
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        if (src != null) {
            src.setBounds(0, 0, size, size);
            src.draw(c);
        }
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        RectF r = new RectF(0, 0, size, size);
        c.drawRoundRect(r, size * 0.22f, size * 0.22f, p);
        return new BitmapDrawable(null, out);
    }
}
