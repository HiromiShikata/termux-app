package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxTerminalViewClientSingleTapUrlOpensInAppTest {

    private static final String VIEW_CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalViewClient.java";

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
    public void singleTapOnPlainTextUrlRoutesThroughMatchingAppOrBrowserHelper() throws IOException {
        String methodBody = methodBody(
            readModuleFile(VIEW_CLIENT_RELATIVE_PATH), "public void onSingleTapUp(");

        Assert.assertTrue("single tap on a plain-text URL must resolve to the matching native app or fall back to the browser",
            methodBody.contains("openUrlInMatchingAppOrBrowser(url)"));
    }

    @Test
    public void singleTapOnHyperlinkRoutesThroughMatchingAppOrBrowserHelper() throws IOException {
        String methodBody = methodBody(
            readModuleFile(VIEW_CLIENT_RELATIVE_PATH), "public void onSingleTapUp(");

        Assert.assertTrue("single tap on an OSC 8 hyperlink must resolve to the matching native app or fall back to the browser",
            methodBody.contains("openUrlInMatchingAppOrBrowser(hyperlinkUri)"));
    }

    @Test
    public void singleTapNoLongerOpensTheInAppBrowserUnconditionally() throws IOException {
        String methodBody = methodBody(
            readModuleFile(VIEW_CLIENT_RELATIVE_PATH), "public void onSingleTapUp(");

        Assert.assertFalse("single tap must no longer bypass native-app resolution by calling openUrlInApp directly",
            methodBody.contains("openUrlInApp("));
    }

    @Test
    public void singleTapOnPlainTextUrlDoesNotLaunchExternalBrowser() throws IOException {
        String methodBody = methodBody(
            readModuleFile(VIEW_CLIENT_RELATIVE_PATH), "public void onSingleTapUp(");

        Assert.assertFalse("single tap on a plain-text URL must not launch the external browser",
            methodBody.contains("ShareUtils.openUrl"));
    }

    @Test
    public void matchingAppOrBrowserHelperResolvesThenFallsBackToTheInAppBrowser() throws IOException {
        String methodBody = methodBody(
            readModuleFile(VIEW_CLIENT_RELATIVE_PATH), "private void openUrlInMatchingAppOrBrowser(");

        Assert.assertTrue("the shared helper must delegate the resolve-then-fallback decision to GoogleAppLink",
            methodBody.contains("GoogleAppLink.openInGoogleAppOrElse(mActivity, url"));
        Assert.assertTrue("the shared helper must fall back to the in-app browser",
            methodBody.contains("openUrlInApp(url)"));
    }

    @Test
    public void longPressOpenInGoogleAppReusesTheSharedHelper() throws IOException {
        String methodBody = methodBody(
            readModuleFile(VIEW_CLIENT_RELATIVE_PATH), "public void openLongPressedUrlInGoogleApp(");

        Assert.assertTrue("long-press Open in Google app must reuse the shared resolve-then-fallback helper",
            methodBody.contains("openUrlInMatchingAppOrBrowser(mLongPressedUrl)"));
    }

    @Test
    public void openUrlInAppRoutesThroughTheInAppBrowserController() throws IOException {
        String methodBody = methodBody(
            readModuleFile(VIEW_CLIENT_RELATIVE_PATH), "private void openUrlInApp(");

        Assert.assertTrue("openUrlInApp must delegate to the in-app browser controller",
            methodBody.contains("browserController.openUrlInNewTab(url)"));
    }

    @Test
    public void longPressOpenInChromeStillLaunchesTheExternalBrowser() throws IOException {
        String methodBody = methodBody(
            readModuleFile(VIEW_CLIENT_RELATIVE_PATH), "public void openLongPressedUrlInChrome(");

        Assert.assertTrue("long-press Open in Chrome must still launch the external browser",
            methodBody.contains("ShareUtils.openUrlInChrome(mActivity, mLongPressedUrl)"));
    }

    @Test
    public void selectUrlDialogOpenInChromeStillLaunchesTheExternalBrowser() throws IOException {
        String methodBody = methodBody(
            readModuleFile(VIEW_CLIENT_RELATIVE_PATH), "private void showUrlOpenChoice(");

        Assert.assertTrue("Select-URL dialog Open in Chrome must still launch the external browser",
            methodBody.contains("ShareUtils.openUrlInChrome(mActivity, url)"));
    }
}
