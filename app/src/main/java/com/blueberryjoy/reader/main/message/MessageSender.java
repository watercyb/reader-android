package com.blueberryjoy.reader.main.message;

import androidx.annotation.NonNull;

import io.reactivex.rxjava3.subjects.PublishSubject;

public class MessageSender {
    static final PublishSubject<MessageNode> messageSender = PublishSubject.create();

    public static void sendMessage(@NonNull MessageNode messageNode) {
        messageSender.onNext(messageNode);
    }

    @NonNull
    public static PublishSubject<MessageNode> getMessageSender() {
        return messageSender;
    }
}
