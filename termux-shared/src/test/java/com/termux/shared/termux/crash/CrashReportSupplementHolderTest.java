package com.termux.shared.termux.crash;

import android.content.Context;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CrashReportSupplementHolderTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @After
    public void tearDown() {
        CrashReportSupplementHolder.set(null);
    }

    @Test
    public void returnsNullWhenNoSupplementRegistered() {
        CrashReportSupplementHolder.set(null);
        Assert.assertNull(CrashReportSupplementHolder.buildSupplementSection(context()));
    }

    @Test
    public void returnsRegisteredSupplementSection() {
        CrashReportSupplementHolder.set(ctx -> "## Session diagnostics\n\nrecent events");
        Assert.assertEquals("## Session diagnostics\n\nrecent events",
            CrashReportSupplementHolder.buildSupplementSection(context()));
    }

    @Test
    public void throwingSupplementIsSwallowedSoCrashReportIsStillProduced() {
        CrashReportSupplementHolder.set(ctx -> {
            throw new IllegalStateException("boom");
        });
        Assert.assertNull(CrashReportSupplementHolder.buildSupplementSection(context()));
    }
}
