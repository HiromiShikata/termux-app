package com.termux.app.terminal;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SessionSwitchPickerControllerRenderTest {

    private static final int ACTIVE_INDICATOR_COLOR = 0xFF3A6F8F;
    private static final int INDICATOR_BAR_WIDTH_PIXELS = 3;

    private static Context context() {
        return RuntimeEnvironment.getApplication();
    }

    private static SpannableStringBuilder renderSessionLine(SessionPickerOverlayLine line) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        SessionSwitchPickerController.appendSessionLine(context(), builder, line, 0,
            ACTIVE_INDICATOR_COLOR, 0, INDICATOR_BAR_WIDTH_PIXELS, 0, 0);
        return builder;
    }

    @Test
    public void markedSessionMapsToTheRealBellDrawableAndUnmarkedToTheTransparentPlaceholder() {
        Assert.assertEquals(R.drawable.ic_session_bell_notification,
            SessionSwitchPickerController.bellMarkDrawableRes(true));
        Assert.assertEquals(R.drawable.ic_session_bell_notification_placeholder,
            SessionSwitchPickerController.bellMarkDrawableRes(false));
    }

    @Test
    public void pickerAndBottomSheetAgreeOnTheBellDrawableForBothStates() {
        Assert.assertEquals(
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(true),
            SessionSwitchPickerController.bellMarkDrawableRes(true));
        Assert.assertEquals(
            TermuxSessionsListViewController.newActivityIndicatorDrawableRes(false),
            SessionSwitchPickerController.bellMarkDrawableRes(false));
    }

    @Test
    public void markedSessionLineRendersTheBellAsAnImageSpanWithoutTheColorEmojiGlyph() {
        SessionPickerOverlayLine markedLine = new SessionPickerOverlayLine(
            SessionPickerOverlayLine.Kind.SESSION, "background", "", false, false, true, "4s ago");

        SpannableStringBuilder builder = renderSessionLine(markedLine);

        ImageSpan[] imageSpans = builder.getSpans(0, builder.length(), ImageSpan.class);
        Assert.assertEquals(1, imageSpans.length);
        Assert.assertFalse(builder.toString().contains(SessionRow.BELL_MARK.trim()));
    }

    @Test
    public void unmarkedSessionLineStillReservesTheBellSlotAsAnImageSpanToPreserveAlignment() {
        SessionPickerOverlayLine unmarkedLine = new SessionPickerOverlayLine(
            SessionPickerOverlayLine.Kind.SESSION, "current", "", false, false, false, "");

        SpannableStringBuilder builder = renderSessionLine(unmarkedLine);

        ImageSpan[] imageSpans = builder.getSpans(0, builder.length(), ImageSpan.class);
        Assert.assertEquals(1, imageSpans.length);
        Assert.assertFalse(builder.toString().contains(SessionRow.BELL_MARK.trim()));
    }

    @Test
    public void currentSessionLineGetsTheBlueLeadingIndicatorBar() {
        SessionPickerOverlayLine currentLine = new SessionPickerOverlayLine(
            SessionPickerOverlayLine.Kind.SESSION, "active", "", false, true, false, "");

        SpannableStringBuilder builder = renderSessionLine(currentLine);

        SessionSwitchPickerController.CurrentSessionIndicatorSpan[] indicatorSpans =
            builder.getSpans(0, builder.length(),
                SessionSwitchPickerController.CurrentSessionIndicatorSpan.class);
        Assert.assertEquals(1, indicatorSpans.length);
    }

    @Test
    public void nonCurrentSessionLineHasNoBlueLeadingIndicatorBar() {
        SessionPickerOverlayLine backgroundLine = new SessionPickerOverlayLine(
            SessionPickerOverlayLine.Kind.SESSION, "background", "", false, false, true, "4s ago");

        SpannableStringBuilder builder = renderSessionLine(backgroundLine);

        SessionSwitchPickerController.CurrentSessionIndicatorSpan[] indicatorSpans =
            builder.getSpans(0, builder.length(),
                SessionSwitchPickerController.CurrentSessionIndicatorSpan.class);
        Assert.assertEquals(0, indicatorSpans.length);
    }
}
