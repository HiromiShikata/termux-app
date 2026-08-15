package com.termux.app.ownercall;

import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallBodySpannedTextTest {

    private static final class RecordedTaps
        implements OwnerCallBodySpannedText.OwnerCallBodyTapActions {

        private final List<String> copiedTexts = new ArrayList<>();
        private final List<String> openedUrls = new ArrayList<>();

        @Override
        public void onCopyableTextTapped(String text) {
            copiedTexts.add(text);
        }

        @Override
        public void onUrlTapped(String url) {
            openedUrls.add(url);
        }
    }

    private static Spanned spannedOf(String body,
                                     OwnerCallBodySpannedText.OwnerCallBodyTapActions actions) {
        return (Spanned) OwnerCallBodySpannedText.of(OwnerCallBodyDisplayText.of(body), actions);
    }

    private static ClickableSpan[] clickableSpansOf(Spanned spanned) {
        return spanned.getSpans(0, spanned.length(), ClickableSpan.class);
    }

    @Test
    public void tappingTheContentBetweenCopyMarkersCopiesIt() {
        RecordedTaps taps = new RecordedTaps();
        Spanned spanned = spannedOf("Run <copy>termux-reload-settings</copy> to read it.", taps);

        clickableSpansOf(spanned)[0].onClick(null);

        Assert.assertEquals(1, taps.copiedTexts.size());
        Assert.assertEquals("termux-reload-settings", taps.copiedTexts.get(0));
        Assert.assertTrue(taps.openedUrls.isEmpty());
    }

    @Test
    public void tappingAUrlOffersTheSameChoiceTheTerminalOffers() {
        RecordedTaps taps = new RecordedTaps();
        Spanned spanned = spannedOf(
            "Read https://github.com/HiromiShikata/termux-app/pull/1925 first.", taps);

        clickableSpansOf(spanned)[0].onClick(null);

        Assert.assertEquals(1, taps.openedUrls.size());
        Assert.assertEquals("https://github.com/HiromiShikata/termux-app/pull/1925",
            taps.openedUrls.get(0));
        Assert.assertTrue(taps.copiedTexts.isEmpty());
    }

    @Test
    public void marksEveryTappableRangeSoTheOwnerCanSeeWhatRespondsToATap() {
        Spanned spanned = spannedOf(
            "Run <copy>termux-reload-settings</copy> then open "
                + "https://github.com/HiromiShikata/termux-app/pull/1925", new RecordedTaps());

        Assert.assertEquals(2, clickableSpansOf(spanned).length);
        Assert.assertEquals(2,
            spanned.getSpans(0, spanned.length(), UnderlineSpan.class).length);
    }

    @Test
    public void spansTheCopyableTextItselfRatherThanTheMarkers() {
        Spanned spanned = spannedOf("Run <copy>termux-reload-settings</copy> to read it.",
            new RecordedTaps());
        ClickableSpan span = clickableSpansOf(spanned)[0];

        Assert.assertEquals("termux-reload-settings",
            spanned.toString().substring(spanned.getSpanStart(span), spanned.getSpanEnd(span)));
    }

    @Test
    public void rendersTheBodyWithoutTheLeadingOwnerCallMarkerLine() {
        Spanned spanned = spannedOf("🔴\n\nDecide whether the release may go out.",
            new RecordedTaps());

        Assert.assertEquals("Decide whether the release may go out.", spanned.toString());
    }

    @Test
    public void addsNoTapTargetWhenNoActionsAreWired() {
        Spanned spanned = (Spanned) OwnerCallBodySpannedText.of(
            OwnerCallBodyDisplayText.of("Run <copy>termux-reload-settings</copy> to read it."),
            null);

        Assert.assertEquals(0, clickableSpansOf(spanned).length);
        Assert.assertEquals("Run termux-reload-settings to read it.", spanned.toString());
    }
}
