package com.termux.app.shell;

import android.content.Context;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RunWith(RobolectricTestRunner.class)
public class SessionNumberCounterDoesNotBlockOnDiskTest {

    private static final String APP_PREFERENCES_PATH =
        "termux-shared/src/main/java/com/termux/shared/termux/settings/preferences/TermuxAppSharedPreferences.java";

    private String readRepositorySource(String repositoryRelativePath) throws IOException {
        Path fromRepositoryRoot = Paths.get(repositoryRelativePath);
        if (Files.exists(fromRepositoryRoot)) {
            return new String(Files.readAllBytes(fromRepositoryRoot), StandardCharsets.UTF_8);
        }
        Path fromModuleDirectory = Paths.get("..").resolve(repositoryRelativePath);
        return new String(Files.readAllBytes(fromModuleDirectory), StandardCharsets.UTF_8);
    }

    private String methodBody(String source, String signature) {
        int signatureIndex = source.indexOf(signature);
        Assert.assertTrue("method not found: " + signature, signatureIndex >= 0);
        int bodyEnd = source.indexOf("\n    }", signatureIndex);
        Assert.assertTrue("method end not found after: " + signature, bodyEnd > signatureIndex);
        return source.substring(signatureIndex, bodyEnd);
    }

    @Test
    public void countingATerminalSessionDoesNotAskForASynchronousFileCommit() throws IOException {
        String body = methodBody(readRepositorySource(APP_PREFERENCES_PATH),
            "public synchronized int getAndIncrementTerminalSessionNumberSinceBoot()");

        Assert.assertTrue("a synchronous commit runs fsync on whatever thread calls it, and this counter is"
                + " read while a session is being created on the main thread, so asking for one freezes the"
                + " user interface for as long as the write takes",
            body.contains("false, Integer.MAX_VALUE"));
    }

    @Test
    public void countingAnAppShellDoesNotAskForASynchronousFileCommit() throws IOException {
        String body = methodBody(readRepositorySource(APP_PREFERENCES_PATH),
            "public synchronized int getAndIncrementAppShellNumberSinceBoot()");

        Assert.assertTrue("the app shell counter is incremented on the same shell environment path as the"
                + " terminal session counter and blocks the same way",
            body.contains("false, Integer.MAX_VALUE"));
    }

    @Test
    public void theTerminalSessionCounterStillCountsUpFromOneCallToTheNext() {
        Context context = RuntimeEnvironment.getApplication();
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(context, true);
        Assert.assertNotNull("the preferences under test must exist, otherwise this test would pass"
            + " without exercising the counter at all", preferences);
        preferences.resetTerminalSessionNumberSinceBoot();

        int firstSession = preferences.getAndIncrementTerminalSessionNumberSinceBoot();
        int secondSession = preferences.getAndIncrementTerminalSessionNumberSinceBoot();
        int thirdSession = preferences.getAndIncrementTerminalSessionNumberSinceBoot();

        Assert.assertEquals("dropping the synchronous commit must not change what the shell environment"
            + " sees, because an applied write updates the in-memory value straight away",
            firstSession + 1, secondSession);
        Assert.assertEquals(secondSession + 1, thirdSession);
    }

    @Test
    public void theTerminalSessionCounterSurvivesANewPreferencesInstance() {
        Context context = RuntimeEnvironment.getApplication();
        TermuxAppSharedPreferences preferences = TermuxAppSharedPreferences.build(context, true);
        Assert.assertNotNull(preferences);
        preferences.resetTerminalSessionNumberSinceBoot();
        preferences.getAndIncrementTerminalSessionNumberSinceBoot();
        int secondSession = preferences.getAndIncrementTerminalSessionNumberSinceBoot();

        TermuxAppSharedPreferences reopenedPreferences = TermuxAppSharedPreferences.build(context, true);
        Assert.assertNotNull(reopenedPreferences);

        Assert.assertEquals("an applied write is visible to every reader of the same preferences, so the"
            + " count must carry on rather than restart",
            secondSession + 1, reopenedPreferences.getAndIncrementTerminalSessionNumberSinceBoot());
    }
}
