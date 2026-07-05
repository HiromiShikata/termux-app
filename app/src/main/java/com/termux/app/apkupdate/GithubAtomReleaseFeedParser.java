package com.termux.app.apkupdate;

import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GithubAtomReleaseFeedParser {

    private static final Pattern ENTRY_PATTERN =
        Pattern.compile("<entry>([\\s\\S]*?)</entry>");

    private static final Pattern TAG_LINK_PATTERN =
        Pattern.compile("/releases/tag/([^\"/?#<>\\s]+)");

    @Nullable
    public String parseLatestTagName(@Nullable String atomFeedXml) {
        if (atomFeedXml == null) {
            return null;
        }
        Matcher entryMatcher = ENTRY_PATTERN.matcher(atomFeedXml);
        while (entryMatcher.find()) {
            String entry = entryMatcher.group(1);
            Matcher tagMatcher = TAG_LINK_PATTERN.matcher(entry);
            if (tagMatcher.find()) {
                String tagName = tagMatcher.group(1).trim();
                if (!tagName.isEmpty()) {
                    return tagName;
                }
            }
        }
        return null;
    }
}
