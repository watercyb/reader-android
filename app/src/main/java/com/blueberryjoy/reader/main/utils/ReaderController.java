package com.blueberryjoy.reader.main.utils;

import android.content.Context;
import android.content.Intent;

import com.blueberryjoy.reader.App;
import com.blueberryjoy.reader.main.ReaderForeground;

public class ReaderController {

    public static final String ACTION_INITIAL = "initial";
    public static final String ACTION_INITIAL_SAME_PAGE = "initialSamePage";
    public static final String ACTION_RESUME = "resume";
    public static final String ACTION_UNFOCUSED = "unfocused";
    public static final String ACTION_REFRESH_WITHOUT_STOP = "refreshWithoutStop";
    public static final String ACTION_NOTIFICATION_SETTING_CHANGED = "notificationSettingChanged";
    public static final String ACTION_STOP_READING_AND_CLEAR_NOTIFICATION = "stopReadingAndClearNotification";
    public static final String ACTION_UPDATE_MEDIA_TITLE = "updateMediaTitle";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_CLEAR_NOTIFICATION = "clearNotification";
    public static final String ACTION_PREV = "prev";
    public static final String ACTION_NEXT = "next";
    public static final String ACTION_IDX = "idx";
    public static final String ACTION_CLEAR = "clear";
    public static final String ACTION_CAN_PLAY = "canPlay";

    public static void resume() {
        Context context = App.Companion.getAppContext();
        Intent intent = new Intent(context, ReaderForeground.class);
        intent.setAction(ACTION_RESUME);
        context.startService(intent);
    }

    public static void unfocused() {
        Context context = App.Companion.getAppContext();
        Intent intent = new Intent(context, ReaderForeground.class);
        intent.setAction(ACTION_UNFOCUSED);
        context.startService(intent);
    }

    public static void initialize(int idx, boolean isMoveToIdx, boolean isSamePage) {
        Context context = App.Companion.getAppContext();
        Intent intent = new Intent(context, ReaderForeground.class);
        intent.putExtra("idx", idx);
        intent.putExtra("move", isMoveToIdx);
        if (!isSamePage) {
            intent.setAction(ACTION_INITIAL);
        } else {
            intent.setAction(ACTION_INITIAL_SAME_PAGE);
        }
        context.startService(intent);
    }

    public static void refreshWithoutStop() {
        Context context = App.Companion.getAppContext();
        Intent intent = new Intent(context, ReaderForeground.class);
        intent.putExtra("move", true);
        intent.setAction(ACTION_REFRESH_WITHOUT_STOP);
        context.startService(intent);
    }

    public static void notificationSettingChanged() {
        Context context = App.Companion.getAppContext();
        Intent intent = new Intent(context, ReaderForeground.class);
        intent.setAction(ACTION_NOTIFICATION_SETTING_CHANGED);
        context.startService(intent);
    }

    public static void stopReadingAndClearNotification() {
        Context context = App.Companion.getAppContext();
        Intent intent = new Intent(context, ReaderForeground.class);
        intent.setAction(ACTION_STOP_READING_AND_CLEAR_NOTIFICATION);
        context.startService(intent);
    }

    public static void updateMediaTitle() {
        Context context = App.Companion.getAppContext();
        Intent intent = new Intent(context, ReaderForeground.class);
        intent.setAction(ACTION_UPDATE_MEDIA_TITLE);
        context.startService(intent);
    }

    public static void pause() {
        Context context = App.Companion.getAppContext();
        Intent intent = new Intent(context, ReaderForeground.class);
        intent.setAction(ACTION_PAUSE);
        context.startService(intent);
    }

    public static void clearNotification() {
        Context context = App.Companion.getAppContext();
        Intent intent = new Intent(context, ReaderForeground.class);
        intent.setAction(ACTION_CLEAR_NOTIFICATION);
        context.startService(intent);
    }

    public static void prev() {
        Context context = App.Companion.getAppContext();
        Intent intent = new Intent(context, ReaderForeground.class);
        intent.setAction(ACTION_PREV);
        context.startService(intent);
    }

    public static void next() {
        Context context = App.Companion.getAppContext();
        Intent intent = new Intent(context, ReaderForeground.class);
        intent.setAction(ACTION_NEXT);
        context.startService(intent);
    }

    public static void idx(int idx) {
        Context context = App.Companion.getAppContext();
        Intent intent = new Intent(context, ReaderForeground.class);
        intent.putExtra("idx", idx);
        intent.setAction(ACTION_IDX);
        context.startService(intent);
    }

    public static void clear() {
        Context context = App.Companion.getAppContext();
        Intent intent = new Intent(context, ReaderForeground.class);
        intent.setAction(ACTION_CLEAR);
        context.startService(intent);
    }

    public static void canPlay(boolean enable) {
        Context context = App.Companion.getAppContext();
        Intent intent = new Intent(context, ReaderForeground.class);
        intent.putExtra("enable", enable);
        intent.setAction(ACTION_CAN_PLAY);
        context.startService(intent);
    }
}