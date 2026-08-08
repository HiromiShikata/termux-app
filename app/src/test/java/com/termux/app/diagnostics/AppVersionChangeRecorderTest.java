package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

public class AppVersionChangeRecorderTest {

    private final AppVersionChangeRecorder recorder = new AppVersionChangeRecorder();

    @Test
    public void aLaunchWithNoRecordedPreviousVersionIsTheFirstLaunchAfterInstallation() {
        DiagnosticsVersionChange versionChange = recorder.versionChangeOfThisLaunch(
            AppVersionChangeRecorder.NO_VERSION_CODE_RECORDED, 3661);

        Assert.assertTrue("a launch that follows no recorded launch is the first one of this installation,"
            + " and a report of it must be readable as such", versionChange.isFirstLaunchOfThisVersion());
        Assert.assertFalse("nothing was replaced, so naming a replaced version would invent a fact",
            versionChange.hasPreviousVersionCode());
    }

    @Test
    public void aLaunchOfTheVersionThatAlreadyRanReplacedNothing() {
        DiagnosticsVersionChange versionChange = recorder.versionChangeOfThisLaunch(3661, 3661);

        Assert.assertFalse("an ordinary restart of the same version must not read as an update, otherwise"
            + " every report claims to follow one", versionChange.isFirstLaunchOfThisVersion());
    }

    @Test
    public void aLaunchOfADifferentVersionNamesTheVersionCodeItReplaced() {
        DiagnosticsVersionChange versionChange = recorder.versionChangeOfThisLaunch(3658, 3661);

        Assert.assertTrue("the installed version changed, so this launch is the first of the new one",
            versionChange.isFirstLaunchOfThisVersion());
        Assert.assertTrue("which version it replaced is the fact that ties the report to the update",
            versionChange.hasPreviousVersionCode());
        Assert.assertEquals("the replaced version code must be the one that actually ran before",
            3658, versionChange.getPreviousVersionCode());
    }

    @Test
    public void aLaunchOfAnOlderVersionStillNamesTheVersionCodeItReplaced() {
        DiagnosticsVersionChange versionChange = recorder.versionChangeOfThisLaunch(3661, 3658);

        Assert.assertTrue("a downgrade also changes the installed version, so it must be visible in the"
            + " report the same way an update is", versionChange.isFirstLaunchOfThisVersion());
        Assert.assertEquals("the replaced version code must be the one that actually ran before",
            3661, versionChange.getPreviousVersionCode());
    }
}
