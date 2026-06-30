package com.termux.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;
import android.os.IBinder;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ServiceTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class AlwaysConnectedWakeLockReconcileInstrumentedTest {

    @Rule
    public final ServiceTestRule serviceTestRule = new ServiceTestRule();

    @Test
    public void reconcileWithoutActiveDefinitionBackedSessionsDoesNotHoldWakeLock() throws TimeoutException {
        Intent serviceIntent = new Intent(
            ApplicationProvider.getApplicationContext(), TermuxService.class);
        IBinder binder = serviceTestRule.bindService(serviceIntent);
        TermuxService service = ((TermuxService.LocalBinder) binder).service;
        assertNotNull(service);

        service.reconcileAlwaysConnectedWakeLock();

        assertFalse(service.isWakeLockHeld());
    }

    @Test
    public void reconcileIsIdempotentWithNoActiveDefinitionBackedSessions() throws TimeoutException {
        Intent serviceIntent = new Intent(
            ApplicationProvider.getApplicationContext(), TermuxService.class);
        IBinder binder = serviceTestRule.bindService(serviceIntent);
        TermuxService service = ((TermuxService.LocalBinder) binder).service;
        assertNotNull(service);

        service.reconcileAlwaysConnectedWakeLock();
        service.reconcileAlwaysConnectedWakeLock();

        assertFalse(service.isWakeLockHeld());
    }
}
