package com.blueberryjoy.reader.main.recyclerview;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class WindowsRecyclerViewItems {
    @NonNull
    private final List<WindowsRecyclerViewNode> items = new ArrayList<>();

    public void insert(WindowsRecyclerViewNode item) {
        items.add(item);
    }

    public int size() {
        return items.size();
    }

    public WindowsRecyclerViewNode get(int idx) {
        return items.get(idx);
    }

    public void remove(int idx) {
        items.remove(idx);
    }

    public void add(WindowsRecyclerViewNode windowsRecyclerViewNode) {
        items.add(windowsRecyclerViewNode);
    }

    public void add(int idx, WindowsRecyclerViewNode windowsRecyclerViewNode) {
        items.add(idx, windowsRecyclerViewNode);
    }

    public WindowsRecyclerViewNode getLast() {
        if (!items.isEmpty()) return items.get(items.size() - 1);
        return null;
    }
}
