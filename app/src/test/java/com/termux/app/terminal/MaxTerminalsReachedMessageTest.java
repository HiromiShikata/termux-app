package com.termux.app.terminal;

import android.app.Activity;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/**
 * Verifies that the "Max sessions reached" block message surfaces the actual live session count and the
 * configured max, so a user who sees fewer visible sessions than the cap (hidden/collapsed live sessions
 * still count) understands why creation is blocked. The two integers passed at the call sites are the
 * live cap count and the configured max — the same numbers the cap enforces.
 */
@RunWith(RobolectricTestRunner.class)
public class MaxTerminalsReachedMessageTest {

    @Test
    public void messageIncludesLiveCountAndMax() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();

        int liveCount = 39;
        int max = 64;
        String message = activity.getString(R.string.msg_max_terminals_reached, liveCount, max);

        Assert.assertTrue(message.contains("39"));
        Assert.assertTrue(message.contains("64"));
        // %1$d is the live count and %2$d is the max, in that order.
        Assert.assertTrue(message.indexOf("39") < message.indexOf("64"));
    }

    @Test
    public void placeholdersAreOrderedLiveCountThenMax() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();

        // Distinct values confirm the arguments are not swapped by the format string.
        String message = activity.getString(R.string.msg_max_terminals_reached, 5, 32);

        Assert.assertTrue("live count must be the first placeholder", message.contains("5 of 32"));
    }
}
