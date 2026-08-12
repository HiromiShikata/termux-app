package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class PhantomProcessMonitorInReportTest {

    private static final long REPORT_MILLIS = 1783216800000L;

    private static final DiagnosticsWorkCostLine NO_WORK_COST = new DiagnosticsWorkCostLine(0, 0, 0, 0);

    private static final String SECTION_HEADING = "Android phantom process monitor";

    private static String renderedReportOf(DiagnosticsPhantomProcessMonitor phantomProcessMonitor) {
        DiagnosticsReport report = new DiagnosticsReport("0.119.0", 119, REPORT_MILLIS,
            0, 0, 32, Collections.<DiagnosticsSessionLine>emptyList(),
            0, 0, false, true, Collections.<DiagnosticEvent>emptyList(),
            new DiagnosticsMemoryUsage(0, 0, 0, 0),
            NO_WORK_COST, NO_WORK_COST, NO_WORK_COST,
            new DiagnosticsSessionReconnectCost(0, 0, 0, 0, Collections.emptyList()),
            new DiagnosticsReplacedSessionShellInput(0, 0, 0L, "", "", ""),
            new DiagnosticsMainThreadStalls(250L, 0L, 0L, "", Collections.emptyList(), 0L, 0L),
            DiagnosticsMainLooperQueue.parse(Collections.<String>emptyList()), ScrollbarViewCensus.empty(), 0L,
            new DiagnosticsBackgroundCycle(0L, Collections.<BackgroundCycleInterval>emptyList()),
            DiagnosticsVersionChange.sameVersionAsThePreviousLaunch(),
            DiagnosticsShellExits.NONE, phantomProcessMonitor,
            DiagnosticsAppProcessPopulation.UNMEASURED);
        return new DiagnosticsReportBuilder().build(report);
    }

    @Test
    public void theReportStatesWhetherAndroidIsMonitoringPhantomProcessesAndTheLimitItEnforces() {
        String report = renderedReportOf(new DiagnosticsPhantomProcessMonitor("true", 32, true));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the shells are ending on signal 9 at a rate the app itself cannot account for,"
                + " and the Android phantom process monitor is the only killer of app child processes that"
                + " the app can observe, so a report that omits its state leaves the cause unattributable."
                + " Actual report:\n" + report,
            sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("whether the monitor is running at all is the first thing that separates it from"
            + " every other candidate killer. Actual report:\n" + report, section.contains("Monitor flag: true"));
        Assert.assertTrue("the enforced maximum is what decides whether the number of sessions in use is"
            + " over the limit, which is the whole question. Actual report:\n" + report,
            section.contains("Enforced maximum: 32"));
        Assert.assertTrue("a reader who sees the monitor running has to know whether it can be turned off"
            + " from this device without a computer attached. Actual report:\n" + report,
            section.contains("Can be switched off from settings: yes"));
    }

    @Test
    public void aLimitAndroidRefusedToDiscloseIsReportedAsNotReadableRatherThanAsANumber() {
        String report = renderedReportOf(new DiagnosticsPhantomProcessMonitor("true", null, false));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present for its content to matter. Actual report:\n"
            + report, sectionIndex >= 0);

        String section = report.substring(sectionIndex);
        Assert.assertTrue("printing a stand-in number for a limit that was never read would send the"
            + " investigation after a value Android never reported. Actual report:\n" + report,
            section.contains("Enforced maximum: not readable"));
        Assert.assertTrue("being unable to switch the monitor off is the finding that explains why the"
            + " symptom persists, so it has to be stated. Actual report:\n" + report,
            section.contains("Can be switched off from settings: no"));
    }

    @Test
    public void aRunInWhichTheMonitorStateWasNeverReadSaysSoRatherThanLookingMeasured() {
        String report = renderedReportOf(DiagnosticsPhantomProcessMonitor.UNMEASURED);

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("an absent section reads as a device on which the monitor is not running, which"
            + " is the one conclusion the reader must not draw by default. Actual report:\n" + report,
            sectionIndex >= 0);
        Assert.assertTrue("an unread state must not be presentable as a measured one. Actual report:\n"
            + report,
            report.substring(sectionIndex).contains("Monitor flag: "
                + DiagnosticsPhantomProcessMonitor.UNMEASURED_MONITOR_FLAG_VALUE));
    }

    @Test
    public void theMonitorStateSitsInsideTheWindowTheReportSurvives() {
        String report = renderedReportOf(new DiagnosticsPhantomProcessMonitor("true", 32, false));

        int sectionIndex = report.indexOf(SECTION_HEADING);
        Assert.assertTrue("the section has to be present for its position to matter. Actual report:\n"
            + report, sectionIndex >= 0);
        Assert.assertTrue("the report reaches the reader by being pasted into a channel that keeps only"
                + " the first " + DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS + " characters, so a"
                + " section that falls outside that window cannot be read at all. It currently begins at"
                + " character " + sectionIndex + ". Actual report:\n" + report,
            sectionIndex < DiagnosticsReportBuilder.PASTE_LIMIT_CHARACTERS);
    }
}
