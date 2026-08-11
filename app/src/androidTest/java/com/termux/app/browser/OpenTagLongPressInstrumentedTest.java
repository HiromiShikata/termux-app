package com.termux.app.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.SystemClock;
import android.view.MotionEvent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.app.TermuxActivity;
import com.termux.terminal.TerminalEmulator;
import com.termux.view.TerminalView;

import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OpenTagLongPressInstrumentedTest {

    private static final String URL_HEAD = "https://example.com/a";

    private static final String URL_TAIL = "bcdef?g=1";

    private static final String WHOLE_URL = URL_HEAD + URL_TAIL;

    private static final long MILLIS_ALLOWED_FOR_THE_FIRST_SESSION_TO_APPEAR = 30000L;

    private static final long MILLIS_BETWEEN_READINESS_READINGS = 100L;

    @Test
    public void longPressingAUrlTheWritingProgramBrokeAcrossRowsOffersTheWholeUrl() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        awaitATerminalViewShowingASession(scenario);
        scenario.onActivity(activity -> {
            TerminalView terminalView = activity.getTerminalView();
            assertNotNull(terminalView);
            assertNotNull(activity.getCurrentSession());

            TerminalEmulator emulator = activity.getCurrentSession().getEmulator();
            assertNotNull(emulator);

            String block = "\r\n<open> " + URL_HEAD + "\r\n  " + URL_TAIL + " </open>\r\n";
            byte[] bytes = block.getBytes(StandardCharsets.UTF_8);
            emulator.append(bytes, bytes.length);

            int rowShowingTheHeadOfTheUrl = rowShowing(emulator, URL_HEAD);
            assertTrue("the tagged URL was not rendered on any row", rowShowingTheHeadOfTheUrl >= 0);

            MotionEvent longPress = eventOnRow(terminalView, rowShowingTheHeadOfTheUrl);
            assertNotNull("no touch position mapped to the row showing the tagged URL", longPress);

            activity.getTermuxTerminalViewClient().onLongPress(longPress);
            longPress.recycle();

            assertEquals("a long press inside an open tag must offer the whole URL the tag names, "
                    + "not the part of it that fitted on the pressed row",
                WHOLE_URL, activity.getTermuxTerminalViewClient().getLongPressedUrl());
        });
    }

    private static void awaitATerminalViewShowingASession(ActivityScenario<TermuxActivity> scenario) {
        boolean[] readyToBeTouched = new boolean[1];
        long deadlineMillis =
            SystemClock.uptimeMillis() + MILLIS_ALLOWED_FOR_THE_FIRST_SESSION_TO_APPEAR;
        while (SystemClock.uptimeMillis() < deadlineMillis) {
            scenario.onActivity(activity -> {
                TerminalView terminalView = activity.getTerminalView();
                readyToBeTouched[0] = terminalView != null && terminalView.getHeight() > 0
                    && activity.getCurrentSession() != null
                    && activity.getCurrentSession().getEmulator() != null;
            });
            if (readyToBeTouched[0]) return;
            SystemClock.sleep(MILLIS_BETWEEN_READINESS_READINGS);
        }
        fail("the activity never produced a laid out terminal view showing a session, so no long press"
            + " could be delivered to one");
    }

    private static int rowShowing(TerminalEmulator emulator, String text) {
        for (int row = 0; row < emulator.mRows; row++) {
            String rendered = emulator.getScreen()
                .getSelectedText(0, row, emulator.mColumns - 1, row, false, false);
            if (rendered != null && rendered.contains(text)) return row;
        }
        return -1;
    }

    private static MotionEvent eventOnRow(TerminalView terminalView, int row) {
        for (int y = 0; y < terminalView.getHeight(); y++) {
            MotionEvent candidate = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 1f, y, 0);
            int[] columnAndRow = terminalView.getColumnAndRow(candidate, true);
            if (columnAndRow[1] == row) return candidate;
            candidate.recycle();
        }
        return null;
    }
}
