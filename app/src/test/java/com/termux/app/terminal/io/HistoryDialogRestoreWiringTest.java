package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HistoryDialogRestoreWiringTest {

    private static final String TOOLBAR_VIEW_PAGER_PATH =
        "app/src/main/java/com/termux/app/terminal/io/TerminalToolbarViewPager.java";

    private String readRepositorySource(String repositoryRelativePath) throws IOException {
        Path fromRepositoryRoot = Paths.get(repositoryRelativePath);
        if (Files.exists(fromRepositoryRoot)) {
            return new String(Files.readAllBytes(fromRepositoryRoot), StandardCharsets.UTF_8);
        }
        return new String(Files.readAllBytes(Paths.get("..").resolve(repositoryRelativePath)),
            StandardCharsets.UTF_8);
    }

    private String methodBody(String source, String signature) {
        int signatureIndex = source.indexOf(signature);
        Assert.assertTrue("method not found: " + signature, signatureIndex >= 0);
        int bodyEnd = source.indexOf("\n        }", signatureIndex);
        Assert.assertTrue("method end not found after: " + signature, bodyEnd > signatureIndex);
        return source.substring(signatureIndex, bodyEnd);
    }

    @Test
    public void theRestoreIconAndTheRowTapBothGoThroughTheSameRestoreRule() throws IOException {
        String body = methodBody(readRepositorySource(TOOLBAR_VIEW_PAGER_PATH),
            "void showSubmittedTextInputHistory(final EditText editText)");

        int restoreCallCount = body.split("HistoryEntryRestore\\.into\\(", -1).length - 1;

        Assert.assertEquals("the icon and the row tap must restore identically, otherwise the two"
            + " ways into the same action drift apart", 2, restoreCallCount);
    }

    @Test
    public void theRestoreIconClosesTheDialogSoTheUserIsReturnedToTheFieldTheyJustFilled()
            throws IOException {
        String body = methodBody(readRepositorySource(TOOLBAR_VIEW_PAGER_PATH),
            "void showSubmittedTextInputHistory(final EditText editText)");

        Assert.assertTrue("a dialog left open over the text input hides the field the entry was"
            + " just restored into", body.contains("dismiss()"));
    }
}
