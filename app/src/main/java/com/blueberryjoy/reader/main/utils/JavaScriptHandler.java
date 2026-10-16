package com.blueberryjoy.reader.main.utils;

import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.blueberryjoy.reader.main.webview.MyWebView;

import org.json.JSONArray;

public class JavaScriptHandler {

    public static void updateWindowHeight(@NonNull MyWebView webView) {
        webView.loadUrl("javascript: if (window.pageReader) window.pageReader.windowHeight = " + webView.getHeight() / webView.getScaleFactor() + ";");
    }

    public static void updateThemeColor(@NonNull WebView webView, boolean isDarkMode) {
        webView.loadUrl("javascript:if(typeof pageReader !=='undefined') pageReader.updateThemeColor(" + isDarkMode + ");");
    }

    public static void loadJavaScriptCode(@NonNull WebView webView, String code) {
        webView.loadUrl("javascript:" + code);
    }

    public static void clearIdx(@NonNull MyWebView webView) {
        webView.loadUrl("javascript: if (window.pageReader) window.pageReader.clearIdx();");
    }

    public static void scrollToCurrentText(@NonNull MyWebView webView, boolean isSmooth) {
        if (isSmooth) {
            webView.loadUrl("javascript: if (window.pageReader) window.pageReader.scrollToHighlightedSegment('smooth');");
        } else {
            webView.loadUrl("javascript: if (window.pageReader) window.pageReader.scrollToHighlightedSegment('auto');");
        }
    }

    public static void scrollTo(@NonNull MyWebView webView, int height) {
        webView.loadUrl("javascript: window.scrollTo({top: " + height + ", behavior: \"smooth\"})");
    }

    public static void performClick(@NonNull MyWebView webView, JSONArray idxList) {
        webView.loadUrl("javascript: if (window.pageReader) window.pageReader.performClick(" + idxList + ");");
    }
}
