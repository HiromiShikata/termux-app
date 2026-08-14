package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

public class PreviousProcessExitImportanceLabelTest {

    @Test
    public void aProcessInFrontOfTheUserIsNamedAsSuch() {
        Assert.assertEquals("a process that ended while the user was looking at it is a different event"
                + " from one reclaimed while it sat in the background",
            "in the foreground", PreviousProcessExitImportanceLabel.of(100));
    }

    @Test
    public void aProcessKeptOnlyForItsCacheIsNamedAsSuch() {
        Assert.assertEquals("a cached process ending is ordinary housekeeping and must not be read as a"
                + " failure",
            "cached", PreviousProcessExitImportanceLabel.of(400));
    }

    @Test
    public void aProcessRunningAServiceIsNamedAsSuch() {
        Assert.assertEquals("this app holds its sessions in a foreground service, so a service-level"
                + " ending is the one that loses them",
            "running a service", PreviousProcessExitImportanceLabel.of(300));
    }

    @Test
    public void anImportanceWithNoNameIsReportedAsThatNumberRatherThanAsSomethingElse() {
        String label = PreviousProcessExitImportanceLabel.of(765);

        Assert.assertTrue("a value this app has no name for must be shown as the value it is, because"
                + " presenting it as any named importance would be a fabricated attribution. Actual"
                + " label: " + label,
            label.contains("765"));
    }
}
