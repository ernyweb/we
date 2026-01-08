package com.gamebooster.launcher;

public class RecordingItem {
    private final String title;
    private final String path;
    private final String meta;

    public RecordingItem(String title, String path, String meta) {
        this.title = title;
        this.path = path;
        this.meta = meta;
    }

    public String title() {
        return title;
    }

    public String path() {
        return path;
    }

    public String meta() {
        return meta;
    }
}
