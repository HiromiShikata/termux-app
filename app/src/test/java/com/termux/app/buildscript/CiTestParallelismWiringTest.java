package com.termux.app.buildscript;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CiTestParallelismWiringTest {

    private static String rootBuildScript() throws IOException {
        Path source = Paths.get("../build.gradle");
        return new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
    }

    @Test
    public void theTestJvmCountIsDecidedSeparatelyForContinuousIntegrationAndForADeveloperMachine()
            throws IOException {
        String buildScript = rootBuildScript();

        Assert.assertTrue("Halving the processor count is right on a developer machine that is shared with an"
                + " editor and a browser, and wrong on a continuous integration runner that exists only to run"
                + " this build, where it leaves half the machine idle for the slowest job in the repository."
                + " The build script must therefore decide the test JVM count from the environment it runs in.",
            buildScript.contains("System.getenv(\"CI\")"));
    }

    @Test
    public void continuousIntegrationRunsTheTestsAtEveryProcessorTheRunnerHas() throws IOException {
        String buildScript = rootBuildScript();

        Assert.assertTrue("The runner is a dedicated single-purpose machine, so the whole of it is available"
                + " to the test JVMs and the count must not be divided down.",
            buildScript.contains("continuousIntegrationTestJvmCount = Math.max(1, availableProcessors)"));
    }

    @Test
    public void aDeveloperMachineKeepsRunningTheTestsAtHalfItsProcessors() throws IOException {
        String buildScript = rootBuildScript();

        Assert.assertTrue("A developer machine runs the build alongside everything else the developer is"
                + " doing, so it keeps the half it had before this distinction existed.",
            buildScript.contains("developerMachineTestJvmCount = Math.max(1, availableProcessors.intdiv(2))"));
    }
}
