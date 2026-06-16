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

        @Override
        public void openProjectUrl(@NonNull String url) {
            openedUrls.add(url);
        }
    }

    @Test
    public void routesNormalizedAbsoluteUrlToProjectBrowser() {
        RecordingProjectUrlOpener opener = new RecordingProjectUrlOpener();
        ProjectUrlRouter router = new ProjectUrlRouter(opener);

        router.route("https://github.com/org/repo/issues/1");

        Assert.assertEquals(1, opener.openedUrls.size());
        Assert.assertEquals("https://github.com/org/repo/issues/1", opener.openedUrls.get(0));
    }

    @Test
    public void prependsHttpsForBareProjectDomain() {
        RecordingProjectUrlOpener opener = new RecordingProjectUrlOpener();
        ProjectUrlRouter router = new ProjectUrlRouter(opener);

        router.route("example.com/overview");

        Assert.assertEquals("https://example.com/overview", opener.openedUrls.get(0));
    }

    @Test
    public void ignoresNullUrl() {
        RecordingProjectUrlOpener opener = new RecordingProjectUrlOpener();
        ProjectUrlRouter router = new ProjectUrlRouter(opener);

        router.route(null);

        Assert.assertTrue(opener.openedUrls.isEmpty());
    }

    @Test
    public void ignoresBlankUrl() {
        RecordingProjectUrlOpener opener = new RecordingProjectUrlOpener();
        ProjectUrlRouter router = new ProjectUrlRouter(opener);

        router.route("   ");

        Assert.assertTrue(opener.openedUrls.isEmpty());
    }

    @Test
    public void projectUrlIsNotAddedToPerSessionTabManager() {
        BrowserTabManager perSessionTabManager = new BrowserTabManager();
        RecordingProjectUrlOpener opener = new RecordingProjectUrlOpener();
        ProjectUrlRouter router = new ProjectUrlRouter(opener);

        router.route("https://github.com/org/repo");

        Assert.assertEquals(1, opener.openedUrls.size());
        Assert.assertTrue(perSessionTabManager.getTabs(SESSION).isEmpty());
        Assert.assertNull(perSessionTabManager.getActiveTab(SESSION));
    }

    @Test
    public void openingMultipleProjectUrlsKeepsPerSessionTabCountAtZero() {
        BrowserTabManager perSessionTabManager = new BrowserTabManager();
        RecordingProjectUrlOpener opener = new RecordingProjectUrlOpener();
        ProjectUrlRouter router = new ProjectUrlRouter(opener);

        router.route("https://github.com/org/repo");
        router.route("https://tdpm.example/console");
        router.route("https://github.com/org/repo/issues/new");

        Assert.assertEquals(3, opener.openedUrls.size());
        Assert.assertEquals(0, perSessionTabManager.getTabs(SESSION).size());
    }
}
