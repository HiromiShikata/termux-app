package com.termux.app.apkupdate;

import org.junit.Assert;
import org.junit.Test;

public class ReleaseTagVersionNameResolverTest {

    private final ReleaseTagVersionNameResolver resolver = new ReleaseTagVersionNameResolver();

    @Test
    public void stripsLeadingVPrefix() {
        Assert.assertEquals("0.118.0", resolver.resolveFromTag("v0.118.0"));
    }

    @Test
    public void stripsBuildMetadataSuffix() {
        Assert.assertEquals("0.118.0", resolver.resolveFromTag("v0.118.0+apt-android-7-github-debug"));
    }

    @Test
    public void keepsPlainVersion() {
        Assert.assertEquals("0.119.0", resolver.resolveFromTag("0.119.0"));
    }
}
