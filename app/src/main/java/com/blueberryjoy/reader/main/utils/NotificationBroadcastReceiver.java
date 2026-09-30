package com.blueberryjoy.reader.main.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.blueberryjoy.reader.main.window.WindowList;

import java.util.Objects;

public class NotificationBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        String action = intent.getAction();
        switch (Objects.requireNonNull(action)) {
            case "pause":
                //MessageSender.sendMessage(new MessageNotification(MessageNotification.ACTION_PAUSE));
                WindowList.pause();
                break;
            case "next":
                //MessageSender.sendMessage(new MessageNotification(MessageNotification.ACTION_NEXT));
                ReaderController.next();
                break;
            case "prev":
                //MessageSender.sendMessage(new MessageNotification(MessageNotification.ACTION_PREV));
                ReaderController.prev();
                break;
            default:
        }
    }
}
