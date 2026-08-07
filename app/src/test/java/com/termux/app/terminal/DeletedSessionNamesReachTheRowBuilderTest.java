package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The row builder can only drop the row of a deleted session when the names the owner deleted
 * actually reach it, and the session list is the single place that holds them. While the list built
 * its rows without them, deleting a session from the long-press menu ended the session and left its
 * row exactly where it was, so the deletion looked as if it had done nothing.
 */
public class DeletedSessionNamesReachTheRowBuilderTest {

    private static String listControllerSource() throws IOException {
        return new String(Files.readAllBytes(
                Paths.get("src/main/java/com/termux/app/terminal/TermuxSessionsListViewController.java")),
            StandardCharsets.UTF_8);
    }

    private static String buildAllRowsBody() throws IOException {
        String source = listControllerSource();
        int start = source.indexOf("private List<SessionHierarchyRow> buildAllRows()");
        Assert.assertTrue("The row building path must exist for this test to mean anything", start >= 0);
        int end = source.indexOf("private Set<String> alwaysNaSessionNames()", start);
        Assert.assertTrue("The end of the row building path must be locatable", end > start);
        return source.substring(start, end);
    }

    @Test
    public void theRowsAreBuiltWithTheNamesTheOwnerDeleted() throws IOException {
        String body = buildAllRowsBody();

        Assert.assertTrue("Every row of the list is built here, so the names the owner deleted must be"
                + " handed to the builder; without them a deleted session keeps its row and the delete"
                + " action looks as if it did nothing: " + body,
            body.contains("userRemovedSessionNames()"));
    }

    @Test
    public void theDeletedNamesAreReadFromTheStoreThatTheDeleteActionWritesTo() throws IOException {
        String source = listControllerSource();

        Assert.assertTrue("Deleting a session records the name through the shared preferences, so the list"
                + " must read that same store; reading anywhere else would let the two drift apart.",
            source.contains("preferences.getUserRemovedSessionNames()"));
    }
}
