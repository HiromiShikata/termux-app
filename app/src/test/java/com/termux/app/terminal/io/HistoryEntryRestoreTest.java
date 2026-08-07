package com.termux.app.terminal.io;

import android.content.Context;
import android.widget.EditText;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class HistoryEntryRestoreTest {

    private EditText textInput() {
        Context context = RuntimeEnvironment.getApplication();
        return new EditText(context);
    }

    @Test
    public void restoringAnEntryReplacesWhateverWasInTheFieldWithIt() {
        EditText editText = textInput();
        editText.setText("half typed text the user abandoned");

        HistoryEntryRestore.into(editText, "the restored submission");

        Assert.assertEquals("the restored submission", editText.getText().toString());
    }

    @Test
    public void restoringAnEntryLeavesTheCursorAtTheEndSoTypingContinuesTheText() {
        EditText editText = textInput();

        HistoryEntryRestore.into(editText, "the restored submission");

        Assert.assertEquals("a cursor left at position zero would make the next keystroke land in"
                + " front of the restored text", "the restored submission".length(),
            editText.getSelectionStart());
        Assert.assertEquals("the restored submission".length(), editText.getSelectionEnd());
    }
}
