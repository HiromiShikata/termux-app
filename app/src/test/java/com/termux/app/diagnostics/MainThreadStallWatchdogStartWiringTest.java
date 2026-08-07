package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainThreadStallWatchdogStartWiringTest {

    private static String applicationSource() throws IOException {
        Path source = Paths.get("src/main/java/com/termux/app/TermuxApplication.java");
        return new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
    }

    @Test
    public void applicationStartsTheStallWatchdogSoDiagnosticsCanNameTheBlockingCode() throws IOException {
        String source = applicationSource();

        Assert.assertTrue("The watchdog only records stalls while it is running, so the application must start it;"
                + " without this call the diagnostics report always says zero stalls and names nothing.",
            source.contains("MainThreadStallWatchdog.start()"));
    }
}
