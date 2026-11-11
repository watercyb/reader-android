package com.blueberryjoy.reader.main.recyclerview;

import android.graphics.Bitmap;

import com.blueberryjoy.reader.main.window.Window;

public class WindowsRecyclerViewNode {
    String item;
    Bitmap icon;
    Window window;

    public WindowsRecyclerViewNode(String item, Bitmap icon, Window window) {
        this.item = item;
        this.icon = icon;
        this.window = window;
    }

    public boolean isPrivate() {
        return window.isPrivate();
    }

    public boolean isDownloadedPage() {
        return window.isDownloadedPage();
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public Bitmap getIcon() {
        return icon;
    }

    public Window getWindow() {
        return window;
    }
}
