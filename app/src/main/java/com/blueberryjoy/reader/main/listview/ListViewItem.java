package com.blueberryjoy.reader.main.listview;

import androidx.annotation.Nullable;

public class ListViewItem {
    private String title;
    private final ListViewAction action;
    @Nullable
    private final ListViewTitleProvider getTitle;
    @Nullable
    private final ListViewEnableProvider getEnable;

    public ListViewItem(String title, ListViewAction action) {
        this.title = title;
        this.action = action;
        this.getTitle = null;
        this.getEnable = null;
    }

    public ListViewItem(String title, ListViewAction action, ListViewTitleProvider getTitle, ListViewEnableProvider getEnable) {
        this.title = title;
        this.action = action;
        this.getTitle = getTitle;
        this.getEnable = getEnable;
    }

    public String getTitle() {
        if (getTitle == null) return title;
        return getTitle.getTitle();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void onClick(boolean isLongClick) {
        action.onClick(this, isLongClick);
    }

    public boolean getEnable() {
        boolean enable = true;
        if (getEnable == null) return enable;
        return getEnable.getEnable();
    }
}
