package com.termux.app.terminal.session;

import org.junit.Assert;
import org.junit.Test;

public class PersistedSessionRestoreDataTest {

    @Test
    public void exposesAllConstructorValues() {
        PersistedSessionRestoreData restoreData = new PersistedSessionRestoreData(
            "handle-1", "build", "/bin/login", new String[]{"--login", "-c", "make"}, true, "/home/user");

        Assert.assertEquals("handle-1", restoreData.getHandle());
        Assert.assertEquals("build", restoreData.getName());
        Assert.assertEquals("/bin/login", restoreData.getExecutablePath());
        Assert.assertArrayEquals(new String[]{"--login", "-c", "make"}, restoreData.getArguments());
        Assert.assertTrue(restoreData.isFailSafe());
        Assert.assertEquals("/home/user", restoreData.getWorkingDirectory());
    }

    @Test
    public void retainsNullValues() {
        PersistedSessionRestoreData restoreData = new PersistedSessionRestoreData(null, null, null, null, false, null);

        Assert.assertNull(restoreData.getHandle());
        Assert.assertNull(restoreData.getName());
        Assert.assertNull(restoreData.getExecutablePath());
        Assert.assertNull(restoreData.getArguments());
        Assert.assertFalse(restoreData.isFailSafe());
        Assert.assertNull(restoreData.getWorkingDirectory());
    }

    @Test
    public void copiesArgumentsOnConstructionSoLaterMutationDoesNotLeak() {
        String[] arguments = {"-c", "echo hi"};
        PersistedSessionRestoreData restoreData =
            new PersistedSessionRestoreData(null, null, null, arguments, false, null);

        arguments[0] = "MUTATED";

        Assert.assertEquals("-c", restoreData.getArguments()[0]);
    }

    @Test
    public void copiesArgumentsOnReadSoCallerCannotMutateInternalState() {
        PersistedSessionRestoreData restoreData =
            new PersistedSessionRestoreData(null, null, null, new String[]{"-c", "echo hi"}, false, null);

        restoreData.getArguments()[0] = "MUTATED";

        Assert.assertEquals("-c", restoreData.getArguments()[0]);
    }
}
