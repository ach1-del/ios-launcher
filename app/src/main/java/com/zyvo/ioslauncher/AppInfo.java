package com.zyvo.ioslauncher;

import android.graphics.drawable.Drawable;

public class AppInfo {
    public final String packageName;
    public final String label;
    public final Drawable fallbackIcon;

    public AppInfo(String packageName, String label, Drawable fallbackIcon) {
        this.packageName = packageName;
        this.label = label;
        this.fallbackIcon = fallbackIcon;
    }
}
