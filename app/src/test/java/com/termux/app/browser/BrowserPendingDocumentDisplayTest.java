package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BrowserPendingDocumentDisplayTest {

    private static final long DOWNLOAD_ID = 41L;

    private final List<Long> displayedDownloadIds = new ArrayList<>();

    private final BrowserPendingDocumentDisplay pendingDisplay = new BrowserPendingDocumentDisplay();

    private void displayRemembered() {
        pendingDisplay.displayRememberedDocument(displayedDownloadIds::add);
    }

    @Test
    public void aDocumentThatFinishedWhileTheUserWasAwayIsDisplayedWhenTheActivityReturns() {
        pendingDisplay.rememberUntilTheActivityReturns(DOWNLOAD_ID);

        displayRemembered();

        Assert.assertEquals("a download that completes while the activity is away produces nothing"
                + " at all today, so the document must be kept and displayed when the user comes back",
            Collections.singletonList(DOWNLOAD_ID), displayedDownloadIds);
    }

    @Test
    public void aDocumentAlreadyDisplayedIsNotDisplayedAgainOnEveryReturn() {
        pendingDisplay.rememberUntilTheActivityReturns(DOWNLOAD_ID);

        displayRemembered();
        displayRemembered();

        Assert.assertEquals("reopening the same document on every return would take the user away"
                + " from whatever they came back to do",
            Collections.singletonList(DOWNLOAD_ID), displayedDownloadIds);
    }

    @Test
    public void nothingIsDisplayedWhenNoDocumentIsWaiting() {
        displayRemembered();

        Assert.assertTrue("a return with no document waiting must leave the user where they are",
            displayedDownloadIds.isEmpty());
    }
}
