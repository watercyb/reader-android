package com.blueberryjoy.reader.main.utils;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class DispatchTouchArea {
    @NonNull
    private final List<View> viewList = new ArrayList<>();
    private boolean isWebUrlVisible = false;
    private boolean isListViewVisible = false;

    public DispatchTouchArea() {
    }

    public boolean isWebUrlVisible() {
        return isWebUrlVisible;
    }

    public boolean isListViewVisible() {
        return isListViewVisible;
    }

    public void setWebUrlVisible(boolean isVisible) {
        isWebUrlVisible = isVisible;
        if (!isVisible)
            clear();
    }

    public void setListViewVisible(boolean isVisible) {
        isListViewVisible = isVisible;
        if (!isVisible)
            clear();
    }

    public boolean isOutside(@NonNull MotionEvent ev) {
        int x = (int) ev.getRawX();
        int y = (int) ev.getRawY();
        Rect rect = new Rect();
        for (View view : viewList) {
            view.getGlobalVisibleRect(rect);
            if (rect.contains(x, y)) {
                return false;
            }
        }
        clear();
        return true;
    }

    private void clear() {
        viewList.clear();
    }

    public void add(View view) {
        viewList.add(view);
    }
}
