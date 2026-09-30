package com.blueberryjoy.reader.main.recyclerview;

import android.graphics.Bitmap;

import com.blueberryjoy.reader.main.window.Window;

public record WindowsRecyclerViewNode(String item, Bitmap icon, Window window) {

    public boolean isPrivate() {
        return window.isPrivate();
    }
}
