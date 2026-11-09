package com.termux.app.voicepatrol;

import static org.junit.Assert.*;
import android.app.Notification;
import android.content.Intent;
import android.app.PendingIntent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowService;
import org.robolectric.shadows.Shadows;

@RunWith(RobolectricTestRunner.class)
public class VoicePatrolServiceTest {
 @Test public void actionStartRunsForegroundWithStopActionNotification(){VoicePatrolRuntime.stop();VoicePatrolService service=Robolectric.buildService(VoicePatrolService.class).create().get();service.onStartCommand(new Intent(service,VoicePatrolService.class).setAction(VoicePatrolService.ACTION_START),0,1);ShadowService shadow=Shadows.shadowOf(service);assertTrue(shadow.isForeground());Notification notification=shadow.getForegroundNotification();assertNotNull(notification);assertNotNull(notification.contentIntent);assertNotNull(notification.actions);assertTrue(notification.actions.length>0);boolean hasStopAction=false;for(Notification.Action action:notification.actions){PendingIntent pendingIntent=action.actionIntent;if(pendingIntent!=null){Intent actionIntent=Shadows.shadowOf(pendingIntent).getSavedIntent();if(VoicePatrolService.ACTION_STOP.equals(actionIntent.getAction())){hasStopAction=true;}}}assertTrue(hasStopAction);assertTrue(VoicePatrolRuntime.isRunning());VoicePatrolRuntime.stop();}
 @Test public void actionStopStopsRuntimeAndService(){VoicePatrolService service=Robolectric.buildService(VoicePatrolService.class).create().get();service.onStartCommand(new Intent(service,VoicePatrolService.class).setAction(VoicePatrolService.ACTION_STOP),0,1);assertFalse(VoicePatrolRuntime.isRunning());assertTrue(Shadows.shadowOf(service).isStoppedBySelf());}
}
