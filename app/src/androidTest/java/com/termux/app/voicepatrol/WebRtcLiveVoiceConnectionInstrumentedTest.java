package com.termux.app.voicepatrol;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class WebRtcLiveVoiceConnectionInstrumentedTest {
    @Test
    public void unreachableEndpointReportsConnectionFailureAndCloses() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CountDownLatch failed = new CountDownLatch(1);
        AtomicReference<String> reason = new AtomicReference<>();
        LiveVoiceConnection connection = new WebRtcLiveVoiceConnectionFactory(context).open(
            "http://127.0.0.1:9/api/live/sessions?k=test",
            new LiveVoiceConnectionListener() {
                public void onConnectFailed(String value) { reason.set(value); failed.countDown(); }
                public void onDisconnected(String value) { }
                public void onEventReceived(String value) { }
            });
        try {
            assertFalse("connection failure was not reported", !failed.await(20, TimeUnit.SECONDS));
            assertNotNull(reason.get());
            assertFalse(reason.get().isEmpty());
        } finally {
            connection.close();
        }
    }
}
