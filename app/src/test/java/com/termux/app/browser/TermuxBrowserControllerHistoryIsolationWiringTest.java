package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxBrowserControllerHistoryIsolationWiringTest {

    private static final String CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private String readControllerSource() throws IOException {
        Path moduleRelative = Paths.get(CONTROLLER_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CONTROLLER_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String displayTabMethodBody(String source) {
        int displayIndex = source.indexOf("private void displayTab(@NonNull BrowserTab tab, boolean forceReload)");
        Assert.assertTrue(displayIndex >= 0);
        int methodEnd = source.indexOf("\n    }", displayIndex);
        Assert.assertTrue(methodEnd > displayIndex);
        return source.substring(displayIndex, methodEnd);
    }

    @Test
    public void switchingToATabRoutesThroughThePerTabWebViewHostInsteadOfReloadingASharedWebView()
            throws IOException {
        String methodBody = displayTabMethodBody(readControllerSource());

        Assert.assertTrue(methodBody.contains("mWebViewHost.showTab(tab)"));
    }

    @Test
    public void switchingToAnAlreadyLoadedTabDoesNotReloadIt() throws IOException {
        String methodBody = displayTabMethodBody(readControllerSource());

        Assert.assertTrue(methodBody.contains("boolean firstDisplay = !mWebViewHost.hasWebViewForTab(tab)"));
        Assert.assertTrue(methodBody.contains("if (forceReload && !firstDisplay) webView.reload()"));
    }

    @Test
    public void noWebViewStateSaveOrRestoreRemainsInTheSwitchPath() throws IOException {
        String source = readControllerSource();

        Assert.assertFalse(source.contains(".saveState"));
        Assert.assertFalse(source.contains(".restoreState"));
        Assert.assertFalse(source.contains("captureDisplayedTabStateBeforeSwitchingTo"));
    }
}
