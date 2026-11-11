package com.blueberryjoy.reader.main.recyclerview;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blueberryjoy.reader.main.webview.MyWebView;

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

    @Nullable
    public WindowsRecyclerViewNode getLast() {
        if (!items.isEmpty()) return items.get(items.size() - 1);
        return null;
    }

    public void move(int from, int to) {
        WindowsRecyclerViewNode temp = items.get(from);
        items.remove(from);
        items.add(to, temp);
    }

    public int updateTitle(MyWebView webView, String title) {
        for (int i = 0; i < items.size(); i++) {
            WindowsRecyclerViewNode item = items.get(i);
            if (item.getWindow().getWebView() == webView) {
                item.setItem(title);
                return i;
            }
        }
        return -1;
    }
}
