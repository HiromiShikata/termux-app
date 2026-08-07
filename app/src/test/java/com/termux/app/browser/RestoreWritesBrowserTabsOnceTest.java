package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RestoreWritesBrowserTabsOnceTest {

    private static final String BROWSER_CONTROLLER_PATH =
        "src/main/java/com/termux/app/browser/TermuxBrowserController.java";

    private static final String SESSION_CLIENT_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private String readModuleSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String methodBody(String source, String declarationPrefix) {
        int declarationIndex = source.indexOf(declarationPrefix);
        Assert.assertTrue("declaration not found: " + declarationPrefix, declarationIndex >= 0);
        int bodyStart = source.indexOf(") {", declarationIndex);
        Assert.assertTrue(bodyStart >= 0);
        int bodyEnd = source.indexOf("\n    }", bodyStart);
        Assert.assertTrue(bodyEnd >= 0);
        return source.substring(bodyStart, bodyEnd);
    }

    @Test
    public void theControllerLetsACallerOpenAndCloseAPersistenceBatch() throws IOException {
        String source = readModuleSource(BROWSER_CONTROLLER_PATH);
        Assert.assertTrue(source.contains("public void beginPersistenceBatch()"));
        Assert.assertTrue(source.contains("public void endPersistenceBatch()"));
    }

    @Test
    public void persistingTabsInsideABatchDefersTheWriteInsteadOfSerializingEveryTime() throws IOException {
        String source = readModuleSource(BROWSER_CONTROLLER_PATH);
        String body = methodBody(source, "private void persistSessionTabs(");
        Assert.assertTrue(body.contains("mPersistenceBatch.requestWrite()"));
    }

    @Test
    public void closingTheBatchWritesTheTabsOnce() throws IOException {
        String source = readModuleSource(BROWSER_CONTROLLER_PATH);
        String body = methodBody(source, "public void endPersistenceBatch(");
        Assert.assertTrue(body.contains("mPersistenceBatch.end()"));
        Assert.assertTrue(body.contains("rebuildAndWritePersistedSessionTabs()"));
    }

    @Test
    public void restoringPersistedSessionsBracketsItsAttachLoopWithOneBatch() throws IOException {
        String source = readModuleSource(SESSION_CLIENT_PATH);
        String body = methodBody(source, "public boolean restorePersistedSessions(");
        int beginIndex = body.indexOf("beginPersistenceBatch()");
        int attachIndex = body.indexOf("attachBrowserTabForUrlSessionName(");
        int finallyIndex = body.indexOf("} finally {");
        int endIndex = body.indexOf("endPersistenceBatch()");
        Assert.assertTrue("restore must open a persistence batch", beginIndex >= 0);
        Assert.assertTrue("restore must attach tabs inside the batch", attachIndex > beginIndex);
        Assert.assertTrue("restore must close the batch in a finally block", finallyIndex > attachIndex);
        Assert.assertTrue("restore must close the persistence batch", endIndex > finallyIndex);
    }
}
