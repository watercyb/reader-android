package com.blueberryjoy.reader.main.utils;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

public interface WindowListener {
    void onProgressChanged(int newProgress);

    void onReceivedIcon(@NonNull String url, @NonNull Bitmap icon);

    void onLoadedTextStateChanged(boolean bool);

    void onAlertStatusChanged(boolean enable);

    void onUrlChanged(String url);

    void onResetTextBoxRequired();

    void onSwipeRefreshRequired(boolean enable);

    void onAppBarLayoutRequired();

    void onReceivedSslError(String url);

    void onLongClick(String url, boolean isPrivate);

    void onWindowNumberChanged(int number);

    void onReadingModeSwitchingRequired(boolean isSwitchToReadingMode);

    void onCurrentWindowChanged(int idx, boolean isPrivate);

    void onTextNotMatching();

    void onPageScroll(int position);
}