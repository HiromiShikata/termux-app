package com.termux.app.copytag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ClipboardManager;
import android.content.Context;
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
public class CopyTagTapInstrumentedTest {

    private static final String PAYLOAD = "the-copy-tag-payload-value";

    private static final long MILLIS_ALLOWED_FOR_THE_FIRST_SESSION_TO_APPEAR = 30000L;

    private static final long MILLIS_BETWEEN_READINESS_READINGS = 100L;

    @Test
    public void tappingInsideACopyTaggedBlockPutsTheWholeBlockOnTheClipboard() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        awaitATerminalViewShowingASession(scenario);
        scenario.onActivity(activity -> {
            TerminalView terminalView = activity.getTerminalView();
            assertNotNull(terminalView);
            assertNotNull(activity.getCurrentSession());

            TerminalEmulator emulator = activity.getCurrentSession().getEmulator();
            assertNotNull(emulator);

            String block = "\r\n<copy>\r\n" + PAYLOAD + "\r\n</copy>\r\n";
            byte[] bytes = block.getBytes(StandardCharsets.UTF_8);
            emulator.append(bytes, bytes.length);

            int payloadRow = rowShowing(emulator, PAYLOAD);
            assertTrue("the payload was not rendered on any row", payloadRow >= 0);

            MotionEvent tap = tapOnRow(terminalView, payloadRow);
            assertNotNull("no touch position mapped to the payload row", tap);

            clearClipboard(activity);
            activity.getTermuxTerminalViewClient().onSingleTapUp(tap);
            tap.recycle();

            assertEquals(PAYLOAD, clipboardText(activity));
        });
    }

    private static void awaitATerminalViewShowingASession(ActivityScenario<TermuxActivity> scenario) {
        boolean[] readyToBeTapped = new boolean[1];
        long deadlineMillis =
            SystemClock.uptimeMillis() + MILLIS_ALLOWED_FOR_THE_FIRST_SESSION_TO_APPEAR;
        while (SystemClock.uptimeMillis() < deadlineMillis) {
            scenario.onActivity(activity -> {
                TerminalView terminalView = activity.getTerminalView();
                readyToBeTapped[0] = terminalView != null && terminalView.getHeight() > 0
                    && activity.getCurrentSession() != null
                    && activity.getCurrentSession().getEmulator() != null;
            });
            if (readyToBeTapped[0]) return;
            SystemClock.sleep(MILLIS_BETWEEN_READINESS_READINGS);
        }
        fail("the activity never produced a laid out terminal view showing a session, so no tap could"
            + " be delivered to one");
    }

    private static int rowShowing(TerminalEmulator emulator, String text) {
        for (int row = 0; row < emulator.mRows; row++) {
            String rendered = emulator.getScreen()
                .getSelectedText(0, row, emulator.mColumns - 1, row, false, false);
            if (rendered != null && rendered.contains(text)) return row;
        }
        return -1;
    }

    private static MotionEvent tapOnRow(TerminalView terminalView, int row) {
        for (int y = 0; y < terminalView.getHeight(); y++) {
            MotionEvent candidate = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, 1f, y, 0);
            int[] columnAndRow = terminalView.getColumnAndRow(candidate, true);
            if (columnAndRow[1] == row) return candidate;
            candidate.recycle();
        }
        return null;
    }

    private static void clearClipboard(Context context) {
        clipboardManager(context).setPrimaryClip(android.content.ClipData.newPlainText("", ""));
    }

    private static String clipboardText(Context context) {
        android.content.ClipData clip = clipboardManager(context).getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return null;
        CharSequence text = clip.getItemAt(0).getText();
        return text == null ? null : text.toString();
    }

    private static ClipboardManager clipboardManager(Context context) {
        return (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }
}
