package com.blueberryjoy.reader.bookmark;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BookmarkList {
    private final List<BookmarkItem> items = new ArrayList<>();
    private final HashMap<String, Divider> dividers = new HashMap<>();

    @NonNull
    public List<BookmarkItem> getItems() {
        return items;
    }

    public int length() {
        return items.size();
    }

    public void remove(int idx) {
        if (idx < items.size())
            items.remove(idx);
    }

    @Nullable
    public BookmarkItem get(int idx) {
        if (idx < items.size()) return items.get(idx);
        return null;
    }

    public void add(@NonNull BookmarkItem bookmarkItem) {
        String type = bookmarkItem.getType();
        if (type != null) {
            if (dividers.containsKey(type)) {
                insert(bookmarkItem, false);
                Divider divider = dividers.get(type);
                assert divider != null;
                divider.count++;
                divider.show = true;
            } else {
                Divider divider = new Divider(1, true);
                dividers.put(type, divider);
                insert(new BookmarkItem(null, null, null, -1, type), true);
                insert(bookmarkItem, false);
            }
        } else {
            insert(bookmarkItem, false);
        }
    }

    public void add(int originalIdx, BookmarkItem bookmarkItem) {
        items.add(originalIdx, bookmarkItem);
    }

    public void setItems(@NonNull List<BookmarkItem> list) {
        list.sort(BookmarkItem::compare);
        items.clear();
        dividers.clear();
        String prv = null;
        int count = 0;
        for (BookmarkItem item : list) {
            if (item.getType() == null) {
                if (prv != null) {
                    dividers.put(prv, new Divider(count, false));
                    prv = null;
                }
            } else {
                if (!item.getType().equals(prv)) {
                    if (prv != null)
                        dividers.put(prv, new Divider(count, false));
                    prv = item.getType();
                    items.add(new BookmarkItem(null, null, null, -1, prv));
                    count = 1;
                } else {
                    count++;
                }
            }
            items.add(item);
        }
        if (prv != null)
            dividers.put(prv, new Divider(count, false));
    }

    private void insert(@NonNull BookmarkItem bookmarkItem, boolean before) {
        if (before) {
            for (int i = 0; i < items.size(); i++) {
                if (bookmarkItem.compare(items.get(i)) <= 0) {
                    items.add(i, bookmarkItem);
                    return;
                }
            }
        } else {
            for (int i = 0; i < items.size(); i++) {
                if (bookmarkItem.compare(items.get(i)) < 0) {
                    items.add(i, bookmarkItem);
                    return;
                }
            }
        }
        items.add(bookmarkItem);
    }

    public boolean isShown(@Nullable String type) {
        if (type == null) return true;
        Divider divider = dividers.getOrDefault(type, null);
        if (divider == null) return false;
        return divider.show;
    }

    public boolean isShown(int idx) {
        if (idx < 0 || idx >= items.size()) return false;
        BookmarkItem bookmarkItem = items.get(idx);
        if (bookmarkItem.getItem() == null || bookmarkItem.getType() == null) return true;
        Divider divider = dividers.getOrDefault(bookmarkItem.getType(), null);
        if (divider == null) return false;
        return divider.show;
    }

    public void toggleDivider(int idx) {
        BookmarkItem bookmarkItem = items.get(idx);
        if (bookmarkItem.getItem() == null) {
            Divider divider = dividers.getOrDefault(bookmarkItem.getType(), null);
            if (divider != null) divider.show = !divider.show;
        }
    }

    public void updateDivider(@NonNull BookmarkItem bookmarkItem, @Nullable String newType) {
        String oldType = bookmarkItem.getType();
        Divider divider = dividers.getOrDefault(oldType, null);
        if (divider == null) return;
        dividers.remove(oldType);
        if (dividers.containsKey(newType)) {
            Divider newDivider = dividers.get(newType);
            Objects.requireNonNull(newDivider).count += divider.count;
            newDivider.show = true;
            List<BookmarkItem> list = new ArrayList<>();
            List<BookmarkItem> updatedItems = new ArrayList<>();
            for (BookmarkItem item : items) {
                if (item.getType() != null && item.getType().equals(oldType)) {
                    if (item.getItem() != null) {
                        item.setType(newType);
                        updatedItems.add(item);
                    }
                } else {
                    list.add(item);
                }
            }
            list.addAll(updatedItems);
            list.sort(BookmarkItem::compare);
            items.clear();
            items.addAll(list);
        } else {
            if (newType != null) {
                dividers.put(newType, divider);
                divider.show = true;
            }
            List<BookmarkItem> list = new ArrayList<>();
            List<BookmarkItem> updatedItems = new ArrayList<>();
            for (BookmarkItem item : items) {
                if (item.getType() != null && item.getType().equals(oldType)) {
                    if (newType == null && item.getItem() == null) continue;
                    item.setType(newType);
                    updatedItems.add(item);
                } else {
                    list.add(item);
                }
            }
            list.addAll(updatedItems);
            list.sort(BookmarkItem::compare);
            items.clear();
            items.addAll(list);
        }
    }

    public void updateBookmark(@NonNull BookmarkItem bookmarkItem, String title, @Nullable String type) {
        String oldType = bookmarkItem.getType();
        bookmarkItem.updateTitle(title);
        if ((oldType == null && type == null) || (oldType != null && oldType.equals(type)))
            return;
        items.remove(bookmarkItem);
        if (oldType != null) {
            Divider divider = dividers.getOrDefault(oldType, null);
            if (divider != null)
                divider.count--;
        }
        bookmarkItem.setType(type);
        if (type != null) {
            if (dividers.containsKey(type)) {
                Divider divider = dividers.get(type);
                Objects.requireNonNull(divider).count++;
            } else {
                dividers.put(type, new Divider(1, true));
                insert(new BookmarkItem(null, null, null, -1, type), true);
            }
        }
        insert(bookmarkItem, false);
    }

    @NonNull
    public HashSet<String> getTypes() {
        return new HashSet<>(dividers.keySet());
    }

    public void updateFilteredCount(@NonNull HashMap<String, Integer> counts) {
        for (Map.Entry<String, Divider> entry : dividers.entrySet()) {
            String type = entry.getKey();
            Divider divider = entry.getValue();
            Integer count = counts.getOrDefault(type, 0);
            if (count == null) count = 0;
            divider.filteredCount = count;
        }
    }

    public void resetFilteredCount() {
        for (Divider divider : dividers.values()) {
            if (divider != null) divider.filteredCount = divider.count;
        }
    }

    public int getFilteredCount(String type) {
        Divider divider = dividers.getOrDefault(type, null);
        if (divider == null) return 0;
        return divider.filteredCount;
    }

    static class Divider {
        int count;
        boolean show;
        int filteredCount;

        public Divider(int count, boolean show) {
            this.count = count;
            this.show = show;
            this.filteredCount = count;
        }
    }

    public void updateDividerCount(String type, int n) {
        Divider divider = dividers.getOrDefault(type, null);
        if (divider != null) {
            divider.count += n;
            divider.filteredCount += n;
        }
    }
}

