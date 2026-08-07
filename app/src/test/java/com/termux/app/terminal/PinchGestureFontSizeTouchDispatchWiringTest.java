package com.termux.app.terminal;

import android.view.MotionEvent;
import android.view.View;

import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalOutput;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;

/**
 * Drives a real two finger pinch as {@link MotionEvent}s through {@link com.termux.view.TerminalView}
 * so the whole path the owner's fingers travel is covered: the view's touch handling, the scale
 * gesture recognizer, and the forwarding of the recognizer's scale start, scale steps and scale end
 * to the terminal view client.
 *
 * The other tests of the deferred font size application call the client's own onScale and onScaleEnd
 * directly, so every one of them still passes when the view stops forwarding the recognizer's scale
 * end to its client, even though pinch to zoom would then never change the font size at all. This
 * test dispatches touch events instead and therefore fails when that forwarding is missing.
 */
@RunWith(RobolectricTestRunner.class)
@Config(instrumentedPackages = {"com.termux.view"}, shadows = {TextSizeApplicationRecordingShadowTerminalView.class})
public class PinchGestureFontSizeTouchDispatchWiringTest {

    private static final int VIEW_WIDTH_PIXELS = 1080;

    private static final int VIEW_HEIGHT_PIXELS = 1920;

    private static final int EMULATOR_COLUMNS = 80;

    private static final int EMULATOR_ROWS = 24;

    private static final int EMULATOR_CELL_WIDTH_PIXELS = 10;

    private static final int EMULATOR_CELL_HEIGHT_PIXELS = 20;

    private static final int EMULATOR_TRANSCRIPT_ROWS = 2000;

    /**
     * The horizontal position both fingers keep. Well away from the right edge, where a touch would
     * be taken for a scroll thumb drag instead of a pinch.
     */
    private static final float PINCH_X_PIXELS = 200f;

    /**
     * The vertical midpoint both fingers stay centred on, so the gesture is a pure pinch: the focal
     * point never moves and no scroll or fling is produced alongside the scaling.
     */
    private static final float PINCH_CENTRE_Y_PIXELS = 900f;

    /** The finger separation when the second finger lands. */
    private static final float INITIAL_FINGER_SEPARATION_PIXELS = 400f;

    /**
     * The finger separations the spreading movement passes through. Each one doubles the separation
     * of the one before it, which is far past the ten percent the font size threshold asks for, and
     * there are enough of them for the threshold to be crossed more than once within the one gesture.
     */
    private static final float[] SPREADING_FINGER_SEPARATIONS_PIXELS = {800f, 1600f, 3200f, 6400f};

    /**
     * Every event of the gesture carries the same time, so the velocity tracker behind the gesture
     * detector measures no movement and the lifting of the fingers cannot be taken for a fling.
     */
    private static final long GESTURE_EVENT_TIME_MILLISECONDS = 100L;

    private PinchGestureFontSizeTestHarness harness;

