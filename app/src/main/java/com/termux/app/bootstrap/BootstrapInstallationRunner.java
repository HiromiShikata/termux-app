package com.termux.app.bootstrap;

public final class BootstrapInstallationRunner {

    public interface InstallationStep {
        void install() throws Exception;
    }

    public interface FailureReporter {
        void reportBootstrapInstallationFailure(Throwable failure);
    }

    private final FailureReporter failureReporter;

    public BootstrapInstallationRunner(FailureReporter failureReporter) {
        if (failureReporter == null) {
            throw new IllegalArgumentException("BootstrapInstallationRunner requires a failure reporter");
        }
        this.failureReporter = failureReporter;
    }

    public void run(InstallationStep installationStep) {
        try {
            installationStep.install();
        } catch (Exception | OutOfMemoryError failure) {
            failureReporter.reportBootstrapInstallationFailure(failure);
        }
    }
}
