package com.blueberryjoy.reader.main.window;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blueberryjoy.reader.App;
import com.blueberryjoy.reader.BuildConfig;
import com.blueberryjoy.reader.R;
import com.blueberryjoy.reader.history.BrowsingHistory;
import com.blueberryjoy.reader.main.utils.JavaScriptHandler;
import com.blueberryjoy.reader.main.utils.MainActivityProvider;
import com.blueberryjoy.reader.main.webview.WebViewJSInterface;
import com.blueberryjoy.reader.sqlite.page.SQLitePage;
import com.blueberryjoy.reader.main.webview.MyWebView;
import com.blueberryjoy.reader.sqlite.page.Page;
import com.blueberryjoy.reader.main.recyclerview.WindowsRecyclerViewNode;
import com.blueberryjoy.reader.settings.ReaderSettings;
import com.blueberryjoy.reader.utils.Ad_Blocker;
import com.blueberryjoy.reader.utils.Favicon;
import com.blueberryjoy.reader.utils.FileStorage;
import com.blueberryjoy.reader.utils.Message;
import com.blueberryjoy.reader.utils.TextToSpeechManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Window {
    private final int id;
    @NonNull
    private MyWebView webView;
    @NonNull
    private String[] readingArr = new String[0];
    private int idx;
    private int nextIdx;
    @NonNull
    private List<JSONArray> readingIdxToPageIdx = new ArrayList<>();
    @NonNull
    private List<Integer> pageIdxToReadingIdx = new ArrayList<>();
    @NonNull
    private final WindowIndexCache windowIndexCache;
    private int position;

    private boolean isReading = false;
    private int loadingState = 0; //-2 - no currentWindow, -1 - loading, 0 - not loading, 1 - loading through onPageStarted, 2 - forward/backward, 3 - isReading
    private boolean isIgnoreSSLError = false;
    private boolean isLoadedText = false;
    private boolean isStartReadingAfterPageLoaded = false;
    private boolean isCurrentWindow = false;
    private final Handler delayRunHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private String lastLongPressedLink = null;
    private boolean isDesktopMode = false;
    private final boolean isPrivate;
    private int removedWindowIdx = -1;
    private boolean isRemovedWindowCurrent = false;

    //Debug
    private int playNextPageState = 0;
    private final Handler playNextPageDelayRunHandler = new Handler(Looper.getMainLooper());


    @NonNull
    private static final Set<Character> HS = Set.of('.', '。', '?', '？', '!', '！');

    public Window(@NonNull MyWebView webView, @NonNull String url, int idx, boolean isPrivate, int position) {
        Page page = SQLitePage.createPage(url, idx, false, isPrivate, position);
        this.webView = webView;
        id = page.id;
        this.idx = page.idx;
        nextIdx = page.idx;
        this.isPrivate = page.isPrivate;
        this.position = position;
        windowIndexCache = new WindowIndexCache(10);

        initializeWebView();
        if (!url.equals("about:newtab")) {
            addPageToCache(url, idx, "", "");
            webView.loadUrl(url);
        }
    }

    public Window(@NonNull MyWebView webView, @NonNull Page page) {
        this.webView = webView;
        id = page.id;
        idx = page.idx;
        nextIdx = idx;
        isPrivate = page.isPrivate;
        windowIndexCache = new WindowIndexCache(10);

        initializeWebView();
        if (!page.url.equals("about:newtab")) {
            byte[] data = Base64.decode(page.state, Base64.DEFAULT);
            if (data.length < 10) webView.loadUrl(page.url);
            addPageToCache(page.url, idx, "", getWebView().getState());
            webView.setState(page.state);
        }
    }

    public void getReadingArr(@NonNull String[] arrFromPage, @NonNull boolean[] nlmFromPage) {
        List<String> list = new ArrayList<>();
        readingArr = new String[0];
        readingIdxToPageIdx = new ArrayList<>();
        pageIdxToReadingIdx = new ArrayList<>();
        if (arrFromPage.length == 0) return;

        List<Integer> pageIndices = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int readingIdx = 0;
        boolean isNewSentence = false;
        int count = 0;
        for (int pageIdx = 0; pageIdx < arrFromPage.length; pageIdx++) {
            if (arrFromPage[pageIdx].length() == 1 && pageIdx > 0) {
                pageIdxToReadingIdx.add(readingIdx);
                pageIndices.add(pageIdx);
            } else {
                if (isNewSentence) {
                    list.add(sb.toString());
                    sb.setLength(0);
                    count = 0;
                    readingIdxToPageIdx.add(getJSONArray(arrFromPage, pageIndices));
                    pageIndices = new ArrayList<>();
                    readingIdx++;
                    isNewSentence = false;
                }
                if (nlmFromPage[pageIdx]) {
                    if (sb.length() > 0) {
                        list.add(sb.toString());
                        sb.setLength(0);
                        count = 0;
                        readingIdxToPageIdx.add(getJSONArray(arrFromPage, pageIndices));
                        pageIndices = new ArrayList<>();
                        readingIdx++;
                    }
                }
                pageIdxToReadingIdx.add(readingIdx);
                pageIndices.add(pageIdx);
                if (sb.length() > 0) sb.append(' ');
            }
            sb.append(arrFromPage[pageIdx]);
            count++;
            if (chkNewSentence(arrFromPage[pageIdx]) || count >= 5) isNewSentence = true;
        }
        if (sb.length() > 0) {
            list.add(sb.toString());
            readingIdxToPageIdx.add(getJSONArray(arrFromPage, pageIndices));
        }
        readingArr = list.toArray(new String[0]);
    }

    @NonNull
    private JSONArray getJSONArray(String[] strs, @NonNull List<Integer> list) {
        int l = 0;
        while (l < list.size() && strs[list.get(l)].trim().isEmpty()) {
            l++;
        }
        int r = list.size() - 1;
        if (l == list.size()) {
            l = 0;
        } else {
            while (strs[list.get(r)].trim().isEmpty()) {
                r--;
            }
        }
        JSONArray jsonArray = new JSONArray();
        for (int i = l; i <= r; i++) {
            jsonArray.put(list.get(i));
        }
        return jsonArray;
    }

    private boolean chkNewSentence(@NonNull String str) {
        return (!str.isEmpty() && HS.contains(str.charAt(str.length() - 1)));
    }

    private void clearReadingArr() {
        readingArr = new String[0];
        if (isCurrentWindow) WindowList.readingArrCleared();
    }

    @Nullable
    public JSONArray getPageIdx() {
        if (idx >= 0 && idx < readingIdxToPageIdx.size()) {
            return readingIdxToPageIdx.get(idx);
        } else {
            return null;
        }
    }

    @Nullable
    public JSONArray getPageIdx(int idx) {
        if (idx >= 0 && idx < readingIdxToPageIdx.size()) {
            return readingIdxToPageIdx.get(idx);
        } else {
            return null;
        }
    }

    public int getReadingIdx(int idx) {
        if (idx >= 0 && idx < pageIdxToReadingIdx.size()) return pageIdxToReadingIdx.get(idx);
        return -1;
    }

    private String getStrFromHTML(String html) throws JSONException {
        return new JSONArray("[" + html + "]").getString(0);
    }

    @NonNull
    public String[] getReadingArr() {
        return readingArr;
    }

    @NonNull
    public MyWebView getWebView() {
        return webView;
    }

    private void setError(String error) {
        readingArr = new String[]{error};
    }

    public int getIdx() {
        return isLoadedText ? idx : getCachedIdx();
    }

    public boolean hasNext() {
        return nextIdx >= 0 && nextIdx < readingArr.length;
    }

    private void setIdxFromCache() {
        idx = getCachedIdx();
        nextIdx = idx;
        saveIdx();
    }

    private void saveIdx() {
        if (loadingState != 0) return;
        SQLitePage.saveIdx(id, idx);
        windowIndexCache.setIdx(idx);
    }

    private void saveIsCurrentWindow() {
        SQLitePage.saveIsCurrent(id, isCurrentWindow);
    }

    public void setIdxWithoutSave(int idx) {
        this.idx = idx;
        nextIdx = idx;
    }

    public void idxInc() {
        idx++;
        nextIdx++;
        if (idx >= readingArr.length) idx = readingArr.length - 1;
        if (nextIdx >= readingArr.length) nextIdx = readingArr.length - 1;
    }

    public void nextIdx() {
        idx = nextIdx;
        nextIdx++;
        saveIdx();
    }

    public void idxDec() {
        idx--;
        if (idx < 0) idx = 0;
        nextIdx = idx;
    }

    public void idxDecForPrv() {
        idx--;
        nextIdx = idx;
    }

    public boolean isLastIdx() {
        return idx >= 0 && idx == readingArr.length - 1;
    }

    public void resetNextIdx() {
        nextIdx = idx;
    }

    public String getCurrentStr() {
        if (idx < 0) idx = 0;
        if (idx >= readingArr.length) idx = readingArr.length - 1;
        if (readingArr.length > 0 && idx >= 0) return readingArr[idx];
        return "";
    }

    @NonNull
    public String getUrl() {
        String url = webView.getUrl();
        if (url == null) return "";
        return url;
    }

    @Nullable
    public String getTitle() {
        return webView.getTitle();
    }

    public boolean isValidReadingStatus() {
        return webView.getTitle() != null && readingArr.length > 0;
    }

    public int getCachedIdx() {
        String url = webView.getUrl();
        if (url != null) return windowIndexCache.getIdx(url);
        return 0;
    }

    //Idx<0 -> not update idx.
    public void addPageToCache(@Nullable String url, int idx, @Nullable String title, @NonNull String state) {
        if (url == null) url = webView.getUrl();
        if (url == null) return;
        if (title == null) title = webView.getTitle();
        windowIndexCache.addPage(url, idx, title);
        SQLitePage.saveUrl(id, url, state);
        if (idx >= 0) SQLitePage.saveIdx(id, idx);
    }

    private void initializeWebView() {
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setSupportZoom(true);

        webView.addJavascriptInterface(new WebViewJSInterface() {
            @Override
            @JavascriptInterface
            public void onError(String data) {
                Message.log("Page error: " + data);
            }

            @Override
            @JavascriptInterface
            public void onPageScroll(int position) {
                if (isCurrentWindow) WindowList.onPageScroll(position);
            }

            @Override
            @JavascriptInterface
            public void onMessage(@NonNull String message) {
                Message.log("Page message: " + message);
                switch (message) {
                    case WebViewJSInterface.ACTION_UNMATCHING,
                         WebViewJSInterface.ACTION_MUTATION -> {
                        if (isCurrentWindow && isLoadedText) WindowList.onTextNotMatching();
                    }
                    default -> Message.toast(message);
                }
            }
        }, "Android");

        Runnable afterNewPageLoaded = () -> {
            if (isCurrentWindow) WindowList.clearNotification();
            addPageToCache(null, -1, null, getWebView().getState());
            addToPage();
            setLoadingState(0);
            setIdxFromCache();
            if (isStartReadingAfterPageLoaded) {
                setIsStartReadingAfterPageLoaded(false);
                pause();
            }
        };

        Runnable afterSamePageLoaded = () -> {
            //Check if the title is updated after afterSamePageLoaded and before samePageLoaded.
            if (!windowIndexCache.checkTitle(webView.getTitle())) {
                //TODO should check the content.
                webView.evaluateJavascript("(function() { if (window.pageReader) { return window.pageReader.isExtractedContentStillPresent(); } else { return false;} })();", html -> {
                    if (!html.equals("true")) {
                        if (isCurrentWindow)
                            Message.log("afterSamePageLoaded different url: different content");
                    } else {
                        if (isCurrentWindow)
                            Message.log("afterSamePageLoaded different url: same content");
                    }
                });
                if (isReading && isCurrentWindow) WindowList.readerPause();
                isLoadedText = false;
                if (isCurrentWindow) WindowList.onLoadedTextStateChanged(false);
                if (ReaderSettings.getIsReloadPageAfterJS()) {
                    runTask(() -> webViewLoadUrl(null), false);
                } else {
                    clearPageIdx();
                    runTask(afterNewPageLoaded, false);
                }
            } else {
                windowIndexCache.setFirstUrl(getUrl());
                addToPage();
                getArrFromPage(false, true, false, -1, -1, -1);
                setLoadingState(0);
            }
        };

        webView.setWebViewClient(new WebViewClient() {
            //Will not be called on page forward/back navigation or JS‑updated pages.
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                //if (isCurrentWindow) Message.log("onPageStarted: " + url);
                if (WindowList.isReadingMode()) return;
                if (isReading && isCurrentWindow) WindowList.readerPause();
                setLoadingState(1);
                isLoadedText = false;
                if (isCurrentWindow) {
                    WindowList.onLoadedTextStateChanged(false);
                    WindowList.onUrlChanged(url);
                    WindowList.onResetTextBoxRequired();
                }
                clearReadingArr();
            }

            //Only called when page open or reload normally, paired with onPageStarted.
            @Override
            public void onPageFinished(@NonNull WebView view, @NonNull String url) {
                if (url.startsWith("file")) ((MyWebView) view).updateThemeColor();
                //Debug
                if (BuildConfig.DEBUG) playNextPageOnPageFinishedDelay();
            }

            //May run before the page finishes loading (title, JS content).
            @Override
            public void doUpdateVisitedHistory(WebView view, @NonNull String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);
                //if (isCurrentWindow) Message.log("doUpdateVisitedHistory: " + url + " newTitle: " + webView.getTitle());
                //Check if there is a frame tag, and disable swipeRefresh if so.
                webView.evaluateJavascript("(function() { return document.body.innerHTML.includes('\\u003Cframe'); })();", (html) -> {
                    if (isCurrentWindow) WindowList.onSwipeRefreshRequired(!html.equals("true"));
                });
                //If onPageStarted is not called and url/title are different -> different page.
                if (getLoadingState() == 0 && (!isCurrentWindow || ((!WindowList.getWebUrl().equals(url) || !windowIndexCache.checkTitle(webView.getTitle())) && !WindowList.isReadingMode()))) {
                    setLoadingState(-1);
                    if (isCurrentWindow) {
                        WindowList.onUrlChanged(url);
                        WindowList.onResetTextBoxRequired();
                    }
                    //Additional check on the page contents, if matched -> same page.
                    webView.evaluateJavascript("(function() { if (window.pageReader) { return window.pageReader.isExtractedContentStillPresent(); } else { return false;} })();", html -> {
                        if (windowIndexCache.checkTitle(webView.getTitle()) && html.equals("true")) {
                            //Same page.
                            //if (isCurrentWindow) Message.log("Same page: " + url + " newTitle: " + webView.getTitle());
                            runTask(afterSamePageLoaded, true);
                        } else {
                            //Different page.
                            //if (isCurrentWindow) Message.log("Different: " + url + " newTitle: " + webView.getTitle());
                            if (isReading && isCurrentWindow) {
                                WindowList.readerPause();
                            }
                            isLoadedText = false;
                            if (isCurrentWindow) WindowList.onLoadedTextStateChanged(false);
                            if (ReaderSettings.getIsReloadPageAfterJS()) {
                                runTask(() -> webViewLoadUrl(null), true);
                            } else {
                                clearPageIdx();
                                runTask(afterNewPageLoaded, true);
                            }
                        }
                    });
                } else if (getLoadingState() == 2 || getLoadingState() == 1) {
                    //if (isCurrentWindow) Message.log("doUpdateVisitedHistory: forward/backward or normal: " + url + " newTitle: " + webView.getTitle());
                    setLoadingState(-1);
                    if (isCurrentWindow) {
                        WindowList.onUrlChanged(url);
                        WindowList.clearNotification();
                    }
                    runTask(afterNewPageLoaded, true);
                } //else {Message.log("The url and title updated but using the same values: " + url + " title: " + webView.getTitle());}
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, @NonNull WebResourceRequest request) {
                if (Ad_Blocker.isAdHost(request.getUrl().toString())) {
                    return Ad_Blocker.createEmptyWebResourceResponse();
                } else {
                    return super.shouldInterceptRequest(view, request);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, @NonNull WebResourceRequest request) {
                //Blocking the page navigation when click an ad, since ad already blocked it should not be called.
                if (Ad_Blocker.isAdHost(request.getUrl().toString())) {
                    return true;
                } else {
                    return super.shouldOverrideUrlLoading(view, request);
                }
            }

            @Override
            public void onScaleChanged(@NonNull WebView view, float oldScale, float newScale) {
                webView.setScaleFactor(newScale);
                JavaScriptHandler.updateWindowHeight((MyWebView) view);
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            @Override
            public void onReceivedSslError(@NonNull WebView view, @NonNull SslErrorHandler handler, @NonNull SslError error) {
                if (!isCurrentWindow) return;
                if (isIgnoreSSLError) {
                    handler.proceed();
                } else {
                    String url = error.getUrl();
                    if (url == null) url = WindowList.getWebUrl();
                    WindowList.onReceivedSslError(url);
                }
            }

            @Override
            public boolean onRenderProcessGone(@NonNull WebView view, @NonNull RenderProcessGoneDetail detail) {
                if (detail.didCrash()) {
                    Message.toastErr(App.Companion.getAppContext().getString(R.string.browser_crashed_error));
                } else {
                    Message.toastErr(App.Companion.getAppContext().getString(R.string.browser_crashed_low_memory));
                }
                view.destroy();
                MainActivityProvider mainActivityProvider = WindowList.getMainActivityProvider();
                webView = mainActivityProvider.createWebView(isPrivate);
                initializeWebView();
                webView.loadUrl(mainActivityProvider.getWebUrl());
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (!isCurrentWindow) return;
                WindowList.onProgressChanged(newProgress);
            }

            @Override
            public void onReceivedIcon(@NonNull WebView view, @NonNull Bitmap icon) {
                super.onReceivedIcon(view, icon);
                if (!isCurrentWindow) return;
                String url = view.getUrl();
                if (url != null) WindowList.onReceivedIcon(url, icon);
            }

            @Override
            public void onReceivedTitle(WebView view, @NonNull String title) {
                if (!isPrivate)
                    new Handler(Looper.getMainLooper()).postDelayed(() -> BrowsingHistory.INSTANCE.debouncedInsert(getUrl(), title), 1000);
            }
        });

        webView.onDoubleTapListener = (idx, x, y) -> {
            if (getLoadingState() != 0 || !isCurrentWindow) return;
            if (isLoadedText && idx >= 0) {
                idx = getReadingIdx(idx);
                if (idx >= 0) WindowList.updateIdx(idx);
            } else if (!isLoadedText) {
                reloadText(x, y, false);
            }
        };

        webView.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
            if (isDoneCounting) {
                if (numberOfMatches == 0) {
                    Message.toastFindInPage(App.Companion.getAppContext().getString(R.string.find_text_not_found));
                } else {
                    Message.toastFindInPage((activeMatchOrdinal + 1) + "/" + numberOfMatches);
                }
            }
        });

        webView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> JavaScriptHandler.updateWindowHeight(webView));

        webView.setOnLongClickListener(v -> {
            WebView.HitTestResult result = ((WebView) v).getHitTestResult();

            int type = result.getType();
            String link = result.getExtra();

            if ((type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) && link != null && !link.equals(lastLongPressedLink)) {

                lastLongPressedLink = link;
                if (isCurrentWindow) WindowList.onLongClick(lastLongPressedLink, isPrivate);
                v.postDelayed(() -> lastLongPressedLink = null, 500);

                return true;
            }
            return false;
        });
    }

    private void addToPage() {
        JavaScriptHandler.loadJavaScriptCode(webView, FileStorage.getJSCode("target", String.valueOf(webView.getHeight() / webView.getScaleFactor())));
    }

    //isDownloadPageAfterLoad: -1 -> not download, 0 -> download, 1 -> append.
    public void getArrFromPage(boolean isPlayAfterLoad, boolean isSamePage, boolean isMoveToIdx, int isDownloadPageAfterLoad, int doubleClicked_x, int doubleClicked_y) {
        if (!isCurrentWindow) return;
        WindowList.onAlertStatusChanged(true);
        webView.evaluateJavascript("(function() { if (window.pageReader) { return window.pageReader.loadContent(); } })();", html -> {
            if (!html.equals("null")) {
                try {
                    JSONArray jsonArray = new JSONArray(getStrFromHTML(html));
                    JSONArray jsonArrayArr = jsonArray.getJSONArray(0);
                    JSONArray jsonArrayNLM = jsonArray.getJSONArray(1);
                    String[] arr = new String[jsonArrayArr.length()];
                    boolean[] nlm = new boolean[arr.length];
                    for (int i = 0; i < arr.length; i++) {
                        arr[i] = jsonArrayArr.getString(i);
                        nlm[i] = jsonArrayNLM.getBoolean(i);
                    }
                    getReadingArr(arr, nlm);
                } catch (JSONException e) {
                    setError(e.toString());
                }
                int idx = getCachedIdx();
                if (idx >= getReadingArr().length) idx = 0;
                //Need delay on some website, which has pop-up layer on double-clicking.
                if (doubleClicked_x >= 0) {
                    int finalIdx = idx;
                    new CountDownTimer(300, 300) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                        }

                        @Override
                        public void onFinish() {
                            webView.evaluateJavascript("(function(x,y) { if (window.pageReader) return window.pageReader.getElementByPosition(x,y); })(" + doubleClicked_x + ", " + doubleClicked_y + ");", html1 -> {
                                if (!isCurrentWindow) return;
                                WindowList.readerInitial(finalIdx, isMoveToIdx, isSamePage);
                                if (html1.equals("null")) return;
                                int nextIdx;
                                try {
                                    nextIdx = Integer.parseInt(html1);
                                } catch (Exception e) {
                                    return;
                                }
                                nextIdx = getReadingIdx(nextIdx);
                                if (nextIdx >= 0) WindowList.updateIdx(nextIdx);
                            });
                        }
                    }.start();
                } else {
                    if (!isCurrentWindow) return;
                    WindowList.readerInitial(idx, isMoveToIdx, isSamePage);
                    if (isPlayAfterLoad && !isReading) WindowList.readerPause();
                    if (isDownloadPageAfterLoad == 0) {
                        FileStorage.DownloadPage(App.Companion.getAppContext(), Objects.requireNonNull(webView.getUrl()), webView.getTitle(), readingArr, idx, false);
                    } else if (isDownloadPageAfterLoad == 1) {
                        FileStorage.DownloadPage(App.Companion.getAppContext(), "", "download", readingArr, idx, true);
                    }
                }
                setLoadingState(0);
                isLoadedText = true;
                if (isCurrentWindow) {
                    WindowList.onAlertStatusChanged(false);
                    WindowList.onLoadedTextStateChanged(true);
                }
            } else {
                webViewLoadUrl(null);
            }
        });
    }

    private void clearPageIdx() {
        JavaScriptHandler.clearIdx(webView);
    }

    public boolean isReading() {
        return isReading;
    }

    public void setReading(boolean isReading) {
        this.isReading = isReading;
    }

    public int getLoadingState() {
        return loadingState;
    }

    private void setLoadingState(int loadingState) {
        this.loadingState = loadingState;
        if (loadingState == 0 && isCurrentWindow) {
            WindowList.onUrlChanged(getUrl());
            WindowList.onResetTextBoxRequired();
        }
    }

    public boolean canGoForward() {
        return webView.canGoForward();
    }

    public boolean getIsLoadedText() {
        return isLoadedText;
    }

    private void setIsStartReadingAfterPageLoaded(boolean isStartReadingAfterPageLoaded) {
        this.isStartReadingAfterPageLoaded = isStartReadingAfterPageLoaded;
    }

    public void reloadText(int doubleClicked_x, int doubleClicked_y, boolean isMoveToIdx) {
        if (getLoadingState() != 0 || !isCurrentWindow) return;
        if (isReading) WindowList.readerPause();
        WindowList.onReadingModeSwitchingRequired(false);
        addToPage();
        getArrFromPage(false, false, isMoveToIdx, -1, doubleClicked_x, doubleClicked_y);
    }

    public void webViewLoadUrl(@Nullable String url) {
        if (!isCurrentWindow) return;
        if (isReading) WindowList.readerPause();
        WindowList.onReadingModeSwitchingRequired(false);
        setLoadingState(1);
        isLoadedText = false;
        WindowList.onLoadedTextStateChanged(false);
        if (url == null) {
            webView.reload();
        } else {
            if (!isValidUrl(url)) url = "https://www.google.com/search?q=" + url;
            webView.loadUrl(url);
        }
    }

    public void pause() {
        if (!isCurrentWindow) return;
        if (BuildConfig.DEBUG && isLastIdx() && playNextPage()) return;

        if (isLoadedText) {
            if (readingArr.length != 0) {
                WindowList.readerPause();
            } else {
                getArrFromPage(true, false, true, -1, -1, -1);
            }
        } else if (getLoadingState() == 0) {
            getArrFromPage(true, false, true, -1, -1, -1);
        } else {
            setIsStartReadingAfterPageLoaded(true);
        }
    }

    public boolean isChangingIdxEnabled() {
        return isCurrentWindow && isLoadedText;
    }

    public boolean forward() {
        if (!isCurrentWindow || !webView.canGoForward()) return false;
        if (isReading) WindowList.readerPause();
        WindowList.onReadingModeSwitchingRequired(false);
        isLoadedText = false;
        WindowList.onLoadedTextStateChanged(false);
        WindowList.onResetTextBoxRequired();
        setLoadingState(2);
        clearReadingArr();
        webView.goForward();
        return true;
    }

    public boolean backward() {
        if (!isCurrentWindow || !webView.canGoBack()) return false;
        if (isReading) WindowList.readerPause();
        WindowList.onReadingModeSwitchingRequired(false);
        isLoadedText = false;
        WindowList.onLoadedTextStateChanged(false);
        WindowList.onResetTextBoxRequired();
        setLoadingState(2);
        clearReadingArr();
        webView.goBack();
        return true;
    }

    public void download(boolean isAppending) {
        if (isAppending) {
            if (!isLoadedText) {
                addToPage();
                getArrFromPage(false, false, false, 1, -1, -1);
            } else {
                FileStorage.DownloadPage(App.Companion.getAppContext(), "", "download", getReadingArr(), 0, true);
            }
        } else {
            if (!isLoadedText) {
                addToPage();
                getArrFromPage(false, false, false, 0, -1, -1);
            } else {
                FileStorage.DownloadPage(App.Companion.getAppContext(), Objects.requireNonNull(getUrl()), getTitle(), readingArr, idx, false);
            }
        }
    }

    public void setDesktopMode(boolean enabled) {
        isDesktopMode = enabled;
        String newUserAgent = webView.getSettings().getUserAgentString();
        if (enabled) {
            try {
                String ua = webView.getSettings().getUserAgentString();
                String androidOSString = webView.getSettings().getUserAgentString().substring(ua.indexOf("("), ua.indexOf(")") + 1);
                newUserAgent = webView.getSettings().getUserAgentString().replace(androidOSString, "(X11; Linux x86_64)");
            } catch (Exception e) {
                Message.toastErr(App.Companion.getAppContext().getString(R.string.error_text_unable_desktop));
            }
        } else {
            newUserAgent = null;
        }
        webView.getSettings().setUseWideViewPort(!enabled);
        webView.getSettings().setLoadWithOverviewMode(!enabled);
        webView.getSettings().setUserAgentString(newUserAgent);
        webView.setInitialScale(100);
        webView.reload();
    }

    public void setIsCurrentWindow(boolean isCurrentWindow, int idx) {
        if (this.isCurrentWindow == isCurrentWindow) return;
        this.isCurrentWindow = isCurrentWindow;
        if (isCurrentWindow) {
            webView.setVisibility(View.VISIBLE);
            WindowList.onUrlChanged(getUrl());
            WindowList.onAppBarLayoutRequired();
            WindowList.onLoadedTextStateChanged(isLoadedText);
            if (loadingState != 0) WindowList.onProgressChanged(100);
            WindowList.onCurrentWindowChanged(idx, isPrivate);
            WindowList.onAlertStatusChanged(false);
        } else {
            webView.setVisibility(View.GONE);
            WindowList.onUrlChanged("");
            WindowList.onProgressChanged(100);
            WindowList.onCurrentWindowChanged(idx, false);
        }
        saveIsCurrentWindow();
    }

    public boolean getIsCurrentWindow() {
        return isCurrentWindow;
    }

    private void runTask(@NonNull Runnable r, boolean isDelay) {
        delayRunHandler.removeCallbacksAndMessages(null);
        if (isDelay) {
            delayRunHandler.postDelayed(r, 1000);
        } else {
            delayRunHandler.postDelayed(r, 0);
        }
    }

    public void acceptSSLError(@NonNull String url) {
        isIgnoreSSLError = true;
        webView.loadUrl(url);
    }

    @NonNull
    public WindowsRecyclerViewNode getRecyclerViewNode() {
        String title = getTitle();
        if (title == null || title.isEmpty()) {
            if (getUrl().isEmpty()) {
                title = App.Companion.getAppContext().getString(R.string.window_title);
            } else if (loadingState != 0) {
                title = "loading...";
            }
        }
        return new WindowsRecyclerViewNode(title, Favicon.getIconFromUrl(getUrl()), this);
    }

    public void remove() {
        SQLitePage.delete(id);
        if (webView.getParent() == null) return;
        ((FrameLayout) webView.getParent()).removeView(webView);
        webView.destroy();
        if (isPrivate) WindowList.CheckAndIncrementPrivateProfileIdx();
    }

    private boolean isValidUrl(@NonNull String url) {
        try {
            if (url.startsWith("http://") || url.startsWith("https://") || url.isEmpty() || url.startsWith("about:") || url.startsWith("file:"))
                return true;
            String cleanedUrl = url.trim().replace(" ", "%20");
            return (Patterns.WEB_URL.matcher(cleanedUrl).matches());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean getIsDesktopMode() {
        return isDesktopMode;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setRemovedWindowIdx(int idx) {
        removedWindowIdx = idx;
    }

    public int getRemovedWindowIdx() {
        return removedWindowIdx;
    }

    public void setIsRemovedWindowCurrent(boolean isRemovedWindowCurrent) {
        this.isRemovedWindowCurrent = isRemovedWindowCurrent;
    }

    public boolean getIsRemovedWindowCurrent() {
        return isRemovedWindowCurrent;
    }

    public int getId() {
        return id;
    }

    public void savePosition(int position) {
        this.position = position;
        SQLitePage.savePosition(id, position);
    }

    public int getPosition() {
        return position;
    }

    public boolean isDownloadedPage() {
        return getUrl().startsWith("file");
    }

    //Debug
    private boolean playNextPage() {
        if (playNextPageState == 0) {
            TextToSpeechManager.speakSystem(App.Companion.getAppContext().getString(R.string.open_next_page));
            WindowList.onReadingModeSwitchingRequired(false);
            if (WindowList.isInForeground()) {
                return playNextPageClick();
            } else {
                playNextPageReload();
                playNextPageState = 1;
                return true;
            }
        } else {
            return true;
        }
    }

    private void playNextPageOnPageFinishedDelay() {
        if (playNextPageState == 1) {
            playNextPageDelayRunHandler.removeCallbacksAndMessages(null);
            playNextPageDelayRunHandler.postDelayed(this::playNextPageOnPageFinished, 1000);
        }
    }

    private void playNextPageOnPageFinished() {
        if (playNextPageState == 1) {
            playNextPageState = 0;
            if (!isCurrentWindow) return;

            //Message.log("loaded");
            WindowList.onAlertStatusChanged(true);
            webView.evaluateJavascript("(function() { if (window.pageReader) { return window.pageReader.loadContent(); } })();", html -> {
                if (!html.equals("null")) {
                    try {
                        JSONArray jsonArray = new JSONArray(getStrFromHTML(html));
                        JSONArray jsonArrayArr = jsonArray.getJSONArray(0);
                        JSONArray jsonArrayNLM = jsonArray.getJSONArray(1);
                        String[] arr = new String[jsonArrayArr.length()];
                        boolean[] nlm = new boolean[arr.length];
                        for (int i = 0; i < arr.length; i++) {
                            arr[i] = jsonArrayArr.getString(i);
                            nlm[i] = jsonArrayNLM.getBoolean(i);
                        }
                        getReadingArr(arr, nlm);
                    } catch (JSONException e) {
                        setError(e.toString());
                    }
                    idx = getCachedIdx();
                    if (idx >= getReadingArr().length) idx = 0;
                    setLoadingState(0);
                    isLoadedText = true;
                    if (isCurrentWindow) {
                        WindowList.onAlertStatusChanged(false);
                        WindowList.onLoadedTextStateChanged(true);
                    }
                    playNextPageClick();
                }
            });
        }
    }

    private int playNextPageFindIdx() {
        String[] keywords = {"next", "下一"};
        int nextPageIdx = -1;
        for (int i = readingArr.length - 1; i >= 0; i--) {
            for (String keyword : keywords) {
                if (readingArr[i].toLowerCase().contains(keyword)) {
                    nextPageIdx = i;
                    break;
                }
            }
            if (nextPageIdx != -1) break;
        }
        return nextPageIdx;
    }

    private boolean playNextPageClick() {
        int nextPageIdx = playNextPageFindIdx();
        if (nextPageIdx != -1) {
            setIsStartReadingAfterPageLoaded(true);
            webView.onResume();
            webView.resumeTimers();
            JavaScriptHandler.performClick(webView, getPageIdx(nextPageIdx));
            //Message.log("click");
            return true;
        } else {
            return false;
        }
    }

    private void playNextPageReload() {
        webView.onResume();
        webView.resumeTimers();
        webView.reload();
        //Message.log("reload");
    }
}
