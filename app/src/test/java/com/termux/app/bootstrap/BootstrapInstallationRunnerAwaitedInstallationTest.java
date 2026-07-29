package com.termux.app.bootstrap;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BootstrapInstallationRunnerAwaitedInstallationTest {

    private static final String FIRST_REQUEST_NAME = "first bootstrap installation request";

    private static final String SECOND_REQUEST_NAME = "second bootstrap installation request";

    private static final long REQUEST_TERMINATION_BOUND_MILLIS = 30L * 1000L;

    private static final long HANDOFF_BOUND_SECONDS = 30L;

    private static final long AWAITING_STATE_BOUND_MILLIS = 10L * 1000L;

    private final List<String> performedInstallations = new CopyOnWriteArrayList<>();

    private final List<String> performedCompletions = new CopyOnWriteArrayList<>();

    private final List<Throwable> firstRequestReportedFailures = new CopyOnWriteArrayList<>();

    private final List<Throwable> firstRequestEscapedFailures = new CopyOnWriteArrayList<>();

    private final List<Throwable> secondRequestReportedFailures = new CopyOnWriteArrayList<>();

    private final AtomicBoolean secondRequestKeptItsInterruptedState = new AtomicBoolean();

    private final CountDownLatch inFlightInstallationStarted = new CountDownLatch(1);

    private final CountDownLatch inFlightInstallationMayFinish = new CountDownLatch(1);

    @Test
    public void secondRequestRunsItsCompletionWithoutInstallingAfterTheAwaitedInstallationSucceeded()
        throws Exception {
        requestASecondInstallationWhileTheFirstOneIsInFlight(() -> {
        }, recordedCompletionOfTheSecondRequest());

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

        requestASecondInstallationWhileTheFirstOneIsInFlight(() -> {
            throw awaitedInstallationFailure;
        }, recordedCompletionOfTheSecondRequest());

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

    @Test
    public void secondRequestWakesAndTerminatesAfterTheAwaitedInstallationThrewARuntimeFailure() throws Exception {
        RuntimeException awaitedInstallationFailure = new RuntimeException("No SYMLINKS.txt encountered");

        requestASecondInstallationWhileTheFirstOneIsInFlight(() -> {
            throw awaitedInstallationFailure;
        }, recordedCompletionOfTheSecondRequest());

        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " kept waiting for the " + FIRST_REQUEST_NAME
                + " after it threw a runtime failure, instead of being woken and reporting that failure",
            Collections.singletonList(awaitedInstallationFailure), secondRequestReportedFailures);
        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " ran its completion although the " + FIRST_REQUEST_NAME
                + " it awaited threw a runtime failure",
            Collections.emptyList(), performedCompletions);
        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " ran its own installation instead of awaiting the "
                + FIRST_REQUEST_NAME,
            Collections.singletonList(FIRST_REQUEST_NAME), performedInstallations);
    }

    @Test
    public void secondRequestWakesAndTerminatesAfterAnErrorEscapedTheAwaitedInstallationThread() throws Exception {
        Error abnormalInstallationTermination = new Error("The bootstrap installation thread died abnormally");

        requestASecondInstallationWhileTheFirstOneIsInFlight(() -> {
            throw abnormalInstallationTermination;
        }, recordedCompletionOfTheSecondRequest());

        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " kept waiting for the " + FIRST_REQUEST_NAME
                + " after an error escaped it, so bootstrap installation requests block forever during startup",
            Collections.singletonList(abnormalInstallationTermination), secondRequestReportedFailures);
        Assert.assertEquals("the error was swallowed instead of escaping the " + FIRST_REQUEST_NAME + " thread",
            Collections.singletonList(abnormalInstallationTermination), firstRequestEscapedFailures);
        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " ran its completion although an error escaped the "
                + FIRST_REQUEST_NAME + " it awaited",
            Collections.emptyList(), performedCompletions);
        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " ran its own installation instead of awaiting the "
                + FIRST_REQUEST_NAME,
            Collections.singletonList(FIRST_REQUEST_NAME), performedInstallations);
    }

    @Test
    public void secondRequestReportsAFailureThrownByItsOwnCompletionInsteadOfLettingItEscapeItsThread()
        throws Exception {
        RuntimeException completionFailure = new RuntimeException("Recreating the environment file failed");

        requestASecondInstallationWhileTheFirstOneIsInFlight(() -> {
        }, () -> {
            throw completionFailure;
        });

        Assert.assertEquals("the failure thrown by the completion of the " + SECOND_REQUEST_NAME
                + " was not reported through its failure reporter",
            Collections.singletonList(completionFailure), secondRequestReportedFailures);
    }

    @Test
    public void secondRequestReportsTheInterruptionOfItsWaitInsteadOfCompletingAsIfTheInstallationSucceeded()
        throws Exception {
        Thread firstRequestThread = startTheFirstRequest(() -> {
        });
        awaitHandoff(inFlightInstallationStarted);

        Thread secondRequestThread = startTheSecondRequest(recordedCompletionOfTheSecondRequest());
        awaitTheSecondRequestLeavingItsInstallationUnstarted(secondRequestThread);
        secondRequestThread.interrupt();
        secondRequestThread.join(REQUEST_TERMINATION_BOUND_MILLIS);

        Assert.assertFalse("the " + SECOND_REQUEST_NAME + " never terminated after its wait was interrupted",
            secondRequestThread.isAlive());
        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " reported " + secondRequestReportedFailures
                + " instead of the interruption of its wait",
            1, secondRequestReportedFailures.size());
        Assert.assertTrue("the " + SECOND_REQUEST_NAME + " reported "
                + secondRequestReportedFailures.get(0).getClass().getName()
                + " instead of the interruption of its wait",
            secondRequestReportedFailures.get(0) instanceof InterruptedException);
        Assert.assertTrue("the " + SECOND_REQUEST_NAME + " lost its interrupted state instead of restoring it",
            secondRequestKeptItsInterruptedState.get());
        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " ran its completion although its wait was interrupted",
            Collections.emptyList(), performedCompletions);
        Assert.assertEquals("the " + SECOND_REQUEST_NAME + " ran its own installation although its wait was"
                + " interrupted",
            Collections.singletonList(FIRST_REQUEST_NAME), performedInstallations);

        inFlightInstallationMayFinish.countDown();
        firstRequestThread.join(REQUEST_TERMINATION_BOUND_MILLIS);
        Assert.assertFalse("the " + FIRST_REQUEST_NAME + " never terminated", firstRequestThread.isAlive());
    }

    private void requestASecondInstallationWhileTheFirstOneIsInFlight(
        BootstrapInstallationRunner.InstallationStep inFlightInstallationOutcome,
        BootstrapInstallationRunner.AwaitedInstallationCompletion secondRequestCompletion) throws Exception {
        Thread firstRequestThread = startTheFirstRequest(inFlightInstallationOutcome);
        awaitHandoff(inFlightInstallationStarted);

        Thread secondRequestThread = startTheSecondRequest(secondRequestCompletion);
        awaitTheSecondRequestLeavingItsInstallationUnstarted(secondRequestThread);
        inFlightInstallationMayFinish.countDown();

        firstRequestThread.join(REQUEST_TERMINATION_BOUND_MILLIS);
        secondRequestThread.join(REQUEST_TERMINATION_BOUND_MILLIS);

        Assert.assertFalse("the " + FIRST_REQUEST_NAME + " never terminated", firstRequestThread.isAlive());
        Assert.assertFalse("the " + SECOND_REQUEST_NAME + " never terminated, so it is still waiting for the "
                + FIRST_REQUEST_NAME + " that already left the installation",
            secondRequestThread.isAlive());
    }

    private Thread startTheFirstRequest(BootstrapInstallationRunner.InstallationStep inFlightInstallationOutcome) {
        Thread firstRequestThread = new Thread(() -> new BootstrapInstallationRunner(firstRequestReportedFailures::add)
            .run(() -> {
                performedInstallations.add(FIRST_REQUEST_NAME);
                inFlightInstallationStarted.countDown();
                awaitHandoff(inFlightInstallationMayFinish);
                inFlightInstallationOutcome.install();
            }, () -> performedCompletions.add(FIRST_REQUEST_NAME)), FIRST_REQUEST_NAME);
        firstRequestThread.setDaemon(true);
        firstRequestThread.setUncaughtExceptionHandler((thread, failure) -> firstRequestEscapedFailures.add(failure));
        firstRequestThread.start();
        return firstRequestThread;
    }

    private Thread startTheSecondRequest(
        BootstrapInstallationRunner.AwaitedInstallationCompletion secondRequestCompletion) {
        Thread secondRequestThread = new Thread(() -> new BootstrapInstallationRunner(failure -> {
            secondRequestKeptItsInterruptedState.set(Thread.currentThread().isInterrupted());
            secondRequestReportedFailures.add(failure);
        }).run(() -> performedInstallations.add(SECOND_REQUEST_NAME), secondRequestCompletion), SECOND_REQUEST_NAME);
        secondRequestThread.setDaemon(true);
        secondRequestThread.start();
        return secondRequestThread;
    }

    private BootstrapInstallationRunner.AwaitedInstallationCompletion recordedCompletionOfTheSecondRequest() {
        return () -> performedCompletions.add(SECOND_REQUEST_NAME);
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
