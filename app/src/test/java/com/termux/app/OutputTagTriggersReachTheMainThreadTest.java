package com.termux.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Looper;

import com.termux.app.apkupdate.UpdateTagUpdateController;
import com.termux.app.terminal.CallToUserTagController;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class OutputTagTriggersReachTheMainThreadTest {

    private static final String UPDATE_TAG_OUTPUT =
        "<update-termux-app> a newer build is available </update-termux-app>\n";

    private static final String CALL_TO_USER_TAG_OUTPUT =
        "<call-to-user> the owner is needed </call-to-user>\n";

    private static final int FEEDER_THREAD_COUNT = 8;

    private static final int SESSION_KEYS_PER_FEEDER_THREAD = 64;

    private static final int SESSION_KEY_COUNT = FEEDER_THREAD_COUNT * SESSION_KEYS_PER_FEEDER_THREAD;

    private static final long LATCH_TIMEOUT_SECONDS = 30L;

    private TermuxService service;

    @Before
    public void setUp() throws Exception {
        Context appContext = RuntimeEnvironment.getApplication();
        service = Robolectric.buildService(TermuxService.class).get();
        set(service, TermuxService.class, "mShellManager", new TermuxShellManager(appContext));
        set(service, TermuxService.class, "mProperties", TermuxAppSharedProperties.init(appContext));
    }

    @Test
    public void theUpdateTagTriggerIsDeferredToTheMainThreadWhenTheScanRunsOffIt() throws Exception {
        AtomicReference<String> triggeringThreadName = new AtomicReference<>(null);
        AtomicReference<Boolean> triggeredOnTheMainThread = new AtomicReference<>(null);
        service.setUpdateTagReasonTrigger(reason -> {
            triggeringThreadName.set(Thread.currentThread().getName());
            triggeredOnTheMainThread.set(Looper.myLooper() == Looper.getMainLooper());
        });

        feedFromABackgroundThread(() ->
            service.getUpdateTagUpdateController().onSessionTextChanged("handle-update", UPDATE_TAG_OUTPUT));

        assertNull("the app-update flow shows an install dialog, so the scan thread must never run it "
                + "directly; the trigger has to be deferred to the main thread, yet it had already been "
                + "invoked on " + triggeringThreadName.get() + " before the main looper was idled",
            triggeredOnTheMainThread.get());

        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("the deferred app-update trigger must actually arrive on the main thread once the "
                + "looper runs, otherwise moving the output tag scan off the main thread silently drops "
                + "the update prompt the owner is waiting for",
            Boolean.TRUE, triggeredOnTheMainThread.get());
    }

    @Test
    public void theUpdateTagFiresOncePerSessionKeyWhenEveryFeederThreadRegistersItsOwnKeys()
            throws Exception {
        AtomicInteger updateRequestCount = new AtomicInteger();
        UpdateTagUpdateController controller =
            new UpdateTagUpdateController(reason -> updateRequestCount.incrementAndGet());

        feedEveryKeyTwiceFromItsOwnThread(sessionKey ->
            controller.onSessionTextChanged(sessionKey, UPDATE_TAG_OUTPUT));

        assertEquals("the output tag scan now runs on the statusline parse thread while the foreground "
                + "session keeps feeding the same controller from the main thread, so the per-session "
                + "scanner registry is written from more than one thread at once; every session key is fed "
                + "the same tag twice here, so a registry that survives concurrent registration reports the "
                + "app update exactly once per key, whereas a registry that loses an entry hands that key a "
                + "fresh scanner and prompts the owner to update twice for one announcement",
            SESSION_KEY_COUNT, updateRequestCount.get());
    }

    @Test
    public void theCallToUserTagFiresOncePerSessionKeyWhenEveryFeederThreadRegistersItsOwnKeys()
            throws Exception {
        List<String> calledSessionHandles = Collections.synchronizedList(new ArrayList<>());
        CallToUserTagController controller =
            new CallToUserTagController((sessionHandle, reason, callCycleKey) ->
                calledSessionHandles.add(sessionHandle));

        feedEveryKeyTwiceFromItsOwnThread(sessionKey ->
            controller.onSessionTextChanged(sessionKey, CALL_TO_USER_TAG_OUTPUT));

        assertEquals("the call-to-user tag scan runs on the statusline parse thread for displayed sessions "
                + "while the foreground session feeds the same controller from the main thread, so the "
                + "per-session scanner registry is written from more than one thread at once; every session "
                + "key is fed the same call twice here, so a registry that survives concurrent registration "
                + "calls the owner exactly once per key, whereas a registry that loses an entry calls him "
                + "twice for one request",
            SESSION_KEY_COUNT, calledSessionHandles.size());
        assertEquals("each session key that called the owner must appear exactly once, so no key was "
                + "silently dropped while another key was registered concurrently",
            SESSION_KEY_COUNT, new LinkedHashSet<>(calledSessionHandles).size());
    }

    private interface SessionKeyFeed {

        void feed(String sessionKey);
    }

    private void feedEveryKeyTwiceFromItsOwnThread(SessionKeyFeed feed) throws Exception {
        CountDownLatch startTogether = new CountDownLatch(1);
        CountDownLatch allFinished = new CountDownLatch(FEEDER_THREAD_COUNT);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>(null);
        for (int threadIndex = 0; threadIndex < FEEDER_THREAD_COUNT; threadIndex++) {
            int firstKeyIndex = threadIndex * SESSION_KEYS_PER_FEEDER_THREAD;
            Thread feeder = new Thread(() -> {
                try {
                    startTogether.await();
                    for (int keyOffset = 0; keyOffset < SESSION_KEYS_PER_FEEDER_THREAD; keyOffset++) {
                        String sessionKey = "handle-" + (firstKeyIndex + keyOffset);
                        feed.feed(sessionKey);
                        feed.feed(sessionKey);
                    }
                } catch (Throwable theFeederFailed) {
                    firstFailure.compareAndSet(null, theFeederFailed);
                } finally {
                    allFinished.countDown();
                }
            });
            feeder.start();
        }
        startTogether.countDown();
        assertTrue("every feeder thread must finish within " + LATCH_TIMEOUT_SECONDS + " seconds; a thread "
                + "still running means the shared scanner registry deadlocked or spun under concurrent "
                + "access",
            allFinished.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertNull("feeding the shared scanner registry concurrently must not throw, yet a feeder failed "
            + "with " + firstFailure.get(), firstFailure.get());
    }

    private void feedFromABackgroundThread(Runnable feed) throws Exception {
        CountDownLatch fed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>(null);
        Thread scanThread = new Thread(() -> {
            try {
                feed.run();
            } catch (Throwable theScanFailed) {
                failure.set(theScanFailed);
            } finally {
                fed.countDown();
            }
        }, "StatuslineParseStandIn");
        scanThread.start();
        assertTrue("the stand-in scan thread must finish within " + LATCH_TIMEOUT_SECONDS + " seconds",
            fed.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertNull("feeding the controller from a background thread must not throw, yet it failed with "
            + failure.get(), failure.get());
    }

    private void set(Object target, Class<?> declaringClass, String fieldName, Object value)
            throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
