package com.blueberryjoy.reader.main.window;

import android.graphics.Bitmap;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.ProfileStore;
import androidx.webkit.WebViewFeature;

import com.blueberryjoy.reader.main.utils.MainActivityProvider;
import com.blueberryjoy.reader.main.utils.ReaderController;
import com.blueberryjoy.reader.main.utils.WindowListener;
import com.blueberryjoy.reader.sqlite.page.SQLitePage;
import com.blueberryjoy.reader.main.webview.MyWebView;
import com.blueberryjoy.reader.sqlite.page.Page;
import com.blueberryjoy.reader.main.recyclerview.WindowsRecyclerViewItems;
import com.blueberryjoy.reader.main.recyclerview.WindowsRecyclerViewNode;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;

public class WindowList {
    private static final List<Window> list = new ArrayList<>();
    @Nullable
    private static Window currentWindow;
    private static MainActivityProvider mainActivityProvider;
    private static int privateProfileIdx = 0;
    private static WindowListener WindowListener;
    private static CountDownTimer updatePositionTimer;

    public static void initializeWindowList(@NonNull MainActivityProvider mainActivityProvider, @NonNull WindowListener WindowListener) {
        WindowList.mainActivityProvider = mainActivityProvider;
        WindowList.WindowListener = WindowListener;
        SQLitePage.getPages((pages) -> {
            list.clear();
            if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                boolean hasPrivate = false;
                for (Page page : pages) {
                    if (page.isPrivate) {
                        hasPrivate = true;
                        break;
                    }
                }

                if (!hasPrivate) {
                    clearPrivateProfile(-1);
                } else {
                    updatePrivateProfileIdx();
                    clearPrivateProfile(privateProfileIdx);
                }
            }

            int idx = -1;
            for (Page page : pages) {
                MyWebView webView = createWebView(page.isPrivate);
                Window window = new Window(webView, page);
                list.add(window);
                if (page.isCurrent) idx = list.size() - 1;
            }
            setCurrentWindow(idx);
            WindowList.onWindowNumberChanged(list.size());
            return Unit.INSTANCE;
        });
    }

    public static WindowsRecyclerViewNode restore(@NonNull Window window) {
        boolean isCurrent = window.getIsRemovedWindowCurrent();
        window.setIsCurrentWindow(false, -1);
        int idx = window.getRemovedWindowIdx();
        if (idx < 0 || idx > list.size()) idx = list.size();
        list.add(idx, window);
        if (isCurrent) setCurrentWindow(idx);
        return window.getRecyclerViewNode();
    }

    public static void switchWindow(Window window) {
        int idx = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == window) {
                idx = i;
                break;
            }
        }
        setCurrentWindow(idx);
    }

    public static WindowsRecyclerViewNode createAndInsert(@NonNull String url, int idx, boolean isSwitch, boolean isPrivate) {
        MyWebView webView = createWebView(isPrivate);
        int position = list.isEmpty() ? 0 : list.get(list.size() - 1).getPosition() + 1000;
        Window window = new Window(webView, url, idx, isPrivate, position);
        list.add(window);
        if (isSwitch) {
            if (list.size() > 1) setCurrentWindow(list.size() - 1);
        } else {
            if (list.size() == 1) setCurrentWindow(0);
        }
        WindowList.onWindowNumberChanged(list.size());
        return window.getRecyclerViewNode();
    }

    public static boolean removeWindow(@NonNull Window window) {
        int idx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == window) {
                idx = i;
                break;
            }
        }
        if (idx == -1) return false;
        if (list.get(idx) == currentWindow) {
            window.setIsCurrentWindow(false, -1);
            window.setIsRemovedWindowCurrent(true);
            list.remove(idx);
            int nextCurrentIdx = Math.max(idx - 1, 0);
            if (nextCurrentIdx >= list.size()) {
                currentWindow = null;
            } else {
                setCurrentWindow(nextCurrentIdx);
            }
        } else {
            window.setIsRemovedWindowCurrent(false);
            list.remove(idx);
        }
        WindowList.onWindowNumberChanged(list.size());
        window.setRemovedWindowIdx(idx);
        return true;
    }

    public static void setCurrentWindow(int idx) {
        currentWindow = null;
        //int oldIdx = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getIsCurrentWindow()) {
                list.get(i).setIsCurrentWindow(false, i);
                //oldIdx = i;
            }
        }
        if (idx >= 0 && idx < list.size()) {
            currentWindow = list.get(idx);
            currentWindow.setIsCurrentWindow(true, idx);
            updateMediaTitle();
        }
    }

    public static void checkAndSetCurrentWindow() {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getIsCurrentWindow()) {
                setCurrentWindow(i);
            }
        }
    }

    @Nullable
    public static Window getCurrentWindow() {
        if (currentWindow == null) checkAndSetCurrentWindow();
        return currentWindow;
    }

    public static MainActivityProvider getMainActivityProvider() {
        return mainActivityProvider;
    }

    public static void setIdxWithoutSave(int idx) {
        if (currentWindow != null) currentWindow.setIdxWithoutSave(idx);
    }

    @Nullable
    public static String getTitle() {
        if (currentWindow != null) return currentWindow.getTitle();
        return "";
    }

    public static void addPageToCache(String url, int idx, String title) {
        if (currentWindow != null) {
            currentWindow.addPageToCache(url, idx, title, currentWindow.getWebView().getState());
        }
    }

    public static boolean isValidReadingStatus() {
        return currentWindow != null && currentWindow.isValidReadingStatus();
    }

    public static String getCurrentStr() {
        if (currentWindow != null) return currentWindow.getCurrentStr();
        return "";
    }

    public static boolean hasNext() {
        return currentWindow != null && currentWindow.hasNext();
    }

    public static void moveToNext() {
        if (currentWindow != null) currentWindow.nextIdx();
    }

    public static void resetNextIdx() {
        if (currentWindow != null) currentWindow.resetNextIdx();
    }

    public static void idxInc() {
        if (currentWindow != null) currentWindow.idxInc();
    }

    public static void idxDecForPrv() {
        if (currentWindow != null) currentWindow.idxDecForPrv();
    }

    public static void idxDec() {
        if (currentWindow != null) currentWindow.idxDec();
    }

    public static boolean isReading() {
        return currentWindow != null && currentWindow.isReading();
    }

    public static boolean isLastIdx() {
        return currentWindow != null && currentWindow.isLastIdx();
    }

    public static String getUrl() {
        if (currentWindow != null) return currentWindow.getUrl();
        return "";
    }

    public static boolean isLoadedText() {
        return currentWindow != null && currentWindow.getIsLoadedText();
    }

    public static void getArrFromPage(boolean isPlayAfterLoad, boolean isSamePage, boolean isMoveToIdx, int isDownloadPageAfterLoad, int doubleClicked_x, int doubleClicked_y) {
        if (currentWindow != null)
            currentWindow.getArrFromPage(isPlayAfterLoad, isSamePage, isMoveToIdx, isDownloadPageAfterLoad, doubleClicked_x, doubleClicked_y);
    }

    @NonNull
    public static MyWebView getWebView() {
        if (currentWindow != null) return currentWindow.getWebView();
        createAndInsert("", 0, false, false);
        return currentWindow.getWebView();
    }

    public static int getIdx() {
        if (currentWindow != null) return currentWindow.getIdx();
        return 0;
    }

    public static int getCachedIdx() {
        if (currentWindow != null) return currentWindow.getCachedIdx();
        return 0;
    }

    public static boolean forward() {
        return currentWindow != null && currentWindow.forward();
    }

    public static boolean backward() {
        return currentWindow != null && currentWindow.backward();
    }

    public static boolean canGoForward() {
        return currentWindow != null && currentWindow.canGoForward();
    }

    public static void webViewLoadUrl(String url) {
        if (currentWindow != null) currentWindow.webViewLoadUrl(url);
    }

    public static void download(boolean isAppending) {
        if (currentWindow != null) currentWindow.download(isAppending);
    }

    public static void reloadText(int doubleClicked_x, int doubleClicked_y, boolean isMoveToIdx) {
        if (currentWindow != null)
            currentWindow.reloadText(doubleClicked_x, doubleClicked_y, isMoveToIdx);
    }

    public static void setDesktopMode(boolean enabled) {
        if (currentWindow != null) currentWindow.setDesktopMode(enabled);
    }

    public static int getLoadingState() {
        if (currentWindow != null) return currentWindow.getLoadingState();
        return -2;
    }

    public static void pause() {
        if (currentWindow != null) currentWindow.pause();
    }

    public static void setReading(boolean isReading) {
        if (currentWindow != null) currentWindow.setReading(isReading);
    }

    @Nullable
    public static JSONArray getPageIdx() {
        if (currentWindow != null) return currentWindow.getPageIdx();
        return null;
    }

    public static int getReadingArrLength() {
        if (currentWindow != null) return currentWindow.getReadingArr().length;
        return 0;
    }

    public static int getReadingIdx(int idx) {
        if (currentWindow != null) return currentWindow.getReadingIdx(idx);
        return -1;
    }

    public static void acceptSSLError(@NonNull String url) {
        if (currentWindow != null) currentWindow.acceptSSLError(url);
    }

    public static int size() {
        return list.size();
    }

    @NonNull
    public static WindowsRecyclerViewItems getWindowsRecyclerViewItems() {
        WindowsRecyclerViewItems items = new WindowsRecyclerViewItems();
        for (int i = 0; i < list.size(); i++) {
            items.insert(list.get(i).getRecyclerViewNode());
        }
        return items;
    }

    public static boolean getIsDesktopMode() {
        return currentWindow != null && currentWindow.getIsDesktopMode();
    }

    public static boolean hasPrivateWindow() {
        for (Window window : list) {
            if (window.isPrivate()) return true;
        }
        return false;
    }

    public static void clearPrivateProfile(int currentIdx) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            for (String profileName : ProfileStore.getInstance().getAllProfileNames()) {
                try {
                    int idx = getPrivateProfileIdxFromName(profileName);
                    if (idx >= 0 && idx != currentIdx)
                        ProfileStore.getInstance().deleteProfile(profileName);
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    public static void updatePrivateProfileIdx() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            for (String profileName : ProfileStore.getInstance().getAllProfileNames()) {
                privateProfileIdx = Math.max(privateProfileIdx, getPrivateProfileIdxFromName(profileName));
            }
        }
    }

    public static int getPrivateProfileIdxFromName(String profileName) {
        if (profileName.startsWith("incognito_profile")) {
            String[] strs = profileName.split("_");
            if (strs.length == 3) {
                try {
                    return Integer.parseInt(strs[2]);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    public static void CheckAndIncrementPrivateProfileIdx() {
        if (!hasPrivateWindow()) privateProfileIdx++;
    }

    public static int getPrivateProfileIdx() {
        return privateProfileIdx;
    }

    public static void readerInitial(int idx, boolean isMoveToIdx, boolean isSamePage) {
        ReaderController.initialize(idx, isMoveToIdx, isSamePage);
        onResetTextBoxRequired();
    }

    public static void updateIdx(int idx) {
        ReaderController.idx(idx);
    }

    public static void readerPause() {
        ReaderController.pause();
    }

    public static void clearNotification() {
        ReaderController.clearNotification();
    }

    public static void readingArrCleared() {
        ReaderController.clear();
    }

    public static void updateMediaTitle() {
        ReaderController.updateMediaTitle();
    }

    public static void onProgressChanged(int newProgress) {
        if (WindowListener != null) WindowListener.onProgressChanged(newProgress);
    }

    public static void onReceivedIcon(@NonNull String url, @NonNull Bitmap icon) {
        if (WindowListener != null) WindowListener.onReceivedIcon(url, icon);
    }

    public static void onLoadedTextStateChanged(boolean bool) {
        if (WindowListener != null) WindowListener.onLoadedTextStateChanged(bool);
    }

    public static void onAlertStatusChanged(boolean enable) {
        if (WindowListener != null) WindowListener.onAlertStatusChanged(enable);
    }

    public static void onUrlChanged(String url) {
        if (WindowListener != null) WindowListener.onUrlChanged(url);
    }

    public static void onResetTextBoxRequired() {
        if (WindowListener != null) WindowListener.onResetTextBoxRequired();
    }

    public static void onSwipeRefreshRequired(boolean enable) {
        if (WindowListener != null) WindowListener.onSwipeRefreshRequired(enable);
    }

    public static void onAppBarLayoutRequired() {
        if (WindowListener != null) WindowListener.onAppBarLayoutRequired();
    }

    public static void onReceivedSslError(String url) {
        if (WindowListener != null) WindowListener.onReceivedSslError(url);
    }

    public static void onLongClick(String url, boolean isPrivate) {
        if (WindowListener != null) WindowListener.onLongClick(url, isPrivate);
    }

    public static void onWindowNumberChanged(int number) {
        if (WindowListener != null) WindowListener.onWindowNumberChanged(number);
    }

    public static void onReadingModeSwitchingRequired(boolean isSwitchToReadingMode) {
        if (WindowListener != null)
            WindowListener.onReadingModeSwitchingRequired(isSwitchToReadingMode);
    }

    public static void onCurrentWindowChanged(int idx, boolean isPrivate) {
        if (WindowListener != null)
            WindowListener.onCurrentWindowChanged(idx, isPrivate);
    }

    public static String getWebUrl() {
        if (mainActivityProvider != null) {
            return mainActivityProvider.getWebUrl();
        } else {
            return "";
        }
    }

    public static boolean isReadingMode() {
        if (mainActivityProvider != null) {
            return mainActivityProvider.isReadingMode();
        } else {
            return false;
        }
    }

    public static MyWebView createWebView(boolean isPrivate) {
        return mainActivityProvider.createWebView(isPrivate);
    }

    public static void onPageScroll(int position) {
        WindowListener.onPageScroll(position);
    }

    public static void onTextNotMatching() {
        WindowListener.onTextNotMatching();
    }

    public static void move(int from, int to) {
        Window window = list.remove(from);
        list.add(to, window);

        if (updatePositionTimer != null) updatePositionTimer.cancel();
        updatePositionTimer = new CountDownTimer(500, 500) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                for (int i = 0; i < list.size(); i++) {
                    Window window = list.get(i);
                    if (window.getPosition() != i) window.savePosition(i);
                }
            }
        }.start();
    }
}
