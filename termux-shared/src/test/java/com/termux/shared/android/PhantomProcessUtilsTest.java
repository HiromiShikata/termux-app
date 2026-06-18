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

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class PhantomProcessUtilsTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void featureFlagAndSettingsKeysMatchAndroidConstants() {
        Assert.assertEquals("settings_enable_monitor_phantom_procs",
            PhantomProcessUtils.FEATURE_FLAG_SETTINGS_ENABLE_MONITOR_PHANTOM_PROCS);
        Assert.assertEquals("max_phantom_processes",
            PhantomProcessUtils.KEY_MAX_PHANTOM_PROCESSES);
        Assert.assertEquals("device_config_sync_disabled",
            PhantomProcessUtils.SETTINGS_GLOBAL_DEVICE_CONFIG_SYNC_DISABLED);
    }

    @Test
    public void getFeatureFlagMonitorPhantomProcsValueStringNeverReturnsNull() {
        FeatureFlagValue value = PhantomProcessUtils.getFeatureFlagMonitorPhantomProcsValueString(context());
        Assert.assertNotNull(value);
        Assert.assertTrue(Arrays.asList(FeatureFlagValue.values()).contains(value));
    }

    @Test
    public void getActivityManagerMaxPhantomProcessesReturnsNullWithoutDumpPermission() {
        Assert.assertNull(PhantomProcessUtils.getActivityManagerMaxPhantomProcesses(context()));
    }

    @Test
    public void getSettingsGlobalDeviceConfigSyncDisabledReturnsNullWhenUnset() {
        Assert.assertNull(PhantomProcessUtils.getSettingsGlobalDeviceConfigSyncDisabled(context()));
    }
}
