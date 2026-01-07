package com.gamebooster.launcher;

import android.graphics.drawable.Drawable;

/**
 * Oyun bilgilerini tutan veri sınıfı
 */
public class GameInfo {
    private String name;
    private String packageName;
    private Drawable icon;
    private boolean isSystemApp;
    private long installTime;

    public GameInfo(String name, String packageName, Drawable icon, boolean isSystemApp, long installTime) {
        this.name = name;
        this.packageName = packageName;
        this.icon = icon;
        this.isSystemApp = isSystemApp;
        this.installTime = installTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public boolean isSystemApp() {
        return isSystemApp;
    }

    public void setSystemApp(boolean systemApp) {
        isSystemApp = systemApp;
    }

    public long getInstallTime() {
        return installTime;
    }

    public void setInstallTime(long installTime) {
        this.installTime = installTime;
    }
}
