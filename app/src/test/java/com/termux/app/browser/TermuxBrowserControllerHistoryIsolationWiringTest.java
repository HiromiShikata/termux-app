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
        int displayIndex = source.indexOf("private void displayTabInWebView(@NonNull BrowserTab tab)");
        Assert.assertTrue(displayIndex >= 0);
        int methodEnd = source.indexOf("\n    }", displayIndex);
        Assert.assertTrue(methodEnd > displayIndex);
        return source.substring(displayIndex, methodEnd);
    }

    @Test
    public void displayingATabLoadsTheTargetTabsOwnUrl() throws IOException {
        String methodBody = displayTabMethodBody(readControllerSource());

        Assert.assertTrue(methodBody.contains("mWebView.loadUrl(targetUrl)"));
        Assert.assertTrue(methodBody.contains("String targetUrl = tab.getUrl()"));
    }

    @Test
    public void crossTabSwitchClearsHistoryGuardedByTheSwitchDecision() throws IOException {
        String methodBody = displayTabMethodBody(readControllerSource());

        Assert.assertTrue(methodBody.contains(
            "BrowserHistoryIsolation.resolve(previouslyDisplayedTab != tab)"));
        Assert.assertTrue(methodBody.contains("mWebView.clearHistory()"));
    }

    @Test
    public void noWebViewStateSaveOrRestoreRemainsInTheSwitchPath() throws IOException {
        String source = readControllerSource();

        Assert.assertFalse(source.contains("mWebView.saveState"));
        Assert.assertFalse(source.contains("mWebView.restoreState"));
        Assert.assertFalse(source.contains("captureDisplayedTabStateBeforeSwitchingTo"));
    }
}
