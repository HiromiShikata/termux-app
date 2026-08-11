package com.termux.app.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.termux.app.TermuxActivity;
import com.termux.app.terminal.TermuxSessionsListViewController;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DiagnosticsSessionDisplayStateInstrumentedTest {

    private static final String SESSION_LINE_PREFIX = "  - ";

    private static final String SESSION_LINE_FIELD_SEPARATOR = " | ";

    private static final int FIRST_SESSION_INDEX = 0;

    private static final long MILLIS_ALLOWED_FOR_THE_FIRST_SESSION_TO_APPEAR = 30000L;

    private static final long MILLIS_BETWEEN_READINESS_READINGS = 100L;

    @Test
    public void theDisplayStateOnASessionLineIsTheStateTheSessionListItselfReportsForThatSession() {
        ActivityScenario<TermuxActivity> scenario = ActivityScenario.launch(TermuxActivity.class);
        awaitASessionListHoldingASession(scenario);
        scenario.onActivity(activity -> {
            TermuxSessionsListViewController listController =
                activity.getTermuxSessionListViewController();
            assertNotNull(listController);
            listController.refreshSessionList();

            List<Integer> sessionIndexesDisplayedInList =
                listController.getSessionIndexesDisplayedInList();
            String report = new DiagnosticsReportBuilder().build(
                new DiagnosticsReportCollector().collect(activity, System.currentTimeMillis()));

            String firstSessionLine = firstSessionLineOf(report);
            assertNotNull("the report must carry a line for the session the activity is showing,"
                + " otherwise there is no per-session state to check: " + report, firstSessionLine);

            DiagnosticsSessionListDisplay displayStateTheListReports =
                DiagnosticsSessionListDisplay.ofSessionIndex(
                    FIRST_SESSION_INDEX, sessionIndexesDisplayedInList);
            assertEquals("the state printed on a session line is read by pairing it with the session"
                    + " list, so it has to be the state that list reports for that same session; a"
                    + " line that names a different session's state would send a reader looking for a"
                    + " session that was never in that position: " + report,
                displayStateTheListReports.getReportLabel(), displayStateOn(firstSessionLine));
        });
    }

    private static String firstSessionLineOf(String report) {
        for (String line : report.split("\n")) {
            if (line.startsWith(SESSION_LINE_PREFIX)) return line;
        }
        return null;
    }

    private static String displayStateOn(String sessionLine) {
        int lastSeparatorIndex = sessionLine.lastIndexOf(SESSION_LINE_FIELD_SEPARATOR);
        assertTrue("a session line must carry its fields separated by " + SESSION_LINE_FIELD_SEPARATOR
            + ": " + sessionLine, lastSeparatorIndex >= 0);
        return sessionLine.substring(lastSeparatorIndex + SESSION_LINE_FIELD_SEPARATOR.length());
    }

    private static void awaitASessionListHoldingASession(ActivityScenario<TermuxActivity> scenario) {
        boolean[] readyToBeRead = new boolean[1];
        long deadlineMillis =
            SystemClock.uptimeMillis() + MILLIS_ALLOWED_FOR_THE_FIRST_SESSION_TO_APPEAR;
        while (SystemClock.uptimeMillis() < deadlineMillis) {
            scenario.onActivity(activity ->
                readyToBeRead[0] = activity.getTermuxSessionListViewController() != null
                    && activity.getTermuxService() != null
                    && !activity.getTermuxService().getTermuxSessions().isEmpty());
            if (readyToBeRead[0]) return;
            SystemClock.sleep(MILLIS_BETWEEN_READINESS_READINGS);
        }
        fail("the activity never produced a session list holding a session, so no per-session display"
            + " state could be read");
    }
}
