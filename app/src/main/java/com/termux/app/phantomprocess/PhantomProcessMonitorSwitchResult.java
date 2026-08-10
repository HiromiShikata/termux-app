package com.termux.app.phantomprocess;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class PhantomProcessMonitorSwitchResult {

    @Nullable
    private final String mValueReadBackAfterTheWrite;

    @Nullable
    private final String mRefusalMessage;

    private PhantomProcessMonitorSwitchResult(@Nullable String valueReadBackAfterTheWrite,
                                             @Nullable String refusalMessage) {
        mValueReadBackAfterTheWrite = valueReadBackAfterTheWrite;
        mRefusalMessage = refusalMessage;
    }

    @NonNull
    public static PhantomProcessMonitorSwitchResult writtenAndReadBackAs(@Nullable String valueReadBackAfterTheWrite) {
        return new PhantomProcessMonitorSwitchResult(valueReadBackAfterTheWrite, null);
    }

    @NonNull
    public static PhantomProcessMonitorSwitchResult refusedBySystem(@NonNull String refusalMessage) {
        return new PhantomProcessMonitorSwitchResult(null, refusalMessage);
    }

    public boolean getMonitorIsNowOff() {
        return PhantomProcessMonitorSwitch.MONITOR_OFF_VALUE.equals(mValueReadBackAfterTheWrite);
    }

    @Nullable
    public String getValueReadBackAfterTheWrite() {
        return mValueReadBackAfterTheWrite;
    }

    @Nullable
    public String getRefusalMessage() {
        return mRefusalMessage;
    }

    @NonNull
    public String describeForTheOwner() {
        if (mRefusalMessage != null) {
            return "Android refused the write, so the phantom process monitor is still on: " + mRefusalMessage;
        }
        if (getMonitorIsNowOff()) {
            return "The phantom process monitor is now off.";
        }
        return "The write was accepted but the value read back is "
            + (mValueReadBackAfterTheWrite == null ? "unset" : mValueReadBackAfterTheWrite)
            + ", so the monitor is still on.";
    }
}
