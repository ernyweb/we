package com.gamebooster.launcher;

class RecordingItem {
    private final String title;
    private final String path;
    private final String meta;

    RecordingItem(String title, String path, String meta) {
        this.title = title;
        this.path = path;
        this.meta = meta;
    }

    String title() { return title; }
    String path() { return path; }
    String meta() { return meta; }
}
