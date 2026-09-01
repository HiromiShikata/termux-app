package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class BrowserBookmarkSessionScopeWiringTest {

    private static final String CONTROLLER_SOURCE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    @Test
    public void loadBookmarksReadsFromPersistedSessionsByCurrentSessionName() throws IOException {
        String body = methodBody("private BrowserBookmarkCollection loadBookmarks()");

        Assert.assertTrue("loadBookmarks must read from mCurrentSessionName",
            body.contains("mCurrentSessionName"));
        Assert.assertTrue("loadBookmarks must look up the session from the map",
            body.contains("mPersistedTabsBySessionName.get("));
    }

    @Test
    public void saveBookmarksPutsToSessionMapAndThenWritesToPrefs() throws IOException {
        String body = methodBody("private void saveBookmarks(");

        Assert.assertTrue("saveBookmarks must use mCurrentSessionName",
            body.contains("mCurrentSessionName"));
        Assert.assertTrue("saveBookmarks must call withBookmarks(",
            body.contains("withBookmarks("));
        Assert.assertTrue("saveBookmarks must put back into the session map",
            body.contains("mPersistedTabsBySessionName.put("));
        Assert.assertTrue("saveBookmarks must persist via writePersistedSessionTabs()",
            body.contains("writePersistedSessionTabs()"));
    }

    @Test
    public void sessionRemovalPreservesBookmarksWithDeletedAtMillis() throws IOException {
        String body = methodBody("public void onSessionRemoved(@NonNull TerminalSession session,");

        Assert.assertTrue("session removal must check hasRetainableData()",
            body.contains("hasRetainableData()"));
        Assert.assertTrue("session removal must stamp deletion time with withDeletedAtMillis(",
            body.contains("withDeletedAtMillis("));
    }

    @Test
    public void reconnectClearsSessionDeletedMarkerBeforeRestoringTabs() throws IOException {
        String body = methodBody("public void restoreTabsForReconnectedSession(");

        int clearIndex = body.indexOf("clearSessionDeletedMarker(");
        int restoreIndex = body.indexOf("restorePersistedTabsForSession(");

        Assert.assertTrue("restoreTabsForReconnectedSession must call clearSessionDeletedMarker(",
            clearIndex >= 0);
        Assert.assertTrue("clearSessionDeletedMarker( must be called before restorePersistedTabsForSession(",
            clearIndex < restoreIndex);
    }

    @Test
    public void loadCurrentSessionHistoryReadsHistoryFromPersistedSessionEntry() throws IOException {
        String body = methodBody("private void loadCurrentSessionHistory()");

        Assert.assertTrue("loadCurrentSessionHistory must look up the session from the map",
            body.contains("mPersistedTabsBySessionName.get("));
        Assert.assertTrue("loadCurrentSessionHistory must read getHistory()",
            body.contains("getHistory()"));
    }

    @Test
    public void loadPersistedSessionTabsPrunesStaleDeletedSessionsAtStartup() throws IOException {
        String body = methodBody("private void loadPersistedSessionTabs()");

        Assert.assertTrue("loadPersistedSessionTabs must call pruneStaleDeleted(",
            body.contains("pruneStaleDeleted("));
    }

    @Test
    public void legacyMigrationIsAppliedOnFirstSessionChange() throws IOException {
        String body = methodBody("public void onSessionChanged(");

        Assert.assertTrue("onSessionChanged must call applyLegacyMigrationIfPending()",
            body.contains("applyLegacyMigrationIfPending()"));
    }

    @Test
    public void legacyMigrationClearsTheLegacyPreferenceKeys() throws IOException {
        String body = methodBody("private void loadPersistedSessionTabs()");

        Assert.assertTrue("loadPersistedSessionTabs must read getBrowserBookmarks()",
            body.contains("getBrowserBookmarks()"));
        Assert.assertTrue("loadPersistedSessionTabs must clear bookmarks via setBrowserBookmarks(",
            body.contains("setBrowserBookmarks("));
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
