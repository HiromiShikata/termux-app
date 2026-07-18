package com.termux.app.terminal.io;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.widget.EditText;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class PinnedTextInputHistoryEditPromptTest {

    private Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }

    private SubmittedTextInputHistory historyWithPinnedEntry() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("older");
        history.add("deploy.sh");
        history.pin("deploy.sh");
        return history;
    }

    @Test
    public void theEditFieldIsPrefilledWithTheCurrentPinnedText() {
        SubmittedTextInputHistory history = historyWithPinnedEntry();
        AlertDialog dialog = new PinnedTextInputHistoryEditPrompt(
            themedContext(), history, () -> {}).promptEdit("deploy.sh");

        EditText input = dialog.findViewById(R.id.toolbar_text_input_history_edit_input);
        Assert.assertEquals("deploy.sh", input.getText().toString());
    }

    @Test
    public void savingTheEditedTextUpdatesPersistsAndKeepsTheEntryPinnedAtItsPosition() {
        SubmittedTextInputHistory history = historyWithPinnedEntry();
        AtomicInteger persistCalls = new AtomicInteger();
        AlertDialog dialog = new PinnedTextInputHistoryEditPrompt(
            themedContext(), history, persistCalls::incrementAndGet).promptEdit("deploy.sh");

        EditText input = dialog.findViewById(R.id.toolbar_text_input_history_edit_input);
        input.setText("deploy.sh --production");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        Assert.assertEquals(1, persistCalls.get());
        Assert.assertTrue(history.isPinned("deploy.sh --production"));
        Assert.assertFalse(history.isPinned("deploy.sh"));
        Assert.assertEquals(Arrays.asList("deploy.sh --production", "older"),
            history.getOrderedEntries());
    }

    @Test
    public void cancellingLeavesThePinnedEntryUnchanged() {
        SubmittedTextInputHistory history = historyWithPinnedEntry();
        AtomicInteger persistCalls = new AtomicInteger();
        AlertDialog dialog = new PinnedTextInputHistoryEditPrompt(
            themedContext(), history, persistCalls::incrementAndGet).promptEdit("deploy.sh");

        EditText input = dialog.findViewById(R.id.toolbar_text_input_history_edit_input);
        input.setText("discarded change");
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        Assert.assertEquals(0, persistCalls.get());
        Assert.assertTrue(history.isPinned("deploy.sh"));
        Assert.assertFalse(history.isPinned("discarded change"));
    }

    @Test
    public void savingBlankTextDoesNotPersistOrChangeThePinnedEntry() {
        SubmittedTextInputHistory history = historyWithPinnedEntry();
        AtomicInteger persistCalls = new AtomicInteger();
        AlertDialog dialog = new PinnedTextInputHistoryEditPrompt(
            themedContext(), history, persistCalls::incrementAndGet).promptEdit("deploy.sh");

        EditText input = dialog.findViewById(R.id.toolbar_text_input_history_edit_input);
        input.setText("");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        Assert.assertEquals(0, persistCalls.get());
        Assert.assertTrue(history.isPinned("deploy.sh"));
    }
}
