package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SessionBrowserTabStripRebindWiringTest {

    private static final String SESSION_CONTROLLER_RELATIVE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private String readModuleResource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void sessionChangeRebindsTheTabStripBeforeTheSwitchingSessionEarlyReturn() throws IOException {
        String source = readModuleResource(SESSION_CONTROLLER_RELATIVE_PATH);
        int onSessionChangedIndex = source.indexOf("public void onSessionChanged(");
        Assert.assertTrue(onSessionChangedIndex >= 0);
        int onSessionChangedEnd = source.indexOf("private void restoreSessionVisibility", onSessionChangedIndex);
        Assert.assertTrue(onSessionChangedEnd > onSessionChangedIndex);
        String onSessionChangedBody = source.substring(onSessionChangedIndex, onSessionChangedEnd);

        int rebindStripIndex = onSessionChangedBody.indexOf("rebindTabStripToCurrentSession()");
        int switchingReturnIndex = onSessionChangedBody.indexOf("restoreSessionVisibility()");
        Assert.assertTrue("Session change must rebind the on-screen tab strip to the new session",
            rebindStripIndex >= 0);
        Assert.assertTrue(
            "Tab strip must be rebound before the switching-session early return so the strip is "
                + "never left showing the previous session's tabs",
            switchingReturnIndex < 0 || rebindStripIndex < switchingReturnIndex);
    }

    @Test
    public void tabStripRebindResolvesTabsThroughTheSessionBindingForTheCurrentSession() throws IOException {
        String source = readModuleResource(SESSION_CONTROLLER_RELATIVE_PATH);
        int rebindIndex = source.indexOf("private void rebindTabStripToCurrentSession()");
        Assert.assertTrue(rebindIndex >= 0);
        int rebindEnd = source.indexOf("private void", rebindIndex
            + "private void rebindTabStripToCurrentSession()".length());
        Assert.assertTrue(rebindEnd > rebindIndex);
        String rebindBody = source.substring(rebindIndex, rebindEnd);
        Assert.assertTrue("Rebind must derive the tab set from the current session via the shared binding",
            rebindBody.contains("BrowserSessionTabStripBinding.forSession(mCurrentSessionHandle, mTabManager)"));
        Assert.assertTrue("Rebind must push the resolved tab set into the favicon strip controller",
            rebindBody.contains("mTabFaviconStripController.update("));
    }
}
