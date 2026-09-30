package com.blueberryjoy.reader.downloads;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DownloadsRecyclerViewItems {
    @NonNull
    private final List<DownloadsRecyclerViewNode> items = new ArrayList<>();

    public void insert(DownloadsRecyclerViewNode item) {
        items.add(item);
    }

    public void sortByName() {
        items.sort(Comparator.comparing(DownloadsRecyclerViewNode::getTitle));
    }

    public void sortByDate() {
        items.sort(Comparator.comparingLong(DownloadsRecyclerViewNode::getDate));
    }

    public int size() {
        return items.size();
    }

    public DownloadsRecyclerViewNode get(int idx) {
        return items.get(idx);
    }

    public void remove(int idx) {
        items.remove(idx);
    }
}
