package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TerminalToolbarTextInputSingleSubmitWiringTest {

    private static final String VIEW_PAGER_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/io/TerminalToolbarViewPager.java";

    private String readViewPagerSource() throws IOException {
        Path moduleRelative = Paths.get(VIEW_PAGER_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(VIEW_PAGER_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String methodBody(String source, String signature) {
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue("method not found: " + signature, methodIndex >= 0);
        int methodEnd = source.indexOf("\n        }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void editorActionSubmitIsGatedSoOneImeEventCannotFireSubmitTwice() throws IOException {
        String source = readViewPagerSource();
        String body = methodBody(source, "public void setupTextInputRow(final View textInputRow) {");
        Assert.assertTrue("the editor action listener must consult the submit decision so a key-event-driven "
                + "editor action is left to the KEYCODE_ENTER key listener instead of firing a second submit "
                + "for the same IME event",
            body.contains("ToolbarTextInputSubmitDecision.shouldSubmitForEditorAction(actionId, event != null)"));
    }
}
