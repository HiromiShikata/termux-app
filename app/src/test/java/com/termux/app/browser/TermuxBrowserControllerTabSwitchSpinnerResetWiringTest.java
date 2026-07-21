package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxBrowserControllerTabSwitchSpinnerResetWiringTest {

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

    private String methodBody(String source, String signature) {
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue("Method not found: " + signature, methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue("Method end not found for: " + signature, methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void displayTabResetsTheSharedSwipeRefreshSpinnerOnTabSwitch() throws IOException {
        String displayTabBody = methodBody(
            readControllerSource(),
            "private void displayTab(@NonNull BrowserTab tab, boolean forceReload)");

        Assert.assertTrue(
            "displayTab must call mSwipeRefreshLayout.setRefreshing(false) so switching tabs "
                + "never leaves the circular spinner visible on a non-loading tab",
            displayTabBody.contains("mSwipeRefreshLayout.setRefreshing(false)"));
    }

    @Test
    public void displayTabResetsThePageLoadProgressBarOnTabSwitch() throws IOException {
        String displayTabBody = methodBody(
            readControllerSource(),
            "private void displayTab(@NonNull BrowserTab tab, boolean forceReload)");

        Assert.assertTrue(
            "displayTab must call hidePageLoadProgress() so the horizontal progress bar is reset "
                + "when switching tabs",
            displayTabBody.contains("hidePageLoadProgress()"));
    }
}
