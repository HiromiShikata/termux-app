package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TermuxBrowserControllerForeignFrameResetWiringTest {

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
    public void resetWebViewToBlankClearsRenderedFrameOwnershipBookkeeping() throws IOException {
        String source = readControllerSource();
        int methodIndex = source.indexOf("private void resetWebViewToBlankForForeignFrame()");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains(
            "BrowserRenderedFrameOwnership.isRenderedFrameForeign"));
        Assert.assertTrue(methodBody.contains("blankFrame()"));
        Assert.assertTrue(methodBody.contains("mWebView.loadUrl(\"about:blank\")"));
    }

    @Test
    public void blankFrameIsTheSingleStateClearingWriteForTheRenderedFrame() throws IOException {
        String source = readControllerSource();
        int methodIndex = source.indexOf("private void blankFrame()");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("mRenderedFrame = BrowserRenderedFrame.BLANK"));
    }

    @Test
    public void clearActionResetsTheWebViewInsteadOfOnlyNullingTheModelPointer() throws IOException {
        String source = readControllerSource();
        int loadActiveTabIndex = source.indexOf("private void loadActiveTab(boolean forceReload)");
        Assert.assertTrue(loadActiveTabIndex >= 0);
        int shouldDisplayIndex = source.indexOf("if (!resolution.shouldDisplay())", loadActiveTabIndex);
        Assert.assertTrue(shouldDisplayIndex > loadActiveTabIndex);
        int resetCallIndex =
            source.indexOf("resetWebViewToBlankForForeignFrame()", shouldDisplayIndex);
        int returnIndex = source.indexOf("return;", shouldDisplayIndex);
        Assert.assertTrue(resetCallIndex > shouldDisplayIndex);
        Assert.assertTrue(resetCallIndex < returnIndex);
    }

    @Test
    public void showTerminalResetsAForeignRenderedFrame() throws IOException {
        String source = readControllerSource();
        int showTerminalIndex = source.indexOf("public void showTerminal()");
        Assert.assertTrue(showTerminalIndex >= 0);
        int nextMethodIndex = source.indexOf("public boolean isBrowserVisible()", showTerminalIndex);
        int resetCallIndex =
            source.indexOf("resetWebViewToBlankForForeignFrame()", showTerminalIndex);
        Assert.assertTrue(resetCallIndex > showTerminalIndex);
        Assert.assertTrue(resetCallIndex < nextMethodIndex);
    }

    @Test
    public void coverDecisionForDisplayingATabKeysOnRenderedFrameOwnership() throws IOException {
        String source = readControllerSource();
        int displayIndex = source.indexOf("private void displayTabInWebView(@NonNull BrowserTab tab)");
        Assert.assertTrue(displayIndex >= 0);
        int methodEnd = source.indexOf("\n    }", displayIndex);
        Assert.assertTrue(methodEnd > displayIndex);
        String methodBody = source.substring(displayIndex, methodEnd);
        Assert.assertTrue(methodBody.contains(
            "BrowserRenderedFrameOwnership.requiresCoverForFrame"));
        Assert.assertTrue(methodBody.contains("mRenderedFrame.getOwnerSessionHandle()"));
    }

    @Test
    public void displayingATabRoutesThroughTheSingleRenderFrameWrite() throws IOException {
        String source = readControllerSource();
        int displayIndex = source.indexOf("private void displayTabInWebView(@NonNull BrowserTab tab)");
        Assert.assertTrue(displayIndex >= 0);
        int methodEnd = source.indexOf("\n    }", displayIndex);
        Assert.assertTrue(methodEnd > displayIndex);
        String methodBody = source.substring(displayIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("renderFrame(tab)"));
    }

    @Test
    public void renderFrameIsTheSingleStateSettingWriteForTheRenderedFrame() throws IOException {
        String source = readControllerSource();
        int methodIndex = source.indexOf("private void renderFrame(@NonNull BrowserTab tab)");
        Assert.assertTrue(methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        String methodBody = source.substring(methodIndex, methodEnd);
        Assert.assertTrue(methodBody.contains(
            "mRenderedFrame = BrowserRenderedFrame.renderingTab(tab)"));
    }

    @Test
    public void callbackAttributionThroughInFlightNavigationIsPreserved() throws IOException {
        String source = readControllerSource();
        Assert.assertTrue(source.contains("inFlightNavigation.tabForUrlCallback"));
        Assert.assertTrue(source.contains("inFlightNavigation.tabForTitleCallback"));
        Assert.assertTrue(source.contains("mRenderedFrame.getInFlightNavigation()"));
    }

    @Test
    public void sessionRemovalStillClearsRenderedFrameOwnership() throws IOException {
        String source = readControllerSource();
        int onRemovedIndex = source.indexOf("public void onSessionRemoved(@NonNull TerminalSession session)");
        Assert.assertTrue(onRemovedIndex >= 0);
        int methodEnd = source.indexOf("\n    }", onRemovedIndex);
        String methodBody = source.substring(onRemovedIndex, methodEnd);
        Assert.assertTrue(methodBody.contains("mRenderedFrame.getOwnerSessionHandle()"));
        Assert.assertTrue(methodBody.contains("blankFrame()"));
    }
}
