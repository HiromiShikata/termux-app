package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Handler;
import android.os.Looper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class SessionGroupingAutoRefresherTest {

    @Test
    public void foregroundRefreshesImmediatelyAndOnceAfterOneMinute() {
        AtomicInteger refreshCount = new AtomicInteger();
        SessionDefinitionAutoReloadScheduler scheduler = new SessionDefinitionAutoReloadScheduler(
            new Handler(Looper.getMainLooper()), refreshCount::incrementAndGet);
        SessionGroupingAutoRefresher refresher = new SessionGroupingAutoRefresher(
            scheduler, refreshCount::incrementAndGet);

        refresher.onForeground("https://example.test/index.json");

        assertEquals(1, refreshCount.get());
        assertTrue(scheduler.isScheduled());

        Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.MINUTES);

        assertEquals(2, refreshCount.get());
    }

    @Test
    public void backgroundStopsTheScheduledRefresh() {
        AtomicInteger refreshCount = new AtomicInteger();
        SessionDefinitionAutoReloadScheduler scheduler = new SessionDefinitionAutoReloadScheduler(
            new Handler(Looper.getMainLooper()), refreshCount::incrementAndGet);
        SessionGroupingAutoRefresher refresher = new SessionGroupingAutoRefresher(
            scheduler, refreshCount::incrementAndGet);

        refresher.onForeground("https://example.test/index.json");
        refresher.onBackground();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.MINUTES);

        assertEquals(1, refreshCount.get());
        assertFalse(scheduler.isScheduled());
    }

    @Test
    public void blankAndNullDefinitionsDoNotRefreshImmediately() {
        String[] definitionUrls = {null, "", "   "};

        for (String definitionUrl : definitionUrls) {
            AtomicInteger refreshCount = new AtomicInteger();
            SessionDefinitionAutoReloadScheduler scheduler = new SessionDefinitionAutoReloadScheduler(
                new Handler(Looper.getMainLooper()), refreshCount::incrementAndGet);
            SessionGroupingAutoRefresher refresher = new SessionGroupingAutoRefresher(
                scheduler, refreshCount::incrementAndGet);

            refresher.onForeground(definitionUrl);

            assertEquals(0, refreshCount.get());
            assertTrue(scheduler.isScheduled());
        }
    }
}
