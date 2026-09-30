package com.blueberryjoy.reader.main.webview;

import android.webkit.JavascriptInterface;

public interface WebViewJSInterface {
    String ACTION_UNMATCHING = "unmatching";
    String ACTION_MUTATION = "mutation";

    @JavascriptInterface
    void onError(String data);

    @JavascriptInterface
    void onPageScroll(int position);

    @JavascriptInterface
    void onMessage(String message);
}
