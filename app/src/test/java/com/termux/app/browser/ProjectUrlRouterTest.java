package com.termux.app.browser;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ProjectUrlRouterTest {

    private static final String SESSION = "session-handle";

    private static final class RecordingProjectUrlOpener implements ProjectUrlOpener {
        final List<String> openedUrls = new ArrayList<>();
        final List<BrowserViewMode> openedViewModes = new ArrayList<>();

        @Override
        public void openProjectUrl(@NonNull String url, @NonNull BrowserViewMode viewMode) {
            openedUrls.add(url);
            openedViewModes.add(viewMode);
        }
    }

    @Test
    public void routesNormalizedAbsoluteUrlToProjectBrowser() {
        RecordingProjectUrlOpener opener = new RecordingProjectUrlOpener();
        ProjectUrlRouter router = new ProjectUrlRouter(opener);

        router.route("https://github.com/org/repo/issues/1", BrowserViewMode.DESKTOP);

        Assert.assertEquals(1, opener.openedUrls.size());
        Assert.assertEquals("https://github.com/org/repo/issues/1", opener.openedUrls.get(0));
    }

    @Test
    public void prependsHttpsForBareProjectDomain() {
        RecordingProjectUrlOpener opener = new RecordingProjectUrlOpener();
        ProjectUrlRouter router = new ProjectUrlRouter(opener);

        router.route("example.com/overview", BrowserViewMode.DESKTOP);

        Assert.assertEquals("https://example.com/overview", opener.openedUrls.get(0));
    }

    @Test
    public void forwardsMobileViewModeToOpener() {
        RecordingProjectUrlOpener opener = new RecordingProjectUrlOpener();
        ProjectUrlRouter router = new ProjectUrlRouter(opener);

        router.route("https://tdpm.example/console", BrowserViewMode.MOBILE);

        Assert.assertEquals(BrowserViewMode.MOBILE, opener.openedViewModes.get(0));
    }

    @Test
    public void forwardsDesktopViewModeToOpener() {
        RecordingProjectUrlOpener opener = new RecordingProjectUrlOpener();
        ProjectUrlRouter router = new ProjectUrlRouter(opener);

        router.route("https://github.com/org/repo", BrowserViewMode.DESKTOP);

        Assert.assertEquals(BrowserViewMode.DESKTOP, opener.openedViewModes.get(0));
    }

    @Test
    public void ignoresNullUrl() {
        RecordingProjectUrlOpener opener = new RecordingProjectUrlOpener();
        ProjectUrlRouter router = new ProjectUrlRouter(opener);

        router.route(null, BrowserViewMode.DESKTOP);

        Assert.assertTrue(opener.openedUrls.isEmpty());
    }

    @Test
    public void ignoresBlankUrl() {
        RecordingProjectUrlOpener opener = new RecordingProjectUrlOpener();
        ProjectUrlRouter router = new ProjectUrlRouter(opener);

        router.route("   ", BrowserViewMode.DESKTOP);

        Assert.assertTrue(opener.openedUrls.isEmpty());
    }

    @Test
    public void projectUrlIsNotAddedToPerSessionTabManager() {
        BrowserTabManager perSessionTabManager = new BrowserTabManager();
        RecordingProjectUrlOpener opener = new RecordingProjectUrlOpener();
        ProjectUrlRouter router = new ProjectUrlRouter(opener);

        router.route("https://github.com/org/repo", BrowserViewMode.DESKTOP);

        Assert.assertEquals(1, opener.openedUrls.size());
        Assert.assertTrue(perSessionTabManager.getTabs(SESSION).isEmpty());
        Assert.assertNull(perSessionTabManager.getActiveTab(SESSION));
    }

    @Test
    public void openingMultipleProjectUrlsKeepsPerSessionTabCountAtZero() {
        BrowserTabManager perSessionTabManager = new BrowserTabManager();
        RecordingProjectUrlOpener opener = new RecordingProjectUrlOpener();
        ProjectUrlRouter router = new ProjectUrlRouter(opener);

        router.route("https://github.com/org/repo", BrowserViewMode.DESKTOP);
        router.route("https://tdpm.example/console", BrowserViewMode.MOBILE);
        router.route("https://github.com/org/repo/issues/new", BrowserViewMode.DESKTOP);

        Assert.assertEquals(3, opener.openedUrls.size());
        Assert.assertEquals(0, perSessionTabManager.getTabs(SESSION).size());
    }
}
