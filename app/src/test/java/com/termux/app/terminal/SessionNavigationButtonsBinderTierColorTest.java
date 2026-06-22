package com.termux.app.terminal;

import android.widget.ImageButton;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class SessionNavigationButtonsBinderTierColorTest {

    private static final int RED = 0xFFE53935;
    private static final int YELLOW = 0xFFFFB300;
    private static final int DEFAULT = 0xFFFFFFFF;

    private static Map<Integer, SessionNewActivityTier> tiers(Object... pairs) {
        Map<Integer, SessionNewActivityTier> tiersByIndex = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            tiersByIndex.put((Integer) pairs[i], (SessionNewActivityTier) pairs[i + 1]);
        }
        return tiersByIndex;
    }

    private static int colorOf(ImageButton button) {
        android.graphics.ColorFilter colorFilter = button.getColorFilter();
        Assert.assertTrue("expected a PorterDuffColorFilter",
            colorFilter instanceof android.graphics.PorterDuffColorFilter);
        return org.robolectric.Shadows.shadowOf(
            (android.graphics.PorterDuffColorFilter) colorFilter).getColor();
    }

    @Test
    public void redTierTintsBothArrowsRedWhenRedSessionsExistAboveAndBelow() {
        ImageButton previous = new ImageButton(RuntimeEnvironment.getApplication());
        ImageButton next = new ImageButton(RuntimeEnvironment.getApplication());
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2), 1,
            tiers(0, SessionNewActivityTier.RED, 2, SessionNewActivityTier.RED));

        SessionNavigationButtonsBinder.applyDirectionTier(previous, next, direction, RED, YELLOW, DEFAULT);

        Assert.assertEquals(RED, colorOf(previous));
        Assert.assertEquals(RED, colorOf(next));
    }

    @Test
    public void redTierNeverShowsYellowEvenWhenYellowSessionExists() {
        ImageButton previous = new ImageButton(RuntimeEnvironment.getApplication());
        ImageButton next = new ImageButton(RuntimeEnvironment.getApplication());
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2), 1,
            tiers(0, SessionNewActivityTier.YELLOW, 2, SessionNewActivityTier.RED));

        SessionNavigationButtonsBinder.applyDirectionTier(previous, next, direction, RED, YELLOW, DEFAULT);

        Assert.assertEquals(DEFAULT, colorOf(previous));
        Assert.assertEquals(RED, colorOf(next));
        Assert.assertNotEquals(YELLOW, colorOf(previous));
        Assert.assertNotEquals(YELLOW, colorOf(next));
    }

    @Test
    public void yellowTierTintsArrowsYellowWhenNoRedExists() {
        ImageButton previous = new ImageButton(RuntimeEnvironment.getApplication());
        ImageButton next = new ImageButton(RuntimeEnvironment.getApplication());
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2), 1,
            tiers(0, SessionNewActivityTier.YELLOW));

        SessionNavigationButtonsBinder.applyDirectionTier(previous, next, direction, RED, YELLOW, DEFAULT);

        Assert.assertEquals(YELLOW, colorOf(previous));
        Assert.assertEquals(DEFAULT, colorOf(next));
    }

    @Test
    public void noActivityLeavesBothArrowsDefault() {
        ImageButton previous = new ImageButton(RuntimeEnvironment.getApplication());
        ImageButton next = new ImageButton(RuntimeEnvironment.getApplication());
        SessionActivityDirection direction = SessionActivityDirection.compute(
            Arrays.asList(0, 1, 2), 1, tiers());

        SessionNavigationButtonsBinder.applyDirectionTier(previous, next, direction, RED, YELLOW, DEFAULT);

        Assert.assertEquals(DEFAULT, colorOf(previous));
        Assert.assertEquals(DEFAULT, colorOf(next));
    }

    @Test
    public void tierColorMappingResolvesEachTier() {
        Assert.assertEquals(RED,
            SessionNavigationButtonsBinder.tierColor(SessionNewActivityTier.RED, RED, YELLOW, DEFAULT));
        Assert.assertEquals(YELLOW,
            SessionNavigationButtonsBinder.tierColor(SessionNewActivityTier.YELLOW, RED, YELLOW, DEFAULT));
        Assert.assertEquals(DEFAULT,
            SessionNavigationButtonsBinder.tierColor(SessionNewActivityTier.NONE, RED, YELLOW, DEFAULT));
    }
}
