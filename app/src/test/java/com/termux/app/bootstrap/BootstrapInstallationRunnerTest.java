package com.termux.app.bootstrap;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BootstrapInstallationRunnerTest {

    @Test
    public void reportsAnAllocationFailureInsteadOfLettingItEscapeTheInstallationThread() {
        List<Throwable> reportedFailures = new ArrayList<>();
        OutOfMemoryError allocationFailure = new OutOfMemoryError("Failed to allocate a 20283264 byte allocation");

        new BootstrapInstallationRunner(reportedFailures::add).run(() -> {
            throw allocationFailure;
        }, () -> {
        });

        Assert.assertEquals(1, reportedFailures.size());
        Assert.assertSame(allocationFailure, reportedFailures.get(0));
    }

    @Test
    public void reportsAnInputFailureInsteadOfLettingItEscapeTheInstallationThread() {
        List<Throwable> reportedFailures = new ArrayList<>();
        IOException inputFailure = new IOException("Truncated bootstrap archive entry");

        new BootstrapInstallationRunner(reportedFailures::add).run(() -> {
            throw inputFailure;
        }, () -> {
        });

        Assert.assertEquals(1, reportedFailures.size());
        Assert.assertSame(inputFailure, reportedFailures.get(0));
    }

    @Test
    public void reportsARuntimeFailureInsteadOfLettingItEscapeTheInstallationThread() {
        List<Throwable> reportedFailures = new ArrayList<>();
        RuntimeException runtimeFailure = new RuntimeException("No SYMLINKS.txt encountered");

        new BootstrapInstallationRunner(reportedFailures::add).run(() -> {
            throw runtimeFailure;
        }, () -> {
        });

        Assert.assertEquals(1, reportedFailures.size());
        Assert.assertSame(runtimeFailure, reportedFailures.get(0));
    }

    @Test
    public void reportsNothingWhenTheInstallationSucceeds() {
        List<Throwable> reportedFailures = new ArrayList<>();
        List<String> performedSteps = new ArrayList<>();

        new BootstrapInstallationRunner(reportedFailures::add).run(() -> performedSteps.add("installed"), () -> {
        });

        Assert.assertEquals(new ArrayList<>(), reportedFailures);
        Assert.assertEquals(1, performedSteps.size());
    }

    @Test
    public void installsForRealOnALaterRequestInsteadOfReplayingTheFailureOfAnEarlierRequest() {
        List<Throwable> earlierRequestReportedFailures = new ArrayList<>();
        List<Throwable> laterRequestReportedFailures = new ArrayList<>();
        List<String> performedInstallations = new ArrayList<>();
        List<String> performedCompletions = new ArrayList<>();
        IOException earlierRequestFailure = new IOException("Truncated bootstrap archive entry");

        new BootstrapInstallationRunner(earlierRequestReportedFailures::add).run(() -> {
            performedInstallations.add("earlier request");
            throw earlierRequestFailure;
        }, () -> performedCompletions.add("earlier request"));

        new BootstrapInstallationRunner(laterRequestReportedFailures::add)
            .run(() -> performedInstallations.add("later request"),
                () -> performedCompletions.add("later request"));

        Assert.assertEquals("The earlier request must report its own failure.",
            Collections.singletonList(earlierRequestFailure), earlierRequestReportedFailures);
        Assert.assertEquals("The later request must install for real instead of skipping the installation.",
            Arrays.asList("earlier request", "later request"), performedInstallations);
        Assert.assertEquals("The later request must not report the remembered failure of the earlier request.",
            Collections.emptyList(), laterRequestReportedFailures);
        Assert.assertEquals("The later request must install rather than run the completion of an awaited"
                + " installation.",
            Collections.emptyList(), performedCompletions);
    }

    @Test
    public void rejectsAMissingFailureReporterSoAFailureCanNeverGoUnreported() {
        try {
            new BootstrapInstallationRunner(null);
            Assert.fail("A missing failure reporter must be rejected rather than accepted.");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("failure reporter"));
        }
    }
}
