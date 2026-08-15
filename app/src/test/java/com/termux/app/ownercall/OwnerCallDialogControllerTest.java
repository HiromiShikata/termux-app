package com.termux.app.ownercall;

import android.content.Context;
import android.os.SystemClock;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.termux.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallDialogControllerTest {

    private static final String FIRST_SESSION = "https://github.com/owner/repo/issues/1";
    private static final String SECOND_SESSION = "https://github.com/owner/repo/issues/2";
    private static final String REPEATED_BODY =
        "Decide whether the invoice recipient may be changed.";
    private static final OwnerCall EARLIER_CALL =
        new OwnerCall(FIRST_SESSION, "2026-08-14T08:00:00Z", REPEATED_BODY);
    private static final OwnerCall LATER_CALL =
        new OwnerCall(FIRST_SESSION, "2026-08-14T08:04:00Z", REPEATED_BODY);
    private static final OwnerCall NEWEST_CALL = new OwnerCall(FIRST_SESSION,
        "2026-08-14T08:09:00Z", "Decide whether the invoice may be reissued.");
    private static final OwnerCall OTHER_SESSION_CALL = new OwnerCall(SECOND_SESSION,
        "2026-08-14T08:02:00Z", "Decide whether the branch may be deleted.");
    private static final OwnerCall OTHER_SESSION_SECOND_CALL = new OwnerCall(SECOND_SESSION,
        "2026-08-14T08:06:00Z", "Decide which release note wording to publish.");
    private static final OwnerCall OTHER_SESSION_THIRD_CALL = new OwnerCall(SECOND_SESSION,
        "2026-08-14T08:08:00Z", "Decide whether the migration may run tonight.");
    private static final long NOW = 1_800_000_000_000L;
    private static final OwnerCallDialogGeometry GEOMETRY =
        OwnerCallDialogGeometry.resolve(2400, 0, 1080, 120, 36);

    @Test
    public void showsTheOldestWaitingCallOfTheSessionOnScreen() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);

        controller.showCallsForSession(FIRST_SESSION, NOW);

        Assert.assertEquals(View.VISIBLE, dialog(root).getVisibility());
        Assert.assertEquals("1 / 2", positionText(root));
        Assert.assertEquals(EARLIER_CALL.getBody(), bodyText(root));
    }

    @Test
    public void showsNoDialogForASessionWithNoWaitingCall() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);

        controller.showCallsForSession("https://github.com/owner/repo/issues/3", NOW);

        Assert.assertEquals(View.GONE, dialog(root).getVisibility());
    }

    @Test
    public void pagesForwardToTheLaterWaitingCallAndBackAgain() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);

        root.findViewById(R.id.owner_call_dialog_next_button).performClick();
        Assert.assertEquals("2 / 2", positionText(root));
        Assert.assertEquals(LATER_CALL.getBody(), bodyText(root));

        root.findViewById(R.id.owner_call_dialog_previous_button).performClick();
        Assert.assertEquals("1 / 2", positionText(root));
        Assert.assertEquals(EARLIER_CALL.getBody(), bodyText(root));
    }

    @Test
    public void closingTheDialogHidesTheWholeDialogNotJustOneCall() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);

        root.findViewById(R.id.owner_call_dialog_close_button).performClick();

        Assert.assertEquals(View.GONE, dialog(root).getVisibility());
    }

    @Test
    public void reopeningAfterCloseShowsAllCallsUnchanged() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        root.findViewById(R.id.owner_call_dialog_close_button).performClick();

        controller.reopenDialog(NOW);

        Assert.assertEquals(View.VISIBLE, dialog(root).getVisibility());
        Assert.assertEquals("1 / 2", positionText(root));
        Assert.assertEquals(EARLIER_CALL.getBody(), bodyText(root));
    }

    @Test
    public void switchingToAnotherSessionAndReturningShowsAllCallsAgainAfterClose() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        root.findViewById(R.id.owner_call_dialog_close_button).performClick();

        controller.showCallsForSession(SECOND_SESSION, NOW);
        controller.showCallsForSession(FIRST_SESSION, NOW);

        Assert.assertEquals(View.VISIBLE, dialog(root).getVisibility());
        Assert.assertEquals("1 / 2", positionText(root));
        Assert.assertEquals(EARLIER_CALL.getBody(), bodyText(root));
    }

    @Test
    public void callListIsUnchangedAfterCloseAndReopen() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        String positionBeforeClose = positionText(root);
        String bodyBeforeClose = bodyText(root);

        root.findViewById(R.id.owner_call_dialog_close_button).performClick();
        controller.reopenDialog(NOW);

        Assert.assertEquals(positionBeforeClose, positionText(root));
        Assert.assertEquals(bodyBeforeClose, bodyText(root));
    }

    @Test
    public void switchesToTheOldestWaitingCallOfTheNewlyDisplayedSession() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        root.findViewById(R.id.owner_call_dialog_next_button).performClick();

        controller.showCallsForSession(SECOND_SESSION, NOW);

        Assert.assertEquals("1 / 3", positionText(root));
        Assert.assertEquals(OTHER_SESSION_CALL.getBody(), bodyText(root));
    }

    @Test
    public void closingForOneSessionDoesNotAffectTheCallsOfAnotherSession() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        root.findViewById(R.id.owner_call_dialog_close_button).performClick();

        controller.showCallsForSession(SECOND_SESSION, NOW);

        Assert.assertEquals("1 / 3", positionText(root));
    }

    @Test
    public void keepsShowingTheCallTheOwnerIsReadingWhenAnEarlierCallIsAnswered() {
        View root = inflateActivityLayout();
        Map<String, List<OwnerCall>> callsBySession = new HashMap<>();
        callsBySession.put(FIRST_SESSION, Arrays.asList(EARLIER_CALL, LATER_CALL));
        OwnerCallDialogController controller = controllerFor(root, callsBySession);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        root.findViewById(R.id.owner_call_dialog_next_button).performClick();
        Assert.assertEquals("2 / 2", positionText(root));

        callsBySession.put(FIRST_SESSION, Collections.singletonList(LATER_CALL));
        controller.showCallsForSession(FIRST_SESSION, NOW);

        Assert.assertEquals("1 / 1", positionText(root));
        Assert.assertEquals(LATER_CALL.getBody(), bodyText(root));
    }

    @Test
    public void keepsShowingTheEarlierCallTheOwnerIsReadingWhenANewerCallArrives() {
        View root = inflateActivityLayout();
        Map<String, List<OwnerCall>> callsBySession = new HashMap<>();
        callsBySession.put(FIRST_SESSION, Arrays.asList(EARLIER_CALL, LATER_CALL));
        OwnerCallDialogController controller = controllerFor(root, callsBySession);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        Assert.assertEquals("1 / 2", positionText(root));

        callsBySession.put(FIRST_SESSION,
            Arrays.asList(EARLIER_CALL, LATER_CALL, NEWEST_CALL));
        controller.showCallsForSession(FIRST_SESSION, NOW);

        Assert.assertEquals("1 / 3", positionText(root));
        Assert.assertEquals(EARLIER_CALL.getBody(), bodyText(root));
    }

    @Test
    public void movesTheDialogOnBothAxesWhileItsHeaderIsDragged() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        controller.onDialogResizedBy(bottomRightCorner(), -200, 0);
        OwnerCallDialogPlacement placementBeforeTheDrag = placementOf(root);

        controller.onDialogMovedBy(120, -200);

        OwnerCallDialogPlacement placement = placementOf(root);
        Assert.assertEquals(placementBeforeTheDrag.getLeftMarginPixels() + 120,
            placement.getLeftMarginPixels());
        Assert.assertEquals(placementBeforeTheDrag.getBottomMarginPixels() + 200,
            placement.getBottomMarginPixels());
    }

    @Test
    public void resizesTheDialogWhileItsBottomRightCornerIsDragged() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        OwnerCallDialogPlacement placementBeforeTheDrag = placementOf(root);

        controller.onDialogResizedBy(bottomRightCorner(), -80, 150);

        OwnerCallDialogPlacement placement = placementOf(root);
        Assert.assertEquals(placementBeforeTheDrag.getWidthPixels() - 80,
            placement.getWidthPixels());
        Assert.assertEquals(placementBeforeTheDrag.getHeightPixels() + 150,
            placement.getHeightPixels());
        Assert.assertEquals("the top edge must stay where it was while the bottom edge is dragged",
            placementBeforeTheDrag.getBottomMarginPixels() - 150,
            placement.getBottomMarginPixels());
    }

    @Test
    public void draggingTheLeftEdgeOutwardWidensTheDialogWithoutMovingItsRightEdge() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        controller.onDialogResizedBy(bottomRightCorner(), -300, 0);
        controller.onDialogMovedBy(200, 0);
        OwnerCallDialogPlacement placementBeforeTheDrag = placementOf(root);

        controller.onDialogResizedBy(leftEdge(), -120, 0);

        OwnerCallDialogPlacement placement = placementOf(root);
        Assert.assertEquals(placementBeforeTheDrag.getLeftMarginPixels() - 120,
            placement.getLeftMarginPixels());
        Assert.assertEquals(placementBeforeTheDrag.getWidthPixels() + 120,
            placement.getWidthPixels());
        Assert.assertEquals("the right edge must stay where it was while the left edge is dragged",
            placementBeforeTheDrag.getLeftMarginPixels() + placementBeforeTheDrag.getWidthPixels(),
            placement.getLeftMarginPixels() + placement.getWidthPixels());
    }

    @Test
    public void draggingTheTopEdgeUpwardGrowsTheDialogUpwardWithoutMovingItsBottomEdge() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        OwnerCallDialogPlacement placementBeforeTheDrag = placementOf(root);

        controller.onDialogResizedBy(topEdge(), 0, -250);

        OwnerCallDialogPlacement placement = placementOf(root);
        Assert.assertEquals(placementBeforeTheDrag.getHeightPixels() + 250,
            placement.getHeightPixels());
        Assert.assertEquals("the bottom edge must stay where it was while the top edge is dragged",
            placementBeforeTheDrag.getBottomMarginPixels(), placement.getBottomMarginPixels());
    }

    @Test
    public void draggingTheBottomEdgeDownwardGrowsTheDialogDownward() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        OwnerCallDialogPlacement placementBeforeTheDrag = placementOf(root);

        controller.onDialogResizedBy(bottomEdge(), 0, 100);

        OwnerCallDialogPlacement placement = placementOf(root);
        Assert.assertEquals(placementBeforeTheDrag.getHeightPixels() + 100,
            placement.getHeightPixels());
        Assert.assertEquals(placementBeforeTheDrag.getBottomMarginPixels() - 100,
            placement.getBottomMarginPixels());
    }

    @Test
    public void aDragThatStartsOnTheFrameEdgeResizesTheDialogAndStoresThePlacement() {
        RecordedPlacementStore store = new RecordedPlacementStore();
        View root = inflateActivityLayout();
        OwnerCallDialogController controller =
            controllerFor(root, allCallsBySession(), new RecordedBodyTaps(), store);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        OwnerCallDialogFrame frame = (OwnerCallDialogFrame) dialog(root);
        layOutDialog(root, frame);
        OwnerCallDialogPlacement placementBeforeTheDrag = placementOf(root);

        int grabY = frame.getHeight() - 1;
        int slop = ViewConfiguration.get(root.getContext()).getScaledTouchSlop();
        long downTime = SystemClock.uptimeMillis();
        dispatchToFrame(frame, MotionEvent.ACTION_DOWN, downTime, downTime,
            frame.getWidth() / 2, grabY);
        dispatchToFrame(frame, MotionEvent.ACTION_MOVE, downTime, downTime + 20,
            frame.getWidth() / 2, grabY + slop + 1);
        dispatchToFrame(frame, MotionEvent.ACTION_MOVE, downTime, downTime + 40,
            frame.getWidth() / 2, grabY + slop + 1 + 90);
        dispatchToFrame(frame, MotionEvent.ACTION_UP, downTime, downTime + 60,
            frame.getWidth() / 2, grabY + slop + 1 + 90);

        OwnerCallDialogPlacement placement = placementOf(root);
        Assert.assertEquals("dragging the bottom frame edge downward must grow the dialog",
            placementBeforeTheDrag.getHeightPixels() + 90, placement.getHeightPixels());
        Assert.assertEquals(placement, store.loadPlacement());
    }

    @Test
    public void aDragThatStartsInTheMiddleOfTheHeaderStillMovesTheDialogInsteadOfResizingIt() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        OwnerCallDialogFrame frame = (OwnerCallDialogFrame) dialog(root);
        layOutDialog(root, frame);
        View header = root.findViewById(R.id.owner_call_dialog_header);

        Assert.assertFalse("the middle of the header must not grip any frame edge",
            frame.gripAt(frame.getWidth() / 2, header.getBottom() / 2).isAnyEdgeGripped());
    }

    private static void layOutDialog(View root, OwnerCallDialogFrame frame) {
        root.measure(View.MeasureSpec.makeMeasureSpec(GEOMETRY.getAvailableWidthPixels(),
                View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(GEOMETRY.getAvailableHeightPixels(),
                View.MeasureSpec.EXACTLY));
        root.layout(0, 0, GEOMETRY.getAvailableWidthPixels(),
            GEOMETRY.getAvailableHeightPixels());
        Assert.assertTrue("the dialog must have been laid out before the drag",
            frame.getWidth() > 0 && frame.getHeight() > 0);
    }

    private static void dispatchToFrame(OwnerCallDialogFrame frame, int action, long downTime,
                                        long eventTime, int x, int y) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        frame.dispatchTouchEvent(event);
        event.recycle();
    }

    private static OwnerCallDialogEdgeGrip bottomRightCorner() {
        return OwnerCallDialogEdgeGrip.bottomRightCorner();
    }

    private static OwnerCallDialogEdgeGrip leftEdge() {
        return OwnerCallDialogEdgeGrip.resolve(0, 500, 1000, 1000, 60, 0);
    }

    private static OwnerCallDialogEdgeGrip topEdge() {
        return OwnerCallDialogEdgeGrip.resolve(500, 0, 1000, 1000, 60, 0);
    }

    private static OwnerCallDialogEdgeGrip bottomEdge() {
        return OwnerCallDialogEdgeGrip.resolve(500, 1000, 1000, 1000, 60, 0);
    }

    @Test
    public void theChosenPlacementSurvivesAReBindFromATerminalScreenUpdate() {
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        controller.onDialogResizedBy(bottomRightCorner(), -80, 150);
        controller.onDialogMovedBy(60, -200);
        OwnerCallDialogPlacement placementAfterTheDrags = placementOf(root);

        controller.showCallsForSession(FIRST_SESSION, NOW + 1000);

        Assert.assertEquals(placementAfterTheDrags, placementOf(root));
    }

    @Test
    public void aLayoutChangeThatMakesTheChosenPlacementInvalidClampsItBackInsideTheScreen() {
        View root = inflateActivityLayout();
        AtomicReference<OwnerCallDialogGeometry> geometryRef =
            new AtomicReference<>(GEOMETRY);
        OwnerCallDialogController controller = new OwnerCallDialogController(root,
            sessionName -> {
                List<OwnerCall> calls = new HashMap<String, List<OwnerCall>>() {{
                    put(FIRST_SESSION, Arrays.asList(EARLIER_CALL, LATER_CALL));
                }}.get(sessionName);
                return calls == null ? Collections.emptyList() : calls;
            },
            geometryRef::get,
            new RecordedBodyTaps(),
            new RecordedPlacementStore());
        controller.showCallsForSession(FIRST_SESSION, NOW);
        controller.onDialogMovedBy(0, -GEOMETRY.getAvailableHeightPixels());

        OwnerCallDialogGeometry reducedGeometry =
            OwnerCallDialogGeometry.resolve(1000, 0, 1080, 120, 36);
        geometryRef.set(reducedGeometry);
        controller.showCallsForSession(FIRST_SESSION, NOW + 1000);

        OwnerCallDialogPlacement placement = placementOf(root);
        Assert.assertTrue("the dialog must stay inside the reduced screen",
            placement.getBottomMarginPixels() + placement.getHeightPixels()
                <= reducedGeometry.getAvailableHeightPixels());
        Assert.assertTrue("the dialog must stay inside the reduced screen",
            placement.getLeftMarginPixels() + placement.getWidthPixels()
                <= reducedGeometry.getAvailableWidthPixels());
    }

    @Test
    public void theChosenPlacementIsStoredWhenTheDragEndsAndIsUsedOnTheNextLaunch() {
        RecordedPlacementStore store = new RecordedPlacementStore();
        View root = inflateActivityLayout();
        OwnerCallDialogController controller = controllerFor(root, allCallsBySession(),
            new RecordedBodyTaps(), store);
        controller.showCallsForSession(FIRST_SESSION, NOW);
        controller.onDialogResizedBy(bottomRightCorner(), -200, 0);
        controller.onDialogMovedBy(120, -200);
        controller.onDialogPlacementCommitted();
        OwnerCallDialogPlacement storedPlacement = placementOf(root);

        View relaunchedRoot = inflateActivityLayout();
        OwnerCallDialogController relaunchedController =
            controllerFor(relaunchedRoot, allCallsBySession(), new RecordedBodyTaps(), store);
        relaunchedController.showCallsForSession(FIRST_SESSION, NOW);

        Assert.assertEquals(storedPlacement, placementOf(relaunchedRoot));
    }

    @Test
    public void rendersTheBodyWithoutTheMarkerLineAndMakesItsCopyTextAndUrlTappable() {
        View root = inflateActivityLayout();
        Map<String, List<OwnerCall>> callsBySession = new HashMap<>();
        callsBySession.put(FIRST_SESSION, Collections.singletonList(
            new OwnerCall(FIRST_SESSION, "2026-08-14T08:10:00Z",
                "🔴\n\nRun <copy>termux-reload-settings</copy> then open "
                    + "https://github.com/HiromiShikata/termux-app/pull/1925")));
        RecordedBodyTaps bodyTaps = new RecordedBodyTaps();
        OwnerCallDialogController controller = controllerFor(root, callsBySession, bodyTaps);

        controller.showCallsForSession(FIRST_SESSION, NOW);

        Assert.assertEquals("Run termux-reload-settings then open "
            + "https://github.com/HiromiShikata/termux-app/pull/1925", bodyText(root));
        ClickableSpan[] tappableRanges = renderedBodyTappableRanges(root);
        Assert.assertEquals(2, tappableRanges.length);
        for (ClickableSpan tappableRange : tappableRanges) {
            tappableRange.onClick(root);
        }
        Assert.assertEquals(Collections.singletonList("termux-reload-settings"),
            bodyTaps.copiedTexts);
        Assert.assertEquals(Collections.singletonList(
            "https://github.com/HiromiShikata/termux-app/pull/1925"), bodyTaps.openedUrls);
    }

    private static ClickableSpan[] renderedBodyTappableRanges(View root) {
        Spanned renderedBody =
            (Spanned) ((TextView) root.findViewById(R.id.owner_call_dialog_body)).getText();
        return renderedBody.getSpans(0, renderedBody.length(), ClickableSpan.class);
    }

    private static OwnerCallDialogController controllerFor(View root) {
        return controllerFor(root, allCallsBySession());
    }

    private static Map<String, List<OwnerCall>> allCallsBySession() {
        Map<String, List<OwnerCall>> callsBySession = new HashMap<>();
        callsBySession.put(FIRST_SESSION, Arrays.asList(EARLIER_CALL, LATER_CALL));
        callsBySession.put(SECOND_SESSION, Arrays.asList(OTHER_SESSION_CALL,
            OTHER_SESSION_SECOND_CALL, OTHER_SESSION_THIRD_CALL));
        return callsBySession;
    }

    private static OwnerCallDialogController controllerFor(View root,
                                                           Map<String, List<OwnerCall>> callsBySession) {
        return controllerFor(root, callsBySession, new RecordedBodyTaps());
    }

    private static OwnerCallDialogController controllerFor(View root,
                                                           Map<String, List<OwnerCall>> callsBySession,
                                                           RecordedBodyTaps bodyTaps) {
        return controllerFor(root, callsBySession, bodyTaps, new RecordedPlacementStore());
    }

    private static OwnerCallDialogController controllerFor(View root,
                                                           Map<String, List<OwnerCall>> callsBySession,
                                                           RecordedBodyTaps bodyTaps,
                                                           RecordedPlacementStore placementStore) {
        return new OwnerCallDialogController(root,
            sessionName -> {
                List<OwnerCall> calls = callsBySession.get(sessionName);
                return calls == null ? Collections.emptyList() : calls;
            },
            () -> GEOMETRY,
            bodyTaps,
            placementStore);
    }

    private static final class RecordedPlacementStore
        implements OwnerCallDialogController.OwnerCallDialogPlacementStore {

        private OwnerCallDialogPlacement storedPlacement = null;

        @Override
        public OwnerCallDialogPlacement loadPlacement() {
            return storedPlacement;
        }

        @Override
        public void savePlacement(OwnerCallDialogPlacement placement) {
            storedPlacement = placement;
        }
    }

    private static OwnerCallDialogPlacement placementOf(View root) {
        ViewGroup.MarginLayoutParams layoutParams =
            (ViewGroup.MarginLayoutParams) dialog(root).getLayoutParams();
        return new OwnerCallDialogPlacement(layoutParams.leftMargin, layoutParams.bottomMargin,
            layoutParams.width, layoutParams.height);
    }

    private static final class RecordedBodyTaps
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

    private static View dialog(View root) {
        return root.findViewById(R.id.owner_call_dialog);
    }

    private static String positionText(View root) {
        return ((TextView) root.findViewById(R.id.owner_call_dialog_position)).getText().toString();
    }

    private static String bodyText(View root) {
        return ((TextView) root.findViewById(R.id.owner_call_dialog_body)).getText().toString();
    }

    private static View inflateActivityLayout() {
        Context context = new ContextThemeWrapper(RuntimeEnvironment.getApplication(),
            R.style.Theme_TermuxActivity_DayNight_NoActionBar);
        return LayoutInflater.from(context)
            .inflate(R.layout.activity_termux, new FrameLayout(context), false);
    }
}
