package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

public class GithubAtomReleaseFeedParserTest {

    private final GithubAtomReleaseFeedParser parser = new GithubAtomReleaseFeedParser();

    private static final String FEED_WITH_TWO_ENTRIES =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<feed xmlns=\"http://www.w3.org/2005/Atom\">\n"
            + "  <entry>\n"
            + "    <id>tag:github.com,2008:Repository/1/v0.119.2744</id>\n"
            + "    <link rel=\"alternate\" type=\"text/html\""
            + " href=\"https://github.com/HiromiShikata/termux-app/releases/tag/v0.119.2744\"/>\n"
            + "    <title>0.119.2744</title>\n"
            + "  </entry>\n"
            + "  <entry>\n"
            + "    <link rel=\"alternate\" type=\"text/html\""
            + " href=\"https://github.com/HiromiShikata/termux-app/releases/tag/v0.119.2743\"/>\n"
            + "    <title>0.119.2743</title>\n"
            + "  </entry>\n"
            + "</feed>\n";

    @Test
    public void returnsTagOfFirstEntryAsLatest() {
        Assert.assertEquals("v0.119.2744", parser.parseLatestTagName(FEED_WITH_TWO_ENTRIES));
    }

    @Test
    public void returnsNullWhenFeedIsNull() {
        Assert.assertNull(parser.parseLatestTagName(null));
    }

    @Test
    public void returnsNullWhenFeedHasNoReleaseEntry() {
        String feedWithoutEntries =
            "<feed xmlns=\"http://www.w3.org/2005/Atom\">\n"
                + "  <link rel=\"self\" href=\"https://github.com/HiromiShikata/termux-app/releases.atom\"/>\n"
                + "</feed>\n";
        Assert.assertNull(parser.parseLatestTagName(feedWithoutEntries));
    }

    @Test
    public void skipsEntryWithoutTagLinkAndFindsNextTag() {
        String feed =
            "<feed>\n"
                + "  <entry>\n"
                + "    <link rel=\"alternate\" href=\"https://github.com/HiromiShikata/termux-app/releases\"/>\n"
                + "    <title>notes</title>\n"
                + "  </entry>\n"
                + "  <entry>\n"
                + "    <link rel=\"alternate\""
                + " href=\"https://github.com/HiromiShikata/termux-app/releases/tag/v0.119.2700\"/>\n"
                + "  </entry>\n"
                + "</feed>\n";
        Assert.assertEquals("v0.119.2700", parser.parseLatestTagName(feed));
    }
}
