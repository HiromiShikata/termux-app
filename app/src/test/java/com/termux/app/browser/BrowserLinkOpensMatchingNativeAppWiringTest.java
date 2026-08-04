package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BrowserLinkOpensMatchingNativeAppWiringTest {

    private static final String CORE_WEB_VIEW_CLIENT_PATH =
        "src/main/java/com/termux/app/browser/BrowserCoreWebViewClient.java";

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
    public void aLinkTappedInThePageIsOfferedToTheMatchingNativeApplication() throws IOException {
        String requestOverload = blockStartingAt(
            readModuleSource(CORE_WEB_VIEW_CLIENT_PATH),
            "public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request)",
            "\n    }");

        Assert.assertTrue("a link tapped inside the in-app browser must be offered to the matching native application",
            requestOverload.contains("routeToMatchingNativeAppIfRequired("));
    }

    @Test
    public void onlyAUserInitiatedMainFrameNavigationLeavesTheInAppBrowser() throws IOException {
        String routingMethod = blockStartingAt(
            readModuleSource(CORE_WEB_VIEW_CLIENT_PATH),
            "private boolean routeToMatchingNativeAppIfRequired(", "\n    }");

        Assert.assertTrue("a navigation the user did not start must stay in the in-app browser",
            routingMethod.contains("hasGesture()"));
        Assert.assertTrue("a subframe navigation must stay in the in-app browser",
            routingMethod.contains("isForMainFrame()"));
        Assert.assertTrue("only a URL with a matching native application may leave the in-app browser",
            routingMethod.contains("openInMatchingNativeApp("));
    }

    @Test
    public void theBrowserResolvesTheMatchingNativeApplicationThroughTheSharedLinkResolver() throws IOException {
        String hostMethod = blockStartingAt(
            readModuleSource(BROWSER_CONTROLLER_PATH),
            "public boolean openInMatchingNativeApp(", "\n            }");

        Assert.assertTrue("the browser must reuse the shared Google application resolver",
            hostMethod.contains("GoogleAppLink.resolveTarget("));
        Assert.assertTrue("the browser must launch the resolved application",
            hostMethod.contains("GoogleAppLink.openInGoogleApp("));
    }
}
