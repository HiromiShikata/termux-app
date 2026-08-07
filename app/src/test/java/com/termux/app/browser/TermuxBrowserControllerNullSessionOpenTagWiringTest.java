package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxBrowserControllerNullSessionOpenTagWiringTest {

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

    @Test
    public void openUrlInNewTabResolvesTheSessionHandleThroughTheSharedDecision() throws IOException {
        String source = readControllerSource();
        int methodIndex = source.indexOf("public boolean openUrlInNewTab(@NonNull String url)");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("resolveSessionHandleForNewTab()"));
    }

    @Test
    public void resolvedSessionHandleFallsBackToTheSessionDisplayedByTheActivity() throws IOException {
        String source = readControllerSource();
        int methodIndex = source.indexOf("private String resolveSessionHandleForNewTab()");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("mActivity.getCurrentSession()"));
        Assert.assertTrue(methodBody.contains("BrowserNewTabSessionHandle.resolve(mCurrentSessionHandle,"));
        Assert.assertTrue(methodBody.contains("displayedSession.mHandle"));
    }

    @Test
    public void openUrlInNewTabDoesNotEarlyReturnOnNullSessionHandleWithoutCheckingActivity() throws IOException {
        String source = readControllerSource();
        int methodIndex = source.indexOf("public boolean openUrlInNewTab(@NonNull String url)");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertFalse(methodBody.contains("if (mCurrentSessionHandle == null) return;"));
    }

    @Test
    public void openUrlInNewTabUsesResolvedSessionHandleForTabCreation() throws IOException {
        String source = readControllerSource();
        int methodIndex = source.indexOf("public boolean openUrlInNewTab(@NonNull String url)");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("mTabManager.addTab(sessionHandle,"));
    }
}
