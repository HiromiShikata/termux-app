package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class BrowserTabsSurviveUntilTheSessionIsOpenedTest {

    private static final String CONTROLLER_SOURCE_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    @Test
    public void theStoredTabsOfASessionThatWasNeverOpenedAreNotErasedByAnotherSessionsWrite()
            throws IOException {
        String body = methodBody("private void rebuildAndWritePersistedSessionTabs(");

        int decisionIndex = body.indexOf("BrowserPersistedSessionTabsAction.decide(");
        int removalIndex = body.indexOf("mPersistedTabsBySessionName.remove(sessionName)");

        Assert.assertTrue("the write must decide per session instead of removing whatever is empty",
            decisionIndex >= 0);
        Assert.assertTrue("the decision must know whether that session's stored tabs were loaded",
            body.contains("mSessionHandlesWithTabsLoaded.contains(sessionHandle)"));
        Assert.assertTrue("the stored tabs may only be removed when the decision says so",
            removalIndex > body.indexOf("BrowserPersistedSessionTabsAction.REMOVE"));
        Assert.assertTrue("the removal must come after the decision", removalIndex > decisionIndex);
    }

    @Test
    public void aSessionWhoseStoredTabsWereLoadedIsRecordedSoItsEmptyStateIsTrusted()
            throws IOException {
        String body = methodBody("private void restorePersistedTabsForSession(");

        int recordIndex = body.indexOf("mSessionHandlesWithTabsLoaded.add(sessionHandle)");
        int alreadyHasTabsIndex = body.indexOf("mTabManager.hasTabs(sessionHandle)");

        Assert.assertTrue("the session must be recorded as loaded", recordIndex >= 0);
        Assert.assertTrue("the session must be recorded even when its tabs are already in memory",
            alreadyHasTabsIndex > recordIndex);
    }

    @Test
    public void openingAUrlInAnotherSessionLoadsThatSessionsStoredTabsBeforeAddingTheNewOne()
            throws IOException {
        String body = methodBody("public void openUrlInTabForSession(");

        int restoreIndex = body.indexOf("restorePersistedTabsForSession(");
        int addTabIndex = body.indexOf("mTabManager.addTab(sessionHandle");

        Assert.assertTrue("the stored tabs of that session must be loaded first", restoreIndex >= 0);
        Assert.assertTrue("the new tab must be added after the stored tabs were loaded",
            addTabIndex > restoreIndex);
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
