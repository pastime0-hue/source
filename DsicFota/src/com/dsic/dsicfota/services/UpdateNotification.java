package com.dsic.dsicfota.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.service.notification.StatusBarNotification;

import com.dsic.dsicfota.MainActivity;
import com.dsic.dsicfota.declaration.FotaConstants;


public class UpdateNotification {
    private NotificationManager _notificationManager = null;
    private static UpdateNotification _update_notification = null;
    private UpdateNotification(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("parameter is null");
        }
        this._context = context;
    }

    public static UpdateNotification getInstance(Context context){
        if(_update_notification == null){
            _update_notification = new UpdateNotification(context);
        }
        return _update_notification;
    }

    private Context get_context() {
        return _context;
    }

    private Context _context = null;

    public boolean startNotification(String message) {
        if (get_context() == null) {
            return false;
        }

        _notificationManager = (NotificationManager) get_context().getSystemService(Context.NOTIFICATION_SERVICE);

        String CHANNEL_ID = "_DSIC_FOTA_CHANNEL_01_";
        CharSequence NAME = "_DSIC_FOTA_CHANNEL_";
        String DESCRIPTION = "DSIC FOTA CHANNEL";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,NAME,importance);
        channel.setDescription(DESCRIPTION);
        channel.enableLights(true);
        channel.setLightColor(Color.RED);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{100, 200, 100, 200});
        channel.setShowBadge(false);
        _notificationManager.createNotificationChannel(channel);

        Notification.Builder notificationBuilder = new Notification.Builder(get_context(),CHANNEL_ID)
                .setContentTitle(get_context().getString(com.dsic.dsicfota.R.string.app_name))
                .setSmallIcon(com.dsic.dsicfota.R.drawable.ic_ota_http)
                .setContentIntent(pendingIntent(get_context()))
                .setWhen(System.currentTimeMillis());


        //null이 아니면 메세지 Set
        if (message != null) {
            notificationBuilder.setContentText(message);
        }


        Notification notification = notificationBuilder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        _notificationManager.notify(FotaConstants.NOTIFY_NUMBER, notification);

        return true;
    }

    public void stopNotification(){
        if(_notificationManager != null){
            _notificationManager.cancelAll();
        }
    }

    private PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(FotaConstants.ACTION_UPDATE_NOTIFICATION,FotaConstants.EVENT_START);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(intent);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static boolean is_notified(Context context){
        boolean result = false;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == FotaConstants.NOTIFY_NUMBER) {
                result = true;
                break;
            }
        }
        return result;
    }
}
