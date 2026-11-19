package com.termux.app.apkupdate;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.logger.Logger;
import com.termux.shared.notification.NotificationUtils;

import java.io.File;

public final class ApkUpdateNotifier {

    private static final String LOG_TAG = "ApkUpdateNotifier";
    private static final String CHANNEL_ID = "apk_update_available";
    private static final int NOTIFICATION_ID = 1345;

    public static void notify(Context context, ApkUpdateAvailability availability) {
        NotificationManager notificationManager = prepareNotificationManager(context);
        if (notificationManager == null) {
            return;
        }

        PendingIntent tapIntent = buildTermuxActivityIntent(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_service_notification)
            .setContentTitle(context.getString(R.string.apk_update_notification_title))
            .setContentText(context.getString(R.string.apk_update_notification_text,
                availability.getLatestVersionName()))
            .setContentIntent(tapIntent)
            .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public static void notifyDownloadedUpdate(Context context, String latestVersionName, File apkFile,
                                              String changelogText) {
        NotificationManager notificationManager = prepareNotificationManager(context);
        if (notificationManager == null) {
            return;
        }

        ApkInstaller apkInstaller = new ApkInstaller(context);
        PendingIntent tapIntent = apkInstaller.canRequestPackageInstalls()
            ? buildInstallPendingIntent(context, apkInstaller.buildInstallIntent(apkFile))
            : buildTermuxActivityIntent(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_service_notification)
            .setContentTitle(context.getString(R.string.apk_update_notification_title))
            .setContentText(context.getString(R.string.apk_update_notification_text, latestVersionName))
            .setStyle(new NotificationCompat.BigTextStyle().bigText(changelogText))
            .setContentIntent(tapIntent)
            .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private static NotificationManager prepareNotificationManager(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Logger.logInfo(LOG_TAG,
                    "POST_NOTIFICATIONS permission not granted; skipping update available notification");
                return null;
            }
        }

        NotificationManager notificationManager = NotificationUtils.getNotificationManager(context);
        if (notificationManager == null) {
            return null;
        }

        NotificationUtils.setupNotificationChannel(context, CHANNEL_ID,
            context.getString(R.string.apk_update_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT);
        return notificationManager;
    }

    private static PendingIntent buildTermuxActivityIntent(Context context) {
        Intent launchIntent = new Intent(context, TermuxActivity.class);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 0, launchIntent, pendingIntentFlags());
    }

    private static PendingIntent buildInstallPendingIntent(Context context, Intent installIntent) {
        return PendingIntent.getActivity(context, 0, installIntent, pendingIntentFlags());
    }

    private static int pendingIntentFlags() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            : PendingIntent.FLAG_UPDATE_CURRENT;
    }
}