    @Before
    public void setUp() throws Exception {
        harness = new PinchGestureFontSizeTestHarness(RuntimeEnvironment.getApplication());
        harness.terminalView.setTerminalViewClient(harness.client);
        harness.terminalView.mEmulator = new TerminalEmulator(new DiscardingTerminalOutput(),
            EMULATOR_COLUMNS, EMULATOR_ROWS, EMULATOR_CELL_WIDTH_PIXELS, EMULATOR_CELL_HEIGHT_PIXELS,
            EMULATOR_TRANSCRIPT_ROWS, null);
        harness.terminalView.measure(
            View.MeasureSpec.makeMeasureSpec(VIEW_WIDTH_PIXELS, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(VIEW_HEIGHT_PIXELS, View.MeasureSpec.EXACTLY));
        harness.terminalView.layout(0, 0, VIEW_WIDTH_PIXELS, VIEW_HEIGHT_PIXELS);
    }

    @Test
    public void aTwoFingerPinchDispatchedAsTouchEventsAppliesTheFontSizeOnceWhenTheFingersLift() {
        dispatchATwoFingerSpreadingPinch();

        int fontSizeTheStepArithmeticArrivedAt = harness.preferences.getFontSize();
        Assert.assertEquals("pinching to zoom is the only way the owner resizes the terminal font with "
                + "their fingers, and it reaches the client solely through the terminal view forwarding "
                + "the scale gesture recognizer's scale end; a view that stops forwarding it leaves the "
                + "gesture changing the stored font size while the terminal on screen never resizes, so "
                + "a real two finger pinch dispatched as touch events must leave exactly one font size "
                + "applied to the terminal, and it must be the size the per step arithmetic arrived at",
            Collections.singletonList(fontSizeTheStepArithmeticArrivedAt), harness.appliedTextSizes());
    }

    @Test
    public void aTwoFingerPinchDispatchedAsTouchEventsAppliesNothingWhileTheFingersAreStillMoving() {
        dispatchATwoFingerSpreadingPinch();
    }

    /**
     * Dispatches the whole gesture into the view, asserting after every finger movement that nothing
     * has been applied to the terminal yet. Applying a font size rebuilds the renderer and reflows the
     * accumulated transcript, so a gesture that applies one per crossed threshold is the stutter this
     * work removes.
     */
    private void dispatchATwoFingerSpreadingPinch() {
        int fontSizeBeforeGesture = harness.preferences.getFontSize();

        harness.terminalView.onTouchEvent(oneFingerEvent(MotionEvent.ACTION_DOWN,
            PINCH_CENTRE_Y_PIXELS - INITIAL_FINGER_SEPARATION_PIXELS / 2f));
        harness.terminalView.onTouchEvent(twoFingerEvent(
            MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            INITIAL_FINGER_SEPARATION_PIXELS));

        for (float separation : SPREADING_FINGER_SEPARATIONS_PIXELS) {
            harness.terminalView.onTouchEvent(twoFingerEvent(MotionEvent.ACTION_MOVE, separation));
            Assert.assertEquals("applying a font size rebuilds the renderer and reflows the whole "
                    + "accumulated transcript on the main thread, which is the stutter the owner feels "
                    + "while pinching, so nothing may be applied to the terminal until the fingers "
                    + "lift; the sizes applied so far while the fingers were still moving were "
                    + harness.appliedTextSizes(),
                Collections.emptyList(), harness.appliedTextSizes());
        }

        float finalSeparation =
            SPREADING_FINGER_SEPARATIONS_PIXELS[SPREADING_FINGER_SEPARATIONS_PIXELS.length - 1];
        harness.terminalView.onTouchEvent(twoFingerEvent(
            MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            finalSeparation));
        harness.terminalView.onTouchEvent(oneFingerEvent(MotionEvent.ACTION_UP,
            PINCH_CENTRE_Y_PIXELS - finalSeparation / 2f));

        int fontSizeAfterGesture = harness.preferences.getFontSize();
        Assert.assertTrue("every assertion made about this gesture is worthless unless the dispatched "
                + "touch events really did reach the scale gesture recognizer and cross the font size "
                + "threshold more than once, because a terminal view that forwards nothing at all to "
                + "its client would otherwise satisfy them for the wrong reason; the stored font size "
                + "moved from " + fontSizeBeforeGesture + " to " + fontSizeAfterGesture
                + ", which is fewer than the two steps this gesture spreads far enough to decide",
            fontSizeAfterGesture - fontSizeBeforeGesture >= 4);
    }

    private MotionEvent oneFingerEvent(int action, float y) {
        MotionEvent.PointerProperties properties = new MotionEvent.PointerProperties();
        properties.id = 0;
        MotionEvent.PointerCoords coordinates = new MotionEvent.PointerCoords();
        coordinates.x = PINCH_X_PIXELS;
        coordinates.y = y;
        return obtain(action, new MotionEvent.PointerProperties[]{properties},
            new MotionEvent.PointerCoords[]{coordinates});
    }

    private MotionEvent twoFingerEvent(int action, float separation) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[2];
        MotionEvent.PointerCoords[] coordinates = new MotionEvent.PointerCoords[2];
        float[] fingerY = {
            PINCH_CENTRE_Y_PIXELS - separation / 2f,
            PINCH_CENTRE_Y_PIXELS + separation / 2f
        };
        for (int finger = 0; finger < 2; finger++) {
            MotionEvent.PointerProperties fingerProperties = new MotionEvent.PointerProperties();
            fingerProperties.id = finger;
            properties[finger] = fingerProperties;
            MotionEvent.PointerCoords fingerCoordinates = new MotionEvent.PointerCoords();
            fingerCoordinates.x = PINCH_X_PIXELS;
            fingerCoordinates.y = fingerY[finger];
            coordinates[finger] = fingerCoordinates;
        }
        return obtain(action, properties, coordinates);
    }

    private MotionEvent obtain(int action, MotionEvent.PointerProperties[] properties,
                               MotionEvent.PointerCoords[] coordinates) {
        return MotionEvent.obtain(GESTURE_EVENT_TIME_MILLISECONDS, GESTURE_EVENT_TIME_MILLISECONDS,
            action, properties.length, properties, coordinates, 0, 0, 1f, 1f, 0, 0, 0, 0);
    }

    /** A terminal output that keeps the emulator constructible without a shell process behind it. */
    private static final class DiscardingTerminalOutput extends TerminalOutput {

        @Override
        public void write(byte[] data, int offset, int count) {
        }

        @Override
        public void titleChanged(String oldTitle, String newTitle) {
        }

        @Override
        public void onCopyTextToClipboard(String text) {
        }

        @Override
        public void onPasteTextFromClipboard() {
        }

        @Override
        public void onBell() {
        }

        @Override
        public void onSpeakNotification(String text) {
        }

        @Override
        public void onColorsChanged() {
        }
    }
}
