package com.termux.app.terminal.io;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SubmittedTextInputHistorySerializerTest {

    private final SubmittedTextInputHistorySerializer mSerializer = new SubmittedTextInputHistorySerializer();

    @Test
    public void deserializeNullReturnsEmptyList() throws JSONException {
        Assert.assertTrue(mSerializer.deserialize(null).isEmpty());
    }

    @Test
    public void deserializeEmptyStringReturnsEmptyList() throws JSONException {
        Assert.assertTrue(mSerializer.deserialize("").isEmpty());
    }

    @Test
    public void serializeDeserializePreservesOrder() throws JSONException {
        List<String> pinnedEntries = Arrays.asList("git status", "npm test", "ls -la");
        String serialized = mSerializer.serialize(pinnedEntries);

        Assert.assertEquals(pinnedEntries, mSerializer.deserialize(serialized));
    }

    @Test
    public void serializeDeserializePreservesMultiLineEntries() throws JSONException {
        List<String> pinnedEntries = Collections.singletonList("line one\nline two\nline three");
        String serialized = mSerializer.serialize(pinnedEntries);

        Assert.assertEquals(pinnedEntries, mSerializer.deserialize(serialized));
    }

    @Test
    public void serializeSkipsNullAndEmptyEntries() throws JSONException {
        List<String> serializedThenParsed =
            mSerializer.deserialize(mSerializer.serialize(Arrays.asList("keep", null, "")));

        Assert.assertEquals(Collections.singletonList("keep"), serializedThenParsed);
    }

    @Test
    public void pinnedHistorySurvivesSimulatedRestart() throws JSONException {
        SubmittedTextInputHistory beforeRestart = new SubmittedTextInputHistory(5);
        beforeRestart.add("first command");
        beforeRestart.add("pinned command");
        beforeRestart.add("last command");
        beforeRestart.pin("pinned command");

        String persisted = mSerializer.serialize(beforeRestart.pinnedEntries());

        List<String> restoredPinned = mSerializer.deserialize(persisted);
        SubmittedTextInputHistory afterRestart = new SubmittedTextInputHistory(5, restoredPinned);
        afterRestart.add("new command after restart");

        Assert.assertEquals("pinned command", afterRestart.orderedEntries().get(0));
        Assert.assertTrue(afterRestart.isPinned("pinned command"));
        Assert.assertEquals(
            Arrays.asList("pinned command", "new command after restart"),
            afterRestart.orderedEntries());
    }
}
