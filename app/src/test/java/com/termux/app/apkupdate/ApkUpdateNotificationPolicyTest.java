package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

public class ApkUpdateNotificationPolicyTest {

    private final ApkUpdateNotificationPolicy policy = new ApkUpdateNotificationPolicy();

    @Test
    public void notifiesUpToDateOnlyWhenUserInitiated() {
        Assert.assertTrue(policy.shouldNotifyUpToDate(true));
        Assert.assertFalse(policy.shouldNotifyUpToDate(false));
    }

    @Test
    public void notifiesCheckFailedOnlyWhenUserInitiated() {
        Assert.assertTrue(policy.shouldNotifyCheckFailed(true));
        Assert.assertFalse(policy.shouldNotifyCheckFailed(false));
    }
}
