package com.termux.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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
public class TermuxServiceBindInstrumentedTest {

    @Rule
    public final ServiceTestRule serviceTestRule = new ServiceTestRule();

    @Test
    public void bindServiceExposesReachableTermuxServiceInstance() throws TimeoutException {
        Intent serviceIntent = new Intent(
            ApplicationProvider.getApplicationContext(), TermuxService.class);

        IBinder binder = serviceTestRule.bindService(serviceIntent);

        assertNotNull(binder);
        assertTrue(binder instanceof TermuxService.LocalBinder);

        TermuxService service = ((TermuxService.LocalBinder) binder).service;
        assertNotNull(service);
    }

    @Test
    public void startThenBindReturnsSameServiceInstance() throws TimeoutException {
        Intent serviceIntent = new Intent(
            ApplicationProvider.getApplicationContext(), TermuxService.class);

        serviceTestRule.startService(serviceIntent);
        IBinder binder = serviceTestRule.bindService(serviceIntent);

        assertNotNull(binder);
        TermuxService boundService = ((TermuxService.LocalBinder) binder).service;
        assertNotNull(boundService);
        assertSame(boundService, ((TermuxService.LocalBinder) binder).service);
    }
}
