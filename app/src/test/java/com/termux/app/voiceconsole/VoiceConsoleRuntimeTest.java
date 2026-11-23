package com.termux.app.voiceconsole;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class VoiceConsoleRuntimeTest {

    private final List<Boolean> notifications = new ArrayList<>();
    private final VoiceConsoleRuntime.Listener listener = notifications::add;

    @After
    public void tearDown() {
        VoiceConsoleRuntime.stop();
        VoiceConsoleRuntime.removeListener(listener);
    }

    @Test
    public void startAndStopNotifyTheRunningStateOnce() {
        VoiceConsoleRuntime.addListener(listener);
        VoiceConsoleController controller = new VoiceConsoleController("https://console.example.com/api/live/sessions",
            new VoiceConsoleControllerTest.FakeFactory(), reasons -> { });

        VoiceConsoleRuntime.start(controller);
        assertTrue(VoiceConsoleRuntime.isRunning());
        VoiceConsoleRuntime.stop();
        VoiceConsoleRuntime.stop();

        assertFalse(VoiceConsoleRuntime.isRunning());
        assertEquals(Arrays.asList(true, false), notifications);
    }

    @Test
    public void failureWhileOpeningLeavesTheRuntimeStopped() {
        VoiceConsoleRuntime.addListener(listener);
        VoiceConsoleController controller = new VoiceConsoleController("https://console.example.com/api/live/sessions",
            (endpointUrl, connectionListener) -> {
                connectionListener.onConnectFailed("network_error");
                return new VoiceConsoleControllerTest.FakeConnection();
            },
            reasons -> VoiceConsoleRuntime.stop());

        VoiceConsoleRuntime.start(controller);

        assertFalse(VoiceConsoleRuntime.isRunning());
        assertEquals(Arrays.asList(false), notifications);
    }
}
