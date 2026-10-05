package com.blueberryjoy.reader.main.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.blueberryjoy.reader.R;
import com.blueberryjoy.reader.settings.ReaderSettings;
import com.blueberryjoy.reader.utils.BringToFront;

public class ReaderNotification {
    private final NotificationManager notificationManager;
    private Notification.Builder builder;
    private Notification.Action pauseAction;
    private Notification.Action playAction;
    private Notification.Action prevAction;
    private Notification.Action nextAction;

    public ReaderNotification(@NonNull Context context) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void close() {
        notificationManager.cancel(1);
    }

    public void play() {
        builder.setActions(prevAction, pauseAction, nextAction);
        notificationManager.notify(1, builder.build());
    }

    public void pause() {
        builder.setActions(prevAction, playAction, nextAction);
        notificationManager.notify(1, builder.build());
    }

    public void updateNotification(String str) {
        if (ReaderSettings.getIsShowContentInNotification()) {
            builder.setContentText(str);
            notificationManager.notify(1, builder.build());
        }
    }

    public void clearNotification() {
        builder.setContentText("");
        notificationManager.notify(1, builder.build());
    }

    public void clearNotificationTitle() {
        builder.setContentTitle(" ");
        notificationManager.notify(1, builder.build());
    }

    public void updateNotificationTitle(String str) {
        builder.setContentTitle(str);
        notificationManager.notify(1, builder.build());
    }

    @NonNull
    public Notification getNotification(@NonNull Context context) {
        String id = "readerChannel";
        NotificationChannel notificationChannel = new NotificationChannel(id, "reader", NotificationManager.IMPORTANCE_LOW);
        notificationChannel.setShowBadge(false);
        notificationChannel.setSound(null, null);

        builder = new Notification.Builder(context, id);

        Intent resume = new Intent("resume");
        resume.setClass(context, BringToFront.class);
        PendingIntent resumePendingIntent = PendingIntent.getActivity(context, 0, resume, PendingIntent.FLAG_IMMUTABLE);

        Intent pause = new Intent("pause");
        pause.setClass(context, NotificationBroadcastReceiver.class);
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(context, 1, pause, PendingIntent.FLAG_IMMUTABLE);
        pauseAction = new Notification.Action.Builder(null, context.getString(R.string.notification_button_pause), pausePendingIntent).build();
        playAction = new Notification.Action.Builder(null, context.getString(R.string.notification_button_play), pausePendingIntent).build();

        Intent prev = new Intent("prev");
        prev.setClass(context, NotificationBroadcastReceiver.class);
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(context, 2, prev, PendingIntent.FLAG_IMMUTABLE);
        prevAction = new Notification.Action.Builder(null, context.getString(R.string.notification_button_prev), prevPendingIntent).build();

        Intent next = new Intent("next");
        next.setClass(context, NotificationBroadcastReceiver.class);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(context, 3, next, PendingIntent.FLAG_IMMUTABLE);
        nextAction = new Notification.Action.Builder(null, context.getString(R.string.notification_button_next), nextPendingIntent).build();

        Notification notification = builder
                .setContentIntent(resumePendingIntent)
                .setContentTitle("")
                .setContentText("")
                .setActions(prevAction, playAction, nextAction)
                .setChannelId(id)
                .setSmallIcon(R.drawable.ic_notification_cat_while)
                .build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);
        notificationManager.notify(1, notification);
        return notification;
    }
}
