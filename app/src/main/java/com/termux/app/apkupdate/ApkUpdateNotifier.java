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

public final class ApkUpdateNotifier {

    private static final String LOG_TAG = "ApkUpdateNotifier";
    private static final String CHANNEL_ID = "apk_update_available";
    private static final int NOTIFICATION_ID = 1345;

    public static void notify(Context context, ApkUpdateAvailability availability) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Logger.logInfo(LOG_TAG,
                    "POST_NOTIFICATIONS permission not granted; skipping update available notification");
                return;
            }
        }

        NotificationManager notificationManager = NotificationUtils.getNotificationManager(context);
        if (notificationManager == null) {
            return;
        }

        NotificationUtils.setupNotificationChannel(context, CHANNEL_ID,
            context.getString(R.string.apk_update_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT);

        int pendingIntentFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            : PendingIntent.FLAG_UPDATE_CURRENT;

        Intent launchIntent = new Intent(context, TermuxActivity.class);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tapIntent = PendingIntent.getActivity(context, 0, launchIntent, pendingIntentFlags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_service_notification)
            .setContentTitle(context.getString(R.string.apk_update_notification_title))
            .setContentText(context.getString(R.string.apk_update_notification_text,
                availability.getLatestVersionName()))
            .setContentIntent(tapIntent)
            .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
