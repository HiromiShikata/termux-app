package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

public class ApkUpdateTapDestinationTest {

    private static final class Case {
        final boolean canRequestPackageInstalls;
        final ApkUpdateTapDestination expectedDestination;

        Case(boolean canRequestPackageInstalls, ApkUpdateTapDestination expectedDestination) {
            this.canRequestPackageInstalls = canRequestPackageInstalls;
            this.expectedDestination = expectedDestination;
        }
    }

    private static final Case[] CASES = {
        new Case(true, ApkUpdateTapDestination.INSTALL_CONFIRMATION),
        new Case(false, ApkUpdateTapDestination.UNKNOWN_APPS_SETTINGS),
    };

    @Test
    public void forInstallPermissionSelectsDestinationForEachPermissionState() {
        for (Case testCase : CASES) {
            Assert.assertEquals(
                "canRequestPackageInstalls=" + testCase.canRequestPackageInstalls,
                testCase.expectedDestination,
                ApkUpdateTapDestination.forInstallPermission(testCase.canRequestPackageInstalls));
        }
    }
}
