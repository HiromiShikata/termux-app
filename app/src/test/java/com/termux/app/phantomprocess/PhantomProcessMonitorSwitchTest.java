package com.termux.app.phantomprocess;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.android.PhantomProcessUtils;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class PhantomProcessMonitorSwitchTest {

    private static final class RecordingGlobalSettingStore
        implements PhantomProcessMonitorSwitch.GlobalSettingStore {

        private final Map<String, String> mValues = new HashMap<>();

        @Nullable
        private final String mRefusalMessage;

        @Nullable
        private final String mValueTheSystemKeeps;

        RecordingGlobalSettingStore(@Nullable String refusalMessage, @Nullable String valueTheSystemKeeps) {
            mRefusalMessage = refusalMessage;
            mValueTheSystemKeeps = valueTheSystemKeeps;
        }

        @Override
        public void putString(@NonNull String key, @NonNull String value) {
            if (mRefusalMessage != null) {
                throw new SecurityException(mRefusalMessage);
            }
            mValues.put(key, mValueTheSystemKeeps == null ? value : mValueTheSystemKeeps);
        }

        @Override
        @Nullable
        public String getString(@NonNull String key) {
            return mValues.get(key);
        }
    }

    @Test
    public void switchingTheMonitorOffWritesTheFlagAndReportsTheValueTheSystemKept() {
        RecordingGlobalSettingStore store = new RecordingGlobalSettingStore(null, null);

        PhantomProcessMonitorSwitchResult result = new PhantomProcessMonitorSwitch(store).switchTheMonitorOff();

        Assert.assertEquals("the monitor is switched off by writing the same flag the report reads back,"
                + " so writing any other key would leave the report and the switch disagreeing",
            PhantomProcessMonitorSwitch.MONITOR_OFF_VALUE,
            store.getString(PhantomProcessUtils.FEATURE_FLAG_SETTINGS_ENABLE_MONITOR_PHANTOM_PROCS));
        Assert.assertTrue("the owner is told the monitor is off only when the value read back after the"
            + " write says so", result.getMonitorIsNowOff());
    }

    @Test
    public void aSystemThatRefusesTheWriteIsReportedAsRefusedRatherThanAsSuccess() {
        PhantomProcessMonitorSwitchResult result = new PhantomProcessMonitorSwitch(
            new RecordingGlobalSettingStore("Permission denial: WRITE_SECURE_SETTINGS", null))
            .switchTheMonitorOff();

        Assert.assertFalse("a refused write leaves the monitor running, and reporting it as done would"
            + " send the owner away believing the churn was fixed", result.getMonitorIsNowOff());
        Assert.assertEquals("the reason Android gave is the only thing that tells the owner what to grant",
            "Permission denial: WRITE_SECURE_SETTINGS", result.getRefusalMessage());
        Assert.assertTrue("the message shown to the owner has to carry the refusal rather than hide it,"
                + " actual message: " + result.describeForTheOwner(),
            result.describeForTheOwner().contains("Permission denial: WRITE_SECURE_SETTINGS"));
    }

    @Test
    public void aWriteAcceptedButNotKeptByTheSystemIsReportedAsTheMonitorStillBeingOn() {
        PhantomProcessMonitorSwitchResult result = new PhantomProcessMonitorSwitch(
            new RecordingGlobalSettingStore(null, "true")).switchTheMonitorOff();

        Assert.assertFalse("an accepted write whose value does not survive still leaves the monitor"
            + " killing shells, so it must not be reported as off", result.getMonitorIsNowOff());
        Assert.assertEquals("the value that actually survived is the evidence the owner needs",
            "true", result.getValueReadBackAfterTheWrite());
        Assert.assertTrue("the message has to name the value that survived, actual message: "
            + result.describeForTheOwner(), result.describeForTheOwner().contains("true"));
    }
}
