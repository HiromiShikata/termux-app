package com.termux.app.voicepatrol;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

@RunWith(RobolectricTestRunner.class)
public class VoicePatrolServiceTest {

    @Test
    public void actionStopStopsRuntimeAndStopsService() {
        VoicePatrolService service = Robolectric.buildService(VoicePatrolService.class).create().get();
        service.onStartCommand(new Intent(service, VoicePatrolService.class)
            .setAction(VoicePatrolService.ACTION_STOP), 0, 1);

        assertFalse(VoicePatrolRuntime.isRunning());
        assertTrue(Shadows.shadowOf(service).isStoppedBySelf());
    }
}
