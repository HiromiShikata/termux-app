package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallBodyDisplayTextTest {

    @Test
    public void dropsTheLeadingLineThatCarriesOnlyTheOwnerCallMarker() {
        OwnerCallBodyDisplayText displayText = OwnerCallBodyDisplayText.of(
            "🔴\n\nDecide whether the previous addresses may be deleted.\n");

        Assert.assertEquals("Decide whether the previous addresses may be deleted.\n",
            displayText.getText());
    }

    @Test
    public void dropsThatLineEvenWhenItCarriesSurroundingWhitespace() {
        OwnerCallBodyDisplayText displayText = OwnerCallBodyDisplayText.of(
            "  🔴  \n\n\nThe release is waiting on your decision.");

        Assert.assertEquals("The release is waiting on your decision.", displayText.getText());
    }

    @Test
    public void keepsABodyThatDoesNotStartWithThatMarker() {
        OwnerCallBodyDisplayText displayText =
            OwnerCallBodyDisplayText.of("The release is waiting on your decision.");

        Assert.assertEquals("The release is waiting on your decision.", displayText.getText());
    }

    @Test
    public void keepsAMarkerThatCarriesTextOnTheSameLine() {
        OwnerCallBodyDisplayText displayText =
            OwnerCallBodyDisplayText.of("🔴 The release is waiting on your decision.");

        Assert.assertEquals("🔴 The release is waiting on your decision.", displayText.getText());
    }

    @Test
    public void rendersNothingWhenTheBodyIsOnlyThatMarker() {
        OwnerCallBodyDisplayText displayText = OwnerCallBodyDisplayText.of("🔴\n");

        Assert.assertEquals("", displayText.getText());
        Assert.assertTrue(displayText.getCopyableRanges().isEmpty());
    }

    @Test
    public void offersTheContentBetweenCopyMarkersAsCopyableAndHidesTheMarkers() {
        OwnerCallBodyDisplayText displayText =
            OwnerCallBodyDisplayText.of("Run <copy>termux-reload-settings</copy> to read it.");

        Assert.assertEquals("Run termux-reload-settings to read it.", displayText.getText());
        List<OwnerCallBodyRange> ranges = displayText.getCopyableRanges();
        Assert.assertEquals(1, ranges.size());
        Assert.assertEquals("termux-reload-settings",
            displayText.getText().substring(ranges.get(0).getStartIndex(),
                ranges.get(0).getEndIndex()));
        Assert.assertEquals("termux-reload-settings", ranges.get(0).getText());
    }

    @Test
    public void stripsTheNewlinesImmediatelyInsideTheCopyMarkers() {
        OwnerCallBodyDisplayText displayText = OwnerCallBodyDisplayText.of(
            "Paste this:\n<copy>\nfirst line\nsecond line\n</copy>\nthen continue.");

        Assert.assertEquals("Paste this:\nfirst line\nsecond line\nthen continue.",
            displayText.getText());
        Assert.assertEquals("first line\nsecond line",
            displayText.getCopyableRanges().get(0).getText());
    }

    @Test
    public void offersEveryCopyMarkerPairInTheBody() {
        OwnerCallBodyDisplayText displayText =
            OwnerCallBodyDisplayText.of("<copy>first</copy> and <copy>second</copy>");

        List<OwnerCallBodyRange> ranges = displayText.getCopyableRanges();
        Assert.assertEquals(2, ranges.size());
        Assert.assertEquals("first", ranges.get(0).getText());
        Assert.assertEquals("second", ranges.get(1).getText());
        Assert.assertEquals("first and second", displayText.getText());
    }

    @Test
    public void leavesAnUnclosedCopyMarkerAsWrittenSoNothingIsSilentlySwallowed() {
        OwnerCallBodyDisplayText displayText =
            OwnerCallBodyDisplayText.of("Run <copy>termux-reload-settings to read it.");

        Assert.assertEquals("Run <copy>termux-reload-settings to read it.", displayText.getText());
        Assert.assertTrue(displayText.getCopyableRanges().isEmpty());
    }

    @Test
    public void dropsTheMarkerLineAndOffersACopyRangeInTheSameBody() {
        OwnerCallBodyDisplayText displayText = OwnerCallBodyDisplayText.of(
            "🔴\n\nRun <copy>termux-reload-settings</copy> to read it.");

        Assert.assertEquals("Run termux-reload-settings to read it.", displayText.getText());
        Assert.assertEquals("termux-reload-settings",
            displayText.getCopyableRanges().get(0).getText());
    }

    @Test
    public void offersEveryUrlInTheBodySoItCanCarryTheSameActionsAsTheTerminal() {
        OwnerCallBodyDisplayText displayText = OwnerCallBodyDisplayText.of(
            "Read https://github.com/HiromiShikata/termux-app/issues/1926 "
                + "and https://github.com/HiromiShikata/termux-app/pull/1925 first.");

        List<OwnerCallBodyRange> urlRanges = displayText.getUrlRanges();
        Assert.assertEquals(2, urlRanges.size());
        Assert.assertEquals("https://github.com/HiromiShikata/termux-app/issues/1926",
            urlRanges.get(0).getText());
        Assert.assertEquals("https://github.com/HiromiShikata/termux-app/pull/1925",
            urlRanges.get(1).getText());
        Assert.assertEquals(urlRanges.get(0).getText(),
            displayText.getText().substring(urlRanges.get(0).getStartIndex(),
                urlRanges.get(0).getEndIndex()));
    }

    @Test
    public void reportsUrlPositionsAgainstTheDisplayedTextRatherThanTheRawBody() {
        OwnerCallBodyDisplayText displayText = OwnerCallBodyDisplayText.of(
            "🔴\n\n<copy>gh pr view</copy> then open "
                + "https://github.com/HiromiShikata/termux-app/pull/1925");

        OwnerCallBodyRange urlRange = displayText.getUrlRanges().get(0);
        Assert.assertEquals("https://github.com/HiromiShikata/termux-app/pull/1925",
            displayText.getText().substring(urlRange.getStartIndex(), urlRange.getEndIndex()));
    }
}
