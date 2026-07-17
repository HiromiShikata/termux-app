package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SubmittedTextInputHistoryTest {

    @Test
    public void mostRecentSubmissionIsListedFirstWhenNothingIsPinned() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("first");
        history.add("second");
        history.add("third");

        Assert.assertEquals(Arrays.asList("third", "second", "first"), history.getOrderedEntries());
    }

    @Test
    public void addPlacesNewestEntryAtIndexZero() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("older");
        history.add("newest");

        Assert.assertEquals("newest", history.getOrderedEntries().get(0));
    }

    @Test
    public void pinnedEntryIsAlwaysRenderedAboveUnpinnedEntries() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("first");
        history.add("second");
        history.add("third");

        history.pin("first");

        Assert.assertEquals(Arrays.asList("first", "third", "second"), history.getOrderedEntries());
    }

    @Test
    public void unpinnedEntriesKeepChronologicalOrderAmongThemselves() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("a");
        history.add("b");
        history.add("c");
        history.add("d");

        history.pin("b");

        Assert.assertEquals(Arrays.asList("b", "d", "c", "a"), history.getOrderedEntries());
    }

    @Test
    public void multiplePinnedEntriesStayAtTopInRecencyOrder() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("a");
        history.add("b");
        history.add("c");

        history.pin("a");
        history.pin("c");

        Assert.assertEquals(Arrays.asList("c", "a", "b"), history.getOrderedEntries());
    }

    @Test
    public void unpinningReturnsEntryToItsChronologicalPosition() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("first");
        history.add("second");
        history.add("third");
        history.pin("first");
        Assert.assertEquals(Arrays.asList("first", "third", "second"), history.getOrderedEntries());

        history.unpin("first");

        Assert.assertEquals(Arrays.asList("third", "second", "first"), history.getOrderedEntries());
    }

    @Test
    public void pinnedEntriesSuppliedAtConstructionRenderAtTopAfterRestart() {
        List<String> persistedPinnedEntries = Arrays.asList("pinned-old", "pinned-older");
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5, persistedPinnedEntries);

        history.add("fresh-session-entry");

        Assert.assertEquals(
            Arrays.asList("pinned-old", "pinned-older", "fresh-session-entry"),
            history.getOrderedEntries());
        Assert.assertTrue(history.isPinned("pinned-old"));
        Assert.assertTrue(history.isPinned("pinned-older"));
        Assert.assertFalse(history.isPinned("fresh-session-entry"));
    }

    @Test
    public void getPinnedEntriesReturnsOnlyPinnedEntriesInDisplayOrder() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("a");
        history.add("b");
        history.add("c");
        history.pin("a");
        history.pin("c");

        Assert.assertEquals(Arrays.asList("c", "a"), history.getPinnedEntries());
    }

    @Test
    public void unpinnedEntriesAreCappedButPinnedEntriesAreNeverEvicted() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(2);
        history.add("keep-me");
        history.pin("keep-me");
        history.add("one");
        history.add("two");
        history.add("three");

        List<String> ordered = history.getOrderedEntries();

        Assert.assertEquals("keep-me", ordered.get(0));
        Assert.assertTrue(ordered.contains("three"));
        Assert.assertTrue(ordered.contains("two"));
        Assert.assertFalse(ordered.contains("one"));
        Assert.assertEquals(3, ordered.size());
    }

    @Test
    public void reSubmittingAPinnedEntryKeepsItPinnedAndMovesItToTheFront() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add("a");
        history.add("b");
        history.pin("a");

        history.add("a");

        Assert.assertTrue(history.isPinned("a"));
        Assert.assertEquals(Arrays.asList("a", "b"), history.getOrderedEntries());
    }

    @Test
    public void blankSubmissionsAreIgnored() {
        SubmittedTextInputHistory history = new SubmittedTextInputHistory(5);
        history.add(null);
        history.add("");

        Assert.assertTrue(history.isEmpty());
        Assert.assertEquals(Collections.emptyList(), history.getOrderedEntries());
    }
}
