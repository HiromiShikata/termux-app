package com.termux.app.bootstrap;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BootstrapInstallationRunnerAwaitedInstallationTest {

    private static final String FIRST_REQUEST_NAME = "first bootstrap installation request";

    private static final String SECOND_REQUEST_NAME = "second bootstrap installation request";

    private static final long REQUEST_TERMINATION_BOUND_MILLIS = 30L * 1000L;

    private static final long HANDOFF_BOUND_SECONDS = 30L;

    private static final long AWAITING_STATE_BOUND_MILLIS = 10L * 1000L;

    private final List<String> performedInstallations = new CopyOnWriteArrayList<>();

    private final List<String> performedCompletions = new CopyOnWriteArrayList<>();

    private final List<Throwable> firstRequestReportedFailures = new CopyOnWriteArrayList<>();

    private final List<Throwable> secondRequestReportedFailures = new CopyOnWriteArrayList<>();

    private final CountDownLatch inFlightInstallationStarted = new CountDownLatch(1);

    private final CountDownLatch inFlightInstallationMayFinish = new CountDownLatch(1);

    @Test
    public void secondRequestRunsItsCompletionWithoutInstallingAfterTheAwaitedInstallationSucceeded()
        throws Exception {
        requestASecondInstallationWhileTheFirstOneIsInFlight(null);

        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " ran its own installation instead of awaiting the "
                + FIRST_REQUEST_NAME,
            Collections.singletonList(FIRST_REQUEST_NAME), performedInstallations);
        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " never ran its completion after awaiting the "
                + FIRST_REQUEST_NAME,
            Collections.singletonList(SECOND_REQUEST_NAME), performedCompletions);
        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " reported a failure although the " + FIRST_REQUEST_NAME
                + " succeeded",
            Collections.emptyList(), secondRequestReportedFailures);
    }

    @Test
    public void secondRequestReportsTheFailureOfTheAwaitedInstallationInsteadOfCompletingAsIfItSucceeded()
        throws Exception {
        IOException awaitedInstallationFailure = new IOException("Truncated bootstrap archive entry");

        requestASecondInstallationWhileTheFirstOneIsInFlight(awaitedInstallationFailure);

        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " did not report the failure of the " + FIRST_REQUEST_NAME
                + " it awaited, but reported " + secondRequestReportedFailures,
            Collections.singletonList(awaitedInstallationFailure), secondRequestReportedFailures);
        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " ran its completion although the " + FIRST_REQUEST_NAME
                + " it awaited failed",
            Collections.emptyList(), performedCompletions);
        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " ran its own installation instead of awaiting the "
                + FIRST_REQUEST_NAME,
            Collections.singletonList(FIRST_REQUEST_NAME), performedInstallations);
        Assert.assertEquals("the " + FIRST_REQUEST_NAME + " did not report its own failure",
            Collections.singletonList(awaitedInstallationFailure), firstRequestReportedFailures);
    }

    private void requestASecondInstallationWhileTheFirstOneIsInFlight(Exception inFlightInstallationFailure)
        throws Exception {
        Thread firstRequestThread = new Thread(() -> new BootstrapInstallationRunner(firstRequestReportedFailures::add)
            .run(() -> {
                performedInstallations.add(FIRST_REQUEST_NAME);
                inFlightInstallationStarted.countDown();
                awaitHandoff(inFlightInstallationMayFinish);
                if (inFlightInstallationFailure != null) {
                    throw inFlightInstallationFailure;
                }
            }, () -> performedCompletions.add(FIRST_REQUEST_NAME)), FIRST_REQUEST_NAME);
        firstRequestThread.start();

        awaitHandoff(inFlightInstallationStarted);

        Thread secondRequestThread = new Thread(() -> new BootstrapInstallationRunner(secondRequestReportedFailures::add)
            .run(() -> performedInstallations.add(SECOND_REQUEST_NAME),
                () -> performedCompletions.add(SECOND_REQUEST_NAME)), SECOND_REQUEST_NAME);
        secondRequestThread.start();

        awaitTheSecondRequestLeavingItsInstallationUnstarted(secondRequestThread);
        inFlightInstallationMayFinish.countDown();

        firstRequestThread.join(REQUEST_TERMINATION_BOUND_MILLIS);
        secondRequestThread.join(REQUEST_TERMINATION_BOUND_MILLIS);

        Assert.assertFalse("the " + FIRST_REQUEST_NAME + " never terminated", firstRequestThread.isAlive());
        Assert.assertFalse("the " + SECOND_REQUEST_NAME + " never terminated", secondRequestThread.isAlive());
    }

    private static void awaitTheSecondRequestLeavingItsInstallationUnstarted(Thread secondRequestThread)
        throws InterruptedException {
        long boundReachedAt = System.currentTimeMillis() + AWAITING_STATE_BOUND_MILLIS;
        while (System.currentTimeMillis() < boundReachedAt) {
            Thread.State secondRequestState = secondRequestThread.getState();
            if (secondRequestState == Thread.State.WAITING || secondRequestState == Thread.State.TERMINATED) {
                return;
            }
            Thread.sleep(1L);
        }
    }

    private static void awaitHandoff(CountDownLatch handoff) {
        try {
            handoff.await(HANDOFF_BOUND_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Waiting for a bootstrap installation handoff was interrupted",
                interruption);
        }
    }
}
