package com.blueberryjoy.reader.downloads;

import android.graphics.Bitmap;

public class DownloadsRecyclerViewNode {
    private final String item;
    private final String filename;
    private final String title;
    private String content;
    private final Bitmap icon;
    private final int idx;
    private final long date;

    public DownloadsRecyclerViewNode(String item, String filename, String title, Bitmap icon, int idx, long date) {
        this.item = item;
        this.filename = filename;
        this.title = title;
        this.icon = icon;
        this.idx = idx;
        this.date = date;
    }

    public String getItem() {
        return item;
    }

    public Bitmap getIcon() {
        return icon;
    }

    public String getFilename() {
        return filename;
    }

    public int getIdx() {
        return idx;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public long getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }
}
