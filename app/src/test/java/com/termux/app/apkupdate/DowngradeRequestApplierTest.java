package com.termux.app.apkupdate;

import android.content.pm.PackageInstaller;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
public class DowngradeRequestApplierTest {

    private static final int INSTALL_REQUEST_DOWNGRADE_FLAG = 0x00000080;

    private final DowngradeRequestApplier applier = new DowngradeRequestApplier();

    @Test
    public void setsRequestDowngradeInstallFlag() throws Exception {
        PackageInstaller.SessionParams sessionParams =
            new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);

        boolean applied = applier.applyTo(sessionParams);

        Assert.assertTrue(applied);
        Assert.assertNotEquals(0, readInstallFlags(sessionParams) & INSTALL_REQUEST_DOWNGRADE_FLAG);
    }

    private int readInstallFlags(PackageInstaller.SessionParams sessionParams) throws Exception {
        Field installFlagsField = PackageInstaller.SessionParams.class.getDeclaredField("installFlags");
        installFlagsField.setAccessible(true);
        return installFlagsField.getInt(sessionParams);
    }
}
