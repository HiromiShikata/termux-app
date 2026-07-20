package com.termux.app.terminal.io;

import android.view.inputmethod.EditorInfo;

import org.junit.Assert;
import org.junit.Test;

public class ToolbarTextInputSubmitDecisionTest {

    @Test
    public void softKeyboardSendActionSubmits() {
        Assert.assertTrue(
            ToolbarTextInputSubmitDecision.shouldSubmitForEditorAction(EditorInfo.IME_ACTION_SEND, false));
    }

    @Test
    public void imeNullActionWithoutAKeyEventSubmits() {
        Assert.assertTrue(
            ToolbarTextInputSubmitDecision.shouldSubmitForEditorAction(EditorInfo.IME_NULL, false));
    }

    @Test
    public void keyEventDrivenEditorActionIsLeftToTheEnterKeyListener() {
        Assert.assertFalse(
            ToolbarTextInputSubmitDecision.shouldSubmitForEditorAction(EditorInfo.IME_NULL, true));
        Assert.assertFalse(
            ToolbarTextInputSubmitDecision.shouldSubmitForEditorAction(EditorInfo.IME_ACTION_SEND, true));
    }

    @Test
    public void unrelatedImeActionsDoNotSubmit() {
        Assert.assertFalse(
            ToolbarTextInputSubmitDecision.shouldSubmitForEditorAction(EditorInfo.IME_ACTION_NEXT, false));
        Assert.assertFalse(
            ToolbarTextInputSubmitDecision.shouldSubmitForEditorAction(EditorInfo.IME_ACTION_SEARCH, false));
        Assert.assertFalse(
            ToolbarTextInputSubmitDecision.shouldSubmitForEditorAction(EditorInfo.IME_ACTION_PREVIOUS, false));
    }
}
