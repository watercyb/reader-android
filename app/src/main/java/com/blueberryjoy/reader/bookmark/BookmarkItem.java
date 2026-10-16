package com.blueberryjoy.reader.bookmark;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

public class BookmarkItem {
    private String item;
    private final Bitmap icon;
    private final String link;
    private final int idx;
    private String type;
    private int oriIdx = -1;

    public BookmarkItem(String item, Bitmap icon, String link, int idx, String type) {
        this.item = item;
        this.icon = icon;
        this.link = link;
        this.idx = idx;
        this.type = type;
    }

    public String getItem() {
        return item;
    }

    public Bitmap getIcon() {
        return icon;
    }

    public String getLink() {
        return link;
    }

    public int getIdx() {
        return idx;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getOriIdx() {
        return oriIdx;
    }

    public void setOriIdx(int oriIdx) {
        this.oriIdx = oriIdx;
    }

    public void updateTitle(String title) {
        item = title + " (" + idx + ")";
    }

    @NonNull
    public String getTitle() {
        int idx = item.lastIndexOf(" (");
        if (idx >= 0) return item.substring(0, idx);
        return item;
    }

    public int compare(@NonNull BookmarkItem bookmarkItem) {
        if (type == null && bookmarkItem.type == null) return 0;
        if (type == null) return 1;
        if (bookmarkItem.type == null) return -1;
        if (type.equals(bookmarkItem.type)) {
            if (item == null) return -1;
            if (bookmarkItem.item == null) return 1;
            return 0;
        }
        return type.compareTo(bookmarkItem.type);
    }
}
