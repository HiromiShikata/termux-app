package com.termux.app.diagnostics;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;

public class PreviousProcessExitCodesComeFromThePlatformTest {

    private static final String WHY_A_REASON_MUST_MATCH =
        "the report translates whatever number the platform hands back, so a translation table that"
            + " disagrees with the platform by one number renames every ending after it and the reader"
            + " is told a cause that never happened";

    private static final String WHY_AN_IMPORTANCE_MUST_MATCH =
        "the importance says whether the app was on screen when it ended, which is what separates an"
            + " ending the owner saw from one the system did while the app was cached";

    private static void assertReasonNamed(String expectedLabel, int platformReason) {
        Assert.assertEquals(WHY_A_REASON_MUST_MATCH, expectedLabel,
            PreviousProcessExitReasonLabel.of(platformReason));
    }

    private static void assertImportanceNamed(String expectedLabel, int platformImportance) {
        Assert.assertEquals(WHY_AN_IMPORTANCE_MUST_MATCH, expectedLabel,
            PreviousProcessExitImportanceLabel.of(platformImportance));
    }

    @Test
    public void everyEndingReasonThePlatformDefinesIsTranslatedFromThePlatformsOwnNumber() {
        assertReasonNamed("a reason the system did not record", ApplicationExitInfo.REASON_UNKNOWN);
        assertReasonNamed("the app ending itself", ApplicationExitInfo.REASON_EXIT_SELF);
        assertReasonNamed("a signal sent to the process", ApplicationExitInfo.REASON_SIGNALED);
        assertReasonNamed("the system reclaiming memory", ApplicationExitInfo.REASON_LOW_MEMORY);
        assertReasonNamed("a crash in Java code", ApplicationExitInfo.REASON_CRASH);
        assertReasonNamed("a crash in native code", ApplicationExitInfo.REASON_CRASH_NATIVE);
        assertReasonNamed("an unresponsive main thread", ApplicationExitInfo.REASON_ANR);
        assertReasonNamed("the app failing to start", ApplicationExitInfo.REASON_INITIALIZATION_FAILURE);
        assertReasonNamed("a permission of the app changing", ApplicationExitInfo.REASON_PERMISSION_CHANGE);
        assertReasonNamed("the system judging the app's resource use excessive",
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE);
        assertReasonNamed("the user asking for the app to end", ApplicationExitInfo.REASON_USER_REQUESTED);
        assertReasonNamed("the user stopping the app from settings", ApplicationExitInfo.REASON_USER_STOPPED);
        assertReasonNamed("a process this app depends on ending", ApplicationExitInfo.REASON_DEPENDENCY_DIED);
        assertReasonNamed("a reason the system grouped as other", ApplicationExitInfo.REASON_OTHER);
        assertReasonNamed("the system freezer", ApplicationExitInfo.REASON_FREEZER);
        assertReasonNamed("the installed package changing state",
            ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE);
        assertReasonNamed("the app being updated", ApplicationExitInfo.REASON_PACKAGE_UPDATED);
    }

    @Test
    public void everyImportanceThePlatformDefinesIsTranslatedFromThePlatformsOwnNumber() {
        assertImportanceNamed("in the foreground",
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND);
        assertImportanceNamed("running a foreground service",
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE);
        assertImportanceNamed("visible to the user",
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE);
        assertImportanceNamed("perceptible to the user",
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE);
        assertImportanceNamed("running a service",
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE);
        assertImportanceNamed("on top with the screen off",
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING);
        assertImportanceNamed("unable to save its state",
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE);
        assertImportanceNamed("cached", ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED);
        assertImportanceNamed("already gone", ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE);
    }

    @Test
    public void theVersionGateIsTheVersionThatIntroducedTheRecordRatherThanANumberOfItsOwn() {
        Assert.assertTrue("the record of why a process ended arrives with the platform version that"
                + " introduced it, so a gate set one version too high silently reports every device on"
                + " that version as keeping no record",
            ProcessExitReasonAvailability.isRecordedBy(Build.VERSION_CODES.R));
        Assert.assertFalse("a gate set one version too low calls an API that does not exist there and"
                + " ends the reading with a failure instead of a report",
            ProcessExitReasonAvailability.isRecordedBy(Build.VERSION_CODES.Q));
    }
}
