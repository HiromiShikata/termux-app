package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserDownloadRestoresVisiblePageTest {

    private static final String BROWSER_CONTROLLER_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private String readModuleSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String blockStartingAt(String source, String marker, String terminator) {
        int markerIndex = source.indexOf(marker);
        Assert.assertTrue("marker not found: " + marker, markerIndex >= 0);
        int blockEnd = source.indexOf(terminator, markerIndex);
        Assert.assertTrue("terminator not found after: " + marker, blockEnd > markerIndex);
        return source.substring(markerIndex, blockEnd);
    }

    @Test
    public void aNavigationThatBecomesADownloadRestoresTheVisiblePage() throws IOException {
        String downloadListener = blockStartingAt(
            readModuleSource(BROWSER_CONTROLLER_PATH), "webView.setDownloadListener(", "\n\n");

        Assert.assertTrue("a navigation that turns into a download must uncover the page it replaced",
            downloadListener.contains("restoreVisiblePageAfterTerminatedNavigation()"));
    }

    @Test
    public void restoringTheVisiblePageUncoversTheWebViewAndStopsTheLoadingIndicators() throws IOException {
        String restoreMethod = blockStartingAt(
            readModuleSource(BROWSER_CONTROLLER_PATH),
            "private void restoreVisiblePageAfterTerminatedNavigation()", "\n    }");

        Assert.assertTrue("restoring the visible page must remove the cover that hides the WebView",
            restoreMethod.contains("revealWebView()"));
        Assert.assertTrue("restoring the visible page must hide the page load progress",
            restoreMethod.contains("hidePageLoadProgress()"));
        Assert.assertTrue("restoring the visible page must stop the pull to refresh spinner",
            restoreMethod.contains("mSwipeRefreshLayout.setRefreshing(false)"));
    }

    @Test
    public void aMainFrameErrorKeepsRestoringTheVisiblePage() throws IOException {
        String errorMethod = blockStartingAt(
            readModuleSource(BROWSER_CONTROLLER_PATH),
            "private void handleMainFrameError()", "\n    }");

        Assert.assertTrue("a main frame error must keep restoring the visible page",
            errorMethod.contains("restoreVisiblePageAfterTerminatedNavigation()"));
    }
}
