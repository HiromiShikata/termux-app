package com.termux.app.terminal;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.logger.Logger;

import java.util.Set;

public final class CallToUserAlertNotifier {
    private static final String LOG_TAG = "CallToUserAlertNotifier";

    static final String CHANNEL_ID = "termux_call_to_user_alert_channel";
    static final int NOTIFICATION_ID = 1346;
    static final String EXTRA_CALL_TO_USER_SESSION_NAME = "com.termux.app.extra.CALL_TO_USER_SESSION_NAME";

    private static final String TAP_DATA_SCHEME = "termux-call-to-user";
    private static final String PREFERENCES_NAME = "call_to_user_alerts";
    private static final String PREFERENCES_KEY_ALERTED_IDENTITIES = "alerted_identities";
    private static final long[] VIBRATION_PATTERN_MILLIS = {0L, 250L, 250L, 250L};

    private CallToUserAlertNotifier() {
    }

    public static void setupChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager notificationManager = notificationManager(context);
        if (notificationManager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
            context.getString(R.string.call_to_user_alert_channel_name), NotificationManager.IMPORTANCE_HIGH);
        channel.enableVibration(true);
        channel.setVibrationPattern(VIBRATION_PATTERN_MILLIS);
        channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null);
        notificationManager.createNotificationChannel(channel);
    }

    @NonNull
    public static CallToUserAlertLedger.Storage ledgerStorage(@NonNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return new CallToUserAlertLedger.Storage() {
            @Override
            public String read() {
                return preferences.getString(PREFERENCES_KEY_ALERTED_IDENTITIES, null);
            }

            @Override
            public void write(@NonNull String value) {
                preferences.edit().putString(PREFERENCES_KEY_ALERTED_IDENTITIES, value).commit();
            }
        };
    }

    public static void postAlert(@NonNull Context context, @NonNull String sessionName, @NonNull String callText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            Logger.logInfo(LOG_TAG, "POST_NOTIFICATIONS permission not granted; skipping call alert");
            return;
        }
        NotificationManager notificationManager = notificationManager(context);
        if (notificationManager == null) return;

        PendingIntent tapIntent = PendingIntent.getActivity(context, 0,
            buildTapIntent(context, sessionName), pendingIntentFlags());
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_service_notification)
            .setContentTitle(sessionName)
            .setContentText(callText)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(callText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build();
        notificationManager.notify(CallToUserNotificationTags.tagFor(sessionName), NOTIFICATION_ID, notification);
    }

    public static void cancelAlertsNotPending(@NonNull Context context, @NonNull Set<String> pendingSessionNames,
                                              @NonNull Set<String> previouslyPendingSessionNames) {
        NotificationManager notificationManager = notificationManager(context);
        if (notificationManager == null) return;
        for (String previouslyPending : previouslyPendingSessionNames) {
            if (!pendingSessionNames.contains(previouslyPending)) {
                notificationManager.cancel(CallToUserNotificationTags.tagFor(previouslyPending), NOTIFICATION_ID);
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        for (StatusBarNotification active : notificationManager.getActiveNotifications()) {
            String sessionName = CallToUserNotificationTags.sessionNameFromTag(active.getTag());
            if (sessionName != null && active.getId() == NOTIFICATION_ID
                && !pendingSessionNames.contains(sessionName)) {
                notificationManager.cancel(active.getTag(), NOTIFICATION_ID);
            }
        }
    }

    @Nullable
    public static String sessionNameFromTapIntent(@Nullable Intent intent) {
        if (intent == null) return null;
        return intent.getStringExtra(EXTRA_CALL_TO_USER_SESSION_NAME);
    }

    @NonNull
    static Intent buildTapIntent(@NonNull Context context, @NonNull String sessionName) {
        Intent intent = new Intent(context, TermuxActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_CALL_TO_USER_SESSION_NAME, sessionName);
        intent.setData(Uri.parse(TAP_DATA_SCHEME + "://session/" + Uri.encode(sessionName)));
        return intent;
    }

    private static int pendingIntentFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return flags;
    }

    @Nullable
    private static NotificationManager notificationManager(@NonNull Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
