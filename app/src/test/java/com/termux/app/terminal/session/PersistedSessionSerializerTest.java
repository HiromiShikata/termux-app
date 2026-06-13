package com.termux.app.terminal.session;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class PersistedSessionSerializerTest {

    private final PersistedSessionSerializer serializer = new PersistedSessionSerializer();

    @Test
    public void deserializeOfEmptyStringReturnsEmptyList() throws JSONException {
        Assert.assertTrue(serializer.deserialize("").isEmpty());
        Assert.assertTrue(serializer.deserialize(null).isEmpty());
    }

    @Test
    public void serializeOfEmptyListRoundTripsToEmptyList() throws JSONException {
        String serialized = serializer.serialize(new ArrayList<>());
        Assert.assertTrue(serializer.deserialize(serialized).isEmpty());
    }

    @Test
    public void roundTripPreservesPlainSession() throws JSONException {
        PersistedSession plainSession = new PersistedSession("shell-one", null, null, false, "/home/user");

        List<PersistedSession> restored = serializer.deserialize(serializer.serialize(Arrays.asList(plainSession)));

        Assert.assertEquals(1, restored.size());
        PersistedSession result = restored.get(0);
        Assert.assertEquals("shell-one", result.getName());
        Assert.assertNull(result.getExecutablePath());
        Assert.assertNull(result.getArguments());
        Assert.assertFalse(result.isFailSafe());
        Assert.assertEquals("/home/user", result.getWorkingDirectory());
    }

    @Test
    public void roundTripPreservesCommandSessionWithArguments() throws JSONException {
        PersistedSession commandSession = new PersistedSession("shell-two", "/bin/sh",
            new String[]{"-c", "sleep 1"}, false, "/home/user/project");

        List<PersistedSession> restored = serializer.deserialize(serializer.serialize(Arrays.asList(commandSession)));

        Assert.assertEquals(1, restored.size());
        PersistedSession result = restored.get(0);
        Assert.assertEquals("shell-two", result.getName());
        Assert.assertEquals("/bin/sh", result.getExecutablePath());
        Assert.assertArrayEquals(new String[]{"-c", "sleep 1"}, result.getArguments());
        Assert.assertFalse(result.isFailSafe());
        Assert.assertEquals("/home/user/project", result.getWorkingDirectory());
    }

    @Test
    public void roundTripPreservesFailSafeFlagAndNullName() throws JSONException {
        PersistedSession failSafeSession = new PersistedSession(null, null, null, true, null);

        List<PersistedSession> restored = serializer.deserialize(serializer.serialize(Arrays.asList(failSafeSession)));

        Assert.assertEquals(1, restored.size());
        PersistedSession result = restored.get(0);
        Assert.assertNull(result.getName());
        Assert.assertNull(result.getExecutablePath());
        Assert.assertNull(result.getArguments());
        Assert.assertTrue(result.isFailSafe());
        Assert.assertNull(result.getWorkingDirectory());
    }

    @Test
    public void roundTripPreservesOrderOfMultipleSessions() throws JSONException {
        List<PersistedSession> sessions = Arrays.asList(
            new PersistedSession("first", null, null, false, "/home/user"),
            new PersistedSession("second", "/bin/sh", new String[]{"-c", "echo hi"}, false, "/tmp"),
            new PersistedSession("third", null, null, true, "/home/user"));

        List<PersistedSession> restored = serializer.deserialize(serializer.serialize(sessions));

        Assert.assertEquals(3, restored.size());
        Assert.assertEquals("first", restored.get(0).getName());
        Assert.assertEquals("second", restored.get(1).getName());
        Assert.assertEquals("third", restored.get(2).getName());
        Assert.assertArrayEquals(new String[]{"-c", "echo hi"}, restored.get(1).getArguments());
        Assert.assertTrue(restored.get(2).isFailSafe());
    }

    @Test
    public void roundTripPreservesEmptyArguments() throws JSONException {
        PersistedSession session = new PersistedSession("empty-args", "/bin/sh", new String[]{}, false, "/tmp");

        List<PersistedSession> restored = serializer.deserialize(serializer.serialize(Arrays.asList(session)));

        Assert.assertEquals(1, restored.size());
        Assert.assertArrayEquals(new String[]{}, restored.get(0).getArguments());
    }
}
