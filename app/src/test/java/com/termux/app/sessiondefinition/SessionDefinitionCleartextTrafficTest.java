package com.termux.app.sessiondefinition;

import android.content.pm.ApplicationInfo;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SessionDefinitionCleartextTrafficTest {

    @Test
    public void applicationPermitsCleartextHttpTrafficForUserConfiguredIndexUrls() {
        ApplicationInfo applicationInfo = RuntimeEnvironment.getApplication().getApplicationInfo();
        boolean cleartextPermitted =
            (applicationInfo.flags & ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0;
        Assert.assertTrue(
            "Application must permit cleartext HTTP traffic so http:// session definition index URLs can be fetched",
            cleartextPermitted);
    }
}
