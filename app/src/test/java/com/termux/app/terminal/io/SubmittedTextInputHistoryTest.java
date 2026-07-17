package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SubmittedTextInputHistoryTest {

    @Test
    public void addKeepsMostRecentEntryFirst() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("first");
        history.add("second");
        history.add("third");

        Assert.assertEquals(Arrays.asList("third", "second", "first"), history.orderedEntries());
    }

    @Test
    public void addDeduplicatesAndMovesEntryToFront() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("a");
        history.add("b");
        history.add("a");

        Assert.assertEquals(Arrays.asList("a", "b"), history.orderedEntries());
    }

    @Test
    public void addIgnoresNullAndEmpty() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add(null);
        history.add("");

        Assert.assertTrue(history.isEmpty());
    }

    @Test
    public void pinnedEntryRendersAboveUnpinnedEntries() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("oldest");
        history.add("middle");
        history.add("newest");
        history.pin("oldest");

        Assert.assertEquals(Arrays.asList("oldest", "newest", "middle"), history.orderedEntries());
        Assert.assertTrue(history.isPinned("oldest"));
    }

    @Test
    public void multiplePinnedEntriesKeepRecencyOrderAmongThemselves() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("one");
        history.add("two");
        history.add("three");
        history.pin("one");
        history.pin("three");

        Assert.assertEquals(Arrays.asList("three", "one", "two"), history.orderedEntries());
    }

    @Test
    public void unpinReturnsEntryToChronologicalPosition() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("oldest");
        history.add("middle");
        history.add("newest");
        history.pin("oldest");
        history.unpin("oldest");

        Assert.assertEquals(Arrays.asList("newest", "middle", "oldest"), history.orderedEntries());
        Assert.assertFalse(history.isPinned("oldest"));
    }

    @Test
    public void unpinnedEntriesAreBoundedByMaxSize() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(3);
        history.add("1");
        history.add("2");
        history.add("3");
        history.add("4");
        history.add("5");

        Assert.assertEquals(Arrays.asList("5", "4", "3"), history.orderedEntries());
    }

    @Test
    public void pinnedEntriesSurviveBeyondUnpinnedLimit() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(3);
        history.add("pinned");
        history.pin("pinned");
        history.add("1");
        history.add("2");
        history.add("3");
        history.add("4");

        Assert.assertEquals(Arrays.asList("pinned", "4", "3", "2"), history.orderedEntries());
        Assert.assertTrue(history.isPinned("pinned"));
    }

    @Test
    public void pinnedEntriesReportedForPersistenceInRecencyOrder() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("a");
        history.add("b");
        history.add("c");
        history.pin("a");
        history.pin("c");

        Assert.assertEquals(Arrays.asList("c", "a"), history.pinnedEntries());
    }

    @Test
    public void seededPinnedEntriesRenderAtTopAfterRestart() {
        List<String> persistedPinned = Arrays.asList("kept-command", "another-pinned");
        SubmittedTextInputHistory restored = new SubmittedTextInputHistory(5, persistedPinned);
        restored.add("freshly-typed");

        Assert.assertEquals(Arrays.asList("kept-command", "another-pinned", "freshly-typed"), restored.orderedEntries());
        Assert.assertTrue(restored.isPinned("kept-command"));
        Assert.assertTrue(restored.isPinned("another-pinned"));
        Assert.assertFalse(restored.isPinned("freshly-typed"));
    }

    @Test
    public void seededConstructorIgnoresNullAndDuplicatePinnedEntries() {
        SubmittedTextInputHistory restored =
            new SubmittedTextInputHistory(5, Arrays.asList("dup", null, "dup", ""));

        Assert.assertEquals(Collections.singletonList("dup"), restored.pinnedEntries());
    }
}
