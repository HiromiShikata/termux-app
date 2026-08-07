package com.termux.app;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxActivityOpenTagUrlOpensInMatchingAppTest {

    private static final String ACTIVITY_RELATIVE_PATH =
        "src/main/java/com/termux/app/TermuxActivity.java";

    private String readModuleFile(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String methodBody(String source, String signature) {
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue("method not found: " + signature, methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void openTagUrlOpenerResolvesTheMatchingNativeApplicationFirst() throws IOException {
        String methodBody = methodBody(
            readModuleFile(ACTIVITY_RELATIVE_PATH), "private void setBrowserView(");

        Assert.assertTrue("a URL opened by the open tag must resolve to the matching Google application before the in-app browser",
            methodBody.contains("new OpenTagUrlNativeAppOpener("));
    }

    @Test
    public void openTagUrlOpenerDoesNotGoStraightToTheInAppBrowser() throws IOException {
        String methodBody = methodBody(
            readModuleFile(ACTIVITY_RELATIVE_PATH), "private void setBrowserView(");

        Assert.assertFalse("a URL opened by the open tag must not bypass native-application resolution",
            methodBody.contains("mOpenTagUrlOpener = mTermuxBrowserController::openUrlInTabForSession"));
    }
}
