package com.blueberryjoy.reader.main.utils;

import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.blueberryjoy.reader.main.webview.MyWebView;

import org.json.JSONArray;

public class JavaScriptHandler {

    public interface JsResultCallback {
        void onResult(String result);
    }

    public static void updateWindowHeight(@NonNull MyWebView webView) {
        webView.loadUrl("javascript: if (window.pageReader) window.pageReader.windowHeight = " + webView.getHeight() / webView.getScaleFactor() + ";");
    }

    public static void updateThemeColor(@NonNull WebView webView, boolean isDarkMode) {
        if (isDarkMode) {
            webView.evaluateJavascript("javascript: document.body.style.backgroundColor = '#121212'; document.body.style.color ='#BDBDBD';", null);
        } else {
            webView.evaluateJavascript("javascript: document.body.style.backgroundColor = 'white'; document.body.style.color = 'black';", null);
        }
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

    public static void searchAndPerformClick(@NonNull MyWebView webView) {
        webView.loadUrl("javascript: window.__clickNextPage();");
    }

    public static void clickElement(@NonNull MyWebView webView) {
        webView.loadUrl("javascript: if (window.pageReader) window.pageReader.clickElement();");
    }

    public static void isExtractedContentStillPresent(@NonNull MyWebView webView, @NonNull JsResultCallback callback) {
        webView.evaluateJavascript("(function() { if (window.pageReader) { return window.pageReader.isExtractedContentStillPresent(); } else { return false;} })();", callback::onResult);
    }

    public static void checkFrameTag(@NonNull MyWebView webView, @NonNull JsResultCallback callback) {
        webView.evaluateJavascript("(function() { return document.body.innerHTML.includes('\\u003Cframe'); })();", callback::onResult);
    }

    public static void getElementAndClickableByPosition(@NonNull MyWebView webView, int x, int y, @NonNull JsResultCallback callback) {
        webView.evaluateJavascript("(function(x,y) { if (typeof pageReader !== 'undefined') return pageReader.getElementAndClickableByPosition(x,y); })(" + x + ", " + y + ");", callback::onResult);
    }

    public static void loadContent(@NonNull MyWebView webView, @NonNull JsResultCallback callback) {
        webView.evaluateJavascript("(function() { if (window.pageReader) { return window.pageReader.loadContent(); } })();", callback::onResult);
    }

    public static void getElementByPosition(@NonNull MyWebView webView, int x, int y, @NonNull JsResultCallback callback) {
        webView.evaluateJavascript("(function(x,y) { if (window.pageReader) return window.pageReader.getElementByPosition(x,y); })(" + x + ", " + y + ");", callback::onResult);
    }

    public static void getReadingModeHTML(@NonNull MyWebView webView, @NonNull JsResultCallback callback) {
        webView.evaluateJavascript("(function() {return pageReader.readingModeHTML; })();", callback::onResult);
    }

    public static void setReadingModeHTML(@NonNull MyWebView webView, String html, @NonNull JsResultCallback callback) {
        webView.evaluateJavascript("(function() { document.documentElement.innerHTML=" + html + "; })();", callback::onResult);
    }

    public static void updateHighlightAndScroll(@NonNull MyWebView webView, int direction, JSONArray idxList, float y, @NonNull JsResultCallback callback) {
        webView.evaluateJavascript("javascript: if (window.pageReader) window.pageReader.updateHighlightAndScroll(" + direction + "," + idxList + "," + y + ");", callback::onResult);
    }


}
