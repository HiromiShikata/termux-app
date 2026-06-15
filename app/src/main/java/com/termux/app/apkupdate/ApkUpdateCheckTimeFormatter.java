package com.termux.app.apkupdate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ApkUpdateCheckTimeFormatter {

    private static final String PATTERN = "yyyy-MM-dd HH:mm";

    public boolean hasCheckTime(long epochMillis) {
        return epochMillis > ApkUpdateManager.NO_CHECK_TIME;
    }

    public String format(long epochMillis, ZoneId zone, Locale locale) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN, locale).withZone(zone);
        return formatter.format(Instant.ofEpochMilli(epochMillis));
    }

    public String format(long epochMillis) {
        return format(epochMillis, ZoneId.systemDefault(), Locale.getDefault());
    }
}
