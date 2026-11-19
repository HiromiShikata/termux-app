package com.termux.app.apkupdate;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.logger.Logger;
import com.termux.shared.notification.NotificationUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        PendingIntent tapIntent = buildInstallPendingIntent(context,
            buildTapDestinationIntent(context, apkInstaller, apkFile));
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

    private static Intent buildTapDestinationIntent(Context context, ApkInstaller apkInstaller, File apkFile) {
        switch (ApkUpdateTapDestination.forInstallPermission(apkInstaller.canRequestPackageInstalls())) {
            case INSTALL_CONFIRMATION:
                return buildInstallConfirmationIntent(context, apkInstaller, apkFile);
            case UNKNOWN_APPS_SETTINGS:
                return apkInstaller.buildInstallUnknownAppsSettingsIntent();
            default:
                throw new IllegalStateException("Unhandled tap destination");
        }
    }

    private static Intent buildInstallConfirmationIntent(Context context, ApkInstaller apkInstaller, File apkFile) {
        Intent installIntent = apkInstaller.buildInstallIntent(apkFile);
        String installerPackageName = InstallerPackageSelector.select(
            resolveInstallActivityPackageNames(context, installIntent));
        if (installerPackageName == null) {
            Logger.logWarn(LOG_TAG,
                "No package installer resolved for the install intent; the tap will show an app chooser");
            return installIntent;
        }
        installIntent.setPackage(installerPackageName);
        return installIntent;
    }

    private static List<String> resolveInstallActivityPackageNames(Context context, Intent installIntent) {
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(installIntent, 0);
        List<String> packageNames = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            packageNames.add(resolveInfo.activityInfo.packageName);
        }
        return packageNames;
    }

    private static int pendingIntentFlags() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            : PendingIntent.FLAG_UPDATE_CURRENT;
    }
}
