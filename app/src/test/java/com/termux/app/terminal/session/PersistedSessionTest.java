package com.termux.app.terminal.session;

import org.junit.Assert;
import org.junit.Test;

public class PersistedSessionTest {

    @Test
    public void exposesAllConstructorValues() {
        PersistedSession session = new PersistedSession(
            "handle-1", "build", "/bin/login", new String[]{"--login", "-c", "make"}, true, "/home/user");

        Assert.assertEquals("handle-1", session.getHandle());
        Assert.assertEquals("build", session.getName());
        Assert.assertEquals("/bin/login", session.getExecutablePath());
        Assert.assertArrayEquals(new String[]{"--login", "-c", "make"}, session.getArguments());
        Assert.assertTrue(session.isFailSafe());
        Assert.assertEquals("/home/user", session.getWorkingDirectory());
    }

    @Test
    public void retainsNullValues() {
        PersistedSession session = new PersistedSession(null, null, null, null, false, null);

        Assert.assertNull(session.getHandle());
        Assert.assertNull(session.getName());
        Assert.assertNull(session.getExecutablePath());
        Assert.assertNull(session.getArguments());
        Assert.assertFalse(session.isFailSafe());
        Assert.assertNull(session.getWorkingDirectory());
    }

    @Test
    public void copiesArgumentsOnConstructionSoLaterMutationDoesNotLeak() {
        String[] arguments = {"-c", "echo hi"};
        PersistedSession session = new PersistedSession(null, null, null, arguments, false, null);

        arguments[0] = "MUTATED";

        Assert.assertEquals("-c", session.getArguments()[0]);
    }

    @Test
    public void copiesArgumentsOnReadSoCallerCannotMutateInternalState() {
        PersistedSession session =
            new PersistedSession(null, null, null, new String[]{"-c", "echo hi"}, false, null);

        session.getArguments()[0] = "MUTATED";

        Assert.assertEquals("-c", session.getArguments()[0]);
    }

    @Test
    public void supportsEmptyArgumentArray() {
        PersistedSession session = new PersistedSession(null, null, null, new String[0], false, null);

        Assert.assertEquals(0, session.getArguments().length);
    }
}
