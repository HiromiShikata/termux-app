package com.termux.app.voiceconsole;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class VoiceConsoleControllerTest {

    private static final String ENDPOINT = "https://console.example.com/api/live/sessions?k=tok";

    static final class FakeConnection implements LiveVoiceConnection {
        final List<String> sentEvents = new ArrayList<>();
        int closeCount;

        @Override
        public void sendEvent(String eventJson) {
            sentEvents.add(eventJson);
        }

        @Override
        public void close() {
            closeCount++;
        }
    }

    static final class FakeFactory implements LiveVoiceConnectionFactory {
        final FakeConnection connection = new FakeConnection();
        final List<String> openedEndpoints = new ArrayList<>();
        LiveVoiceConnectionListener listener;

        @Override
        public LiveVoiceConnection open(String endpointUrl, LiveVoiceConnectionListener connectionListener) {
            openedEndpoints.add(endpointUrl);
            listener = connectionListener;
            return connection;
        }
    }

    @Test
    public void startOpensTheEndpointAndConnectsWhenTheConnectionReports() {
        FakeFactory factory = new FakeFactory();
        VoiceConsoleController controller = new VoiceConsoleController(ENDPOINT, factory, reasons -> { });

        controller.start();
        assertTrue(controller.isRunning());
        assertFalse(controller.isConnected());
        assertEquals(Collections.singletonList(ENDPOINT), factory.openedEndpoints);

        factory.listener.onConnected();
        assertTrue(controller.isConnected());
    }

    @Test
    public void startWhileRunningDoesNotOpenASecondConnection() {
        FakeFactory factory = new FakeFactory();
        VoiceConsoleController controller = new VoiceConsoleController(ENDPOINT, factory, reasons -> { });

        controller.start();
        controller.start();
        assertEquals(1, factory.openedEndpoints.size());
    }

    @Test
    public void stopSendsSessionCloseOnceAndClosesTheConnection() {
        FakeFactory factory = new FakeFactory();
        VoiceConsoleController controller = new VoiceConsoleController(ENDPOINT, factory, reasons -> { });
        controller.start();
        factory.listener.onConnected();

        controller.stop();
        controller.stop();

        assertFalse(controller.isRunning());
        assertEquals(Collections.singletonList(VoiceConsoleEvent.SESSION_CLOSE_EVENT_JSON),
            factory.connection.sentEvents);
        assertEquals(1, factory.connection.closeCount);
    }

    @Test
    public void stopBeforeConnectedStillSendsSessionCloseAndClosesTheConnection() {
        FakeFactory factory = new FakeFactory();
        VoiceConsoleController controller = new VoiceConsoleController(ENDPOINT, factory, reasons -> { });
        controller.start();

        controller.stop();

        assertEquals(Collections.singletonList(VoiceConsoleEvent.SESSION_CLOSE_EVENT_JSON),
            factory.connection.sentEvents);
        assertEquals(1, factory.connection.closeCount);
    }

    @Test
    public void connectFailureReportsTheReasonAndStopsWithoutSendingSessionClose() {
        FakeFactory factory = new FakeFactory();
        List<String> reasons = new ArrayList<>();
        VoiceConsoleController controller = new VoiceConsoleController(ENDPOINT, factory, reasons::add);
        controller.start();

        factory.listener.onConnectFailed("network_error");

        assertFalse(controller.isRunning());
        assertEquals(Collections.singletonList("network_error"), reasons);
        assertEquals(1, factory.connection.closeCount);
        assertTrue(factory.connection.sentEvents.isEmpty());
    }

    @Test
    public void connectionLostAfterConnectedReportsTheReasonAndStops() {
        FakeFactory factory = new FakeFactory();
        List<String> reasons = new ArrayList<>();
        VoiceConsoleController controller = new VoiceConsoleController(ENDPOINT, factory, reasons::add);
        controller.start();
        factory.listener.onConnected();

        factory.listener.onDisconnected("ice_failed");

        assertFalse(controller.isRunning());
        assertEquals(Collections.singletonList("ice_failed"), reasons);
    }

    @Test
    public void sessionClosedOrErrorEventEndsTheSessionWithItsReason() {
        FakeFactory factory = new FakeFactory();
        List<String> reasons = new ArrayList<>();
        VoiceConsoleController controller = new VoiceConsoleController(ENDPOINT, factory, reasons::add);
        controller.start();
        factory.listener.onConnected();

        factory.listener.onEventReceived("{\"type\":\"error\"}");

        assertFalse(controller.isRunning());
        assertEquals(Collections.singletonList("session_error"), reasons);
    }

    @Test
    public void otherServerEventsAreIgnored() {
        FakeFactory factory = new FakeFactory();
        List<String> reasons = new ArrayList<>();
        VoiceConsoleController controller = new VoiceConsoleController(ENDPOINT, factory, reasons::add);
        controller.start();
        factory.listener.onConnected();

        factory.listener.onEventReceived("{\"type\":\"response.done\"}");

        assertTrue(controller.isConnected());
        assertTrue(reasons.isEmpty());
    }

    @Test
    public void failureReportedAfterStopIsIgnored() {
        FakeFactory factory = new FakeFactory();
        List<String> reasons = new ArrayList<>();
        VoiceConsoleController controller = new VoiceConsoleController(ENDPOINT, factory, reasons::add);
        controller.start();
        controller.stop();

        factory.listener.onConnectFailed("network_error");

        assertTrue(reasons.isEmpty());
    }

    @Test
    public void failureDuringOpenEndsTheSessionAndClosesTheConnection() {
        FakeConnection connection = new FakeConnection();
        List<String> reasons = new ArrayList<>();
        LiveVoiceConnectionFactory failingDuringOpen = (endpointUrl, listener) -> {
            listener.onConnectFailed("peer_connection_unavailable");
            return connection;
        };
        VoiceConsoleController controller = new VoiceConsoleController(ENDPOINT, failingDuringOpen, reasons::add);

        controller.start();

        assertFalse(controller.isRunning());
        assertEquals(Collections.singletonList("peer_connection_unavailable"), reasons);
        assertEquals(1, connection.closeCount);
    }
}
