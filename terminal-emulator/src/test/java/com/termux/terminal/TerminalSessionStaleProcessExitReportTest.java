package com.termux.terminal;

import org.junit.Assert;
import org.junit.Test;

public class TerminalSessionStaleProcessExitReportTest {

    private static final int SHELL_PROCESS_GENERATION_OF_THE_FIRST_INSTANCE = 0;

    private static final int SHELL_PROCESS_GENERATION_AFTER_ONE_RELEASE = 1;

    private static final int SHELL_PROCESS_GENERATION_AFTER_TWO_RELEASES = 2;

    @Test
    public void recognisesTheExitReportOfAnAlreadyReleasedShellProcessInstanceAsSuperseded() {
        Assert.assertTrue(TerminalSession.exitReportBelongsToASupersededShellProcess(
            SHELL_PROCESS_GENERATION_OF_THE_FIRST_INSTANCE, SHELL_PROCESS_GENERATION_AFTER_ONE_RELEASE));
    }

    @Test
    public void tearsTheSessionDownForAReportOfTheShellProcessInstanceItStillOwns() {
        Assert.assertFalse(TerminalSession.exitReportBelongsToASupersededShellProcess(
            SHELL_PROCESS_GENERATION_AFTER_ONE_RELEASE, SHELL_PROCESS_GENERATION_AFTER_ONE_RELEASE));
    }

    @Test
    public void recognisesAReportThatArrivesAfterTwoSuccessiveReleasesAsSuperseded() {
        Assert.assertTrue(TerminalSession.exitReportBelongsToASupersededShellProcess(
            SHELL_PROCESS_GENERATION_OF_THE_FIRST_INSTANCE, SHELL_PROCESS_GENERATION_AFTER_TWO_RELEASES));
    }

    @Test
    public void tearsTheSessionDownForAReportOfTheInstanceStartedAfterTwoSuccessiveReleases() {
        Assert.assertFalse(TerminalSession.exitReportBelongsToASupersededShellProcess(
            SHELL_PROCESS_GENERATION_AFTER_TWO_RELEASES, SHELL_PROCESS_GENERATION_AFTER_TWO_RELEASES));
    }
}
