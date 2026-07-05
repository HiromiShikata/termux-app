package com.termux.app.apkupdate;

import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the newest release tag from a GitHub Releases Atom feed
 * ({@code https://github.com/{owner}/{repo}/releases.atom}).
 *
 * <p>The Atom feed is served from the github.com web host, not {@code api.github.com}, so it is not
 * subject to the 60-requests-per-hour unauthenticated REST rate limit that the
 * {@code releases/latest} endpoint enforces. It is therefore used as a fallback source for the
 * latest release tag when the REST API answers with a rate-limit response.
 *
 * <p>Each release is an {@code <entry>} whose {@code <link rel="alternate" .../releases/tag/{tag}>}
 * carries the git tag. The feed lists entries newest first, so the first entry with a tag link is
 * the latest release. Parsing is done with anchored regular expressions rather than a full XML
 * parser to keep the dependency surface minimal and to tolerate the feed's fixed, well-formed shape.
 */
public final class GithubAtomReleaseFeedParser {

    private static final Pattern ENTRY_PATTERN =
        Pattern.compile("<entry>([\\s\\S]*?)</entry>");

    private static final Pattern TAG_LINK_PATTERN =
        Pattern.compile("/releases/tag/([^\"/?#<>\\s]+)");

    /**
     * Returns the tag name of the newest release in the feed, or {@code null} when the feed has no
     * release entry with a tag link.
     */
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
