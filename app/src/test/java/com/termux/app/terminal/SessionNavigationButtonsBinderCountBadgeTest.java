package com.termux.app.terminal;

import android.view.View;
import android.widget.TextView;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class SessionNavigationButtonsBinderCountBadgeTest {

    private static Map<Integer, SessionNewActivityTier> tiers(Object... pairs) {
        Map<Integer, SessionNewActivityTier> tiersByIndex = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            tiersByIndex.put((Integer) pairs[i], (SessionNewActivityTier) pairs[i + 1]);
        }
        return tiersByIndex;
    }

    private static TextView newBadge() {
        return new TextView(RuntimeEnvironment.getApplication());
    }

    @Test
    public void badgesShowAboveAndBelowCountsExcludingCurrentSession() {
        TextView previousBadge = newBadge();
        TextView nextBadge = newBadge();
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2, 3, 4), 2,
            tiers(0, SessionNewActivityTier.RED,
                1, SessionNewActivityTier.RED,
                3, SessionNewActivityTier.RED));

        SessionNavigationButtonsBinder.applyDirectionCountBadges(previousBadge, nextBadge, direction);

        Assert.assertEquals(View.VISIBLE, previousBadge.getVisibility());
        Assert.assertEquals("2", previousBadge.getText().toString());
        Assert.assertEquals(View.VISIBLE, nextBadge.getVisibility());
        Assert.assertEquals("1", nextBadge.getText().toString());
    }

    @Test
    public void badgeIsHiddenWhenDirectionalCountIsZero() {
        TextView previousBadge = newBadge();
        TextView nextBadge = newBadge();
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2, 3), 1,
            tiers(3, SessionNewActivityTier.RED));

        SessionNavigationButtonsBinder.applyDirectionCountBadges(previousBadge, nextBadge, direction);

        Assert.assertEquals(View.GONE, previousBadge.getVisibility());
        Assert.assertEquals("", previousBadge.getText().toString());
        Assert.assertEquals(View.VISIBLE, nextBadge.getVisibility());
        Assert.assertEquals("1", nextBadge.getText().toString());
    }

    @Test
    public void bothBadgesHiddenWhenNoCallToUserSessionExists() {
        TextView previousBadge = newBadge();
        TextView nextBadge = newBadge();
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2), 1,
            tiers(0, SessionNewActivityTier.YELLOW));

        SessionNavigationButtonsBinder.applyDirectionCountBadges(previousBadge, nextBadge, direction);

        Assert.assertEquals(View.GONE, previousBadge.getVisibility());
        Assert.assertEquals(View.GONE, nextBadge.getVisibility());
    }

    @Test
    public void currentSessionCallToUserDoesNotAppearInEitherBadge() {
        TextView previousBadge = newBadge();
        TextView nextBadge = newBadge();
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2), 1,
            tiers(1, SessionNewActivityTier.RED));

        SessionNavigationButtonsBinder.applyDirectionCountBadges(previousBadge, nextBadge, direction);

        Assert.assertEquals(View.GONE, previousBadge.getVisibility());
        Assert.assertEquals(View.GONE, nextBadge.getVisibility());
    }

    @Test
    public void rehidingABadgeClearsItsPreviousCountText() {
        TextView previousBadge = newBadge();
        TextView nextBadge = newBadge();
        SessionActivityDirection withRedAbove = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2), 2,
            tiers(0, SessionNewActivityTier.RED));
        SessionNavigationButtonsBinder.applyDirectionCountBadges(previousBadge, nextBadge, withRedAbove);
        Assert.assertEquals("1", previousBadge.getText().toString());

        SessionActivityDirection withoutRed = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2), 2, tiers());
        SessionNavigationButtonsBinder.applyDirectionCountBadges(previousBadge, nextBadge, withoutRed);

        Assert.assertEquals(View.GONE, previousBadge.getVisibility());
        Assert.assertEquals("", previousBadge.getText().toString());
    }
}
