package com.termux.shared.termux.crash;

import com.termux.shared.data.DataUtils;

import org.junit.Assert;
import org.junit.Test;

public class TermuxCrashUtilsCopyActionSizeGuardTest {

    @Test
    public void includesCopyActionWhenReportUnderTransactionLimit() {
        String report = repeat('a', DataUtils.TRANSACTION_SIZE_LIMIT_IN_BYTES - 1);
        Assert.assertTrue(TermuxCrashUtils.shouldIncludeNotificationCopyAction(report));
    }

    @Test
    public void includesCopyActionWhenReportExactlyAtTransactionLimit() {
        String report = repeat('a', DataUtils.TRANSACTION_SIZE_LIMIT_IN_BYTES);
        Assert.assertTrue(TermuxCrashUtils.shouldIncludeNotificationCopyAction(report));
    }

    @Test
    public void omitsCopyActionWhenReportOverTransactionLimit() {
        String report = repeat('a', DataUtils.TRANSACTION_SIZE_LIMIT_IN_BYTES + 1);
        Assert.assertFalse(TermuxCrashUtils.shouldIncludeNotificationCopyAction(report));
    }

    @Test
    public void omitsCopyActionForNullOrEmptyReport() {
        Assert.assertFalse(TermuxCrashUtils.shouldIncludeNotificationCopyAction(null));
        Assert.assertFalse(TermuxCrashUtils.shouldIncludeNotificationCopyAction(""));
    }

    private static String repeat(char character, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int index = 0; index < count; index++) {
            builder.append(character);
        }
        return builder.toString();
    }
}
