package com.termux.app.apkupdate;

import java.util.ArrayList;
import java.util.List;

public final class UpdateNotificationTextComposer {

    private static final String CHANGE_LINE_PREFIX = "- ";
    private static final String LINE_SEPARATOR = "\n";
    private static final String RELEASE_SEPARATOR = "\n\n";
    private static final String LINE_BREAK_PATTERN = "\\r?\\n";

    private UpdateNotificationTextComposer() {
    }

    public static String composeBigText(List<ApkRelease> releasesNewestFirst) {
        List<String> releaseBlocks = new ArrayList<>();
        for (ApkRelease release : releasesNewestFirst) {
            releaseBlocks.add(composeReleaseBlock(release));
        }
        return String.join(RELEASE_SEPARATOR, releaseBlocks);
    }

    private static String composeReleaseBlock(ApkRelease release) {
        List<String> changeLines = new ArrayList<>();
        for (String line : release.getBody().split(LINE_BREAK_PATTERN)) {
            if (line.startsWith(CHANGE_LINE_PREFIX)) {
                changeLines.add(line);
            }
        }
        if (changeLines.isEmpty()) {
            return release.getVersionName();
        }
        return release.getVersionName() + LINE_SEPARATOR + String.join(LINE_SEPARATOR, changeLines);
    }
}
