package com.blueberryjoy.reader.main.utils;

import android.content.Context;

import com.blueberryjoy.reader.main.webview.MyWebView;

public interface MainActivityProvider {
    String getWebUrl();

    boolean isReadingMode();

    MyWebView createWebView(boolean isPrivate);

    //TODO
    Context getMainContext();
}