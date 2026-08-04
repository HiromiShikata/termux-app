package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OpenTagPerSessionRoutingWiringTest {

    private String readModuleSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void openTagControllerRoutesToTheProducingSessionHandle() throws IOException {
        String source = readModuleSource(
            "src/main/java/com/termux/app/browser/OpenTagBrowserController.java");
        Assert.assertTrue(source.contains("void openUrlInTabForSession(@NonNull String sessionHandle, @NonNull String url)"));
        Assert.assertTrue(source.contains("urlOpener.openUrlInTabForSession(sessionKey, openUrl)"));
        Assert.assertFalse(source.contains("openUrlInNewTab(openUrl)"));
    }

    @Test
    public void termuxActivityWiresOpenTagOpenerToSessionTargetedMethod() throws IOException {
        String source = readModuleSource("src/main/java/com/termux/app/TermuxActivity.java");
        int assignmentIndex = source.indexOf("mOpenTagUrlOpener = ");
        Assert.assertTrue("the open tag opener must be assigned", assignmentIndex >= 0);
        String assignment = source.substring(assignmentIndex, source.indexOf(";", assignmentIndex));
        Assert.assertTrue("the open tag opener must reach the browser through the session targeted method",
            assignment.contains("mTermuxBrowserController::openUrlInTabForSession"));
        Assert.assertFalse("the open tag opener must not open a tab on the currently shown session",
            assignment.contains("openUrlInNewTab"));
    }

    @Test
    public void browserControllerSessionTargetedOpenAddsTabToGivenSessionNotCurrentSession() throws IOException {
        String source = readModuleSource(
            "src/main/java/com/termux/app/browser/TermuxBrowserController.java");
        int methodIndex = source.indexOf(
            "public void openUrlInTabForSession(@NonNull String sessionHandle, @NonNull String url)");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("mTabManager.addTab(sessionHandle, normalizeUrl(url))"));
        Assert.assertFalse(methodBody.contains("mTabManager.addTab(mCurrentSessionHandle"));
    }
}
