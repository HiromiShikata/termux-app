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
        PersistedSessionRestoreData plainSession = new PersistedSessionRestoreData("handle-1", "shell-one", null, null, false, "/home/user");

        List<PersistedSessionRestoreData> restored = serializer.deserialize(serializer.serialize(Arrays.asList(plainSession)));

        Assert.assertEquals(1, restored.size());
        PersistedSessionRestoreData result = restored.get(0);
        Assert.assertEquals("handle-1", result.getHandle());
        Assert.assertEquals("shell-one", result.getName());
        Assert.assertNull(result.getExecutablePath());
        Assert.assertNull(result.getArguments());
        Assert.assertFalse(result.isFailSafe());
        Assert.assertEquals("/home/user", result.getWorkingDirectory());
    }

    @Test
    public void roundTripPreservesCommandSessionWithArguments() throws JSONException {
        PersistedSessionRestoreData commandSession = new PersistedSessionRestoreData("handle-2", "shell-two", "/bin/sh",
            new String[]{"-c", "sleep 1"}, false, "/home/user/project");

        List<PersistedSessionRestoreData> restored = serializer.deserialize(serializer.serialize(Arrays.asList(commandSession)));

        Assert.assertEquals(1, restored.size());
        PersistedSessionRestoreData result = restored.get(0);
        Assert.assertEquals("handle-2", result.getHandle());
        Assert.assertEquals("shell-two", result.getName());
        Assert.assertEquals("/bin/sh", result.getExecutablePath());
        Assert.assertArrayEquals(new String[]{"-c", "sleep 1"}, result.getArguments());
        Assert.assertFalse(result.isFailSafe());
        Assert.assertEquals("/home/user/project", result.getWorkingDirectory());
    }

    @Test
    public void roundTripPreservesFailSafeFlagAndNullName() throws JSONException {
        PersistedSessionRestoreData failSafeSession = new PersistedSessionRestoreData("handle-3", null, null, null, true, null);

        List<PersistedSessionRestoreData> restored = serializer.deserialize(serializer.serialize(Arrays.asList(failSafeSession)));

        Assert.assertEquals(1, restored.size());
        PersistedSessionRestoreData result = restored.get(0);
        Assert.assertEquals("handle-3", result.getHandle());
        Assert.assertNull(result.getName());
        Assert.assertNull(result.getExecutablePath());
        Assert.assertNull(result.getArguments());
        Assert.assertTrue(result.isFailSafe());
        Assert.assertNull(result.getWorkingDirectory());
    }

    @Test
    public void roundTripPreservesOrderOfMultipleSessions() throws JSONException {
        List<PersistedSessionRestoreData> sessions = Arrays.asList(
            new PersistedSessionRestoreData("handle-a", "first", null, null, false, "/home/user"),
            new PersistedSessionRestoreData("handle-b", "second", "/bin/sh", new String[]{"-c", "echo hi"}, false, "/tmp"),
            new PersistedSessionRestoreData("handle-c", "third", null, null, true, "/home/user"));

        List<PersistedSessionRestoreData> restored = serializer.deserialize(serializer.serialize(sessions));

        Assert.assertEquals(3, restored.size());
        Assert.assertEquals("first", restored.get(0).getName());
        Assert.assertEquals("second", restored.get(1).getName());
        Assert.assertEquals("third", restored.get(2).getName());
        Assert.assertArrayEquals(new String[]{"-c", "echo hi"}, restored.get(1).getArguments());
        Assert.assertTrue(restored.get(2).isFailSafe());
    }

    @Test
    public void roundTripPreservesEmptyArguments() throws JSONException {
        PersistedSessionRestoreData session = new PersistedSessionRestoreData("handle-4", "empty-args", "/bin/sh", new String[]{}, false, "/tmp");

        List<PersistedSessionRestoreData> restored = serializer.deserialize(serializer.serialize(Arrays.asList(session)));

        Assert.assertEquals(1, restored.size());
        Assert.assertArrayEquals(new String[]{}, restored.get(0).getArguments());
    }

    @Test
    public void deserializeToleratesExplicitJsonNullFields() throws JSONException {
        String serialized = "[{\"handle\":\"handle-5\",\"name\":null,\"executablePath\":null,"
            + "\"arguments\":null,\"isFailSafe\":true,\"workingDirectory\":null}]";

        List<PersistedSessionRestoreData> restored = serializer.deserialize(serialized);

        Assert.assertEquals(1, restored.size());
        PersistedSessionRestoreData result = restored.get(0);
        Assert.assertEquals("handle-5", result.getHandle());
        Assert.assertNull(result.getName());
        Assert.assertNull(result.getExecutablePath());
        Assert.assertNull(result.getArguments());
        Assert.assertTrue(result.isFailSafe());
        Assert.assertNull(result.getWorkingDirectory());
    }

    @Test(expected = JSONException.class)
    public void deserializeRejectsMalformedJson() throws JSONException {
        serializer.deserialize("not-json");
    }
}
