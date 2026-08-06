package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class BrowserTabsSurviveASessionReconnectTest {

    private static final String CONTROLLER_SOURCE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    @Test
    public void aReconnectDoesNotDestroyTheLiveTabsOfTheSessionBeingReconnected() throws IOException {
        String body = methodBody("public void onSessionRemoved(@NonNull TerminalSession session,");

        int retentionIndex = body.indexOf("keepLiveTabsForReconnect(");
        int webViewTeardownIndex = body.indexOf("mWebViewHost.removeSession(");
        int tabTeardownIndex = body.indexOf("mTabManager.removeSession(");

        Assert.assertTrue("the removal must ask whether the live tabs are kept", retentionIndex >= 0);
        Assert.assertTrue("the WebViews may only be destroyed after that question",
            webViewTeardownIndex > retentionIndex);
        Assert.assertTrue("the tabs may only be dropped after that question",
            tabTeardownIndex > retentionIndex);
    }

    @Test
    public void aReconnectedSessionTakesOverTheLiveTabsInsteadOfReloadingThemFromTheStoredUrls()
            throws IOException {
        String body = methodBody("public void restoreTabsForReconnectedSession(");

        int moveIndex = body.indexOf("moveLiveTabsToReconnectedSession(");
        int persistedRestoreIndex = body.indexOf("restorePersistedTabsForSession(");

        Assert.assertTrue("the live tabs must be carried over first", moveIndex >= 0);
        Assert.assertTrue("reloading from the stored urls is only the fallback",
            persistedRestoreIndex > moveIndex);
    }

    @Test
    public void theCarriedOverTabsKeepTheirWebViewsByMovingTheSessionHandleTheyBelongTo()
            throws IOException {
        String body = methodBody("private boolean moveLiveTabsToReconnectedSession(");

        Assert.assertTrue("the tabs of the previous handle must be moved, not rebuilt",
            body.contains("mTabManager.moveSession("));
        Assert.assertTrue("the browser visibility of that session must move with them",
            body.contains("mSessionVisibilityState.moveSession("));
    }

    @Test
    public void aSessionClosedWhileAReconnectWasPendingLeavesNoLiveTabsBehind() throws IOException {
        String body = methodBody("private boolean keepLiveTabsForReconnect(");

        Assert.assertTrue("a removal that is not a reconnect must release the pending handle",
            body.contains("forgetReconnectingSessionHandle("));
        Assert.assertTrue("only a reconnect keeps the live tabs",
            body.contains("BrowserSessionRemovalLiveTabRetention.shouldKeepLiveTabs(reason)"));
    }

    private static String methodBody(String declarationPrefix) throws IOException {
        String source = readControllerSource();
        int declarationIndex = source.indexOf(declarationPrefix);
        Assert.assertTrue("TermuxBrowserController.java must declare " + declarationPrefix,
            declarationIndex >= 0);
        int bodyStart = source.indexOf(") {", declarationIndex);
        Assert.assertTrue("the parameter list of " + declarationPrefix + " must be terminated",
            bodyStart >= 0);
        int bodyEnd = source.indexOf("\n    }", bodyStart);
        Assert.assertTrue("the body of " + declarationPrefix + " must be terminated", bodyEnd >= 0);
        return source.substring(bodyStart, bodyEnd);
    }

    private static String readControllerSource() throws IOException {
        File fromModuleDirectory = new File(CONTROLLER_SOURCE_PATH);
        File source = fromModuleDirectory.exists()
            ? fromModuleDirectory
            : new File("app/" + CONTROLLER_SOURCE_PATH);
        Assert.assertTrue(
            "TermuxBrowserController.java must be readable at " + source.getAbsolutePath(),
            source.exists());
        return new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
    }
}
