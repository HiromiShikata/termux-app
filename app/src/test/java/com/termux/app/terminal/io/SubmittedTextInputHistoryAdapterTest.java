package com.termux.app.terminal.io;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.termux.R;
import com.termux.app.terminal.io.TerminalToolbarViewPager.SubmittedTextInputHistoryAdapter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class SubmittedTextInputHistoryAdapterTest {

    private Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }

    private SubmittedTextInputHistory pinnedFirstHistory() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("plain-entry");
        history.add("pinned-entry");
        history.pin("pinned-entry");
        return history;
    }

    private View rowFor(SubmittedTextInputHistoryAdapter adapter, FrameLayout parent, int position) {
        return adapter.getView(position, null, parent);
    }

    @Test
    public void pinnedRowShowsTheEditIconAndUnpinnedRowDoesNot() {
        Context context = themedContext();
        FrameLayout parent = new FrameLayout(context);
        SubmittedTextInputHistory history = pinnedFirstHistory();
        SubmittedTextInputHistoryAdapter adapter =
            new SubmittedTextInputHistoryAdapter(context, history, () -> {});

        View pinnedRow = rowFor(adapter, parent, 0);
        View unpinnedRow = rowFor(adapter, parent, 1);

        Assert.assertEquals("pinned-entry", adapter.getItem(0));
        Assert.assertEquals(View.VISIBLE,
            pinnedRow.findViewById(R.id.toolbar_text_input_history_edit_button).getVisibility());
        Assert.assertEquals(View.GONE,
            unpinnedRow.findViewById(R.id.toolbar_text_input_history_edit_button).getVisibility());
    }

    @Test
    public void tappingTheEditIconOpensAPrefilledDialogWhoseSavePersistsTheEditedPinnedText() {
        Context context = themedContext();
        FrameLayout parent = new FrameLayout(context);
        SubmittedTextInputHistory history = pinnedFirstHistory();
        AtomicInteger persistCalls = new AtomicInteger();
        SubmittedTextInputHistoryAdapter adapter =
            new SubmittedTextInputHistoryAdapter(context, history, persistCalls::incrementAndGet);

        View pinnedRow = rowFor(adapter, parent, 0);
        ImageButton editButton =
            pinnedRow.findViewById(R.id.toolbar_text_input_history_edit_button);
        editButton.performClick();

        AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        EditText input = dialog.findViewById(R.id.toolbar_text_input_history_edit_input);
        Assert.assertEquals("pinned-entry", input.getText().toString());

        input.setText("pinned-entry updated");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        Assert.assertTrue(persistCalls.get() >= 1);
        Assert.assertTrue(history.isPinned("pinned-entry updated"));
        Assert.assertEquals(Arrays.asList("pinned-entry updated", "plain-entry"),
            history.getOrderedEntries());
    }
}
