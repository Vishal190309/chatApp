package com.mechat.app.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class OreoNotification extends ContextWrapper {

    private static final String CHANNEL_ID = "com.mechat.app";
    private static final String CHANNEL_NAME = "app";

    private NotificationManager notificationManager;

    public OreoNotification(Context base) {
        super(base);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannel() {

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
        channel.enableVibration(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        getManager().createNotificationChannel(channel);
    }

    public NotificationManager getManager() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        return notificationManager;
    }

    @TargetApi(Build.VERSION_CODES.O)
    public Notification.Builder getOreoNotification(String title, String message,
                                                    PendingIntent pendingIntent, Uri soundUri, String icon, Notification.Style style) {
        return new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(style)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH)
                .setColor(Color.BLUE)
                .setSmallIcon(Integer.parseInt(icon))
                .setSound(soundUri)
                .setAutoCancel(true);
    }


}
