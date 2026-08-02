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

    @Test
    public void imeSendActionSubmitsThroughTheSameSendPathAsTheSendButton() throws IOException {
        String source = readViewPagerSource();
        String body = methodBody(source, "public void setupTextInputRow(final View textInputRow) {");
        int editorActionListenerStart = body.indexOf("editText.setOnEditorActionListener(");
        int keyListenerStart = body.indexOf("editText.setOnKeyListener(");
        Assert.assertTrue(editorActionListenerStart >= 0 && keyListenerStart > editorActionListenerStart);
        String editorActionListener = body.substring(editorActionListenerStart, keyListenerStart);
        Assert.assertTrue("the IME send action must run the shared send-button submit, which commits the "
                + "composing text, writes the text and then writes the enter sequence, instead of only "
                + "writing the text into the foreground program prompt",
            editorActionListener.contains("enterKeyController.submit(editText, this)"));
    }

    @Test
    public void enterKeySubmitsThroughTheSameSendPathAsTheSendButton() throws IOException {
        String source = readViewPagerSource();
        String body = methodBody(source, "public void setupTextInputRow(final View textInputRow) {");
        int keyListenerStart = body.indexOf("editText.setOnKeyListener(");
        Assert.assertTrue(keyListenerStart >= 0);
        String keyListener = body.substring(keyListenerStart);
        Assert.assertTrue("the KEYCODE_ENTER key listener must run the shared send-button submit, which "
                + "commits the composing text, writes the text and then writes the enter sequence, instead "
                + "of only writing the text into the foreground program prompt",
            keyListener.contains("enterKeyController.submit(editText, this)"));
    }

    @Test
    public void editorActionListenerConsumesOnlyTheActionsItActuallySubmits() throws IOException {
        String source = readViewPagerSource();
        String body = methodBody(source, "public void setupTextInputRow(final View textInputRow) {");
        int editorActionListenerStart = body.indexOf("editText.setOnEditorActionListener(");
        int keyListenerStart = body.indexOf("editText.setOnKeyListener(");
        Assert.assertTrue(editorActionListenerStart >= 0 && keyListenerStart > editorActionListenerStart);
        String editorActionListener = body.substring(editorActionListenerStart, keyListenerStart);
        Assert.assertTrue("the editor action listener must return the submit decision so an unhandled IME "
                + "action (next, previous, search, or a key-event-driven action) propagates to the IME "
                + "default behavior instead of being silently consumed",
            editorActionListener.contains("return shouldSubmit;"));
        Assert.assertFalse("the editor action listener must not unconditionally consume every IME action",
            editorActionListener.contains("return true;"));
    }
}
