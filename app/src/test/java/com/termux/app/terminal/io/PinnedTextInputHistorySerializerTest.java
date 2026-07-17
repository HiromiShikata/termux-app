package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class PinnedTextInputHistorySerializerTest {

    private final PinnedTextInputHistorySerializer mSerializer = new PinnedTextInputHistorySerializer();

    @Test
    public void serializeThenDeserializeRoundTripsThePinnedEntriesInOrder() {
        List<String> pinnedEntries = Arrays.asList("git status", "ls -la");

        List<String> restored = mSerializer.deserialize(mSerializer.serialize(pinnedEntries));

        Assert.assertEquals(pinnedEntries, restored);
    }

    @Test
    public void serializeThenDeserializePreservesEntriesContainingNewlines() {
        List<String> pinnedEntries = Arrays.asList("echo first\necho second", "single line");

        List<String> restored = mSerializer.deserialize(mSerializer.serialize(pinnedEntries));

        Assert.assertEquals(pinnedEntries, restored);
    }

    @Test
    public void deserializeNullYieldsAnEmptyList() {
        Assert.assertTrue(mSerializer.deserialize(null).isEmpty());
    }

    @Test
    public void deserializeEmptyStringYieldsAnEmptyList() {
        Assert.assertTrue(mSerializer.deserialize("").isEmpty());
    }

    @Test
    public void deserializeMalformedJsonYieldsAnEmptyList() {
        Assert.assertTrue(mSerializer.deserialize("not-json").isEmpty());
    }

    @Test
    public void serializeEmptyListYieldsAnEmptyJsonArray() {
        Assert.assertEquals("[]", mSerializer.serialize(java.util.Collections.emptyList()));
    }

    @Test
    public void pinnedEntriesSurviveASerializationRoundTripAndRenderAtTopAfterRestart() {
        SubmittedTextInputHistory beforeRestart = new SubmittedTextInputHistory(5);
        beforeRestart.add("deploy.sh");
        beforeRestart.add("git pull");
        beforeRestart.add("npm test");
        beforeRestart.pin("deploy.sh");

        String persisted = mSerializer.serialize(beforeRestart.getPinnedEntries());

        SubmittedTextInputHistory afterRestart =
            new SubmittedTextInputHistory(5, mSerializer.deserialize(persisted));
        afterRestart.add("new-command");

        Assert.assertTrue(afterRestart.isPinned("deploy.sh"));
        Assert.assertEquals("deploy.sh", afterRestart.getOrderedEntries().get(0));
    }
}
