package com.termux.app.link;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class NativeAppLink {

    public static final class NativeAppTarget {

        private final String mAppDisplayName;

        private final String mPackageName;

        NativeAppTarget(@NonNull String appDisplayName, @NonNull String packageName) {
            this.mAppDisplayName = appDisplayName;
            this.mPackageName = packageName;
        }

        @NonNull
        public String getAppDisplayName() {
            return mAppDisplayName;
        }

        @NonNull
        public String getPackageName() {
            return mPackageName;
        }
    }

    private NativeAppLink() {
    }

    @Nullable
    public static NativeAppTarget resolveTarget(@Nullable String url) {
        if (url == null) return null;
        Uri uri = Uri.parse(url.trim());
        String scheme = uri.getScheme();
        if (scheme == null) return null;
        scheme = scheme.toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) return null;
        String host = uri.getHost();
        if (host == null) return null;
        host = host.toLowerCase(Locale.ROOT);
        String path = uri.getPath();
        if (path == null) path = "";
        path = path.toLowerCase(Locale.ROOT);

        if (host.equals("docs.google.com")) {
            if (path.startsWith("/spreadsheets")) {
                return new NativeAppTarget("Google Sheets", "com.google.android.apps.docs.editors.sheets");
            }
            if (path.startsWith("/document")) {
                return new NativeAppTarget("Google Docs", "com.google.android.apps.docs.editors.docs");
            }
            if (path.startsWith("/presentation")) {
                return new NativeAppTarget("Google Slides", "com.google.android.apps.docs.editors.slides");
            }
            return null;
        }
        if (host.equals("drive.google.com")) {
            return new NativeAppTarget("Google Drive", "com.google.android.apps.docs");
        }
        if (host.equals("meet.google.com")) {
            return new NativeAppTarget("Google Meet", "com.google.android.apps.tachyon");
        }
        if (host.equals("calendar.google.com")) {
            return new NativeAppTarget("Google Calendar", "com.google.android.calendar");
        }
        if (host.equals("mail.google.com")) {
            return new NativeAppTarget("Gmail", "com.google.android.gm");
        }
        if (host.equals("photos.google.com")) {
            return new NativeAppTarget("Google Photos", "com.google.android.apps.photos");
        }
        if (host.equals("keep.google.com")) {
            return new NativeAppTarget("Google Keep", "com.google.android.keep");
        }
        if (host.equals("maps.google.com")) {
            return new NativeAppTarget("Google Maps", "com.google.android.apps.maps");
        }
        if (host.equals("www.google.com") || host.equals("google.com")) {
            if (path.startsWith("/maps")) {
                return new NativeAppTarget("Google Maps", "com.google.android.apps.maps");
            }
            if (path.startsWith("/calendar")) {
                return new NativeAppTarget("Google Calendar", "com.google.android.calendar");
            }
            return null;
        }
        if (host.equals("app.slack.com")) {
            if (path.startsWith("/client/")) {
                return new NativeAppTarget("Slack", "com.Slack");
            }
            return null;
        }
        if (host.endsWith(".slack.com")) {
            if (path.startsWith("/archives/")) {
                return new NativeAppTarget("Slack", "com.Slack");
            }
            return null;
        }
        return null;
    }

    public static boolean openInNativeApp(@NonNull Context context, @NonNull String url,
                                          @NonNull NativeAppTarget target) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setPackage(target.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException activityNotFound) {
            return false;
        }
    }

    public static void openInNativeAppOrElse(@NonNull Context context, @NonNull String url,
                                             @NonNull Runnable browserFallback) {
        NativeAppTarget target = resolveTarget(url);
        if (target == null || !openInNativeApp(context, url, target)) {
            browserFallback.run();
        }
    }
}
