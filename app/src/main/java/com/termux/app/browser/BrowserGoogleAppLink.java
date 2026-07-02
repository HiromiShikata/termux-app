package com.termux.app.browser;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public final class BrowserGoogleAppLink {

    public static final class GoogleAppTarget {

        private final String mAppDisplayName;

        private final String mPackageName;

        GoogleAppTarget(@NonNull String appDisplayName, @NonNull String packageName) {
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

    private BrowserGoogleAppLink() {
    }

    @Nullable
    public static GoogleAppTarget resolveTarget(@Nullable String url) {
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
                return new GoogleAppTarget("Google Sheets", "com.google.android.apps.docs.editors.sheets");
            }
            if (path.startsWith("/document")) {
                return new GoogleAppTarget("Google Docs", "com.google.android.apps.docs.editors.docs");
            }
            if (path.startsWith("/presentation")) {
                return new GoogleAppTarget("Google Slides", "com.google.android.apps.docs.editors.slides");
            }
            return null;
        }
        if (host.equals("drive.google.com")) {
            return new GoogleAppTarget("Google Drive", "com.google.android.apps.docs");
        }
        if (host.equals("meet.google.com")) {
            return new GoogleAppTarget("Google Meet", "com.google.android.apps.tachyon");
        }
        if (host.equals("calendar.google.com")) {
            return new GoogleAppTarget("Google Calendar", "com.google.android.calendar");
        }
        if (host.equals("mail.google.com")) {
            return new GoogleAppTarget("Gmail", "com.google.android.gm");
        }
        if (host.equals("photos.google.com")) {
            return new GoogleAppTarget("Google Photos", "com.google.android.apps.photos");
        }
        if (host.equals("keep.google.com")) {
            return new GoogleAppTarget("Google Keep", "com.google.android.keep");
        }
        if (host.equals("maps.google.com")) {
            return new GoogleAppTarget("Google Maps", "com.google.android.apps.maps");
        }
        if (host.equals("www.google.com") || host.equals("google.com")) {
            if (path.startsWith("/maps")) {
                return new GoogleAppTarget("Google Maps", "com.google.android.apps.maps");
            }
            return null;
        }
        return null;
    }
}
