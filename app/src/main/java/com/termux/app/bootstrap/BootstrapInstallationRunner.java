package com.termux.app.bootstrap;

public final class BootstrapInstallationRunner {

    public interface InstallationStep {
        void install() throws Exception;
    }

    public interface AwaitedInstallationCompletion {
        void completeAwaitedInstallation() throws Exception;
    }

    public interface FailureReporter {
        void reportBootstrapInstallationFailure(Throwable failure);
    }

    private static final Object PROCESS_WIDE_INSTALLATION_MONITOR = new Object();

    private static boolean processWideInstallationInFlight;

    private static Throwable finishedProcessWideInstallationFailure;

    private final FailureReporter failureReporter;

    public BootstrapInstallationRunner(FailureReporter failureReporter) {
        if (failureReporter == null) {
            throw new IllegalArgumentException("BootstrapInstallationRunner requires a failure reporter");
        }
        this.failureReporter = failureReporter;
    }

    public void run(InstallationStep installationStep) {
        run(installationStep, () -> {
        });
    }

    public void run(InstallationStep installationStep, AwaitedInstallationCompletion awaitedInstallationCompletion) {
        boolean thisRequestStartsTheInstallation;
        Throwable awaitedInstallationFailure = null;
        synchronized (PROCESS_WIDE_INSTALLATION_MONITOR) {
            thisRequestStartsTheInstallation = !processWideInstallationInFlight;
            if (thisRequestStartsTheInstallation) {
                processWideInstallationInFlight = true;
            } else {
                try {
                    awaitInFlightProcessWideInstallation();
                    awaitedInstallationFailure = finishedProcessWideInstallationFailure;
                } catch (InterruptedException interruption) {
                    Thread.currentThread().interrupt();
                    awaitedInstallationFailure = interruption;
                }
            }
        }

        if (thisRequestStartsTheInstallation) {
            performInstallation(installationStep);
            return;
        }
        if (awaitedInstallationFailure != null) {
            failureReporter.reportBootstrapInstallationFailure(awaitedInstallationFailure);
            return;
        }
        completeAwaitedInstallation(awaitedInstallationCompletion);
    }

    private void performInstallation(InstallationStep installationStep) {
        Throwable installationFailure = null;
        try {
            installationStep.install();
        } catch (Exception | OutOfMemoryError failure) {
            installationFailure = failure;
        } catch (Throwable failure) {
            finishProcessWideInstallation(failure);
            throw failure;
        }

        finishProcessWideInstallation(installationFailure);
        if (installationFailure != null) {
            failureReporter.reportBootstrapInstallationFailure(installationFailure);
        }
    }

    private void completeAwaitedInstallation(AwaitedInstallationCompletion awaitedInstallationCompletion) {
        try {
            awaitedInstallationCompletion.completeAwaitedInstallation();
        } catch (Exception | OutOfMemoryError failure) {
            failureReporter.reportBootstrapInstallationFailure(failure);
        }
    }

    private static void awaitInFlightProcessWideInstallation() throws InterruptedException {
        while (processWideInstallationInFlight) {
            PROCESS_WIDE_INSTALLATION_MONITOR.wait();
        }
    }

    private static void finishProcessWideInstallation(Throwable installationFailure) {
        synchronized (PROCESS_WIDE_INSTALLATION_MONITOR) {
            finishedProcessWideInstallationFailure = installationFailure;
            processWideInstallationInFlight = false;
            PROCESS_WIDE_INSTALLATION_MONITOR.notifyAll();
        }
    }
}
