package com.termux.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class InputWrittenToASessionWithNoShellProcessIsCountedTest {

    private static final byte[] SUBMITTED_LINE =
        "the owner submitted this line\r".getBytes(StandardCharsets.UTF_8);

    @Test
    public void aSubmissionToASessionWithNoShellProcessIsCountedAsDiscarded() {
        TerminalSession session = new TerminalSession(null, null, null, null, null, null);

        session.write(SUBMITTED_LINE, 0, SUBMITTED_LINE.length);

        ShellInputDeliveryRecord record = session.getShellInputDeliveryRecord();
        Assert.assertEquals("the toolbar clears the field and records the history entry as though the"
                + " submission succeeded, so a submission the session dropped has to be counted rather"
                + " than returning normally with nothing recorded anywhere",
            SUBMITTED_LINE.length, record.getBytesDiscardedBeforeDelivery());
        Assert.assertEquals("nothing was handed to the queue, so nothing may be reported as accepted",
            0L, record.getBytesAcceptedForDelivery());
        Assert.assertEquals("nothing reached the shell", 0L, record.getBytesWrittenToTheShell());
    }
}
