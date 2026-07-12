package com.termux.app.diagnostics;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CrashReportDiagnosticsSupplementTest {

    private Context context() {
        return RuntimeEnvironment.getApplication();
    }

    @Test
    public void withoutLiveActivityAppendsOnlyRecentEventsTail() {
        DiagnosticEventLog eventLog = new DiagnosticEventLog();
        eventLog.record(1783216770000L, DiagnosticEventType.SESSION_CREATED, "host-a");
        eventLog.record(1783216771000L, DiagnosticEventType.MAX_SESSIONS_REACHED, "cap=32");

        CrashReportDiagnosticsSupplement supplement = new CrashReportDiagnosticsSupplement(
            new DiagnosticsReportCollector(eventLog), new DiagnosticsReportBuilder(), eventLog);

        String section = supplement.buildSupplementSection(context());

        Assert.assertNotNull(section);
        Assert.assertTrue(section.contains(CrashReportDiagnosticsSupplement.SECTION_HEADER));
        Assert.assertTrue(section.contains("No live activity; recent events only"));
        Assert.assertTrue(section.contains("SESSION_CREATED host-a"));
        Assert.assertTrue(section.contains("MAX_SESSIONS_REACHED cap=32"));
        Assert.assertFalse(section.contains("Counted toward cap"));
        Assert.assertFalse(section.contains("Open tabs"));
        Assert.assertFalse(section.contains("Wake lock"));
    }

    @Test
    public void withoutLiveActivityAndNoEventsRendersPlaceholder() {
        DiagnosticEventLog eventLog = new DiagnosticEventLog();

        CrashReportDiagnosticsSupplement supplement = new CrashReportDiagnosticsSupplement(
            new DiagnosticsReportCollector(eventLog), new DiagnosticsReportBuilder(), eventLog);

        String section = supplement.buildSupplementSection(context());

        Assert.assertNotNull(section);
        Assert.assertTrue(section.contains(CrashReportDiagnosticsSupplement.SECTION_HEADER));
        Assert.assertTrue(section.contains("(no recent events)"));
    }

}
