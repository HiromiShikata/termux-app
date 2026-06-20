package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserSessionSwitchIsolationScenarioTest {

    private static final String SESSION_A = "session-a";
    private static final String SESSION_B = "session-b";
    private static final String URL_A = "https://example.com/a";
    private static final String URL_B = "https://example.com/b";
    private static final String SHARED_URL = "https://example.com/shared";

    private static final class RenderedFrameModel {
        String ownerHandle;
        String loadedUrl;
        String displayedTabSessionHandle;

        void loadTabForSession(String sessionHandle, String url, boolean browserVisible) {
            boolean cover = BrowserRenderedFrameOwnership.requiresCoverForFrame(
                sessionHandle, ownerHandle, loadedUrl, url, browserVisible);
            lastLoadShowedCover = cover;
            ownerHandle = sessionHandle;
            displayedTabSessionHandle = sessionHandle;
            loadedUrl = url;
        }

        void blankForForeignFrame(String currentSessionHandle) {
            if (BrowserRenderedFrameOwnership.isRenderedFrameForeign(currentSessionHandle, ownerHandle)) {
                ownerHandle = null;
                loadedUrl = null;
                displayedTabSessionHandle = null;
            }
        }

        boolean lastLoadShowedCover;
    }

    private void switchTo(RenderedFrameModel frame, String sessionHandle,
                          String activeTabSessionHandle, String activeTabUrl, boolean browserVisible) {
        BrowserDisplayedTabResolution resolution = BrowserDisplayedTabResolution.resolve(
            sessionHandle, activeTabSessionHandle, frame.ownerHandle,
            frame.displayedTabSessionHandle, false);
        if (!resolution.shouldDisplay()) {
            frame.blankForForeignFrame(sessionHandle);
            return;
        }
        if (resolution.shouldLoadWebView()
                || !sameSession(frame.displayedTabSessionHandle, activeTabSessionHandle)) {
            frame.loadTabForSession(sessionHandle, activeTabUrl, browserVisible);
        }
    }

    private boolean sameSession(String a, String b) {
        return a != null && a.equals(b);
    }

    @Test
    public void switchingFromSessionWithPageToSessionWithItsOwnPageShowsTheNewSessionsContent() {
        RenderedFrameModel frame = new RenderedFrameModel();
        switchTo(frame, SESSION_A, SESSION_A, URL_A, true);
        Assert.assertEquals(URL_A, frame.loadedUrl);
        Assert.assertEquals(SESSION_A, frame.ownerHandle);

        switchTo(frame, SESSION_B, SESSION_B, URL_B, true);
        Assert.assertEquals(URL_B, frame.loadedUrl);
        Assert.assertEquals(SESSION_B, frame.ownerHandle);
        Assert.assertNotEquals(URL_A, frame.loadedUrl);
    }

    @Test
    public void switchingToASessionWithoutATabBlanksTheWebViewAndClearsOwnership() {
        RenderedFrameModel frame = new RenderedFrameModel();
        switchTo(frame, SESSION_A, SESSION_A, URL_A, true);

        switchTo(frame, SESSION_B, null, null, true);
        Assert.assertNull(frame.loadedUrl);
        Assert.assertNull(frame.ownerHandle);
        Assert.assertNull(frame.displayedTabSessionHandle);
    }

    @Test
    public void rapidBackAndForthSwitchesNeverLeaveTheWrongSessionsFrameRendered() {
        RenderedFrameModel frame = new RenderedFrameModel();
        switchTo(frame, SESSION_A, SESSION_A, URL_A, true);
        switchTo(frame, SESSION_B, SESSION_B, URL_B, true);
        switchTo(frame, SESSION_A, SESSION_A, URL_A, true);
        Assert.assertEquals(SESSION_A, frame.ownerHandle);
        Assert.assertEquals(URL_A, frame.loadedUrl);

        switchTo(frame, SESSION_B, SESSION_B, URL_B, true);
        Assert.assertEquals(SESSION_B, frame.ownerHandle);
        Assert.assertEquals(URL_B, frame.loadedUrl);
    }

    @Test
    public void switchingBetweenSessionsThatShareAUrlReloadsBehindTheCover() {
        RenderedFrameModel frame = new RenderedFrameModel();
        switchTo(frame, SESSION_A, SESSION_A, SHARED_URL, true);
        Assert.assertEquals(SESSION_A, frame.ownerHandle);

        switchTo(frame, SESSION_B, SESSION_B, SHARED_URL, true);
        Assert.assertEquals(SESSION_B, frame.ownerHandle);
        Assert.assertTrue(frame.lastLoadShowedCover);
    }
}
