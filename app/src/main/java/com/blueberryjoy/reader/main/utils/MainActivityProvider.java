package com.blueberryjoy.reader.main.utils;

import com.blueberryjoy.reader.main.webview.MyWebView;

public interface MainActivityProvider {
    String getWebUrl();

    boolean isReadingMode();

    MyWebView createWebView(boolean isPrivate);
}