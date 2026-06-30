package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public class BrowserSessionSplitRatiosTest {

    private static final float DELTA = 1e-6f;

    @Test
    public void returnsNullForASessionThatHasNoStoredRatio() {
        BrowserSessionSplitRatios ratios = new BrowserSessionSplitRatios();
        Assert.assertNull(ratios.getRatio("sessionA"));
    }

    @Test
    public void resolvesToTheDefaultForASessionThatHasNoStoredRatio() {
        BrowserSessionSplitRatios ratios = new BrowserSessionSplitRatios();
        Assert.assertEquals(BrowserSplitRatio.DEFAULT, ratios.resolveRatioToApply("brandNewSession"), DELTA);
    }

    @Test
    public void storesAndReturnsARatioForASession() {
        BrowserSessionSplitRatios ratios = new BrowserSessionSplitRatios();
        ratios.setRatio("sessionA", 0.4f);
        Assert.assertEquals(0.4f, ratios.getRatio("sessionA"), DELTA);
        Assert.assertEquals(0.4f, ratios.resolveRatioToApply("sessionA"), DELTA);
    }

    @Test
    public void keepsEachSessionRatioIndependentWhenSwitchingBetweenSessions() {
        BrowserSessionSplitRatios ratios = new BrowserSessionSplitRatios();
        ratios.setRatio("sessionA", 0.4f);
        Assert.assertEquals(BrowserSplitRatio.DEFAULT, ratios.resolveRatioToApply("sessionB"), DELTA);
        ratios.setRatio("sessionB", 0.8f);
        Assert.assertEquals(0.8f, ratios.resolveRatioToApply("sessionB"), DELTA);
        Assert.assertEquals(0.4f, ratios.resolveRatioToApply("sessionA"), DELTA);
    }

    @Test
    public void clampsAnOutOfRangeRatioBeforeStoringIt() {
        BrowserSessionSplitRatios ratios = new BrowserSessionSplitRatios();
        ratios.setRatio("sessionA", 1.5f);
        Assert.assertEquals(BrowserSplitRatio.MAX, ratios.getRatio("sessionA"), DELTA);
    }

    @Test
    public void doesNotStoreACollapsedRatioSoTheSessionFallsBackToTheDefault() {
        BrowserSessionSplitRatios ratios = new BrowserSessionSplitRatios();
        ratios.setRatio("sessionA", BrowserSplitRatio.COLLAPSE_THRESHOLD / 2f);
        Assert.assertNull(ratios.getRatio("sessionA"));
        Assert.assertEquals(BrowserSplitRatio.DEFAULT, ratios.resolveRatioToApply("sessionA"), DELTA);
    }

    @Test
    public void ignoresANullSessionName() {
        BrowserSessionSplitRatios ratios = new BrowserSessionSplitRatios();
        ratios.setRatio(null, 0.4f);
        Assert.assertNull(ratios.getRatio(null));
        Assert.assertEquals(BrowserSplitRatio.DEFAULT, ratios.resolveRatioToApply(null), DELTA);
    }

    @Test
    public void ignoresAnEmptySessionName() {
        BrowserSessionSplitRatios ratios = new BrowserSessionSplitRatios();
        ratios.setRatio("", 0.4f);
        Assert.assertNull(ratios.getRatio(""));
        Assert.assertEquals(BrowserSplitRatio.DEFAULT, ratios.resolveRatioToApply(""), DELTA);
    }

    @Test
    public void removesAStoredRatioSoTheSessionFallsBackToTheDefault() {
        BrowserSessionSplitRatios ratios = new BrowserSessionSplitRatios();
        ratios.setRatio("sessionA", 0.4f);
        ratios.removeSession("sessionA");
        Assert.assertNull(ratios.getRatio("sessionA"));
        Assert.assertEquals(BrowserSplitRatio.DEFAULT, ratios.resolveRatioToApply("sessionA"), DELTA);
    }

    @Test
    public void replaceAllReplacesTheWholeStoreAndClampsValues() {
        BrowserSessionSplitRatios ratios = new BrowserSessionSplitRatios();
        ratios.setRatio("stale", 0.3f);
        Map<String, Float> replacement = new LinkedHashMap<>();
        replacement.put("sessionA", 0.4f);
        replacement.put("sessionB", 1.5f);
        ratios.replaceAll(replacement);
        Assert.assertNull(ratios.getRatio("stale"));
        Assert.assertEquals(0.4f, ratios.getRatio("sessionA"), DELTA);
        Assert.assertEquals(BrowserSplitRatio.MAX, ratios.getRatio("sessionB"), DELTA);
    }

    @Test
    public void asMapReturnsACopyThatDoesNotMutateTheStore() {
        BrowserSessionSplitRatios ratios = new BrowserSessionSplitRatios();
        ratios.setRatio("sessionA", 0.4f);
        Map<String, Float> snapshot = ratios.asMap();
        snapshot.put("sessionB", 0.8f);
        Assert.assertNull(ratios.getRatio("sessionB"));
    }
}
