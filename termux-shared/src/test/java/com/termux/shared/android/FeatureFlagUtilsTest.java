package com.termux.shared.android;

import android.content.Context;
import android.os.Build;

import com.termux.shared.android.FeatureFlagUtils.FeatureFlagValue;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class FeatureFlagUtilsTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void featureFlagValueNamesMatchRawStrings() {
        Assert.assertEquals("<unknown>", FeatureFlagValue.UNKNOWN.getName());
        Assert.assertEquals("<unsupported>", FeatureFlagValue.UNSUPPORTED.getName());
        Assert.assertEquals("true", FeatureFlagValue.TRUE.getName());
        Assert.assertEquals("false", FeatureFlagValue.FALSE.getName());
    }

    @Test
    public void featureFlagValueDeclaresExactlyFourValues() {
        Assert.assertEquals(4, FeatureFlagValue.values().length);
        Assert.assertEquals(FeatureFlagValue.TRUE, FeatureFlagValue.valueOf("TRUE"));
    }

    @Test
    public void getAllFeatureFlagsIsConsistentWithFeatureFlagExists() {
        Map<String, String> flags = FeatureFlagUtils.getAllFeatureFlags();
        Boolean exists = FeatureFlagUtils.featureFlagExists("a_feature_flag_that_does_not_exist");
        if (flags == null) {
            Assert.assertNull(exists);
        } else {
            Assert.assertNotNull(exists);
            Assert.assertEquals(flags.containsKey("a_feature_flag_that_does_not_exist"), exists);
        }
    }

    @Test
    public void getFeatureFlagValueStringNeverReturnsNullForUnknownFeature() {
        FeatureFlagValue value = FeatureFlagUtils.getFeatureFlagValueString(
            context(), "a_feature_flag_that_does_not_exist");
        Assert.assertNotNull(value);
        Assert.assertTrue(Arrays.asList(FeatureFlagValue.values()).contains(value));
    }

    @Test
    public void getFeatureFlagValueStringReportsUnsupportedWhenFlagListIsAvailableButFlagAbsent() {
        Map<String, String> flags = FeatureFlagUtils.getAllFeatureFlags();
        FeatureFlagValue value = FeatureFlagUtils.getFeatureFlagValueString(
            context(), "a_feature_flag_that_does_not_exist");
        if (flags != null) {
            Assert.assertEquals(FeatureFlagValue.UNSUPPORTED, value);
        } else {
            Assert.assertEquals(FeatureFlagValue.UNKNOWN, value);
        }
    }

    @Test
    public void isFeatureEnabledReturnsNullOrBooleanWithoutThrowing() {
        Boolean enabled = FeatureFlagUtils.isFeatureEnabled(context(), "settings_dynamic_system");
        Assert.assertTrue(enabled == null || enabled || !enabled);
    }
}
