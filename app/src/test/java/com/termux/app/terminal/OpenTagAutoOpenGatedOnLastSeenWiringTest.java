package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OpenTagAutoOpenGatedOnLastSeenWiringTest {

    private String readModuleSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String openTagsForSessionBody() throws IOException {
        String source = readModuleSource(
            "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java");
        int start = source.indexOf("private void openTagsForSession(TerminalSession session,");
        Assert.assertTrue("the open tag scan must still be driven from one place", start >= 0);
        int end = source.indexOf("\n    }", start);
        Assert.assertTrue(end > start);
        return source.substring(start, end);
    }

    @Test
    public void theScanDecidesFromTheStoredSeenAndOutTimesWhetherTheOwnerHasBeenShownThisOutput() throws IOException {
        String body = openTagsForSessionBody();

        Assert.assertTrue("without this decision every rescan of a transcript that still holds the tag"
                + " opens the URL again, which is what the owner reported as the same page opening"
                + " over and over",
            body.contains("OpenTagAutoOpenEligibility.shouldAutoOpen("));
        Assert.assertTrue("the seen time is the owner's own reading of the session",
            body.contains("getLastSeenTimeMillis("));
    }

    @Test
    public void whatWasAlreadyOpenedIsRememberedBySessionNameRatherThanBySessionInstance() throws IOException {
        String body = openTagsForSessionBody();

        Assert.assertTrue("the session handle changes every time the shell behind a session is"
                + " replaced, so remembering by handle forgets everything on each replacement and the"
                + " tag still on screen opens again",
            body.contains("session.mSessionName"));
    }
}
