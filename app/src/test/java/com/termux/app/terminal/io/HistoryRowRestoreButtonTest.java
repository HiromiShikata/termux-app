package com.termux.app.terminal.io;

import android.content.Context;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.termux.R;
import com.termux.app.terminal.io.TerminalToolbarViewPager.SubmittedTextInputHistoryAdapter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class HistoryRowRestoreButtonTest {

    private Context themedContext() {
        return new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
    }

    private SubmittedTextInputHistory twoEntryHistory() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("the older submission");
        history.add("the newer submission");
        return history;
    }

    @Test
    public void theRestoreIconSitsAtTheRightEdgeOfTheRowAndTheBookmarkIconAtTheLeftEdge() {
        Context context = themedContext();
        SubmittedTextInputHistoryAdapter adapter = new SubmittedTextInputHistoryAdapter(
            context, twoEntryHistory(), () -> {}, entry -> {});

        ViewGroup row = (ViewGroup) adapter.getView(0, null, new FrameLayout(context));

        Assert.assertEquals("the restore target is the button the owner reaches on every row, so it"
                + " belongs at the right edge under the thumb rather than at the left edge",
            R.id.toolbar_text_input_history_restore_button,
            row.getChildAt(row.getChildCount() - 1).getId());
        Assert.assertEquals("the bookmark button is the occasional one and takes the left edge the"
                + " restore button vacates", R.id.toolbar_text_input_history_pin_button,
            row.getChildAt(0).getId());
    }

    @Test
    public void theRestoreIconSitsOutsideTheScrollingContainerThatSwallowsARowTap() {
        Context context = themedContext();
        SubmittedTextInputHistoryAdapter adapter = new SubmittedTextInputHistoryAdapter(
            context, twoEntryHistory(), () -> {}, entry -> {});

        ViewGroup row = (ViewGroup) adapter.getView(0, null, new FrameLayout(context));
        View restoreButton = row.findViewById(R.id.toolbar_text_input_history_restore_button);
        View entryScroll = row.findViewById(R.id.toolbar_text_input_history_entry_scroll);

        Assert.assertSame("a tap target nested inside the scrolling container would compete with the"
            + " scroll gesture exactly as the entry text does today", row, restoreButton.getParent());
        Assert.assertNotSame(entryScroll, restoreButton.getParent());
    }

    @Test
    public void tappingTheRestoreIconDeliversThatRowsOwnEntry() {
        Context context = themedContext();
        List<String> restoredEntries = new ArrayList<>();
        SubmittedTextInputHistoryAdapter adapter = new SubmittedTextInputHistoryAdapter(
            context, twoEntryHistory(), () -> {}, restoredEntries::add);
        FrameLayout parent = new FrameLayout(context);

        adapter.getView(1, null, parent)
            .findViewById(R.id.toolbar_text_input_history_restore_button).performClick();
        adapter.getView(0, null, parent)
            .findViewById(R.id.toolbar_text_input_history_restore_button).performClick();

        Assert.assertEquals(java.util.Arrays.asList("the older submission", "the newer submission"),
            restoredEntries);
    }

    @Test
    public void theRestoreIconCarriesAContentDescriptionSoItIsAnnouncedRatherThanUnnamed() {
        Context context = themedContext();
        SubmittedTextInputHistoryAdapter adapter = new SubmittedTextInputHistoryAdapter(
            context, twoEntryHistory(), () -> {}, entry -> {});

        ImageButton restoreButton = adapter.getView(0, null, new FrameLayout(context))
            .findViewById(R.id.toolbar_text_input_history_restore_button);

        Assert.assertFalse("a bare icon with no content description is unreachable for a user who"
            + " relies on a screen reader", TextUtils.isEmpty(restoreButton.getContentDescription()));
    }

    @Test
    public void aRecycledRowRestoresTheEntryItWasRebound() {
        Context context = themedContext();
        List<String> restoredEntries = new ArrayList<>();
        SubmittedTextInputHistoryAdapter adapter = new SubmittedTextInputHistoryAdapter(
            context, twoEntryHistory(), () -> {}, restoredEntries::add);
        FrameLayout parent = new FrameLayout(context);

        View recycled = adapter.getView(0, null, parent);
        adapter.getView(1, recycled, parent)
            .findViewById(R.id.toolbar_text_input_history_restore_button).performClick();

        Assert.assertEquals(java.util.Collections.singletonList("the older submission"),
            restoredEntries);
    }
}
