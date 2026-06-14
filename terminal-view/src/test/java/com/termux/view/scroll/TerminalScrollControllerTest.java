package com.termux.view.scroll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TerminalScrollControllerTest {

    private static final int THUMB_WIDTH = 12;
    private static final int TRACK_PADDING = 4;
    private static final int MIN_THUMB_HEIGHT = 40;
    private static final int VIEW_HEIGHT = 1000;
    private static final int VIEW_WIDTH = 600;
    private static final int VISIBLE_ROWS = 24;

    private TerminalScrollController controller() {
        return new TerminalScrollController(THUMB_WIDTH, TRACK_PADDING, MIN_THUMB_HEIGHT);
    }

    @Test
    public void isScrollableOnlyWhenTranscriptRowsPositive() {
        TerminalScrollController controller = controller();
        assertFalse(controller.isScrollable(0));
        assertTrue(controller.isScrollable(1));
        assertTrue(controller.isScrollable(500));
    }

    @Test
    public void thumbLeftIsInsetFromRightEdge() {
        TerminalScrollController controller = controller();
        assertEquals(VIEW_WIDTH - THUMB_WIDTH - TRACK_PADDING, controller.getThumbLeftPx(VIEW_WIDTH));
    }

    @Test
    public void thumbHeightReflectsVisibleToTotalRatio() {
        TerminalScrollController controller = controller();
        int totalRows = VISIBLE_ROWS + 24;
        int trackHeight = (VIEW_HEIGHT - TRACK_PADDING) - TRACK_PADDING;
        int expected = trackHeight * VISIBLE_ROWS / totalRows;
        assertEquals(expected, controller.getThumbHeightPx(VIEW_HEIGHT, VISIBLE_ROWS, totalRows));
    }

    @Test
    public void thumbHeightIsClampedToMinimumForLargeTranscript() {
        TerminalScrollController controller = controller();
        int totalRows = VISIBLE_ROWS + 100000;
        assertEquals(MIN_THUMB_HEIGHT, controller.getThumbHeightPx(VIEW_HEIGHT, VISIBLE_ROWS, totalRows));
    }

    @Test
    public void thumbIsAtBottomWhenAtLivePosition() {
        TerminalScrollController controller = controller();
        int transcriptRows = 100;
        int totalRows = VISIBLE_ROWS + transcriptRows;
        int thumbTop = controller.getThumbTopPx(VIEW_HEIGHT, VISIBLE_ROWS, totalRows, 0, transcriptRows);
        int thumbBottom = thumbTop + controller.getThumbHeightPx(VIEW_HEIGHT, VISIBLE_ROWS, totalRows);
        assertEquals(controller.getTrackBottomPx(VIEW_HEIGHT), thumbBottom);
    }

    @Test
    public void thumbIsAtTopWhenAtOldestScrollback() {
        TerminalScrollController controller = controller();
        int transcriptRows = 100;
        int totalRows = VISIBLE_ROWS + transcriptRows;
        int thumbTop = controller.getThumbTopPx(VIEW_HEIGHT, VISIBLE_ROWS, totalRows, -transcriptRows, transcriptRows);
        assertEquals(controller.getTrackTopPx(), thumbTop);
    }

    @Test
    public void draggingThumbToTopMapsToOldestScrollback() {
        TerminalScrollController controller = controller();
        int transcriptRows = 100;
        int totalRows = VISIBLE_ROWS + transcriptRows;
        int thumbHeight = controller.getThumbHeightPx(VIEW_HEIGHT, VISIBLE_ROWS, totalRows);
        float thumbCenterAtTop = controller.getTrackTopPx() + thumbHeight / 2f;
        int topRow = controller.topRowForThumbCenter(VIEW_HEIGHT, VISIBLE_ROWS, totalRows, thumbCenterAtTop, transcriptRows);
        assertEquals(-transcriptRows, topRow);
    }

    @Test
    public void draggingThumbToBottomMapsToLivePosition() {
        TerminalScrollController controller = controller();
        int transcriptRows = 100;
        int totalRows = VISIBLE_ROWS + transcriptRows;
        int thumbHeight = controller.getThumbHeightPx(VIEW_HEIGHT, VISIBLE_ROWS, totalRows);
        float thumbCenterAtBottom = controller.getTrackBottomPx(VIEW_HEIGHT) - thumbHeight / 2f;
        int topRow = controller.topRowForThumbCenter(VIEW_HEIGHT, VISIBLE_ROWS, totalRows, thumbCenterAtBottom, transcriptRows);
        assertEquals(0, topRow);
    }

    @Test
    public void draggingBeyondTrackEndsIsClamped() {
        TerminalScrollController controller = controller();
        int transcriptRows = 100;
        int totalRows = VISIBLE_ROWS + transcriptRows;
        int aboveTrack = controller.topRowForThumbCenter(VIEW_HEIGHT, VISIBLE_ROWS, totalRows, -500f, transcriptRows);
        int belowTrack = controller.topRowForThumbCenter(VIEW_HEIGHT, VISIBLE_ROWS, totalRows, VIEW_HEIGHT + 500f, transcriptRows);
        assertEquals(-transcriptRows, aboveTrack);
        assertEquals(0, belowTrack);
    }

    @Test
    public void roundTripMappingIsConsistentAtMidPosition() {
        TerminalScrollController controller = controller();
        int transcriptRows = 100;
        int totalRows = VISIBLE_ROWS + transcriptRows;
        int topRow = -50;
        int thumbTop = controller.getThumbTopPx(VIEW_HEIGHT, VISIBLE_ROWS, totalRows, topRow, transcriptRows);
        int thumbHeight = controller.getThumbHeightPx(VIEW_HEIGHT, VISIBLE_ROWS, totalRows);
        float thumbCenter = thumbTop + thumbHeight / 2f;
        int mappedBack = controller.topRowForThumbCenter(VIEW_HEIGHT, VISIBLE_ROWS, totalRows, thumbCenter, transcriptRows);
        assertTrue(Math.abs(mappedBack - topRow) <= 1);
    }

    @Test
    public void grabAreaMatchesThumbWithExtraPadding() {
        TerminalScrollController controller = controller();
        int transcriptRows = 100;
        int totalRows = VISIBLE_ROWS + transcriptRows;
        int topRow = -50;
        int grabExtra = 16;
        int thumbLeft = controller.getThumbLeftPx(VIEW_WIDTH);
        int thumbTop = controller.getThumbTopPx(VIEW_HEIGHT, VISIBLE_ROWS, totalRows, topRow, transcriptRows);
        int thumbHeight = controller.getThumbHeightPx(VIEW_HEIGHT, VISIBLE_ROWS, totalRows);
        float thumbCenterY = thumbTop + thumbHeight / 2f;
        assertTrue(controller.isWithinThumbGrabArea(VIEW_WIDTH, VIEW_HEIGHT, VISIBLE_ROWS, totalRows, topRow, transcriptRows, thumbLeft, thumbCenterY, grabExtra));
        assertFalse(controller.isWithinThumbGrabArea(VIEW_WIDTH, VIEW_HEIGHT, VISIBLE_ROWS, totalRows, topRow, transcriptRows, thumbLeft - grabExtra - 5, thumbCenterY, grabExtra));
    }

    @Test
    public void grabAreaIsEmptyWhenNotScrollable() {
        TerminalScrollController controller = controller();
        assertFalse(controller.isWithinThumbGrabArea(VIEW_WIDTH, VIEW_HEIGHT, VISIBLE_ROWS, VISIBLE_ROWS, 0, 0, VIEW_WIDTH - 1, VIEW_HEIGHT / 2f, 16));
    }

    @Test
    public void controlRendersWhenScrollbackExistsEvenWithoutAlternateScreen() {
        TerminalScrollController controller = controller();
        assertTrue(controller.shouldRenderControl(100, false));
    }

    @Test
    public void controlRendersOnAlternateScreenWithoutScrollback() {
        TerminalScrollController controller = controller();
        assertTrue(controller.shouldRenderControl(0, true));
    }

    @Test
    public void controlHiddenWhenNoScrollbackAndNotAlternateScreen() {
        TerminalScrollController controller = controller();
        assertFalse(controller.shouldRenderControl(0, false));
    }

    @Test
    public void relativeThumbIsCenteredInTrack() {
        TerminalScrollController controller = controller();
        int thumbTop = controller.getRelativeThumbTopPx(VIEW_HEIGHT);
        int thumbHeight = controller.getRelativeThumbHeightPx(VIEW_HEIGHT);
        int trackTop = controller.getTrackTopPx();
        int trackBottom = controller.getTrackBottomPx(VIEW_HEIGHT);
        int expectedCenter = (trackTop + trackBottom) / 2;
        int actualCenter = thumbTop + thumbHeight / 2;
        assertTrue(Math.abs(actualCenter - expectedCenter) <= 1);
    }

    @Test
    public void relativeThumbHeightUsesMinimumWhenTrackIsTall() {
        TerminalScrollController controller = controller();
        assertEquals(MIN_THUMB_HEIGHT, controller.getRelativeThumbHeightPx(VIEW_HEIGHT));
    }

    @Test
    public void relativeThumbHeightIsClampedToTrackWhenTrackIsShort() {
        TerminalScrollController controller = controller();
        int shortViewHeight = MIN_THUMB_HEIGHT;
        int trackHeight = controller.getTrackBottomPx(shortViewHeight) - controller.getTrackTopPx();
        assertEquals(trackHeight, controller.getRelativeThumbHeightPx(shortViewHeight));
    }

    @Test
    public void relativeGrabAreaCoversCenteredThumb() {
        TerminalScrollController controller = controller();
        int thumbLeft = controller.getThumbLeftPx(VIEW_WIDTH);
        int thumbTop = controller.getRelativeThumbTopPx(VIEW_HEIGHT);
        int thumbHeight = controller.getRelativeThumbHeightPx(VIEW_HEIGHT);
        float thumbCenterY = thumbTop + thumbHeight / 2f;
        assertTrue(controller.isWithinRelativeThumbGrabArea(VIEW_WIDTH, VIEW_HEIGHT, thumbLeft, thumbCenterY, 16));
    }

    @Test
    public void relativeGrabAreaRejectsTouchLeftOfTrack() {
        TerminalScrollController controller = controller();
        int thumbLeft = controller.getThumbLeftPx(VIEW_WIDTH);
        int thumbTop = controller.getRelativeThumbTopPx(VIEW_HEIGHT);
        int thumbHeight = controller.getRelativeThumbHeightPx(VIEW_HEIGHT);
        float thumbCenterY = thumbTop + thumbHeight / 2f;
        assertFalse(controller.isWithinRelativeThumbGrabArea(VIEW_WIDTH, VIEW_HEIGHT, thumbLeft - 16 - 5, thumbCenterY, 16));
    }

    @Test
    public void wheelStepsArePositiveWhenDraggingDown() {
        TerminalScrollController controller = controller();
        int rowHeight = 30;
        assertEquals(3, controller.wheelStepsForDragDelta(rowHeight * 3, rowHeight));
    }

    @Test
    public void wheelStepsAreNegativeWhenDraggingUp() {
        TerminalScrollController controller = controller();
        int rowHeight = 30;
        assertEquals(-2, controller.wheelStepsForDragDelta(-rowHeight * 2, rowHeight));
    }

    @Test
    public void wheelStepsAreZeroForSubRowDrag() {
        TerminalScrollController controller = controller();
        int rowHeight = 30;
        assertEquals(0, controller.wheelStepsForDragDelta(rowHeight - 1, rowHeight));
    }

    @Test
    public void wheelStepsAreZeroForNonPositiveRowHeight() {
        TerminalScrollController controller = controller();
        assertEquals(0, controller.wheelStepsForDragDelta(100f, 0));
    }

    @Test
    public void scrollControlOnAlternateScreenEmitsMouseWheelNotArrowKeys() {
        TerminalScrollController controller = controller();
        assertEquals(TerminalScrollEvent.MOUSE_WHEEL,
            controller.scrollEventType(false, true, true));
    }

    @Test
    public void mouseTrackingAlwaysEmitsMouseWheel() {
        TerminalScrollController controller = controller();
        assertEquals(TerminalScrollEvent.MOUSE_WHEEL,
            controller.scrollEventType(true, true, true));
        assertEquals(TerminalScrollEvent.MOUSE_WHEEL,
            controller.scrollEventType(true, true, false));
        assertEquals(TerminalScrollEvent.MOUSE_WHEEL,
            controller.scrollEventType(true, false, false));
    }

    @Test
    public void fingerSwipeOnAlternateScreenStillEmitsArrowKeys() {
        TerminalScrollController controller = controller();
        assertEquals(TerminalScrollEvent.ARROW_KEY,
            controller.scrollEventType(false, true, false));
    }

    @Test
    public void scrollWithoutAlternateScreenEmitsLocalScrollback() {
        TerminalScrollController controller = controller();
        assertEquals(TerminalScrollEvent.LOCAL_SCROLLBACK,
            controller.scrollEventType(false, false, false));
        assertEquals(TerminalScrollEvent.LOCAL_SCROLLBACK,
            controller.scrollEventType(false, false, true));
    }
}
