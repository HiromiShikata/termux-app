package com.termux.app.terminal.tts;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * A diagnostics report from the owner's device recorded a 963 ms main-thread stall whose stack was
 * android.speech.tts.Voice being deserialized from a parcel. The engine hands its voice list back
 * when a language is applied, and the app applied the language inside the engine initialization
 * callback, which is delivered on the main thread.
 */
public class TtsLanguageIsAppliedOffTheEngineCallbackThreadTest {

    private static String synthesizerSource() throws IOException {
        return new String(Files.readAllBytes(
                Paths.get("src/main/java/com/termux/app/terminal/tts/AndroidTextToSpeechSynthesizer.java")),
            StandardCharsets.UTF_8);
    }

    private static String initializeBody() throws IOException {
        String source = synthesizerSource();
        int start = source.indexOf("public void initialize(");
        Assert.assertTrue("The engine initialization path must exist for this test to mean anything",
            start >= 0);
        int end = source.indexOf("public void speak(", start);
        Assert.assertTrue("The end of the engine initialization path must be locatable", end > start);
        return source.substring(start, end);
    }

    @Test
    public void theEngineCallbackHandsTheLanguageToAWorkerRatherThanApplyingItInline() throws IOException {
        String body = initializeBody();

        int callbackStart = body.indexOf("status ->");
        Assert.assertTrue("The engine initialization callback must exist: " + body, callbackStart >= 0);
        int inlineLanguageApplication = body.indexOf("mTextToSpeech.setLanguage(", callbackStart);
        int handOffToTheWorker = body.indexOf("mEngineWorkExecutor", callbackStart);

        Assert.assertTrue("The engine calls back on the main thread, and applying the language there makes"
                + " it deserialize its whole voice list, which stalled the owner's main thread for 963 ms."
                + " The language application must be handed to a worker: " + body,
            handOffToTheWorker >= 0);
        Assert.assertTrue("Applying the language directly in the callback is exactly the stall that was"
                + " captured, so no such call may remain in it: " + body,
            inlineLanguageApplication < 0);
    }

    @Test
    public void theCallerIsToldTheEngineIsReadyThroughThePlannerThatOrdersTheTwo() throws IOException {
        String body = initializeBody();

        Assert.assertTrue("Speech queued before the engine was ready is flushed the moment the caller is"
                + " told it is ready, so the ordering between applying the language and saying ready must"
                + " be decided in one place rather than re-derived here: " + body,
            body.contains("mReadyPlanner.onEngineInitialized("));
    }
}
