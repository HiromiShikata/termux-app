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

import java.util.HashSet;
import java.util.Set;

public final class CallToUserAlertNotifier {
    private static final String LOG_TAG = "CallToUserAlertNotifier";

    static final String CHANNEL_ID = "termux_call_to_user_alert_channel";
    static final int NOTIFICATION_ID = 1346;
    static final String EXTRA_CALL_TO_USER_SESSION_NAME = "com.termux.app.extra.CALL_TO_USER_SESSION_NAME";

    private static final String TAP_DATA_SCHEME = "termux-call-to-user";
    static final String PREFERENCES_NAME = "call_to_user_alerts";
    private static final String PREFERENCES_KEY_ALERTED_IDENTITIES = "alerted_identities";
    private static final String PREFERENCES_KEY_SHOWN_SESSION_NAMES = "shown_session_names";
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
    private static SharedPreferences preferences(@NonNull Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public static CallToUserAlertLedger.Storage ledgerStorage(@NonNull Context context) {
        SharedPreferences preferences = preferences(context);
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
        if (!canPostAlerts(context, sessionName)) return;
        NotificationManager notificationManager = notificationManager(context);
        if (notificationManager == null) return;
        notificationManager.notify(CallToUserNotificationTags.tagFor(sessionName), NOTIFICATION_ID,
            buildAlert(context, sessionName, callText, false));
        Set<String> shownSessionNames = readShownSessionNames(context);
        shownSessionNames.add(sessionName);
        writeShownSessionNames(context, shownSessionNames);
    }

    /**
     * Replaces the text of the alert already shown for {@code sessionName} with the body of the owner
     * call it is about. The alert keeps its place and its identity, and the update does not sound again.
     * Does nothing when no alert is shown for the session, for example because the owner answered it first.
     */
    public static boolean isAlertShown(@NonNull Context context, @NonNull String sessionName) {
        return readShownSessionNames(context).contains(sessionName);
    }

    public static void updateAlertBody(@NonNull Context context, @NonNull String sessionName,
                                       @NonNull String callBody) {
        if (!isAlertShown(context, sessionName)) return;
        if (!canPostAlerts(context, sessionName)) return;
        NotificationManager notificationManager = notificationManager(context);
        if (notificationManager == null) return;
        notificationManager.notify(CallToUserNotificationTags.tagFor(sessionName), NOTIFICATION_ID,
            buildAlert(context, sessionName, callBody, true));
    }

    private static boolean canPostAlerts(@NonNull Context context, @NonNull String sessionName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            Logger.logWarn(LOG_TAG, "Skipping the call alert for session \"" + sessionName
                + "\": the POST_NOTIFICATIONS permission is not granted");
            return false;
        }
        return true;
    }

    @NonNull
    private static Notification buildAlert(@NonNull Context context, @NonNull String sessionName,
                                           @NonNull String text, boolean onlyAlertOnce) {
        PendingIntent tapIntent = PendingIntent.getActivity(context, 0,
            buildTapIntent(context, sessionName), pendingIntentFlags());
        return new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_service_notification)
            .setContentTitle(sessionName)
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
            .setOnlyAlertOnce(onlyAlertOnce)
            .setContentIntent(tapIntent)
            .build();
    }

    public static void cancelAlertsNotPending(@NonNull Context context, @NonNull Set<String> pendingSessionNames) {
        NotificationManager notificationManager = notificationManager(context);
        if (notificationManager == null) return;
        Set<String> knownSessionNames = readShownSessionNames(context);
        knownSessionNames.addAll(activeAlertSessionNames(notificationManager));
        Set<String> sessionNamesToCancel =
            CallToUserAlertCancellation.sessionsToCancel(knownSessionNames, pendingSessionNames);
        for (String sessionName : sessionNamesToCancel) {
            notificationManager.cancel(CallToUserNotificationTags.tagFor(sessionName), NOTIFICATION_ID);
            knownSessionNames.remove(sessionName);
        }
        writeShownSessionNames(context, knownSessionNames);
    }

    @NonNull
    private static Set<String> activeAlertSessionNames(@NonNull NotificationManager notificationManager) {
        Set<String> sessionNames = new HashSet<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return sessionNames;
        for (StatusBarNotification active : notificationManager.getActiveNotifications()) {
            String sessionName = CallToUserNotificationTags.sessionNameFromTag(active.getTag());
            if (sessionName != null && active.getId() == NOTIFICATION_ID) {
                sessionNames.add(sessionName);
            }
        }
        return sessionNames;
    }

    @NonNull
    private static Set<String> readShownSessionNames(@NonNull Context context) {
        Set<String> stored = preferences(context).getStringSet(PREFERENCES_KEY_SHOWN_SESSION_NAMES, null);
        return stored == null ? new HashSet<>() : new HashSet<>(stored);
    }

    private static void writeShownSessionNames(@NonNull Context context, @NonNull Set<String> sessionNames) {
        preferences(context).edit().putStringSet(PREFERENCES_KEY_SHOWN_SESSION_NAMES, new HashSet<>(sessionNames))
            .commit();
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
