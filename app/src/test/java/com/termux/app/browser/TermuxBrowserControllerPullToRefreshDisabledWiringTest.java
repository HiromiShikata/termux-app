package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxBrowserControllerPullToRefreshDisabledWiringTest {

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
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void configureWebViewDisablesTheSwipeRefreshLayoutSoUpwardScrollIsNeverIntercepted()
            throws IOException {
        String configureBody = methodBody(readControllerSource(), "private void configureWebView()");

        Assert.assertTrue(configureBody.contains("mSwipeRefreshLayout.setEnabled(false)"));
    }

    @Test
    public void configureWebViewDoesNotArmPullToRefreshGestureHandling() throws IOException {
        String configureBody = methodBody(readControllerSource(), "private void configureWebView()");

        Assert.assertFalse(configureBody.contains("setOnRefreshListener"));
        Assert.assertFalse(configureBody.contains("setOnChildScrollUpCallback"));
        Assert.assertFalse(configureBody.contains("setDistanceToTriggerSync"));
    }
}
