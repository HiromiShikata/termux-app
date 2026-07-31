package com.termux.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.InputDevice;
import android.view.MotionEvent;

import androidx.drawerlayout.widget.DrawerLayout;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.TermuxService;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.app.terminal.TermuxTerminalViewClient;
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class PinchGestureAppliesFontSizeOnceTest {

    private static final int TERMINAL_COLUMNS = 80;

    private static final int TERMINAL_ROWS = 40;

    private static final int TERMINAL_CELL_WIDTH_PIXELS = 12;

    private static final int TERMINAL_CELL_HEIGHT_PIXELS = 24;

    private static final Integer TRANSCRIPT_ROWS = 2000;

    private static final int INITIAL_FONT_SIZE = 12;

    private static final int FONT_SIZE_STEP = 2;

    private static final int GESTURE_FOCUS_X = 500;

    private static final int GESTURE_FOCUS_Y = 500;

    private static final long GESTURE_EVENT_INTERVAL_MILLIS = 20L;

    private static final float[] SPREADING_PINCH_SPANS = {300f, 360f, 432f, 518f, 622f, 746f, 895f};

    private static final float[] BARELY_MOVING_PINCH_SPANS = {300f, 318f, 322f, 326f, 328f};

    private TerminalView terminalView;

    private TermuxAppSharedPreferences preferences;

    private final List<Integer> fontSizesAppliedToTheTerminal = new ArrayList<>();

    private TerminalRenderer rendererBeforeTheLastEvent;

    @Before
    public void setUp() throws Exception {
        TermuxActivity activity = Robolectric.buildActivity(TermuxActivity.class).get();
        Context appContext = RuntimeEnvironment.getApplication();

        TermuxService service = Robolectric.buildService(TermuxService.class).get();
        TermuxShellManager shellManager = new TermuxShellManager(appContext);
        set(service, TermuxService.class, "mShellManager", shellManager);
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        set(activity, TermuxActivity.class, "mTermuxService", service);
        set(activity, TermuxActivity.class, "mTermuxTerminalSessionActivityClient",
            new TermuxTerminalSessionActivityClient(activity));
        service.setTermuxTerminalSessionClient(activity.getTermuxTerminalSessionClient());

        preferences = TermuxAppSharedPreferences.build(appContext, true);
        set(activity, TermuxActivity.class, "mPreferences", preferences);
        preferences.setFontSize(INITIAL_FONT_SIZE);

        set(activity, TermuxActivity.class, "mProperties", TermuxAppSharedProperties.init(appContext));

        DrawerLayout drawerLayout = new DrawerLayout(appContext);
        drawerLayout.setId(R.id.drawer_layout);
        activity.setContentView(drawerLayout);

        terminalView = new TerminalView(appContext, null);
        set(activity, TermuxActivity.class, "mTerminalView", terminalView);

        TerminalSession terminalSession = new TerminalSession(null, null, null, null, null,
            activity.getTermuxTerminalSessionClient());
        terminalView.mTermSession = terminalSession;
        terminalView.mEmulator = new TerminalEmulator(terminalSession, TERMINAL_COLUMNS, TERMINAL_ROWS,
            TERMINAL_CELL_WIDTH_PIXELS, TERMINAL_CELL_HEIGHT_PIXELS, TRANSCRIPT_ROWS,
            activity.getTermuxTerminalSessionClient());
        terminalView.mClient = new TermuxTerminalViewClient(activity,
            activity.getTermuxTerminalSessionClient());

        terminalView.setTextSize(INITIAL_FONT_SIZE);
        rendererBeforeTheLastEvent = terminalView.mRenderer;
    }

    @Test
    public void aPinchGestureCrossingTheThresholdSeveralTimesAppliesTheFontSizeOnce() {
        pinch(SPREADING_PINCH_SPANS);

        int fontSizeThePerStepAccumulationArrivedAt = preferences.getFontSize();

        assertTrue("a pinch that spreads far enough to cross the ten percent scale threshold on every"
                + " move must not make the terminal rebuild its renderer and re-emit its whole"
                + " scrollback once per crossing; the size the gesture arrives at belongs on screen"
                + " once, when the fingers leave it, yet this gesture applied the font size "
                + fontSizesAppliedToTheTerminal.size() + " times, applying "
                + fontSizesAppliedToTheTerminal,
            fontSizesAppliedToTheTerminal.size() == 1);
        assertEquals("the one font size that reaches the terminal at the end of the gesture must be"
                + " the size the per step accumulation arrived at, because saving the intermediate"
                + " reflows must cost the user nothing in where the pinch lands",
            fontSizeThePerStepAccumulationArrivedAt, fontSizesAppliedToTheTerminal.get(0).intValue());
        assertTrue("this gesture is evidence about repeated reflow only if it really asked for several"
                + " font size steps, so the accumulated size must have moved at least three steps away"
                + " from " + INITIAL_FONT_SIZE + ", yet it ended at "
                + fontSizeThePerStepAccumulationArrivedAt,
            fontSizeThePerStepAccumulationArrivedAt >= INITIAL_FONT_SIZE + 3 * FONT_SIZE_STEP);
    }

    @Test
    public void aPinchGestureThatNeverCrossesTheThresholdAppliesNoFontSize() {
        pinch(BARELY_MOVING_PINCH_SPANS);

        assertTrue("a pinch whose accumulated scale stayed inside the ten percent dead zone decided no"
                + " font size step at all, so the end of the gesture must not make the terminal"
                + " rebuild its renderer and re-emit its scrollback for a size nobody asked to change,"
                + " yet this gesture applied the font size " + fontSizesAppliedToTheTerminal.size()
                + " times, applying " + fontSizesAppliedToTheTerminal,
            fontSizesAppliedToTheTerminal.isEmpty());
        assertEquals("a pinch that never crossed the threshold must leave the font size where it"
                + " started, because the dead zone exists so that a small finger movement costs no"
                + " reflow and no visible change",
            INITIAL_FONT_SIZE, preferences.getFontSize());
    }

    private void pinch(float[] spans) {
        long downTime = 1000L;
        long eventTime = downTime;

        dispatch(singleFingerEvent(MotionEvent.ACTION_DOWN, downTime, eventTime, spans[0]));

        eventTime += GESTURE_EVENT_INTERVAL_MILLIS;
        dispatch(twoFingerEvent(secondPointerAction(MotionEvent.ACTION_POINTER_DOWN), downTime,
            eventTime, spans[0]));

        for (int spanIndex = 1; spanIndex < spans.length; spanIndex++) {
            eventTime += GESTURE_EVENT_INTERVAL_MILLIS;
            dispatch(twoFingerEvent(MotionEvent.ACTION_MOVE, downTime, eventTime, spans[spanIndex]));
        }

        float finalSpan = spans[spans.length - 1];

        eventTime += GESTURE_EVENT_INTERVAL_MILLIS;
        dispatch(twoFingerEvent(secondPointerAction(MotionEvent.ACTION_POINTER_UP), downTime,
            eventTime, finalSpan));

        eventTime += GESTURE_EVENT_INTERVAL_MILLIS;
        dispatch(singleFingerEvent(MotionEvent.ACTION_UP, downTime, eventTime, finalSpan));
    }

    private void dispatch(MotionEvent event) {
        try {
            terminalView.onTouchEvent(event);
        } finally {
            event.recycle();
        }
        recordAnyFontSizeApplication();
    }

    private void recordAnyFontSizeApplication() {
        TerminalRenderer rendererAfterTheEvent = terminalView.mRenderer;
        if (rendererAfterTheEvent == rendererBeforeTheLastEvent) return;

        fontSizesAppliedToTheTerminal.add(rendererAfterTheEvent.mTextSize);
        rendererBeforeTheLastEvent = rendererAfterTheEvent;
    }

    private static int secondPointerAction(int action) {
        return action | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
    }

    private static MotionEvent singleFingerEvent(int action, long downTime, long eventTime, float span) {
        MotionEvent.PointerProperties[] properties = {pointerProperties(0)};
        MotionEvent.PointerCoords[] coordinates = {pointerCoordinates(GESTURE_FOCUS_X - span / 2f)};

        return obtain(action, downTime, eventTime, properties, coordinates);
    }

    private static MotionEvent twoFingerEvent(int action, long downTime, long eventTime, float span) {
        MotionEvent.PointerProperties[] properties = {pointerProperties(0), pointerProperties(1)};
        MotionEvent.PointerCoords[] coordinates = {
            pointerCoordinates(GESTURE_FOCUS_X - span / 2f),
            pointerCoordinates(GESTURE_FOCUS_X + span / 2f)
        };

        return obtain(action, downTime, eventTime, properties, coordinates);
    }

    private static MotionEvent obtain(int action, long downTime, long eventTime,
                                      MotionEvent.PointerProperties[] properties,
                                      MotionEvent.PointerCoords[] coordinates) {
        return MotionEvent.obtain(downTime, eventTime, action, properties.length, properties,
            coordinates, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
    }

    private static MotionEvent.PointerProperties pointerProperties(int pointerId) {
        MotionEvent.PointerProperties properties = new MotionEvent.PointerProperties();
        properties.id = pointerId;
        properties.toolType = MotionEvent.TOOL_TYPE_FINGER;
        return properties;
    }

    private static MotionEvent.PointerCoords pointerCoordinates(float x) {
        MotionEvent.PointerCoords coordinates = new MotionEvent.PointerCoords();
        coordinates.x = x;
        coordinates.y = GESTURE_FOCUS_Y;
        coordinates.pressure = 1f;
        coordinates.size = 1f;
        return coordinates;
    }

    private static void set(Object target, Class<?> declaringClass, String fieldName, Object value)
        throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
