package com.termux.view.url;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TerminalUrlExtractorTest {

    @Test
    public void columnInsideUrlTokenReturnsWholeUrl() {
        String row = "see https://example.com/path now";
        int column = row.indexOf("example");
        Assert.assertEquals("https://example.com/path",
            TerminalUrlExtractor.extractBrowsableUrlAt(row, column));
    }

    @Test
    public void columnAtUrlStartReturnsUrl() {
        String row = "https://github.com/HiromiShikata/termux-app rest";
        Assert.assertEquals("https://github.com/HiromiShikata/termux-app",
            TerminalUrlExtractor.extractBrowsableUrlAt(row, 0));
    }

    @Test
    public void columnAtUrlEndReturnsUrl() {
        String row = "prefix https://example.com";
        int lastColumn = row.length() - 1;
        Assert.assertEquals("https://example.com",
            TerminalUrlExtractor.extractBrowsableUrlAt(row, lastColumn));
    }

    @Test
    public void schemelessHostTokenIsExtracted() {
        String row = "visit github.com today";
        int column = row.indexOf("github");
        Assert.assertEquals("github.com",
            TerminalUrlExtractor.extractBrowsableUrlAt(row, column));
    }

    @Test
    public void columnOnWhitespaceReturnsNull() {
        String row = "see https://example.com now";
        int spaceColumn = row.indexOf("see") + 3;
        Assert.assertNull(TerminalUrlExtractor.extractBrowsableUrlAt(row, spaceColumn));
    }

    @Test
    public void columnOnNonUrlWordReturnsNull() {
        String row = "this is plain text";
        int column = row.indexOf("plain");
        Assert.assertNull(TerminalUrlExtractor.extractBrowsableUrlAt(row, column));
    }

    @Test
    public void columnOutOfRangeReturnsNull() {
        String row = "https://example.com";
        Assert.assertNull(TerminalUrlExtractor.extractBrowsableUrlAt(row, row.length()));
        Assert.assertNull(TerminalUrlExtractor.extractBrowsableUrlAt(row, -1));
    }

    @Test
    public void nullRowReturnsNull() {
        Assert.assertNull(TerminalUrlExtractor.extractBrowsableUrlAt(null, 0));
    }

    @Test
    public void ipHostUrlWithPortIsExtracted() {
        String row = "open http://50.30.32.53:9980/in-tmux/index.json please";
        int column = row.indexOf("50.30");
        Assert.assertEquals("http://50.30.32.53:9980/in-tmux/index.json",
            TerminalUrlExtractor.extractBrowsableUrlAt(row, column));
    }
}
