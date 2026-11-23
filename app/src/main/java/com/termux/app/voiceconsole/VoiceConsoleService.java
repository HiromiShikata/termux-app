package com.termux.app.voiceconsole;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import com.termux.R;

public final class VoiceConsoleService extends Service {

    public static final String ACTION_START = "com.termux.app.voiceconsole.action.START";
    public static final String ACTION_STOP = "com.termux.app.voiceconsole.action.STOP";

    private static final String NOTIFICATION_CHANNEL_ID = "termux_voice_console";
    private static final int NOTIFICATION_ID = 7001;
    private static final int REQUEST_CODE_STOP = 7002;

    public static Intent startIntent(Context context) {
        return new Intent(context, VoiceConsoleService.class).setAction(ACTION_START);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            VoiceConsoleRuntime.stop();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        createNotificationChannel();
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        VoiceConsoleRuntime.stop();
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
            getString(R.string.voice_console_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        Intent stopIntent = new Intent(this, VoiceConsoleService.class).setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, REQUEST_CODE_STOP, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            : new Notification.Builder(this);
        return builder
            .setSmallIcon(R.drawable.ic_toolbar_voice_console)
            .setContentTitle(getString(R.string.voice_console_notification_title))
            .setContentText(getString(R.string.voice_console_notification_text))
            .setOngoing(true)
            .addAction(new Notification.Action.Builder(null,
                getString(R.string.voice_console_notification_stop_action), stopPendingIntent).build())
            .build();
    }
}
