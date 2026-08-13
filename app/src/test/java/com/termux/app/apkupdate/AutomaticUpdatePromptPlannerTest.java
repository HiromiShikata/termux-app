package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

public class AutomaticUpdatePromptPlannerTest {

    private static ApkUpdateAvailability availableVersion(String latestVersionName) {
        return ApkUpdateAvailability.available(latestVersionName, "https://example.com/app.apk", "app.apk", 0L);
    }

    @Test
    public void anUpdateTheOwnerHasNotSeenOpensTheDialogWithoutAnyButtonPress() {
        Assert.assertEquals("an unseen update has to open the dialog by itself",
            AutomaticUpdatePromptPlanner.Outcome.OPEN_THE_DIALOG,
            AutomaticUpdatePromptPlanner.plan(availableVersion("1.2.3"), null));
    }

    @Test
    public void theVersionTheOwnerCancelledLeavesOnlyTheFloatingButton() {
        Assert.assertEquals("cancelling has to stop the dialog reopening for that same version",
            AutomaticUpdatePromptPlanner.Outcome.SHOW_THE_FLOATING_BUTTON_ONLY,
            AutomaticUpdatePromptPlanner.plan(availableVersion("1.2.3"), "1.2.3"));
    }

    @Test
    public void aNewerVersionThanTheCancelledOneOpensTheDialogAgain() {
        Assert.assertEquals("a version the owner never declined has to open the dialog",
            AutomaticUpdatePromptPlanner.Outcome.OPEN_THE_DIALOG,
            AutomaticUpdatePromptPlanner.plan(availableVersion("1.2.4"), "1.2.3"));
    }

    @Test
    public void anInstalledVersionThatIsAlreadyTheLatestShowsNothing() {
        Assert.assertEquals("being up to date has to leave the screen untouched",
            AutomaticUpdatePromptPlanner.Outcome.SHOW_NOTHING,
            AutomaticUpdatePromptPlanner.plan(ApkUpdateAvailability.upToDate("1.2.3"), null));
    }

    @Test
    public void anAbsentAvailabilityShowsNothing() {
        Assert.assertEquals("no availability has to leave the screen untouched",
            AutomaticUpdatePromptPlanner.Outcome.SHOW_NOTHING,
            AutomaticUpdatePromptPlanner.plan(null, null));
    }
}
