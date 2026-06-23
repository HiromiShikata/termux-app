package com.termux.app.terminal;

import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalOutput;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class ExplicitCallEndToEndRedDotTest {

    private static final String CALLED_SESSION = "called-session";

    private static final class RecordingTerminalOutput extends TerminalOutput {

        private final SessionNewActivityStore mStore;
        private final String mSessionName;

        private RecordingTerminalOutput(SessionNewActivityStore store, String sessionName) {
            mStore = store;
            mSessionName = sessionName;
        }

        @Override
        public void write(byte[] data, int offset, int count) {
        }

        @Override
        public void titleChanged(String oldTitle, String newTitle) {
        }

        @Override
        public void onCopyTextToClipboard(String text) {
        }

        @Override
        public void onPasteTextFromClipboard() {
        }

        @Override
        public void onBell() {
        }

        @Override
        public void onMarkerNotification(String reason) {
            mStore.recordExplicitCall(mSessionName, System.currentTimeMillis(), reason);
        }

        @Override
        public void onUrgentNotification(String reason) {
            mStore.recordExplicitCall(mSessionName, System.currentTimeMillis(), reason);
        }

        @Override
        public void onSpeakNotification(String text) {
        }

        @Override
        public void onColorsChanged() {
        }
    }

    private static SessionNewActivityStore feed(String wire) {
        SessionNewActivityStore store = new SessionNewActivityStore();
        RecordingTerminalOutput output = new RecordingTerminalOutput(store, CALLED_SESSION);
        TerminalEmulator emulator = new TerminalEmulator(output, 8, 2, 13, 15, 4, null);
        byte[] bytes = wire.getBytes(StandardCharsets.UTF_8);
        emulator.append(bytes, bytes.length);
        return store;
    }

    @Test
    public void plainUrgentMarkerProducesRedTierWithReason() {
        SessionNewActivityStore store = feed("\033]9998;claude-urgent;needs approval\007");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));
        Assert.assertEquals("needs approval", store.getLastExplicitCallReason(CALLED_SESSION));
    }

    @Test
    public void tmuxWrappedUrgentMarkerProducesRedTierWithReason() {
        SessionNewActivityStore store =
            feed("\033Ptmux;\033\033]9998;claude-urgent;needs approval\007\033\\");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));
        Assert.assertEquals("needs approval", store.getLastExplicitCallReason(CALLED_SESSION));
    }

    @Test
    public void tmuxWrappedCompletionMarkerProducesRedTierWithReason() {
        SessionNewActivityStore store =
            feed("\033Ptmux;\033\033]9999;claude-done;all tests pass\007\033\\");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));
        Assert.assertEquals("all tests pass", store.getLastExplicitCallReason(CALLED_SESSION));
    }

    @Test
    public void redTierSurvivesAutomaticSeenTicksAfterTmuxWrappedMarker() {
        SessionNewActivityStore store =
            feed("\033Ptmux;\033\033]9998;claude-urgent;needs approval\007\033\\");

        long nowMillis = System.currentTimeMillis();
        for (long tick = nowMillis + 1_000L; tick <= nowMillis + 30_000L; tick += 1_000L) {
            if (!store.hasPendingExplicitCall(CALLED_SESSION)) {
                store.recordSeen(CALLED_SESSION, tick);
            }
        }

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));
    }
}
