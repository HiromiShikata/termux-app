package com.termux.app.voicepatrol;

import static org.junit.Assert.*;
import android.os.Looper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class VoicePatrolRuntimeTest {
 @Test public void pollsOnMainLooperEveryTwoSecondsAndStopRemovesPolling(){FakeController c=new FakeController();VoicePatrolRuntime.start(c);assertTrue(VoicePatrolRuntime.isRunning());ShadowLooper looper=ShadowLooper.shadowOf(Looper.getMainLooper());looper.idleFor(1999);assertEquals(0,c.changes);looper.idleFor(1);assertEquals(1,c.changes);looper.idleFor(2000);assertEquals(2,c.changes);VoicePatrolRuntime.stop();assertFalse(VoicePatrolRuntime.isRunning());looper.idleFor(4000);assertEquals(2,c.changes);}
 static final class FakeController extends VoicePatrolController {int changes;FakeController(){super("endpoint",null,null,null,null,null);}public void onCallingSessionsChanged(){changes++;}}
}
