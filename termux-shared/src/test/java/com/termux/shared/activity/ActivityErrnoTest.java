package com.termux.shared.activity;

import com.termux.shared.errors.Errno;
import com.termux.shared.errors.Error;

import org.junit.Assert;
import org.junit.Test;

public class ActivityErrnoTest {

    @Test
    public void typeConstantIsActivityError() {
        Assert.assertEquals("Activity Error", ActivityErrno.TYPE);
        Assert.assertEquals(ActivityErrno.TYPE, ActivityErrno.ERRNO_START_ACTIVITY_FAILED_WITH_EXCEPTION.getType());
    }

    @Test
    public void startActivityErrnosHaveExpectedCodes() {
        Assert.assertEquals(100, ActivityErrno.ERRNO_START_ACTIVITY_FAILED_WITH_EXCEPTION.getCode());
        Assert.assertEquals(101, ActivityErrno.ERRNO_START_ACTIVITY_FOR_RESULT_FAILED_WITH_EXCEPTION.getCode());
        Assert.assertEquals(102, ActivityErrno.ERRNO_STARTING_ACTIVITY_WITH_NULL_CONTEXT.getCode());
    }

    @Test
    public void getErrorFormatsActivityNameAndException() {
        Error error = ActivityErrno.ERRNO_START_ACTIVITY_FAILED_WITH_EXCEPTION.getError("SettingsActivity", "boom");
        Assert.assertEquals("Failed to start \"SettingsActivity\" activity.\nException: boom", error.getMessage());
    }

    @Test
    public void nullContextErrnoMessageDescribesNullContext() {
        Error error = ActivityErrno.ERRNO_STARTING_ACTIVITY_WITH_NULL_CONTEXT.getError("HelpActivity");
        Assert.assertTrue(error.getMessage().contains("null Context"));
    }

    @Test
    public void valueOfResolvesRegisteredActivityErrno() {
        Errno expected = ActivityErrno.ERRNO_START_ACTIVITY_FOR_RESULT_FAILED_WITH_EXCEPTION;
        Errno resolved = Errno.valueOf(ActivityErrno.TYPE, 101);
        Assert.assertSame(expected, resolved);
    }
}
