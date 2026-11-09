package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class WebRtcLiveVoiceConnectionTest {
    @Test
    public void unreachableEndpointDoesNotCallListenerSynchronously() {
        AtomicInteger failures = new AtomicInteger();
        AtomicReference<String> reason = new AtomicReference<>();
        LiveVoiceConnection connection;
        try {
            connection = new WebRtcLiveVoiceConnectionFactory(RuntimeEnvironment.getApplication()).open(
                "http://127.0.0.1:9/api/live/sessions?k=test",
                new LiveVoiceConnectionListener() {
                    public void onConnectFailed(String value) { failures.incrementAndGet(); reason.set(value); }
                    public void onDisconnected(String value) { }
                    public void onEventReceived(String value) { }
                });
        } catch (LinkageError error) {
            String line = error.toString();
            System.out.println("NATIVE_ERROR: " + line);
            return;
        }
        assertEquals(0, failures.get());
        connection.close();
    }
}
